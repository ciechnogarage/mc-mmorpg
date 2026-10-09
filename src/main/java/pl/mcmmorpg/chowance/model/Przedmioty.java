package pl.mcmmorpg.chowance.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** przedmioty.yml (KONTRAKT §5). Klucze map = ID MMOItems. */
public record Przedmioty(
    Map<String, Pieczec> pieczecie,
    Set<String> kamienie,
    Map<String, Karma> karma,
    Map<String, Eliksir> eliksiry,
    Map<String, String> siodla,
    String jajoIkona) {

  public enum RodzajPieczeci {
    ZWYKLA, NOCNA, WODNA, KRADZIEZY, LOCHOWA, LEGENDARNA;

    public static RodzajPieczeci parse(String s) {
      if (s == null) return null;
      try { return valueOf(s.trim().toUpperCase(java.util.Locale.ROOT)); } catch (IllegalArgumentException e) { return null; }
    }
  }

  public record Pieczec(String id, String typMmoitems, RodzajPieczeci rodzaj, double mnoznik,
      double mnoznikWarunku, String modelMob) {}

  public record Karma(String id, String typMmoitems, int exp, int wiez) {}

  public record Eliksir(String id, String typMmoitems, double leczenieProcent, boolean wskrzesza, boolean leczyStatus) {}

  public static Przedmioty puste() {
    return new Przedmioty(Map.of(), Set.of(), Map.of(), Map.of(), Map.of(), "JAJO_CHOWANCA");
  }

  /** Wszystkie ID, które silnik traktuje jako „swoje” (do walidacji ewolucji). */
  public boolean znaKamien(String id) { return kamienie.contains(id); }

  public List<String> wszystkieId() {
    java.util.ArrayList<String> r = new java.util.ArrayList<>();
    r.addAll(pieczecie.keySet()); r.addAll(kamienie); r.addAll(karma.keySet());
    r.addAll(eliksiry.keySet()); r.addAll(siodla.keySet());
    return r;
  }
}
