package pl.mcmmorpg.chowance.model;

import java.util.Collection;

/** Mnożniki skuteczności atak -> obrona. Domyślnie 1.0. */
public final class TabelaTypow {
  private final double[][] m = new double[Typ.values().length][Typ.values().length];
  /** mnożnik ruchu w typie własnym stwora (STAB) — typy.yml: stab */
  private double stab = 1.5;

  public double stab() { return stab; }
  public void ustawStab(double v) { this.stab = v; }

  public TabelaTypow() {
    for (double[] w : m) java.util.Arrays.fill(w, 1.0);
  }

  public void ustaw(Typ atak, Typ obrona, double mnoznik) {
    m[atak.ordinal()][obrona.ordinal()] = mnoznik;
  }

  public double mnoznik(Typ atak, Typ obrona) {
    return m[atak.ordinal()][obrona.ordinal()];
  }

  /** Iloczyn po typach obrońcy (1–2 typy). */
  public double mnoznik(Typ atak, Collection<Typ> obronca) {
    double r = 1.0;
    for (Typ t : obronca) r *= mnoznik(atak, t);
    return r;
  }
}
