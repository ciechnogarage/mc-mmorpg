package pl.mcmmorpg.minioncore;

/** Boundary that keeps the registry independent of Bukkit while enforcing its main-thread contract. */
@FunctionalInterface
public interface MainThreadGuard {
  void requireMainThread();

  static MainThreadGuard noop() {
    return () -> { };
  }
}
