package pl.mcmmorpg.charactercreator.persist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Wybor postaci, ktorej skorke gracz dostaje PRZY WEJSCIU, zanim cokolwiek kliknie
 * ({@link CharacterStore#pickLastUsed}).
 *
 * <p>Ma test, bo pomylka tutaj jest cicha i widoczna dopiero w grze: gracz dostaje skorke
 * cudzej albo nie tej postaci i nikt nie zobaczy tego w logu. Reszta {@link CharacterStore}
 * wymaga zywego Bukkita, ta regula nie — jest czysta.
 */
class LastUsedPickTest {

  private static final UUID GRACZ = UUID.randomUUID();
  private static final UUID OBCY = UUID.randomUUID();

  private static CharacterRecord record(String name, UUID owner, Instant created, Instant used) {
    return new CharacterRecord(UUID.randomUUID().toString(), name, "ork", "mag",
        "ork_mag", owner, "kto", created, used);
  }

  @Test
  void wygrywa_najswiezszy_last_used_a_nie_najnowsza_postac() {
    Instant t = Instant.parse("2026-09-01T00:00:00Z");
    CharacterRecord grana = record("Stara", GRACZ, t, t.plusSeconds(9000));
    CharacterRecord swiezo_utworzona = record("Nowa", GRACZ, t.plusSeconds(3600), t.plusSeconds(3600));
    assertEquals(grana, CharacterStore.pickLastUsed(List.of(swiezo_utworzona, grana), GRACZ));
  }

  @Test
  void rekord_sprzed_schematu_3_liczy_sie_po_dacie_utworzenia() {
    Instant t = Instant.parse("2026-09-01T00:00:00Z");
    CharacterRecord stary = record("BezZnacznika", GRACZ, t.plusSeconds(7200), null);
    CharacterRecord nowy = record("ZeZnacznikiem", GRACZ, t, t.plusSeconds(3600));
    assertEquals(stary, CharacterStore.pickLastUsed(List.of(nowy, stary), GRACZ));
  }

  @Test
  void cudzych_postaci_nie_tykamy() {
    Instant t = Instant.parse("2026-09-01T00:00:00Z");
    CharacterRecord cudza = record("Cudza", OBCY, t, t.plusSeconds(9000));
    CharacterRecord moja = record("Moja", GRACZ, t, t.plusSeconds(60));
    assertEquals(moja, CharacterStore.pickLastUsed(List.of(cudza, moja), GRACZ));
  }

  @Test
  void rekord_bez_wlasciciela_nie_jest_zgadywany() {
    Instant t = Instant.parse("2026-09-01T00:00:00Z");
    CharacterRecord bezWlasciciela = record("Sierota", null, t, t.plusSeconds(9000));
    assertNull(CharacterStore.pickLastUsed(List.of(bezWlasciciela), GRACZ));
  }

  @Test
  void brak_postaci_to_null_a_nie_wyjatek() {
    assertNull(CharacterStore.pickLastUsed(List.of(), GRACZ));
    assertNull(CharacterStore.pickLastUsed(List.of(), null));
  }
}
