package pl.mcmmorpg.minioncore;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Fail-closed capability gate for player targeting in the Bukkit listener stack. */
public final class PvpCompatibilityGate {
  private boolean playerTargetingEnabled;

  public boolean verify(BooleanSupplier illegalHitRemainsCancelled) {
    playerTargetingEnabled = Objects.requireNonNull(illegalHitRemainsCancelled, "illegalHitRemainsCancelled")
        .getAsBoolean();
    return playerTargetingEnabled;
  }

  public boolean playerTargetingEnabled() {
    return playerTargetingEnabled;
  }
}
