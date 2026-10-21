package pl.mcmmorpg.chowance.model;

import java.util.List;

public record NagrodaBestiariusza(String id, Warunek warunek, int wartosc, Typ typ, String opis, List<String> komendy) {
  public enum Warunek { PROCENT_ZLAPANYCH, KOMPLET_TYPU, WSZYSTKIE_LSNIACE }
}
