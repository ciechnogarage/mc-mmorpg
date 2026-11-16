package pl.mcmmorpg.chowance.model;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

/** Gatunek wczytany z gatunki/*.yml (KONTRAKT §2). Niezmienny. */
public record Gatunek(
    String id,
    String nazwa,
    List<String> opis,
    List<Typ> typy,
    Rzadkosc rzadkosc,
    int wspolczynnikZlapania,
    int stadium,
    String linia,
    int[] statyBazowe,
    KrzywaExp krzywa,
    int expBazowy,
    int wiezStartowa,
    List<String> cechy,
    NavigableMap<Integer, List<String>> ruchy,
    List<Ewolucja> ewolucje,
    List<String> grupyJaj,
    int dystansJaja,
    boolean lapaniePieczecia,
    boolean oswajanie,
    Map<String, Integer> przysmaki,
    String wymaganaPieczec,
    boolean legendarny,
    boolean wymienialny,
    boolean wierzchowiec,
    String mobOswojony,
    String mobOswojonyLsniacy,
    String mobDziki,
    String mobDzikiLsniacy,
    String mobZastepczy,
    String ikona,
    String ikonaLsniaca,
    List<String> wystepowanie,
    boolean testowy) {

  public int bazowy(Stat s) { return statyBazowe[s.ordinal()]; }

  public boolean plodny() { return !grupyJaj.isEmpty() && !grupyJaj.contains("NIEPLODNY"); }

  public String mobOswojony(boolean lsniacy) {
    return lsniacy && mobOswojonyLsniacy != null && !mobOswojonyLsniacy.isBlank() ? mobOswojonyLsniacy : mobOswojony;
  }

  public String mobDziki(boolean lsniacy) {
    return lsniacy && mobDzikiLsniacy != null && !mobDzikiLsniacy.isBlank() ? mobDzikiLsniacy : mobDziki;
  }

  public String ikona(boolean lsniacy) {
    return lsniacy && ikonaLsniaca != null && !ikonaLsniaca.isBlank() ? ikonaLsniaca : ikona;
  }

  /** Ruchy dostępne do poziomu włącznie, w kolejności nauki (bez duplikatów). */
  public List<String> ruchyDo(int poziom) {
    java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
    for (Map.Entry<Integer, List<String>> e : ruchy.headMap(poziom, true).entrySet()) {
      out.addAll(e.getValue());
    }
    return List.copyOf(out);
  }

  /** Ruchy uczone dokładnie na danym poziomie. */
  public List<String> ruchyNaPoziomie(int poziom) {
    return ruchy.getOrDefault(poziom, List.of());
  }
}
