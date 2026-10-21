package pl.mcmmorpg.minioncore;

import java.util.UUID;

/**
 * Entity boundary for the lifecycle core. A future Mythic adapter implements this;
 * this interface deliberately has no vendor API types.
 */
public interface MinionEntityAdapter {
  UUID spawn(String typeId, UUID ownerId, UUID groupId);

  /**
   * Makes one fully indexed entity operational after its group commits. Spawned
   * entities must remain fail-closed until this boundary is reached.
   */
  default void activate(UUID entityId) {
    // Existing non-Bukkit lifecycle fakes do not need an activation effect.
  }

  /** Drops adapter-local state after Bukkit reports an entity was removed naturally. */
  default void onEntityRemoved(UUID entityId) {
    // Existing lifecycle fakes retain no adapter-local state beyond despawn.
  }

  /** Called after T2 has indexed a spawned entity but before the group is activated. */
  default void onIndexed(UUID entityId) {
    // Adapter implementations may observe the fail-closed SPAWNING boundary.
  }

  void despawn(UUID entityId);

  void setTarget(UUID entityId, UUID targetId);

  void clearTarget(UUID entityId);
}
