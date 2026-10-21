package pl.mcmmorpg.minioncore;

import java.util.UUID;

/** Regression coverage for group target authority around Bukkit target churn. */
public final class TargetResolverTest {
  public static void main(String[] args) {
    UUID shared = UUID.randomUUID();
    UUID stray = UUID.randomUUID();

    TargetResolver.TargetEventDecision strayDecision =
        TargetResolver.targetEventDecision(shared, stray, true);
    check(strayDecision.cancelEvent(), "stray target event is blocked");
    check(!strayDecision.clearGroupTarget(), "stray target must not clear shared group target");

    TargetResolver.TargetEventDecision legalShared =
        TargetResolver.targetEventDecision(shared, shared, true);
    check(!legalShared.cancelEvent(), "legal shared target is allowed");
    check(!legalShared.clearGroupTarget(), "legal shared target remains");

    TargetResolver.TargetEventDecision deniedShared =
        TargetResolver.targetEventDecision(shared, shared, false);
    check(deniedShared.cancelEvent(), "policy-denied shared target is blocked");
    check(deniedShared.clearGroupTarget(), "policy-denied shared target clears group");

    TargetResolver.TargetEventDecision missingShared =
        TargetResolver.targetEventDecision(null, stray, true);
    check(missingShared.cancelEvent(), "target event without shared target is blocked");
    check(!missingShared.clearGroupTarget(), "target event without shared target must not fan out cleanup");

    System.out.println("TARGET_RESOLVER_TEST_PASS");
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }
}
