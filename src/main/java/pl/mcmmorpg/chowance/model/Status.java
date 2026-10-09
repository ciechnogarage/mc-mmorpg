package pl.mcmmorpg.chowance.model;

/** Statusy. Pierwsze trzy liczą się do wzoru łapania (SPEC §2). */
public enum Status {
  SEN("Sen", true), SPETANIE("Spętanie", true), OSZOLOMIENIE("Oszołomienie", true),
  OPARZENIE("Oparzenie", false), ZATRUCIE("Zatrucie", false);

  private final String nazwa;
  private final boolean lapanie;
  Status(String nazwa, boolean lapanie) { this.nazwa = nazwa; this.lapanie = lapanie; }
  public String nazwa() { return nazwa; }
  /** Czy status podnosi szansę złapania. */
  public boolean pomagaLapaniu() { return lapanie; }

  public static Status parse(String s) {
    if (s == null) return null;
    try { return valueOf(s.trim().toUpperCase(java.util.Locale.ROOT)); } catch (IllegalArgumentException e) { return null; }
  }
}
