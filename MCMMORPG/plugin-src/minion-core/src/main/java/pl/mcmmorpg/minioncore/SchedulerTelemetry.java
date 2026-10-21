package pl.mcmmorpg.minioncore;

import java.util.Map;
import java.util.UUID;

/** Snapshot of central scheduler service timing, kept small enough for future metrics export. */
public record SchedulerTelemetry(
    Map<GroupService, Map<UUID, ServiceTick>> latestServiceTicks,
    Map<GroupService, Long> maxObservedDelayTicks,
    Map<GroupService, ServiceTiming> serviceTimings) {
  public SchedulerTelemetry {
    latestServiceTicks = Map.copyOf(latestServiceTicks);
    maxObservedDelayTicks = Map.copyOf(maxObservedDelayTicks);
    serviceTimings = Map.copyOf(serviceTimings);
  }
}
