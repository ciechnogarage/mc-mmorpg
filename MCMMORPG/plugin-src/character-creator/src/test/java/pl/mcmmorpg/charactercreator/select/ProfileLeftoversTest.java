package pl.mcmmorpg.charactercreator.select;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Pliki usunietej postaci: przeniesione do archiwum z zachowaniem sciezki, cudze nietkniete. */
class ProfileLeftoversTest {

  @TempDir
  Path plugins;

  @Test
  void przenosi_tylko_pliki_tej_postaci() throws Exception {
    UUID id = UUID.randomUUID();
    UUID inna = UUID.randomUUID();
    for (String dir : List.of("CoreTools/userdata", "MMOCore/userdata")) {
      Files.createDirectories(plugins.resolve(dir));
      Files.writeString(plugins.resolve(dir + "/" + id + ".yml"), "x");
      Files.writeString(plugins.resolve(dir + "/" + inna + ".yml"), "y");
    }
    Path archive = plugins.resolve("CharacterCreator/usuniete");
    List<String> moved = ProfileLeftovers.archive(plugins.toFile(), archive.toFile(), id,
        List.of("CoreTools/userdata/{id}.yml", "MMOCore/userdata/{id}.yml", "MMOItems/userdata/{id}.yml",
            "../poza/{id}.yml"));
    assertEquals(List.of("CoreTools/userdata/" + id + ".yml", "MMOCore/userdata/" + id + ".yml",
        "BLAD: niedozwolona sciezka ../poza/" + id + ".yml"), moved);
    assertFalse(Files.exists(plugins.resolve("CoreTools/userdata/" + id + ".yml")));
    assertTrue(Files.exists(archive.resolve(id + "/CoreTools/userdata/" + id + ".yml")));
    assertTrue(Files.exists(plugins.resolve("CoreTools/userdata/" + inna + ".yml")));
    assertTrue(Files.exists(plugins.resolve("MMOCore/userdata/" + inna + ".yml")));
  }
}
