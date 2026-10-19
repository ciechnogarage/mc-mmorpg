package pl.mcmmorpg.wyscigi;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Jeden przejazd: czasówka (1 gracz, bez zderzeń) albo wyścig (do 8 graczy, z poczekalnią). */
final class Wyscig {
  enum Tryb { CZASOWKA, WYSCIG }
  enum Faza { POCZEKALNIA, ODLICZANIE, JAZDA, KONIEC }

  private static final MiniMessage MM = MiniMessage.miniMessage();
  private static final int ODLICZANIE_TICKI = 80;

  final WyscigiPlugin plugin;
  final Ustawienia u;
  final Tor tor;
  final World swiat;
  final Tryb tryb;
  final List<Kierowca> kierowcy = new ArrayList<>();
  final List<String> wyniki = new ArrayList<>();
  final List<String> uczestnicy = new ArrayList<>();
  Faza faza = Faza.POCZEKALNIA;
  int fazaOd, startTick = -1, koniecDo = -1, ukonczyli;
  int poczekalniaDo;

  Wyscig(WyscigiPlugin plugin, Tryb tryb, World swiat) {
    this.plugin = plugin;
    this.u = plugin.u;
    this.tor = u.tor;
    this.swiat = swiat;
    this.tryb = tryb;
    this.fazaOd = Bukkit.getCurrentTick();
    this.poczekalniaDo = fazaOd + (tryb == Tryb.CZASOWKA ? 30 : u.poczekalnia * 20);
  }

  boolean przyjmuje() { return tryb == Tryb.WYSCIG && faza == Faza.POCZEKALNIA && kierowcy.size() < u.maxGraczy; }

  int wolneMiejsce() {
    for (int nr = 0; nr < u.maxGraczy; nr++) {
      final int n = nr;
      if (kierowcy.stream().noneMatch(k -> k.nrStartu == n)) return nr;
    }
    return -1;
  }

  void dolaczyl(Kierowca k) {
    kierowcy.add(k);
    uczestnicy.add(k.nick);
    if (tryb == Tryb.WYSCIG && kierowcy.size() > 1) {
      int teraz = Bukkit.getCurrentTick();
      poczekalniaDo = Math.min(fazaOd + u.poczekalniaMax * 20, Math.max(poczekalniaDo, teraz + u.poczekalniaDokladka * 20));
      for (Kierowca inny : List.copyOf(kierowcy)) {
        Player p = inny.gracz();
        if (p != null && inny != k) p.sendMessage(MM.deserialize("<gold>[Wyścigi]</gold> <yellow>" + k.nick + "</yellow> <gray>dołącza na</gray> " + k.gatunek.nazwa() + "<gray>.</gray>"));
      }
    }
  }

  // ------------------------------------------------------------------ tick
  void tick() {
    int teraz = Bukkit.getCurrentTick();
    kierowcy.removeIf(k -> {
      Player p = k.gracz();
      if (p != null && p.isOnline()) return false;
      plugin.sprzatnij(k, false, "wyjście z gry");
      return true;
    });
    if (kierowcy.isEmpty() && faza != Faza.KONIEC) { faza = Faza.KONIEC; koniecDo = teraz; }
    switch (faza) {
      case POCZEKALNIA -> {
        boolean gotowi = kierowcy.stream().allMatch(k -> k.model != null);
        if (teraz >= poczekalniaDo && gotowi) { faza = Faza.ODLICZANIE; fazaOd = teraz; }
        for (Kierowca k : List.copyOf(kierowcy)) {
          k.zamrozonyDo = teraz + 5;
          Player p = k.gracz();
          if (p == null) continue;
          int zostalo = Math.max(0, (poczekalniaDo - teraz + 19) / 20);
          String opis = tryb == Tryb.CZASOWKA ? "<yellow>Czasówka</yellow> <gray>· przygotuj się…</gray>"
              : "<yellow>Czekamy na graczy</yellow> <gray>" + kierowcy.size() + "/" + u.maxGraczy + " · start za</gray> <white>" + zostalo + " s</white>";
          plugin.pasekBoss(p, opis, 1f - Math.min(1f, (float) (poczekalniaDo - teraz) / Math.max(1, poczekalniaDo - fazaOd)));
          sprawdzWyjscie(k, p);
          if (teraz % 20 == 0) pasek(p, 10, "§7Przytrzymaj §fShift§7, aby wyjść · §fW§7 = gaz, §fmysz§7 = skręt");
        }
      }
      case ODLICZANIE -> odliczanie(teraz);
      case JAZDA -> jazda(teraz);
      case KONIEC -> {
        if (teraz >= koniecDo) {
          for (Kierowca k : new ArrayList<>(kierowcy)) plugin.zakoncz(k, true, null);
          kierowcy.clear();
          plugin.usunWyscig(this);
        }
      }
    }
  }

  private void odliczanie(int teraz) {
    int t = teraz - fazaOd;
    for (Kierowca k : List.copyOf(kierowcy)) {
      k.zamrozonyDo = teraz + 5;
      Player p = k.gracz();
      if (p == null) continue;
      plugin.pasekBoss(p, "<gold>" + tor.nazwa + "</gold> <gray>· " + tor.okrazenia + " okrążenia · " + kierowcy.size() + (kierowcy.size() == 1 ? " jeździec" : " jeźdźców") + "</gray>", 0f);
      if (t == 0) tytul(p, "<gold>Gotowi?</gold>", "<gray>Wciśnij <white>W</white> tuż przed startem = turbo</gray>", Sound.BLOCK_NOTE_BLOCK_HAT, 1f);
      else if (t == 20) tytul(p, "<red>3</red>", "", Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f);
      else if (t == 40) tytul(p, "<gold>2</gold>", "", Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f);
      else if (t == 60) tytul(p, "<yellow>1</yellow>", "", Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f);
    }
    if (t < ODLICZANIE_TICKI) return;
    for (Kierowca k : kierowcy) plugin.debug(k.nick + " START " + tryb + " " + kierowcy.size() + " gaz=" + (k.gazOd < 0 ? -1 : teraz - k.gazOd));
    faza = Faza.JAZDA;
    startTick = teraz;
    for (Kierowca k : List.copyOf(kierowcy)) {
      k.zamrozonyDo = 0;
      k.startTick = teraz;
      k.okrazenieOdTick = teraz;
      Player p = k.gracz();
      if (p != null) tytul(p, "<green>START!</green>", "", Sound.BLOCK_NOTE_BLOCK_PLING, 1.6f);
      if (k.gazOd >= 0) {
        int trzyma = teraz - k.gazOd;
        if (trzyma <= u.startOkno) { k.daj(u.startCzas, u.startMnoznik, "start"); pasek(p, 30, "§b§lTURBO STARTOWE!"); }
        else if (trzyma > u.startZaWczesnie) {
          k.karaDo = teraz + u.startKara;
          if (k.mob != null) k.mob.getWorld().spawnParticle(Particle.LARGE_SMOKE, k.mob.getLocation().add(0, 0.5, 0), 12, 0.3, 0.2, 0.3, 0.01);
          pasek(p, 30, "§cZa wcześnie! Wierzchowiec się spłoszył.");
          plugin.debug(k.nick + " start-za-wczesnie trzyma=" + trzyma);
        }
      }
    }
  }

  private void jazda(int teraz) {
    double meta = tor.okrazenia * tor.dlugosc;
    for (Kierowca k : List.copyOf(kierowcy)) {
      Player p = k.gracz();
      if (p == null || k.mob == null || !k.mob.isValid()) continue;
      if (k.naMecie()) continue;
      sprawdzWyjscie(k, p);
      Location l = k.mob.getLocation();
      Tor.Rzut r = tor.rzut(l.getX(), l.getZ(), k.indeks, 40);
      double sNorm = mod(r.s(), tor.dlugosc), stary = mod(k.postep, tor.dlugosc);
      double delta = sNorm - stary;
      if (delta > tor.dlugosc / 2) delta -= tor.dlugosc;
      if (delta < -tor.dlugosc / 2) delta += tor.dlugosc;
      double poprzedni = k.postep;
      if (Math.abs(delta) < 15) { k.postep += delta; k.indeks = r.indeks(); }
      k.pozaTorem = r.odleglosc() > tor.polszerokosc + 0.3;
      // Pola przyspieszenia
      for (int i = 0; i < tor.pola.size(); i++) {
        Tor.Pole pole = tor.pola.get(i);
        double ds = Math.abs(mod(sNorm - pole.s() + tor.dlugosc / 2, tor.dlugosc) - tor.dlugosc / 2);
        if (ds < 1.6 && Math.abs(r.bok() - pole.bok()) < 1.9 && teraz - k.poleOstatnio[i] > 20) {
          k.poleOstatnio[i] = teraz;
          k.daj(u.poleCzas, u.poleMnoznik, "pole");
        }
      }
      // Okrążenia
      int przed = (int) Math.floor(poprzedni / tor.dlugosc), po = (int) Math.floor(k.postep / tor.dlugosc);
      if (po > przed && po >= 1 && k.postep > k.maxPostep) {
        double ulamek = (po * tor.dlugosc - poprzedni) / Math.max(1e-6, k.postep - poprzedni);
        long czasOkr = Math.round(((teraz - 1 - k.okrazenieOdTick) + ulamek - k.okrazenieOdUlamek) * 50);
        k.najlepszeOkrazenieMs = Math.min(k.najlepszeOkrazenieMs, czasOkr);
        k.okrazenieOdTick = teraz - 1; k.okrazenieOdUlamek = ulamek;
        plugin.debug(k.nick + " okrazenie " + po + " " + czas(czasOkr));
        if (k.postep >= meta) { meta(k, p, teraz, ulamek); continue; }
        if (po > k.okrazenieZgloszone) {
          k.okrazenieZgloszone = po;
          String naglowek = po == tor.okrazenia - 1 ? "<gold>Ostatnie okrążenie!</gold>" : "<yellow>Okrążenie " + (po + 1) + "/" + tor.okrazenia + "</yellow>";
          tytul(p, "", naglowek + " <gray>(" + czas(czasOkr) + ")</gray>", po == tor.okrazenia - 1 ? Sound.ENTITY_PLAYER_LEVELUP : Sound.BLOCK_NOTE_BLOCK_BELL, 1.2f);
        }
      }
      k.maxPostep = Math.max(k.maxPostep, k.postep);
      // Zła strona / wypadnięcie z toru
      boolean wypadl = l.getY() < tor.y - 3 || r.odleglosc() > tor.polszerokosc + tor.pobocze + 2.0;
      if (wypadl) { plugin.powrotNaTor(k, "§eWracasz na tor…"); continue; }
      if (k.postep < k.maxPostep - 12 && teraz % 10 == 0) tytul(p, "", "<red>⟲ Zła strona!</red>", Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f);
      hud(k, p, teraz);
    }
    pozycje();
    boolean wszyscy = kierowcy.stream().allMatch(Kierowca::naMecie);
    boolean poCzasie = koniecDo >= 0 && teraz >= koniecDo;
    boolean limit = teraz - startTick > u.limit * 20;
    if (wszyscy || poCzasie || limit) zakonczWyscig(teraz);
  }

  private void sprawdzWyjscie(Kierowca k, Player p) {
    if (k.shiftPostoj <= 0) return;
    if (k.shiftPostoj >= u.wyjscieShift) {
      k.shiftPostoj = 0;
      plugin.wyjdz(p, "Opuszczasz wyścig.");
      return;
    }
    if (k.shiftPostoj % 5 == 0) {
      int pelne = k.shiftPostoj * 10 / u.wyjscieShift;
      pasek(p, 25, "§7Wyjście: §c" + "■".repeat(pelne) + "§8" + "■".repeat(10 - pelne));
    }
  }

  private void meta(Kierowca k, Player p, int teraz, double ulamek) {
    k.metaTick = teraz - 1;
    k.metaUlamek = ulamek;
    long ms = czasMs(k);
    ukonczyli++;
    k.pozycja = ukonczyli;
    plugin.debug(k.nick + " meta " + czas(ms) + " miejsce " + ukonczyli);
    Rekordy.Wynik w = plugin.rekordy.zapisz(tor.id, k.id, k.nick, k.gatunek.id(), ms, k.najlepszeOkrazenieMs);
    String miejsce = tryb == Tryb.WYSCIG ? "<yellow>" + ukonczyli + ". miejsce</yellow> · " : "";
    String dopisek = w.rekordToru() ? " <gold>★ REKORD TORU!</gold>" : w.rekordOsobisty() ? " <green>Rekord osobisty!</green>" : "";
    tytul(p, "<green>META!</green>", miejsce + "<white>" + czas(ms) + "</white>" + dopisek, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f);
    wyniki.add(ukonczyli + ". " + k.nick + " (" + k.gatunek.nazwa() + ") — " + czas(ms) + (w.rekordToru() ? " ★" : ""));
    p.sendMessage(MM.deserialize("<gold>[Wyścigi]</gold> <gray>Czas:</gray> <white>" + czas(ms) + "</white> <gray>· najlepsze okrążenie:</gray> <white>" + czas(k.najlepszeOkrazenieMs) + "</white>"
        + (w.poprzedniOsobisty() > 0 && !w.rekordOsobisty() ? " <gray>· twój rekord:</gray> " + czas(w.poprzedniOsobisty()) : "") + dopisek));
    if (k.mob != null) swiat.spawnParticle(Particle.FIREWORK, k.mob.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
    plugin.pasekBoss(p, "<green>Meta!</green> <white>" + czas(ms) + "</white>", 1f);
    if (tryb == Tryb.WYSCIG && ukonczyli == 1) {
      koniecDo = teraz + u.poZwyciezcy * 20;
      for (Kierowca inny : List.copyOf(kierowcy)) {
        Player ip = inny.gracz();
        if (ip != null && inny != k) ip.sendMessage(MM.deserialize("<gold>[Wyścigi]</gold> <yellow>" + k.nick + "</yellow> <gray>wygrywa! Masz</gray> <white>" + u.poZwyciezcy + " s</white> <gray>na dojazd.</gray>"));
      }
    }
    // Czasówka i pojedynczy jeździec wracają po chwili; w wyścigu z innymi czekamy na wszystkich.
    if (tryb == Tryb.CZASOWKA) Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.pokazRekordy(p), 30L);
  }

  private void zakonczWyscig(int teraz) {
    faza = Faza.KONIEC;
    koniecDo = teraz + u.poMecie * 20;
    for (Kierowca k : List.copyOf(kierowcy)) {
      if (k.naMecie()) continue;
      k.metaTick = teraz;
      wyniki.add("— " + k.nick + " (" + k.gatunek.nazwa() + ") — nie ukończył");
      Player p = k.gracz();
      if (p != null) tytul(p, "<red>Koniec czasu</red>", "<gray>Wyścig zakończony</gray>", Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f);
    }
    if (tryb == Tryb.WYSCIG) {
      List<String> linie = new ArrayList<>();
      linie.add("<gold>[Wyścigi]</gold> <yellow>" + tor.nazwa + "</yellow> <gray>— wyniki:</gray>");
      for (String w : wyniki) linie.add("<white>" + MM.escapeTags(w) + "</white>");
      for (String nick : uczestnicy) {
        Player p = Bukkit.getPlayerExact(nick);
        if (p != null) for (String l : linie) p.sendMessage(MM.deserialize(l));
      }
    }
  }

  private void pozycje() {
    List<Kierowca> lista = new ArrayList<>(kierowcy);
    lista.sort(Comparator.<Kierowca>comparingInt(k -> k.naMecie() ? 0 : 1)
        .thenComparingLong(k -> k.naMecie() ? czasMs(k) : 0)
        .thenComparingDouble(k -> -k.postep));
    for (int i = 0; i < lista.size(); i++) if (!lista.get(i).naMecie()) lista.get(i).pozycja = i + 1;
  }

  private void hud(Kierowca k, Player p, int teraz) {
    int okr = Math.max(1, Math.min(tor.okrazenia, (int) Math.floor(k.postep / tor.dlugosc) + 1));
    String poz = tryb == Tryb.WYSCIG ? " <gray>·</gray> <yellow>Pozycja " + k.pozycja + "/" + kierowcy.size() + "</yellow>" : "";
    float postep = (float) Math.max(0, Math.min(1, k.postep / (tor.okrazenia * tor.dlugosc)));
    plugin.pasekBoss(p, "<gold>Okrążenie " + okr + "/" + tor.okrazenia + "</gold>" + poz + " <gray>·</gray> <white>" + czas((teraz - startTick) * 50L) + "</white>", postep);
    if (teraz % 4 == 0) {
      double bps = Math.abs(k.v) * 20;
      int kreski = (int) Math.round(Math.min(1.4, Math.abs(k.v) / k.gatunek.predkosc()) * 10);
      String kolor = k.turbo > 0 ? "§b" : k.pozaTorem ? "§6" : "§a";
      String tekst = kolor + "▮".repeat(Math.min(14, kreski)) + "§8" + "▮".repeat(Math.max(0, 10 - kreski)) + " §f" + String.format("%.0f", bps) + " §7bl/s";
      if (k.poslizg) {
        double mn = "zelaznorog".equals(k.gatunek.id()) ? 0.85 : 1;
        tekst += k.ladowanie >= u.turbo2 * mn ? "  §6§lPOŚLIZG ✦✦" : k.ladowanie >= u.turbo1 * mn ? "  §9§lPOŚLIZG ✦" : "  §7POŚLIZG";
      } else if (k.pozaTorem) tekst += "  §6pobocze";
      pasek(p, 5, tekst);
    }
  }

  long czasMs(Kierowca k) {
    if (!k.naMecie() || startTick < 0) return Long.MAX_VALUE;
    return Math.round(((k.metaTick - startTick) + k.metaUlamek) * 50);
  }

  static String czas(long ms) {
    if (ms == Long.MAX_VALUE || ms < 0) return "—";
    return String.format("%d:%02d.%03d", ms / 60000, (ms / 1000) % 60, ms % 1000);
  }

  private static double mod(double a, double m) { return ((a % m) + m) % m; }

  void pasek(Player p, int priorytet, String tekst) {
    if (p == null) return;
    if (MMOPlayerData.has(p)) MMOPlayerData.get(p).getActionBar().show(priorytet, 10, tekst);
    else p.sendActionBar(Component.text(tekst));
  }

  static void tytul(Player p, String gora, String dol, Sound dzwiek, float ton) {
    if (p == null) return;
    p.showTitle(Title.title(MM.deserialize(gora), MM.deserialize(dol), Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ofMillis(250))));
    if (dzwiek != null) p.playSound(p.getLocation(), dzwiek, 1f, ton);
  }

  static BossBar nowyPasek() { return BossBar.bossBar(Component.empty(), 0f, BossBar.Color.YELLOW, BossBar.Overlay.NOTCHED_10); }
}
