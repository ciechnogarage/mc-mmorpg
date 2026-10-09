package pl.mcmmorpg.chowance.gra;

import io.lumine.mythic.core.mobs.ActiveMob;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.integracja.Mythic;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Osobnik;

/**
 * Stawianie ciał stworów. Pierwszeństwo: mob MythicMobs z gatunku (KONTRAKT §10.1); gdy go brak —
 * ciało zastępcze vanilla (prototyp, WARN raz na mob). Ciała są NIEPRZETRWAŁE: nigdy nie trafiają
 * na dysk świata, więc restart/crash nie zostawi sierot.
 */
public final class Ciala {
  private final Chowance c;
  private final Set<String> ostrzezone = ConcurrentHashMap.newKeySet();

  public Ciala(Chowance c) { this.c = c; }

  public record Cialo(LivingEntity encja, boolean mythic) {}

  public Cialo postaw(Gatunek g, String mobId, Location l, int poziom) {
    if (Mythic.mobIstnieje(mobId)) {
      ActiveMob am = Mythic.spawn(mobId, l, poziom);
      if (am != null && am.getEntity() != null && am.getEntity().getBukkitEntity() instanceof LivingEntity le && le.isValid()) {
        le.setPersistent(false);
        le.setRemoveWhenFarAway(false);
        return new Cialo(le, true);
      }
      if (am != null && am.getEntity() != null) Mythic.usun(am.getEntity().getBukkitEntity());
      return null;
    }
    if (mobId != null && !mobId.isBlank() && ostrzezone.add(mobId)) {
      c.log.warning("Mob MythicMobs '" + mobId + "' gatunku " + g.id() + " nie istnieje — ciało zastępcze '"
          + g.mobZastepczy() + "' (tylko prototyp; ciała robi Agent B).");
    }
    EntityType typ = typZastepczy(g.mobZastepczy());
    if (typ == null) return null;
    Entity e = l.getWorld().spawnEntity(l, typ, CreatureSpawnEvent.SpawnReason.CUSTOM, x -> {
      x.setPersistent(false);
      if (x instanceof LivingEntity le) le.setRemoveWhenFarAway(false);
    });
    if (!(e instanceof LivingEntity le) || !le.isValid()) {
      if (e != null) e.remove();
      return null;
    }
    return new Cialo(le, false);
  }

  private EntityType typZastepczy(String s) {
    if (s == null || s.isBlank()) s = "WOLF";
    try {
      EntityType t = EntityType.valueOf(s.trim().toUpperCase(Locale.ROOT));
      return t.isAlive() && t.isSpawnable() ? t : EntityType.WOLF;
    } catch (IllegalArgumentException e) {
      return EntityType.WOLF;
    }
  }

  public void ustawZdrowie(LivingEntity e, int max, double hp) {
    AttributeInstance a = e.getAttribute(Attribute.MAX_HEALTH);
    double m = Math.max(1, Math.min(1024, max));
    if (a != null) {
      a.setBaseValue(m);
      m = a.getValue();
    }
    e.setHealth(Math.max(0.5, Math.min(m, hp)));
  }

  public void nazwij(LivingEntity e, Osobnik o, String dopisek) {
    e.customName(Chowance.mm(c.nazwaKolor(o) + " <gray>Poz. " + o.poziom + "</gray>" + (dopisek == null ? "" : " " + dopisek)));
    e.setCustomNameVisible(true);
  }

  public void oznaczOswojony(LivingEntity e, Osobnik o, java.util.UUID wlasciciel) {
    e.getPersistentDataContainer().set(c.kInstancja, PersistentDataType.STRING, o.id);
    e.getPersistentDataContainer().set(c.kWlasciciel, PersistentDataType.STRING, wlasciciel.toString());
  }

  public static String instancja(Chowance c, Entity e) {
    return e == null ? null : e.getPersistentDataContainer().get(c.kInstancja, PersistentDataType.STRING);
  }
}
