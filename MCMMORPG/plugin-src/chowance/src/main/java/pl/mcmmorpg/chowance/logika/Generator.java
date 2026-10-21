package pl.mcmmorpg.chowance.logika;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Osobnik;
import pl.mcmmorpg.chowance.model.Stat;
import pl.mcmmorpg.chowance.tresc.Rejestr;

/** Tworzy nowe osobniki: geny, charakter, cecha, 4 ruchy (SPEC §3). */
public final class Generator {
  private Generator() {}

  public static Osobnik nowy(Rejestr r, Gatunek g, int poziom, boolean lsniacy, RandomGenerator rng) {
    Osobnik o = new Osobnik();
    o.gatunek = g.id();
    o.poziom = Math.max(1, Math.min(100, poziom));
    o.exp = g.krzywa().lacznyExp(o.poziom);
    for (Stat s : Stat.values()) o.geny[s.ordinal()] = rng.nextInt(Statystyki.GEN_MAX + 1);
    List<String> ch = new ArrayList<>(r.charaktery().keySet());
    ch.sort(null);
    o.charakter = ch.get(rng.nextInt(ch.size()));
    o.cecha = g.cechy().get(rng.nextInt(g.cechy().size()));
    o.ruchy = domyslneRuchy(g, o.poziom);
    o.wiez = g.wiezStartowa();
    o.lsniacy = lsniacy;
    o.hp = -1;
    return o;
  }

  /** 4 najnowsze ruchy dostępne na poziomie. */
  public static List<String> domyslneRuchy(Gatunek g, int poziom) {
    List<String> dost = g.ruchyDo(poziom);
    int od = Math.max(0, dost.size() - 4);
    return new ArrayList<>(dost.subList(od, dost.size()));
  }

  public static boolean losujLsniacy(double szansa, RandomGenerator rng) {
    return szansa > 0 && rng.nextDouble() < szansa;
  }
}
