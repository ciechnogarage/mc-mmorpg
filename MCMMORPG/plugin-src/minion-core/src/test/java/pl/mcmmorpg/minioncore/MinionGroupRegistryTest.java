package pl.mcmmorpg.minioncore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** In-memory lifecycle contract tests; no Bukkit, MythicMobs, or scheduler is involved. */
public final class MinionGroupRegistryTest {
  private static final UUID OWNER_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID OWNER_B = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final UUID TARGET = UUID.fromString("00000000-0000-0000-0000-000000000099");

  public static void main(String[] args) {
    rollsBackEverySpawnFailurePosition();
    enforcesOwnerAndGlobalQuotasBeforeFirstSpawn();
    removesADeadEntityOnlyOnceAndClosesItsEmptyGroup();
    disableDuringSpawningLeavesNoLateEntityOrAllocation();
    rollsBackWhenActivationFailsAfterAllEntitiesSpawn();
    propagatesSharedTargetAndClearsItForHold();
    dismissWinsOverAReentrantTargetUpdate();
    cleansUpAllLifecycleEntryPointsAndRejectsOffThreadUse();
    System.out.println("GROUP_REGISTRY_TEST_PASS");
  }

  private static void rollsBackEverySpawnFailurePosition() {
    for (int failingSpawn = 1; failingSpawn <= 3; failingSpawn++) {
      FakeAdapter adapter = new FakeAdapter();
      adapter.failSpawnNumber = failingSpawn;
      MinionGroupRegistry registry = registry(adapter, 3, 30);

      SummonResult result = registry.summon(OWNER_A, "minion.melee.tier1", 3, Duration.ofSeconds(60));

      check(!result.success(), "spawn #" + failingSpawn + " must fail the summon");
      check(result.failure() == SummonFailure.SPAWN_FAILED, "spawn failure reason must be retained");
      check(adapter.liveEntityIds.isEmpty(), "spawn #" + failingSpawn + " left a live entity");
      check(registry.diagnostics().isEmpty(), "spawn #" + failingSpawn + " left registry references");
    }
  }

  private static void enforcesOwnerAndGlobalQuotasBeforeFirstSpawn() {
    FakeAdapter adapter = new FakeAdapter();
    MinionGroupRegistry registry = registry(adapter, 3, 5);

    SummonResult ownerAllocation = registry.summon(OWNER_A, "minion.melee.tier1", 3, Duration.ofSeconds(60));
    SummonResult ownerOverflow = registry.summon(OWNER_A, "minion.melee.tier1", 1, Duration.ofSeconds(60));
    SummonResult globalAllocation = registry.summon(OWNER_B, "minion.ranged.tier1", 2, Duration.ofSeconds(60));
    SummonResult globalOverflow = registry.summon(UUID.randomUUID(), "minion.support.tier1", 1, Duration.ofSeconds(60));

    check(ownerAllocation.success() && globalAllocation.success(), "permitted allocations must succeed");
    check(ownerOverflow.failure() == SummonFailure.OWNER_QUOTA_EXCEEDED, "owner cap must reject before spawn");
    check(globalOverflow.failure() == SummonFailure.GLOBAL_QUOTA_EXCEEDED, "global cap must reject before spawn");
    check(adapter.spawnCalls == 5, "rejected quota requests must not spawn entities");
    check(registry.diagnostics().activeEntityCount() == 5, "active allocation must match spawned entities");
  }

  private static void removesADeadEntityOnlyOnceAndClosesItsEmptyGroup() {
    FakeAdapter adapter = new FakeAdapter();
    MinionGroupRegistry registry = registry(adapter, 3, 30);
    UUID groupId = requiredGroup(registry.summon(OWNER_A, "minion.melee.tier1", 1, Duration.ofSeconds(60)));
    UUID entityId = registry.group(groupId).orElseThrow().entityIds().iterator().next();

    registry.onEntityRemoved(entityId);
    registry.onEntityRemoved(entityId);

    check(registry.diagnostics().isEmpty(), "duplicate death/remove must leave no references");
    check(adapter.despawnedEntityIds.isEmpty(), "already removed entity must not be despawned again");
  }

  private static void disableDuringSpawningLeavesNoLateEntityOrAllocation() {
    FakeAdapter adapter = new FakeAdapter();
    MinionGroupRegistry registry = registry(adapter, 3, 30);
    adapter.afterSpawn = registry::disable;

    SummonResult result = registry.summon(OWNER_A, "minion.melee.tier1", 2, Duration.ofSeconds(60));

    check(!result.success() && result.failure() == SummonFailure.REGISTRY_DISABLED, "disable must abort SPAWNING");
    check(adapter.liveEntityIds.isEmpty(), "late spawn return after disable must be despawned");
    check(registry.diagnostics().isEmpty(), "disable must clear spawning references and reservations");
  }

  private static void rollsBackWhenActivationFailsAfterAllEntitiesSpawn() {
    FakeAdapter adapter = new FakeAdapter();
    adapter.failActivationNumber = 2;
    MinionGroupRegistry registry = registry(adapter, 3, 30);

    SummonResult result = registry.summon(OWNER_A, "minion.melee.tier1", 3, Duration.ofSeconds(60));

    check(!result.success() && result.failure() == SummonFailure.SPAWN_FAILED,
        "activation failure must fail the whole summon");
    check(adapter.liveEntityIds.isEmpty(), "activation failure must despawn every provisional entity");
    check(registry.diagnostics().isEmpty(), "activation failure must release every lifecycle reference");
  }

  private static void propagatesSharedTargetAndClearsItForHold() {
    FakeAdapter adapter = new FakeAdapter();
    MinionGroupRegistry registry = registry(adapter, 3, 30);
    UUID groupId = requiredGroup(registry.summon(OWNER_A, "minion.melee.tier1", 3, Duration.ofSeconds(60)));

    registry.setTarget(groupId, TARGET);

    MinionGroup attacking = registry.group(groupId).orElseThrow();
    check(attacking.sharedTargetId().equals(TARGET), "group must retain its shared target");
    check(attacking.order() == MinionOrder.ATTACK_SHARED_TARGET, "target must set ATTACK order");
    check(adapter.targetedEntityIds.size() == 3, "target must propagate to every member");

    registry.holdGroup(groupId);

    MinionGroup holding = registry.group(groupId).orElseThrow();
    check(holding.sharedTargetId() == null, "HOLD must clear the shared target");
    check(holding.order() == MinionOrder.HOLD_POSITION, "HOLD must retain its group order");
    check(adapter.clearedEntityIds.size() == 3, "HOLD must clear every member target");
  }

  private static void dismissWinsOverAReentrantTargetUpdate() {
    FakeAdapter adapter = new FakeAdapter();
    MinionGroupRegistry registry = registry(adapter, 3, 30);
    UUID groupId = requiredGroup(registry.summon(OWNER_A, "minion.melee.tier1", 2, Duration.ofSeconds(60)));
    adapter.afterSetTarget = () -> registry.dismissGroup(groupId);

    registry.setTarget(groupId, TARGET);

    check(adapter.targetedEntityIds.size() == 1, "dismiss race must stop target propagation after first re-entry");
    check(registry.diagnostics().isEmpty(), "dismiss race must release all registry references");
    check(adapter.liveEntityIds.isEmpty(), "dismiss race must despawn every entity");
  }

  private static void cleansUpAllLifecycleEntryPointsAndRejectsOffThreadUse() {
    FakeAdapter adapter = new FakeAdapter();
    TestThreadGuard guard = new TestThreadGuard();
    MinionGroupRegistry registry = new MinionGroupRegistry(adapter, guard, fixedClock(), 3, 30);
    UUID first = requiredGroup(registry.summon(OWNER_A, "minion.melee.tier1", 1, Duration.ofSeconds(1)));
    UUID second = requiredGroup(registry.summon(OWNER_A, "minion.ranged.tier1", 1, Duration.ofSeconds(1)));

    registry.dismissAll(OWNER_A);
    check(registry.group(first).isEmpty() && registry.group(second).isEmpty(), "dismissAll must close owner groups");
    UUID timeout = requiredGroup(registry.summon(OWNER_A, "minion.melee.tier1", 1, Duration.ofSeconds(1)));
    registry.expireAt(Instant.parse("2030-01-01T00:00:02Z"));
    check(registry.group(timeout).isEmpty(), "expiry must close the group");
    requiredGroup(registry.summon(OWNER_A, "minion.melee.tier1", 1, Duration.ofSeconds(60)));
    registry.onOwnerQuit(OWNER_A);
    check(registry.diagnostics().isEmpty(), "quit must clear all references");
    requiredGroup(registry.summon(OWNER_A, "minion.melee.tier1", 1, Duration.ofSeconds(60)));
    registry.onOwnerDeath(OWNER_A);
    check(registry.diagnostics().isEmpty(), "owner death must clear all references");

    guard.mainThread = false;
    expect(IllegalStateException.class, () -> registry.summon(OWNER_A, "minion.melee.tier1", 1, Duration.ofSeconds(60)));
  }

  private static MinionGroupRegistry registry(FakeAdapter adapter, int ownerCap, int globalCap) {
    return new MinionGroupRegistry(adapter, MainThreadGuard.noop(), fixedClock(), ownerCap, globalCap);
  }

  private static Clock fixedClock() {
    return Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);
  }

  private static UUID requiredGroup(SummonResult result) {
    check(result.success(), "expected a successful summon but got " + result.failure());
    return result.groupId().orElseThrow();
  }

  private static <T extends Throwable> T expect(Class<T> type, Runnable action) {
    try {
      action.run();
    } catch (Throwable failure) {
      if (type.isInstance(failure)) {
        return type.cast(failure);
      }
      throw new AssertionError("Expected " + type.getName() + " but got " + failure, failure);
    }
    throw new AssertionError("Expected " + type.getName() + " to be thrown");
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }

  private static final class TestThreadGuard implements MainThreadGuard {
    private boolean mainThread = true;

    @Override
    public void requireMainThread() {
      if (!mainThread) {
        throw new IllegalStateException("Minion registry must run on the main thread");
      }
    }
  }

  private static final class FakeAdapter implements MinionEntityAdapter {
    private int spawnCalls;
    private int activationCalls;
    private int failSpawnNumber = -1;
    private int failActivationNumber = -1;
    private Runnable afterSpawn;
    private Runnable afterSetTarget;
    private final Set<UUID> liveEntityIds = new LinkedHashSet<>();
    private final List<UUID> despawnedEntityIds = new ArrayList<>();
    private final List<UUID> targetedEntityIds = new ArrayList<>();
    private final List<UUID> clearedEntityIds = new ArrayList<>();

    @Override
    public UUID spawn(String typeId, UUID ownerId, UUID groupId) {
      spawnCalls++;
      if (spawnCalls == failSpawnNumber) {
        throw new IllegalStateException("Injected spawn failure #" + spawnCalls);
      }
      UUID entityId = UUID.randomUUID();
      liveEntityIds.add(entityId);
      if (afterSpawn != null) {
        Runnable callback = afterSpawn;
        afterSpawn = null;
        callback.run();
      }
      return entityId;
    }

    @Override
    public void activate(UUID entityId) {
      activationCalls++;
      if (activationCalls == failActivationNumber) {
        throw new IllegalStateException("Injected activation failure #" + activationCalls);
      }
    }

    @Override
    public void despawn(UUID entityId) {
      liveEntityIds.remove(entityId);
      despawnedEntityIds.add(entityId);
    }

    @Override
    public void setTarget(UUID entityId, UUID targetId) {
      targetedEntityIds.add(entityId);
      if (afterSetTarget != null) {
        Runnable callback = afterSetTarget;
        afterSetTarget = null;
        callback.run();
      }
    }

    @Override
    public void clearTarget(UUID entityId) {
      clearedEntityIds.add(entityId);
    }
  }
}
