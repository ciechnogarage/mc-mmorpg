package pl.mcmmorpg.wiary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Czysta logika wiar (bez serwera): bóstwa z configu, kto może którego wybrać, siła łaski i jej opis.
 * Wszystko, co tu jest, sprawdzają testy jednostkowe — plugin tylko podaje dane gracza.
 */
public final class Panteon {
  public static final String STARSI = "starsi";

  /** Jeden efekt łaski: stat MythicLib o wartości baza + naPunkt * min(wiara, limit). */
  public record Efekt(String stat, String typ, double baza, double naPunkt, double skala, String opis) {
    public double wartosc(int wiara, int limit) {
      return baza + naPunkt * Math.max(0, Math.min(wiara, limit));
    }

    public String tekst(int wiara, int limit) {
      return "+" + liczba(wartosc(wiara, limit) * skala) + opis;
    }
  }

  public record Bostwo(String id, String imie, String tytul, String grupa, String kolor, String domena, List<Efekt> efekty) {
    public String pelne() { return imie + ", " + tytul; }
  }

  private final Map<String, Bostwo> bostwa;
  private final Map<String, String> rasy;
  private final int limit;

  public Panteon(Map<String, Bostwo> bostwa, Map<String, String> rasy, int limit) {
    this.bostwa = Collections.unmodifiableMap(new LinkedHashMap<>(bostwa));
    this.rasy = Collections.unmodifiableMap(new LinkedHashMap<>(rasy));
    this.limit = limit;
  }

  /** Wczytuje config; problemy zwraca w liście (plugin je loguje), złe wpisy pomija. */
  public static Panteon wczytaj(ConfigurationSection cfg, List<String> problemy) {
    Map<String, String> rasy = new LinkedHashMap<>();
    ConfigurationSection r = cfg.getConfigurationSection("rasy");
    if (r != null) for (String k : r.getKeys(false)) rasy.put(k, r.getString(k, k));
    Map<String, Bostwo> out = new LinkedHashMap<>();
    ConfigurationSection b = cfg.getConfigurationSection("bostwa");
    if (b != null) {
      for (String id : b.getKeys(false)) {
        ConfigurationSection s = b.getConfigurationSection(id);
        if (s == null) continue;
        String grupa = s.getString("grupa", "");
        if (!grupa.equals(STARSI) && !rasy.containsKey(grupa)) {
          problemy.add("bóstwo " + id + ": nieznana grupa '" + grupa + "' — pominięte");
          continue;
        }
        List<Efekt> ef = new ArrayList<>();
        for (Map<?, ?> m : s.getMapList("efekty")) {
          String stat = String.valueOf(m.get("stat")).toUpperCase(Locale.ROOT);
          String typ = String.valueOf(m.containsKey("typ") ? m.get("typ") : "FLAT").toUpperCase(Locale.ROOT);
          if (!typ.equals("FLAT") && !typ.equals("RELATIVE")) {
            problemy.add("bóstwo " + id + ": typ '" + typ + "' (dozwolone FLAT, RELATIVE) — efekt pominięty");
            continue;
          }
          ef.add(new Efekt(stat, typ, num(m.get("baza")), num(m.get("na-punkt")),
              m.containsKey("skala") ? num(m.get("skala")) : 1.0, String.valueOf(m.containsKey("opis") ? m.get("opis") : "")));
        }
        if (ef.isEmpty()) {
          problemy.add("bóstwo " + id + ": brak efektów — pominięte");
          continue;
        }
        out.put(id, new Bostwo(id, s.getString("imie", id), s.getString("tytul", ""), grupa,
            s.getString("kolor", "&f"), s.getString("domena", ""), List.copyOf(ef)));
      }
    }
    return new Panteon(out, rasy, cfg.getInt("laska.wiara-limit", 40));
  }

  private static double num(Object o) {
    if (o instanceof Number n) return n.doubleValue();
    try { return Double.parseDouble(String.valueOf(o)); } catch (NumberFormatException e) { return 0; }
  }

  public Map<String, Bostwo> bostwa() { return bostwa; }
  public Map<String, String> rasy() { return rasy; }
  public int limit() { return limit; }
  public Bostwo bostwo(String id) { return id == null ? null : bostwa.get(id.toLowerCase(Locale.ROOT)); }

  /** Czy postać tej rasy może obrać to bóstwo: Starsi zawsze, reszta tylko własna rasa. */
  public boolean dostepne(Bostwo b, String rasa) {
    if (b == null) return false;
    if (b.grupa().equals(STARSI)) return rasa != null;
    return b.grupa().equals(rasa);
  }

  /** Bóstwa do wyboru dla rasy, w kolejności configu (3 Starszych + 3 rasy). */
  public List<Bostwo> doWyboru(String rasa) {
    List<Bostwo> l = new ArrayList<>();
    for (Bostwo b : bostwa.values()) if (dostepne(b, rasa)) l.add(b);
    return l;
  }

  public String opisLaski(Bostwo b, int wiara) {
    List<String> t = new ArrayList<>();
    for (Efekt e : b.efekty()) t.add(e.tekst(wiara, limit));
    return String.join(", ", t);
  }

  /** Milisekundy do końca odnowienia zmiany patrona; 0 = można teraz. Pierwszy wybór zawsze od razu. */
  public static long doZmiany(String obecny, long ostatniaZmiana, long teraz, long odnowienieMs) {
    if (obecny == null) return 0;
    return Math.max(0, ostatniaZmiana + odnowienieMs - teraz);
  }

  public static String liczba(double v) {
    double r = Math.round(v * 10.0) / 10.0;
    if (r == Math.rint(r)) return String.valueOf((long) r);
    return String.valueOf(r).replace('.', ',');
  }

  public static String czas(long ms) {
    long min = (ms + 59_999) / 60_000;
    if (min < 60) return min + " min";
    long h = min / 60, m = min % 60;
    return m == 0 ? h + " h" : h + " h " + m + " min";
  }
}
