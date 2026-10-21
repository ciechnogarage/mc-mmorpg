package pl.mcmmorpg.chowance.integracja;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NazwyMmoTest {
  @Test
  void czyscTagiIKody() {
    assertEquals("Kamień Przemiany Powietrza", NazwyMmo.czysc("<#9FD6E8>Kamień Przemiany Powietrza"));
    assertEquals("Pieczęć Więzi", NazwyMmo.czysc("&6&lPieczęć <bold>Więzi</bold>"));
  }

  @Test
  void brakNazwyToNieSuroweId() {
    assertEquals("Kamien przemiany ogien", new NazwyMmo().nazwa("KAMIEN_PRZEMIANY_OGIEN"));
  }
}
