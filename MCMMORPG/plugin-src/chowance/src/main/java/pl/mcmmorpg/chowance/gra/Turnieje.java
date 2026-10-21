package pl.mcmmorpg.chowance.gra;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.dane.ProfilDane;
import pl.mcmmorpg.chowance.logika.Drabinka;

/**
 * Turniej sezonowy (SPEC §6): zapisy, drabinka pucharowa rozstawiona po ELO, mecze = walki turowe
 * (rodzaj TURNIEJ, rankingowe). Walkower, gdy gracz jest offline przy starcie meczu albo mecz nie
 * zacznie się / nie skończy w turniej.czas_na_mecz_s. Przebieg dopisywany do dane/turnieje.log.
 * Stan trzymany w pamięci — restart serwera przerywa turniej (zapisane w DECYZJE).
 */
public final class Turnieje {
  private final Chowance c;
  private final WalkiTurowe walki;
  private final Ranking ranking;
  private final Map<UUID, String> zapisani = new LinkedHashMap<>();
  private Drabinka drabinka;
  private final Map<Drabinka.Para, String> wyniki = new HashMap<>();
  private final Map<Drabinka.Para, Long> terminy = new HashMap<>();
  private String nazwa = "";

  public Turnieje(Chowance c, WalkiTurowe walki, Ranking ranking) {
    this.c = c; this.walki = walki; this.ranking = ranking;
    walki.poMeczuTurnieju = this::wynik;
  }

  public String stan() {
    if (drabinka != null) return drabinka.koniec() ? "ZAKONCZONY" : "RUNDA_" + drabinka.runda();
    return zapisani.isEmpty() ? "BRAK" : "ZAPISY";
  }

  public String zapisz(Player p) {
    if (drabinka != null) return "Turniej już trwa.";
    ProfilDane d = c.dane(p);
    if (d == null) return "Wybierz postać.";
    if (d.druzyna.isEmpty()) return "Potrzebujesz przynajmniej jednego chowańca.";
    zapisani.put(p.getUniqueId(), p.getName());
    return null;
  }

  public boolean wypisz(Player p) { return drabinka == null && zapisani.remove(p.getUniqueId()) != null; }

  public int zapisanych() { return zapisani.size(); }

  public String start(String nazwa) {
    if (drabinka != null && !drabinka.koniec()) return "Turniej już trwa.";
    List<UUID> online = new ArrayList<>();
    for (UUID u : zapisani.keySet()) if (Bukkit.getPlayer(u) != null && c.dane(Bukkit.getPlayer(u)) != null) online.add(u);
    if (online.size() < c.ust.turniejMin) return "Za mało zapisanych online (" + online.size() + "/" + c.ust.turniejMin + ").";
    online.sort((a, b) -> Integer.compare(ranking.elo(c.dane(Bukkit.getPlayer(b))), ranking.elo(c.dane(Bukkit.getPlayer(a)))));
    List<String> rozst = new ArrayList<>();
    for (UUID u : online) rozst.add(u.toString());
    this.nazwa = nazwa;
    drabinka = new Drabinka(rozst);
    wyniki.clear();
    log("START " + nazwa + " uczestnicy=" + online.size());
    ogloszenie("<gold>Turniej <yellow>" + nazwa + "</yellow> rusza! Uczestników: " + online.size());
    rundaStart();
    return null;
  }

  private void rundaStart() {
    ogloszenie("<gold>Runda " + drabinka.runda() + ":");
    long termin = System.currentTimeMillis() + c.ust.turniejCzasMeczuS * 1000L;
    for (Drabinka.Para p : drabinka.pary()) {
      if (p.wolnyLos()) { ogloszenie("<gray>" + nick(p.ktokolwiek()) + " — wolny los."); continue; }
      terminy.put(p, termin);
      ogloszenie("<yellow>" + nick(p.a()) + " <gray>vs <yellow>" + nick(p.b()));
      Player a = Bukkit.getPlayer(UUID.fromString(p.a())), b = Bukkit.getPlayer(UUID.fromString(p.b()));
      if (a == null && b == null) wyniki.put(p, p.a());
      else if (a == null) walkower(p, p.b());
      else if (b == null) walkower(p, p.a());
      else if (!walki.startPvp(a, b, WalkiTurowe.Rodzaj.TURNIEJ)) walkowerZaStan(p, a, b);
    }
    sprawdzRunde();
  }

  private void walkowerZaStan(Drabinka.Para p, Player a, Player b) {
    // Kto nie może walczyć (brak sprawnych), oddaje mecz; gdy obaj — wyżej rozstawiony przechodzi.
    boolean okA = mozeWalczyc(a), okB = mozeWalczyc(b);
    walkower(p, okA || !okB ? p.a() : p.b());
  }

  private boolean mozeWalczyc(Player p) {
    ProfilDane d = c.dane(p);
    return d != null && !d.druzyna.isEmpty();
  }

  private void walkower(Drabinka.Para p, String zwyciezca) {
    wyniki.put(p, zwyciezca);
    ogloszenie("<gray>Walkower: przechodzi " + nick(zwyciezca));
    log("WALKOWER " + nick(zwyciezca));
  }

  /** Wynik meczu z WalkiTurowe (rodzaj TURNIEJ). */
  private void wynik(UUID zw, UUID pr) {
    if (drabinka == null) return;
    for (Drabinka.Para p : drabinka.pary()) {
      if (p.wolnyLos() || wyniki.containsKey(p)) continue;
      if ((p.a().equals(zw.toString()) && p.b().equals(pr.toString())) || (p.b().equals(zw.toString()) && p.a().equals(pr.toString()))) {
        wyniki.put(p, zw.toString());
        ogloszenie("<green>" + nick(zw.toString()) + " pokonuje " + nick(pr.toString()));
        log("MECZ " + nick(zw.toString()) + " > " + nick(pr.toString()));
        sprawdzRunde();
        return;
      }
    }
  }

  /** Co sekundę: mecze po terminie = walkower dla tego, kto jest online (albo wyżej rozstawionego). */
  public void tik() {
    if (drabinka == null || drabinka.koniec()) return;
    long teraz = System.currentTimeMillis();
    boolean zmiana = false;
    for (Drabinka.Para p : drabinka.pary()) {
      if (p.wolnyLos() || wyniki.containsKey(p)) continue;
      Long t = terminy.get(p);
      if (t != null && teraz > t) {
        Player a = Bukkit.getPlayer(UUID.fromString(p.a()));
        Player b = Bukkit.getPlayer(UUID.fromString(p.b()));
        if (a != null && walki.wWalce(a)) walki.przerwij(a);
        walkower(p, a != null || b == null ? p.a() : p.b());
        zmiana = true;
      }
    }
    if (zmiana) sprawdzRunde();
  }

  private void sprawdzRunde() {
    for (Drabinka.Para p : drabinka.pary()) if (!p.wolnyLos() && !wyniki.containsKey(p)) return;
    drabinka.zakonczRunde(wyniki);
    wyniki.clear();
    terminy.clear();
    if (drabinka.koniec()) {
      String m = drabinka.mistrz();
      ogloszenie("<gold><b>Mistrzem turnieju " + nazwa + " zostaje " + nick(m) + "!</b>");
      log("MISTRZ " + nick(m));
      c.zdarzenie("WYGRANA_TURNIEJU", Map.of("gracz", nick(m), "turniej", nazwa));
      zapisani.clear();
      drabinka = null;
      return;
    }
    Bukkit.getScheduler().runTaskLater(c.plugin, this::rundaStart, 100L);
  }

  public void stop() {
    if (drabinka != null) log("STOP " + nazwa);
    drabinka = null;
    wyniki.clear();
    terminy.clear();
    zapisani.clear();
  }

  private String nick(String uuid) {
    if (uuid == null) return "?";
    String n = zapisani.get(UUID.fromString(uuid));
    return n == null ? uuid.substring(0, 8) : n;
  }

  private void ogloszenie(String t) {
    for (UUID u : zapisani.keySet()) {
      Player p = Bukkit.getPlayer(u);
      if (p != null) c.msg(p, t);
    }
    c.log.info("[Turniej] " + Chowance.MM.stripTags(t));
  }

  private void log(String t) {
    String wpis = Instant.now() + " " + t + "\n";
    c.dane.wykonajIO(() -> {
      try {
        Files.writeString(c.dane.katalog().resolve("turnieje.log"), wpis, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (Exception e) {
        c.log.warning("turnieje.log: " + e);
      }
    });
  }
}
