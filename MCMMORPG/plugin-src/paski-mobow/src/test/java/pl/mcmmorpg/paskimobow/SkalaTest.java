package pl.mcmmorpg.paskimobow;

/** Uruchamiany także bez Gradle: javac Skala.java SkalaTest.java && java ...SkalaTest. */
public final class SkalaTest {
  private static void blisko(float oczekiwana, double odleglosc, double max) {
    float wynik = Skala.dlaOdleglosci(odleglosc, max);
    if (Math.abs(wynik - oczekiwana) > 0.001f)
      throw new AssertionError("odleglosc=" + odleglosc + ", max=" + max + ": " + wynik + " zamiast " + oczekiwana);
  }

  public static void main(String[] args) {
    blisko(0.14f, 0.2, 0.9);
    blisko(0.18f, 1.0, 0.9);
    blisko(0.54f, 3.0, 0.9);
    blisko(0.9f, 5.0, 0.9);
    blisko(0.7f, 8.0, 0.7);
  }
}
