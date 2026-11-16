package pl.mcmmorpg.chowance;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import pl.mcmmorpg.chowance.logika.Kroki;
import pl.mcmmorpg.chowance.logika.LimitSygnalow;
import pl.mcmmorpg.chowance.logika.Odstep;

final class TowarzyszLogikaTest {
  @Test void kolejnoscKrokowNieZalezyOdKolejnosciZdarzen() {
    int m = Kroki.zalicz(0, 4);
    m = Kroki.zalicz(m, 2);
    assertEquals(2, Kroki.postep(m));
    assertEquals(1, Kroki.nastepny(m));
    m = Kroki.zalicz(Kroki.zalicz(m, 1), 3);
    assertEquals(0, Kroki.nastepny(m));
    assertEquals(Kroki.WSZYSTKIE, m);
  }
  @Test void limitOdnawiaSiePoSekundzie() {
    LimitSygnalow l = new LimitSygnalow();
    assertTrue(l.wez(1001, 2));
    assertTrue(l.wez(1999, 2));
    assertFalse(l.wez(1999, 2));
    assertTrue(l.wez(2000, 2));
  }
  @Test void glaskanieNieOdnawiaSiePoRelogu() {
    long zapisane = 100_000;
    assertFalse(Odstep.gotowy(zapisane, 699_999, 600_000));
    assertTrue(Odstep.gotowy(zapisane, 700_000, 600_000));
  }
}
