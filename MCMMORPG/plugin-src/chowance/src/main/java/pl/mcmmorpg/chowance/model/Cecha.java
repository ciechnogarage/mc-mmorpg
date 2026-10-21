package pl.mcmmorpg.chowance.model;

import java.util.Map;

public record Cecha(String id, String nazwa, String opis, RodzajCechy rodzaj, Map<String, String> parametry) {

  public String p(String k) { return parametry.get(k); }

  public double d(String k, double def) {
    try { return Double.parseDouble(parametry.get(k)); } catch (RuntimeException e) { return def; }
  }

  public Typ typ() { return Typ.parse(parametry.get("typ")); }
  public Status status() { return Status.parse(parametry.get("status")); }
  public Stat stat() { return Stat.parse(parametry.get("stat")); }

  public static final java.util.Set<String> SRODOWISKA = java.util.Set.of("NOC", "DZIEN", "DESZCZ", "BURZA", "W_WODZIE");

  /** Warunek środowiska (PROBLEMY B-003): pusty = zawsze. Wartości z {@link #SRODOWISKA}. */
  public java.util.Set<String> gdy() {
    String g = parametry.get("gdy");
    if (g == null || g.isBlank()) return java.util.Set.of();
    java.util.Set<String> out = new java.util.LinkedHashSet<>();
    for (String x : g.replace("[", "").replace("]", "").split(",")) if (!x.isBlank()) out.add(x.trim().toUpperCase(java.util.Locale.ROOT));
    return out;
  }

  /** Czy warunek środowiska jest spełniony (dowolny z wymienionych). */
  public boolean aktywnaW(java.util.Set<String> srodowisko) {
    java.util.Set<String> g = gdy();
    if (g.isEmpty()) return true;
    for (String x : g) if (srodowisko.contains(x)) return true;
    return false;
  }
}
