package pl.mcmmorpg.minioncore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Main-thread, in-memory source of truth for temporary minion groups.
 *
 * <p>Groups are invisible to consumers until every entity has spawned and the
 * owner/entity indexes have been committed. Cleanup detaches every reference
 * and releases its allocation before asking the entity adapter to despawn, so
 * repeated event callbacks and adapter re-entry are harmless.</p>
 */
public final class MinionGroupRegistry implements MinionService {
  private final MinionEntityAdapter entityAdapter;
  private final MainThreadGuard mainThread;
  private final Clock clock;
  private final QuotaLedger quotas;
  private final Map<UUID, GroupState> groups = new HashMap<>();
  private final Map<UUID, LinkedHashSet<UUID>> ownerGroups = new HashMap<>();
  private final Map<UUID, UUID> entityGroups = new HashMap<>();
  private boolean disabled;

  public MinionGroupRegistry(
      MinionEntityAdapter entityAdapter,
      MainThreadGuard mainThread,
      Clock clock,
      int perOwnerLimit,
      int globalLimit) {
    this.entityAdapter = Objects.requireNonNull(entityAdapter, "entityAdapter");
    this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.quotas = new QuotaLedger(perOwnerLimit, globalLimit);
  }

  @Override
  public SummonResult summon(UUID ownerId, String typeId, int amount, Duration duration) {
    requireMainThread();
    if (disabled) {
      return SummonResult.failure(SummonFailure.REGISTRY_DISABLED);
    }
    if (ownerId == null || typeId == null || typeId.isBlank() || amount <= 0 || duration == null || duration.isNegative() || duration.isZero()) {
      return SummonResult.failure(SummonFailure.INVALID_REQUEST);
    }

    SummonFailure quotaFailure = quotas.reserve(ownerId, amount);
    if (quotaFailure != null) {
      return SummonResult.failure(quotaFailure);
    }

    GroupState state = new GroupState(UUID.randomUUID(), ownerId, typeId, amount, clock.instant().plus(duration));
    groups.put(state.groupId, state);
    state.lifecycle = GroupLifecycleState.SPAWNING;

    try {
      for (int index = 0; index < amount; index++) {
        if (state.lifecycle != GroupLifecycleState.SPAWNING) {
          return SummonResult.failure(disabled ? SummonFailure.REGISTRY_DISABLED : SummonFailure.SPAWN_FAILED);
        }
        UUID entityId = entityAdapter.spawn(typeId, ownerId, state.groupId);
        if (entityId == null || entityGroups.containsKey(entityId)) {
          if (entityId != null) {
            safelyDespawn(entityId);
          }
          close(state);
          return SummonResult.failure(SummonFailure.SPAWN_FAILED);
        }
        if (state.lifecycle != GroupLifecycleState.SPAWNING) {
          safelyDespawn(entityId);
          return SummonResult.failure(disabled ? SummonFailure.REGISTRY_DISABLED : SummonFailure.SPAWN_FAILED);
        }
        state.entityIds.add(entityId);
        entityGroups.put(entityId, state.groupId);
        entityAdapter.onIndexed(entityId);
      }
    } catch (RuntimeException failure) {
      close(state);
      return SummonResult.failure(disabled ? SummonFailure.REGISTRY_DISABLED : SummonFailure.SPAWN_FAILED);
    }

    if (state.lifecycle != GroupLifecycleState.SPAWNING) {
      return SummonResult.failure(disabled ? SummonFailure.REGISTRY_DISABLED : SummonFailure.SPAWN_FAILED);
    }
    quotas.commitReservation(ownerId, amount);
    state.reservedUnits = 0;
    state.activeUnits = amount;
    state.lifecycle = GroupLifecycleState.ACTIVE;
    ownerGroups.computeIfAbsent(ownerId, ignored -> new LinkedHashSet<>()).add(state.groupId);
    try {
      for (UUID entityId : List.copyOf(state.entityIds)) {
        if (state.lifecycle != GroupLifecycleState.ACTIVE) {
          return SummonResult.failure(disabled ? SummonFailure.REGISTRY_DISABLED : SummonFailure.SPAWN_FAILED);
        }
        entityAdapter.activate(entityId);
      }
    } catch (RuntimeException failure) {
      close(state);
      return SummonResult.failure(disabled ? SummonFailure.REGISTRY_DISABLED : SummonFailure.SPAWN_FAILED);
    }
    if (state.lifecycle != GroupLifecycleState.ACTIVE) {
      return SummonResult.failure(disabled ? SummonFailure.REGISTRY_DISABLED : SummonFailure.SPAWN_FAILED);
    }
    return SummonResult.success(state.groupId);
  }

  @Override
  public void dismissGroup(UUID groupId) {
    requireMainThread();
    if (groupId != null) {
      close(groups.get(groupId));
    }
  }

  @Override
  public void dismissAll(UUID ownerId) {
    requireMainThread();
    if (ownerId == null) {
      return;
    }
    for (GroupState state : new ArrayList<>(groups.values())) {
      if (state.ownerId.equals(ownerId)) {
        close(state);
      }
    }
  }

  @Override
  public void setTarget(UUID groupId, UUID targetId) {
    requireMainThread();
    if (targetId == null) {
      clearTarget(groupId);
      return;
    }
    GroupState state = active(groupId);
    if (state == null) {
      return;
    }
    state.sharedTargetId = targetId;
    state.order = MinionOrder.ATTACK_SHARED_TARGET;
    for (UUID entityId : List.copyOf(state.entityIds)) {
      if (state.lifecycle != GroupLifecycleState.ACTIVE) {
        return;
      }
      try {
        entityAdapter.setTarget(entityId, targetId);
      } catch (RuntimeException ignored) {
        // The future adapter may log a per-entity failure; lifecycle ownership remains valid.
      }
    }
  }

  @Override
  public void clearTarget(UUID groupId) {
    requireMainThread();
    GroupState state = active(groupId);
    if (state == null) {
      return;
    }
    state.sharedTargetId = null;
    state.order = MinionOrder.FOLLOW_OWNER;
    for (UUID entityId : List.copyOf(state.entityIds)) {
      if (state.lifecycle != GroupLifecycleState.ACTIVE) {
        return;
      }
      safelyClearTarget(entityId);
    }
  }

  /** Idempotent callback shared by entity death and remove events. */
  public void onEntityRemoved(UUID entityId) {
    requireMainThread();
    if (entityId == null) {
      return;
    }
    UUID groupId = entityGroups.remove(entityId);
    if (groupId == null) {
      return;
    }
    entityAdapter.onEntityRemoved(entityId);
    GroupState state = groups.get(groupId);
    if (state == null || state.lifecycle == GroupLifecycleState.CLOSING || state.lifecycle == GroupLifecycleState.CLOSED) {
      return;
    }
    state.entityIds.remove(entityId);
    if (state.lifecycle == GroupLifecycleState.SPAWNING) {
      close(state);
      return;
    }
    if (state.lifecycle == GroupLifecycleState.ACTIVE) {
      state.activeUnits--;
      quotas.releaseActive(state.ownerId, 1);
      if (state.entityIds.isEmpty()) {
        close(state);
      }
    }
  }

  public void onOwnerQuit(UUID ownerId) {
    dismissAll(ownerId);
  }

  public void onOwnerDeath(UUID ownerId) {
    dismissAll(ownerId);
  }

  /** Runs from the future shared lifecycle scheduler; it owns no scheduler task itself. */
  public void expireAt(Instant now) {
    requireMainThread();
    Objects.requireNonNull(now, "now");
    expireAt(now, List.copyOf(groups.keySet()));
  }

  /** Expires only scheduler-selected groups, keeping cleanup within its per-tick budget. */
  public void expireAt(Instant now, Collection<UUID> groupIds) {
    requireMainThread();
    Objects.requireNonNull(now, "now");
    Objects.requireNonNull(groupIds, "groupIds");
    for (UUID groupId : List.copyOf(groupIds)) {
      GroupState state = groups.get(groupId);
      if (state != null
          && (state.lifecycle == GroupLifecycleState.SPAWNING || state.lifecycle == GroupLifecycleState.ACTIVE)
          && !now.isBefore(state.expiresAt)) {
        close(state);
      }
    }
  }

  /** Blocks new summons, then closes a stable snapshot of active and spawning groups. */
  public void disable() {
    requireMainThread();
    if (disabled) {
      return;
    }
    disabled = true;
    for (GroupState state : new ArrayList<>(groups.values())) {
      close(state);
    }
  }

  /** Only ACTIVE groups are observable to targeting and command layers. */
  public Optional<MinionGroup> group(UUID groupId) {
    requireMainThread();
    GroupState state = active(groupId);
    return state == null ? Optional.empty() : Optional.of(state.snapshot());
  }

  public Optional<GroupLifecycleState> lifecycleState(UUID groupId) {
    requireMainThread();
    GroupState state = groups.get(groupId);
    return state == null ? Optional.empty() : Optional.of(state.lifecycle);
  }

  /** Deterministic active snapshots for the sole lifecycle scheduler. */
  public List<MinionGroup> activeGroups() {
    requireMainThread();
    return groups.values().stream()
        .filter(state -> state.lifecycle == GroupLifecycleState.ACTIVE)
        .map(GroupState::snapshot)
        .sorted((left, right) -> left.groupId().compareTo(right.groupId()))
        .toList();
  }

  /** Active owner groups for the command and event target adapters. */
  public List<MinionGroup> groupsForOwner(UUID ownerId) {
    requireMainThread();
    if (ownerId == null) {
      return List.of();
    }
    LinkedHashSet<UUID> groupIds = ownerGroups.get(ownerId);
    if (groupIds == null) {
      return List.of();
    }
    return groupIds.stream()
        .map(this::active)
        .filter(Objects::nonNull)
        .map(GroupState::snapshot)
        .toList();
  }

  /** Only active, indexed minions participate in target event handling. */
  public Optional<UUID> groupIdForEntity(UUID entityId) {
    requireMainThread();
    UUID groupId = entityGroups.get(entityId);
    return active(groupId) == null ? Optional.empty() : Optional.of(groupId);
  }

  /** PDC binding check for guards that must also recognize indexed SPAWNING entities. */
  public Optional<GroupLifecycleState> bindingState(UUID entityId, UUID ownerId, UUID groupId) {
    requireMainThread();
    if (entityId == null || ownerId == null || groupId == null || !groupId.equals(entityGroups.get(entityId))) {
      return Optional.empty();
    }
    GroupState state = groups.get(groupId);
    if (state == null || !state.ownerId.equals(ownerId) || !state.entityIds.contains(entityId)
        || (state.lifecycle != GroupLifecycleState.SPAWNING && state.lifecycle != GroupLifecycleState.ACTIVE)) {
      return Optional.empty();
    }
    return Optional.of(state.lifecycle);
  }

  /** HOLD is a group state change and always removes the current shared target. */
  public void holdGroup(UUID groupId) {
    requireMainThread();
    GroupState state = active(groupId);
    if (state == null) {
      return;
    }
    state.sharedTargetId = null;
    state.order = MinionOrder.HOLD_POSITION;
    for (UUID entityId : List.copyOf(state.entityIds)) {
      if (state.lifecycle != GroupLifecycleState.ACTIVE) {
        return;
      }
      safelyClearTarget(entityId);
    }
  }

  public RegistryDiagnostics diagnostics() {
    requireMainThread();
    int activeGroups = 0;
    for (GroupState state : groups.values()) {
      if (state.lifecycle == GroupLifecycleState.ACTIVE) {
        activeGroups++;
      }
    }
    return new RegistryDiagnostics(
        groups.size(),
        activeGroups,
        entityGroups.size(),
        quotas.activeGlobal(),
        quotas.reservedGlobal(),
        ownerGroups.size());
  }

  private GroupState active(UUID groupId) {
    if (groupId == null) {
      return null;
    }
    GroupState state = groups.get(groupId);
    return state != null && state.lifecycle == GroupLifecycleState.ACTIVE ? state : null;
  }

  private void close(GroupState state) {
    if (state == null || state.lifecycle == GroupLifecycleState.CLOSING || state.lifecycle == GroupLifecycleState.CLOSED) {
      return;
    }
    state.lifecycle = GroupLifecycleState.CLOSING;
    List<UUID> entities = List.copyOf(state.entityIds);
    for (UUID entityId : entities) {
      entityGroups.remove(entityId, state.groupId);
    }
    state.entityIds.clear();
    if (state.reservedUnits > 0) {
      quotas.releaseReservation(state.ownerId, state.reservedUnits);
      state.reservedUnits = 0;
    }
    if (state.activeUnits > 0) {
      quotas.releaseActive(state.ownerId, state.activeUnits);
      state.activeUnits = 0;
    }
    removeFromOwnerIndex(state);
    state.sharedTargetId = null;
    for (UUID entityId : entities) {
      safelyDespawn(entityId);
    }
    state.lifecycle = GroupLifecycleState.CLOSED;
    groups.remove(state.groupId, state);
  }

  private void removeFromOwnerIndex(GroupState state) {
    LinkedHashSet<UUID> ids = ownerGroups.get(state.ownerId);
    if (ids == null) {
      return;
    }
    ids.remove(state.groupId);
    if (ids.isEmpty()) {
      ownerGroups.remove(state.ownerId);
    }
  }

  private void safelyDespawn(UUID entityId) {
    try {
      entityAdapter.despawn(entityId);
    } catch (RuntimeException ignored) {
      // Cleanup is best-effort at the boundary; ownership was already removed exactly once.
    }
  }

  private void safelyClearTarget(UUID entityId) {
    try {
      entityAdapter.clearTarget(entityId);
    } catch (RuntimeException ignored) {
      // Target propagation never retains registry state after an adapter fault.
    }
  }

  private void requireMainThread() {
    mainThread.requireMainThread();
  }

  private static final class GroupState {
    private final UUID groupId;
    private final UUID ownerId;
    private final String typeId;
    private final Instant expiresAt;
    private final Set<UUID> entityIds = new LinkedHashSet<>();
    private GroupLifecycleState lifecycle = GroupLifecycleState.RESERVING;
    private UUID sharedTargetId;
    private MinionOrder order = MinionOrder.FOLLOW_OWNER;
    private int reservedUnits;
    private int activeUnits;

    private GroupState(UUID groupId, UUID ownerId, String typeId, int reservedUnits, Instant expiresAt) {
      this.groupId = groupId;
      this.ownerId = ownerId;
      this.typeId = typeId;
      this.reservedUnits = reservedUnits;
      this.expiresAt = expiresAt;
    }

    private MinionGroup snapshot() {
      return new MinionGroup(groupId, ownerId, typeId, entityIds, sharedTargetId, order, expiresAt);
    }
  }

  private static final class QuotaLedger {
    private final int perOwnerLimit;
    private final int globalLimit;
    private final Map<UUID, Integer> ownerReserved = new HashMap<>();
    private final Map<UUID, Integer> ownerActive = new HashMap<>();
    private int reservedGlobal;
    private int activeGlobal;

    private QuotaLedger(int perOwnerLimit, int globalLimit) {
      if (perOwnerLimit <= 0 || globalLimit <= 0) {
        throw new IllegalArgumentException("quota limits must be positive");
      }
      this.perOwnerLimit = perOwnerLimit;
      this.globalLimit = globalLimit;
    }

    private SummonFailure reserve(UUID ownerId, int amount) {
      int ownerTotal = ownerReserved.getOrDefault(ownerId, 0) + ownerActive.getOrDefault(ownerId, 0);
      if (ownerTotal + amount > perOwnerLimit) {
        return SummonFailure.OWNER_QUOTA_EXCEEDED;
      }
      if (reservedGlobal + activeGlobal + amount > globalLimit) {
        return SummonFailure.GLOBAL_QUOTA_EXCEEDED;
      }
      ownerReserved.merge(ownerId, amount, Integer::sum);
      reservedGlobal += amount;
      return null;
    }

    private void commitReservation(UUID ownerId, int amount) {
      decrement(ownerReserved, ownerId, amount);
      reservedGlobal -= amount;
      ownerActive.merge(ownerId, amount, Integer::sum);
      activeGlobal += amount;
    }

    private void releaseReservation(UUID ownerId, int amount) {
      decrement(ownerReserved, ownerId, amount);
      reservedGlobal -= amount;
    }

    private void releaseActive(UUID ownerId, int amount) {
      decrement(ownerActive, ownerId, amount);
      activeGlobal -= amount;
    }

    private int activeGlobal() {
      return activeGlobal;
    }

    private int reservedGlobal() {
      return reservedGlobal;
    }

    private static void decrement(Map<UUID, Integer> counts, UUID ownerId, int amount) {
      int current = counts.getOrDefault(ownerId, 0);
      if (amount <= 0 || current < amount) {
        throw new IllegalStateException("quota release did not match its allocation");
      }
      if (current == amount) {
        counts.remove(ownerId);
      } else {
        counts.put(ownerId, current - amount);
      }
    }
  }
}
