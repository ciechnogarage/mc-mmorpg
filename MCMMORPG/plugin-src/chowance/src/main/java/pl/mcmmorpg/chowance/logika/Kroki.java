package pl.mcmmorpg.chowance.logika;

/** Czysta logika czterech niezależnych kroków. */
public final class Kroki {
  private Kroki() {}
  public static final int WSZYSTKIE = 0b1111;
  public static int zalicz(int maska, int krok) { return krok < 1 || krok > 4 ? maska : maska | (1 << (krok - 1)); }
  public static boolean zaliczony(int maska, int krok) { return krok >= 1 && krok <= 4 && (maska & (1 << (krok - 1))) != 0; }
  public static int nastepny(int maska) {
    for (int krok = 1; krok <= 4; krok++) if (!zaliczony(maska, krok)) return krok;
    return 0;
  }
  public static int postep(int maska) { return Integer.bitCount(maska & WSZYSTKIE); }
}
