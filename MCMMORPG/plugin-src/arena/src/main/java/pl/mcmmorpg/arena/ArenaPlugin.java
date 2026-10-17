package pl.mcmmorpg.arena;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.lib.api.event.PlayerAttackEvent;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.damage.DamageMetadata;
import io.lumine.mythic.lib.damage.DamageType;
import net.Indyuce.mmocore.api.event.PlayerExperienceGainEvent;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Arena is deliberately self-contained: all travel is mediated by one console command. */
public final class ArenaPlugin extends JavaPlugin implements Listener {
  private record ReturnPoint(String world, double x, double y, double z, float yaw, float pitch, String profile) {}
  private record Hit(long at, double amount) {}
  private static final class Meter {
    double total;
    int count;
    final ArrayDeque<Hit> recent = new ArrayDeque<>();
  }
  private final Map<UUID, ReturnPoint> returns = new HashMap<>();
  private final Map<UUID, Long> combatUntil = new HashMap<>();
  private final Map<UUID, Long> forfeitUntil = new HashMap<>();
  private final Map<UUID, UUID> lastOpponent = new HashMap<>();
  private final Map<UUID, Double> deathExp = new HashMap<>();
  private final Map<UUID, Long> safeUntil = new HashMap<>();
  private final Map<UUID, Meter> meters = new HashMap<>();
  private final Map<UUID, UUID> dummyOwner = new HashMap<>();
  private final Map<UUID, Long> dummyOwnerTime = new HashMap<>();
  private final Map<UUID, Long> lastNumber = new HashMap<>();
  private final Map<UUID, Long> lastDummyHitTick = new HashMap<>();
  private final Set<UUID> internalTeleport = new HashSet<>();
  private NamespacedKey dummyKey, numberKey, launchRingKey;
  private File stateFile;
  private long tick;

  @Override public void onEnable() {
    if (getConfig().getBoolean("sonda-pvp", false)) getServer().getPluginManager().registerEvents(new Sonda(this), this);
    saveDefaultConfig();
    dummyKey = new NamespacedKey(this, "dummy");
    numberKey = new NamespacedKey(this, "number");
    launchRingKey = new NamespacedKey(this, "launch_ring");
    stateFile = new File(getDataFolder(), "returns.yml");
    loadState();
    Bukkit.getPluginManager().registerEvents(this, this);
    Bukkit.getScheduler().runTaskTimer(this, () -> {
      tick++;
      if (tick % getConfig().getLong("dummy.check-ticks") == 0) ensureDummies();
      if (tick % getConfig().getLong("service.player-check-ticks") == 0) {
        World world = arenaWorld();
        if (world != null) for (Player player : world.getPlayers()) {
          ReturnPoint saved = returns.get(player.getUniqueId());
          if (saved != null && !saved.profile.equals(profile(player)) && !forfeitUntil.containsKey(player.getUniqueId())) {
            forfeitUntil.put(player.getUniqueId(), System.currentTimeMillis() + getConfig().getLong("combat.logout-penalty-seconds") * 1000L);
            combatUntil.remove(player.getUniqueId());
            safeTeleport(player, spawn());
            deny(player, "Zmiana postaci oznacza poddanie walki; powrót tylko do Stolicy.");
            saveState();
          }
          if (forfeitUntil.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis() && inRing(player.getLocation())) safeTeleport(player, spawn());
        }
      }
    }, getConfig().getLong("service.startup-delay-ticks"), 1L);
    Bukkit.getScheduler().runTaskLater(this, this::cleanNumbers, getConfig().getLong("service.number-cleanup-delay-ticks"));
  }

  @Override public void onDisable() { saveState(); cleanNumbers(); }

  private World arenaWorld() { return Bukkit.getWorld(getConfig().getString("world", "arena")); }
  private boolean arena(Location location) { return location != null && location.getWorld() != null && location.getWorld().getName().equals(getConfig().getString("world", "arena")); }
  private Location point(String path, World world) {
    return new Location(world, getConfig().getDouble(path + ".x"), getConfig().getDouble(path + ".y"), getConfig().getDouble(path + ".z"), (float) getConfig().getDouble(path + ".yaw"), (float) getConfig().getDouble(path + ".pitch"));
  }
  private Location spawn() { World world = arenaWorld(); return world == null ? null : point("spawn", world); }
  private boolean inRing(Location location) {
    if (!arena(location)) return false;
    double dx = location.getX() - getConfig().getDouble("ring.x");
    double dz = location.getZ() - getConfig().getDouble("ring.z");
    double r = getConfig().getDouble("ring.pvp-radius");
    return dx * dx + dz * dz <= r * r && location.getY() >= getConfig().getDouble("ring.min-y") && location.getY() <= getConfig().getDouble("ring.max-y");
  }
  private void bar(Player player, int priority, String message) {
    if (MMOPlayerData.has(player)) MMOPlayerData.get(player).getActionBar().show(priority, getConfig().getLong("ui.actionbar-ticks"), message);
    else player.sendMessage(message);
  }
  private void deny(Player player, String message) { bar(player, 40, "§c" + message); }
  private String profile(Player player) {
    if (!MMOPlayerData.has(player)) return "";
    try {
      UUID id = MMOPlayerData.get(player).getProfileId();
      return id == null ? "" : id.toString();
    } catch (RuntimeException brakProfilu) {   // MythicLib rzuca NPE "No profile", gdy postac nie jest wybrana (kreator)
      return "";
    }
  }
  private boolean tagged(Player player) { return combatUntil.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis(); }
  private void tag(Player a, Player b) {
    long until = System.currentTimeMillis() + getConfig().getLong("combat.seconds") * 1000L;
    combatUntil.put(a.getUniqueId(), until);
    combatUntil.put(b.getUniqueId(), until);
    lastOpponent.put(a.getUniqueId(), b.getUniqueId());
    lastOpponent.put(b.getUniqueId(), a.getUniqueId());
  }
  private boolean safeTeleport(Player player, Location target) {
    if (target == null) return false;
    internalTeleport.add(player.getUniqueId());
    try { return player.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN); }
    finally { internalTeleport.remove(player.getUniqueId()); }
  }
  private boolean validReturn(ReturnPoint p, Player player) {
    if (p == null || !p.profile.equals(profile(player))) return false;
    if (!getConfig().getStringList("entry-worlds").contains(p.world)) return false;
    World world = Bukkit.getWorld(p.world);
    if (world == null) return false;
    Location location = new Location(world, p.x, p.y, p.z);
    return location.getBlock().isPassable() && location.clone().add(0, 1, 0).getBlock().isPassable()
        && location.clone().add(0, -1, 0).getBlock().getType().isSolid();
  }
  private Location fallback() {
    for (String worldName : getConfig().getStringList("fallback-worlds")) {
      World world = Bukkit.getWorld(worldName);
      if (world != null) return world.getSpawnLocation().add(0.5, 0, 0.5);
    }
    return null;
  }

  @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    // menu woła [console]; RCON to tez tylko administrator (testy na klonie). Gracz nie ma dostepu.
    if (!(sender instanceof org.bukkit.command.ConsoleCommandSender) && !(sender instanceof org.bukkit.command.RemoteConsoleCommandSender)) { sender.sendMessage("§cTylko menu areny może wykonać tę czynność."); return true; }
    if (args.length != 2 || !(args[0].equalsIgnoreCase("wejdz") || args[0].equalsIgnoreCase("wyjdz"))) { sender.sendMessage("arena <wejdz|wyjdz> <gracz>"); return true; }
    Player player = Bukkit.getPlayerExact(args[1]);
    if (player == null) { sender.sendMessage("Gracz jest offline: " + args[1]); return true; }
    if (args[0].equalsIgnoreCase("wejdz")) enter(player); else leave(player);
    return true;
  }
  private void enter(Player player) {
    Location target = spawn();
    if (target == null || !target.clone().add(0, -1, 0).getBlock().getType().isSolid()) { deny(player, "Arena nie jest gotowa."); return; }
    if (!getConfig().getStringList("entry-worlds").contains(player.getWorld().getName())) { deny(player, "Wejście możliwe tylko z bezpiecznego świata."); return; }
    if (player.isDead() || !player.isOnGround() || player.getFallDistance() > getConfig().getDouble("entry.max-fall-distance") || PlayerData.has(player) && PlayerData.get(player).isInCombat()) {
      deny(player, "Nie możesz wejść podczas walki, śmierci ani spadania."); return;
    }
    Location from = player.getLocation();
    ReturnPoint point = new ReturnPoint(from.getWorld().getName(), from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch(), profile(player));
    returns.put(player.getUniqueId(), point);
    saveState();
    if (!safeTeleport(player, target)) { returns.remove(player.getUniqueId()); saveState(); deny(player, "Teleport na arenę nie powiódł się."); return; }
    meters.remove(player.getUniqueId());
    lastNumber.remove(player.getUniqueId());
    safeUntil.put(player.getUniqueId(), System.currentTimeMillis() + getConfig().getLong("spawn.invulnerable-seconds") * 1000L);
    bar(player, 30, "§aOtwarta Arena: PvP tylko w ringu. Trening po prawej stronie.");
  }
  private void leave(Player player) {
    if (!arena(player.getLocation())) { deny(player, "Nie jesteś na arenie."); return; }
    if (tagged(player)) { deny(player, "Walka trwa. Wyjście za " + Math.max(1, (combatUntil.get(player.getUniqueId()) - System.currentTimeMillis() + 999) / 1000) + " s."); return; }
    ReturnPoint point = returns.get(player.getUniqueId());
    Location destination;
    if (validReturn(point, player)) {
      World world = Bukkit.getWorld(point.world);
      destination = new Location(world, point.x, point.y, point.z, point.yaw, point.pitch);
    } else destination = fallback();
    if (destination == null) { deny(player, "Miejsce powrotu jest niedostępne."); return; }
    safeTeleport(player, destination);
    if (!arena(player.getLocation())) { returns.remove(player.getUniqueId()); saveState(); }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onTeleport(PlayerTeleportEvent event) {
    if (internalTeleport.contains(event.getPlayer().getUniqueId()) || event.getTo() == null) return;
    // wewnatrz areny (perla, komenda administratora) - wolno; blokada tylko wejscia/wyjscia inna droga niz menu
    if (arena(event.getFrom()) && arena(event.getTo())) return;
    if (arena(event.getFrom()) || arena(event.getTo())) {
      event.setCancelled(true);
      deny(event.getPlayer(), "Podróż z areny odbywa się przez menu.");
    }
  }
  @EventHandler public void onMove(PlayerMoveEvent event) {
    Location to = event.getTo();
    if (to == null || !arena(to) || (to.getBlockX() == event.getFrom().getBlockX() && to.getBlockY() == event.getFrom().getBlockY() && to.getBlockZ() == event.getFrom().getBlockZ())) return;
    double dx = to.getX() - getConfig().getDouble("ring.x"), dz = to.getZ() - getConfig().getDouble("ring.z");
    double limit = getConfig().getDouble("boundary.radius");
    if (to.getY() < getConfig().getDouble("boundary.min-y") || dx * dx + dz * dz > limit * limit || forfeitUntil.getOrDefault(event.getPlayer().getUniqueId(), 0L) > System.currentTimeMillis() && inRing(to)) {
      safeTeleport(event.getPlayer(), spawn());
      deny(event.getPlayer(), "Powrót na platformę areny.");
    }
  }
  @EventHandler public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    if (arena(player.getLocation())) Bukkit.getScheduler().runTask(this, () -> {
      if (player.isOnline() && arena(player.getLocation())) { safeTeleport(player, spawn()); safeUntil.put(player.getUniqueId(), System.currentTimeMillis() + getConfig().getLong("spawn.invulnerable-seconds") * 1000L); }
    });
    combatUntil.remove(player.getUniqueId());
  }
  @EventHandler public void onQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    if (arena(player.getLocation()) && tagged(player)) {
      forfeitUntil.put(player.getUniqueId(), System.currentTimeMillis() + getConfig().getLong("combat.logout-penalty-seconds") * 1000L);
      UUID opponent = lastOpponent.get(player.getUniqueId());
      Player winner = opponent == null ? null : Bukkit.getPlayer(opponent);
      if (winner != null && arena(winner.getLocation())) winner.sendMessage("§e" + player.getName() + " poddał walkę przez wylogowanie.");
      saveState();
    }
    combatUntil.remove(player.getUniqueId());
    safeUntil.remove(player.getUniqueId());
  }
  @EventHandler public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
    if (!arena(event.getPlayer().getLocation())) return;
    String cmd = event.getMessage().split(" ", 2)[0].toLowerCase();
    if (getConfig().getStringList("blocked-player-commands").contains(cmd)) { event.setCancelled(true); deny(event.getPlayer(), "Ta komenda jest niedostępna na arenie."); }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onAttack(PlayerAttackEvent event) {
    Player attacker = event.getPlayer();
    LivingEntity target = event.getEntity();
    if (isDummy(target)) {
      DamageMetadata damage = event.getDamage();
      if (!event.isCancelled() && arena(attacker.getLocation()) && damage.getDamage() > 0) {
        boolean critical = damage.isWeaponCriticalStrike() || damage.isSkillCriticalStrike() || damage.collectElements().stream().anyMatch(damage::isElementalCriticalStrike);
        record(attacker, target, damage.getDamage(), kind(damage), critical);
        lastDummyHitTick.put(target.getUniqueId(), tick);
      }
      event.setCancelled(true);
      return;
    }
    if (target instanceof Player victim && (arena(attacker.getLocation()) || arena(victim.getLocation()))) {
      if (!inRing(attacker.getLocation()) || !inRing(victim.getLocation()) || protectedNow(victim) || event.getDamage().hasType(DamageType.MINION)) { dbg("onAttack: ring A=" + inRing(attacker.getLocation()) + " ring B=" + inRing(victim.getLocation()) + " ochrona=" + protectedNow(victim) + " minion=" + event.getDamage().hasType(DamageType.MINION)); event.setCancelled(true); }
      else if (!event.isCancelled() && event.getDamage().getDamage() > 0) tag(attacker, victim);
    }
  }
  private String kind(DamageMetadata d) {
    if (!d.collectElements().isEmpty()) return "żywioł " + d.collectElements().iterator().next().toString().toLowerCase();
    if (d.hasType(DamageType.SKILL)) return "umiejętność";
    if (d.hasType(DamageType.MAGIC)) return "magiczne";
    if (d.hasType(DamageType.DOT)) return "obrażenia w czasie";
    return "fizyczne";
  }
  private Player attacker(Entity damager) {
    if (damager instanceof Player player) return player;
    if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
    return null;
  }
  private void dbg(String m) { if (getConfig().getBoolean("sonda-pvp", false)) getLogger().info("SONDA-ARENA " + m); }
  private boolean protectedNow(Player player) { return safeUntil.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis(); }
  @EventHandler public void onProjectile(ProjectileLaunchEvent event) {
    if (event.getEntity().getShooter() instanceof Player player && inRing(player.getLocation())) event.getEntity().getPersistentDataContainer().set(launchRingKey, PersistentDataType.BYTE, (byte) 1);
  }
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onDamage(EntityDamageEvent event) {
    if (isDummy(event.getEntity())) {
      Entity dummy = event.getEntity();
      if (!event.isCancelled() && lastDummyHitTick.getOrDefault(dummy.getUniqueId(), -1L) != tick) {
        Player source = event instanceof EntityDamageByEntityEvent hit ? attacker(hit.getDamager()) : null;
        String type = "fizyczne";
        if (source == null && (event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK || event.getCause() == EntityDamageEvent.DamageCause.FIRE || event.getCause() == EntityDamageEvent.DamageCause.POISON || event.getCause() == EntityDamageEvent.DamageCause.WITHER)) {
          UUID owner = dummyOwner.get(dummy.getUniqueId());
          if (owner != null && System.currentTimeMillis() - dummyOwnerTime.getOrDefault(dummy.getUniqueId(), 0L) <= getConfig().getLong("dummy.owner-window-seconds") * 1000L) source = Bukkit.getPlayer(owner);
          type = "obrażenia w czasie";
        }
        if (source != null && arena(source.getLocation())) record(source, dummy, Math.max(0, event.getFinalDamage()), type, false);
      }
      event.setCancelled(true);
      return;
    }
    if (event.getEntity() instanceof Player victim && arena(victim.getLocation()) && (!inRing(victim.getLocation()) || protectedNow(victim))) { dbg("onDamage: ofiara poza ringiem/ochrona " + victim.getName() + " ring=" + inRing(victim.getLocation()) + " ochrona=" + protectedNow(victim)); event.setCancelled(true); return; }
    if (!(event instanceof EntityDamageByEntityEvent hit)) return;
    Player source = attacker(hit.getDamager());
    if (!(hit.getEntity() instanceof Player victim)) return;
    if (!arena(victim.getLocation()) && (source == null || !arena(source.getLocation()))) return;
    boolean projectileWasInRing = !(hit.getDamager() instanceof Projectile projectile) || projectile.getPersistentDataContainer().has(launchRingKey, PersistentDataType.BYTE);
    if (source == null || !inRing(source.getLocation()) || !inRing(victim.getLocation()) || !projectileWasInRing) { dbg("onDamage: zrodlo " + (source == null ? "brak" : source.getName() + " ring=" + inRing(source.getLocation())) + " pocisk-z-ringu=" + projectileWasInRing); event.setCancelled(true); return; }
    if (!event.isCancelled() && event.getFinalDamage() > 0) tag(source, victim);
  }
  @EventHandler(priority = EventPriority.HIGHEST) public void onExp(PlayerExperienceGainEvent event) {
    if (arena(event.getPlayer().getLocation())) event.setCancelled(true);
  }
  @EventHandler(priority = EventPriority.HIGHEST) public void onVanillaExp(org.bukkit.event.player.PlayerExpChangeEvent event) {
    if (arena(event.getPlayer().getLocation())) event.setAmount(0);
  }
  @EventHandler(priority = EventPriority.HIGHEST) public void onAdvancement(com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent event) {
    if (arena(event.getPlayer().getLocation())) event.setCancelled(true);
  }
  @EventHandler(priority = EventPriority.LOWEST) public void beforeDeath(PlayerDeathEvent event) {
    Player player = event.getEntity();
    if (arena(player.getLocation()) && PlayerData.has(player)) deathExp.put(player.getUniqueId(), PlayerData.get(player).getExperience());
  }
  @EventHandler(priority = EventPriority.HIGHEST) public void onDeath(PlayerDeathEvent event) {
    Player victim = event.getEntity();
    if (!arena(victim.getLocation())) return;
    event.setKeepInventory(true);
    event.getDrops().clear();
    event.setKeepLevel(true);
    event.setDroppedExp(0);
    event.deathMessage(null);
    UUID last = lastOpponent.get(victim.getUniqueId());
    Player killer = victim.getKiller();
    if (killer == null && last != null && tagged(victim)) killer = Bukkit.getPlayer(last);
    String line = killer == null ? victim.getName() + " poległ na arenie." : killer.getName() + " pokonał " + victim.getName() + ".";
    World world = arenaWorld();
    if (world != null) for (Player player : world.getPlayers()) player.sendMessage("§6" + line);
    combatUntil.remove(victim.getUniqueId());
  }
  @EventHandler(priority = EventPriority.MONITOR) public void afterDeath(PlayerDeathEvent event) {
    Player victim = event.getEntity();
    Double exp = deathExp.remove(victim.getUniqueId());
    if (exp != null) Bukkit.getScheduler().runTaskLater(this, () -> { if (PlayerData.has(victim)) PlayerData.get(victim).setExperience(exp); }, getConfig().getLong("service.exp-restore-delay-ticks"));
  }
  @EventHandler public void onRespawn(PlayerRespawnEvent event) {
    if (!arena(event.getPlayer().getLocation())) return;
    Location location = spawn();
    if (location != null) event.setRespawnLocation(location);
    safeUntil.put(event.getPlayer().getUniqueId(), System.currentTimeMillis() + getConfig().getLong("spawn.invulnerable-seconds") * 1000L);
  }

  private void record(Player player, Entity dummy, double amount, String kind, boolean crit) {
    if (!Double.isFinite(amount) || amount <= 0) return;
    UUID id = player.getUniqueId();
    Meter meter = meters.computeIfAbsent(id, ignored -> new Meter());
    long now = System.currentTimeMillis();
    meter.total += amount;
    meter.count++;
    meter.recent.addLast(new Hit(now, amount));
    long window = getConfig().getLong("ui.dps-window-seconds") * 1000L;
    while (!meter.recent.isEmpty() && meter.recent.peekFirst().at < now - window) meter.recent.removeFirst();
    double recent = meter.recent.stream().mapToDouble(h -> h.amount).sum();
    double dps = recent / (window / 1000.0);
    bar(player, 30, String.format(java.util.Locale.ROOT, "§e%.1f §7%s%s  §fSuma %.1f  DPS %.1f  Trafienia %d", amount, kind, crit ? " KRYT" : "", meter.total, dps, meter.count));
    dummyOwner.put(dummy.getUniqueId(), id);
    dummyOwnerTime.put(dummy.getUniqueId(), now);
    if (now - lastNumber.getOrDefault(id, 0L) < getConfig().getLong("ui.number-cooldown-ms")) return;
    lastNumber.put(id, now);
    if (dummy.getWorld().getEntitiesByClass(TextDisplay.class).stream().filter(e -> e.getPersistentDataContainer().has(numberKey, PersistentDataType.BYTE)).count() >= getConfig().getInt("ui.max-numbers")) return;
    Location above = dummy.getLocation().add(0, getConfig().getDouble("ui.number-height"), 0);
    TextDisplay display = dummy.getWorld().spawn(above, TextDisplay.class, e -> {
      e.text(Component.text(String.format(java.util.Locale.ROOT, "%.1f", amount)));
      e.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
      e.setPersistent(false);
      e.getPersistentDataContainer().set(numberKey, PersistentDataType.BYTE, (byte) 1);
    });
    Bukkit.getScheduler().runTaskLater(this, display::remove, getConfig().getLong("ui.number-lifetime-ticks"));
  }
  private boolean isDummy(Entity entity) { return entity != null && entity.getPersistentDataContainer().has(dummyKey, PersistentDataType.BYTE); }
  private void ensureDummies() {
    World world = arenaWorld();
    if (world == null) return;
    var positions = getConfig().getConfigurationSection("dummy.positions");
    if (positions == null) return;
    for (String name : positions.getKeys(false)) {
      Location point = point("dummy.positions." + name, world);
      world.getChunkAt(point).load();
      if (!point.clone().add(0, -1, 0).getBlock().getType().isSolid()) continue;
      double rx = getConfig().getDouble("dummy.search-radius-xz"), ry = getConfig().getDouble("dummy.search-radius-y");
      boolean found = world.getNearbyEntities(point, rx, ry, rx).stream().anyMatch(this::isDummy);
      if (found) continue;
      try {
        var mob = MythicBukkit.inst().getMobManager().spawnMob(getConfig().getString("dummy.mob-id"), point);
        Entity entity = mob.getEntity().getBukkitEntity();
        entity.getPersistentDataContainer().set(dummyKey, PersistentDataType.BYTE, (byte) 1);
        if (entity instanceof LivingEntity living) { living.setAI(false); living.setSilent(true); living.setRemoveWhenFarAway(false); living.setCollidable(false); }
      } catch (Exception exception) { getLogger().warning("Nie udało się przywołać manekina: " + exception.getMessage()); }
    }
    for (Entity entity : world.getEntities()) if (isDummy(entity)) {
      double max = getConfig().getDouble("dummy.max-displacement");
      boolean atStation = positions.getKeys(false).stream().anyMatch(name -> entity.getLocation().distanceSquared(point("dummy.positions." + name, world)) < max * max);
      if (!atStation) { entity.remove(); continue; }
      entity.setVelocity(new Vector());
      if (entity.getFireTicks() > getConfig().getInt("dummy.max-fire-seconds") * 20) entity.setFireTicks(0);
    }
  }
  private void cleanNumbers() {
    World world = arenaWorld();
    if (world != null) for (TextDisplay e : world.getEntitiesByClass(TextDisplay.class)) if (e.getPersistentDataContainer().has(numberKey, PersistentDataType.BYTE)) e.remove();
  }
  @EventHandler public void onBurn(EntityCombustEvent event) { if (isDummy(event.getEntity())) event.setDuration(Math.min(event.getDuration(), getConfig().getInt("dummy.max-fire-seconds"))); }
  @EventHandler public void onMobDeath(EntityDeathEvent event) { if (isDummy(event.getEntity())) { event.getDrops().clear(); event.setDroppedExp(0); } }
  @EventHandler public void onBreak(BlockBreakEvent event) { if (arena(event.getBlock().getLocation())) event.setCancelled(true); }
  @EventHandler public void onPlace(BlockPlaceEvent event) { if (arena(event.getBlock().getLocation())) event.setCancelled(true); }
  @EventHandler public void onDrop(PlayerDropItemEvent event) { if (arena(event.getPlayer().getLocation())) event.setCancelled(true); }
  @EventHandler public void onPickup(org.bukkit.event.entity.EntityPickupItemEvent event) { if (arena(event.getEntity().getLocation())) event.setCancelled(true); }
  @EventHandler public void onBucketEmpty(PlayerBucketEmptyEvent event) { if (arena(event.getBlock().getLocation())) event.setCancelled(true); }
  @EventHandler public void onBucketFill(PlayerBucketFillEvent event) { if (arena(event.getBlock().getLocation())) event.setCancelled(true); }
  @EventHandler public void onHanging(HangingPlaceEvent event) { if (arena(event.getEntity().getLocation())) event.setCancelled(true); }
  @EventHandler public void onVehicle(VehicleCreateEvent event) { if (arena(event.getVehicle().getLocation())) event.setCancelled(true); }
  @EventHandler public void onExplode(EntityExplodeEvent event) { if (arena(event.getLocation())) { event.blockList().clear(); event.setYield(0); } }
  @EventHandler public void onInteractEntity(PlayerInteractEntityEvent event) { if (isDummy(event.getRightClicked())) event.setCancelled(true); }
  @EventHandler public void onLeash(org.bukkit.event.entity.PlayerLeashEntityEvent event) { if (isDummy(event.getEntity())) event.setCancelled(true); }
  @EventHandler public void onEntityPlace(org.bukkit.event.entity.EntityPlaceEvent event) { if (arena(event.getEntity().getLocation())) event.setCancelled(true); }
  @EventHandler public void onSplash(org.bukkit.event.entity.PotionSplashEvent event) {
    Player source = attacker(event.getPotion());
    for (LivingEntity entity : event.getAffectedEntities()) if (entity instanceof Player target && (arena(target.getLocation()) || source != null && arena(source.getLocation()))) {
      if (source == null || !inRing(source.getLocation()) || !inRing(target.getLocation())) event.setIntensity(entity, 0);
    }
  }
  @EventHandler public void onCloud(org.bukkit.event.entity.AreaEffectCloudApplyEvent event) {
    if (arena(event.getEntity().getLocation())) event.getAffectedEntities().removeIf(entity -> entity instanceof Player target && !inRing(target.getLocation()));
  }
  @EventHandler public void onLingering(org.bukkit.event.entity.LingeringPotionSplashEvent event) {
    Player source = attacker(event.getEntity());
    if (arena(event.getEntity().getLocation()) && (source == null || !inRing(source.getLocation()))) event.setCancelled(true);
  }

  private void loadState() {
    if (!stateFile.isFile()) return;
    YamlConfiguration y = YamlConfiguration.loadConfiguration(stateFile);
    if (y.getConfigurationSection("returns") != null) for (String key : y.getConfigurationSection("returns").getKeys(false)) try {
      UUID id = UUID.fromString(key), ignored = id;
      String p = "returns." + key;
      returns.put(id, new ReturnPoint(y.getString(p + ".world", ""), y.getDouble(p + ".x"), y.getDouble(p + ".y"), y.getDouble(p + ".z"), (float) y.getDouble(p + ".yaw"), (float) y.getDouble(p + ".pitch"), y.getString(p + ".profile", "")));
    } catch (IllegalArgumentException ignored) { getLogger().warning("Nieprawidłowy wpis powrotu: " + key); }
    if (y.getConfigurationSection("forfeit") != null) for (String key : y.getConfigurationSection("forfeit").getKeys(false)) try { forfeitUntil.put(UUID.fromString(key), y.getLong("forfeit." + key)); } catch (IllegalArgumentException ignored) { getLogger().warning("Nieprawidłowy wpis poddania: " + key); }
  }
  private void saveState() {
    YamlConfiguration y = new YamlConfiguration();
    for (var e : returns.entrySet()) {
      String p = "returns." + e.getKey(); ReturnPoint r = e.getValue();
      y.set(p + ".world", r.world); y.set(p + ".x", r.x); y.set(p + ".y", r.y); y.set(p + ".z", r.z); y.set(p + ".yaw", r.yaw); y.set(p + ".pitch", r.pitch); y.set(p + ".profile", r.profile);
    }
    for (var e : forfeitUntil.entrySet()) if (e.getValue() > System.currentTimeMillis()) y.set("forfeit." + e.getKey(), e.getValue());
    File temporary = new File(getDataFolder(), "returns.yml.tmp");
    try {
      y.save(temporary);
      try { java.nio.file.Files.move(temporary.toPath(), stateFile.toPath(), java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
      catch (java.nio.file.AtomicMoveNotSupportedException ignored) { java.nio.file.Files.move(temporary.toPath(), stateFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
    } catch (IOException error) { getLogger().severe("Nie udało się zapisać powrotów: " + error.getMessage()); }
  }
}
