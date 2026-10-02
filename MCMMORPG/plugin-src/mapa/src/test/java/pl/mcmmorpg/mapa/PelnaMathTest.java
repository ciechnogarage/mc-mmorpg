package pl.mcmmorpg.mapa;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class PelnaMathTest {
  @Test void promienDlaCzterechKierunkow() {
    for (int kierunek = 0; kierunek < 4; kierunek++) {
      double yaw = kierunek * 90;
      double rad = Math.toRadians(yaw);
      double fx = -Math.sin(rad), fz = Math.cos(rad);
      PelnaMath.Punkt center = PelnaMath.przeciecie(0, 0, 0, fx, 0, fz,
          fx * 2, 0, fz * 2, yaw, 512, 384);
      assertNotNull(center);
      assertEquals(256, center.u(), 0.001);
      assertEquals(192, center.v(), 0.001);
      PelnaMath.Punkt right = PelnaMath.przeciecie(0, 0, 0, fx * 2 - Math.cos(rad), 0,
          fz * 2 - Math.sin(rad), fx * 2, 0, fz * 2, yaw, 512, 384);
      assertNotNull(right);
      assertEquals(384, right.u(), 0.001);
    }
    assertNull(PelnaMath.przeciecie(0, 0, 0, 0, 1, 0, 0, 0, 2, 0, 512, 384));
  }

  @Test void kolkoPrzechodziPrzezKrawedzHotbara() {
    assertEquals(1, PelnaMath.kolko(8, 0));
    assertEquals(-1, PelnaMath.kolko(0, 8));
  }

  @Test void przeciaganiePrzesuwaSrodek() {
    PelnaMath.Punkt p = PelnaMath.przesun(100, -20, 12, -6, 2);
    assertEquals(76, p.u());
    assertEquals(-8, p.v());
  }

  @Test void pasekRozpoznajeZakladkiIZamkniecie() {
    assertEquals(0, PelnaMath.przycisk(4, 8, 512, 3));
    assertEquals(2, PelnaMath.przycisk(150, 8, 512, 3));
    assertEquals(-2, PelnaMath.przycisk(500, 8, 512, 3));
    assertEquals(-1, PelnaMath.przycisk(250, 30, 512, 3));
  }

  @Test void pasekRozpoznajeAtlasPrzedZamknieciem() {
    assertEquals(7, PelnaMath.przycisk(447, 8, 512, 8));
    assertEquals(-3, PelnaMath.przycisk(448, 8, 512, 8));
    assertEquals(-3, PelnaMath.przycisk(487, 19, 512, 8));
    assertEquals(-2, PelnaMath.przycisk(488, 8, 512, 8));
    assertEquals(-1, PelnaMath.przycisk(470, PelnaMath.PASEK, 512, 8));
    assertEquals(-3, PelnaMath.przycisk(470, 24, 512, 8));
  }

  @Test void pikselPrzeliczaSieNaSwiat() {
    assertEquals(new PelnaMath.Punkt(100, -50),
        PelnaMath.punktSwiata(256, 192, 512, 384, 100, -50, 2));
    assertEquals(new PelnaMath.Punkt(120, -66),
        PelnaMath.punktSwiata(266, 184, 512, 384, 100, -50, 2));
  }

  @Test void wybieraNajblizszyZnacznikDoDziesieciuPikseli() {
    MapaData.Marker a = new MapaData.Marker("world", 10, 0, "A", "strefa", "", List.of());
    MapaData.Marker b = new MapaData.Marker("world", 15, 0, "B", "wlasny", "pin_biala", List.of());
    assertEquals(b, PelnaMath.najblizszy(List.of(a, b), 116, 100, 200, 200, 0, 0, 1));
    assertNull(PelnaMath.najblizszy(List.of(a, b), 130, 100, 200, 200, 0, 0, 1));
  }

  @Test void machnieciePoPpmNieJestKliknieciem() {
    assertTrue(PelnaMath.ignorujMachniecie(102, 100, -100));
    assertFalse(PelnaMath.ignorujMachniecie(103, 100, -100));
    assertTrue(PelnaMath.ignorujMachniecie(103, -100, 102));
    assertFalse(PelnaMath.ignorujMachniecie(104, -100, 102));
  }

  @Test void ruchPoziomyZamykaMapeAleObrotISkokNie() {
    assertFalse(PelnaMath.zamknijPoRuchu(10, 20, 10.3, 20));
    assertTrue(PelnaMath.zamknijPoRuchu(10, 20, 10.301, 20));
    assertFalse(PelnaMath.zamknijPoRuchu(10, 20, 10, 20));
  }

  @Test void pelnyObrazDopieroPoObrocieO15StopniLubPikselu() {
    assertFalse(PelnaMath.pelnePrzerysowanie(false, true, true, true,
        0, 0, 0, 0, 0, 14, 1));
    assertTrue(PelnaMath.pelnePrzerysowanie(false, true, true, true,
        0, 0, 0, 0, 0, 15, 1));
    assertTrue(PelnaMath.pelnePrzerysowanie(false, true, true, true,
        0, 0, 0, 1, 0, 0, 1));
    assertTrue(PelnaMath.pelnePrzerysowanie(true, true, true, true,
        0, 0, 0, 0, 0, 0, 1));
  }

  @Test void kursorWObrebieJednegoPikselaNieWymagaWysylki() {
    assertTrue(PelnaMath.tenSamPiksel(new PelnaMath.Punkt(10.1, 20.2),
        new PelnaMath.Punkt(10.4, 20.3)));
    assertFalse(PelnaMath.tenSamPiksel(new PelnaMath.Punkt(10.1, 20.2),
        new PelnaMath.Punkt(10.6, 20.3)));
    assertFalse(PelnaMath.tenSamPiksel(null, new PelnaMath.Punkt(10, 20)));
  }
}
