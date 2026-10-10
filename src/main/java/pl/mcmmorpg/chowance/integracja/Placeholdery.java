package pl.mcmmorpg.chowance.integracja;

import java.util.List;
import java.util.Map;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.dane.Jajo;
import pl.mcmmorpg.chowance.dane.ProfilDane;
import pl.mcmmorpg.chowance.gra.Bestiariusz;
import pl.mcmmorpg.chowance.gra.Druzyny;
import pl.mcmmorpg.chowance.gra.Hodowla;
import pl.mcmmorpg.chowance.gra.Lapanie;
import pl.mcmmorpg.chowance.gra.Pojedynki;
import pl.mcmmorpg.chowance.gra.Przywolania;
import pl.mcmmorpg.chowance.gra.Ranking;
import pl.mcmmorpg.chowance.gra.Turnieje;
import pl.mcmmorpg.chowance.gra.WalkiTurowe;
import pl.mcmmorpg.chowance.logika.Rozwoj;
import pl.mcmmorpg.chowance.logika.Kroki;
import pl.mcmmorpg.chowance.logika.Statystyki;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Osobnik;
import pl.mcmmorpg.chowance.model.Stat;
import pl.mcmmorpg.chowance.model.Typ;

/** %chowance_*% (KONTRAKT §14). Wszystko z pamięci — zero I/O w wątku głównym. */
public final class Placeholdery extends PlaceholderExpansion {
  private final Chowance c;
  private final Druzyny druzyny;
  private final Przywolania przywolania;
  private final Bestiariusz best;
  private final Ranking ranking;
  private final Hodowla hodowla;
  private final Lapanie lapanie;
  private final WalkiTurowe walki;
  private final Pojedynki pojedynki;
  private final Turnieje turnieje;
  /** HUD walki (panele MythicHUD czytają %chowance_walka_*%); ustawiany po utworzeniu. */
  public pl.mcmmorpg.chowance.gra.HudWalki hud;

  public Placeholdery(Chowance c, Druzyny druzyny, Przywolania przywolania, Bestiariusz best, Ranking ranking,
      Hodowla hodowla, Lapanie lapanie, WalkiTurowe walki, Pojedynki pojedynki, Turnieje turnieje) {
    this.c = c; this.druzyny = druzyny; this.przywolania = przywolania; this.best = best; this.ranking = ranking;
    this.hodowla = hodowla; this.lapanie = lapanie; this.walki = walki; this.pojedynki = pojedynki; this.turnieje = turnieje;
  }

  @Override public String getIdentifier() { return "chowance"; }
  @Override public String getAuthor() { return "mcmmorpg"; }
  @Override public String getVersion() { return c.plugin.getPluginMeta().getVersion(); }
  @Override public boolean persist() { return true; }

  @Override
  public String onRequest(OfflinePlayer op, String k) {
    try {
      return wartosc(op, k);
    } catch (RuntimeException e) {
      return "";
    }
  }

  /** %chowance_walka_*% — stan HUD walki (MythicHUD: panele stworów i lista klawiszy). */
  private String walka(Player p, String k) {
    var st = hud == null ? null : hud.stan(p.getUniqueId());
    if (k.equals("aktywna")) return st != null ? "tak" : "nie";
    if (k.equals("klawisze") && st == null) return "nie";
    if (st == null) return k.endsWith("hp") || k.endsWith("hpmax") ? "0" : "";
    switch (k) {
      case "klawisze": return st.klawisze.isEmpty() ? "nie" : "tak";
      case "naglowek": return st.naglowek;
      case "pozostalo_tury": return st.pozostaloTury;
      case "kto": return st.kto;
      case "on_nazwa": return st.onNazwa;
      case "on_opis": return st.onOpis;
      case "on_hp": return String.valueOf((int) Math.ceil(st.onHp));
      case "on_hpmax": return String.valueOf(st.onMax);
      case "on_hptekst": return (int) Math.ceil(st.onHp) + "/" + st.onMax;
      case "ja_nazwa": return st.jaNazwa;
      case "ja_opis": return st.jaOpis;
      case "ja_hp": return String.valueOf((int) Math.ceil(st.jaHp));
      case "ja_hpmax": return String.valueOf(st.jaMax);
      case "ja_hptekst": return (int) Math.ceil(st.jaHp) + "/" + st.jaMax;
      default:
        if (k.startsWith("k")) {
          try {
            int i = Integer.parseInt(k.substring(1)) - 1;
            var l = st.klawisze;
            return i >= 0 && i < l.size() ? l.get(i) : "";
          } catch (NumberFormatException e) { return ""; }
        }
        return "";
    }
  }

  private String wartosc(OfflinePlayer op, String k) {
    if (k.startsWith("ranking_top_")) return top(k.substring("ranking_top_".length()));
    if (k.equals("turniej_stan")) return turnieje.stan();
    if (!(op instanceof Player p) || !p.isOnline()) return "";
    if (k.startsWith("walka_")) return walka(p, k.substring(6));
    if (k.equals("pozostalo_tury")) return walka(p, k);
    if (k.equals("ma_zadanie_mmocore")) {
      String nazwa = PlaceholderAPI.setPlaceholders(p, "%mmocore_quest%");
      return nazwa != null && !nazwa.isBlank() && !nazwa.equalsIgnoreCase("None")
          && !nazwa.equals("%mmocore_quest%") ? "tak" : "nie";
    }
    ProfilDane d = c.dane(p);
    if (d == null) return k.endsWith("_istnieje") || k.equals("starter_wybrany") ? "nie" : "";
    switch (k) {
      case "kroki_postep": return Kroki.postep(d.pierwszeKrokiMaska) + "/4";
      case "kroki_opis": { int n = Kroki.nastepny(d.pierwszeKrokiMaska); return n == 0 ? "Ukończone" : c.ust.krokiOpisy.get(n - 1); }
      case "kroki_aktywne": return c.ust.pierwszeKrokiWlaczone && d.starter && Kroki.nastepny(d.pierwszeKrokiMaska) != 0 ? "true" : "false";
      case "druzyna_liczba": return String.valueOf(d.druzyna.size());
      case "druzyna_limit": return String.valueOf(c.limitDruzyny(p));
      case "magazyn_liczba": return String.valueOf(d.magazyn.size());
      case "magazyn_limit": return String.valueOf(c.limitMagazynu(p));
      case "magazyn_strona": return String.valueOf(d.stronaMagazynu + 1);
      case "magazyn_stron": return String.valueOf(Math.max(1, (d.magazyn.size() + c.ust.stronaMagazynu - 1) / c.ust.stronaMagazynu));
      case "magazyn_strona_nast": return String.valueOf(Math.min(Math.max(1, (d.magazyn.size() + c.ust.stronaMagazynu - 1) / c.ust.stronaMagazynu), d.stronaMagazynu + 2));
      case "magazyn_strona_poprz": return String.valueOf(Math.max(1, d.stronaMagazynu));
      case "bestiariusz_stron": return String.valueOf(Math.max(1, (best.wszystkie() + c.ust.stronaBestiariusza - 1) / c.ust.stronaBestiariusza));
      case "bestiariusz_strona_nast": return String.valueOf(Math.min(Math.max(1, (best.wszystkie() + c.ust.stronaBestiariusza - 1) / c.ust.stronaBestiariusza), d.stronaBestiariusza + 2));
      case "bestiariusz_strona_poprz": return String.valueOf(Math.max(1, d.stronaBestiariusza));
      case "bestiariusz_widziane": return String.valueOf(best.widziane(d));
      case "bestiariusz_zlapane": return String.valueOf(best.zlapane(d));
      case "bestiariusz_wszystkie": return String.valueOf(best.wszystkie());
      case "bestiariusz_procent": return String.valueOf(best.procent(d));
      case "bestiariusz_strona": return String.valueOf(d.stronaBestiariusza + 1);
      case "odznaki_liczba": return String.valueOf(d.odznaki.size());
      case "limit_poziomu": return String.valueOf(c.limitPoziomu(d));
      case "ranking_elo": return String.valueOf(ranking.elo(d));
      case "ranking_pozycja": { int poz = ranking.pozycja(d.profil); return poz == 0 ? "-" : String.valueOf(poz); }
      case "oswajanie_postep": return String.valueOf(lapanie.postepOswajania(p));
      case "hodowla_postep_procent": return String.valueOf(hodowla.postep(d));
      case "hodowla_1_nazwa": return d.hodowlaA == null ? "" : c.nazwa(d.hodowlaA);
      case "hodowla_2_nazwa": return d.hodowlaB == null ? "" : c.nazwa(d.hodowlaB);
      case "hodowla_1_istnieje": return d.hodowlaA == null ? "nie" : "tak";
      case "hodowla_2_istnieje": return d.hodowlaB == null ? "nie" : "tak";
      case "hodowla_uwaga": { String n = hodowla.niezgodnosc(d); return n == null ? "Para pasuje — jajo w drodze." : n; }
      case "starter_wybrany": return d.starter ? "tak" : "nie";
      case "w_walce": return walki.wWalce(p) || pojedynki.wPojedynku(p) ? "tak" : "nie";
      default: break;
    }
    if (k.startsWith("aktywny_")) {
      Osobnik o = przywolania.osobnikAktywny(p);
      String pole = k.substring("aktywny_".length());
      if (pole.equals("istnieje")) return o == null ? "nie" : "tak";
      return o == null ? "" : pole(p, d, o, pole);
    }
    if (k.startsWith("druzyna_")) {
      String[] cz = k.split("_", 3);
      if (cz.length < 3) return "";
      int n = parse(cz[1]) - 1;
      Osobnik o = n >= 0 && n < d.druzyna.size() ? d.druzyna.get(n) : null;
      if (cz[2].equals("istnieje")) return o == null ? "nie" : "tak";
      if (o == null) return pustyIkona(cz[2]);
      return pole(p, d, o, cz[2]);
    }
    if (k.startsWith("magazyn_")) {
      String[] cz = k.split("_", 3);
      if (cz.length < 3) return "";
      int nr = d.stronaMagazynu * c.ust.stronaMagazynu + parse(cz[1]) - 1;
      Osobnik o = nr >= 0 && nr < d.magazyn.size() ? d.magazyn.get(nr) : null;
      if (cz[2].equals("istnieje")) return o == null ? "nie" : "tak";
      if (o == null) return pustyIkona(cz[2]);
      if (cz[2].equals("nr")) return String.valueOf(nr + 1);
      return pole(p, d, o, cz[2]);
    }
    if (k.startsWith("bestiariusz_")) {
      String[] cz = k.split("_", 3);
      if (cz.length < 3) return "";
      int nr = d.stronaBestiariusza * c.ust.stronaBestiariusza + parse(cz[1]) - 1;
      List<Gatunek> all = c.rej.bestiariusz();
      if (nr < 0 || nr >= all.size()) return pustyIkona(cz[2]);
      Gatunek g = all.get(nr);
      int st = d.stanBestiariusza(g.id());
      return switch (cz[2]) {
        case "stan" -> st >= 2 ? "ZLAPANY" : st == 1 ? "WIDZIANY" : "NIEZNANY";
        case "nazwa" -> "#" + String.format("%02d", nr + 1) + " " + (st >= 1 ? g.nazwa() : "???");
        case "id" -> g.id();
        case "ikona" -> g.ikona(false);
        case "material" -> g.ikona(false).isBlank() ? "PAPER" : "nexo-" + g.ikona(false);
        default -> "";
      };
    }
    if (k.startsWith("gatunek_") && k.endsWith("_stan")) {
      String id = k.substring("gatunek_".length(), k.length() - "_stan".length());
      int st = d.stanBestiariusza(id);
      return st >= 2 ? "ZLAPANY" : st == 1 ? "WIDZIANY" : "NIEZNANY";
    }
    if (k.startsWith("odznaka_")) return d.odznaki.contains(k.substring("odznaka_".length())) ? "tak" : "nie";
    if (k.startsWith("wieza_") && k.endsWith("_rekord")) {
      return String.valueOf(d.wieza.getOrDefault(k.substring("wieza_".length(), k.length() - "_rekord".length()), 0));
    }
    if (k.startsWith("jajo_")) {
      String[] cz = k.split("_", 3);
      if (cz.length < 3) return "";
      int n = parse(cz[1]) - 1;
      Jajo j = n >= 0 && n < d.jaja.size() ? d.jaja.get(n) : null;
      if (cz[2].equals("istnieje")) return j == null ? "nie" : "tak";
      if (j == null) return "";
      if (cz[2].equals("postep_procent")) return String.valueOf((int) Math.min(100, j.przebyte * 100 / Math.max(1, j.wymagane)));
      return "";
    }
    return null;
  }

  /** Pusty slot: ikona/materiał zastępczy (DeluxeMenus nie otwiera okna z materiałem „nexo-”), reszta pusta. */
  private String pustyIkona(String pole) {
    return switch (pole) {
      case "ikona" -> c.ust.ikonaPustegoSlotu;
      case "material" -> "nexo-" + c.ust.ikonaPustegoSlotu;
      default -> "";
    };
  }

  private String pole(Player p, ProfilDane d, Osobnik o, String pole) {
    Gatunek g = c.gatunek(o);
    if (g == null) return "";
    if (pole.startsWith("ruch_")) {
      String[] cz = pole.split("_");
      int i = parse(cz[1]) - 1;
      if (i < 0 || i >= o.ruchy.size()) return "";
      var r = c.rej.ruch(o.ruchy.get(i));
      if (r == null) return "";
      return cz.length > 2 && cz[2].equals("typ") ? r.typ().nazwa() : r.nazwa();
    }
    return switch (pole) {
      case "nazwa" -> c.nazwa(o);
      case "gatunek" -> g.id();
      case "gatunek_nazwa" -> g.nazwa();
      case "poziom" -> String.valueOf(o.poziom);
      case "hp" -> String.valueOf((int) Math.ceil(druzyny.hp(p, o)));
      case "hpmax" -> String.valueOf(druzyny.hpMax(o));
      case "hp_procent" -> String.valueOf((int) Math.round(druzyny.hp(p, o) * 100 / Math.max(1, druzyny.hpMax(o))));
      case "exp" -> String.valueOf(o.exp);
      case "exp_do" -> String.valueOf(Rozwoj.expDoNastepnego(o, g));
      case "exp_procent" -> String.valueOf((int) Math.floor(Rozwoj.postep(o, g) * 100));
      case "typ" -> typy(g.typy());
      case "cecha" -> { var ce = c.rej.cecha(o.cecha); yield ce == null ? "" : ce.nazwa(); }
      case "charakter" -> { var ch = c.rej.charakter(o.charakter); yield ch == null ? "" : ch.nazwa(); }
      case "wiez" -> String.valueOf(o.wiez);
      case "lsniacy" -> o.lsniacy ? "tak" : "nie";
      case "ikona" -> g.ikona(o.lsniacy);
      case "material" -> g.ikona(o.lsniacy).isBlank() ? "PAPER" : "nexo-" + g.ikona(o.lsniacy);
      case "aktywny" -> { var a = przywolania.aktywny(p); yield a != null && a.osobnikId.equals(o.id) ? "tak" : "nie"; }
      case "zemdlony" -> druzyny.zemdlony(p, o) ? "tak" : "nie";
      case "mozna_ewoluowac" -> Rozwoj.ewolucjaBezPrzedmiotu(o, g) != null ? "tak" : "nie";
      case "staty" -> staty(g, o);
      default -> "";
    };
  }

  private String staty(Gatunek g, Osobnik o) {
    int[] s = Statystyki.wszystkie(g, o, c.rej.charakter(o.charakter), c.ust.premiaWiezi, c.rej.cecha(o.cecha));
    StringBuilder sb = new StringBuilder();
    for (Stat st : Stat.values()) sb.append(st.skrot()).append(' ').append(s[st.ordinal()]).append(st.ordinal() < 5 ? " · " : "");
    return sb.toString();
  }

  private static String typy(List<Typ> t) {
    StringBuilder sb = new StringBuilder();
    for (Typ x : t) sb.append(sb.length() > 0 ? "/" : "").append(x.nazwa());
    return sb.toString();
  }

  private String top(String reszta) {
    String[] cz = reszta.split("_", 2);
    int n = parse(cz[0]) - 1;
    var t = ranking.top();
    if (n < 0 || n >= t.size() || cz.length < 2) return "";
    Map.Entry<String, pl.mcmmorpg.chowance.gra.Ranking.Wpis> e = t.get(n);
    return cz[1].equals("elo") ? String.valueOf(e.getValue().elo) : cz[1].equals("nazwa") ? e.getValue().nick : "";
  }

  private static int parse(String s) {
    try { return Integer.parseInt(s); } catch (NumberFormatException e) { return -1; }
  }
}
