package pl.mcmmorpg.minioncore;

/** Fail-closed summon result reasons that need no Bukkit or vendor dependency. */
public enum SummonFailure {
  INVALID_REQUEST,
  REGISTRY_DISABLED,
  OWNER_QUOTA_EXCEEDED,
  GLOBAL_QUOTA_EXCEEDED,
  SPAWN_FAILED
}
