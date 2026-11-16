package pl.mcmmorpg.animacjebroni;

import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Sygnaly do mobow MythicMobs (ActiveMob.signalMob, w configu moba ~onSignal:NAZWA, @trigger = gracz).
 * Osobna klasa: ladowana tylko, gdy MythicMobs jest wlaczony (softdepend), wiec bez MM nie ma NoClassDefFoundError.
 */
final class SygnalyMobow {
  private final double zasieg;

  SygnalyMobow(double zasieg) { this.zasieg = zasieg; }

  /** Wysyla sygnal mobom MM, ktore celuja w gracza i stoja przed nim w promieniu zasiegu. Zwraca liczbe odbiorcow. */
  int wyslij(Player p, String sygnal) {
    Vector przod = p.getLocation().getDirection().setY(0);
    if (przod.lengthSquared() < 1e-4) return 0;
    przod.normalize();
    var gracz = BukkitAdapter.adapt(p);
    var moby = MythicBukkit.inst().getMobManager();
    int ile = 0;
    for (Entity e : p.getNearbyEntities(zasieg, 3, zasieg)) {
      if (!(e instanceof Mob m) || m.isDead() || !p.equals(m.getTarget())) continue;
      Vector doMoba = e.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
      if (doMoba.lengthSquared() > zasieg * zasieg || doMoba.dot(przod) <= 0) continue;
      var am = moby.getActiveMob(e.getUniqueId());
      if (am.isEmpty()) continue;
      am.get().signalMob(gracz, sygnal);
      ile++;
    }
    return ile;
  }
}
