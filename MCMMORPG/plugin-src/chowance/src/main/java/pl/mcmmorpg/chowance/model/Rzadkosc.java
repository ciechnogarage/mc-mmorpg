package pl.mcmmorpg.chowance.model;

public enum Rzadkosc {
  POSPOLITY("Pospolity"), NIEPOSPOLITY("Niepospolity"), RZADKI("Rzadki"), EPICKI("Epicki"), LEGENDARNY("Legendarny");

  private final String nazwa;
  Rzadkosc(String nazwa) { this.nazwa = nazwa; }
  public String nazwa() { return nazwa; }

  public static Rzadkosc parse(String s) {
    if (s == null) return null;
    try { return valueOf(s.trim().toUpperCase(java.util.Locale.ROOT)); } catch (IllegalArgumentException e) { return null; }
  }
}
