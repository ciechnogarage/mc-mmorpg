package pl.mcmmorpg.charactercreator.creator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Stan sesji bez Bukkita: cykliczny wybor, blokada commitu i throttling straznika. */
class CreatorSessionTest {

  private static CreatorSession session() {
    return new CreatorSession(UUID.randomUUID(), UUID.randomUUID(), () -> { },
        List.of("czlowiek", "ork", "elf", "nieumarly"),
        List.of("wojownik", "mag", "akolita", "ninja"));
  }

  @Test
  void cyclingWrapsAroundInBothDirections() {
    CreatorSession session = session();
    assertEquals("czlowiek", session.currentRaceId());
    session.cycleRace(-1);
    assertEquals("nieumarly", session.currentRaceId());
    session.cycleRace(1);
    assertEquals("czlowiek", session.currentRaceId());
  }

  @Test
  void directSelectionIgnoresOutOfRangeIndexes() {
    CreatorSession session = session();
    session.selectRace(2);
    assertEquals("elf", session.currentRaceId());
    session.selectRace(99);
    assertEquals("elf", session.currentRaceId());
    session.selectRace(-1);
    assertEquals("elf", session.currentRaceId());
  }

  @Test
  void displayNameIsNeverEmpty() {
    CreatorSession session = session();
    assertFalse(session.hasName());
    assertFalse(session.displayName().isBlank());
    session.setPendingName("Gorak");
    assertEquals("Gorak", session.displayName());
  }

  /** Blokada podwojnego commitu: raz ustawiona, zostaje na zawsze. */
  @Test
  void confirmingIsOneWay() {
    CreatorSession session = session();
    assertFalse(session.confirming());
    session.markConfirming();
    assertTrue(session.confirming());
    session.markConfirming();
    assertTrue(session.confirming());
  }

  /** Straznik nie moze otwierac menu czesciej niz co okres cooldownu. */
  @Test
  void menuReopenIsThrottled() {
    CreatorSession session = session();
    assertTrue(session.menuReopenAllowed(10_000L, 1000L));
    assertFalse(session.menuReopenAllowed(10_500L, 1000L));
    assertTrue(session.menuReopenAllowed(11_100L, 1000L));
  }

  @Test
  void dialogMarkerExpires() {
    CreatorSession session = session();
    assertFalse(session.awaitingDialog());
    session.markDialogOpen();
    assertTrue(session.awaitingDialog());
    session.clearDialog();
    assertFalse(session.awaitingDialog());
  }
}
