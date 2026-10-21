package pl.mcmmorpg.minioncore;

/** Pure distance/cooldown boundaries behind the central ranged role service. */
public final class RoleBehaviorTest {
  public static void main(String[] args) {
    check(RoleBehavior.withinRangedAttackDistance(25.0d * 25.0d), "ranged attack includes chase boundary");
    check(!RoleBehavior.withinRangedAttackDistance(25.01d * 25.01d), "ranged attack stops outside chase boundary");
    check(!RoleBehavior.withinRangedAttackDistance(0.0d), "ranged attack never normalizes a zero vector");
    check(RoleBehavior.shouldRetreat(3.99d * 3.99d), "close target triggers a short retreat");
    check(!RoleBehavior.shouldRetreat(4.0d * 4.0d), "retreat ends at its configured boundary");
    System.out.println("ROLE_BEHAVIOR_TEST_PASS");
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
