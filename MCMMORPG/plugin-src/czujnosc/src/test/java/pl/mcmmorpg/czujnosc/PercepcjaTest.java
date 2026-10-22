package pl.mcmmorpg.czujnosc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import pl.mcmmorpg.czujnosc.Percepcja.Postawa;
import pl.mcmmorpg.czujnosc.Stan.Poziom;

class PercepcjaTest {
  final Profil d = Profil.z("domyslny", null, null);

  /** Sekundy do alarmu przy stalym przyroscie (wzrok rosnie co 0,5 s). */
  double sekundy(double pktNaS) { return pktNaS <= 0 ? Double.POSITIVE_INFINITY : 100 / pktNaS; }

  @Test void odTylu_nicNieWidzi() {
    assertEquals(0, Percepcja.wzrok(d, 3, 170, Postawa.CHODZI, 15, 1));
  }

  @Test void naWprost_blisko_szybko_daleko_wolno() {
    assertEquals(0.6, sekundy(Percepcja.wzrok(d, 0, 0, Postawa.CHODZI, 15, 1)), 1e-9);
    assertEquals(6, sekundy(Percepcja.wzrok(d, 20, 0, Postawa.CHODZI, 15, 1)), 1e-9);
    assertEquals(0, Percepcja.wzrok(d, 20.1, 0, Postawa.CHODZI, 15, 1));
  }

  @Test void kucanieWCiemnosci_12bl_niezauwazony_przez_dlugo() {
    // 12 bl.: 0,6+5,4*0,6 = 3,84 s pelnym swietle chodzac; kucanie x0,35, ciemnosc x0,3 -> ~36 s
    double s = sekundy(Percepcja.wzrok(d, 12, 0, Postawa.KUCA, 0, 1));
    assertTrue(s > 30 && s < 40, "s=" + s);
  }

  @Test void sprint_szybciej_niz_chod() {
    assertTrue(Percepcja.wzrok(d, 10, 0, Postawa.BIEGNIE, 15, 1) > Percepcja.wzrok(d, 10, 0, Postawa.CHODZI, 15, 1));
  }

  @Test void kucanie_jest_ciche_nawet_w_zbroi() {
    assertEquals(0, Percepcja.glosnosc(Postawa.KUCA, 0.6, 1));
    assertEquals(0, Percepcja.sluch(d, 1, Percepcja.glosnosc(Postawa.KUCA, 0.6, 1)));
    assertTrue(Percepcja.sluch(d, 5, Percepcja.glosnosc(Postawa.BIEGNIE, 0, 1)) > 0);
    assertEquals(0, Percepcja.sluch(d, 7, Percepcja.glosnosc(Postawa.CHODZI, 0, 1)));   // chod: 6 bl.
  }

  @Test void progi_z_histereza() {
    assertEquals(Poziom.SPOKOJNY, Poziom.z(24, Poziom.SPOKOJNY));
    assertEquals(Poziom.ZANIEPOKOJONY, Poziom.z(25, Poziom.SPOKOJNY));
    assertEquals(Poziom.ZANIEPOKOJONY, Poziom.z(20, Poziom.ZANIEPOKOJONY));
    assertEquals(Poziom.SZUKA, Poziom.z(55, Poziom.SZUKA));
    assertEquals(Poziom.ZANIEPOKOJONY, Poziom.z(49, Poziom.SZUKA));
    assertEquals(Poziom.SZUKA, Poziom.z(70, Poziom.ALARM));
    assertEquals(Poziom.ALARM, Poziom.z(100, Poziom.SPOKOJNY));
  }

  @Test void kat() {
    assertEquals(0, Percepcja.kat(0, 0, 1, 0, 0, 5), 1e-9);
    assertEquals(90, Percepcja.kat(0, 0, 1, 3, 0, 0), 1e-9);
    assertEquals(180, Percepcja.kat(0, 0, 1, 0, 0, -2), 1e-9);
  }
}
