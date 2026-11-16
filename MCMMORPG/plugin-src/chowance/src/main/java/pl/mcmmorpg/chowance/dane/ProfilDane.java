package pl.mcmmorpg.chowance.dane;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import pl.mcmmorpg.chowance.model.Osobnik;

/**
 * Wszystko, co Chowańce trzymają dla JEDNEGO profilu (postaci) MMOProfiles. Worek pod Gson.
 * Zapis: dane/profile/&lt;uuidProfilu&gt;.json — patrz {@link MagazynDanych}.
 */
public final class ProfilDane {
  public static final int SCHEMAT = 1;

  public int schemat = SCHEMAT;
  public String profil;
  public String gracz;
  public String nick;
  public List<Osobnik> druzyna = new ArrayList<>();
  public List<Osobnik> magazyn = new ArrayList<>();
  /** gatunek -> 1 widziany, 2 złapany */
  public Map<String, Integer> bestiariusz = new LinkedHashMap<>();
  /** gatunki złapane jako lśniące */
  public Set<String> lsniace = new LinkedHashSet<>();
  public Set<String> nagrody = new LinkedHashSet<>();
  public Set<String> odznaki = new LinkedHashSet<>();
  public boolean starter;
  /** Brak pola w starym JSON oznacza profil sprzed samouczka. */
  public int pierwszeKrokiWersja;
  public int pierwszeKrokiMaska;
  public Set<Integer> pierwszeKrokiNagrody = new LinkedHashSet<>();
  public Map<String, Long> ostatniePoglaszczanie = new LinkedHashMap<>();
  public int elo = -1;
  /** sezon rankingu, w którym liczone jest elo */
  public int sezon;
  public int wygrane, przegrane;
  public Map<String, Integer> wieza = new LinkedHashMap<>();
  public Set<String> nagrodyWiezy = new LinkedHashSet<>();
  public Osobnik hodowlaA, hodowlaB;
  public long hodowlaStart;
  public List<Jajo> jaja = new ArrayList<>();
  public long utworzono = System.currentTimeMillis();

  public transient int stronaMagazynu;
  public transient int stronaBestiariusza;
  public transient volatile boolean brudne;

  public Osobnik znajdz(String id) {
    for (Osobnik o : druzyna) if (o.id.equals(id)) return o;
    for (Osobnik o : magazyn) if (o.id.equals(id)) return o;
    return null;
  }

  public int stanBestiariusza(String gatunek) {
    return bestiariusz.getOrDefault(gatunek, 0);
  }

  public boolean oznaczWidziany(String gatunek) {
    if (stanBestiariusza(gatunek) >= 1) return false;
    bestiariusz.put(gatunek, 1);
    brudne = true;
    return true;
  }

  public void oznaczZlapany(String gatunek, boolean lsniacy) {
    bestiariusz.put(gatunek, 2);
    if (lsniacy) lsniace.add(gatunek);
    brudne = true;
  }

  public int wszystkich() { return druzyna.size() + magazyn.size(); }
}
