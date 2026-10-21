package pl.mcmmorpg.minioncore;

/** Internal lifecycle phases. Only ACTIVE groups are visible through {@link MinionGroupRegistry#group}. */
public enum GroupLifecycleState {
  RESERVING,
  SPAWNING,
  ACTIVE,
  CLOSING,
  CLOSED
}
