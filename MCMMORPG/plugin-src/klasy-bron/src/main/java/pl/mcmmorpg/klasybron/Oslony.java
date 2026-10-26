package pl.mcmmorpg.klasybron;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Zdejmuje osierocone oslony skilli (MM {@code shield{}} = vanilla absorpcja bez czasu trwania).
 *
 * <p>Skille robia shield{+X} -> delay -> shield{-X}; wyjscie przed delay zostawialo absorpcje na stale.
 * MM 5.11.2 (VolatileEntityHandlerImpl v1_21_R7.setEntityAbsorptionHearts) przy tym PODNOSI wartosc bazowa
 * atrybutu max_absorption i nigdy jej nie obniza - to tez zostaje w danych gracza. Przy wyjsciu, wejsciu (+1 tick)
 * i zmianie postaci MMOProfiles: baza max_absorption wraca do domyslnej, a absorpcja do min(obecna, efekt ABSORPTION).
 */
final class Oslony implements Listener {

  private final Plugin plugin;

  Oslony(Plugin plugin) {
    this.plugin = plugin;
  }

  /** Absorpcja dawana przez aktywny efekt ABSORPTION (vanilla: 4 HP na poziom, poziom = amplifier+1). */
  static double zEfektu(Player p) {
    PotionEffect ef = p.getPotionEffect(PotionEffectType.ABSORPTION);
    return ef == null ? 0 : 4.0 * (ef.getAmplifier() + 1);
  }

  static void zdejmij(Player p) {
    AttributeInstance max = p.getAttribute(Attribute.MAX_ABSORPTION);
    if (max != null && max.getBaseValue() != max.getDefaultValue()) max.setBaseValue(max.getDefaultValue());
    double cap = zEfektu(p);
    if (p.getAbsorptionAmount() > cap) p.setAbsorptionAmount(cap);
  }

  void zdejmijZaTick(Player p) {
    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
      if (p.isOnline()) zdejmij(p);
    }, 1L);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void wyjscie(PlayerQuitEvent e) {
    zdejmij(e.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void wejscie(PlayerJoinEvent e) {
    zdejmijZaTick(e.getPlayer());
  }
}
