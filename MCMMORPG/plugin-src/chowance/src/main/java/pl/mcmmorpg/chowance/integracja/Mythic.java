package pl.mcmmorpg.chowance.integracja;

import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

/** Cienka warstwa nad API MythicMobs 5.11.2 (sygnatury sprawdzone javap). */
public final class Mythic {
  private Mythic() {}

  public static boolean mobIstnieje(String id) {
    if (id == null || id.isBlank()) return false;
    return MythicBukkit.inst().getMobManager().getMythicMob(id).isPresent();
  }

  public static boolean skillIstnieje(String id) {
    if (id == null || id.isBlank()) return false;
    return MythicBukkit.inst().getSkillManager().getSkill(id).isPresent();
  }

  public static ActiveMob spawn(String id, Location l, int poziom) {
    return MythicBukkit.inst().getMobManager().spawnMob(id, l, poziom);
  }

  public static ActiveMob aktywny(Entity e) {
    if (e == null) return null;
    return MythicBukkit.inst().getMobManager().getActiveMob(e.getUniqueId()).orElse(null);
  }

  public static String typ(Entity e) {
    ActiveMob am = aktywny(e);
    return am == null ? null : am.getMobType();
  }

  public static void sygnal(Entity e, String sygnal, Entity trigger) {
    ActiveMob am = aktywny(e);
    if (am == null) return;
    am.signalMob(trigger == null ? null : BukkitAdapter.adapt(trigger), sygnal);
  }

  /** Odegranie skilla ruchu: caster = ciało atakującego, cel = ciało przeciwnika. */
  public static boolean skill(Entity caster, String skill, Entity cel) {
    if (caster == null || skill == null || skill.isBlank()) return false;
    try {
      return MythicBukkit.inst().getAPIHelper().castSkill(caster, skill, caster, caster.getLocation(),
          cel == null ? List.of() : List.of(cel), List.of(), 1f);
    } catch (RuntimeException e) {
      return false;
    }
  }

  public static void usun(Entity e) {
    ActiveMob am = aktywny(e);
    if (am != null) {
      am.setDespawned();
      am.remove();
    }
    if (e.isValid()) e.remove();
  }
}
