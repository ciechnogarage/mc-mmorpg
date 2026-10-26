package pl.mcmmorpg.klasybron;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.targeters.IEntityTargeter;
import io.lumine.mythic.bukkit.BukkitAdapter;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;

/**
 * Targeter {@code @Celownik{r=20;s=0.3}}: istota pod celownikiem gracza, liczona promieniem z oczu
 * (World.rayTrace: bloki zaslaniaja, bez przenikliwych; hitbox celu powiekszony o s).
 * Pomija rzucajacego i sojusznikow (Warunki.sojusznik), martwe, widzow, nie-LivingEntity.
 *
 * Zamiast MM {@code @Target}: MythicUtil.getTargetedEntity (MM 5.11.2) idzie po blokach linii wzroku i dla
 * KAZDEGO bloku zwraca pierwsza istote z getNearbyEntities (kolejnosc dowolna), ktorej stopy leza w pudle
 * blok -0.75..+1.75 / y -1..+2.5. Juz pierwsze bloki przy oczach lapia sluge stojacego obok gracza, wiec
 * Rozkaz trafial wlasnego straznika (celWrogi odrzuca) zamiast zajaca - wynik zalezal od kolejnosci listy.
 * Caster-mob: jego cel (jak @Target).
 */
final class Celownik implements IEntityTargeter {

  private final Plugin plugin;
  private final double zasieg;
  private final double margines;

  Celownik(Plugin plugin, MythicLineConfig cfg) {
    this.plugin = plugin;
    this.zasieg = Math.max(1, Math.min(64, cfg.getDouble(new String[] {"range", "r"}, 20)));
    this.margines = Math.max(0, Math.min(1, cfg.getDouble(new String[] {"size", "s"}, 0.3)));
  }

  @Override
  public Collection<AbstractEntity> getEntities(SkillMetadata data) {
    Entity c = data.getCaster().getEntity().getBukkitEntity();
    LivingEntity cel;
    if (c instanceof Player gracz) {
      cel = Bukkit.isPrimaryThread() ? namierz(gracz) : naGlownym(gracz);
    } else if (c instanceof org.bukkit.entity.Mob mob) {
      cel = mob.getTarget();
    } else {
      cel = null;
    }
    return cel == null ? List.of() : List.of(BukkitAdapter.adapt(cel));
  }

  private LivingEntity naGlownym(Player gracz) {
    try {
      return Bukkit.getScheduler().callSyncMethod(plugin, () -> namierz(gracz)).get(1, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return null;
    } catch (Exception ex) {
      plugin.getLogger().log(Level.WARNING, "@Celownik: namierzanie poza watkiem glownym nieudane", ex);
      return null;
    }
  }

  LivingEntity namierz(Player gracz) {
    Location oko = gracz.getEyeLocation();
    RayTraceResult r = gracz.getWorld().rayTrace(oko, oko.getDirection(), zasieg, FluidCollisionMode.NEVER,
        true, margines, e -> e instanceof LivingEntity le && !le.isDead() && !(e instanceof org.bukkit.entity.ArmorStand)
            && !(e instanceof Player p && p.getGameMode() == GameMode.SPECTATOR)
            && !Warunki.sojusznik(gracz, e));
    return r != null && r.getHitEntity() instanceof LivingEntity le ? le : null;
  }
}
