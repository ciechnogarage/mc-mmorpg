package pl.mcmmorpg.zaklecia;

import io.lumine.mythic.bukkit.MythicBukkit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/** Skille MM: wynik odswiezany po reloadzie, bez trwalego cache braku. */
final class Skille {
  private final Zaklecia plugin;
  private final Map<String, Boolean> istnieje = new HashMap<>();
  private final Map<String, Long> sprawdzono = new HashMap<>();
  private final Set<String> ostrzezone = new HashSet<>();
  private final String wymagany;

  Skille(Zaklecia plugin, String wymagany) { this.plugin = plugin; this.wymagany = wymagany; }

  private boolean jest(String nazwa) {
    if (nazwa == null || nazwa.isBlank()) return false;
    String klucz = nazwa.toLowerCase(Locale.ROOT);
    long teraz = System.currentTimeMillis();
    if (teraz - sprawdzono.getOrDefault(klucz, 0L) < 30000) return istnieje.getOrDefault(klucz, false);
    boolean jest = MythicBukkit.inst().getSkillManager().getSkill(nazwa).isPresent();
    sprawdzono.put(klucz, teraz);
    istnieje.put(klucz, jest);
    if (!jest && nazwa.equalsIgnoreCase(wymagany) && ostrzezone.add(klucz))
      plugin.getLogger().warning("Brak wymaganego skilla MythicMobs: " + nazwa);
    return jest;
  }

  boolean cast(Player p, String nazwa) {
    if (!jest(nazwa)) return false;
    try { return MythicBukkit.inst().getAPIHelper().castSkill(p, nazwa); }
    catch (Throwable t) { plugin.getLogger().log(Level.WARNING, "skill " + nazwa + ": " + t, t); return false; }
  }

  boolean cele(Player p, String nazwa, List<Entity> cele) {
    boolean obecny = jest(nazwa);
    if (plugin.getConfig().getBoolean("debug", false)) plugin.getLogger().info("ZAKDBG skill=" + nazwa
        + " obecny=" + obecny + " cele=" + cele.stream().map(e -> e.getUniqueId().toString()).toList());
    if (!obecny || cele.isEmpty()) return false;
    try { return MythicBukkit.inst().getAPIHelper().castSkill(p, nazwa, p, p.getLocation(), cele, List.of(), 1f); }
    catch (Throwable t) { plugin.getLogger().log(Level.WARNING, "trafienia " + nazwa + ": " + t, t); return false; }
  }

  boolean punkt(Player p, String nazwa, Location gdzie) {
    if (!jest(nazwa)) return false;
    try { return MythicBukkit.inst().getAPIHelper().castSkill(p, nazwa, gdzie); }
    catch (Throwable t) { plugin.getLogger().log(Level.WARNING, "uderzenie " + nazwa + ": " + t, t); return false; }
  }
}
