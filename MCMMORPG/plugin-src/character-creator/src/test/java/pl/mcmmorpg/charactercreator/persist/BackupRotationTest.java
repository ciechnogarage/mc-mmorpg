package pl.mcmmorpg.charactercreator.persist;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Rotacja kopii zapasowych — jedyna czesc {@link CharacterStore}, ktora da sie sprawdzic
 * bez dzialajacego serwera (czyste java.nio, zero Bukkita).
 *
 * <p>Regula, ktora te testy pilnuja, wzięla sie z produkcji: rotacja po samej LICZBIE
 * zostawiala niecale dwa dni historii, bo kopia startowa powstawala przy kazdym z 93
 * restartow i wypychala prawdziwe kopie. Liczba i czas musza dzialac RAZEM.
 */
class BackupRotationTest {

  private static final int BACKUPS_KEPT = 10;
  private static final Duration RETENTION = Duration.ofDays(30);

  /** Kopia logiki z {@code CharacterStore#rotateBackups} — ta sama regula, bez Bukkita. */
  private static void rotate(Path dir) throws IOException {
    Instant cutoff = Instant.now().minus(RETENTION);
    try (Stream<Path> stream = Files.list(dir)) {
      List<Path> backups = stream
          .filter(p -> p.getFileName().toString().startsWith("characters-"))
          .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
          .toList();
      for (int i = BACKUPS_KEPT; i < backups.size(); i++) {
        Path candidate = backups.get(i);
        if (Files.getLastModifiedTime(candidate).toInstant().isBefore(cutoff)) {
          Files.deleteIfExists(candidate);
        }
      }
    }
  }

  private static Path backup(Path dir, String stamp, Duration age) throws IOException {
    Path file = dir.resolve("characters-" + stamp + "-startup.yml");
    Files.writeString(file, "schema-version: 2\n");
    Files.setLastModifiedTime(file, FileTime.from(Instant.now().minus(age)));
    return file;
  }

  @Test
  void swieze_kopie_przezywaja_seria_restartow(@TempDir Path dir) throws IOException {
    // 30 kopii z tego samego dnia — dokladnie scenariusz "93 restarty".
    List<Path> made = new ArrayList<>();
    for (int i = 0; i < 30; i++) {
      made.add(backup(dir, String.format("20260920-%06d", i), Duration.ofHours(1)));
    }
    rotate(dir);
    for (Path file : made) {
      assertTrue(Files.exists(file), "swieza kopia nie moze wypasc przez sama liczbe: " + file);
    }
  }

  @Test
  void stare_kopie_znikaja_dopiero_poza_gwarantowana_dziesiatka(@TempDir Path dir)
      throws IOException {
    for (int i = 0; i < 10; i++) {
      backup(dir, String.format("20260101-%06d", i), Duration.ofDays(200));
    }
    rotate(dir);
    assertEquals(10, Files.list(dir).count(),
        "dziesiec najnowszych zostaje nawet gdy wszystkie sa stare");

    backup(dir, "20260101-999999", Duration.ofDays(200));
    rotate(dir);
    assertEquals(10, Files.list(dir).count(), "jedenasta, stara kopia wypada");
  }

  @Test
  void kopia_mlodsza_niz_retencja_zostaje_takze_poza_dziesiatka(@TempDir Path dir)
      throws IOException {
    for (int i = 0; i < 15; i++) {
      backup(dir, String.format("20260920-%06d", i), Duration.ofDays(1));
    }
    rotate(dir);
    assertEquals(15, Files.list(dir).count(),
        "nic mlodszego niz retencja nie moze zostac skasowane");
  }
}
