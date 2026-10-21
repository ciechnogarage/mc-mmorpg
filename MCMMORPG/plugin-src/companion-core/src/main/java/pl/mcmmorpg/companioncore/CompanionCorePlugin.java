package pl.mcmmorpg.companioncore;

import java.io.File;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

/** Core authority for one active companion per online owner. */
public final class CompanionCorePlugin extends JavaPlugin implements Listener {
  static final String TANK_T1 = "companion.tank.tier1";
  private final Map<UUID, ActiveCompanion> active = new HashMap<>();
  private final Map<UUID, UUID> ownerProfile = new HashMap<>();
  private NamespacedKey ownerKey;
  private NamespacedKey typeKey;
  private YamlConfiguration profiles;
  private File profilesFile;

  @Override public void onEnable() {
    saveDefaultConfig();
    ownerKey = new NamespacedKey(this, "owner_uuid");
    typeKey = new NamespacedKey(this, "companion_type");
    profilesFile = new File(getDataFolder(), "profiles.yml");
    profiles = YamlConfiguration.loadConfiguration(profilesFile);
    getServer().getPluginManager().registerEvents(this, this);
    if (getCommand("companion") != null) getCommand("companion").setExecutor(this::command);
  }

  @Override public void onDisable() { active.keySet().toArray(UUID[]::new); active.values().forEach(a -> despawn(a.entityId())); active.clear(); saveProfiles(); }
  @EventHandler public void onQuit(PlayerQuitEvent event) { dismiss(event.getPlayer(), false); ownerProfile.remove(event.getPlayer().getUniqueId()); saveProfiles(); }

  private boolean command(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) { sender.sendMessage("This command is player-only."); return true; }
    if (!player.hasPermission("companion.use")) return message(player, "&cBrak uprawnienia companion.use.");
    String sub = args.length == 0 ? "status" : args[0].toLowerCase();
    return switch (sub) {
      case "summon" -> summon(player, args.length > 1 ? args[1] : TANK_T1);
      case "dismiss" -> dismiss(player, true);
      case "mode" -> mode(player, args);
      case "status", "debug" -> status(player, sub.equals("debug"));
      case "admin" -> admin(player, args);
      default -> message(player, "&eUżycie: /companion summon <type>|dismiss|mode <follow>|status|debug");
    };
  }

  private boolean summon(Player owner, String type) {
    if (!owner.hasPermission("companion.summon")) return message(owner, "&cBrak uprawnienia companion.summon.");
    if (!TANK_T1.equals(type)) return message(owner, "&cNieznany typ kompana.");
    if (active.containsKey(owner.getUniqueId())) return message(owner, "&eMasz już aktywnego kompana.");
    Optional<UUID> profileId = resolveProfile(owner);
    if (profileId.isEmpty()) return message(owner, "&cAktywna postać MMOProfiles nie jest dostępna.");
    ownerProfile.put(owner.getUniqueId(), profileId.get());
    String base = profileId.get().toString();
    if (!profiles.getBoolean(base + ".unlocked." + TANK_T1, true)) return message(owner, "&cTen kompan nie jest odblokowany.");
    long available = profiles.getLong(base + ".respawn_available_at", 0L);
    if (available > Instant.now().toEpochMilli()) return message(owner, "&eKompan jest na cooldownie.");
    UUID entityId = spawnMythic(type, owner);
    if (entityId == null) return message(owner, "&cMythicMobs jest niedostępny lub spawn się nie udał.");
    active.put(owner.getUniqueId(), new ActiveCompanion(owner.getUniqueId(), profileId.get(), entityId, type, CompanionMode.FOLLOW));
    profiles.set(base + ".selected_type", type); profiles.set(base + ".selected_mode", "FOLLOW"); saveProfiles();
    return message(owner, "&aPrzywołano Tank T1.");
  }

  private boolean dismiss(Player owner, boolean feedback) {
    ActiveCompanion companion = active.remove(owner.getUniqueId());
    if (companion == null) return feedback ? message(owner, "&eNie masz aktywnego kompana.") : true;
    despawn(companion.entityId());
    return feedback ? message(owner, "&aKompan został odwołany.") : true;
  }

  private boolean mode(Player player, String[] args) {
    if (!player.hasPermission("companion.mode")) return message(player, "&cBrak uprawnienia companion.mode.");
    if (args.length < 2) return message(player, "&eUżycie: /companion mode <follow|hold|defensive|aggressive>");
    CompanionMode next;
    try { next = CompanionMode.valueOf(args[1].toUpperCase()); } catch (IllegalArgumentException ex) { return message(player, "&cNieznany tryb."); }
    ActiveCompanion current = active.get(player.getUniqueId());
    if (current != null) active.put(player.getUniqueId(), current.withMode(next));
    Optional<UUID> profile = resolveProfile(player);
    profile.ifPresent(id -> { profiles.set(id + ".selected_mode", next.name()); saveProfiles(); });
    return message(player, "&aTryb kompana: " + next.name());
  }

  private boolean status(Player player, boolean debug) {
    ActiveCompanion current = active.get(player.getUniqueId());
    if (current == null) return message(player, "&7Brak aktywnego kompana.");
    Entity entity = Bukkit.getEntity(current.entityId());
    String text = "&aAktywny: &f" + current.typeId() + " &7(" + current.mode() + ")";
    if (debug) text += " &8owner=" + current.ownerId() + " entity=" + current.entityId() + " exists=" + (entity != null);
    return message(player, text);
  }

  private boolean admin(Player player, String[] args) {
    if (!player.hasPermission("companion.admin")) return message(player, "&cBrak uprawnienia companion.admin.");
    if (args.length != 3 || !"unlock".equalsIgnoreCase(args[1]) || !TANK_T1.equals(args[2])) return message(player, "&eUżycie: /companion admin unlock companion.tank.tier1");
    return resolveProfile(player).map(id -> { profiles.set(id + ".unlocked." + TANK_T1, true); saveProfiles(); return message(player, "&aTank T1 odblokowany."); }).orElseGet(() -> message(player, "&cBrak aktywnej postaci MMOProfiles."));
  }

  private UUID spawnMythic(String type, Player owner) {
    Plugin mythic = Bukkit.getPluginManager().getPlugin("MythicMobs");
    if (mythic == null || !mythic.isEnabled()) return null;
    try {
      Class<?> bukkit = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
      Object api = bukkit.getMethod("inst").invoke(null);
      Object helper = bukkit.getMethod("getAPIHelper").invoke(api);
      Object activeMob = helper.getClass().getMethod("spawnMythicMob", String.class, Location.class).invoke(helper, "Companion_Tank_T1", owner.getLocation());
      Object abstractEntity = activeMob.getClass().getMethod("getEntity").invoke(activeMob);
      Entity entity = (Entity) abstractEntity.getClass().getMethod("getBukkitEntity").invoke(abstractEntity);
      // Tank T1 is a tamed wolf in the first slice, so Paper/vanilla owns FOLLOW pathfinding.
      if (entity instanceof Wolf wolf) { wolf.setTamed(true); wolf.setOwner(owner); }
      entity.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
      entity.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type);
      return entity.getUniqueId();
    } catch (ReflectiveOperationException | ClassCastException ex) { getLogger().warning("Companion spawn failed: " + ex.getClass().getSimpleName()); return null; }
  }

  private void despawn(UUID entityId) { Entity entity = Bukkit.getEntity(entityId); if (entity != null && entity.isValid()) entity.remove(); }
  private Optional<UUID> resolveProfile(Player player) {
    UUID cached = ownerProfile.get(player.getUniqueId()); if (cached != null) return Optional.of(cached);
    Plugin profilesPlugin = Bukkit.getPluginManager().getPlugin("MMOProfiles"); if (profilesPlugin == null) return Optional.empty();
    try {
      Object list = profilesPlugin.getClass().getMethod("getPlayerData", UUID.class).invoke(profilesPlugin, player.getUniqueId());
      if (list == null) return Optional.empty(); Object current = list.getClass().getMethod("getCurrent").invoke(list);
      if (current == null) return Optional.empty(); return Optional.of(UUID.fromString(String.valueOf(current.getClass().getMethod("getUniqueId").invoke(current))));
    } catch (ReflectiveOperationException | IllegalArgumentException ex) { return Optional.empty(); }
  }
  private boolean message(Player player, String value) { player.sendMessage(getConfig().getString("messages.prefix", "") + value); return true; }
  private void saveProfiles() { try { profiles.save(profilesFile); } catch (Exception ex) { getLogger().warning("Could not save companion profiles: " + ex.getClass().getSimpleName()); } }

  enum CompanionMode { FOLLOW, HOLD, DEFENSIVE, AGGRESSIVE }
  record ActiveCompanion(UUID ownerId, UUID profileId, UUID entityId, String typeId, CompanionMode mode) { ActiveCompanion withMode(CompanionMode next) { return new ActiveCompanion(ownerId, profileId, entityId, typeId, next); } }
}
