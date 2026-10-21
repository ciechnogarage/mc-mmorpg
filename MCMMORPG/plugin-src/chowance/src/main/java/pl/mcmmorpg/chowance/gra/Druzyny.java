package pl.mcmmorpg.chowance.gra;

import java.util.Map;
import org.bukkit.entity.Player;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.dane.ProfilDane;
import pl.mcmmorpg.chowance.logika.Rozwoj;
import pl.mcmmorpg.chowance.logika.Statystyki;
import pl.mcmmorpg.chowance.logika.Wojownik;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Osobnik;
import pl.mcmmorpg.chowance.model.RodzajCechy;
import pl.mcmmorpg.chowance.model.Stat;

/** Operacje na drużynie i magazynie profilu — jedno miejsce z regułami (limity, związanie, HP). */
public final class Druzyny {
  private final Chowance c;
  Przywolania przywolania;
  Bestiariusz bestiariusz;

  public void ustawBestiariusz(Bestiariusz b) { this.bestiariusz = b; }

  public Druzyny(Chowance c) { this.c = c; }

  public enum Gdzie { DRUZYNA, MAGAZYN, BRAK_MIEJSCA }

  /** Dodaje nowego stwora (złapany, wykluty, starter, handel...). */
  public Gdzie dodaj(Player p, ProfilDane d, Osobnik o, boolean nowyWBestiariuszu) {
    if (o.pierwszyWlasciciel == null) o.pierwszyWlasciciel = d.profil;
    if (o.zlapany == 0) o.zlapany = System.currentTimeMillis();
    Gdzie g;
    if (d.druzyna.size() < c.limitDruzyny(p)) { d.druzyna.add(o); g = Gdzie.DRUZYNA; }
    else if (d.magazyn.size() < c.limitMagazynu(p)) { d.magazyn.add(o); g = Gdzie.MAGAZYN; }
    else return Gdzie.BRAK_MIEJSCA;
    if (nowyWBestiariuszu) {
      d.oznaczZlapany(o.gatunek, o.lsniacy);
      if (bestiariusz != null) bestiariusz.sprawdzNagrody(p, d);
    }
    d.brudne = true;
    return g;
  }

  public int maxPoziom(ProfilDane d) {
    int m = 1;
    for (Osobnik o : d.druzyna) m = Math.max(m, o.poziom);
    return m;
  }

  public int hpMax(Osobnik o) {
    Gatunek g = c.gatunek(o);
    return g == null ? 1 : Statystyki.stat(g, o, c.rej.charakter(o.charakter), Stat.ZDROWIE, c.ust.premiaWiezi, c.rej.cecha(o.cecha));
  }

  /** Bieżące HP: z ciała, gdy przywołany, inaczej z zapisu + leniwa regeneracja. */
  public double hp(Player wlasciciel, Osobnik o) {
    if (przywolania != null && wlasciciel != null) {
      Double z = przywolania.hpCiala(wlasciciel, o);
      if (z != null) return Math.min(z, hpMax(o));
    }
    return Statystyki.biezaceHp(o, hpMax(o), System.currentTimeMillis(), c.ust.regenNaMinute, c.ust.regenZemdlony);
  }

  /**
   * Wyczerpany = mniej niż 1 HP. Regeneracja poza walką działa też na wyczerpanych (config), ale stwór wraca
   * do gry dopiero od 1 HP — inaczej po sekundzie miałby ułamek HP i dało się go od razu przywołać (PROBLEMY A-009).
   */
  public static final double PROG_WYCZERPANIA = 1.0;

  public boolean zemdlony(Player p, Osobnik o) { return hp(p, o) < PROG_WYCZERPANIA; }

  public void ustawHp(Osobnik o, double hp) {
    Statystyki.ustawHp(o, hp, hpMax(o), System.currentTimeMillis());
  }

  public Wojownik wojownik(Player p, Osobnik o) {
    double h = hp(p, o);
    return new Wojownik(c.rej, o, c.ust.premiaWiezi, h < PROG_WYCZERPANIA ? 0 : h);
  }

  public void leczWszystkich(Player p, ProfilDane d, double procent) {
    for (Osobnik o : d.druzyna) {
      int max = hpMax(o);
      double nowe = Math.min(max, hp(p, o) + max * procent / 100.0);
      ustawHp(o, nowe);
      if (przywolania != null) przywolania.odswiezZdrowie(p, o);
    }
    d.brudne = true;
  }

  /** Exp z komunikatami, nauką ruchów, sygnałem OKRZYK i odświeżeniem ciała. */
  public Rozwoj.Awans dajExp(Player p, ProfilDane d, Osobnik o, long ile) { return dajExp(p, d, o, ile, false); }

  /** {@code cicho} = bez paska „+X exp” (dzielony exp reszty drużyny nie nadpisuje paska walczącego). */
  public Rozwoj.Awans dajExp(Player p, ProfilDane d, Osobnik o, long ile, boolean cicho) {
    Gatunek g = c.gatunek(o);
    if (g == null || ile <= 0) return null;
    double hpPrzed = hp(p, o);
    int maxPrzed = hpMax(o);
    var cecha = c.rej.cecha(o.cecha);
    if (cecha != null && cecha.rodzaj() == RodzajCechy.PREMIA_EXP) ile = Math.round(ile * cecha.d("mnoznik", 1.0));
    Rozwoj.Awans a = Rozwoj.dodajExp(o, g, ile, c.limitPoziomu(d));
    d.brudne = true;
    // Widoczny postęp (recenzja R-03): „+X exp” z paskiem do następnego poziomu przy każdym zdobyciu.
    if (!cicho && (!a.naLimicie() || a.awansowal())) {
      int proc = (int) Math.round(Rozwoj.postep(o, g) * 100);
      int pelne = proc / 10;
      c.pasek(p, "<aqua>+" + ile + " exp</aqua> " + c.nazwaKolor(o) + " <gray>poz. " + o.poziom + " <aqua>" + "▮".repeat(pelne)
          + "<dark_gray>" + "▮".repeat(10 - pelne) + " <gray>" + proc + "%");
    }
    if (a.awansowal()) {
      p.showTitle(net.kyori.adventure.title.Title.title(Chowance.mm("<gold><b>" + c.nazwa(o) + " — poziom " + a.naPoziom() + "!"),
          Chowance.mm(a.nauczone().isEmpty() ? "<gray>statystyki rosną" : "<aqua>Nowy ruch: " + nazwaRuchu(a.nauczone().get(0))),
          net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(150), java.time.Duration.ofMillis(2200), java.time.Duration.ofMillis(400))));
      p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
      // Awans podnosi max HP — obecne rośnie o tyle samo (jak w seriach), nie leczy do pełna.
      ustawHp(o, hpPrzed + (hpMax(o) - maxPrzed));
      c.msg(p, c.nazwaKolor(o) + " <green>osiąga poziom <yellow>" + a.naPoziom() + "</yellow>!");
      for (String r : a.nauczone()) c.msg(p, c.nazwaKolor(o) + " uczy się ruchu <aqua>" + nazwaRuchu(r) + "</aqua>.");
      for (String r : a.doNauki()) c.msg(p, c.nazwaKolor(o) + " może nauczyć się <aqua>" + nazwaRuchu(r)
          + "</aqua> — " + linkKarty(d, o, "naucz"));
      if (Rozwoj.ewolucjaBezPrzedmiotu(o, g) != null) c.msg(p, c.nazwaKolor(o) + " <light_purple>jest gotów do przemiany! " + linkKarty(d, o, "przemiana"));
      if (przywolania != null) {
        przywolania.odswiezZdrowie(p, o);
        przywolania.sygnalAktywnemu(p, o, "OKRZYK");
      }
    } else if (a.naLimicie() && o.poziom >= c.limitPoziomu(d)) {
      c.pasek(p, "<gray>" + c.nazwa(o) + " jest na limicie poziomu (" + c.limitPoziomu(d) + ") — zdobądź odznakę Mistrza Więzi.");
    }
    return a;
  }

  /** Klikalny odnośnik do karty stwora (w magazynie — do menu). Gracz nigdy nie przepisuje komendy. */
  private String linkKarty(ProfilDane d, Osobnik o, String co) {
    int n = d.druzyna.indexOf(o) + 1;
    String cmd = n > 0 ? "/chw karta " + n : "/chw menu";
    return "<yellow><click:run_command:'" + cmd + "'><hover:show_text:'Otwórz kartę chowańca'>[" + co + "]</hover></click></yellow>";
  }

  public String nazwaRuchu(String id) {
    var r = c.rej.ruch(id);
    return r == null ? id : r.nazwa();
  }

  public void dodajWiez(Osobnik o, int ile) {
    var cecha = c.rej.cecha(o.cecha);
    if (cecha != null && cecha.rodzaj() == RodzajCechy.PREMIA_WIEZI) ile = (int) Math.round(ile * cecha.d("mnoznik", 1.0));
    o.wiez = Math.max(0, Math.min(c.ust.wiezMax, o.wiez + ile));
  }

  public Map<String, String> ph(Player p, Osobnik o) {
    return Map.of("gracz", p.getName(), "gatunek", o.gatunek, "nazwa", c.nazwa(o), "poziom", String.valueOf(o.poziom),
        "lsniacy", o.lsniacy ? "tak" : "nie");
  }

  /** Slot 1-based -> osobnik z drużyny albo null. */
  public static Osobnik slot(ProfilDane d, String arg) {
    try {
      int i = Integer.parseInt(arg) - 1;
      return i >= 0 && i < d.druzyna.size() ? d.druzyna.get(i) : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
