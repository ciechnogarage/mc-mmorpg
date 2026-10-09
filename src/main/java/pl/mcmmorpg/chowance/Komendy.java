package pl.mcmmorpg.chowance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import pl.mcmmorpg.chowance.api.event.ChowaniecEwolucjaEvent;
import pl.mcmmorpg.chowance.api.event.ChowaniecZlapanyEvent;
import pl.mcmmorpg.chowance.dane.Jajo;
import pl.mcmmorpg.chowance.dane.ProfilDane;
import pl.mcmmorpg.chowance.gra.Bestiariusz;
import pl.mcmmorpg.chowance.gra.Druzyny;
import pl.mcmmorpg.chowance.gra.Dzikie;
import pl.mcmmorpg.chowance.gra.Handel;
import pl.mcmmorpg.chowance.gra.Hodowla;
import pl.mcmmorpg.chowance.gra.Okna;
import pl.mcmmorpg.chowance.gra.Pojedynki;
import pl.mcmmorpg.chowance.gra.Przywolania;
import pl.mcmmorpg.chowance.gra.Ranking;
import pl.mcmmorpg.chowance.gra.Startery;
import pl.mcmmorpg.chowance.gra.Turnieje;
import pl.mcmmorpg.chowance.gra.WalkiTurowe;
import pl.mcmmorpg.chowance.gra.Zaproszenia;
import pl.mcmmorpg.chowance.integracja.PrzedmiotMmo;
import pl.mcmmorpg.chowance.logika.Generator;
import pl.mcmmorpg.chowance.logika.Rozwoj;
import pl.mcmmorpg.chowance.logika.Statystyki;
import pl.mcmmorpg.chowance.model.Ewolucja;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Osobnik;
import pl.mcmmorpg.chowance.model.Stat;

/** /chw — komendy gracza i admina (KONTRAKT §13). */
public final class Komendy implements TabExecutor {
  private final ChowancePlugin plg;
  private final Chowance c;

  public Komendy(ChowancePlugin pl) {
    this.plg = pl;
    this.c = plg.ctx();
  }

  private Druzyny dr() { return plg.druzyny; }
  private Przywolania prz() { return plg.przywolania; }

  @Override
  public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
    try {
      if (a.length > 0 && a[0].equalsIgnoreCase("admin")) return admin(s, a);
      if (!(s instanceof Player p)) { s.sendMessage("Tylko dla graczy (albo /chw admin ...)."); return true; }
      gracz(p, a);
    } catch (RuntimeException e) {
      c.log.log(java.util.logging.Level.SEVERE, "Błąd komendy /chw " + String.join(" ", a), e);
      s.sendMessage(Chowance.mm(Chowance.PREFIX + "<red>Wewnętrzny błąd — zgłoś administracji."));
    }
    return true;
  }

  // ================================================================== gracz

  private void gracz(Player p, String[] a) {
    String sub = a.length == 0 ? "pomoc" : a[0].toLowerCase(Locale.ROOT);
    switch (sub) {
      case "pomoc", "help" -> pomoc(p);
      case "menu" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dm open " + c.ust.menuDm + " " + p.getName());
      case "starter" -> plg.startery.otworz(p);
      case "druzyna" -> druzyna(p);
      case "info", "karta" -> info(p, a);
      case "gracze" -> gracze(p);
      case "przywolaj" -> {
        ProfilDane d = c.daneLubBlad(p); if (d == null) return;
        Osobnik o = a.length > 1 ? Druzyny.slot(d, a[1]) : (d.druzyna.isEmpty() ? null : d.druzyna.get(0));
        if (o == null) { c.blad(p, "Nie ma stwora w tym slocie."); return; }
        var ak = prz().aktywny(p);
        if (ak != null && ak.osobnikId.equals(o.id) && !(plg.walki.wWalce(p) || plg.pojedynki.wPojedynku(p))) { prz().odwolaj(p, false); return; }
        prz().przywolaj(p, o);
      }
      case "odwolaj" -> {
        if (plg.walki.wWalce(p) || plg.pojedynki.wPojedynku(p)) { c.blad(p, "Nie w trakcie walki."); return; }
        if (prz().aktywny(p) == null) { c.pasek(p, "<gray>Nikogo nie przywołano."); return; }
        prz().odwolaj(p, false);
      }
      case "zamien" -> zamien(p, a);
      case "magazyn" -> magazyn(p, a);
      case "nazwij" -> nazwij(p, a);
      case "ewoluuj" -> ewoluuj(p, a);
      case "naucz" -> naucz(p, a);
      case "uwolnij" -> uwolnij(p, a);
      case "walka" -> walka(p, a);
      case "pojedynek" -> zapros(p, a, Zaproszenia.Rodzaj.POJEDYNEK);
      case "handel" -> zapros(p, a, Zaproszenia.Rodzaj.HANDEL);
      case "akceptuj" -> akceptuj(p);
      case "odrzuc" -> { var z = plg.zaproszenia.odbierz(p); if (z != null) { Player od = Bukkit.getPlayer(z.od()); if (od != null) c.msg(od, "<red>" + p.getName() + " odrzuca zaproszenie."); } c.pasek(p, "<gray>Odrzucono."); }
      case "poddaj" -> { if (plg.walki.wWalce(p)) plg.walki.przerwij(p); else if (plg.pojedynki.wPojedynku(p)) plg.pojedynki.przerwij(p); }
      case "ruch" -> { if (a.length > 1) plg.pojedynki.ruch(p, parse(a[1]) - 1); }
      case "okno" -> plg.walki.pokazPonownie(p);
      case "hodowla" -> hodowla(p, a);
      case "zamknij" -> plg.okna.zamknij(p);
      case "bestiariusz" -> bestiariusz(p, a);
      case "ranking" -> ranking(p);
      case "turniej" -> turniej(p, a);
      default -> pomoc(p);
    }
  }

  private void pomoc(Player p) {
    c.msg(p, "<gold>Chowańce <gray>— /chw menu, przywolaj <1-6>, odwolaj, druzyna, info <slot>, magazyn, nazwij, ewoluuj, naucz, uwolnij, starter, walka [gracz], pojedynek <gracz>, handel <gracz>, akceptuj, odrzuc, hodowla, bestiariusz, ranking, turniej");
  }

  private void druzyna(Player p) {
    ProfilDane d = c.daneLubBlad(p); if (d == null) return;
    c.msg(p, "<gold>Drużyna " + d.druzyna.size() + "/" + c.limitDruzyny(p) + " <gray>· magazyn " + d.magazyn.size() + "/" + c.limitMagazynu(p) + " · limit poziomu " + c.limitPoziomu(d));
    for (int i = 0; i < d.druzyna.size(); i++) {
      Osobnik o = d.druzyna.get(i);
      var ak = prz().aktywny(p);
      boolean akt = ak != null && ak.osobnikId.equals(o.id);
      p.sendMessage(Chowance.mm(" <yellow>" + (i + 1) + ".</yellow> " + c.nazwaKolor(o) + " <gray>Poz. " + o.poziom + " · " + (int) Math.ceil(dr().hp(p, o)) + "/" + dr().hpMax(o) + " HP"
          + (akt ? " <green>[przywołany]" : "") + (dr().zemdlony(p, o) ? " <red>[wyczerpany]" : "")));
    }
  }

  /** Karta stwora (PPM w Menażerii albo klik w czacie): opis + wszystkie akcje, bez wpisywania komend. */
  private void info(Player p, String[] a) {
    ProfilDane d = c.daneLubBlad(p); if (d == null) return;
    Osobnik o = a.length > 1 ? Druzyny.slot(d, a[1]) : prz().osobnikAktywny(p);
    if (o == null) { c.blad(p, "W tym miejscu drużyny nie ma stwora."); return; }
    karta(p, o);
  }

  private void karta(Player p, Osobnik o) {
    ProfilDane d = c.dane(p); if (d == null || !d.druzyna.contains(o)) return;
    Gatunek g = c.gatunek(o);
    int slot = d.druzyna.indexOf(o) + 1;
    int[] st = Statystyki.wszystkie(g, o, c.rej.charakter(o.charakter), c.ust.premiaWiezi, c.rej.cecha(o.cecha));
    List<String> t = new ArrayList<>();
    t.add(c.nazwaKolor(o) + " <gray>(" + g.nazwa() + ") Poz. " + o.poziom + " · exp " + (int) Math.floor(Rozwoj.postep(o, g) * 100) + "%");
    t.add("<gray>Typ: " + g.typy().stream().map(x -> x.kolor() + x.nazwa()).collect(Collectors.joining("<gray>/")));
    StringBuilder s = new StringBuilder("<white>");
    for (Stat x : Stat.values()) s.append(x.skrot()).append(" ").append(st[x.ordinal()]).append(" <dark_gray>(gen ").append(o.geny[x.ordinal()]).append(")<white>  ");
    t.add(s.toString());
    var ch = c.rej.charakter(o.charakter);
    var ce = c.rej.cecha(o.cecha);
    t.add("<gray>Charakter: <white>" + (ch == null ? "?" : ch.nazwa()) + " <gray>· Cecha: <white>" + (ce == null ? "?" : ce.nazwa() + " <gray>— " + ce.opis()));
    t.add("<gray>Ruchy: <white>" + o.ruchy.stream().map(dr()::nazwaRuchu).collect(Collectors.joining(", ")));
    t.add("<gray>Więź: <light_purple>" + o.wiez + "</light_purple> · HP " + (int) Math.ceil(dr().hp(p, o)) + "/" + dr().hpMax(o)
        + " · pochodzenie: " + (o.pochodzenie == null ? "?" : o.pochodzenie.toLowerCase()) + (o.zwiazany ? " · <red>związany" : "") + (o.siodlo ? " · siodło" : ""));
    Ewolucja e = g.ewolucje().isEmpty() ? null : g.ewolucje().get(0);
    if (e != null) t.add("<gray>Przemiana: " + c.rej.gatunek(e.doGatunku()).nazwa() + " od poz. " + e.poziom() + (e.wymagaPrzedmiotu() ? " + " + c.nazwyMmo.nazwa(e.przedmiot()) : "") + (e.wiez() > 0 ? " + więź " + e.wiez() : ""));

    List<Okna.Przycisk> pr = new ArrayList<>();
    var ak = prz().aktywny(p);
    boolean aktywny = ak != null && ak.osobnikId.equals(o.id);
    if (aktywny) pr.add(Okna.Przycisk.of("<yellow>Odwołaj", "Stwór wraca do pieczęci", x -> gracz(x, new String[] {"odwolaj"})));
    else pr.add(Okna.Przycisk.of("<green>Przywołaj", "Stwór pojawi się przy tobie", x -> gracz(x, new String[] {"przywolaj", String.valueOf(slotOf(x, o))})));
    pr.add(Okna.Przycisk.of("<white>Zmień imię", "Do 16 znaków", x -> oknoImienia(x, o)));
    List<String> nowe = g.ruchyDo(o.poziom).stream().filter(r -> !o.ruchy.contains(r)).toList();
    if (!nowe.isEmpty()) pr.add(Okna.Przycisk.of("<aqua>Naucz ruchu (" + nowe.size() + ")", "Ruchy, które już może poznać", x -> oknoNauki(x, o)));
    if (Rozwoj.ewolucjaBezPrzedmiotu(o, g) != null) pr.add(Okna.Przycisk.of("<light_purple>Przemiana!", e != null && e.wymagaPrzedmiotu() ? "Zużywa: " + c.nazwyMmo.nazwa(e.przedmiot()) : "", x -> { int n = slotOf(x, o); if (n > 0) ewoluuj(x, new String[] {"ewoluuj", String.valueOf(n)}); }));
    if (slot > 1) pr.add(Okna.Przycisk.of("<white>Na początek drużyny", "Pierwszy w drużynie wychodzi do walki", x -> { int n = slotOf(x, o); if (n > 1) zamien(x, new String[] {"zamien", "1", String.valueOf(n)}); karta(x, o); }));
    pr.add(Okna.Przycisk.of("<white>Do magazynu", "", x -> { int n = slotOf(x, o); if (n > 0) magazyn(x, new String[] {"magazyn", "wloz", String.valueOf(n)}); }));
    if (g.plodny()) pr.add(Okna.Przycisk.of("<white>Do zagrody", "Hodowla: para rodziców składa jajo", x -> { int n = slotOf(x, o); if (n > 0) hodowla(x, new String[] {"hodowla", "wloz", String.valueOf(n)}); }));
    pr.add(Okna.Przycisk.of("<red>Wypuść", "Na zawsze", x -> oknoWypuszczenia(x, o)));
    plg.okna.pokaz(p, "<gold>Karta chowańca", t, pr, Okna.Przycisk.of("<gray>Zamknij", "", x -> {}), 2, true, false);
  }

  private int slotOf(Player p, Osobnik o) {
    ProfilDane d = c.dane(p);
    return d == null ? -1 : d.druzyna.indexOf(o) + 1;
  }

  private void oknoImienia(Player p, Osobnik o) {
    plg.okna.pokaz(p, "<gold>Imię chowańca", List.of("<gray>Nowe imię dla " + c.nazwaKolor(o) + "<gray> (do 16 znaków, litery i cyfry). Puste = nazwa gatunku."),
        List.of(Okna.Przycisk.of("<green>Zapisz", "", x -> {
          int n = slotOf(x, o); if (n <= 0) return;
          String imie = plg.okna.tekst(x, "imie");
          if (imie == null || imie.isBlank()) { o.przydomek = null; ProfilDane d = c.dane(x); if (d != null) d.brudne = true; prz().odswiezZdrowie(x, o); c.pasek(x, "<green>Przywrócono nazwę gatunku."); }
          else nazwij(x, (n + " " + imie.trim()).split(" "), true);
          karta(x, o);
        })), Okna.Przycisk.of("<gray>Wstecz", "", x -> karta(x, o)), 1, true, false,
        new Okna.Pole("imie", "Imię", o.przydomek == null ? "" : o.przydomek, 16));
  }

  private void oknoNauki(Player p, Osobnik o) {
    Gatunek g = c.gatunek(o);
    List<Okna.Przycisk> pr = new ArrayList<>();
    for (String r : g.ruchyDo(o.poziom)) {
      if (o.ruchy.contains(r)) continue;
      var ruch = c.rej.ruch(r);
      String opis = ruch == null ? "" : ruch.typ().nazwa() + " · " + ruch.kategoria().name().toLowerCase(Locale.ROOT) + (ruch.moc() > 0 ? " · moc " + ruch.moc() : "");
      pr.add(Okna.Przycisk.of("<aqua>" + dr().nazwaRuchu(r), opis, x -> {
        if (o.ruchy.size() < 4) { int n = slotOf(x, o); if (n > 0) naucz(x, new String[] {"naucz", String.valueOf(n), r, String.valueOf(o.ruchy.size() + 1)}); karta(x, o); }
        else oknoZamianyRuchu(x, o, r);
      }));
    }
    if (pr.isEmpty()) { karta(p, o); return; }
    plg.okna.pokaz(p, "<aqua>Nauka ruchu", List.of("<gray>Którego ruchu ma się nauczyć " + c.nazwaKolor(o) + "<gray>?"), pr, Okna.Przycisk.of("<gray>Wstecz", "", x -> karta(x, o)), 2, true, false);
  }

  private void oknoZamianyRuchu(Player p, Osobnik o, String nowy) {
    List<Okna.Przycisk> pr = new ArrayList<>();
    for (int i = 0; i < o.ruchy.size(); i++) {
      int poz = i + 1;
      pr.add(Okna.Przycisk.of("<white>Zapomnij: " + dr().nazwaRuchu(o.ruchy.get(i)), "", x -> { int n = slotOf(x, o); if (n > 0) naucz(x, new String[] {"naucz", String.valueOf(n), nowy, String.valueOf(poz)}); karta(x, o); }));
    }
    plg.okna.pokaz(p, "<aqua>Nauka ruchu", List.of("<gray>" + c.nazwaKolor(o) + "<gray> zna już 4 ruchy. Który zastąpić ruchem <aqua>" + dr().nazwaRuchu(nowy) + "</aqua>?"),
        pr, Okna.Przycisk.of("<gray>Wstecz", "", x -> oknoNauki(x, o)), 2, true, false);
  }

  private void oknoWypuszczenia(Player p, Osobnik o) {
    plg.okna.pokaz(p, "<red>Wypuścić?", List.of("<gray>" + c.nazwaKolor(o) + "<gray> odejdzie na wolność. <red>Tego nie cofniesz."),
        List.of(Okna.Przycisk.of("<red>Tak, wypuść", "", x -> { int n = slotOf(x, o); if (n > 0) uwolnij(x, new String[] {"uwolnij", String.valueOf(n), "potwierdz"}); })),
        Okna.Przycisk.of("<gray>Nie", "", x -> karta(x, o)), 1, true, false);
  }

  /** Gracze w pobliżu: pojedynek, walka turowa, handel — z przycisku w menu (bez wpisywania nicku). */
  private void gracze(Player p) {
    if (c.daneLubBlad(p) == null) return;
    List<Player> obok = new ArrayList<>();
    for (Player x : p.getWorld().getPlayers()) {
      if (x.equals(p) || x.getLocation().distanceSquared(p.getLocation()) > c.ust.pojedynekZasieg * c.ust.pojedynekZasieg) continue;
      if (c.dane(x) == null) continue;
      obok.add(x);
    }
    obok.sort(java.util.Comparator.comparingDouble(x -> x.getLocation().distanceSquared(p.getLocation())));
    if (obok.isEmpty()) {
      plg.okna.pokaz(p, "<gold>Gracze w pobliżu", List.of("<gray>Nikogo nie ma w pobliżu (" + (int) c.ust.pojedynekZasieg + " bloków). Podejdź do gracza, którego chcesz wyzwać."), List.of(), Okna.Przycisk.of("<gray>OK", "", x -> {}), 1, true, false);
      return;
    }
    List<Okna.Przycisk> pr = new ArrayList<>();
    for (Player x : obok.subList(0, Math.min(8, obok.size()))) {
      String n = x.getName();
      pr.add(Okna.Przycisk.of("<red>Real-time: " + n, "Stwory walczą same, widok areny, rankingowy", y -> zapros(y, new String[] {"pojedynek", n}, Zaproszenia.Rodzaj.POJEDYNEK)));
      pr.add(Okna.Przycisk.of("<gold>Turowa: " + n, "Wybierasz ruchy, tura odgrywana na arenie", y -> zapros(y, new String[] {"walka", n}, Zaproszenia.Rodzaj.WALKA)));
      pr.add(Okna.Przycisk.of("<green>Handel: " + n, "Wymiana chowańców", y -> zapros(y, new String[] {"handel", n}, Zaproszenia.Rodzaj.HANDEL)));
    }
    plg.okna.pokaz(p, "<gold>Gracze w pobliżu", List.of("<gray>Wybierz gracza i rodzaj wyzwania. Dostanie zaproszenie w oknie."), pr, Okna.Przycisk.of("<gray>Zamknij", "", x -> {}), 3, true, false);
  }

  private void zamien(Player p, String[] a) {
    ProfilDane d = c.daneLubBlad(p); if (d == null || a.length < 3) { if (d != null) c.blad(p, "/chw zamien <a> <b>"); return; }
    int x = parse(a[1]) - 1, y = parse(a[2]) - 1;
    if (x < 0 || y < 0 || x >= d.druzyna.size() || y >= d.druzyna.size()) { c.blad(p, "Złe sloty."); return; }
    java.util.Collections.swap(d.druzyna, x, y);
    d.brudne = true;
    c.pasek(p, "<green>Zamieniono sloty " + (x + 1) + " i " + (y + 1));
  }

  private void magazyn(Player p, String[] a) {
    ProfilDane d = c.daneLubBlad(p); if (d == null) return;
    String op = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "";
    switch (op) {
      case "strona" -> {
        int max = Math.max(1, (d.magazyn.size() + c.ust.stronaMagazynu - 1) / c.ust.stronaMagazynu);
        d.stronaMagazynu = Math.max(0, Math.min(max - 1, strona(a.length > 2 ? a[2] : "1", d.stronaMagazynu, max) - 1));
        c.pasek(p, "<gray>Magazyn: strona " + (d.stronaMagazynu + 1) + "/" + max);
      }
      case "wloz" -> {
        Osobnik o = a.length > 2 ? Druzyny.slot(d, a[2]) : null;
        if (o == null) { c.blad(p, "/chw magazyn wloz <slot>"); return; }
        if (d.magazyn.size() >= c.limitMagazynu(p)) { c.blad(p, "Magazyn pełny (" + c.limitMagazynu(p) + ")."); return; }
        if (plg.walki.wWalce(p) || plg.pojedynki.wPojedynku(p)) { c.blad(p, "Nie w trakcie walki."); return; }
        var ak = prz().aktywny(p);
        if (ak != null && ak.osobnikId.equals(o.id)) prz().odwolaj(p, true);
        d.druzyna.remove(o);
        d.magazyn.add(o);
        d.brudne = true;
        c.pasek(p, "<green>" + c.nazwa(o) + " → magazyn");
      }
      case "wyjmij" -> {
        int nr = a.length > 2 ? parse(a[2]) - 1 : -1;
        if (nr < 0 || nr >= d.magazyn.size()) { c.blad(p, "/chw magazyn wyjmij <nr>"); return; }
        if (d.druzyna.size() >= c.limitDruzyny(p)) { c.blad(p, "Drużyna pełna (" + c.limitDruzyny(p) + ")."); return; }
        Osobnik o = d.magazyn.remove(nr);
        d.druzyna.add(o);
        d.brudne = true;
        c.pasek(p, "<green>" + c.nazwa(o) + " → drużyna");
      }
      default -> {
        c.msg(p, "<gold>Magazyn " + d.magazyn.size() + "/" + c.limitMagazynu(p) + " <gray>(strona " + (d.stronaMagazynu + 1) + ")");
        int od = d.stronaMagazynu * c.ust.stronaMagazynu;
        for (int i = od; i < Math.min(d.magazyn.size(), od + c.ust.stronaMagazynu); i++) {
          Osobnik o = d.magazyn.get(i);
          p.sendMessage(Chowance.mm(" <yellow>" + (i + 1) + ".</yellow> " + c.nazwaKolor(o) + " <gray>Poz. " + o.poziom));
        }
      }
    }
  }

  private void nazwij(Player p, String[] a) { nazwij(p, a, false); }

  private void nazwij(Player p, String[] a, boolean zOkna) {
    if (zOkna) { String[] b = new String[a.length + 1]; b[0] = "nazwij"; System.arraycopy(a, 0, b, 1, a.length); a = b; }
    ProfilDane d = c.daneLubBlad(p); if (d == null) return;
    Osobnik o = a.length > 2 ? Druzyny.slot(d, a[1]) : null;
    if (o == null) { c.blad(p, "/chw nazwij <slot> <imię>"); return; }
    String imie = String.join(" ", java.util.Arrays.copyOfRange(a, 2, a.length)).trim();
    if (imie.length() > 16 || !imie.matches("[\\p{L}0-9 _\\-']+")) { c.blad(p, "Imię: do 16 znaków, litery/cyfry/spacja."); return; }
    o.przydomek = imie;
    d.brudne = true;
    prz().odswiezZdrowie(p, o);
    c.pasek(p, "<green>Nowe imię: " + imie);
  }

  private void ewoluuj(Player p, String[] a) {
    ProfilDane d = c.daneLubBlad(p); if (d == null) return;
    Osobnik o = a.length > 1 ? Druzyny.slot(d, a[1]) : null;
    if (o == null) { c.blad(p, "/chw ewoluuj <slot>"); return; }
    if (plg.walki.wWalce(p) || plg.pojedynki.wPojedynku(p)) { c.blad(p, "Nie w trakcie walki."); return; }
    Gatunek g = c.gatunek(o);
    // Kamień może być w ręce albo gdziekolwiek w ekwipunku (z menu gracz nie trzyma go w ręce).
    Ewolucja bez = Rozwoj.ewolucjaBezPrzedmiotu(o, g);
    org.bukkit.inventory.ItemStack reka = p.getInventory().getItemInMainHand();
    if (bez != null && bez.wymagaPrzedmiotu() && !bez.przedmiot().equals(PrzedmiotMmo.id(reka))) {
      for (org.bukkit.inventory.ItemStack it : p.getInventory().getContents()) {
        if (it != null && bez.przedmiot().equals(PrzedmiotMmo.id(it))) { reka = it; break; }
      }
    }
    String przedmiot = PrzedmiotMmo.id(reka);
    Ewolucja e = Rozwoj.dostepnaEwolucja(o, g, przedmiot);
    if (e == null) {
      if (bez != null && bez.wymagaPrzedmiotu()) c.blad(p, "Potrzebny przedmiot: " + c.nazwyMmo.nazwa(bez.przedmiot()) + " (Warsztat Poskramiacza).");
      else if (g.ewolucje().isEmpty()) c.blad(p, g.nazwa() + " nie przechodzi przemiany.");
      else { Ewolucja x = g.ewolucje().get(0); c.blad(p, "Wymagane: poziom " + x.poziom() + (x.wiez() > 0 ? ", więź " + x.wiez() : "") + (x.wymagaPrzedmiotu() ? ", " + c.nazwyMmo.nazwa(x.przedmiot()) : "") + "."); }
      return;
    }
    Gatunek nowy = c.rej.gatunek(e.doGatunku());
    if (e.wymagaPrzedmiotu()) reka.setAmount(reka.getAmount() - 1);
    String z = g.id();
    double hpProc = dr().hp(p, o) / Math.max(1, dr().hpMax(o));
    Rozwoj.ewoluuj(o, g, nowy);
    dr().ustawHp(o, hpProc * dr().hpMax(o));
    d.oznaczZlapany(nowy.id(), o.lsniacy);
    plg.bestiariusz.sprawdzNagrody(p, d);
    d.brudne = true;
    prz().przestaw(p, o);
    prz().sygnalAktywnemu(p, o, "EWOLUCJA");
    p.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 60, 0.6, 0.8, 0.6, 0.3);
    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.7f);
    c.msg(p, "<light_purple>" + g.nazwa() + " przemienia się w <gold>" + nowy.nazwa() + "</gold>!");
    c.mmocore.dajExp(p, c.ust.expEwolucja);
    Bukkit.getPluginManager().callEvent(new ChowaniecEwolucjaEvent(p, o, z));
    c.zdarzenie("EWOLUCJA", Map.of("gracz", p.getName(), "z", z, "do", nowy.id()));
    c.dane.zapisz(d);
  }

  private void naucz(Player p, String[] a) {
    ProfilDane d = c.daneLubBlad(p); if (d == null) return;
    Osobnik o = a.length > 3 ? Druzyny.slot(d, a[1]) : null;
    if (o == null) { c.blad(p, "/chw naucz <slot> <RUCH> <1-4>"); return; }
    String ruch = a[2].toUpperCase(Locale.ROOT);
    int poz = parse(a[3]) - 1;
    Gatunek g = c.gatunek(o);
    if (!Rozwoj.moznaNauczyc(o, g, ruch)) { c.blad(p, c.nazwa(o) + " nie zna i nie może teraz poznać tego ruchu."); return; }
    if (poz < 0 || poz > 3) { c.blad(p, "Pozycja 1-4."); return; }
    if (poz >= o.ruchy.size()) o.ruchy.add(ruch); else o.ruchy.set(poz, ruch);
    d.brudne = true;
    c.pasek(p, "<green>" + c.nazwa(o) + " uczy się: " + dr().nazwaRuchu(ruch));
  }

  private void uwolnij(Player p, String[] a) {
    ProfilDane d = c.daneLubBlad(p); if (d == null) return;
    Osobnik o = a.length > 1 ? Druzyny.slot(d, a[1]) : null;
    if (o == null) { c.blad(p, "/chw uwolnij <slot> potwierdz"); return; }
    if (a.length < 3 || !a[2].equalsIgnoreCase("potwierdz")) { c.msg(p, "<red>Na pewno? <gray>/chw uwolnij " + a[1] + " potwierdz — tego nie cofniesz."); return; }
    if (d.druzyna.size() + d.magazyn.size() <= 1) { c.blad(p, "Nie możesz wypuścić ostatniego chowańca."); return; }
    var ak = prz().aktywny(p);
    if (ak != null && ak.osobnikId.equals(o.id)) prz().odwolaj(p, true);
    d.druzyna.remove(o);
    d.brudne = true;
    c.msg(p, "<gray>" + c.nazwa(o) + " odchodzi na wolność.");
  }

  private void walka(Player p, String[] a) {
    if (a.length > 1) { zapros(p, a, Zaproszenia.Rodzaj.WALKA); return; }
    Entity cel = p.getTargetEntity(16);
    if (!(cel instanceof LivingEntity x) || !plg.dzikie.dziki(x)) cel = dzikiWPoluWidzenia(p, 8, 30);
    if (!(cel instanceof LivingEntity x) || !plg.dzikie.dziki(x)) cel = dzikiWPoluWidzenia(p, 16, 180);
    if (cel instanceof LivingEntity le && plg.dzikie.dziki(le)) { plg.walki.startDziki(p, le); return; }
    c.blad(p, "W pobliżu (16 bloków) nie ma dzikiego chowańca, którego widzisz.");
  }

  /**
   * Małe ciała (np. leżące nisko) łatwo minąć promieniem — więc zapasowo: najbliższy dziki w stożku
   * {@code stopnie} wokół kierunku patrzenia, do {@code zasieg} bloków.
   */
  private Entity dzikiWPoluWidzenia(Player p, double zasieg, double stopnie) {
    var oko = p.getEyeLocation();
    var kier = oko.getDirection().normalize();
    double cos = Math.cos(Math.toRadians(stopnie));
    Entity best = null;
    double bestD = Double.MAX_VALUE;
    for (Entity e : p.getNearbyEntities(zasieg, zasieg, zasieg)) {
      if (!(e instanceof LivingEntity) || !plg.dzikie.dziki(e)) continue;
      var v = e.getLocation().add(0, e.getHeight() / 2, 0).toVector().subtract(oko.toVector());
      double d = v.length();
      if (d < 0.01 || v.normalize().dot(kier) < cos || !p.hasLineOfSight(e)) continue;
      if (d < bestD) { bestD = d; best = e; }
    }
    return best;
  }

  private void zapros(Player p, String[] a, Zaproszenia.Rodzaj r) {
    if (a.length < 2) { c.blad(p, "Podaj gracza."); return; }
    Player cel = Bukkit.getPlayerExact(a[1]);
    if (cel == null || cel.equals(p)) { c.blad(p, "Nie ma takiego gracza online."); return; }
    if (c.daneLubBlad(p) == null) return;
    if (c.dane(cel) == null) { c.blad(p, cel.getName() + " nie wybrał jeszcze postaci."); return; }
    if (plg.walki.wWalce(cel) || plg.pojedynki.wPojedynku(cel)) { c.blad(p, cel.getName() + " właśnie walczy."); return; }
    if (plg.walki.wWalce(p) || plg.pojedynki.wPojedynku(p)) { c.blad(p, "Najpierw skończ swoją walkę."); return; }
    plg.zaproszenia.wyslij(p, cel, r, r == Zaproszenia.Rodzaj.HANDEL ? c.ust.handelZaproszenieS : c.ust.zaproszenieS);
  }

  private void akceptuj(Player p) {
    var z = plg.zaproszenia.odbierz(p);
    if (z == null) { c.blad(p, "Nie masz ważnego zaproszenia."); return; }
    Player od = Bukkit.getPlayer(z.od());
    if (od == null) { c.blad(p, "Zapraszający wyszedł."); return; }
    switch (z.rodzaj()) {
      case WALKA -> plg.walki.startPvp(od, p, WalkiTurowe.Rodzaj.PVP);
      case POJEDYNEK -> plg.pojedynki.start(od, p);
      case HANDEL -> plg.handel.start(od, p);
    }
  }

  private void hodowla(Player p, String[] a) {
    ProfilDane d = c.daneLubBlad(p); if (d == null) return;
    Hodowla h = plg.hodowla;
    String op = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "";
    switch (op) {
      case "wloz" -> {
        Osobnik o = a.length > 2 ? Druzyny.slot(d, a[2]) : null;
        if (o == null) { c.blad(p, "/chw hodowla wloz <slot>"); return; }
        if (plg.walki.wWalce(p) || plg.pojedynki.wPojedynku(p)) { c.blad(p, "Nie w trakcie walki."); return; }
        String b = h.wloz(p, d, o);
        if (b != null) c.blad(p, b); else c.pasek(p, "<green>" + c.nazwa(o) + " → zagroda");
      }
      case "wybierz" -> {
        List<Okna.Przycisk> pr = new ArrayList<>();
        for (Osobnik o : d.druzyna) {
          Gatunek g = c.gatunek(o);
          if (g == null || !g.plodny()) continue;
          pr.add(Okna.Przycisk.of("<white>" + c.nazwa(o) + " <gray>poz. " + o.poziom, g.nazwa(), x -> { int n = slotOf(x, o); if (n > 0) hodowla(x, new String[] {"hodowla", "wloz", String.valueOf(n)}); }));
        }
        if (pr.isEmpty()) { plg.okna.pokaz(p, "<light_purple>Zagroda", List.of("<gray>W drużynie nie ma stwora, który może mieć młode."), List.of(), Okna.Przycisk.of("<gray>OK", "", x -> {}), 1, true, false); return; }
        plg.okna.pokaz(p, "<light_purple>Zagroda", List.of("<gray>Kogo wpuścić do zagrody? Para z tej samej grupy jaj składa jajo."), pr, Okna.Przycisk.of("<gray>Wstecz", "", x -> {}), 2, true, false);
      }
      case "wyjmij" -> {
        String b = h.wyjmij(p, d, a.length > 2 ? parse(a[2]) : 1);
        if (b != null) c.blad(p, b); else c.pasek(p, "<green>Wyjęto z zagrody.");
      }
      default -> {
        h.sprawdzJaja(p, d);
        String nz = h.niezgodnosc(d);
        c.msg(p, "<light_purple>Zagroda: <white>" + (d.hodowlaA == null ? "—" : c.nazwa(d.hodowlaA)) + " <gray>+ <white>" + (d.hodowlaB == null ? "—" : c.nazwa(d.hodowlaB))
            + (nz == null ? " <gray>· następne jajo: " + h.postep(d) + "%" : " <red>· " + nz));
        for (int i = 0; i < d.jaja.size(); i++) {
          Jajo j = d.jaja.get(i);
          p.sendMessage(Chowance.mm(" <yellow>Jajo " + (i + 1) + ":</yellow> <gray>" + (int) j.przebyte + "/" + j.wymagane + " bloków"));
        }
      }
    }
  }

  private void bestiariusz(Player p, String[] a) {
    ProfilDane d = c.daneLubBlad(p); if (d == null) return;
    Bestiariusz b = plg.bestiariusz;
    if (a.length > 2 && a[1].equalsIgnoreCase("gatunek")) {
      Gatunek g = c.rej.gatunek(a[2].toUpperCase(Locale.ROOT));
      if (g == null || g.testowy()) { c.blad(p, "Nieznany gatunek."); return; }
      int st = d.stanBestiariusza(g.id());
      List<String> t = new ArrayList<>();
      // Karta jak w Pokédeksie (recenzja R-06/R-28): numer, typy, a po złapaniu staty bazowe, ruchy i ewolucje.
      String nr = "<dark_gray>#" + String.format("%02d", c.rej.bestiariusz().indexOf(g) + 1) + " ";
      if (st == 0) {
        t.add(nr + "<gray>??? — jeszcze go nie spotkałeś.");
        if (!g.wystepowanie().isEmpty()) t.add("<aqua>Wskazówka: <gray>" + g.wystepowanie().get(0));
      } else {
        t.add(nr + "<white><b>" + g.nazwa() + "</b> <gray>" + g.rzadkosc().nazwa() + " · " + g.typy().stream().map(x -> x.kolor() + x.nazwa()).collect(Collectors.joining("<gray>/")));
        for (String l : g.opis()) t.add("<gray>" + l);
        if (!g.wystepowanie().isEmpty()) t.add("<aqua>Występowanie: <gray>" + String.join("; ", g.wystepowanie()));
        if (st >= 2) {
          int suma = 0;
          StringBuilder sb = new StringBuilder("<aqua>Staty bazowe: <gray>");
          String[] skr = {"HP", "ATK", "OBR", "ATK.S", "OBR.S", "SZYB"};
          int i = 0;
          for (var x : pl.mcmmorpg.chowance.model.Stat.values()) {
            suma += g.bazowy(x);
            if (i < skr.length) sb.append(skr[i]).append(" <white>").append(g.bazowy(x)).append("</white>  ");
            i++;
          }
          t.add(sb.append("<gray>(suma <white>").append(suma).append("</white>)").toString());
          StringBuilder rs = new StringBuilder("<aqua>Ruchy: <gray>");
          for (var e : g.ruchy().entrySet()) for (String r : e.getValue()) { var ru = c.rej.ruch(r); rs.append("poz. ").append(e.getKey()).append(" ").append(ru == null ? r : ru.typ().kolor() + ru.nazwa() + "<gray>").append(", "); }
          t.add(rs.substring(0, Math.max(0, rs.length() - 2)));
          for (var ew : g.ewolucje()) {
            var dg = c.rej.gatunek(ew.doGatunku());
            t.add("<light_purple>Przemiana: <white>" + (dg == null ? ew.doGatunku() : (d.stanBestiariusza(dg.id()) >= 1 ? dg.nazwa() : "???"))
                + " <gray>— poz. " + ew.poziom() + (ew.przedmiot() == null || ew.przedmiot().isBlank() ? "" : " + " + c.nazwyMmo.nazwa(ew.przedmiot()))
                + (ew.wiez() > 0 ? ", więź " + ew.wiez() : ""));
          }
        }
        t.add(st >= 2 ? "<green>Złapany" + (d.lsniace.contains(g.id()) ? " <gold>(także lśniący)" : "") : "<yellow>Widziany — złap go, by poznać szczegóły");
      }
      plg.okna.pokaz(p, "<aqua>Bestiariusz", t, List.of(), Okna.Przycisk.of("<gray>Zamknij", "", x -> {}), 1, true, false);
      return;
    }
    if (a.length > 2 && a[1].equalsIgnoreCase("strona")) {
      int max = Math.max(1, (b.wszystkie() + c.ust.stronaBestiariusza - 1) / c.ust.stronaBestiariusza);
      d.stronaBestiariusza = Math.max(0, Math.min(max - 1, strona(a[2], d.stronaBestiariusza, max) - 1));
    }
    b.sprawdzNagrody(p, d);
    c.msg(p, "<aqua>Bestiariusz: <white>" + b.zlapane(d) + "/" + b.wszystkie() + " złapanych (" + b.procent(d) + "%), " + b.widziane(d) + " widzianych");
  }

  private void ranking(Player p) {
    Ranking r = plg.ranking;
    ProfilDane d = c.dane(p);
    c.msg(p, "<gold>Ranking — sezon " + r.sezon() + (d != null ? " <gray>· twoje ELO " + r.elo(d) + ", miejsce " + (r.pozycja(d.profil) == 0 ? "-" : r.pozycja(d.profil)) : ""));
    var t = r.top();
    for (int i = 0; i < Math.min(c.ust.rankingTop, t.size()); i++) {
      var w = t.get(i).getValue();
      p.sendMessage(Chowance.mm(" <yellow>" + (i + 1) + ".</yellow> " + w.nick + " <gray>" + w.elo + " (" + w.w + "/" + w.l + ")"));
    }
  }

  private void turniej(Player p, String[] a) {
    Turnieje t = plg.turnieje;
    String op = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "";
    if (op.equals("zapisz")) { String b = t.zapisz(p); if (b != null) c.blad(p, b); else c.msg(p, "<green>Zapisano do turnieju (" + t.zapisanych() + " chętnych)."); }
    else if (op.equals("wypisz")) c.pasek(p, t.wypisz(p) ? "<gray>Wypisano." : "<red>Nie można się wypisać.");
    else c.msg(p, "<gold>Turniej: " + t.stan() + " · zapisanych " + t.zapisanych() + " <gray>(/chw turniej zapisz|wypisz)");
  }

  // ================================================================== admin

  private boolean admin(CommandSender s, String[] a) {
    if (!s.hasPermission("chowance.admin")) { s.sendMessage("Brak uprawnień."); return true; }
    String sub = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "";
    switch (sub) {
      case "przeladuj" -> { var w = plg.przeladuj(); s.sendMessage("Chowance: przeładowano — gatunków " + c.rej.gatunki().size() + ", ruchów " + c.rej.ruchy().size() + ", błędów " + w.bledy() + ", uwag " + w.uwagi()); w.wpisy().stream().limit(40).forEach(x -> s.sendMessage(" " + x)); }
      case "zapisz" -> { for (ProfilDane d : c.dane.online()) c.dane.zapisz(d); s.sendMessage("Zapisano " + c.dane.online().size() + " profili."); }
      case "diag" -> s.sendMessage("Chowance diag: profili online " + c.dane.online().size() + ", przywołanych " + prz().wszystkie().size() + ", dzikich w cache " + plg.dzikie.rozmiarCache()
          + ", walk " + plg.walki.aktywnych() + ", pojedynków " + plg.pojedynki.aktywnych() + ", gatunków " + c.rej.gatunki().size() + ", mechanik MM " + plg.mythicZarejestrowane() + ", profesja " + (c.mmocore.dostepna() ? "OK" : "BRAK"));
      case "daj" -> {
        Player p = gracz(s, a, 2); if (p == null) return true;
        ProfilDane d = c.dane(p); Gatunek g = a.length > 3 ? c.rej.gatunek(a[3].toUpperCase(Locale.ROOT)) : null;
        if (d == null || g == null) { s.sendMessage("Brak danych gracza albo gatunku."); return true; }
        int poz = a.length > 4 ? Math.max(1, Math.min(100, parse(a[4]))) : 5;
        boolean l = a.length > 5 && a[5].equalsIgnoreCase("lsniacy");
        Osobnik o = Generator.nowy(c.rej, g, poz, l, ThreadLocalRandom.current());
        o.pochodzenie = "ADMIN";
        var gdzie = dr().dodaj(p, d, o, true);
        c.dane.zapisz(d);
        Bukkit.getPluginManager().callEvent(new ChowaniecZlapanyEvent(p, o, "ADMIN"));
        s.sendMessage("Dano " + g.id() + " poz. " + poz + " -> " + gdzie + " (id " + o.id + ")");
      }
      case "info" -> {
        Player p = gracz(s, a, 2); if (p == null) return true;
        ProfilDane d = c.dane(p);
        if (d == null) { s.sendMessage("Brak wczytanych danych (profil nie wybrany albo duch)."); return true; }
        s.sendMessage(p.getName() + " profil=" + d.profil + " druzyna=" + d.druzyna.size() + " magazyn=" + d.magazyn.size() + " odznaki=" + d.odznaki + " elo=" + plg.ranking.elo(d) + " starter=" + d.starter
            + " jaja=" + d.jaja.size() + " aktywny=" + (prz().aktywny(p) == null ? "-" : prz().aktywny(p).osobnikId)
            + " cialo=" + (prz().aktywny(p) == null ? "-" : prz().aktywny(p).cialo.getUniqueId())
            + " pojazd=" + (p.getVehicle() == null ? "-" : p.getVehicle().getType().name()) + " kamera=" + (plg.kamera != null && plg.kamera.aktywna(p) ? "tak" : "nie"));
        for (int i = 0; i < d.druzyna.size(); i++) { Osobnik o = d.druzyna.get(i); s.sendMessage(" " + (i + 1) + ". " + o.gatunek + " poz." + o.poziom + " hp=" + (int) dr().hp(p, o) + "/" + dr().hpMax(o) + " id=" + o.id); }
      }
      case "lecz" -> { Player p = gracz(s, a, 2); if (p == null || c.dane(p) == null) return true; dr().leczWszystkich(p, c.dane(p), 100); s.sendMessage("Uleczono drużynę " + p.getName()); }
      case "odznaka" -> {
        Player p = gracz(s, a, 2); if (p == null || c.dane(p) == null || a.length < 4) return true;
        ProfilDane d = c.dane(p);
        String id = a[3].toUpperCase(Locale.ROOT);
        if (a.length > 4 && a[4].equalsIgnoreCase("zabierz")) d.odznaki.remove(id); else if (d.odznaki.add(id)) c.zdarzenie("ODZNAKA", Map.of("gracz", p.getName(), "odznaka", id));
        d.brudne = true;
        s.sendMessage("Odznaki " + p.getName() + ": " + d.odznaki);
      }
      case "walka-npc" -> { Player p = gracz(s, a, 2); if (p == null || a.length < 4) return true; s.sendMessage(plg.walki.startTrener(p, a[3].toUpperCase(Locale.ROOT), null, 0) ? "Walka rozpoczęta." : "Nie rozpoczęto (patrz komunikat gracza)."); }
      case "pojedynek-stan" -> { Player p = gracz(s, a, 2); if (p == null) return true; s.sendMessage(plg.pojedynki.stan(p)); }
      case "pojedynek-npc" -> {
        // eval: /chw admin pojedynek-npc <gracz> [TRENER] — real-time z pierwszym stworem trenera (jak z okna NPC)
        Player p = gracz(s, a, 2); if (p == null) return true;
        var t = c.rej.trenerzy().get(a.length > 3 ? a[3].toUpperCase(Locale.ROOT) : "TRENER_PROBNY");
        if (t == null) { s.sendMessage("Nieznany trener."); return true; }
        var l = plg.walki.druzynaTrenera(t);
        s.sendMessage(!l.isEmpty() && plg.pojedynki.startNpc(p, t.nazwa(), l) ? "Pojedynek NPC rozpoczęty." : "Nie rozpoczęto.");
      }
      case "wieza" -> {
        Player p = gracz(s, a, 2); if (p == null || a.length < 4) return true;
        var w = c.rej.wieze().get(a[3].toUpperCase(Locale.ROOT));
        if (w == null) { s.sendMessage("Nieznana wieża."); return true; }
        s.sendMessage(plg.walki.startTrener(p, w.fale().get(0), w, 1) ? "Wieża: fala 1." : "Nie rozpoczęto.");
      }
      case "turniej" -> {
        String op = a.length > 2 ? a[2].toLowerCase(Locale.ROOT) : "stan";
        if (op.equals("start")) { String b = plg.turnieje.start(a.length > 3 ? a[3] : "Turniej sezonu " + plg.ranking.sezon()); s.sendMessage(b == null ? "Turniej wystartował." : b); }
        else if (op.equals("stop")) { plg.turnieje.stop(); s.sendMessage("Turniej zatrzymany."); }
        else s.sendMessage("Turniej: " + plg.turnieje.stan() + ", zapisanych " + plg.turnieje.zapisanych());
      }
      case "npc" -> {
        // /chw admin npc tutaj [TRENER]   |   /chw admin npc usun <promien>   (jako gracz)
        // /chw admin npc <swiat> <x> <y> <z> <yaw> [TRENER]   (konsola)
        String op = a.length > 2 ? a[2].toLowerCase(Locale.ROOT) : "";
        if (op.equals("usun") && s instanceof Player p) { s.sendMessage("Usunięto NPC: " + plg.trenerNpc.usun(p.getLocation(), a.length > 3 ? Double.parseDouble(a[3]) : 5)); return true; }
        org.bukkit.Location l = null;
        String trener = "TRENER_PROBNY";
        if (op.equals("tutaj") && s instanceof Player p) { l = p.getLocation(); l.setPitch(0); if (a.length > 3) trener = a[3].toUpperCase(Locale.ROOT); }
        else if (a.length >= 7) {
          var w = Bukkit.getWorld(a[2]);
          if (w != null) l = new org.bukkit.Location(w, Double.parseDouble(a[3]), Double.parseDouble(a[4]), Double.parseDouble(a[5]), Float.parseFloat(a[6]), 0);
          if (a.length > 7) trener = a[7].toUpperCase(Locale.ROOT);
        }
        if (l == null) { s.sendMessage("/chw admin npc tutaj [TRENER] | usun <promien> | <swiat> <x> <y> <z> <yaw> [TRENER]"); return true; }
        var m = plg.trenerNpc.postaw(l, trener);
        s.sendMessage(m == null ? "Nieznany trener " + trener + " (trenerzy/*.yml)." : "Postawiono NPC " + trener + " (" + m.getUniqueId() + ")");
      }
      case "ranking" -> {
        if (a.length > 3 && a[2].equalsIgnoreCase("usun")) s.sendMessage("Usunięto wpisów: " + plg.ranking.usun(a[3]));
        else s.sendMessage("/chw admin ranking usun <nick>");
      }
      case "sezon" -> { if (a.length > 2 && a[2].equalsIgnoreCase("nowy")) { plg.ranking.nowySezon(); s.sendMessage("Nowy sezon: " + plg.ranking.sezon()); } }
      case "exp" -> {
        Player p = gracz(s, a, 2); if (p == null || a.length < 5) return true;
        ProfilDane d = c.dane(p); Osobnik o = d == null ? null : Druzyny.slot(d, a[3]);
        if (o == null) { s.sendMessage("Zły slot."); return true; }
        var aw = dr().dajExp(p, d, o, Long.parseLong(a[4]));
        s.sendMessage("Exp dodany: poziom " + (aw == null ? "?" : aw.zPoziomu() + " -> " + aw.naPoziom()));
      }
      case "jajo" -> {
        Player p = gracz(s, a, 2); if (p == null || a.length < 4 || c.dane(p) == null) return true;
        Gatunek g = c.rej.gatunek(a[3].toUpperCase(Locale.ROOT));
        if (g == null) { s.sendMessage("Nieznany gatunek."); return true; }
        Jajo j = new Jajo();
        j.mlode = Generator.nowy(c.rej, g, 1, Generator.losujLsniacy(c.ust.szansaLsniacego, ThreadLocalRandom.current()), ThreadLocalRandom.current());
        j.mlode.pochodzenie = "JAJO";
        j.wymagane = g.dystansJaja();
        j.zlozone = System.currentTimeMillis();
        c.dane(p).jaja.add(j);
        c.dane(p).brudne = true;
        s.sendMessage("Jajo " + g.id() + " dla " + p.getName() + " (" + j.wymagane + " bloków).");
      }
      case "spawn" -> {
        if (!(s instanceof Player p) || a.length < 3) { s.sendMessage("/chw admin spawn <GATUNEK> [poziom] [lsniacy] — jako gracz"); return true; }
        Gatunek g = c.rej.gatunek(a[2].toUpperCase(Locale.ROOT));
        if (g == null) { s.sendMessage("Nieznany gatunek."); return true; }
        var e = plg.dzikie.postaw(g, p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(3)), a.length > 3 ? parse(a[3]) : 5, a.length > 4 && a[4].equalsIgnoreCase("lsniacy"));
        s.sendMessage(e == null ? "Nie udało się." : "Postawiono dzikiego " + g.id() + " (" + e.getUniqueId() + ")");
      }
      case "spawnw" -> {
        // konsola/eval: /chw admin spawnw <gracz> <GATUNEK> [poziom] — przed graczem
        Player p = gracz(s, a, 2); if (p == null || a.length < 4) return true;
        Gatunek g = c.rej.gatunek(a[3].toUpperCase(Locale.ROOT));
        if (g == null) { s.sendMessage("Nieznany gatunek."); return true; }
        var e = plg.dzikie.postaw(g, p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(3)), a.length > 4 ? parse(a[4]) : 5, a.length > 5 && a[5].equalsIgnoreCase("lsniacy"));
        s.sendMessage(e == null ? "Nie udało się." : "Postawiono dzikiego " + g.id() + " (" + e.getUniqueId() + ")");
      }
      case "sprzatnij" -> {
        // /chw admin sprzatnij <swiat> <x> <y> <z> <promien>  albo jako gracz: /chw admin sprzatnij <promien>
        org.bukkit.Location l; double r;
        if (a.length >= 7) {
          var w = Bukkit.getWorld(a[2]);
          if (w == null) { s.sendMessage("Nieznany świat."); return true; }
          l = new org.bukkit.Location(w, Double.parseDouble(a[3]), Double.parseDouble(a[4]), Double.parseDouble(a[5]));
          r = Double.parseDouble(a[6]);
        } else if (s instanceof Player p && a.length >= 3) { l = p.getLocation(); r = Double.parseDouble(a[2]); }
        else { s.sendMessage("/chw admin sprzatnij <swiat> <x> <y> <z> <promien> | (gracz) /chw admin sprzatnij <promien>"); return true; }
        s.sendMessage("Usunięto dzikich ciał: " + plg.dzikie.sprzatnij(l, Math.min(256, r)));
      }
      case "status" -> {
        // test/eval: /chw admin status <uuid-ciala> <STATUS> <sek>
        if (a.length < 5) return true;
        Entity e = Bukkit.getEntity(java.util.UUID.fromString(a[2]));
        var st = pl.mcmmorpg.chowance.model.Status.parse(a[3]);
        if (e == null || st == null || !plg.dzikie.dziki(e)) { s.sendMessage("Brak ciała/statusu."); return true; }
        plg.dzikie.nalozStatus(e, st, parse(a[4]));
        s.sendMessage("Status " + st + " na " + a[2]);
      }
      default -> s.sendMessage("/chw admin daj|przeladuj|zapisz|info|lecz|odznaka|walka-npc|wieza|turniej|sezon|ranking|exp|jajo|spawn|spawnw|status|sprzatnij|diag");
    }
    return true;
  }

  private Player gracz(CommandSender s, String[] a, int i) {
    Player p = a.length > i ? Bukkit.getPlayerExact(a[i]) : null;
    if (p == null) s.sendMessage("Gracz offline albo nie podano.");
    return p;
  }

  /** Numer strony (1-based) albo słowa nastepna|poprzednia (PROBLEMY B-007: DeluxeMenus nie liczy strona+1). */
  static int strona(String arg, int biezaca0, int max) {
    String x = arg.toLowerCase(Locale.ROOT);
    if (x.startsWith("nast")) return Math.min(max, biezaca0 + 2);
    if (x.startsWith("poprz")) return Math.max(1, biezaca0);
    int n = parse(arg);
    return n < 1 ? 1 : n;
  }

  private static int parse(String s) {
    try { return Integer.parseInt(s.trim()); } catch (RuntimeException e) { return -1; }
  }

  // ================================================================== tab

  private static final List<String> GRACZ = List.of("menu", "przywolaj", "odwolaj", "druzyna", "info", "zamien", "magazyn", "nazwij", "ewoluuj",
      "naucz", "uwolnij", "starter", "karta", "gracze", "walka", "pojedynek", "handel", "akceptuj", "odrzuc", "poddaj", "ruch", "okno", "hodowla", "bestiariusz", "ranking", "turniej");
  private static final List<String> ADMIN = List.of("daj", "przeladuj", "zapisz", "info", "pojedynek-stan", "lecz", "odznaka", "walka-npc", "wieza", "turniej", "sezon", "ranking", "npc", "exp", "jajo", "spawn", "spawnw", "status", "sprzatnij", "diag");

  @Override
  public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
    List<String> opcje = new ArrayList<>();
    if (a.length == 1) { opcje.addAll(GRACZ); if (s.hasPermission("chowance.admin")) opcje.add("admin"); }
    else if (a.length == 2 && a[0].equalsIgnoreCase("admin") && s.hasPermission("chowance.admin")) opcje.addAll(ADMIN);
    else if (a.length == 2 && List.of("walka", "pojedynek", "handel").contains(a[0].toLowerCase(Locale.ROOT))) Bukkit.getOnlinePlayers().forEach(x -> opcje.add(x.getName()));
    else if (a.length == 2 && List.of("przywolaj", "info", "nazwij", "ewoluuj", "naucz", "uwolnij", "zamien").contains(a[0].toLowerCase(Locale.ROOT))) opcje.addAll(List.of("1", "2", "3", "4", "5", "6"));
    else if (a.length == 2 && a[0].equalsIgnoreCase("magazyn")) opcje.addAll(List.of("strona", "wloz", "wyjmij"));
    else if (a.length == 2 && a[0].equalsIgnoreCase("hodowla")) opcje.addAll(List.of("wloz", "wyjmij", "wybierz"));
    else if (a.length == 2 && a[0].equalsIgnoreCase("turniej")) opcje.addAll(List.of("zapisz", "wypisz"));
    else if (a.length == 2 && a[0].equalsIgnoreCase("ruch")) opcje.addAll(List.of("1", "2", "3", "4"));
    else if (a.length == 4 && a[0].equalsIgnoreCase("admin") && List.of("daj", "jajo").contains(a[1].toLowerCase(Locale.ROOT))) opcje.addAll(c.rej.gatunki().keySet());
    else if (a.length == 3 && a[0].equalsIgnoreCase("admin") && a[1].equalsIgnoreCase("spawn")) opcje.addAll(c.rej.gatunki().keySet());
    else if (a.length == 3 && a[0].equalsIgnoreCase("admin")) Bukkit.getOnlinePlayers().forEach(x -> opcje.add(x.getName()));
    String pref = a[a.length - 1].toLowerCase(Locale.ROOT);
    return opcje.stream().filter(x -> x.toLowerCase(Locale.ROOT).startsWith(pref)).sorted().toList();
  }
}
