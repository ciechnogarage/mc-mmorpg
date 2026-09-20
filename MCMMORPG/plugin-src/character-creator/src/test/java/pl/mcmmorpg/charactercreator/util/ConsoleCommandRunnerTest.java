package pl.mcmmorpg.charactercreator.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Nazwa gracza trafia do komendy wykonywanej jako KONSOLA, a serwer ma
 * {@code online-mode=false} — nick podaje klient. Te testy pilnuja, ze nic, co pozwoliloby
 * dokleic do komendy wlasna tresc, nie przejdzie przez podstawienie.
 */
class ConsoleCommandRunnerTest {

  @ParameterizedTest
  @ValueSource(strings = {"Notch", "gracz_1", "A", "aaaaaaaaaaaaaaaa"})
  void zwykle_nicki_przechodza(String name) {
    assertTrue(ConsoleCommandRunner.isSafeName(name), name);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "a b",                 // spacja rozbija komende na argumenty
      "gracz;op gracz",      // separator komend
      "gracz\"x",
      "gracz'x",
      "gracz\nop x",         // nowa linia
      "aaaaaaaaaaaaaaaaa",   // 17 znakow, ponad limit Mojanga
      "",
      "gracz§4",
      "%player%",            // probá podstawienia w samym nicku
  })
  void nicki_z_czyms_wiecej_niz_litery_cyfry_i_podkreslnik_odpadaja(String name) {
    assertFalse(ConsoleCommandRunner.isSafeName(name), name);
  }

  @Test
  void null_nie_wysadza_walidacji() {
    assertFalse(ConsoleCommandRunner.isSafeName(null));
  }
}
