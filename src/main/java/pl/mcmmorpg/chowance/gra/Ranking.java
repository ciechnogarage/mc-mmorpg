package pl.mcmmorpg.chowance.gra;

import com.google.gson.reflect.TypeToken;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.dane.ProfilDane;
import pl.mcmmorpg.chowance.logika.Elo;

/**
 * Ranking PvP (ELO) per profil + sezony. Indeks dane/ranking.json pozwala pokazać TOP bez czytania
 * plików wszystkich profili. Sezon: dane/sezon.txt; nowy sezon archiwizuje indeks i zeruje ELO leniwie
 * (profil z innym numerem sezonu startuje od elo_start).
 */
public final class Ranking {
  public static final class Wpis {
    public String nick;
    public int elo;
    public int w, l;
  }

  private final Chowance c;
  private final Path plik, plikSezonu;
  private final Map<String, Wpis> indeks = new ConcurrentHashMap<>();
  private volatile int sezon = 1;
  private volatile List<Map.Entry<String, Wpis>> top = List.of();

  public Ranking(Chowance c) {
    this.c = c;
    this.plik = c.dane.katalog().resolve("ranking.json");
    this.plikSezonu = c.dane.katalog().resolve("sezon.txt");
    try {
      if (Files.isRegularFile(plikSezonu)) sezon = Integer.parseInt(Files.readString(plikSezonu).trim());
      if (Files.isRegularFile(plik)) {
        Map<String, Wpis> m = c.dane.gson().fromJson(Files.readString(plik, StandardCharsets.UTF_8), new TypeToken<Map<String, Wpis>>() {}.getType());
        if (m != null) indeks.putAll(m);
      }
    } catch (Exception e) {
      c.log.warning("Nie wczytano rankingu: " + e);
    }
    przelicz();
  }

  public int sezon() { return sezon; }

  public int elo(ProfilDane d) {
    return d.elo < 0 || d.sezon != sezon ? c.ust.eloStart : d.elo;
  }

  private void ustaw(ProfilDane d, int elo, boolean wygral) {
    if (d.sezon != sezon) { d.wygrane = 0; d.przegrane = 0; }
    d.elo = elo;
    d.sezon = sezon;
    if (wygral) d.wygrane++; else d.przegrane++;
    d.brudne = true;
    Wpis w = new Wpis();
    w.nick = d.nick;
    w.elo = elo;
    w.w = d.wygrane;
    w.l = d.przegrane;
    indeks.put(d.profil, w);
  }

  /** Wynik meczu rankingowego. Zwraca [nowe elo zwycięzcy, nowe elo przegranego]. */
  public int[] wynik(Player zw, ProfilDane dz, Player pr, ProfilDane dp) {
    int[] n = Elo.po(elo(dz), elo(dp), c.ust.eloK);
    ustaw(dz, n[0], true);
    ustaw(dp, n[1], false);
    przelicz();
    zapisz();
    return n;
  }

  private void przelicz() {
    List<Map.Entry<String, Wpis>> l = new ArrayList<>(indeks.entrySet());
    l.sort(Comparator.comparingInt((Map.Entry<String, Wpis> e) -> -e.getValue().elo).thenComparing(e -> e.getValue().nick == null ? "" : e.getValue().nick));
    top = List.copyOf(l);
  }

  public List<Map.Entry<String, Wpis>> top() { return top; }

  /** Usuwa wpisy o danym nicku (sprzątanie po botach z evali). Zwraca liczbę usuniętych. */
  public int usun(String nick) {
    int przed = indeks.size();
    indeks.values().removeIf(w -> w.nick != null && w.nick.equalsIgnoreCase(nick));
    int n = przed - indeks.size();
    if (n > 0) { przelicz(); zapisz(); }
    return n;
  }

  public int pozycja(String profil) {
    List<Map.Entry<String, Wpis>> t = top;
    for (int i = 0; i < t.size(); i++) if (t.get(i).getKey().equals(profil)) return i + 1;
    return 0;
  }

  private void zapisz() {
    String json = c.dane.gson().toJson(new LinkedHashMap<>(indeks));
    c.dane.wykonajIO(() -> {
      try {
        Path tmp = plik.resolveSibling("ranking.json.tmp");
        Files.writeString(tmp, json, StandardCharsets.UTF_8);
        Files.move(tmp, plik, StandardCopyOption.REPLACE_EXISTING);
      } catch (Exception e) {
        c.log.warning("Nie zapisano rankingu: " + e);
      }
    });
  }

  public void nowySezon() {
    int stary = sezon;
    String json = c.dane.gson().toJson(new LinkedHashMap<>(indeks));
    sezon = stary + 1;
    indeks.clear();
    przelicz();
    int nowy = sezon;
    c.dane.wykonajIO(() -> {
      try {
        Files.writeString(c.dane.katalog().resolve("ranking-sezon-" + stary + ".json"), json, StandardCharsets.UTF_8);
        Files.writeString(plikSezonu, String.valueOf(nowy));
        Files.deleteIfExists(plik);
      } catch (Exception e) {
        c.log.warning("Nie zarchiwizowano sezonu: " + e);
      }
    });
  }
}
