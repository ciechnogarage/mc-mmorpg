package pl.mcmmorpg.mapa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class MapyGraczyTest {
  @Test
  void zapisIOdczytYamlWPamieci() throws Exception {
    UUID ala = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID ola = UUID.fromString("00000000-0000-0000-0000-000000000002");
    MapyGraczy registry = new MapyGraczy();
    registry.put(ala, 30001);
    registry.put(ola, 30002);
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.loadFromString(registry.toYaml());

    MapyGraczy restored = MapyGraczy.fromYaml(yaml);
    assertEquals(30001, restored.id(ala));
    assertEquals(30002, restored.id(ola));
    assertNull(restored.id(UUID.fromString("00000000-0000-0000-0000-000000000003")));
  }
}
