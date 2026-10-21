package pl.mcmmorpg.chowance.logika;

import pl.mcmmorpg.chowance.model.Charakter;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Osobnik;
import pl.mcmmorpg.chowance.model.Stat;

/**
 * Wzory statystyk (jawne, testowane):
 * <pre>
 *  HP    = floor((2*B + G) * L / 100) + L + 10
 *  inne  = floor((floor((2*B + G) * L / 100) + 5) * charakter)
 *  więź  = * (1 + premiaWiezi * wiez/255)   (premiaWiezi z config, domyślnie 0.05)
 * </pre>
 * B = stat bazowy gatunku, G = gen 0..31, L = poziom.
 */
public final class Statystyki {
  public static final int GEN_MAX = 31;

  private Statystyki() {}

  /**
   * Stat z cechą: MNOZNIK_STATU bez warunku `gdy` jest stałą częścią statu (także ZDROWIE — PROBLEMY B-004);
   * z warunkiem liczy go dopiero walka (SilnikWalki), bo zależy od środowiska.
   */
  public static int stat(Gatunek g, Osobnik o, Charakter ch, Stat s, double premiaWiezi, pl.mcmmorpg.chowance.model.Cecha cecha) {
    int v = stat(g, o, ch, s, premiaWiezi);
    if (cecha != null && cecha.rodzaj() == pl.mcmmorpg.chowance.model.RodzajCechy.MNOZNIK_STATU && cecha.stat() == s && cecha.gdy().isEmpty()) {
      v = (int) Math.floor(v * cecha.d("mnoznik", 1.0));
    }
    return Math.max(1, v);
  }

  public static int[] wszystkie(Gatunek g, Osobnik o, Charakter ch, double premiaWiezi, pl.mcmmorpg.chowance.model.Cecha cecha) {
    int[] r = new int[Stat.values().length];
    for (Stat s : Stat.values()) r[s.ordinal()] = stat(g, o, ch, s, premiaWiezi, cecha);
    return r;
  }

  public static int stat(Gatunek g, Osobnik o, Charakter ch, Stat s, double premiaWiezi) {
    int b = g.bazowy(s);
    int gen = o.geny[s.ordinal()];
    int l = o.poziom;
    double wiezMn = 1.0 + premiaWiezi * Math.max(0, Math.min(255, o.wiez)) / 255.0;
    if (s == Stat.ZDROWIE) {
      int hp = (2 * b + gen) * l / 100 + l + 10;
      return (int) Math.floor(hp * wiezMn);
    }
    int base = (2 * b + gen) * l / 100 + 5;
    double chm = ch == null ? 1.0 : ch.mnoznik(s);
    return (int) Math.floor(Math.floor(base * chm) * wiezMn);
  }

  public static int[] wszystkie(Gatunek g, Osobnik o, Charakter ch, double premiaWiezi) {
    int[] r = new int[Stat.values().length];
    for (Stat s : Stat.values()) r[s.ordinal()] = stat(g, o, ch, s, premiaWiezi);
    return r;
  }

  /**
   * Bieżące HP z leniwą regeneracją poza walką. hp < 0 znaczy „pełne”.
   * @param procentNaMinute ile % max HP wraca na minutę
   * @param zemdlonyTez czy zemdlony (hp == 0) też się regeneruje
   */
  public static double biezaceHp(Osobnik o, int maxHp, long teraz, double procentNaMinute, boolean zemdlonyTez) {
    if (o.hp < 0) return maxHp;
    double hp = Math.min(o.hp, maxHp);
    if (hp <= 0 && !zemdlonyTez) return 0;
    if (o.hpCzas <= 0 || teraz <= o.hpCzas || procentNaMinute <= 0) return hp;
    double minuty = (teraz - o.hpCzas) / 60000.0;
    return Math.min(maxHp, hp + maxHp * procentNaMinute / 100.0 * minuty);
  }

  /** Utrwala bieżące HP (po regeneracji) jako nowy punkt odniesienia. */
  public static void ustawHp(Osobnik o, double hp, int maxHp, long teraz) {
    double v = Math.max(0, Math.min(maxHp, hp));
    o.hp = v >= maxHp ? -1 : v;
    o.hpCzas = teraz;
  }
}
