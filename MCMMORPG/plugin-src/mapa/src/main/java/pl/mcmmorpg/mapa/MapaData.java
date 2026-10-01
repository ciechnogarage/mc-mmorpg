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
  record Terrain(int x0, int z0, int width, int height, byte[] pixels) {
    byte color(int x, int z) {
      long ix = (long) x - x0;
      long iz = (long) z - z0;
      if (ix < 0 || iz < 0 || ix >= width || iz >= height) return 0;
      return pixels[(int) (iz * width + ix)];
    }
  }

  record Level(int bpp, int columns, int rows) {}
  record Grid(String world, int x0, int z0, List<Level> levels) {
    Level level(int index) { return levels.get(index); }
  }

  record Marker(String world, int x, int z, String name, String type, List<String> description) {}

  private final Map<String, Terrain> terrain;
  private final Map<String, Grid> grids;
  private final List<Marker> markers;

  private MapaData(Map<String, Terrain> terrain, Map<String, Grid> grids, List<Marker> markers) {
    this.terrain = Map.copyOf(terrain);
    this.grids = Map.copyOf(grids);
    this.markers = List.copyOf(markers);
  }

  Terrain terrain(String world) { return terrain.get(world); }
  Grid grid(String world) { return grids.get(world); }
  List<Marker> markers() { return markers; }

  static MapaData load(File folder, ConfigurationSection config, Logger log) {
    Map<String, Terrain> terrain = new HashMap<>();
    Map<String, Grid> grids = new HashMap<>();
    List<Marker> markers = new ArrayList<>();

    for (Map<?, ?> raw : config.getMapList("znaczniki")) {
      String world = String.valueOf(raw.containsKey("swiat") ? raw.get("swiat") : "world");
      String name = String.valueOf(raw.containsKey("nazwa") ? raw.get("nazwa") : "");
      String type = String.valueOf(raw.containsKey("typ") ? raw.get("typ") : "strefa");
      int x = number(raw.get("x"));
      int z = number(raw.get("z"));
      List<String> description = new ArrayList<>();
      Object lines = raw.get("opis");
      if (lines instanceof List<?> list) for (Object line : list) description.add(String.valueOf(line));
      markers.add(new Marker(world, x, z, name, type, List.copyOf(description)));
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
        terrain.put(world, new Terrain(info.getInt("x0"), info.getInt("z0"), w, h, pixels));
      } catch (IOException ex) {
        log.warning("Nie można wczytać terenu " + world + ": " + ex.getMessage());
      }
    }
    return new MapaData(terrain, grids, markers);
  }

  private static int number(Object value) {
    return value instanceof Number n ? n.intValue() : 0;
  }
}
