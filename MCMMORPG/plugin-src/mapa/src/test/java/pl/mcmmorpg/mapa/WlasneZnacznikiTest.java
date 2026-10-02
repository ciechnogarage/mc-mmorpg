package pl.mcmmorpg.mapa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WlasneZnacznikiTest {
  @TempDir Path temp;

  @Test void zapisIOdczytYamlWPamieci() throws Exception {
    List<WlasneZnaczniki.Entry> list = List.of(new WlasneZnaczniki.Entry(
        "1234abcd", "loch_001", -10, 25, "Mój dom", "oak_door"));
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.loadFromString(WlasneZnaczniki.serialize(list));
    assertEquals(list, WlasneZnaczniki.parse(yaml));
  }

  @Test void limitBlokujeDodanie() {
    WlasneZnaczniki own = new WlasneZnaczniki(temp.toFile(), Logger.getAnonymousLogger());
    UUID owner = UUID.randomUUID();
    try {
      assertTrue(own.add(owner, new WlasneZnaczniki.Entry("1234abcd", "world", 1, 2,
          "Pierwszy", "pin_czerwona"), 1));
      assertFalse(own.add(owner, new WlasneZnaczniki.Entry("5678abcd", "world", 3, 4,
          "Drugi", "pin_biala"), 1));
    } finally { own.close(); }
  }
}
