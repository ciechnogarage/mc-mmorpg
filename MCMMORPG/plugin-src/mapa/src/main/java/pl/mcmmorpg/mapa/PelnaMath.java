package pl.mcmmorpg.mapa;

final class PelnaMath {
  record Punkt(double u, double v) {}
  private PelnaMath() {}

  static float kierunek(float yaw) {
    return Math.round(yaw / 90f) * 90f;
  }

  static Punkt przeciecie(double ox, double oy, double oz, double dx, double dy, double dz,
      double cx, double cy, double cz, double yaw, int width, int height) {
    double rad = Math.toRadians(yaw);
    double fx = -Math.sin(rad), fz = Math.cos(rad);
    double divisor = dx * fx + dz * fz;
    if (divisor <= 1e-9) return null;
    double distance = ((cx - ox) * fx + (cz - oz) * fz) / divisor;
    if (distance <= 0) return null;
    double px = ox + dx * distance - cx, py = oy + dy * distance - cy;
    double pz = oz + dz * distance - cz;
    double u = width / 2.0 + (px * -Math.cos(rad) + pz * -Math.sin(rad)) * 128;
    double v = height / 2.0 - py * 128;
    return u >= 0 && v >= 0 && u < width && v < height ? new Punkt(u, v) : null;
  }

  static int kolko(int oldSlot, int newSlot) {
    return Math.floorMod(newSlot - oldSlot + 4, 9) - 4;
  }

  static Punkt przesun(double centerX, double centerZ, double du, double dv, double zoom) {
    return new Punkt(centerX - du * zoom, centerZ - dv * zoom);
  }

  // -1 oznacza tlo, -2 przycisk zamkniecia, pozostale wartosci to indeksy zakladek.
  static int przycisk(double u, double v, int width, int tabs) {
    if (v < 0 || v >= 20 || u < 0 || u >= width) return -1;
    if (u >= width - 24) return -2;
    int tab = (int) u / 64;
    return tab < tabs && u < width - 24 ? tab : -1;
  }
}
