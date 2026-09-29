package pl.mcmmorpg.zaklecia;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.ThreadSafetyLevel;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import java.util.Locale;
import java.util.logging.Level;
import io.lumine.mythic.lib.player.resource.ResourceUpdateReason;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Ciche zasoby MMOCore dla zaklec koncentracji (MythicMobs/Crucible).
 *
 * Po co: `mmocore admin resource-mana take` zawsze odpisuje do nadawcy ("X now has Y mana points"),
 * a przy konsoli to linia w logu serwera co tik rzucania (2/s na gracza). Zaden zainstalowany plugin
 * (MMOCore, MythicLib, MMOItems) nie daje MythicMobs mechaniki many — stad ten plugin.
 *
 * - mechanika {@code zakZasob{zasob=mana|stamina;a=2}} — zabiera zasob celowi (gracz), cicho;
 * - warunek {@code zakMaZasob{zasob=mana;a=2}} — czy cel (gracz) ma co najmniej {@code a}.
 *
 * Rejestracja przez Mythic*LoadEvent (wzor: github.com/xikage/MythicMobs-API-Examples).
 */
public final class Zaklecia extends JavaPlugin implements Listener {

  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(this, this);
  }

  private static String zasob(MythicLineConfig cfg) {
    String z = cfg.getString(new String[] {"zasob", "z"}, "mana").toLowerCase(Locale.ROOT);
    if (!z.equals("mana") && !z.equals("stamina")) throw new IllegalArgumentException("zasob=" + z);
    return z;
  }

  private static double stan(PlayerData d, String zasob) {
    return zasob.equals("mana") ? d.getMana() : d.getStamina();
  }

  @EventHandler
  public void mechaniki(MythicMechanicLoadEvent e) {
    if (!e.getMechanicName().equalsIgnoreCase("zakZasob")) return;
    MythicLineConfig cfg = e.getConfig();
    String zasob = zasob(cfg);
    double ile = cfg.getDouble(new String[] {"amount", "a"}, 1);
    e.register(new ITargetedEntitySkill() {
      @Override
      public ThreadSafetyLevel getThreadSafetyLevel() {
        return ThreadSafetyLevel.SYNC_ONLY; // PlayerResourceUpdateEvent jest zdarzeniem Bukkit
      }

      @Override
      public SkillResult castAtEntity(SkillMetadata data, AbstractEntity cel) {
        try {
          if (!(cel.getBukkitEntity() instanceof Player p)) return SkillResult.INVALID_TARGET;
          PlayerData d = PlayerData.get(p);
          if (zasob.equals("mana")) d.giveMana(-ile, ResourceUpdateReason.SKILL);
          else d.giveStamina(-ile, ResourceUpdateReason.SKILL);
          return SkillResult.SUCCESS;
        } catch (Throwable t) { // MM polyka wyjatki mechanik bez sladu — logujemy sami
          getLogger().log(Level.WARNING, "zakZasob: " + t, t);
          return SkillResult.ERROR;
        }
      }
    });
  }

  @EventHandler
  public void warunki(MythicConditionLoadEvent e) {
    if (!e.getConditionName().equalsIgnoreCase("zakMaZasob")) return;
    MythicLineConfig cfg = e.getConfig();
    String zasob = zasob(cfg);
    double ile = cfg.getDouble(new String[] {"amount", "a"}, 1);
    e.register((IEntityCondition) cel -> {
      if (!(cel.getBukkitEntity() instanceof Player p)) return false;
      return stan(PlayerData.get(p), zasob) >= ile;
    });
  }
}
