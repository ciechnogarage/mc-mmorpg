package pl.mcmmorpg.chowance.logika;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Osobnik;
import pl.mcmmorpg.chowance.model.Stat;
import pl.mcmmorpg.chowance.tresc.Rejestr;

/**
 * Hodowla (SPEC §7): zgodność pary i dziedziczenie.
 * <ul>
 *  <li>zgodne: dwa RÓŻNE osobniki, oba płodne, wspólna grupa jaj;</li>
 *  <li>gatunek młodego: bazowy gatunek linii jednego z rodziców (50/50);</li>
 *  <li>geny: {@code genyDziedziczone} (domyślnie 3) losowe staty po losowym rodzicu, reszta losowa;</li>
 *  <li>charakter: 50% po losowym rodzicu, inaczej losowy;</li>
 *  <li>cecha: 60% cecha rodzica tej linii, jeśli młody może ją mieć;</li>
 *  <li>lśnienie: szansa bazowa × {@code mnoznikObcy}, gdy rodzice mają różnych pierwszych właścicieli.</li>
 * </ul>
 */
public final class Dziedziczenie {
  private Dziedziczenie() {}

  public static String niezgodnosc(Osobnik a, Osobnik b, Gatunek ga, Gatunek gb) {
    if (a.id.equals(b.id)) return "to ten sam stwór";
    if (!ga.plodny() || !gb.plodny()) return "jeden z rodziców nie może mieć potomstwa";
    for (String gr : ga.grupyJaj()) if (gb.grupyJaj().contains(gr)) return null;
    return "rodzice nie mają wspólnej grupy jaj";
  }

  public static Osobnik mlode(Rejestr r, Osobnik a, Osobnik b, Gatunek ga, Gatunek gb, int genyDziedziczone,
      double szansaLsniacego, double mnoznikObcy, RandomGenerator rng) {
    boolean poA = rng.nextBoolean();
    Osobnik matka = poA ? a : b;
    Gatunek gm = poA ? ga : gb;
    Gatunek baza = r.gatunek(gm.linia());
    if (baza == null) baza = gm;
    double szansa = szansaLsniacego;
    if (a.pierwszyWlasciciel != null && b.pierwszyWlasciciel != null && !a.pierwszyWlasciciel.equals(b.pierwszyWlasciciel)) {
      szansa *= mnoznikObcy;
    }
    Osobnik m = Generator.nowy(r, baza, 1, Generator.losujLsniacy(szansa, rng), rng);
    List<Stat> staty = new ArrayList<>(List.of(Stat.values()));
    Collections.shuffle(staty, new java.util.Random(rng.nextLong()));
    int n = Math.max(0, Math.min(6, genyDziedziczone));
    for (int i = 0; i < n; i++) {
      Stat s = staty.get(i);
      m.geny[s.ordinal()] = (rng.nextBoolean() ? a : b).geny[s.ordinal()];
    }
    if (rng.nextDouble() < 0.5) m.charakter = (rng.nextBoolean() ? a : b).charakter;
    if (rng.nextDouble() < 0.6 && baza.cechy().contains(matka.cecha)) m.cecha = matka.cecha;
    m.pochodzenie = "JAJO";
    return m;
  }
}
