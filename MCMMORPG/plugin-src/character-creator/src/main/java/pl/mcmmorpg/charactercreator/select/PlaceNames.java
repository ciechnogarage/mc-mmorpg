package pl.mcmmorpg.charactercreator.select;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Nazwa miejsca, w ktorym postac zostala: "Perlowa Zatoka", "Loch 001 » Kwietna Polana".
 *
 * <p>Zrodla, po kolei:
 * <ol>
 *   <li>region WorldGuard z flaga {@code greeting-title} (albo {@code greeting}) — ten sam napis,
 *       ktory gracz widzi przy wejsciu do regionu. Czytany wprost z {@code regions.yml}
 *       (bez API WorldGuarda: to dane tylko do odczytu, a zaleznosc od WG+WorldEdit dla
 *       jednego napisu na karcie bylaby przesada). Obslugiwane prostopadlosciany — tylko
 *       takie sa na world1; inne typy sa pomijane;</li>
 *   <li>swiat instancji MythicDungeons ({@code <mapa>_<n>}) — {@code DisplayName} mapy;</li>
 *   <li>{@code wybor-postaci.miejsca} w config.yml: nazwa swiata (wyrazenie regularne) → napis;</li>
 * </ol>
 * Nic nie pasuje = {@code null} (karta nie pokazuje wtedy wiersza "Miejsce").
 */
public final class PlaceNames {

  /** Prostopadloscian z napisem. */
  public record Region(String id, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
      int priority, String title) {

    boolean contains(double x, double y, double z) {
      return x >= minX && x < maxX + 1 && y >= minY && y < maxY + 1 && z >= minZ && z < maxZ + 1;
    }

    long volume() {
      return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }
  }

  private record Cached(long modified, List<Region> regions) {}

  private record WorldRule(Pattern pattern, String name) {}

  private static final Pattern COLOR = Pattern.compile("(?i)[&§][0-9a-fk-orx]");
  private static final Pattern INSTANCE = Pattern.compile("^(.+)_\\d+$");

  private final File pluginsDir;
  private final List<WorldRule> rules = new ArrayList<>();
  private final Map<String, Cached> regionCache = new HashMap<>();
  private final Map<String, String> dungeonNames = new HashMap<>();

  /** @param places lista {@code {swiat: <regex nazwy swiata>, nazwa: <napis>}} */
  public PlaceNames(File pluginsDir, List<Map<?, ?>> places) {
    this.pluginsDir = pluginsDir;
    if (places != null) {
      for (Map<?, ?> m : places) {
        Object world = m.get("swiat");
        Object name = m.get("nazwa");
        if (world != null && name != null && !name.toString().isBlank()) {
          rules.add(new WorldRule(Pattern.compile(world.toString()), name.toString()));
        }
      }
    }
  }

  /** @return napis dla gracza albo {@code null} */
  public synchronized String describe(String world, double x, double y, double z) {
    if (world == null || world.isBlank()) {
      return null;
    }
    Region region = pick(regions(world), x, y, z);
    if (region != null) {
      return region.title();
    }
    String dungeon = dungeonName(world);
    if (dungeon != null) {
      return dungeon;
    }
    for (WorldRule rule : rules) {
      if (rule.pattern().matcher(world).matches()) {
        return rule.name();
      }
    }
    return null;
  }

  /** Region zawierajacy punkt: najwyzszy priorytet, przy remisie najmniejszy (najbardziej konkretny). */
  static Region pick(List<Region> regions, double x, double y, double z) {
    Region best = null;
    for (Region r : regions) {
      if (!r.contains(x, y, z)) {
        continue;
      }
      if (best == null || r.priority() > best.priority()
          || (r.priority() == best.priority() && r.volume() < best.volume())) {
        best = r;
      }
    }
    return best;
  }

  /** Prostopadlosciany z napisem z {@code regions.yml} WorldGuarda. */
  static List<Region> parse(YamlConfiguration yml) {
    List<Region> out = new ArrayList<>();
    ConfigurationSection root = yml.getConfigurationSection("regions");
    if (root == null) {
      return out;
    }
    for (String id : root.getKeys(false)) {
      ConfigurationSection r = root.getConfigurationSection(id);
      if (r == null || !"cuboid".equalsIgnoreCase(r.getString("type"))) {
        continue;
      }
      ConfigurationSection flags = r.getConfigurationSection("flags");
      String title = flags == null ? null : flags.getString("greeting-title", flags.getString("greeting"));
      title = plain(title);
      ConfigurationSection min = r.getConfigurationSection("min");
      ConfigurationSection max = r.getConfigurationSection("max");
      if (title == null || min == null || max == null) {
        continue;
      }
      out.add(new Region(id, min.getInt("x"), min.getInt("y"), min.getInt("z"),
          max.getInt("x"), max.getInt("y"), max.getInt("z"), r.getInt("priority", 0), title));
    }
    return out;
  }

  static String plain(String legacy) {
    if (legacy == null) {
      return null;
    }
    String out = COLOR.matcher(legacy).replaceAll("").trim();
    return out.isEmpty() ? null : out;
  }

  private List<Region> regions(String world) {
    File file = new File(pluginsDir, "WorldGuard/worlds/" + world + "/regions.yml");
    if (!file.isFile()) {
      return List.of();
    }
    Cached cached = regionCache.get(world);
    if (cached == null || cached.modified() != file.lastModified()) {
      cached = new Cached(file.lastModified(), parse(YamlConfiguration.loadConfiguration(file)));
      regionCache.put(world, cached);
    }
    return cached.regions();
  }

  private String dungeonName(String world) {
    var m = INSTANCE.matcher(world);
    String map = m.matches() ? m.group(1) : world;
    return dungeonNames.computeIfAbsent(map, key -> {
      File cfg = new File(pluginsDir, "MythicDungeons/maps/" + key + "/config.yml");
      if (!cfg.isFile()) {
        return null;
      }
      YamlConfiguration yml = YamlConfiguration.loadConfiguration(cfg);
      String name = yml.getString("General.DisplayName", findDisplayName(yml));
      return plain(name);
    });
  }

  private static String findDisplayName(ConfigurationSection s) {
    Map<String, Object> all = new LinkedHashMap<>(s.getValues(true));
    for (Map.Entry<String, Object> e : all.entrySet()) {
      if (e.getKey().endsWith("DisplayName") && e.getValue() instanceof String str) {
        return str;
      }
    }
    return null;
  }
}
