package pl.mcmmorpg.mapa;

import java.util.List;

final class PelnaMath {
  record Punkt(double u, double v) {}
  static final int PASEK = 28;   // wysokosc gornego paska z zakladkami (px)
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

  static Punkt punktSwiata(double u, double v, int width, int height,
      double centerX, double centerZ, double zoom) {
    return new Punkt(centerX + (u - width / 2.0) * zoom,
        centerZ + (v - height / 2.0) * zoom);
  }

  static MapaData.Marker najblizszy(List<MapaData.Marker> markers, double u, double v,
      int width, int height, double centerX, double centerZ, double zoom) {
    MapaData.Marker found = null;
    double best = 100.0;
    for (MapaData.Marker marker : markers) {
      double x = width / 2.0 + (marker.x() - centerX) / zoom;
      double y = height / 2.0 + (marker.z() - centerZ) / zoom;
      double distance = Math.pow(u - x, 2) + Math.pow(v - y, 2);
      if (distance <= best) { best = distance; found = marker; }
    }
    return found;
  }

  static int szerokoscZakladki(int width, int tabs) {
    return Math.max(1, Math.min(64, (width - 64) / Math.max(1, tabs)));
  }

  // -1 oznacza tlo, -2 zamkniecie, -3 atlas, pozostale wartosci to indeksy zakladek.
  static int przycisk(double u, double v, int width, int tabs) {
    if (v < 0 || v >= PASEK || u < 0 || u >= width) return -1;
    if (u >= width - 24) return -2;
    if (u >= width - 64) return -3;
    int tab = (int) u / szerokoscZakladki(width, tabs);
    return tab < tabs ? tab : -1;
  }
}
