package pl.mcmmorpg.zaklecia;

import io.lumine.mythic.bukkit.MythicBukkit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/** Opcjonalne skille MM: sprawdzenie nazwy tylko przy pierwszym użyciu. */
final class Skille {
  private final Zaklecia plugin;
  private final Map<String, Boolean> istnieje = new HashMap<>();

  Skille(Zaklecia plugin) { this.plugin = plugin; }

  private boolean jest(String nazwa) {
    if (nazwa == null || nazwa.isBlank()) return false;
    return istnieje.computeIfAbsent(nazwa.toLowerCase(Locale.ROOT), k ->
        MythicBukkit.inst().getSkillManager().getSkill(nazwa).isPresent());
  }

  void cast(Player p, String nazwa) {
    if (!jest(nazwa)) return;
    try { MythicBukkit.inst().getAPIHelper().castSkill(p, nazwa); }
    catch (Throwable t) { plugin.getLogger().log(Level.WARNING, "skill " + nazwa + ": " + t, t); }
  }

  void cele(Player p, String nazwa, List<Entity> cele) {
    if (!jest(nazwa) || cele.isEmpty()) return;
    try { MythicBukkit.inst().getAPIHelper().castSkill(p, nazwa, p, p.getLocation(), cele, List.of(), 1f); }
    catch (Throwable t) { plugin.getLogger().log(Level.WARNING, "trafienia " + nazwa + ": " + t, t); }
  }

  void punkt(Player p, String nazwa, Location gdzie) {
    if (!jest(nazwa)) return;
    try { MythicBukkit.inst().getAPIHelper().castSkill(p, nazwa, gdzie); }
    catch (Throwable t) { plugin.getLogger().log(Level.WARNING, "uderzenie " + nazwa + ": " + t, t); }
  }
}
