package pl.mcmmorpg.mapa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class ProwadzenieTest {
  @Test void osiemKierunkowWzgledemWzroku() {
    String[] expected = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
    for (int i = 0; i < 8; i++) {
      double bearing = Math.toRadians(i * 45);
      assertEquals(expected[i], Prowadzenie.arrow(0,
          -Math.sin(bearing), Math.cos(bearing)));
    }
    assertEquals("↑", Prowadzenie.arrow(90, -1, 0));
  }
}
