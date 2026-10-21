package pl.mcmmorpg.chowance.logika;

/** Ranking ELO (SPEC §6). */
public final class Elo {
  private Elo() {}

  public static double oczekiwany(int ra, int rb) {
    return 1.0 / (1.0 + Math.pow(10, (rb - ra) / 400.0));
  }

  /** Nowe oceny [zwycięzca, przegrany]. */
  public static int[] po(int zwyciezca, int przegrany, int k) {
    double e = oczekiwany(zwyciezca, przegrany);
    int delta = (int) Math.round(k * (1 - e));
    delta = Math.max(1, delta);
    return new int[] {zwyciezca + delta, Math.max(0, przegrany - delta)};
  }
}
