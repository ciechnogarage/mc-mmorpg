package pl.mcmmorpg.charactercreator.creator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * Wygasanie niedokonczonych sesji.
 *
 * <p>Na produkcji dwa wpisy z 18 wrzesnia przezyly kazdy z 93 restartow, bo wpis kasowalo
 * WYLACZNIE ponowne wejscie gracza. Wprowadzenie TTL-a niesie ze soba drugie ryzyko —
 * skasowania komus plecaka — wiec test pilnuje obu stron reguly naraz.
 */
class SessionStashTtlTest {

  private static final long TTL_MILLIS = Duration.ofDays(14).toMillis();

  private static long cutoff(long now) {
    return now - TTL_MILLIS;
  }

  @Test
  void swiezy_wpis_zostaje() {
    long now = System.currentTimeMillis();
    long zapisany = now - Duration.ofDays(2).toMillis();
    assertFalse(SessionStateStore.isStale(zapisany, cutoff(now)));
  }

  @Test
  void wpis_starszy_niz_ttl_wygasa() {
    long now = System.currentTimeMillis();
    long zapisany = now - Duration.ofDays(15).toMillis();
    assertTrue(SessionStateStore.isStale(zapisany, cutoff(now)));
  }

  @Test
  void wpis_dokladnie_na_granicy_jeszcze_zostaje() {
    long now = System.currentTimeMillis();
    assertFalse(SessionStateStore.isStale(cutoff(now), cutoff(now)));
  }

  @Test
  void wpis_bez_znacznika_czasu_NIGDY_nie_wygasa() {
    long now = System.currentTimeMillis();
    // Wiek nieznany (plik sprzed pola 'saved-at'). Utrata plecaka jest nieodwracalna,
    // zbedny wpis w pliku nie jest — przy watpliwosci wpis ma zostac.
    assertFalse(SessionStateStore.isStale(0L, cutoff(now)));
    assertFalse(SessionStateStore.isStale(-1L, cutoff(now)));
  }
}
