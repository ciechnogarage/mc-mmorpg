package pl.mcmmorpg.chowance.logika;

/** Zapis ostatniego skutecznego użycia i kontrola odstępu w milisekundach. */
public final class Odstep {
  private Odstep() {}
  public static boolean gotowy(long poprzedni, long teraz, long odstepMs) {
    return poprzedni <= 0 || teraz - poprzedni >= odstepMs;
  }
}
