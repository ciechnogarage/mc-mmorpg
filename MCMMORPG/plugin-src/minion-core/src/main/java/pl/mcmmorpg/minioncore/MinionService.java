package pl.mcmmorpg.minioncore;

import java.time.Duration;
import java.util.UUID;

/** Public lifecycle operations; command and skill adapters are deliberately deferred. */
public interface MinionService {
  SummonResult summon(UUID ownerId, String typeId, int amount, Duration duration);

  void dismissGroup(UUID groupId);

  void dismissAll(UUID ownerId);

  void setTarget(UUID groupId, UUID targetId);

  void clearTarget(UUID groupId);
}
