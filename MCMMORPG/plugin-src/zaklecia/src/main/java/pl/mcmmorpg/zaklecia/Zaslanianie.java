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

  boolean pierwszyCel(Player p, LivingEntity cel, Location start, Vector doCelu) {
    if (!wlaczone) return p.hasLineOfSight(cel);
    double odleglosc = doCelu.length();
    if (odleglosc < 0.01) return false;
    long t0 = System.nanoTime();
    BoundingBox bb = cel.getBoundingBox();
    Vector center = bb.getCenter();
    Vector side = new Vector(-doCelu.getZ(), 0, doCelu.getX()).normalize()
        .multiply(Math.min(bb.getWidthX(), bb.getWidthZ()) * 0.4);
    Vector[] points = {center,
        new Vector(center.getX(), bb.getMinY() + bb.getHeight() * 0.85, center.getZ()),
        new Vector(center.getX(), bb.getMinY() + bb.getHeight() * 0.2, center.getZ()),
        center.clone().add(side), center.clone().subtract(side)};
    double centerAngle = start.getDirection().angle(doCelu);
    boolean visible = false;
    int rays = 0;
    for (Vector point : points) {
      Vector ray = point.subtract(start.toVector());
      double distance = ray.length();
      if (distance < 0.01 || start.getDirection().angle(ray) > centerAngle + Math.toRadians(2)) continue;
      rays++;
      RayTraceResult hit = p.getWorld().rayTrace(start, ray.multiply(1 / distance),
          distance + 0.3, FluidCollisionMode.NEVER, true, 0.15, e -> istota(p, e));
      if (hit != null && cel.equals(hit.getHitEntity())) { visible = true; break; }
    }
    if (plugin.getConfig().getBoolean("debug", false)) {
      sondy++; promienie += rays; nanos += System.nanoTime() - t0;
      if (sondy % 20 == 0) plugin.getLogger().info("ZAKDBG zaslona sondy=" + sondy
          + " promienie=" + promienie + " avg-us=" + (nanos / sondy / 1000));
    }
    return visible;
  }
}
