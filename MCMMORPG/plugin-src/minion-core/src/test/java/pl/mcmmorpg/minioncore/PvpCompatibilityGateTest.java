package pl.mcmmorpg.minioncore;

/** Positive and injected-late-listener contracts for fail-closed player targeting. */
public final class PvpCompatibilityGateTest {
  public static void main(String[] args) {
    enablesOnlyWhenTheFinalListenerStackPreservesCancellation();
    disablesWhenALaterListenerUncancelsTheIllegalHit();
    keepsTheApprovedPlayerDamageMultiplierBelowOne();
    System.out.println("PVP_COMPATIBILITY_GATE_TEST_PASS");
  }

  private static void enablesOnlyWhenTheFinalListenerStackPreservesCancellation() {
    PvpCompatibilityGate gate = new PvpCompatibilityGate();
    check(gate.verify(() -> true), "compatible stack must enable player targeting");
    check(gate.playerTargetingEnabled(), "enabled capability must be observable");
  }

  private static void disablesWhenALaterListenerUncancelsTheIllegalHit() {
    PvpCompatibilityGate gate = new PvpCompatibilityGate();
    check(!gate.verify(() -> false), "late uncancel must fail the compatibility gate");
    check(!gate.playerTargetingEnabled(), "failed compatibility gate must disable player targeting");
  }

  private static void keepsTheApprovedPlayerDamageMultiplierBelowOne() {
    check(DamageGuard.MINION_VS_PLAYER_DAMAGE_MULTIPLIER == 0.35d,
        "player damage multiplier must retain the approved 0.35 value");
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }
}
