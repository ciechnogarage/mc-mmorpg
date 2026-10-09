package pl.mcmmorpg.chowance.model;

import java.util.List;
import java.util.Map;

public record Startery(int poziom, List<String> domyslne, Map<String, List<String>> rasy,
    Map<String, List<String>> klasy) {

  public static Startery puste() { return new Startery(5, List.of(), Map.of(), Map.of()); }

  /** Oferta: rasa > klasa > domyślne. rasa/klasa mogą być null. */
  public List<String> oferta(String rasa, String klasa) {
    if (rasa != null && rasy.containsKey(rasa)) return rasy.get(rasa);
    if (klasa != null && klasy.containsKey(klasa)) return klasy.get(klasa);
    return domyslne;
  }
}
