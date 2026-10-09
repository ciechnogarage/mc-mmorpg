package pl.mcmmorpg.chowance.logika;

import java.util.random.RandomGenerator;

/**
 * Wzór szansy złapania (SPEC §2), wzorowany na generacji III, wszystkie współczynniki z config.yml.
 * <pre>
 *  a = (3*Hmax - 2*H) / (3*Hmax) * wsp_gatunku * m_pieczeci * m_statusu * m_poziomu * m_profesji
 *  p = clamp(a / 255, 0, 1)
 *  m_poziomu  = clamp(1 + poziom.wsp * (poziomOdniesienia - poziomCelu), poziom.min, poziom.max)
 *  m_profesji = min(1 + profesja.wsp * poziomProfesji, profesja.max)
 * </pre>
 * Próba = 4 niezależne testy o szansie p^(1/4) (iloczyn = p). Liczba drżeń pokazywanych graczowi
 * = liczba zaliczonych testów spośród pierwszych trzech (0–3); sukces = wszystkie cztery.
 */
public final class WzorLapania {

  /** Parametry z config.yml. */
  public record Parametry(double statusSen, double statusSpetanie, double statusOszolomienie,
      double poziomWsp, double poziomMin, double poziomMax, double profesjaWsp, double profesjaMax) {
    public static Parametry domyslne() {
      return new Parametry(2.0, 1.5, 2.0, 0.02, 0.4, 1.5, 0.01, 1.5);
    }
  }

  /** Dane jednej próby. */
  public record Wejscie(double hp, double hpMax, int wspolczynnikGatunku, double mnoznikPieczeci,
      String status, int poziomCelu, int poziomOdniesienia, int poziomProfesji) {}

  public record Wynik(double szansa, int drzenia, boolean sukces) {}

  private final Parametry p;

  public WzorLapania(Parametry p) { this.p = p; }

  public double mnoznikStatusu(String status) {
    if (status == null) return 1.0;
    return switch (status) {
      case "SEN" -> p.statusSen();
      case "SPETANIE" -> p.statusSpetanie();
      case "OSZOLOMIENIE" -> p.statusOszolomienie();
      default -> 1.0;
    };
  }

  public double mnoznikPoziomu(int cel, int odniesienie) {
    double m = 1.0 + p.poziomWsp() * (odniesienie - cel);
    return Math.max(p.poziomMin(), Math.min(p.poziomMax(), m));
  }

  public double mnoznikProfesji(int poziomProfesji) {
    return Math.min(p.profesjaMax(), 1.0 + p.profesjaWsp() * Math.max(0, poziomProfesji));
  }

  public double szansa(Wejscie w) {
    if (w.hpMax() <= 0) return 0;
    double hp = Math.max(1, Math.min(w.hp(), w.hpMax()));
    double hpCzynnik = (3 * w.hpMax() - 2 * hp) / (3 * w.hpMax());
    double a = hpCzynnik * w.wspolczynnikGatunku() * w.mnoznikPieczeci() * mnoznikStatusu(w.status())
        * mnoznikPoziomu(w.poziomCelu(), w.poziomOdniesienia()) * mnoznikProfesji(w.poziomProfesji());
    return Math.max(0, Math.min(1, a / 255.0));
  }

  public Wynik proba(Wejscie w, RandomGenerator rng) {
    double s = szansa(w);
    double naTest = Math.pow(s, 0.25);
    int zaliczone = 0;
    for (int i = 0; i < 4; i++) {
      if (rng.nextDouble() < naTest) zaliczone++;
      else break;
    }
    boolean sukces = zaliczone == 4;
    return new Wynik(s, Math.min(3, zaliczone), sukces);
  }
}
