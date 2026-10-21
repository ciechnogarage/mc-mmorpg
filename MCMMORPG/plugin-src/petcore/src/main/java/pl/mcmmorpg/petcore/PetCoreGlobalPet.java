package pl.mcmmorpg.petcore;

import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.core.mobs.ActiveMob;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** One deliberately small, vanilla-backed Global Pet definition. */
final class GlobalPetDefinitions {
  static final String ID = "global_pet.wolf.t1";
  static final String MYTHIC_ID = "PetCore_GlobalPet_T1";
  static final String ALLOW_LISTED_SKILL = "PetCore_GlobalPetPulse";
  static final PetDefinition GLOBAL_WOLF = new PetDefinition(
      ID, MYTHIC_ID, CompanionPolicy.GLOBAL_PET, Set.of(PetCapability.FOLLOW, PetCapability.TARGET,
          PetCapability.ALLOW_LISTED_SKILL), Set.of(ALLOW_LISTED_SKILL));

  private GlobalPetDefinitions() {}
}

enum PetCapability { FOLLOW, TARGET, ALLOW_LISTED_SKILL }

record PetDefinition(String id, String mythicMobId, CompanionPolicy policy,
                     Set<PetCapability> capabilities, Set<String> allowListedSkills) {
  PetDefinition {
    if (id == null || id.isBlank() || mythicMobId == null || mythicMobId.isBlank())
      throw new IllegalArgumentException("pet definition IDs are required");
    capabilities = Set.copyOf(capabilities);
    allowListedSkills = Set.copyOf(allowListedSkills);
  }
  boolean allows(PetCapability capability) { return capabilities.contains(capability); }
  boolean allowsSkill(String skill) { return allowListedSkills.contains(skill); }
}

record PetSpawnResult(boolean spawned, Optional<UUID> entityId, String reason) {
  static PetSpawnResult success(UUID entityId) { return new PetSpawnResult(true, Optional.of(entityId), "spawned"); }
  static PetSpawnResult failed(String reason) { return new PetSpawnResult(false, Optional.empty(), reason); }
}

enum FollowAction { NO_OP, NORMAL_FOLLOW, CATCH_UP_REPATH, SAFE_TELEPORT, REJECTED }

record FollowResult(FollowAction action, String reason) {}

/**
 * Narrow vendor seam. PetCore owns decisions; this interface owns only the
 * physical MythicMobs operation and never schedules work itself.
 */
interface PetExecutorAdapter {
  PetSpawnResult spawn(PetDefinition definition, ActiveLease lease, UUID ownerId, Location location);
  boolean setTarget(ActiveLease lease, UUID targetId);
  boolean clearTarget(ActiveLease lease);
  FollowResult follow(ActiveLease lease, UUID ownerId);
  boolean invokeSkill(ActiveLease lease, String skillId);
  boolean despawn(ActiveLease lease);
  boolean despawnTaggedEntity(UUID entityId, ActiveLease expectedLease);
  void reconcileTaggedEntities(Collection<ActiveLease> currentLeases);
}

/** Real MythicMobs 5.11.2 adapter, using only the verified public API. */
final class MythicMobsAdapter implements PetExecutorAdapter {
  private final NamespacedKey petInstanceKey;
  private final NamespacedKey runtimeEpochKey;
  private final NamespacedKey subjectKey;
  private final NamespacedKey ownerKey;
  private final NamespacedKey policyKey;
  private final NamespacedKey definitionKey;

  MythicMobsAdapter(JavaPlugin plugin) {
    petInstanceKey = new NamespacedKey(plugin, "pet_instance_id");
    runtimeEpochKey = new NamespacedKey(plugin, "runtime_epoch");
    subjectKey = new NamespacedKey(plugin, "pet_subject_id");
    ownerKey = new NamespacedKey(plugin, "owner_id");
    policyKey = new NamespacedKey(plugin, "policy");
    definitionKey = new NamespacedKey(plugin, "definition_id");
  }

  @Override public PetSpawnResult spawn(PetDefinition definition, ActiveLease lease, UUID ownerId, Location location) {
    if (!mythicReady()) return PetSpawnResult.failed("MythicMobs unavailable");
    if (location == null || location.getWorld() == null) return PetSpawnResult.failed("spawn location unavailable");
    Player owner = Bukkit.getPlayer(ownerId);
    try {
      BukkitAPIHelper helper = MythicBukkit.inst().getAPIHelper();
      Entity entity = helper.spawnMythicMob(definition.mythicMobId(), location.clone());
      if (entity == null || entity.isDead()) return PetSpawnResult.failed("MythicMobs returned no live entity");
      writeTags(entity, definition, lease);
      entity.addScoreboardTag("petcore.pet");
      entity.addScoreboardTag("petcore.global");
      entity.setPersistent(false);
      entity.setSilent(false);
      entity.setGravity(true);
      if (entity instanceof LivingEntity living) living.setCollidable(false);
      if (entity instanceof Wolf wolf && owner != null) {
        wolf.setTamed(true);
        wolf.setOwner(owner);
        wolf.setSitting(false);
      }
      return PetSpawnResult.success(entity.getUniqueId());
    } catch (Exception failure) {
      return PetSpawnResult.failed("MythicMobs spawn failed: " + failure.getClass().getSimpleName());
    }
  }

  @Override public boolean setTarget(ActiveLease lease, UUID targetId) {
    Entity entity = liveEntity(lease.entityId());
    Entity target = targetId == null ? null : Bukkit.getEntity(targetId);
    if (entity == null || target == null || !(target instanceof LivingEntity) || target.isDead()) return false;
    ActiveMob active = activeMob(entity);
    if (active == null) return false;
    AbstractEntity adapted = BukkitAdapter.adapt(target);
    active.setTarget(adapted);
    return true;
  }

  @Override public boolean clearTarget(ActiveLease lease) {
    Entity entity = liveEntity(lease.entityId());
    ActiveMob active = entity == null ? null : activeMob(entity);
    if (active == null) return false;
    active.resetTarget();
    return true;
  }

  @Override public FollowResult follow(ActiveLease lease, UUID ownerId) {
    Entity entity = liveEntity(lease.entityId());
    Player owner = Bukkit.getPlayer(ownerId);
    if (entity == null || owner == null || owner.getWorld() == null) return new FollowResult(FollowAction.REJECTED, "owner/entity offline");
    if (!entity.getWorld().equals(owner.getWorld())) return new FollowResult(FollowAction.REJECTED, "world mismatch");
    double distanceSquared = entity.getLocation().distanceSquared(owner.getLocation());
    if (distanceSquared <= 9) return new FollowResult(FollowAction.NO_OP, "within follow deadband");
    if (entity instanceof Wolf wolf) {
      wolf.setSitting(false);
      wolf.setAI(true);
    }
    if (distanceSquared <= 144) return new FollowResult(FollowAction.NORMAL_FOLLOW, "vanilla owner pathfinding");
    if (distanceSquared <= 900) return new FollowResult(FollowAction.CATCH_UP_REPATH, "vanilla owner pathfinding catch-up");
    Location safe = safeTeleportLocation(owner.getLocation());
    if (safe == null) return new FollowResult(FollowAction.REJECTED, "no safe same-world teleport location");
    if (!entity.teleport(safe)) return new FollowResult(FollowAction.REJECTED, "teleport rejected");
    return new FollowResult(FollowAction.SAFE_TELEPORT, "distance threshold exceeded");
  }

  @Override public boolean invokeSkill(ActiveLease lease, String skillId) {
    Entity entity = liveEntity(lease.entityId());
    if (entity == null || !mythicReady()) return false;
    if (MythicBukkit.inst().getSkillManager().getSkill(skillId).isEmpty()) return false;
    return MythicBukkit.inst().getAPIHelper().castSkill(entity, skillId);
  }

  @Override public boolean despawn(ActiveLease lease) {
    Entity entity = liveEntity(lease.entityId());
    if (entity == null) return true;
    if (!matches(entity, lease)) return false;
    ActiveMob active = activeMob(entity);
    if (active != null) active.despawn(); else entity.remove();
    return true;
  }

  @Override public boolean despawnTaggedEntity(UUID entityId, ActiveLease expectedLease) {
    Entity entity = liveEntity(entityId);
    if (entity == null || !matches(entity, expectedLease)) return entity == null;
    ActiveMob active = activeMob(entity);
    if (active != null) active.despawn(); else entity.remove();
    return true;
  }

  @Override public void reconcileTaggedEntities(Collection<ActiveLease> currentLeases) {
    Map<UUID, ActiveLease> expected = new ConcurrentHashMap<>();
    for (ActiveLease lease : currentLeases) expected.put(lease.entityId(), lease);
    for (World world : Bukkit.getWorlds()) {
      for (Entity entity : world.getEntities()) {
        UUID petId = entity.getPersistentDataContainer().get(petInstanceKey, PersistentDataType.STRING) == null
            ? null : parseUuid(entity.getPersistentDataContainer().get(petInstanceKey, PersistentDataType.STRING));
        if (petId == null) continue;
        ActiveLease lease = expected.get(entity.getUniqueId());
        if (lease == null || !matches(entity, lease)) entity.remove();
      }
    }
  }

  private void writeTags(Entity entity, PetDefinition definition, ActiveLease lease) {
    var data = entity.getPersistentDataContainer();
    data.set(petInstanceKey, PersistentDataType.STRING, lease.petInstanceId().toString());
    data.set(runtimeEpochKey, PersistentDataType.LONG, lease.runtimeEpoch());
    data.set(subjectKey, PersistentDataType.STRING, lease.subjectId().value());
    data.set(ownerKey, PersistentDataType.STRING, lease.ownerPlayerId().toString());
    data.set(policyKey, PersistentDataType.STRING, CompanionPolicy.GLOBAL_PET.name());
    data.set(definitionKey, PersistentDataType.STRING, definition.id());
  }

  private boolean matches(Entity entity, ActiveLease lease) {
    var data = entity.getPersistentDataContainer();
    String petId = data.get(petInstanceKey, PersistentDataType.STRING);
    Long epoch = data.get(runtimeEpochKey, PersistentDataType.LONG);
    String subject = data.get(subjectKey, PersistentDataType.STRING);
    String owner = data.get(ownerKey, PersistentDataType.STRING);
    return lease.entityId().equals(entity.getUniqueId()) && lease.petInstanceId().toString().equals(petId)
        && lease.runtimeEpoch() == (epoch == null ? Long.MIN_VALUE : epoch)
        && lease.subjectId().value().equals(subject) && lease.ownerPlayerId().toString().equals(owner);
  }

  private Entity liveEntity(UUID id) {
    Entity entity = id == null ? null : Bukkit.getEntity(id);
    return entity == null || !entity.isValid() || entity.isDead() ? null : entity;
  }

  private ActiveMob activeMob(Entity entity) {
    return mythicReady() ? MythicBukkit.inst().getAPIHelper().getMythicMobInstance(entity) : null;
  }

  private boolean mythicReady() {
    return Bukkit.getPluginManager().isPluginEnabled("MythicMobs") && MythicBukkit.inst() != null;
  }

  private static UUID parseUuid(String value) {
    try { return UUID.fromString(value); } catch (IllegalArgumentException ignored) { return null; }
  }

  private static Location safeTeleportLocation(Location owner) {
    for (int[] offset : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
      Location candidate = owner.clone().add(offset[0], 0, offset[1]);
      Block ground = candidate.clone().subtract(0, 1, 0).getBlock();
      Block feet = candidate.getBlock();
      Block head = candidate.clone().add(0, 1, 0).getBlock();
      if (!ground.getType().isSolid() || !feet.isPassable() || !head.isPassable()) continue;
      if (ground.getType().name().contains("LAVA") || feet.getType().name().contains("LAVA")) continue;
      return candidate;
    }
    return null;
  }
}

interface PetSubjectProvider { Optional<PetSubjectId> resolve(Player player); }

/** Verified MMOProfiles 1.2 seam; unavailable profile state is fail-closed. */
final class MMOProfilesSubjectProvider implements PetSubjectProvider {
  @Override public Optional<PetSubjectId> resolve(Player player) {
    if (player == null || !Bukkit.getPluginManager().isPluginEnabled("MMOProfiles")) return Optional.empty();
    try {
      fr.phoenixdevt.mmoprofiles.bukkit.profile.ProfileListImpl profiles =
          fr.phoenixdevt.mmoprofiles.bukkit.profile.ProfileListImpl.getOrNull(player);
      if (profiles == null || profiles.getCurrent() == null) return Optional.empty();
      return Optional.of(PetSubjectId.of(profiles.getCurrent().getUniqueId().toString()));
    } catch (RuntimeException unavailable) {
      return Optional.empty();
    }
  }
}

final class ConfiguredContentEligibilityProvider implements ContentEligibilityProvider {
  private final java.util.function.Supplier<ContentResolution> state;
  ConfiguredContentEligibilityProvider(java.util.function.Supplier<ContentResolution> state) { this.state = state; }
  @Override public ContentResolution resolve(PetSubjectId subject, PlayerSession session) {
    return state.get();
  }
}

record GlobalPetSummonResult(boolean accepted, Optional<ActiveLease> lease, String reason) {
  static GlobalPetSummonResult accepted(ActiveLease lease) { return new GlobalPetSummonResult(true, Optional.of(lease), "summoned"); }
  static GlobalPetSummonResult rejected(String reason) { return new GlobalPetSummonResult(false, Optional.empty(), reason); }
}

/** Central, single-tick owner of all Global Pet lifecycle and optional work. */
final class GlobalPetRuntime {
  private final PetAdmissionService admission;
  private final ActiveLeaseRegistry leases;
  private final FailClosedGate gate;
  private final PetExecutorAdapter adapter;
  private final ProfileSessionRegistry sessions;
  private final PetSubjectProvider subjects;
  private final PetSubjectCompatibility compatibility;
  private final ContentEligibilityService content;
  private final Supplier<AuthorityState> authority;

  GlobalPetRuntime(PetAdmissionService admission, ActiveLeaseRegistry leases, FailClosedGate gate,
                   PetExecutorAdapter adapter, ProfileSessionRegistry sessions,
                   PetSubjectProvider subjects, PetSubjectCompatibility compatibility, ContentEligibilityService content) {
    this(admission, leases, gate, adapter, sessions, subjects, compatibility, content, () -> AuthorityState.HEALTHY);
  }

  GlobalPetRuntime(PetAdmissionService admission, ActiveLeaseRegistry leases, FailClosedGate gate,
                   PetExecutorAdapter adapter, ProfileSessionRegistry sessions,
                   PetSubjectProvider subjects, PetSubjectCompatibility compatibility, ContentEligibilityService content,
                   Supplier<AuthorityState> authority) {
    this.admission = admission; this.leases = leases; this.gate = gate; this.adapter = adapter;
    this.sessions = sessions; this.subjects = subjects; this.compatibility = compatibility; this.content = content;
    this.authority = authority == null ? (() -> AuthorityState.UNAVAILABLE) : authority;
  }

  GlobalPetSummonResult summon(PlayerSession session, UUID ownerId, String definitionId, Location location,
                               AuthorityState authority, String requestId, long tick) {
    if (session == null || ownerId == null || location == null) return GlobalPetSummonResult.rejected("invalid summon request");
    if (!sessions.isCurrent(session)) return GlobalPetSummonResult.rejected("stale player session");
    if (!PetSubjectCompatibility.CompatibilityResolution.safeResolve(compatibility, session.subjectId())
        .allowsPhaseOneReadThrough()) return GlobalPetSummonResult.rejected("domain-02 character mapping is not verified");
    if (!gate.allowNewAction(authority, content.resolve(session.subjectId(), session))) return GlobalPetSummonResult.rejected("content/authority unavailable");
    PetDefinition definition = definition(definitionId);
    if (definition == null || definition.policy() != CompanionPolicy.GLOBAL_PET) return GlobalPetSummonResult.rejected("unknown Global Pet definition");
    AdmissionContext context = new AdmissionContext(session.subjectId(), CompanionPolicy.GLOBAL_PET,
        location.getWorld() == null ? "unknown" : location.getWorld().getName(), Optional.empty());
    AdmissionDecision reservation = admission.reserve(context, requestId, tick);
    if (!reservation.accepted()) return GlobalPetSummonResult.rejected("admission rejected: " + reservation.rejection().orElseThrow());
    UUID petInstanceId = reservation.reservationId().orElseThrow();
    ActiveLease lease = new ActiveLease(petInstanceId, session.epoch(), session.subjectId(), ownerId);
    PetSpawnResult spawn = adapter.spawn(definition, lease, ownerId, location);
    if (!spawn.spawned() || spawn.entityId().isEmpty()) { admission.release(petInstanceId); return GlobalPetSummonResult.rejected(spawn.reason()); }
    UUID entityId = spawn.entityId().orElseThrow();
    if (!admission.commit(petInstanceId, tick)) {
      adapter.despawnTaggedEntity(entityId, lease);
      return GlobalPetSummonResult.rejected("reservation commit failed");
    }
    ActiveLease committed = new ActiveLease(petInstanceId, session.epoch(), session.subjectId(), ownerId, entityId);
    leases.activate(committed);
    return GlobalPetSummonResult.accepted(committed);
  }

  boolean setTarget(ActiveLease lease, UUID targetId) {
    if (!guardAction(lease, "stale profile session before target" ) || !validTarget(lease, targetId)) return false;
    return adapter.setTarget(lease, targetId);
  }

  boolean clearTarget(ActiveLease lease) { return guardAction(lease, "stale profile session before clear") && adapter.clearTarget(lease); }

  boolean invokeSkill(ActiveLease lease, String skillId) {
    return guardAction(lease, "stale profile session before skill")
        && GlobalPetDefinitions.GLOBAL_WOLF.allowsSkill(skillId) && adapter.invokeSkill(lease, skillId);
  }

  boolean invokeDuelSkill(ActiveLease lease, String skillId) {
    return guardAction(lease, "stale profile session before duel skill")
        && GlobalPetDefinitions.GLOBAL_WOLF.allowsSkill(skillId) && adapter.invokeSkill(lease, skillId);
  }

  boolean setDuelTarget(ActiveLease lease, UUID targetId) {
    return guardAction(lease, "stale profile session before duel target") && adapter.setTarget(lease, targetId);
  }

  boolean guardDuelRequest(ActiveLease lease) {
    return guardAction(lease, "domain-02 character mapping is not verified before duel request");
  }

  boolean clearDuelTarget(ActiveLease lease) {
    return guardAction(lease, "stale profile session before duel clear") && adapter.clearTarget(lease);
  }

  void tick(long tick) {
    admission.cleanupExpired(tick);
    gate.onTick(tick);
    for (ActiveLease lease : leases.snapshot()) {
      Player owner = Bukkit.getPlayer(lease.ownerPlayerId());
      if (owner == null || !owner.isOnline()) { recall(lease, "owner offline"); continue; }
      if (!sessionMatches(lease)) { recall(lease, "profile session epoch changed"); continue; }
      Optional<PetSubjectId> subject;
      try {
        subject = subjects == null ? Optional.empty() : subjects.resolve(owner);
      } catch (RuntimeException unavailable) {
        subject = Optional.empty();
      }
      if (subject == null || subject.isEmpty() || !subject.get().equals(lease.subjectId())) {
        recall(lease, "profile session changed");
        continue;
      }
      if (!guardAction(lease, "active pet state unavailable during tick")) continue;
      adapter.follow(lease, owner.getUniqueId());
    }
  }

  void recall(ActiveLease lease, String reason) {
    if (!leaseCurrent(lease)) { adapter.despawnTaggedEntity(lease.entityId(), lease); return; }
    adapter.despawn(lease);
    leases.release(lease);
    admission.release(lease.petInstanceId());
  }

  void recallOwner(UUID ownerId, String reason) {
    for (ActiveLease lease : leases.snapshot()) if (lease.ownerPlayerId().equals(ownerId)) recall(lease, reason);
  }

  void handleCallback(ActiveLease callback, String reason) {
    if (callback == null) return;
    if (!leaseCurrent(callback)) {
      adapter.despawnTaggedEntity(callback.entityId(), callback);
      return;
    }
    recall(callback, reason);
  }

  void reconcile() { adapter.reconcileTaggedEntities(leases.snapshot()); }
  Collection<ActiveLease> activeLeases() { return leases.snapshot(); }

  private boolean leaseCurrent(ActiveLease lease) { return lease != null && leases.isCurrent(lease); }
  private boolean sessionMatches(ActiveLease lease) {
    return sessions.current(lease.ownerPlayerId())
        .filter(session -> session.epoch() == lease.runtimeEpoch() && session.subjectId().equals(lease.subjectId()))
        .isPresent();
  }
  private boolean current(ActiveLease lease) { return leaseCurrent(lease) && sessionMatches(lease); }
  private boolean guardAction(ActiveLease lease, String reason) {
    if (!leaseCurrent(lease)) {
      if (lease != null) adapter.despawnTaggedEntity(lease.entityId(), lease);
      return false;
    }
    if (!sessionMatches(lease)) {
      recall(lease, reason);
      return false;
    }
    if (!authorityHealthy()) {
      recall(lease, "durable PetCore authority unavailable");
      return false;
    }
    if (!PetSubjectCompatibility.CompatibilityResolution.safeResolve(compatibility, lease.subjectId())
        .allowsPhaseOneReadThrough()) {
      recall(lease, "domain-02 character mapping is not verified");
      return false;
    }
    PlayerSession session = sessions.current(lease.ownerPlayerId()).orElse(null);
    if (session == null || content == null) {
      recall(lease, "content eligibility unavailable");
      return false;
    }
    try {
      ContentResolution resolution = content.resolve(lease.subjectId(), session);
      if (resolution == null || !resolution.known()) {
        recall(lease, "content eligibility unavailable");
        return false;
      }
    } catch (RuntimeException unavailable) {
      recall(lease, "content eligibility unavailable");
      return false;
    }
    return true;
  }
  private boolean authorityHealthy() {
    try { return authority.get() == AuthorityState.HEALTHY; }
    catch (RuntimeException unavailable) { return false; }
  }
  private PetDefinition definition(String id) { return GlobalPetDefinitions.ID.equals(id) ? GlobalPetDefinitions.GLOBAL_WOLF : null; }
  private boolean validTarget(ActiveLease lease, UUID targetId) {
    Entity target = targetId == null ? null : Bukkit.getEntity(targetId);
    Entity owner = Bukkit.getPlayer(lease.ownerPlayerId());
    return target instanceof LivingEntity && !(target instanceof Player) && target != owner && !target.isDead()
        && target.getPersistentDataContainer().getKeys().stream().noneMatch(key -> key.getNamespace().equals("petcore")
            && key.getKey().equals("pet_instance_id"))
        && owner != null && target.getWorld().equals(owner.getWorld());
  }
}
