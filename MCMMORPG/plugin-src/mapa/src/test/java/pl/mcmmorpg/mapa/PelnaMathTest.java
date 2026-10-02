package pl.mcmmorpg.mapa;

import static org.junit.jupiter.api.Assertions.*;
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
}
