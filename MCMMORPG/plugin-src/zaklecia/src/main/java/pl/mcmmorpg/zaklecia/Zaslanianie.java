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

/** Pierwsza przeszkoda na linii strumienia; własne przywołania można pominąć. */
final class Zaslanianie {
  private final boolean wlaczone;
  private final boolean wlasneZaslaniaja;
  private final NamespacedKey kWlasciciel;

  Zaslanianie(Zaklecia plugin) {
    wlaczone = plugin.getConfig().getBoolean("zaslanianie.wlaczone", true);
    wlasneZaslaniaja = plugin.getConfig().getBoolean("zaslanianie.wlasne-przywolania-zaslaniaja", false);
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
    if (wlasne(p, cel)) return false;
    double odleglosc = doCelu.length();
    if (odleglosc < 0.01) return false;
    RayTraceResult hit = p.getWorld().rayTrace(start, doCelu.multiply(1 / odleglosc),
        odleglosc + 0.3, FluidCollisionMode.NEVER, true, 0.15,
        e -> istota(p, e));
    return hit != null && cel.equals(hit.getHitEntity());
  }
}
