package pl.mcmmorpg.charactercreator.persist;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Znajduje SIEROTY: rekordy w characters.yml, dla ktorych nie istnieje juz zaden profil
 * MMOProfiles. Na produkcji bylo ich 9 z 18 — kazda z nich to slad po commicie, ktory
 * zapisal nasz rekord, a potem nie dokonczyl tworzenia profilu (patrz nowa kolejnosc
 * commitu w CharacterProfileModule#onConfirm).
 *
 * <p>Zrodlem prawdy jest {@code plugins/MMOProfiles/userdata/&lt;playerUuid&gt;.json}, gdzie
 * klucze obiektu {@code "Profiles"} to identyfikatory profili. Czytamy te pliki tylko do
 * odczytu i nigdy ich nie modyfikujemy — MMOProfiles jest ich wlascicielem.
 */
public final class OrphanScanner {

  /** Wynik skanu: co jest sierota i na ilu plikach udalo sie to ustalic. */
  public record Scan(List<UUID> orphans, int profilesSeen, int filesRead, int filesFailed,
      boolean userdataAvailable) {
  }

  private final File userdataDir;
  private final Logger logger;

  public OrphanScanner(File mmoProfilesDataFolder, Logger logger) {
    this.userdataDir = new File(mmoProfilesDataFolder, "userdata");
    this.logger = logger;
  }

  public Scan scan(Iterable<UUID> knownProfileIds) {
    List<UUID> orphans = new ArrayList<>();
    if (!userdataDir.isDirectory()) {
      logger.warning("Nie znaleziono " + userdataDir + " — bez tego katalogu nie da sie "
          + "odroznic sieroty od zywego profilu. Skan przerwany, nic nie usunieto.");
      return new Scan(List.of(), 0, 0, 0, false);
    }
    Set<String> live = new HashSet<>();
    int filesRead = 0;
    int filesFailed = 0;
    try (Stream<Path> stream = Files.list(userdataDir.toPath())) {
      for (Path path : stream.filter(p -> p.toString().endsWith(".json")).toList()) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
          JsonElement root = JsonParser.parseReader(reader);
          if (root != null && root.isJsonObject()) {
            JsonElement profiles = root.getAsJsonObject().get("Profiles");
            if (profiles != null && profiles.isJsonObject()) {
              JsonObject object = profiles.getAsJsonObject();
              live.addAll(object.keySet());
            }
          }
          filesRead++;
        } catch (IOException | RuntimeException ex) {
          // Jeden nieczytelny plik NIE moze zamienic cudzych zywych profili w "sieroty".
          filesFailed++;
          logger.log(Level.WARNING, "Nie udalo sie przeczytac " + path.getFileName(), ex);
        }
      }
    } catch (IOException ex) {
      logger.log(Level.WARNING, "Nie udalo sie wylistowac " + userdataDir, ex);
      return new Scan(List.of(), 0, filesRead, filesFailed, false);
    }

    if (filesFailed > 0) {
      logger.warning(filesFailed + " plikow userdata bylo nieczytelnych — skan sierot "
          + "zglasza wynik, ale NIE nadaje sie do automatycznego czyszczenia.");
    }
    for (UUID profileId : knownProfileIds) {
      if (!live.contains(profileId.toString())) {
        orphans.add(profileId);
      }
    }
    return new Scan(List.copyOf(orphans), live.size(), filesRead, filesFailed, filesFailed == 0);
  }
}
