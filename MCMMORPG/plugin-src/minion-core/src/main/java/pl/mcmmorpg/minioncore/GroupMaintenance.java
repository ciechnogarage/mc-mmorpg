package pl.mcmmorpg.minioncore;

import java.util.List;

/** Bukkit-facing group work delegated from the pure central scheduler loop. */
public interface GroupMaintenance {
  void validateTarget(MinionGroup group);

  void serviceOwnerDistance(MinionGroup group);

  void scanForTarget(MinionGroup group, int candidateLimit);

  default void serviceRole(MinionGroup group, long currentTick) { }

  default void reconcileGroups(List<MinionGroup> groups) { }
}
