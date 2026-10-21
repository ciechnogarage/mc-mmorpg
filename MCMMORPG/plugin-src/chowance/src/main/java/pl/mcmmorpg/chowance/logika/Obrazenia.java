package pl.mcmmorpg.chowance.logika;

/**
 * Wzór obrażeń (jawny, wspólny dla walki turowej, pojedynku real-time i PvE):
 * <pre>
 *  base = floor(floor(floor(2*L/5 + 2) * moc * A / D) / 50) + 2
 *  dmg  = floor(base * STAB * typ * krytyk * losowy * cechy)
 * </pre>
 * STAB (domyślnie 1.5, typy.yml: stab) gdy typ ruchu == typ atakującego; losowy 0.85..1.00; krytyk 1.5.
 */
public final class Obrazenia {
  private Obrazenia() {}

  public static int bazowe(int poziom, int moc, double atak, double obrona) {
    if (moc <= 0) return 0;
    double d = Math.max(1, obrona);
    long a = (long) Math.floor(2.0 * poziom / 5.0 + 2);
    long x = (long) Math.floor(a * moc * atak / d);
    return (int) (x / 50 + 2);
  }

  public static int koncowe(int bazowe, boolean stab, double typ, boolean krytyk, double losowy, double cechy) {
    return koncowe(bazowe, stab ? 1.5 : 1.0, typ, krytyk, losowy, cechy);
  }

  /** Wariant z jawnym mnożnikiem STAB (typy.yml: stab). */
  public static int koncowe(int bazowe, double stab, double typ, boolean krytyk, double losowy, double cechy) {
    if (typ <= 0) return 0;
    double v = bazowe * stab * typ * (krytyk ? 1.5 : 1.0) * losowy * cechy;
    return Math.max(1, (int) Math.floor(v));
  }
}
