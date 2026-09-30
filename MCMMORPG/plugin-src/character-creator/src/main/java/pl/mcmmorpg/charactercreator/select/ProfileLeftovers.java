package pl.mcmmorpg.charactercreator.select;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pliki postaci, ktore zostaja po {@code removeProfile}. Zmierzone na klonie 30.09 dla trzech
 * trwale usunietych postaci: MMOProfiles zdejmuje profil, a na dysku zostaja
 * {@code CoreTools/userdata/<id>.yml} (ekonomia, zmienne, PlayerVaults — w tym zawartosc Sakwy,
 * uprawnienia CorePerms), {@code MMOCore/userdata}, {@code MMOItems/userdata} i
 * {@code MMOInventory/userdata}. CoreTools w ogole nie waliduje ProfileRemoveEvent, pozostale
 * waliduja, ale plikow nie kasuja. Chowance archiwizuja swoje same (archiwum/*.usuniety-*.json).
 *
 * <p>Nie kasujemy — PRZENOSIMY do {@code CharacterCreator/usuniete/<id>/} z zachowaniem sciezki,
 * tak jak robia Chowance: pomylke da sie cofnac recznie (przeniesc pliki z powrotem), a katalog
 * mozna czyscic okresowo. Szablony w {@code wybor-postaci.po-usunieciu} ({id} = UUID profilu).
 */
public final class ProfileLeftovers {

  public static final List<String> DEFAULT = List.of(
      "CoreTools/userdata/{id}.yml",
      "MMOCore/userdata/{id}.yml",
      "MMOItems/userdata/{id}.yml",
      "MMOInventory/userdata/{id}.yml");

  private ProfileLeftovers() {
  }

  /**
   * @return przeniesione sciezki (wzgledne wobec katalogu plugins); bledy jako "BLAD: ..."
   */
  public static List<String> archive(File pluginsDir, File archiveRoot, UUID profileId, List<String> templates) {
    List<String> out = new ArrayList<>();
    for (String template : templates) {
      String rel = template.replace("{id}", profileId.toString());
      if (rel.contains("..") || rel.startsWith("/")) {
        out.add("BLAD: niedozwolona sciezka " + rel);
        continue;
      }
      File src = new File(pluginsDir, rel);
      if (!src.isFile()) {
        continue;
      }
      File dst = new File(new File(archiveRoot, profileId.toString()), rel);
      try {
        Files.createDirectories(dst.getParentFile().toPath());
        Files.move(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
        out.add(rel);
      } catch (IOException ex) {
        out.add("BLAD: " + rel + ": " + ex);
      }
    }
    return out;
  }
}
