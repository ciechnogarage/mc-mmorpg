package pl.mcmmorpg.chowance.model;

/** Sześć statystyk osobnika. Indeks = pozycja w tablicach genów/statów. */
public enum Stat {
  ZDROWIE("HP"), ATAK("Atk"), OBRONA("Obr"), ATAK_SPEC("AtkS"), OBRONA_SPEC("ObrS"), SZYBKOSC("Szyb");

  private final String skrot;
  Stat(String skrot) { this.skrot = skrot; }
  public String skrot() { return skrot; }

  public static Stat parse(String s) {
    if (s == null) return null;
    try { return valueOf(s.trim().toUpperCase(java.util.Locale.ROOT)); } catch (IllegalArgumentException e) { return null; }
  }
}
