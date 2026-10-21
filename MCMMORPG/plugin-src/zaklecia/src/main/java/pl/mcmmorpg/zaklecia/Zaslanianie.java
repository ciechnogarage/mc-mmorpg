package pl.mcmmorpg.zaklecia;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.util.BoundingBox;

/** Pierwsza przeszkoda na linii strumienia; własne przywołania można pominąć. */
final class Zaslanianie {
  private final boolean wlaczone;
  private final boolean wlasneZaslaniaja;
  private final NamespacedKey kWlasciciel;
  private final Zaklecia plugin;
  private long sondy, promienie, nanos;

  Zaslanianie(Zaklecia plugin) {
    this.plugin = plugin;
    wlaczone = plugin.getConfig().getBoolean("zaslanianie.wlaczone", true);
    wlasneZaslaniaja = plugin.getConfig().getBoolean("zaslanianie.wlasne-przywolania-zaslaniaja", true);
    kWlasciciel = new NamespacedKey(plugin, "sluga_wlasciciel");
  }

  boolean wlaczone() { return wlaczone; }

  boolean wlasne(Player p, Entity e) {
    if (wlasneZaslaniaja) return false;
    String id = e.getPersistentDataContainer().get(kWlasciciel, PersistentDataType.STRING);
    return p.getUniqueId().toString().equals(id);
  }

  boolean istota(Player p, Entity e) {
    return e != p && e instanceof LivingEntity && !(e instanceof ArmorStand)
        && !wlasne(p, e);
  }

  boolean pierwszyCel(Player p, LivingEntity cel, Location start, Vector doCelu, double zasieg) {
    if (!wlaczone) return p.hasLineOfSight(cel);
    double odleglosc = doCelu.length();
    if (odleglosc < 0.01) return false;
    long t0 = System.nanoTime();
    BoundingBox bb = cel.getBoundingBox();
    Vector center = bb.getCenter();
    Vector gaze = start.getDirection().normalize();
    int rays = 1;
    RayTraceResult hit = p.getWorld().rayTrace(start, gaze, zasieg,
        FluidCollisionMode.NEVER, true, 0.15, e -> istota(p, e));
    boolean visible = hit != null && cel.equals(hit.getHitEntity());
    if (!visible) {
      Vector side = new Vector(-doCelu.getZ(), 0, doCelu.getX()).normalize();
      double halfWidth = (Math.abs(side.getX()) * bb.getWidthX()
          + Math.abs(side.getZ()) * bb.getWidthZ()) / 2;
      Vector[] points = new Vector[21];
      double[] angles = new double[21];
      int count = 0;
      for (double column : new double[] {-0.99, -0.9, -0.45, 0, 0.45, 0.9, 0.99}) {
        for (double row : new double[] {0.15, 0.5, 0.9}) {
          Vector point = new Vector(center.getX() + side.getX() * halfWidth * column,
              bb.getMinY() + bb.getHeight() * row,
              center.getZ() + side.getZ() * halfWidth * column);
          double angle = gaze.angle(point.clone().subtract(start.toVector()));
          int pos = count;
          while (pos > 0 && angles[pos - 1] > angle) {
            angles[pos] = angles[pos - 1]; points[pos] = points[pos - 1]; pos--;
          }
          angles[pos] = angle; points[pos] = point; count++;
        }
      }
      double limit = gaze.angle(doCelu) + Math.toRadians(2);
      for (int i = 0; i < count && angles[i] <= limit; i++) {
        Vector ray = points[i].subtract(start.toVector());
        double distance = ray.length();
        if (distance < 0.01) continue;
        rays++;
        hit = p.getWorld().rayTrace(start, ray.multiply(1 / distance),
            Math.min(distance + 0.3, zasieg), FluidCollisionMode.NEVER, true, 0.15, e -> istota(p, e));
        if (hit != null && cel.equals(hit.getHitEntity())) { visible = true; break; }
      }
    }
    if (plugin.getConfig().getBoolean("debug", false)) {
      sondy++; promienie += rays; nanos += System.nanoTime() - t0;
      if (sondy % 20 == 0) plugin.getLogger().info("ZAKDBG zaslona sondy=" + sondy
          + " promienie=" + promienie + " avg-us=" + (nanos / sondy / 1000));
    }
    return visible;
  }
}
