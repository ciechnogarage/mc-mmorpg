package pl.mcmmorpg.chowance.model;

import java.util.List;

/** trenerzy/*.yml (KONTRAKT §7). */
public record Trener(String id, String nazwa, Rodzaj rodzaj, String odznaka, int wymaganeOdznaki,
    List<Czlonek> druzyna, List<String> nagrodaKomendy) {

  public enum Rodzaj { MISTRZ, TRENER }

  public record Czlonek(String gatunek, int poziom, List<String> ruchy, String cecha, int geny, String charakter) {}
}
