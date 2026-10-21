package pl.mcmmorpg.charactercreator.persist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Kosz postaci: termin, liczenie dni i to, ze przezywa restart (zapis do trash.yml). */
class TrashStoreTest {

  private static final Duration TYDZIEN = Duration.ofDays(7);
  private static final Instant T0 = Instant.parse("2026-09-30T10:00:00Z");
  private static final UUID GRACZ = UUID.randomUUID();

  @TempDir
  Path dir;

  @Test
  void termin_i_dni() {
    TrashStore.Entry e = new TrashStore.Entry(UUID.randomUUID(), GRACZ, "Halruk", T0);
    assertFalse(e.expired(T0.plus(Duration.ofDays(7)).minusSeconds(1), TYDZIEN));
    assertTrue(e.expired(T0.plus(Duration.ofDays(7)), TYDZIEN));
    assertEquals(7, e.daysLeft(T0, TYDZIEN));
    assertEquals(1, e.daysLeft(T0.plus(Duration.ofDays(6)).plusSeconds(3600), TYDZIEN));
    assertEquals(0, e.daysLeft(T0.plus(Duration.ofDays(8)), TYDZIEN));
  }

  @Test
  void przezywa_restart_i_przywracanie() {
    File file = dir.resolve("trash.yml").toFile();
    TrashStore store = new TrashStore(file, Logger.getAnonymousLogger());
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    UUID obcy = UUID.randomUUID();
    store.trash(new TrashStore.Entry(a, GRACZ, "Ąłek", T0));
    store.trash(new TrashStore.Entry(b, GRACZ, "Bor", T0.minus(Duration.ofDays(8))));
    store.trash(new TrashStore.Entry(UUID.randomUUID(), obcy, "Cudzy", T0.minus(Duration.ofDays(9))));

    TrashStore reloaded = new TrashStore(file, Logger.getAnonymousLogger());
    reloaded.load();
    assertTrue(reloaded.isTrashed(a));
    assertEquals("Ąłek", reloaded.get(a).name());
    assertEquals(T0, reloaded.get(a).trashedAt());
    // Najdawniej wyrzucona pierwsza; tylko wlasne.
    List<TrashStore.Entry> mine = reloaded.byOwner(GRACZ);
    assertEquals(List.of(b, a), mine.stream().map(TrashStore.Entry::profileId).toList());
    assertEquals(List.of(b), reloaded.expired(GRACZ, T0, TYDZIEN).stream().map(TrashStore.Entry::profileId).toList());

    assertTrue(reloaded.restore(a));
    assertFalse(reloaded.restore(a));
    TrashStore again = new TrashStore(file, Logger.getAnonymousLogger());
    again.load();
    assertFalse(again.isTrashed(a));
    assertTrue(again.isTrashed(b));
  }
}
