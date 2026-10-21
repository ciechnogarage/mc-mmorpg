package pl.mcmmorpg.minioncore;

/** Deterministic central-scheduler work classes and their maximum service delay in ticks. */
public enum GroupService {
  TARGET_VALIDATION(5),
  OWNER_DISTANCE(10),
  SCAN(20),
  CLEANUP(40);

  private final int maxDelayTicks;

  GroupService(int maxDelayTicks) {
    this.maxDelayTicks = maxDelayTicks;
  }

  public int maxDelayTicks() {
    return maxDelayTicks;
  }
}
