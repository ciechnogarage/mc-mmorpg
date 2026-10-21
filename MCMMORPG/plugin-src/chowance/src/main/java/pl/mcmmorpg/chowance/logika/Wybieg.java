package pl.mcmmorpg.chowance.logika;

import java.util.List;

/** Dopasowanie spawnera Wybiegu bez zależności od Bukkit. */
public final class Wybieg {
  private Wybieg() {}

  public static boolean spawnerPasuje(String nazwa, List<String> prefiksy) {
    if (nazwa == null || prefiksy == null) return false;
    for (String prefiks : prefiksy) {
      if (prefiks != null && !prefiks.isEmpty() && nazwa.startsWith(prefiks)) return true;
    }
    return false;
  }
}
