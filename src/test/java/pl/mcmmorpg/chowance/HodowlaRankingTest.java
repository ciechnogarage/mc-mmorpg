package pl.mcmmorpg.chowance;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import pl.mcmmorpg.chowance.logika.Drabinka;
import pl.mcmmorpg.chowance.logika.Dziedziczenie;
import pl.mcmmorpg.chowance.logika.Elo;
import pl.mcmmorpg.chowance.logika.Generator;
import pl.mcmmorpg.chowance.model.Osobnik;
import pl.mcmmorpg.chowance.tresc.Rejestr;

class HodowlaRankingTest {
  @Test void mlodeToBazaLiniiZGenamiRodzicow() throws Exception {
    Rejestr r = Fixture.rejestr();
    Random rng = new Random(7);
    Osobnik a = Generator.nowy(r, r.gatunek("TESTOGNIAK"), 20, false, rng);
    Osobnik b = Generator.nowy(r, r.gatunek("TESTWODNIK"), 20, false, rng);
    a.geny = new int[] {31, 31, 31, 31, 31, 31};
    b.geny = new int[] {30, 30, 30, 30, 30, 30};
    assertNull(Dziedziczenie.niezgodnosc(a, b, r.gatunek("TESTOGNIAK"), r.gatunek("TESTWODNIK")));
    int dziedziczone = 0, n = 2000;
    for (int i = 0; i < n; i++) {
      Osobnik m = Dziedziczenie.mlode(r, a, b, r.gatunek("TESTOGNIAK"), r.gatunek("TESTWODNIK"), 3, 0, 1, rng);
      assertTrue(m.gatunek.equals("TESTOGNIK") || m.gatunek.equals("TESTWODNIK"), m.gatunek);
      assertEquals(1, m.poziom);
      for (int g : m.geny) if (g >= 30) dziedziczone++;
    }
    // 3 z 6 dziedziczone zawsze + losowe trafiające 30/31 z p=2/32
    double srednio = dziedziczone / (double) n;
    assertEquals(3 + 3 * 2.0 / 32, srednio, 0.1);
  }

  @Test void niezgodni() throws Exception {
    Rejestr r = Fixture.rejestr();
    Osobnik a = Generator.nowy(r, r.gatunek("TESTOGNIK"), 5, false, new Random(1));
    assertNotNull(Dziedziczenie.niezgodnosc(a, a, r.gatunek("TESTOGNIK"), r.gatunek("TESTOGNIK")));
  }

  @Test void lsnienieObcychRodzicow() throws Exception {
    Rejestr r = Fixture.rejestr();
    Random rng = new Random(9);
    Osobnik a = Generator.nowy(r, r.gatunek("TESTOGNIK"), 5, false, rng);
    Osobnik b = Generator.nowy(r, r.gatunek("TESTOGNIK"), 5, false, rng);
    a.pierwszyWlasciciel = "x"; b.pierwszyWlasciciel = "y";
    int l = 0, n = 20000;
    for (int i = 0; i < n; i++) if (Dziedziczenie.mlode(r, a, b, r.gatunek("TESTOGNIK"), r.gatunek("TESTOGNIK"), 3, 0.01, 4, rng).lsniacy) l++;
    assertEquals(0.04, l / (double) n, 0.006);
  }

  @Test void elo() {
    int[] r = Elo.po(1000, 1000, 32);
    assertArrayEquals(new int[] {1016, 984}, r);
    int[] faworyt = Elo.po(1400, 1000, 32);
    assertTrue(faworyt[0] - 1400 < 5);
    assertEquals(0, Elo.po(1000, 0, 32)[1]);
  }

  @Test void drabinkaZWolnymiLosami() {
    Drabinka d = new Drabinka(List.of("a", "b", "c", "d", "e"));
    int rundy = 0;
    while (!d.koniec()) {
      Map<Drabinka.Para, String> w = new HashMap<>();
      for (Drabinka.Para p : d.pary()) if (!p.wolnyLos()) w.put(p, p.a().compareTo(p.b()) < 0 ? p.a() : p.b());
      d.zakonczRunde(w);
      rundy++;
    }
    assertEquals(3, rundy);
    assertEquals("a", d.mistrz());
  }

  @Test void rozstawienieJedenIDwaWFinale() {
    Drabinka d = new Drabinka(List.of("1", "2", "3", "4", "5", "6", "7", "8"));
    List<Drabinka.Para> p = d.pary();
    assertEquals(new Drabinka.Para("1", "8"), p.get(0));
    // 2 w przeciwnej połówce niż 1
    int idx2 = -1;
    for (int i = 0; i < p.size(); i++) if ("2".equals(p.get(i).a()) || "2".equals(p.get(i).b())) idx2 = i;
    assertTrue(idx2 >= 2);
  }
}
