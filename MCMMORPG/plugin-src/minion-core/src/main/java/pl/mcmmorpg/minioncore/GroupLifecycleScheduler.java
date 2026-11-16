package pl.mcmmorpg.minioncore;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;

/**
 * The only lifecycle scheduler. Its Bukkit task is installed by the plugin;
 * this class is deliberately clock-driven for deterministic topology tests.
 */
public final class GroupLifecycleScheduler {
  private final MinionGroupRegistry registry;
  private final GroupMaintenance maintenance;
  private final Map<GroupService, Integer> cursors = new EnumMap<>(GroupService.class);
  private final Map<GroupService, Map<UUID, ServiceTick>> latest = new EnumMap<>(GroupService.class);
  private final Map<GroupService, Long> maxObservedDelay = new EnumMap<>(GroupService.class);
  private final Map<GroupService, ArrayDeque<Long>> durationSamples = new EnumMap<>(GroupService.class);
  private final Map<GroupService, Long> maxDurationNanos = new EnumMap<>(GroupService.class);

  public GroupLifecycleScheduler(MinionGroupRegistry registry, GroupMaintenance maintenance) {
    this.registry = Objects.requireNonNull(registry, "registry");
    this.maintenance = Objects.requireNonNull(maintenance, "maintenance");
    for (GroupService service : GroupService.values()) {
      cursors.put(service, 0);
      latest.put(service, new HashMap<>());
      maxObservedDelay.put(service, 0L);
      durationSamples.put(service, new ArrayDeque<>());
      maxDurationNanos.put(service, 0L);
    }
  }

  public void tick(long currentTick, Instant now) {
    Objects.requireNonNull(now, "now");
    if (currentTick < 0) {
      throw new IllegalArgumentException("currentTick must not be negative");
    }
    List<MinionGroup> groups = registry.activeGroups();
    maintenance.reconcileGroups(groups);
    for (GroupService service : GroupService.values()) {
      latest.get(service).keySet().retainAll(groups.stream().map(MinionGroup::groupId).toList());
    }
    service(groups, GroupService.TARGET_VALIDATION, currentTick, group -> maintenance.validateTarget(group));
    service(groups, GroupService.OWNER_DISTANCE, currentTick, group -> {
      maintenance.serviceOwnerDistance(group);
      maintenance.serviceRole(group, currentTick);
    });
    service(groups, GroupService.SCAN, currentTick, group -> maintenance.scanForTarget(group, 16));
    service(groups, GroupService.CLEANUP, currentTick,
        group -> registry.expireAt(now, List.of(group.groupId())));
  }

  public SchedulerTelemetry telemetry() {
    Map<GroupService, Map<UUID, ServiceTick>> snapshots = new EnumMap<>(GroupService.class);
    for (GroupService service : GroupService.values()) {
      snapshots.put(service, Map.copyOf(latest.get(service)));
    }
    Map<GroupService, ServiceTiming> timings = new EnumMap<>(GroupService.class);
    for (GroupService service : GroupService.values()) {
      ArrayList<Long> sorted = new ArrayList<>(durationSamples.get(service));
      Collections.sort(sorted);
      long p95 = sorted.isEmpty() ? 0L : sorted.get(Math.min(sorted.size() - 1, (int) Math.ceil(sorted.size() * 0.95d) - 1));
      timings.put(service, new ServiceTiming(p95, maxDurationNanos.get(service), sorted.size()));
    }
    return new SchedulerTelemetry(snapshots, maxObservedDelay, timings);
  }

  private void service(
      List<MinionGroup> groups,
      GroupService service,
      long currentTick,
      java.util.function.Consumer<MinionGroup> work) {
    if (groups.isEmpty()) {
      cursors.put(service, 0);
      return;
    }
    int budget = Math.ceilDiv(groups.size(), service.maxDelayTicks());
    int start = Math.floorMod(cursors.get(service), groups.size());
    Map<UUID, ServiceTick> serviceTicks = latest.get(service);
    for (int offset = 0; offset < budget; offset++) {
      MinionGroup group = groups.get((start + offset) % groups.size());
      ServiceTick previous = serviceTicks.get(group.groupId());
      long expected = previous == null
          ? currentTick + service.maxDelayTicks() - 1L
          : previous.actualTick() + service.maxDelayTicks();
      if (previous != null) {
        maxObservedDelay.merge(service, currentTick - previous.actualTick(), Math::max);
      }
      serviceTicks.put(group.groupId(), new ServiceTick(expected, currentTick));
      long started = System.nanoTime();
      try {
        work.accept(group);
      } finally {
        long elapsed = Math.max(0L, System.nanoTime() - started);
        ArrayDeque<Long> samples = durationSamples.get(service);
        if (samples.size() >= 256) samples.removeFirst();
        samples.addLast(elapsed);
        maxDurationNanos.merge(service, elapsed, Math::max);
      }
    }
    cursors.put(service, (start + budget) % groups.size());
  }
}
