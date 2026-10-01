package pl.mcmmorpg.mapa;

final class MapaMath {
  private MapaMath() {}

  static int modelDirection(float yaw) {
    return Math.floorMod(Math.round(yaw / 45f), 8);
  }

  static int cursorDirection(float yaw) {
    return Math.floorMod(Math.round(yaw / 22.5f), 16);
  }

  static int cursorCoordinate(int marker, int center, int zoom) {
    return (int) (((long) marker - center) * 2 / zoom);
  }

  static boolean cursorInRange(int marker, int center, int zoom) {
    double value = ((double) marker - center) * 2 / zoom;
    return value >= -128 && value <= 127;
  }

  static int tileX(int slot, int left) {
    return left + slot % 9;
  }

  static int tileZ(int slot, int top) {
    return top + slot / 9;
  }

  static int leftForCenter(double blockX, int origin, int bpp, int columns) {
    int desired = (int) Math.round((blockX - origin) / (18.0 * bpp) - 4.5);
    return clamp(desired, -4, Math.max(-4, columns - 5));
  }

  static int topForCenter(double blockZ, int origin, int bpp, int rows) {
    int desired = (int) Math.round((blockZ - origin) / (18.0 * bpp) - 2.5);
    return clamp(desired, -4, Math.max(-4, rows - 1));
  }

  static double centerX(int left, int origin, int bpp) {
    return origin + (left + 4.5) * 18.0 * bpp;
  }

  static double centerZ(int top, int origin, int bpp) {
    return origin + (top + 2.5) * 18.0 * bpp;
  }

  static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }
}
