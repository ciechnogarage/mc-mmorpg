package pl.mcmmorpg.charactercreator.creator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Czysta geometria menu. Zadna z tych metod nie dotyka serwera, a mimo to zadna nie byla
 * dotad przetestowana — a to na nich stoi caly uklad ekranu i rozpoznawanie klikniec.
 *
 * <p>Testow fontu przesuwajacego juz tu nie ma: minimalistyczne UI nie rysuje ramy, wiec
 * {@code CreatorMenu.shift()} przestalo istniec razem z nia.
 */
class CreatorMenuTest {

  @Test
  void raceAndClassSlotsMapToTheirIndex() {
    for (int i = 0; i < CreatorMenu.SLOTS_RACE.length; i++) {
      assertEquals("race_" + i, CreatorMenu.actionOf(CreatorMenu.SLOTS_RACE[i]));
    }
    for (int i = 0; i < CreatorMenu.SLOTS_CLASS.length; i++) {
      assertEquals("class_" + i, CreatorMenu.actionOf(CreatorMenu.SLOTS_CLASS[i]));
    }
  }

  @Test
  void nameAndConfirmSlotsAreRecognised() {
    assertEquals("set_name", CreatorMenu.actionOf(CreatorMenu.SLOT_NAME));
    assertEquals("confirm", CreatorMenu.actionOf(CreatorMenu.SLOT_CONFIRM));
  }

  /**
   * Regresja: akcja "random_name" byla obslugiwana w listenerze, ale {@code actionOf} nigdy
   * jej nie zwracala — losowanie imienia dziala wylacznie prawym przyciskiem na slocie imienia.
   */
  @Test
  void thereIsNoRandomNameSlot() {
    for (int rawSlot = 0; rawSlot < 63; rawSlot++) {
      String action = CreatorMenu.actionOf(rawSlot);
      assertTrue(action == null || !action.equals("random_name"),
          "slot " + rawSlot + " zwrocil martwa akcje random_name");
    }
  }

  @Test
  void unusedSlotsHaveNoAction() {
    assertNull(CreatorMenu.actionOf(1));
    assertNull(CreatorMenu.actionOf(13));
    assertNull(CreatorMenu.actionOf(62));
  }

  /** Raw 27..53 to plecak (indeks - 18), raw 54..62 to hotbar (indeks - 54). */
  @Test
  void playerIndexMapsRawSlotsToInventoryIndexes() {
    assertEquals(9, CreatorMenu.playerIndex(27));
    assertEquals(27, CreatorMenu.playerIndex(45));
    assertEquals(35, CreatorMenu.playerIndex(53));
    assertEquals(0, CreatorMenu.playerIndex(54));
    assertEquals(4, CreatorMenu.playerIndex(58));
  }

  /**
   * Sweeper czysci sloty 0..35 gracza z pominieciem tych, w ktorych siedza NASZE ikony.
   * Gdyby ten zbior sie rozjechal z ukladem, straznik kasowalby wlasne przyciski co 10 tickow.
   */
  @Test
  void slotsInUseCoverEveryIconPlacedInThePlayerInventory() {
    for (int rawSlot : CreatorMenu.SLOTS_RACE) {
      if (rawSlot >= 27) {
        assertTrue(CreatorMenu.PLAYER_SLOTS_IN_USE.contains(CreatorMenu.playerIndex(rawSlot)),
            "rasa na raw " + rawSlot + " nie jest chroniona przed sweeperem");
      }
    }
    for (int rawSlot : CreatorMenu.SLOTS_CLASS) {
      if (rawSlot >= 27) {
        assertTrue(CreatorMenu.PLAYER_SLOTS_IN_USE.contains(CreatorMenu.playerIndex(rawSlot)),
            "klasa na raw " + rawSlot + " nie jest chroniona przed sweeperem");
      }
    }
    assertTrue(CreatorMenu.PLAYER_SLOTS_IN_USE.contains(CreatorMenu.playerIndex(CreatorMenu.SLOT_CONFIRM)));
  }
}
