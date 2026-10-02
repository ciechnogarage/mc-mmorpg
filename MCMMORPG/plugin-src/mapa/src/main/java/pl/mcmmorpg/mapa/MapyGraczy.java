package pl.mcmmorpg.mapa;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;

final class MapyGraczy {
  private final Map<UUID, Integer> ids = new LinkedHashMap<>();

  Integer id(UUID player) { return ids.get(player); }
  Map<UUID, Integer> entries() { return Map.copyOf(ids); }
  void put(UUID player, int id) { ids.put(player, id); }

  static MapyGraczy fromYaml(YamlConfiguration yaml) {
    MapyGraczy result = new MapyGraczy();
    for (String key : yaml.getKeys(false)) {
      try {
        UUID player = UUID.fromString(key);
        int id = yaml.getInt(key, -1);
        if (id >= 0) result.put(player, id);
      } catch (IllegalArgumentException ignored) {
        // Uszkodzony wpis nie blokuje pozostalych map.
      }
    }
    return result;
  }

  String toYaml() {
    YamlConfiguration yaml = new YamlConfiguration();
    ids.forEach((player, id) -> yaml.set(player.toString(), id));
    return yaml.saveToString();
  }
}
