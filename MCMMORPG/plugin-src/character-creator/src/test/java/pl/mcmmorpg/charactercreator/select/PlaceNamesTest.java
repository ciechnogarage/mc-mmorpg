package pl.mcmmorpg.charactercreator.select;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Napis "Miejsce" na karcie postaci: region WorldGuard, mapa MythicDungeons, regula z configu. */
class PlaceNamesTest {

  private static final String REGIONS = """
      regions:
          chw_perlowa_zatoka:
              min: {x: 172, y: -64, z: 316}
              max: {x: 268, y: 320, z: 412}
              flags: {greeting-title: '&6Perłowa Zatoka'}
              type: cuboid
              priority: 0
          maly_port:
              min: {x: 200, y: -64, z: 350}
              max: {x: 210, y: 320, z: 360}
              flags: {greeting: '&bPort'}
              type: cuboid
              priority: 0
          bez_napisu:
              min: {x: 0, y: 0, z: 0}
              max: {x: 1000, y: 300, z: 1000}
              flags: {pvp: deny}
              type: cuboid
              priority: 5
          wielokat:
              type: poly2d
              priority: 9
              flags: {greeting-title: 'Nie'}
          __global__:
              type: global
      """;

  @TempDir
  Path plugins;

  @Test
  void region_z_napisem_najbardziej_konkretny() throws Exception {
    List<PlaceNames.Region> regions = PlaceNames.parse(yaml(REGIONS));
    assertEquals(2, regions.size());
    assertEquals("Perłowa Zatoka", PlaceNames.pick(regions, 180, 70, 320).title());
    // W srodku obu (ten sam priorytet) — wygrywa mniejszy.
    assertEquals("Port", PlaceNames.pick(regions, 205.5, 70, 355.5).title());
    assertNull(PlaceNames.pick(regions, 100, 70, 100));
    // Granica max jest wlacznie (blok 268 nalezy do regionu).
    assertEquals("Perłowa Zatoka", PlaceNames.pick(regions, 268.9, 70, 330).title());
  }

  @Test
  void kolejnosc_zrodel() throws Exception {
    Path wg = plugins.resolve("WorldGuard/worlds/world");
    Files.createDirectories(wg);
    Files.writeString(wg.resolve("regions.yml"), REGIONS);
    Path map = plugins.resolve("MythicDungeons/maps/loch_001");
    Files.createDirectories(map);
    Files.writeString(map.resolve("config.yml"), "General:\n  DisplayName: '&dLoch 001 &8» &aKwietna Polana'\n");
    PlaceNames names = new PlaceNames(plugins.toFile(),
        List.of(Map.of("swiat", "world", "nazwa", "Kraina"), Map.of("swiat", "creator_.*", "nazwa", "x")));
    assertEquals("Perłowa Zatoka", names.describe("world", 180, 70, 320));
    assertEquals("Kraina", names.describe("world", 5000, 70, 5000));
    assertEquals("Loch 001 » Kwietna Polana", names.describe("loch_001_2", 0, 100, 0));
    assertNull(names.describe("nieznany", 0, 0, 0));
    assertNull(names.describe(null, 0, 0, 0));
  }

  private static YamlConfiguration yaml(String text) throws Exception {
    YamlConfiguration y = new YamlConfiguration();
    y.loadFromString(text);
    return y;
  }
}
