package pl.mcmmorpg.minioncore;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Outcome of one all-or-nothing summon transaction. */
public final class SummonResult {
  private final UUID groupId;
  private final SummonFailure failure;

  private SummonResult(UUID groupId, SummonFailure failure) {
    this.groupId = groupId;
    this.failure = failure;
  }

  public static SummonResult success(UUID groupId) {
    return new SummonResult(Objects.requireNonNull(groupId, "groupId"), null);
  }

  public static SummonResult failure(SummonFailure failure) {
    return new SummonResult(null, Objects.requireNonNull(failure, "failure"));
  }

  public boolean success() {
    return groupId != null;
  }

  public Optional<UUID> groupId() {
    return Optional.ofNullable(groupId);
  }

  public SummonFailure failure() {
    return failure;
  }
}
