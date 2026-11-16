package pl.mcmmorpg.chowance.gra;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.entity.Entity;

/** Środowisko ciała dla cech z warunkiem `gdy` (PROBLEMY B-003): NOC|DZIEN, DESZCZ, BURZA, W_WODZIE. */
public final class Srodowisko {
  private Srodowisko() {}

  public static Set<String> z(Entity e) {
    Set<String> s = new HashSet<>();
    if (e == null || e.getWorld() == null) return s;
    long t = e.getWorld().getTime();
    s.add(t >= 13000 && t <= 23000 ? "NOC" : "DZIEN");
    if (e.getWorld().hasStorm()) s.add("DESZCZ");
    if (e.getWorld().isThundering()) s.add("BURZA");
    if (e.isInWater()) s.add("W_WODZIE");
    return s;
  }
}
