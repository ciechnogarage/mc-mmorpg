package pl.mcmmorpg.chowance.model;

/**
 * Efekt ruchu (KONTRAKT §3). Pola nieużywane przez dany rodzaj są zerowe/null.
 * cel: true = SIEBIE, false = PRZECIWNIK.
 */
public record EfektRuchu(Rodzaj rodzaj, Status status, StatEtapu stat, int zmiana, int szansa,
    boolean naSiebie, int czas, int procent) {

  public enum Rodzaj {
    STATUS, STAT, LECZENIE, WYSYSANIE, ODRZUT;

    public static Rodzaj parse(String s) {
      if (s == null) return null;
      try { return valueOf(s.trim().toUpperCase(java.util.Locale.ROOT)); } catch (IllegalArgumentException e) { return null; }
    }
  }
}
