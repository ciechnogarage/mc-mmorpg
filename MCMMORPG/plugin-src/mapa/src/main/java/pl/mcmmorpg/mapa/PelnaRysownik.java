package pl.mcmmorpg.mapa;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import org.bukkit.map.MapPalette;

final class PelnaRysownik {
  record Obraz(BufferedImage base, BufferedImage image) {}
  record Dane(int width, int height, String world, double x, double z, double zoom,
      MapaData.Terrain terrain, List<MapaData.Marker> markers, List<MapaData.Tab> tabs,
      Map<String, BufferedImage> icons, boolean playerVisible, double playerX, double playerZ,
      float playerYaw, PelnaMath.Punkt cursor) {}

  private final int[] colors = new int[256];

  @SuppressWarnings("deprecation")
  PelnaRysownik() {
    // paleta map ma mniej niz 256 kolorow (1.21.11: 248); bajty spoza niej zostaja 0 = przezroczyste
    for (int i = 1; i < colors.length; i++) {
      try { colors[i] = MapPalette.getColor((byte) i).getRGB(); }
      catch (IndexOutOfBoundsException koniecPalety) { break; }
    }
  }

  Obraz rysuj(Dane data, BufferedImage previous) {
    BufferedImage base = previous == null ? podklad(data) : previous;
    BufferedImage image = new BufferedImage(data.width(), data.height(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    g.drawImage(base, 0, 0, null);
    if (data.cursor() != null) {
      int x = (int) Math.round(data.cursor().u()), y = (int) Math.round(data.cursor().v());
      g.setColor(Color.BLACK);
      g.drawLine(x - 4, y, x + 4, y);
      g.drawLine(x, y - 4, x, y + 4);
      g.setColor(Color.WHITE);
      g.drawLine(x - 3, y, x + 3, y);
      g.drawLine(x, y - 3, x, y + 3);
    }
    g.dispose();
    return new Obraz(base, image);
  }

  private BufferedImage podklad(Dane data) {
    int width = data.width(), height = data.height();
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    int parchment = 0xffd5c98c;   // dokladnie kolor palety map (piasek, odcien 1): bez przekłamania przy konwersji
    MapaData.Terrain terrain = data.terrain();
    for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
      byte pixel = terrain == null ? 0 : terrain.fullPixel(data.x(), data.z(),
          data.zoom(), x, y, width / 2, height / 2);
      image.setRGB(x, y, pixel == 0 ? parchment : colors[pixel & 255]);
    }
    Graphics2D g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    g.setFont(new Font("Dialog", Font.BOLD, 11));
    for (MapaData.Marker marker : data.markers()) {
      int x = (int) Math.round(width / 2.0 + (marker.x() - data.x()) / data.zoom());
      int y = (int) Math.round(height / 2.0 + (marker.z() - data.z()) / data.zoom());
      if (x < -40 || y < -40 || x >= width + 40 || y >= height + 40) continue;
      BufferedImage icon = data.icons().get(marker.icon());
      int size = data.zoom() <= 1 ? 32 : 16;
      if (icon != null) g.drawImage(icon, x - size / 2, y - size / 2, size, size, null);
      g.setColor(Color.BLACK);
      g.drawString(marker.name(), x + 1, y + size / 2 + 12);
      g.setColor(Color.WHITE);
      g.drawString(marker.name(), x, y + size / 2 + 11);
    }
    if (data.playerVisible()) {
      int x = (int) Math.round(width / 2.0 + (data.playerX() - data.x()) / data.zoom());
      int y = (int) Math.round(height / 2.0 + (data.playerZ() - data.z()) / data.zoom());
      Polygon arrow = new Polygon(new int[] {0, -6, 0, 6}, new int[] {-9, 7, 3, 7}, 4);
      java.awt.geom.AffineTransform old = g.getTransform();
      g.translate(x, y);
      g.rotate(Math.toRadians(180 + data.playerYaw()));
      g.setColor(Color.WHITE);
      g.fill(arrow);
      g.setColor(Color.BLACK);
      g.setStroke(new BasicStroke(1.5f));
      g.draw(arrow);
      g.setTransform(old);
    }
    g.setColor(new Color(0, 0, 0, 180));
    g.fillRect(0, 0, width, 20);
    g.fillRect(0, height - 14, width, 14);
    for (int i = 0; i < data.tabs().size() && i * 64 < width - 24; i++) {
      MapaData.Tab tab = data.tabs().get(i);
      String label = tab.name().length() > 9 ? tab.name().substring(0, 9) : tab.name();
      g.setColor(Color.WHITE);
      g.drawString(label, i * 64 + 3, 13);
      if (tab.world().equals(data.world())) {
        g.setColor(new Color(0xe0c38c));
        g.fillRect(i * 64 + 2, 17, 60, 2);
      }
    }
    g.setColor(Color.WHITE);
    g.drawString("✕", width - 17, 14);
    g.drawString("Przybliżenie: " + data.zoom() + "  ·  PPM — przeciągnij · kółko — zoom · Shift — wyjście",
        4, height - 3);
    g.dispose();
    return image;
  }
}
