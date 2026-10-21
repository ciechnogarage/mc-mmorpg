package pl.mcmmorpg.chowance;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import pl.mcmmorpg.chowance.logika.Generator;
import pl.mcmmorpg.chowance.logika.Rozwoj;
import pl.mcmmorpg.chowance.model.Ewolucja;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Osobnik;
import pl.mcmmorpg.chowance.tresc.Rejestr;

class RozwojTest {
  @Test void generatorDajeCzteryNajnowszeRuchy() throws Exception {
    Rejestr r = Fixture.rejestr();
    Gatunek g = r.gatunek("TESTOGNIK");
    Osobnik o = Generator.nowy(r, g, 9, false, new Random(1));
    assertEquals(List.of("TEST_DRAPNIECIE", "TEST_ISKRA", "TEST_WARKNIECIE", "TEST_PLOMIEN"), o.ruchy);
    Osobnik m = Generator.nowy(r, g, 1, false, new Random(1));
    assertEquals(List.of("TEST_DRAPNIECIE", "TEST_ISKRA"), m.ruchy);
    for (int gen : o.geny) assertTrue(gen >= 0 && gen <= 31);
    assertEquals(g.krzywa().lacznyExp(9), o.exp);
  }

  @Test void awansINaukaRuchow() throws Exception {
    Rejestr r = Fixture.rejestr();
    Gatunek g = r.gatunek("TESTOGNIK");
    Osobnik o = Generator.nowy(r, g, 1, false, new Random(2));
    Rozwoj.Awans a = Rozwoj.dodajExp(o, g, g.krzywa().lacznyExp(5), 20);
    assertEquals(5, a.naPoziom());
    assertEquals(List.of("TEST_WARKNIECIE"), a.nauczone());
    o.ruchy.add("TEST_X");
    Rozwoj.Awans b = Rozwoj.dodajExp(o, g, g.krzywa().lacznyExp(9) - o.exp, 20);
    assertEquals(9, b.naPoziom());
    assertEquals(List.of("TEST_PLOMIEN"), b.doNauki());
    assertEquals(4, o.ruchy.size());
  }

  @Test void limitPoziomuZOdznak() throws Exception {
    Rejestr r = Fixture.rejestr();
    Gatunek g = r.gatunek("TESTOGNIK");
    Osobnik o = Generator.nowy(r, g, 1, false, new Random(3));
    Rozwoj.dodajExp(o, g, 10_000_000L, 20);
    assertEquals(20, o.poziom);
    assertEquals(g.krzywa().lacznyExp(20), o.exp);
    Rozwoj.Awans a = Rozwoj.dodajExp(o, g, 500, 20);
    assertFalse(a.awansowal());
    assertTrue(a.naLimicie());
    assertEquals(g.krzywa().lacznyExp(20), o.exp);
  }

  @Test void stworPowyzejLimituNieTraciExp() throws Exception {
    // regresja PROBLEMY A-013: poziom 30 przy limicie 20 (handel/admin) — exp nie może spaść do progu 20
    Rejestr r = Fixture.rejestr();
    Gatunek g = r.gatunek("TESTOGNIK");
    Osobnik o = Generator.nowy(r, g, 30, false, new Random(5));
    long przed = o.exp;
    Rozwoj.Awans a = Rozwoj.dodajExp(o, g, 500, 20);
    assertEquals(przed, o.exp);
    assertEquals(30, o.poziom);
    assertTrue(a.naLimicie());
  }

  @Test void ewolucja() throws Exception {
    Rejestr r = Fixture.rejestr();
    Gatunek g = r.gatunek("TESTOGNIK");
    Osobnik o = Generator.nowy(r, g, 9, false, new Random(4));
    assertNull(Rozwoj.dostepnaEwolucja(o, g, null));
    Rozwoj.dodajExp(o, g, g.krzywa().lacznyExp(10) - o.exp, 30);
    Ewolucja e = Rozwoj.dostepnaEwolucja(o, g, null);
    assertNotNull(e);
    Rozwoj.ewoluuj(o, g, r.gatunek(e.doGatunku()));
    assertEquals("TESTOGNIAK", o.gatunek);
    assertEquals(10, o.poziom);
  }

  @Test void ewolucjaWymagaPrzedmiotuIWiezi() {
    Gatunek g = new Gatunek("A", "A", List.of(), List.of(pl.mcmmorpg.chowance.model.Typ.OGIEN),
        pl.mcmmorpg.chowance.model.Rzadkosc.POSPOLITY, 190, 1, "A", new int[] {1, 1, 1, 1, 1, 1},
        pl.mcmmorpg.chowance.model.KrzywaExp.SREDNIA, 60, 70, List.of("C"), new java.util.TreeMap<>(),
        List.of(new Ewolucja("B", 16, "KAMIEN", 200)), List.of(), 100, true, false, java.util.Map.of(), "",
        false, true, false, "m", "", "", "", "", "", "", List.of(), false);
    Osobnik o = new Osobnik();
    o.poziom = 20; o.wiez = 100;
    assertNull(Rozwoj.dostepnaEwolucja(o, g, "KAMIEN"));
    o.wiez = 200;
    assertNull(Rozwoj.dostepnaEwolucja(o, g, "INNY"));
    assertNotNull(Rozwoj.dostepnaEwolucja(o, g, "KAMIEN"));
  }

  @Test void expZaPokonanie() throws Exception {
    Rejestr r = Fixture.rejestr();
    Gatunek g = r.gatunek("TESTOGNIK"); // exp_bazowy 60
    assertEquals(86, Rozwoj.expZaPokonanie(g, 10, false, 1.0)); // 60*10/7 = 85.7
    assertEquals(129, Rozwoj.expZaPokonanie(g, 10, true, 1.0));
  }
}
