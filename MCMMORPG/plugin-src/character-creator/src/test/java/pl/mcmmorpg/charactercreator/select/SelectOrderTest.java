package pl.mcmmorpg.charactercreator.select;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Ekran wyboru: stala kolejnosc, domyslnie zaznaczona ostatnio grana, pozycja po usunieciu.
 * Wlasciciel 30.09: "wybor postaci bywa mozolny", "usuwanie tak samo" — to sa reguly, ktore
 * o tym decyduja, wiec maja testy.
 */
class SelectOrderTest {

  private record P(String name, Instant used) {}

  private static final Instant T0 = Instant.parse("2026-09-30T10:00:00Z");
  private static final ZoneId PL = ZoneId.of("Europe/Warsaw");

  @Test
  void ostatnio_grana_jest_zaznaczona_ale_nie_przesuwana() {
    List<P> list = List.of(new P("A", T0), new P("B", T0.plusSeconds(60)), new P("C", T0.minusSeconds(60)));
    assertEquals(1, SelectOrder.lastUsedIndex(list, P::used));
    // Kolejnosc listy zostaje ta sama — metoda niczego nie sortuje.
    assertEquals("A", list.get(0).name());
  }

  @Test
  void brak_dat_i_pusta_lista() {
    assertEquals(-1, SelectOrder.lastUsedIndex(List.<P>of(), P::used));
    assertEquals(0, SelectOrder.lastUsedIndex(List.of(new P("A", null), new P("B", null)), P::used));
    assertEquals(1, SelectOrder.lastUsedIndex(List.of(new P("A", null), new P("B", T0)), P::used));
  }

  @Test
  void po_usunieciu_zostaje_na_tej_samej_postaci() {
    List<String> before = List.of("A", "B", "C", "D", "E");
    List<String> after = List.of("A", "C", "E");
    // Stalem na C (2) — C przetrwalo, zostaje na C (teraz 1). Nie wraca na poczatek.
    assertEquals(1, SelectOrder.keepPosition(before, 2, after, s -> s));
  }

  @Test
  void po_usunieciu_biezacej_idzie_na_nastepna() {
    List<String> before = List.of("A", "B", "C", "D", "E");
    List<String> after = new ArrayList<>(List.of("A", "B", "E"));
    // Stalem na C, C i D usuniete — nastepna zyjaca to E.
    assertEquals(2, SelectOrder.keepPosition(before, 2, after, s -> s));
    // Usunieta ostatnia — zostaje na nowej ostatniej.
    assertEquals(1, SelectOrder.keepPosition(List.of("A", "B", "C"), 2, List.of("A", "B"), s -> s));
    assertEquals(0, SelectOrder.keepPosition(before, 3, List.<String>of(), s -> s));
  }

  @Test
  void czas_wzgledny() {
    Instant now = ZonedDateTime.of(2026, 9, 30, 14, 0, 0, 0, PL).toInstant();
    assertEquals("dziś 12:49", SelectOrder.when(ZonedDateTime.of(2026, 9, 30, 12, 49, 0, 0, PL).toInstant(), now, PL));
    assertEquals("wczoraj 23:59", SelectOrder.when(ZonedDateTime.of(2026, 9, 29, 23, 59, 0, 0, PL).toInstant(), now, PL));
    assertEquals("3 dni temu", SelectOrder.when(ZonedDateTime.of(2026, 9, 27, 8, 0, 0, 0, PL).toInstant(), now, PL));
    assertEquals("12.09.2026", SelectOrder.when(ZonedDateTime.of(2026, 9, 12, 8, 0, 0, 0, PL).toInstant(), now, PL));
    assertEquals("nigdy", SelectOrder.when(null, now, PL));
  }

  @Test
  void czas_gry_i_odmiana() {
    assertEquals("45 min", SelectOrder.playTime(45 * 60_000L));
    assertEquals("3 h 05 min", SelectOrder.playTime((3 * 60 + 5) * 60_000L));
    assertEquals("1 postać", SelectOrder.characters(1));
    assertEquals("3 postacie", SelectOrder.characters(3));
    assertEquals("5 postaci", SelectOrder.characters(5));
    assertEquals("12 postaci", SelectOrder.characters(12));
    assertEquals("22 postacie", SelectOrder.characters(22));
    assertEquals("1 dzień", SelectOrder.days(1));
    assertEquals("7 dni", SelectOrder.days(7));
  }

  @Test
  void imie_w_potwierdzeniu() {
    assertTrue(SelectOrder.nameMatches("  halruk ", "Halruk"));
    assertFalse(SelectOrder.nameMatches("Halru", "Halruk"));
    assertFalse(SelectOrder.nameMatches(null, "Halruk"));
    assertFalse(SelectOrder.nameMatches("", "Halruk"));
  }
}
