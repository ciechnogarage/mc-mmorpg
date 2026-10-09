package pl.mcmmorpg.chowance.logika;

import java.util.ArrayList;
import java.util.List;
import pl.mcmmorpg.chowance.model.Ewolucja;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Osobnik;

/** Exp, poziomy, nauka ruchów, ewolucja (SPEC §3, §7). Czysta logika. */
public final class Rozwoj {
  private Rozwoj() {}

  /** Wynik dodania exp. */
  public record Awans(int zPoziomu, int naPoziom, List<String> nauczone, List<String> doNauki, boolean naLimicie) {
    public boolean awansowal() { return naPoziom > zPoziomu; }
  }

  /**
   * Dodaje exp z limitem poziomu (odznaki). Na limicie exp się nie kumuluje ponad próg limitu.
   * Nowe ruchy: wchodzą same, gdy jest wolny slot (< 4); inaczej trafiają do „doNauki” (gracz wymienia /chw naucz).
   */
  public static Awans dodajExp(Osobnik o, Gatunek g, long ile, int limitPoziomu) {
    int z = o.poziom;
    int limit = Math.max(1, Math.min(100, limitPoziomu));
    // Na limicie (albo POWYŻEJ — stwór z handlu/od admina) exp się nie zmienia. Wcześniej był tu
    // „min(exp, próg limitu)”, co stworowi powyżej limitu kasowało exp (PROBLEMY A-013).
    if (ile <= 0 || o.poziom >= limit) {
      return new Awans(z, o.poziom, List.of(), List.of(), o.poziom >= limit);
    }
    long sufit = g.krzywa().lacznyExp(limit);
    o.exp = Math.min(sufit, o.exp + ile);
    List<String> nauczone = new ArrayList<>();
    List<String> doNauki = new ArrayList<>();
    while (o.poziom < limit && o.exp >= g.krzywa().lacznyExp(o.poziom + 1)) {
      o.poziom++;
      for (String r : g.ruchyNaPoziomie(o.poziom)) {
        if (o.ruchy.contains(r)) continue;
        if (o.ruchy.size() < 4) { o.ruchy.add(r); nauczone.add(r); }
        else doNauki.add(r);
      }
    }
    return new Awans(z, o.poziom, nauczone, doNauki, o.poziom >= limit);
  }

  /** Postęp w obrębie poziomu 0..1. */
  public static double postep(Osobnik o, Gatunek g) {
    if (o.poziom >= 100) return 1.0;
    long a = g.krzywa().lacznyExp(o.poziom);
    long b = g.krzywa().lacznyExp(o.poziom + 1);
    return b <= a ? 1.0 : Math.max(0, Math.min(1, (double) (o.exp - a) / (b - a)));
  }

  public static long expDoNastepnego(Osobnik o, Gatunek g) {
    if (o.poziom >= 100) return 0;
    return Math.max(0, g.krzywa().lacznyExp(o.poziom + 1) - o.exp);
  }

  /**
   * Exp za pokonanie: exp_bazowy * poziom / 7 * (trener ? 1.5 : 1) * premia.
   */
  public static long expZaPokonanie(Gatunek pokonany, int poziomPokonanego, boolean trener, double premia) {
    double v = pokonany.expBazowy() * (double) poziomPokonanego / 7.0 * (trener ? 1.5 : 1.0) * premia;
    return Math.max(1, Math.round(v));
  }

  /** Pierwsza gałąź ewolucji spełniona przez osobnika (przedmiot = ID MMOItems w ręce albo null). */
  public static Ewolucja dostepnaEwolucja(Osobnik o, Gatunek g, String przedmiotWRece) {
    for (Ewolucja e : g.ewolucje()) {
      if (o.poziom < e.poziom()) continue;
      if (o.wiez < e.wiez()) continue;
      if (e.wymagaPrzedmiotu() && !e.przedmiot().equals(przedmiotWRece)) continue;
      return e;
    }
    return null;
  }

  /** Czy jakakolwiek gałąź jest spełniona pomijając przedmiot (do placeholdera „mozna_ewoluowac”). */
  public static Ewolucja ewolucjaBezPrzedmiotu(Osobnik o, Gatunek g) {
    for (Ewolucja e : g.ewolucje()) {
      if (o.poziom >= e.poziom() && o.wiez >= e.wiez()) return e;
    }
    return null;
  }

  /**
   * Zamienia gatunek. Poziom, exp, geny, charakter, ruchy zostają; cecha: ta sama pozycja na liście
   * nowego gatunku (albo pierwsza). Exp przeliczany, gdy nowa krzywa jest inna — poziom się nie zmienia.
   */
  public static void ewoluuj(Osobnik o, Gatunek stary, Gatunek nowy) {
    int idx = Math.max(0, stary.cechy().indexOf(o.cecha));
    o.cecha = idx < nowy.cechy().size() ? nowy.cechy().get(idx) : nowy.cechy().get(0);
    if (stary.krzywa() != nowy.krzywa()) {
      double p = postep(o, stary);
      long a = nowy.krzywa().lacznyExp(o.poziom);
      long b = nowy.krzywa().lacznyExp(Math.min(100, o.poziom + 1));
      o.exp = a + (long) Math.floor((b - a) * p);
    }
    o.gatunek = nowy.id();
  }

  /** Czy ruch może być nauczony: z puli gatunku do bieżącego poziomu. */
  public static boolean moznaNauczyc(Osobnik o, Gatunek g, String ruch) {
    return g.ruchyDo(o.poziom).contains(ruch) && !o.ruchy.contains(ruch);
  }
}
