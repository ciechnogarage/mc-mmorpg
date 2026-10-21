package pl.mcmmorpg.paskimobow;

/** Uruchamiany także bez Gradle: javac Skala.java SkalaTest.java && java ...SkalaTest. */
public final class SkalaTest {
  private static void blisko(float oczekiwana, double odleglosc, double max) {
    float wynik = Skala.dlaOdleglosci(odleglosc, max);
    if (Math.abs(wynik - oczekiwana) > 0.001f)
      throw new AssertionError("odleglosc=" + odleglosc + ", max=" + max + ": " + wynik + " zamiast " + oczekiwana);
  }

  public static void main(String[] args) {
    blisko(0.016f, 0.2, 0.4);
    blisko(0.08f, 1.0, 0.4);
    blisko(0.24f, 3.0, 0.4);
    blisko(0.4f, 5.0, 0.4);
    blisko(0.4f, 10.0, 0.4);
  }
}
