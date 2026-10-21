package pl.mcmmorpg.minioncore;

/** Rolling central-scheduler timing window, expressed in nanoseconds. */
public record ServiceTiming(long p95Nanos, long maxNanos, int sampleCount) { }
