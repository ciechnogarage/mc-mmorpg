package pl.mcmmorpg.paskimobow;

final class Skala {
  private Skala() {}

  static float dlaOdleglosci(double odleglosc, double maksimum) {
    // Stały rozmiar kątowy do 5 bloków: przy mobie tuż przy kamerze pasek
    // nie zasłania karty ani minimapy, a z dalszej odległości nie maleje bardziej.
    return (float) Math.min(maksimum, Math.max(0.14, 0.18 * odleglosc));
  }
}
