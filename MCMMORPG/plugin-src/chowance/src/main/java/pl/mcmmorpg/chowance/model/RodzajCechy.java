package pl.mcmmorpg.chowance.model;

import java.util.List;

/** Rodzaje cech pasywnych implementowane przez silnik (KONTRAKT §4) + wymagane parametry. */
public enum RodzajCechy {
  WZMOCNIENIE_TYPU("typ", "mnoznik"),
  WZMOCNIENIE_TYPU_PRZY_NISKIM_HP("typ", "mnoznik", "prog_hp"),
  ODPORNOSC_NA_TYP("typ", "mnoznik"),
  ODPORNOSC_NA_STATUS("status"),
  REGENERACJA("procent"),
  MNOZNIK_STATU("stat", "mnoznik"),
  KONTRA_STATUS("status", "szansa"),
  UNIK("procent"),
  PREMIA_WIEZI("mnoznik"),
  PREMIA_EXP("mnoznik");

  private final List<String> parametry;
  RodzajCechy(String... p) { this.parametry = List.of(p); }
  public List<String> wymaganeParametry() { return parametry; }

  public static RodzajCechy parse(String s) {
    if (s == null) return null;
    try { return valueOf(s.trim().toUpperCase(java.util.Locale.ROOT)); } catch (IllegalArgumentException e) { return null; }
  }
}
