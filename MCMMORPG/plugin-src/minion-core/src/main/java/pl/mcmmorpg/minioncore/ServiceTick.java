package pl.mcmmorpg.minioncore;

/** Latest expected and actual central-scheduler service ticks for one group/class pair. */
public record ServiceTick(long expectedTick, long actualTick) { }
