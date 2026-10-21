package pl.mcmmorpg.minioncore;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Immutable public snapshot. The registry never exposes its mutable lifecycle state. */
public record MinionGroup(
    UUID groupId,
    UUID ownerId,
    String typeId,
    Set<UUID> entityIds,
    UUID sharedTargetId,
    MinionOrder order,
    Instant expiresAt) {
  public MinionGroup {
    Objects.requireNonNull(groupId, "groupId");
    Objects.requireNonNull(ownerId, "ownerId");
    Objects.requireNonNull(typeId, "typeId");
    entityIds = Set.copyOf(entityIds);
    Objects.requireNonNull(order, "order");
    Objects.requireNonNull(expiresAt, "expiresAt");
  }
}
