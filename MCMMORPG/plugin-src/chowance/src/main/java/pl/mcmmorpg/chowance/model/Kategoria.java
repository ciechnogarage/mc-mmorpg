package pl.mcmmorpg.chowance.model;

public enum Kategoria {
  FIZYCZNY, SPECJALNY, STATUS;

  public static Kategoria parse(String s) {
    if (s == null) return null;
    try { return valueOf(s.trim().toUpperCase(java.util.Locale.ROOT)); } catch (IllegalArgumentException e) { return null; }
  }
}
