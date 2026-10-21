package pl.mcmmorpg.chowance;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;
import pl.mcmmorpg.chowance.logika.WzorLapania;

class WzorLapaniaTest {
  final WzorLapania w = new WzorLapania(WzorLapania.Parametry.domyslne());

  WzorLapania.Wejscie we(double hp, int wsp, double piecz, String st, int cel, int odn, int prof) {
    return new WzorLapania.Wejscie(hp, 100, wsp, piecz, st, cel, odn, prof);
  }

  @Test void pelneHpPospolityKamienna() {
    // (300-200)/300 * 190 / 255 = 0.24837
    assertEquals(0.24837, w.szansa(we(100, 190, 1.0, null, 10, 10, 0)), 1e-4);
  }

  @Test void jedenHpRosnie() {
    // (300-2)/300 * 190 / 255 = 0.74013
    assertEquals(0.74013, w.szansa(we(1, 190, 1.0, null, 10, 10, 0)), 1e-4);
  }

  @Test void statusSenPodwaja() {
    double bez = w.szansa(we(100, 60, 1.0, null, 10, 10, 0));
    double sen = w.szansa(we(100, 60, 1.0, "SEN", 10, 10, 0));
    assertEquals(2.0, sen / bez, 1e-9);
    assertEquals(1.5, w.szansa(we(100, 60, 1.0, "SPETANIE", 10, 10, 0)) / bez, 1e-9);
  }

  @Test void klasaPieczeci() {
    double k = w.szansa(we(100, 60, 1.0, null, 10, 10, 0));
    assertEquals(2.0, w.szansa(we(100, 60, 2.0, null, 10, 10, 0)) / k, 1e-9);
  }

  @Test void roznicaPoziomowZaciskana() {
    assertEquals(0.4, w.mnoznikPoziomu(80, 10), 1e-9); // 1 + 0.02*(-70) = -0.4 -> 0.4
    assertEquals(1.5, w.mnoznikPoziomu(1, 90), 1e-9);
    assertEquals(0.8, w.mnoznikPoziomu(20, 10), 1e-9);
  }

  @Test void profesja() {
    assertEquals(1.0, w.mnoznikProfesji(0), 1e-9);
    assertEquals(1.25, w.mnoznikProfesji(25), 1e-9);
    assertEquals(1.5, w.mnoznikProfesji(90), 1e-9);
  }

  @Test void legendarnyPrawieNiemozliwy() {
    assertTrue(w.szansa(we(100, 3, 1.0, null, 50, 50, 0)) < 0.005);
  }

  @Test void clamp() {
    assertEquals(1.0, w.szansa(we(1, 255, 3.0, "SEN", 1, 50, 50)), 1e-9);
    assertEquals(0.0, w.szansa(new WzorLapania.Wejscie(10, 0, 190, 1, null, 1, 1, 0)), 1e-9);
  }

  @Test void empirycznieZgodneZeWzorem() {
    Random rng = new Random(42);
    WzorLapania.Wejscie x = we(50, 120, 1.5, null, 15, 12, 5);
    double p = w.szansa(x);
    int ok = 0, n = 200_000;
    int[] drz = new int[4];
    for (int i = 0; i < n; i++) {
      WzorLapania.Wynik r = w.proba(x, rng);
      if (r.sukces()) { ok++; assertEquals(3, r.drzenia()); }
      drz[r.drzenia()]++;
    }
    assertEquals(p, ok / (double) n, 0.005);
    assertTrue(drz[0] > 0 && drz[1] > 0 && drz[2] > 0 && drz[3] > 0);
  }
}
