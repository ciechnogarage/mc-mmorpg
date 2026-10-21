package pl.mcmmorpg.chowance;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import pl.mcmmorpg.chowance.logika.Obrazenia;
import pl.mcmmorpg.chowance.logika.Statystyki;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Osobnik;
import pl.mcmmorpg.chowance.model.Stat;
import pl.mcmmorpg.chowance.tresc.Rejestr;

class StatystykiTest {
  @Test void wzoryStatow() throws Exception {
    Rejestr r = Fixture.rejestr();
    Gatunek g = r.gatunek("TESTOGNIK"); // hp 45, atak 55
    Osobnik o = new Osobnik();
    o.gatunek = g.id(); o.poziom = 50; o.geny = new int[] {31, 0, 0, 0, 0, 0};
    // HP: (90+31)*50/100 = 60 + 50 + 10 = 120
    assertEquals(120, Statystyki.stat(g, o, r.charakter("SPOKOJNY"), Stat.ZDROWIE, 0));
    // Atak: (110+0)*50/100 = 55 + 5 = 60; SMIALY +10% = 66
    assertEquals(60, Statystyki.stat(g, o, r.charakter("SPOKOJNY"), Stat.ATAK, 0));
    assertEquals(66, Statystyki.stat(g, o, r.charakter("SMIALY"), Stat.ATAK, 0));
    // AtkS pod SMIALY -10%: (130*50/100=65)+5 = 70 -> 63
    assertEquals(63, Statystyki.stat(g, o, r.charakter("SMIALY"), Stat.ATAK_SPEC, 0));
    // więź 255 przy premii 5%: 60 -> 63
    o.wiez = 255;
    assertEquals(63, Statystyki.stat(g, o, r.charakter("SPOKOJNY"), Stat.ATAK, 0.05));
  }

  @Test void regeneracjaLeniwa() {
    Osobnik o = new Osobnik();
    Statystyki.ustawHp(o, 50, 100, 1_000);
    assertEquals(50, Statystyki.biezaceHp(o, 100, 1_000, 5, true), 1e-9);
    assertEquals(55, Statystyki.biezaceHp(o, 100, 61_000, 5, true), 1e-9);
    assertEquals(100, Statystyki.biezaceHp(o, 100, 100 * 60_000L, 5, true), 1e-9);
    Statystyki.ustawHp(o, 0, 100, 0);
    assertEquals(0, Statystyki.biezaceHp(o, 100, 600_000, 5, false), 1e-9);
    Statystyki.ustawHp(o, 100, 100, 0);
    assertEquals(-1, o.hp, 1e-9); // pełne = -1
  }

  @Test void obrazeniaBazowe() {
    // L50: floor(2*50/5+2)=22; 22*40*60/60 = 880; 880/50 = 17 + 2 = 19
    assertEquals(19, Obrazenia.bazowe(50, 40, 60, 60));
    assertEquals(0, Obrazenia.bazowe(50, 0, 60, 60));
    // STAB * 2.0 * losowy 1.0: 19*1.5*2 = 57
    assertEquals(57, Obrazenia.koncowe(19, true, 2.0, false, 1.0, 1.0));
    assertEquals(0, Obrazenia.koncowe(19, true, 0.0, false, 1.0, 1.0));
    assertEquals(1, Obrazenia.koncowe(1, false, 0.5, false, 0.85, 1.0));
  }
}
