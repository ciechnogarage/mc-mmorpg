package pl.mcmmorpg.chowance.model;

/** Staty, które ruchy mogą modyfikować etapami -6..+6 w trakcie walki. */
public enum StatEtapu {
  ATAK, OBRONA, ATAK_SPEC, OBRONA_SPEC, SZYBKOSC, CELNOSC, UNIK;

  public static StatEtapu parse(String s) {
    if (s == null) return null;
    try { return valueOf(s.trim().toUpperCase(java.util.Locale.ROOT)); } catch (IllegalArgumentException e) { return null; }
  }

  /** Mnożnik statu za etap: +1 = 1.5, +2 = 2.0 ... -1 = 2/3 (schemat (2+n)/2). */
  public static double mnoznik(int etap) {
    int e = Math.max(-6, Math.min(6, etap));
    return e >= 0 ? (2.0 + e) / 2.0 : 2.0 / (2.0 - e);
  }

  /** Celność/unik: schemat (3+n)/3. */
  public static double mnoznikCelnosci(int etap) {
    int e = Math.max(-6, Math.min(6, etap));
    return e >= 0 ? (3.0 + e) / 3.0 : 3.0 / (3.0 - e);
  }
}
