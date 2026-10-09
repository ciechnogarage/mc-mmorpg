package pl.mcmmorpg.chowance.model;

/**
 * Krzywe doświadczenia: łączny exp potrzebny do OSIĄGNIĘCIA poziomu n (n >= 1; poziom 1 = 0 exp).
 * Sześcienne jak w seriach, z których czerpie gatunek — przewidywalne i łatwe do strojenia.
 */
public enum KrzywaExp {
  SZYBKA(0.8), SREDNIA(1.0), WOLNA(1.25);

  private final double wsp;
  KrzywaExp(double wsp) { this.wsp = wsp; }

  public long lacznyExp(int poziom) {
    if (poziom <= 1) return 0;
    return (long) Math.floor(wsp * (double) poziom * poziom * poziom);
  }

  public static KrzywaExp parse(String s) {
    if (s == null) return null;
    try { return valueOf(s.trim().toUpperCase(java.util.Locale.ROOT)); } catch (IllegalArgumentException e) { return null; }
  }
}
