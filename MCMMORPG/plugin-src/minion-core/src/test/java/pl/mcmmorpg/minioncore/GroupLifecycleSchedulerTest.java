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

/** Topology and expiry contracts for the one central lifecycle scheduler. */
public final class GroupLifecycleSchedulerTest {
  private static final Instant START = Instant.parse("2030-01-01T00:00:00Z");

  public static void main(String[] args) {
    servicesThirtyAndSixtyGroupsWithinEveryBucketDeadline();
    expiresAGroupWithinTheCleanupServiceBoundAfterSixtySeconds();
    System.out.println("GROUP_LIFECYCLE_SCHEDULER_TEST_PASS");
  }

  private static void servicesThirtyAndSixtyGroupsWithinEveryBucketDeadline() {
    assertTopology(30);
    assertTopology(60);
  }

  private static void assertTopology(int groupCount) {
    FakeAdapter adapter = new FakeAdapter();
    MinionGroupRegistry registry = new MinionGroupRegistry(
        adapter, MainThreadGuard.noop(), Clock.fixed(START, ZoneOffset.UTC), 3, 60);
    RecordingMaintenance maintenance = new RecordingMaintenance();
    GroupLifecycleScheduler scheduler = new GroupLifecycleScheduler(registry, maintenance);

    for (int owner = 0; owner < groupCount; owner++) {
      SummonResult result = registry.summon(ownerId(owner), MythicMinionAdapter.MELEE_TIER_1, 1, Duration.ofMinutes(2));
      check(result.success(), "topology summon failed at owner " + owner + ": " + result.failure());
    }
    for (long tick = 1; tick <= 80; tick++) {
      scheduler.tick(tick, START.plusMillis(tick * 50));
    }

    SchedulerTelemetry telemetry = scheduler.telemetry();
    for (GroupService service : GroupService.values()) {
      check(telemetry.latestServiceTicks().get(service).size() == groupCount,
          service + " did not reach every group for topology " + groupCount);
      check(telemetry.maxObservedDelayTicks().get(service) <= service.maxDelayTicks(),
          service + " exceeded its delay budget for topology " + groupCount);
    }
    check(maintenance.maxScanCandidates <= 16, "scan must inspect no more than 16 candidates per group");
  }

  private static void expiresAGroupWithinTheCleanupServiceBoundAfterSixtySeconds() {
    FakeAdapter adapter = new FakeAdapter();
    MinionGroupRegistry registry = new MinionGroupRegistry(
        adapter, MainThreadGuard.noop(), Clock.fixed(START, ZoneOffset.UTC), 3, 3);
    GroupLifecycleScheduler scheduler = new GroupLifecycleScheduler(registry, new RecordingMaintenance());
    UUID groupId = requiredGroup(registry.summon(ownerId(99), MythicMinionAdapter.MELEE_TIER_1, 1, Duration.ofSeconds(60)));

    for (long tick = 1; tick <= 1240; tick++) {
      scheduler.tick(tick, START.plusMillis(tick * 50));
    }

    check(registry.group(groupId).isEmpty(), "60 second group must expire within the 40 tick cleanup bound");
    check(registry.diagnostics().isEmpty(), "expiry must release every group reference and allocation");
    check(adapter.live.isEmpty(), "expiry must despawn the minion");
  }

  private static UUID ownerId(int sequence) {
    return new UUID(0L, sequence + 1L);
  }

  private static UUID requiredGroup(SummonResult result) {
    check(result.success(), "expected summon success but got " + result.failure());
    return result.groupId().orElseThrow();
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }

  private static final class RecordingMaintenance implements GroupMaintenance {
    private int maxScanCandidates;

    @Override
    public void validateTarget(MinionGroup group) { }

    @Override
    public void serviceOwnerDistance(MinionGroup group) { }

    @Override
    public void scanForTarget(MinionGroup group, int candidateLimit) {
      maxScanCandidates = Math.max(maxScanCandidates, candidateLimit);
    }
  }

  private static final class FakeAdapter implements MinionEntityAdapter {
    private final Set<UUID> live = new LinkedHashSet<>();

    @Override
    public UUID spawn(String typeId, UUID ownerId, UUID groupId) {
      UUID entityId = UUID.randomUUID();
      live.add(entityId);
      return entityId;
    }

    @Override
    public void despawn(UUID entityId) {
      live.remove(entityId);
    }

    @Override
    public void setTarget(UUID entityId, UUID targetId) { }

    @Override
    public void clearTarget(UUID entityId) { }
  }
}
