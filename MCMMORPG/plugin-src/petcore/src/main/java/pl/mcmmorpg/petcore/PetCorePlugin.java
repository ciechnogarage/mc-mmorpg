package pl.mcmmorpg.petcore;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.bukkit.events.MythicMobDespawnEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import com.mojang.brigadier.arguments.StringArgumentType;

/** PetCore platform shell plus the single Global Pet vertical slice. */
public final class PetCorePlugin extends JavaPlugin implements Listener {
  private final AtomicLong ticks = new AtomicLong();
  private PetCoreRuntime runtime;
  private GlobalPetRuntime globalPets;
  private ProfileSessionRegistry sessions;
 private PetSubjectProvider subjects;
 private PetSubjectCompatibility subjectCompatibility;
  private PetCoreStore store;
  private AuthorityState authority = AuthorityState.UNAVAILABLE;
  private PetDuelService duels;
  private PetCoreUiStatusService uiStatus;

  @Override public void onEnable() {
    saveDefaultConfig();
 sessions = new ProfileSessionRegistry();
 subjects = new MMOProfilesSubjectProvider();
 subjectCompatibility = new UnavailablePetSubjectCompatibility(
     "domain-02 PetCore read-through adapter not configured");
 uiStatus = new PetCoreUiStatusService();
    var recalls = new PluginRecallSink();
    this.runtime = new PetCoreRuntime(new FailClosedGate(recalls));
    try {
      if (getConfig().getBoolean("database.enabled", false)) {
        store = new JdbcPetCoreStore(
            getConfig().getString("database.jdbc-url", ""),
            getConfig().getString("database.username", ""),
            getConfig().getString("database.password", ""),
            getConfig().getString("database.table-prefix", "petcore_"));
        authority = AuthorityState.HEALTHY;
        getLogger().info("PetCore durable kernel initialized; Global Pet admission is available only with a verified content mode.");
      } else {
        getLogger().warning("PetCore durable storage is disabled: Global Pet actions remain fail-closed.");
      }
    } catch (DurableAuthorityException | IllegalArgumentException exception) {
      authority = AuthorityState.UNAVAILABLE;
      Throwable cause = exception.getCause();
      String detail = cause == null ? exception.getMessage()
          : exception.getMessage() + " (" + cause.getClass().getSimpleName() + ": " + cause.getMessage() + ")";
      getLogger().severe("PetCore durable authority unavailable; actions are fail-closed: " + detail);
    }
    ContentRunProjectionService runs = new ContentRunProjectionService(
        store == null ? new InMemoryPetCoreStore() : store, () -> java.time.Instant.now());
    ContentEligibilityService eligibility = new ContentEligibilityService(
        new ConfiguredContentEligibilityProvider(this::configuredContent), runs);
    var telemetry = new PetCoreTelemetry();
    var admission = new PetAdmissionService(new CapacityLimits(1, 20, 20, 20, 20, 4), 20, telemetry);
 globalPets = new GlobalPetRuntime(
     admission,
     new ActiveLeaseRegistry(),
     new FailClosedGate(recalls),
     new MythicMobsAdapter(this),
     sessions,
     subjects,
     subjectCompatibility,
     eligibility,
     () -> authority);
    duels = new PetDuelService(globalPets, 200, 20);
    getServer().getPluginManager().registerEvents(this, this);
    getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
        event -> event.registrar().register(Commands.literal("petcore")
            .requires(source -> source.getSender().hasPermission("petcore.use"))
            .executes(context -> { command(context.getSource().getSender(), null, "petcore", new String[0]); return 1; })
            .then(Commands.argument("args", StringArgumentType.greedyString())
                .executes(context -> {
                  String raw = StringArgumentType.getString(context, "args").trim();
                  command(context.getSource().getSender(), null, "petcore", raw.isEmpty() ? new String[0] : raw.split("\\s+"));
                  return 1;
                }))
            .build()));
    getServer().getScheduler().runTaskTimer(this, () -> {
      long tick = ticks.incrementAndGet();
      runtime.tick(tick);
      if (globalPets != null) globalPets.tick(tick);
      if (duels != null) duels.tick(tick);
    }, 1L, 1L);
    registerPlaceholderExpansion();
    getLogger().info("PetCore loaded; MythicMobs adapter=" + (Bukkit.getPluginManager().isPluginEnabled("MythicMobs") ? "ready" : "unavailable")
        + ", MMOProfiles subject seam=" + (Bukkit.getPluginManager().isPluginEnabled("MMOProfiles") ? "ready" : "unavailable"));
  }

  @Override public void onDisable() {
    authority = AuthorityState.UNAVAILABLE;
    if (globalPets != null) for (ActiveLease lease : globalPets.activeLeases()) globalPets.recall(lease, "plugin disable");
  }

  private boolean command(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command,
                          String label, String[] args) {
    if (!(sender instanceof Player player)) { sender.sendMessage("PetCore commands require a player."); return true; }
    if (!player.hasPermission("petcore.use")) { player.sendMessage("PetCore is unavailable for this account."); return true; }
    if (args.length == 0) { player.sendMessage("Usage: /petcore summon|recall|follow|clear|skill|duel|status"); return true; }
    ActiveLease active = activeFor(player.getUniqueId()).orElse(null);
    switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
      case "summon" -> {
        Optional<PetSubjectId> subject = subjects.resolve(player);
        if (subject.isEmpty()) { player.sendMessage("Global Pet unavailable: active MMOProfiles character is not verified."); return true; }
        PetSubjectCompatibility.CompatibilityResolution compatibility =
            PetSubjectCompatibility.CompatibilityResolution.safeResolve(subjectCompatibility, subject.orElseThrow());
        if (!compatibility.allowsPhaseOneReadThrough()) { player.sendMessage("Global Pet unavailable: domain-02 character mapping is not verified."); return true; }
        PlayerSession session = sessions.bind(player.getUniqueId(), subject.orElseThrow());
        if (store == null || authority != AuthorityState.HEALTHY) { player.sendMessage("Global Pet unavailable: durable PetCore authority is offline."); return true; }
        if (!ensureProfileForSummon(
            store,
            session,
            "petcore-profile-" + player.getUniqueId() + "-" + session.epoch())) {
          authority = AuthorityState.UNAVAILABLE;
          player.sendMessage("Global Pet unavailable: durable PetCore authority is offline.");
          return true;
        }
        GlobalPetSummonResult result = globalPets.summon(session, player.getUniqueId(), GlobalPetDefinitions.ID,
            player.getLocation(), authority, "petcore-summon-" + player.getUniqueId() + "-" + session.epoch(), ticks.get());
        player.sendMessage(result.accepted() ? "Global Pet summoned." : "Global Pet summon denied: " + result.reason());
      }
      case "recall" -> { if (active == null) player.sendMessage("No Global Pet is active."); else { globalPets.recall(active, "player recall"); player.sendMessage("Global Pet recalled."); } }
      case "follow" -> { if (active == null) player.sendMessage("No Global Pet is active."); else { globalPets.clearTarget(active); player.sendMessage("Global Pet is following."); } }
      case "clear" -> { if (active != null) globalPets.clearTarget(active); player.sendMessage("Global Pet target cleared."); }
      case "skill" -> {
        if (active == null || args.length < 2 || !globalPets.invokeSkill(active, args[1])) player.sendMessage("Global Pet skill denied.");
        else player.sendMessage("Global Pet skill invoked.");
      }
      case "duel" -> handleDuelCommand(player, args);
      case "status" -> {
        String field = args.length > 1 ? args[1] : "summary";
        player.sendMessage("PetCore status " + field + ": " + uiStatusValue(player, field));
      }
      default -> player.sendMessage("Usage: /petcore summon|recall|follow|clear|skill|duel|status");
    }
    return true;
  }

  static boolean ensureProfileForSummon(PetCoreStore store, PlayerSession session, String operationId) {
    if (store == null || session == null || operationId == null || operationId.isBlank()) return false;
    try {
      return store.ensureProfile(session.subjectId(), operationId) != null;
    } catch (RuntimeException unavailable) {
      return false;
    }
  }

  @EventHandler public void onQuit(PlayerQuitEvent event) {
    if (duels != null) duels.endForOwner(event.getPlayer().getUniqueId(), "owner logout");
    if (globalPets != null) globalPets.recallOwner(event.getPlayer().getUniqueId(), "owner logout");
  }

  @EventHandler public void onWorldChange(PlayerChangedWorldEvent event) {
    if (duels != null) duels.endForOwner(event.getPlayer().getUniqueId(), "world change");
    if (globalPets != null) globalPets.recallOwner(event.getPlayer().getUniqueId(), "world change");
  }

  @EventHandler public void onPetDuelDamage(EntityDamageByEntityEvent event) {
    if (duels == null) return;
    UUID source = event.getDamager().getUniqueId(), target = event.getEntity().getUniqueId();
    if ((duels.isDuelPet(source) || duels.isDuelPet(target)) && !duels.canDamage(source, target)) {
      event.setCancelled(true);
    }
  }

  @EventHandler public void onPetDuelPetEnvironmentDamage(EntityDamageEvent event) {
    if (duels == null || event instanceof EntityDamageByEntityEvent) return;
    if (duels.isDuelPet(event.getEntity().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler public void onPetDuelPlayerDamageDiagnostics(EntityDamageEvent event) {
    if (!getConfig().getBoolean("debug.duel-damage", false)) return;
    if (duels == null || duels.sessions().isEmpty() || !(event.getEntity() instanceof Player player)) return;
    String damager = "none";
    boolean damagerDuelPet = false;
    if (event instanceof EntityDamageByEntityEvent byEntity) {
      Entity entity = byEntity.getDamager();
      damager = entity.getType() + "/" + entity.getUniqueId();
      damagerDuelPet = duels.isDuelPet(entity.getUniqueId());
    }
    getLogger().info("PetDuel damage diagnostic victim=" + player.getName()
        + " cause=" + event.getCause()
        + " damage=" + event.getFinalDamage()
        + " cancelled=" + event.isCancelled()
        + " damager=" + damager
        + " damagerDuelPet=" + damagerDuelPet
        + " victimDuelPet=" + duels.isDuelPet(player.getUniqueId()));
  }

  private void handleDuelCommand(Player player, String[] args) {
    if (args.length < 2) { player.sendMessage("Usage: /petcore duel request <player>|accept|decline|skill <id>|end"); return; }
    UUID owner = player.getUniqueId();
    switch (args[1].toLowerCase(java.util.Locale.ROOT)) {
      case "request" -> {
        Player target = args.length > 2 ? Bukkit.getPlayerExact(args[2]) : null;
        PetDuelResult result = target == null ? PetDuelResult.INELIGIBLE : duels.request(owner, target.getUniqueId(), ticks.get());
        player.sendMessage("Pet Duel request: " + result);
        if (target != null && result == PetDuelResult.ACCEPTED) target.sendMessage("Pet Duel request received. Use /petcore duel accept or decline.");
      }
      case "accept", "decline" -> {
        Optional<PetDuelRequest> pending = duels.pendingFor(owner, ticks.get());
        PetDuelResult result = pending.isEmpty() ? PetDuelResult.NOT_FOUND
            : (args[1].equalsIgnoreCase("accept") ? duels.accept(owner, pending.get().requestId(), ticks.get()) : duels.decline(owner, pending.get().requestId()));
        player.sendMessage("Pet Duel " + args[1] + ": " + result);
      }
      case "skill" -> player.sendMessage(duels.commandSkill(owner, args.length > 2 ? args[2] : "", ticks.get()) ? "Pet Duel skill invoked." : "Pet Duel skill denied.");
      case "end" -> { duels.endForOwner(owner, "owner ended duel"); player.sendMessage("Pet Duel ended."); }
      default -> player.sendMessage("Usage: /petcore duel request <player>|accept|decline|skill <id>|end");
    }
  }

  @EventHandler public void onMythicDeath(MythicMobDeathEvent event) {
    if (globalPets == null || event.getEntity() == null) return;
    if (duels != null) duels.endForPetEntity(event.getEntity().getUniqueId(), "duel pet death");
    globalPets.activeLeases().stream()
        .filter(lease -> lease.entityId() != null && lease.entityId().equals(event.getEntity().getUniqueId()))
        .findFirst().ifPresent(lease -> globalPets.handleCallback(lease, "MythicMobs death callback"));
  }

  @EventHandler public void onMythicDespawn(MythicMobDespawnEvent event) {
    if (globalPets == null || event.getEntity() == null) return;
    if (duels != null) duels.endForPetEntity(event.getEntity().getUniqueId(), "duel pet despawn");
    globalPets.activeLeases().stream()
        .filter(lease -> lease.entityId() != null && lease.entityId().equals(event.getEntity().getUniqueId()))
        .findFirst().ifPresent(lease -> globalPets.handleCallback(lease, "MythicMobs despawn callback"));
  }

  private Optional<ActiveLease> activeFor(UUID ownerId) {
    return globalPets.activeLeases().stream().filter(lease -> lease.ownerPlayerId().equals(ownerId)).findFirst();
  }

  String uiStatusValue(Player player, String field) {
    return uiSnapshot(player).value(field == null || field.isBlank() ? "summary" : field);
  }

  PetCoreUiSnapshot uiSnapshot(Player player) {
    Optional<PetSubjectId> subject = subjects == null ? Optional.empty() : subjects.resolve(player);
    Optional<PetSubjectCompatibility.CompatibilityResolution> compatibility =
        subject.isEmpty() || subjectCompatibility == null
            ? Optional.empty()
            : Optional.of(PetSubjectCompatibility.CompatibilityResolution.safeResolve(
                subjectCompatibility, subject.orElseThrow()));
    Optional<ActiveLease> active = globalPets == null ? Optional.empty() : activeFor(player.getUniqueId());
    boolean ownerInDuel = duels != null && duels.isOwnerInDuel(player.getUniqueId());
    return uiStatus.snapshot(subject, compatibility, authority, configuredContent(), active, ownerInDuel);
  }

  private void registerPlaceholderExpansion() {
    if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      getLogger().warning("PetCore PlaceholderAPI expansion skipped: PlaceholderAPI unavailable.");
      return;
    }
    try {
      PetCorePlaceholderRegistrationResult result =
          PetCorePlaceholderRegistrationResult.from(new PetCorePlaceholderExpansion(this).register());
      if (result.registered()) {
        getLogger().info(result.logMessage());
      } else {
        getLogger().warning(result.logMessage());
      }
    } catch (RuntimeException failure) {
      getLogger().warning("PetCore PlaceholderAPI expansion unavailable: " + failure.getClass().getSimpleName());
    }
  }

  private ContentResolution configuredContent() {
    if (!getConfig().getBoolean("content.staging-enabled", false)) return ContentResolution.unknown();
    try {
      ContentMode mode = ContentMode.valueOf(getConfig().getString("content.mode", "UNKNOWN").toUpperCase(java.util.Locale.ROOT));
      if (mode == ContentMode.SAFE || mode == ContentMode.OPEN_PVE) {
        return ContentResolution.resolved(mode, Optional.empty(), getConfig().getInt("content.party-size", 1),
            getConfig().getLong("content.revision", 1));
      }
    } catch (IllegalArgumentException ignored) { }
    return ContentResolution.unknown();
  }

  private final class PluginRecallSink implements RecallSink {
    @Override public void recall(ActiveLease lease, String status) {
      getLogger().warning("PetCore fenced lease " + lease.petInstanceId() + " epoch "
          + lease.runtimeEpoch() + ": " + status);
    }
  }
}
