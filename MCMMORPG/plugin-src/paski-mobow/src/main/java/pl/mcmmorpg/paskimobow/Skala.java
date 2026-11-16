package pl.mcmmorpg.paskimobow;

final class Skala {
  private Skala() {}

  static float dlaOdleglosci(double odleglosc, double maksimum) {
    // Skala maleje az do samej kamery; stala dolna granica robila z bliska giganta.
    return (float) Math.min(maksimum, 0.08 * Math.max(0.05, odleglosc));
  }
}
