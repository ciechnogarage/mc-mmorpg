package pl.mcmmorpg.czujnosc;

/** Czysta matematyka czujnosci (bez Bukkita, testowalna). Jednostka: punkty czujnosci 0..100 na sekunde.
 *  Wzrok: czas do pelnego alarmu rosnie liniowo z dystansem (czasBlisko..czasDaleko), mnozony przez postawe,
 *  swiatlo i widocznosc. Sluch: tylko gdy gracz robi halas, slabnie z dystansem, nie daje alarmu sam (limit w Stan). */
public final class Percepcja {
  private Percepcja() {}

  public enum Postawa { KUCA, STOI, CHODZI, BIEGNIE }

  /** Przyrost z wzroku [pkt/s]; 0 gdy poza zasiegiem lub stozkiem (linia wzroku sprawdzana osobno, PO tej funkcji). */
  public static double wzrok(Profil pr, double dystans, double katStopnie, Postawa postawa, int swiatlo, double widocznosc) {
    if (dystans > pr.wzrokZasieg() || katStopnie > pr.wzrokKat() / 2) return 0;
    double t = pr.czasBlisko() + (pr.czasDaleko() - pr.czasBlisko()) * (dystans / pr.wzrokZasieg());
    double v = 100.0 / t;
    if (katStopnie > pr.wzrokOstry() / 2) v *= pr.peryferia();
    v *= switch (postawa) { case KUCA -> pr.mnKuca(); case STOI -> pr.mnStoi(); case CHODZI -> 1.0; case BIEGNIE -> pr.mnBieg(); };
    v *= pr.swiatloMin() + (1 - pr.swiatloMin()) * Math.max(0, Math.min(15, swiatlo)) / 15.0;
    return v * widocznosc;
  }

  /** Glosnosc gracza 0..~1.5 z postawy i ciezaru zbroi (zbroja slychac tylko w ruchu). */
  public static double glosnosc(Postawa postawa, double ciezarZbroi, double mnozGracza) {
    double g = switch (postawa) { case KUCA, STOI -> 0; case CHODZI -> 0.5; case BIEGNIE -> 1.0; };
    if (g > 0) g += ciezarZbroi;
    return g * mnozGracza;
  }

  /** Przyrost ze sluchu [pkt/s] (bez kierunku i bez linii wzroku). */
  public static double sluch(Profil pr, double dystans, double glosnosc) {
    double zasieg = pr.sluchZasieg() * glosnosc;
    if (zasieg <= 0 || dystans >= zasieg) return 0;
    return pr.sluchSzybkosc() * (1 - dystans / zasieg);
  }

  public static double kat(double wx, double wy, double wz, double dx, double dy, double dz) {
    double lw = Math.sqrt(wx * wx + wy * wy + wz * wz), ld = Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (lw < 1e-6 || ld < 1e-6) return 0;
    double c = (wx * dx + wy * dy + wz * dz) / (lw * ld);
    return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, c))));
  }
}
