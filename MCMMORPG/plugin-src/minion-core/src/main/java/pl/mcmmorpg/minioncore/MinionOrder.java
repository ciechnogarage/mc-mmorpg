package pl.mcmmorpg.minioncore;

/** MVP group-level order snapshot; order execution is intentionally out of this ticket. */
public enum MinionOrder {
  FOLLOW_OWNER,
  HOLD_POSITION,
  ATTACK_SHARED_TARGET,
  RETURN_TO_OWNER
}
