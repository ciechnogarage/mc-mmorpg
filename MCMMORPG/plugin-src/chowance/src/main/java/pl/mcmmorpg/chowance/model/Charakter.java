package pl.mcmmorpg.chowance.model;

/** Charakter (natura): +10% jednego statu, -10% innego; plus == minus = neutralny. */
public record Charakter(String id, String nazwa, Stat plus, Stat minus) {
  public double mnoznik(Stat s) {
    if (plus == null || minus == null || plus == minus) return 1.0;
    if (s == plus) return 1.1;
    if (s == minus) return 0.9;
    return 1.0;
  }

  public boolean neutralny() { return plus == null || minus == null || plus == minus; }
}
