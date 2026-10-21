package pl.mcmmorpg.chowance;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import pl.mcmmorpg.chowance.logika.Generator;
import pl.mcmmorpg.chowance.logika.SilnikWalki;
import pl.mcmmorpg.chowance.logika.Statystyki;
import pl.mcmmorpg.chowance.logika.Wojownik;
import pl.mcmmorpg.chowance.model.Osobnik;
import pl.mcmmorpg.chowance.model.Stat;
import pl.mcmmorpg.chowance.tresc.LadowaczTresci;
import pl.mcmmorpg.chowance.tresc.Rejestr;

/** Cechy: warunek środowiska `gdy` (B-003), MNOZNIK_STATU na ZDROWIE (B-004), walidacja. */
class CechyTest {
  Rejestr rejestr(String cechy) throws Exception {
    Path d = Fixture.katalog();
    Files.writeString(d.resolve("cechy/b.yml"), cechy);
    Files.writeString(d.resolve("gatunki/b.yml"), """
        CECHOWIEC:
          typy: [OGIEN]
          staty_bazowe: {zdrowie: 50, atak: 50, obrona: 50, atak_spec: 50, obrona_spec: 50, szybkosc: 50}
          cechy: [C1]
          ruchy: {1: [TEST_ISKRA, TEST_DRAPNIECIE]}
          mob: {zastepczy: FOX}
        """);
    LadowaczTresci l = new LadowaczTresci(d.toFile());
    Rejestr r = l.wczytaj();
    assertEquals(0, l.problemy().bledy(), l.problemy().wpisy().toString());
    return r;
  }

  Wojownik w(Rejestr r, String g) {
    Osobnik o = Generator.nowy(r, r.gatunek(g), 30, false, new Random(1));
    o.charakter = "SPOKOJNY";
    java.util.Arrays.fill(o.geny, 15);
    return new Wojownik(r, o, 0, Double.MAX_VALUE);
  }

  @Test void mnoznikZdrowiaWStatach() throws Exception {
    Rejestr r = rejestr("C1: {rodzaj: MNOZNIK_STATU, parametry: {stat: ZDROWIE, mnoznik: 1.2}}\n");
    Wojownik x = w(r, "CECHOWIEC");
    int bez = Statystyki.stat(x.g, x.o, r.charakter("SPOKOJNY"), Stat.ZDROWIE, 0);
    assertEquals((int) Math.floor(bez * 1.2), x.hpMax);
  }

  @Test void wzmocnienieTypuTylkoNoca() throws Exception {
    Rejestr r = rejestr("C1: {rodzaj: WZMOCNIENIE_TYPU, parametry: {typ: OGIEN, mnoznik: 2.0, gdy: [NOC]}}\n");
    Wojownik at = w(r, "CECHOWIEC");
    Wojownik ob = w(r, "TESTOGNIK");
    var ruch = r.ruch("TEST_ISKRA");
    at.srodowisko = Set.of("DZIEN");
    long dzien = 0, noc = 0;
    for (int i = 0; i < 400; i++) dzien += SilnikWalki.obrazenia(r, at, ob, ruch, new Random(i)).obrazenia();
    at.srodowisko = Set.of("NOC", "DESZCZ");
    for (int i = 0; i < 400; i++) noc += SilnikWalki.obrazenia(r, at, ob, ruch, new Random(i)).obrazenia();
    assertEquals(2.0, noc / (double) dzien, 0.08);
  }

  @Test void zlyWarunekOdrzucony() throws Exception {
    Path d = Fixture.katalog();
    Files.writeString(d.resolve("cechy/b.yml"), """
        ZLA1: {rodzaj: REGENERACJA, parametry: {procent: 5, gdy: [NOC]}}
        ZLA2: {rodzaj: MNOZNIK_STATU, parametry: {stat: ATAK, mnoznik: 1.1, gdy: [PELNIA]}}
        ZLA3: {rodzaj: MNOZNIK_STATU, parametry: {stat: ZDROWIE, mnoznik: 1.1, gdy: [NOC]}}
        UWAGA: {rodzaj: UNIK, parametry: {procent: 5, zbedny: 1}}
        """);
    LadowaczTresci l = new LadowaczTresci(d.toFile());
    Rejestr r = l.wczytaj();
    assertNull(r.cecha("ZLA1"));
    assertNull(r.cecha("ZLA2"));
    assertNull(r.cecha("ZLA3"));
    assertNotNull(r.cecha("UWAGA"));
    assertTrue(l.problemy().wpisy().toString().contains("zbedny"));
  }
}
