package pl.mcmmorpg.mapa;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

final class MapaData {
  record Terrain(int x0, int z0, int width, int height, byte[] pixels, byte[] blocks, byte[] textures) {
    byte color(int x, int z) {
      long ix = (long) x - x0;
      long iz = (long) z - z0;
      if (ix < 0 || iz < 0 || ix >= width || iz >= height) return 0;
      return pixels[(int) (iz * width + ix)];
    }

    byte pixel(int centerX, int centerZ, double zoom, int px, int py) {
      if (zoom >= 1) return color((int) Math.floor(centerX + (px - 64) * zoom),
          (int) Math.floor(centerZ + (py - 64) * zoom));
      int k = (int) Math.round(1 / zoom);
      int x = centerX + Math.floorDiv(px - 64, k);
      int z = centerZ + Math.floorDiv(py - 64, k);
      if (blocks == null || textures == null) return color(x, z);
      long ix = (long) x - x0, iz = (long) z - z0;
      if (ix < 0 || iz < 0 || ix >= width || iz >= height) return 0;
      int offset = (int) (iz * width + ix) * 2;
      int value = (blocks[offset] & 255) | ((blocks[offset + 1] & 255) << 8);
      int index = value & 0x3fff;
      int shade = value >>> 14;
      long texture = (long) index * 48 + shade * 16L;
      if (index == 0 || shade > 2 || texture + 16 > textures.length) return color(x, z);
      int sx = Math.floorMod(px - 64, k) * (4 / k);
      int sy = Math.floorMod(py - 64, k) * (4 / k);
      return textures[(int) texture + sy * 4 + sx];
    }

    byte fullPixel(double centerX, double centerZ, double zoom, int px, int py,
        int halfWidth, int halfHeight) {
      double wx = centerX + (px - halfWidth) * zoom;
      double wz = centerZ + (py - halfHeight) * zoom;
      int x = (int) Math.floor(wx), z = (int) Math.floor(wz);
      if (zoom >= 1 || blocks == null || textures == null) return color(x, z);
      long ix = (long) x - x0, iz = (long) z - z0;
      if (ix < 0 || iz < 0 || ix >= width || iz >= height) return 0;
      int offset = (int) (iz * width + ix) * 2;
      int value = (blocks[offset] & 255) | ((blocks[offset + 1] & 255) << 8);
      int index = value & 0x3fff, shade = value >>> 14;
      long texture = (long) index * 48 + shade * 16L;
      if (index == 0 || shade > 2 || texture + 16 > textures.length) return color(x, z);
      int sx = Math.min(3, (int) Math.floor((wx - x) * 4));
      int sy = Math.min(3, (int) Math.floor((wz - z) * 4));
      return textures[(int) texture + sy * 4 + sx];
    }
  }

  record Level(int bpp, int columns, int rows) {}
  record Grid(String world, int x0, int z0, List<Level> levels) {
    Level level(int index) { return levels.get(index); }
  }

  record Marker(String world, int x, int z, String name, String type, String icon,
      List<String> description) {}
  record Tab(String world, String name, String icon) {}

  private final Map<String, Terrain> terrain;
  private final Map<String, Grid> grids;
  private final List<Marker> markers;
  private final List<Tab> tabs;

  private MapaData(Map<String, Terrain> terrain, Map<String, Grid> grids, List<Marker> markers, List<Tab> tabs) {
    this.terrain = Map.copyOf(terrain);
    this.grids = Map.copyOf(grids);
    this.markers = List.copyOf(markers);
    this.tabs = List.copyOf(tabs);
  }

  static String klucz(String world) { return world.replaceFirst("_\\d+$", ""); }
  private static <T> T find(Map<String, T> values, String world) {
    T exact = values.get(world);
    return exact != null ? exact : values.get(klucz(world));
  }

  Terrain terrain(String world) { return find(terrain, world); }
  Grid grid(String world) { return find(grids, world); }
  List<Marker> markers() { return markers; }
  List<Marker> markers(String world) {
    String key = markers.stream().anyMatch(marker -> marker.world().equals(world)) ? world : klucz(world);
    return markers.stream().filter(marker -> marker.world().equals(key)).toList();
  }
  List<Tab> tabs() { return tabs; }
  static Tab tab(List<Tab> tabs, int slot) { return slot >= 0 && slot < tabs.size() ? tabs.get(slot) : null; }

  static MapaData load(File folder, ConfigurationSection config, Logger log) {
    Map<String, Terrain> terrain = new HashMap<>();
    Map<String, Grid> grids = new HashMap<>();
    List<Marker> markers = new ArrayList<>();
    List<Tab> tabs = new ArrayList<>();

    for (Map<?, ?> raw : config.getMapList("zakladki")) {
      if (tabs.size() == 9) break;
      String world = String.valueOf(raw.get("swiat"));
      if (!world.matches("[a-z0-9_.-]+")) continue;
      tabs.add(new Tab(world, String.valueOf(raw.containsKey("nazwa") ? raw.get("nazwa") : world),
          String.valueOf(raw.containsKey("ikona") ? raw.get("ikona") : "mapa:pusty")));
    }

    for (Map<?, ?> raw : config.getMapList("znaczniki")) {
      String world = String.valueOf(raw.containsKey("swiat") ? raw.get("swiat") : "world");
      String name = String.valueOf(raw.containsKey("nazwa") ? raw.get("nazwa") : "");
      String type = String.valueOf(raw.containsKey("typ") ? raw.get("typ") : "strefa");
      int x = number(raw.get("x"));
      int z = number(raw.get("z"));
      List<String> description = new ArrayList<>();
      Object lines = raw.get("opis");
      if (lines instanceof List<?> list) for (Object line : list) description.add(String.valueOf(line));
      String icon = String.valueOf(raw.containsKey("ikona") ? raw.get("ikona") : "");
      markers.add(new Marker(world, x, z, name, type, icon, List.copyOf(description)));
    }

    File tilesFile = new File(folder, "kafle.json");
    if (!tilesFile.isFile()) log.warning("Brak kafle.json; atlas jest niedostępny.");
    else {
      ConfigurationSection worlds = YamlConfiguration.loadConfiguration(tilesFile).getConfigurationSection("swiaty");
      if (worlds == null) log.warning("Niepoprawny kafle.json: brak swiaty.");
      else for (String world : worlds.getKeys(false)) {
        ConfigurationSection section = worlds.getConfigurationSection(world);
        if (section == null || !world.matches("[a-z0-9_.-]+")) {
          log.warning("Pominięto niepoprawny świat w kafle.json: " + world);
          continue;
        }
        List<Level> levels = new ArrayList<>();
        for (Map<?, ?> raw : section.getMapList("poziomy")) {
          int bpp = number(raw.get("bpp"));
          int columns = number(raw.get("kol"));
          int rows = number(raw.get("wier"));
          if (bpp > 0 && columns > 0 && rows > 0) levels.add(new Level(bpp, columns, rows));
        }
        if (levels.isEmpty()) log.warning("Brak poprawnych poziomów kafli: " + world);
        else grids.put(world, new Grid(world, section.getInt("x0"), section.getInt("z0"), List.copyOf(levels)));
      }
    }

    File worldsFolder = new File(folder, "swiaty");
    byte[] textures = null;
    File texturesFile = new File(folder, "tekstury.bin");
    if (texturesFile.isFile()) try {
      byte[] loaded = Files.readAllBytes(texturesFile.toPath());
      if (loaded.length % 48 == 0) textures = loaded;
      else log.warning("Niepoprawna długość tekstur: tekstury.bin");
    } catch (IOException ex) {
      log.warning("Nie można wczytać tekstur: " + ex.getMessage());
    }
    File[] metadata = worldsFolder.listFiles((dir, name) -> name.matches("[a-z0-9_.-]+\\.json"));
    if (metadata == null || metadata.length == 0)
      log.warning("Brak danych w katalogu swiaty; mapa w ręce pokaże brak danych.");
    else for (File json : metadata) {
      String world = json.getName().substring(0, json.getName().length() - 5);
      File bin = new File(worldsFolder, world + ".bin");
      if (!bin.isFile()) {
        log.warning("Brak pliku terenu: " + bin.getName());
        continue;
      }
      ConfigurationSection info = YamlConfiguration.loadConfiguration(json);
      int w = info.getInt("w"), h = info.getInt("h");
      if (w <= 0 || h <= 0 || (long) w * h > Integer.MAX_VALUE) {
        log.warning("Niepoprawne wymiary terenu: " + json.getName());
        continue;
      }
      try {
        if (Files.size(bin.toPath()) != (long) w * h) {
          log.warning("Niepoprawna długość terenu: " + bin.getName());
          continue;
        }
        byte[] pixels = Files.readAllBytes(bin.toPath());
        if (pixels.length != (long) w * h) {
          log.warning("Niepoprawna długość terenu: " + bin.getName());
          continue;
        }
        byte[] blocks = null;
        File blk = new File(worldsFolder, world + ".blk");
        if (blk.isFile()) {
          if (Files.size(blk.toPath()) == (long) w * h * 2) blocks = Files.readAllBytes(blk.toPath());
          else log.warning("Niepoprawna długość bloków: " + blk.getName());
        }
        terrain.put(world, new Terrain(info.getInt("x0"), info.getInt("z0"), w, h, pixels,
            blocks, textures));
      } catch (IOException ex) {
        log.warning("Nie można wczytać terenu " + world + ": " + ex.getMessage());
      }
    }
    return new MapaData(terrain, grids, markers, tabs);
  }

  private static int number(Object value) {
    return value instanceof Number n ? n.intValue() : 0;
  }
}
