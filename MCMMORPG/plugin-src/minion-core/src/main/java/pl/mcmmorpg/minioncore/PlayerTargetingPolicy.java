package pl.mcmmorpg.minioncore;

import org.bukkit.entity.Entity;

public interface PlayerTargetingPolicy {
  boolean canTarget(MinionGroup group, Entity candidate);
}
