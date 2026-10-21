package pl.mcmmorpg.chowance;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import pl.mcmmorpg.chowance.logika.Generator;
import pl.mcmmorpg.chowance.logika.SilnikWalki;
import pl.mcmmorpg.chowance.logika.SilnikWalki.Akcja;
import pl.mcmmorpg.chowance.logika.SilnikWalki.Zdarzenie;
import pl.mcmmorpg.chowance.logika.Wojownik;
import pl.mcmmorpg.chowance.model.Osobnik;
import pl.mcmmorpg.chowance.model.Status;
import pl.mcmmorpg.chowance.tresc.Rejestr;

class SilnikWalkiTest {
  Wojownik w(Rejestr r, String g, int lvl, long seed) {
    Osobnik o = Generator.nowy(r, r.gatunek(g), lvl, false, new Random(seed));
    o.charakter = "SPOKOJNY";
    Wojownik x = new Wojownik(r, o, 0, Double.MAX_VALUE);
    return x;
  }

  SilnikWalki walka(Rejestr r, List<Wojownik> a, List<Wojownik> b, long seed) {
    return new SilnikWalki(r, new SilnikWalki.Strona("A", a, false), new SilnikWalki.Strona("B", b, true), new Random(seed));
  }

  @Test void walkaSieKonczy() throws Exception {
    Rejestr r = Fixture.rejestr();
    for (long seed = 0; seed < 50; seed++) {
      SilnikWalki s = walka(r, new ArrayList<>(List.of(w(r, "TESTOGNIK", 12, seed), w(r, "TESTWODNIK", 10, seed + 1))),
          new ArrayList<>(List.of(w(r, "TESTWODNIK", 12, seed + 2))), seed);
      int tury = 0;
      while (!s.koniec() && tury < 200) {
        for (int st = 0; st < 2; st++) if (s.wymagaZmiany(st)) s.wymuszonaZmiana(st, s.strony[st].pierwszyZdolny());
        s.tura(new Akcja.UzyjRuchu(s.wyborSI(0)), new Akcja.UzyjRuchu(s.wyborSI(1)));
        tury++;
      }
      assertTrue(s.koniec(), "seed " + seed);
      int przegrany = 1 - s.zwyciezca();
      assertTrue(s.strony[przegrany].przegrala());
    }
  }

  @Test void wodaNaOgienBardzoSkuteczna() throws Exception {
    Rejestr r = Fixture.rejestr();
    Wojownik woda = w(r, "TESTWODNIK", 30, 1);
    Wojownik ogien = w(r, "TESTOGNIK", 30, 2);
    var ruch = r.ruch("TEST_STRUMIEN");
    var wynik = SilnikWalki.obrazenia(r, woda, ogien, ruch, new Random(3));
    assertEquals(2.0, wynik.skutecznosc());
    var odwrotnie = SilnikWalki.obrazenia(r, ogien, woda, r.ruch("TEST_ISKRA"), new Random(3));
    assertEquals(0.5, odwrotnie.skutecznosc());
  }

  @Test void odpornoscNaSen() throws Exception {
    Rejestr r = Fixture.rejestr();
    Wojownik a = w(r, "TESTWODNIK", 20, 1); // ma Kołysankę
    Wojownik b = w(r, "TESTWODNIK", 20, 2); // cecha TEST_ODPORNY_SEN
    a.o.ruchy = new ArrayList<>(List.of("TEST_KOLYSANKA"));
    a.etapy[pl.mcmmorpg.chowance.model.StatEtapu.CELNOSC.ordinal()] = 6;
    SilnikWalki s = walka(r, List.of(a), List.of(b), 5);
    List<Zdarzenie> z = s.tura(new Akcja.UzyjRuchu(0), new Akcja.Pomin("test"));
    assertNull(b.status);
    assertTrue(z.stream().anyMatch(e -> e.tekst().contains("odporny")), z.toString());
  }

  @Test void senBlokujeAtak() throws Exception {
    Rejestr r = Fixture.rejestr();
    Wojownik a = w(r, "TESTOGNIK", 20, 1);
    Wojownik b = w(r, "TESTOGNIK", 20, 2);
    a.status = Status.SEN; a.statusCzas = 5;
    double hpB = b.hp;
    SilnikWalki s = walka(r, List.of(a), List.of(b), 5);
    s.tura(new Akcja.UzyjRuchu(0), new Akcja.Pomin("test"));
    assertEquals(hpB, b.hp, 1e-9);
  }

  @Test void zmianaIDlaZemdlonegoWymuszona() throws Exception {
    Rejestr r = Fixture.rejestr();
    Wojownik a1 = w(r, "TESTOGNIK", 5, 1);
    Wojownik a2 = w(r, "TESTOGNIK", 5, 2);
    Wojownik b = w(r, "TESTOGNIK", 60, 3);
    SilnikWalki s = walka(r, new ArrayList<>(List.of(a1, a2)), List.of(b), 1);
    a1.hp = 1;
    s.tura(new Akcja.Pomin("x"), new Akcja.UzyjRuchu(0));
    assertTrue(a1.zemdlony());
    assertTrue(s.wymagaZmiany(0));
    assertFalse(s.koniec());
    s.wymuszonaZmiana(0, 1);
    assertSame(a2, s.strony[0].akt());
  }

  @Test void szybszyAtakujePierwszy() throws Exception {
    Rejestr r = Fixture.rejestr();
    Wojownik szybki = w(r, "TESTOGNIK", 50, 1); // szybkosc 65
    Wojownik wolny = w(r, "TESTWODNIK", 50, 2); // szybkosc 45
    SilnikWalki s = walka(r, List.of(wolny), List.of(szybki), 1);
    List<Zdarzenie> z = s.tura(new Akcja.UzyjRuchu(0), new Akcja.UzyjRuchu(0));
    Zdarzenie pierwszyRuch = z.stream().filter(e -> e.rodzaj() == Zdarzenie.Rodzaj.RUCH).findFirst().orElseThrow();
    assertEquals(1, pierwszyRuch.strona());
  }
}
