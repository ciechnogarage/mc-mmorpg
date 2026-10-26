package pl.mcmmorpg.klasybron;

import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent;
import io.lumine.mythic.bukkit.events.MythicTargeterLoadEvent;
import io.lumine.mythic.lib.api.item.NBTItem;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Warunek MythicMobs {@code bronRasowa{typ=TOME}} (kilka typow po przecinku): prawda, gdy cel-gracz trzyma
 * w glownej LUB drugiej rece przedmiot MMOItems tego typu. Sluzy premii sygnatur klas rasowych.
 *
 * <p>Zastepuje placeholder Crucible {@code <caster.item.hand.nbt.MMOITEMS_ITEM_TYPE>}, ktory po restarcie
 * serwera jest "Invalid Placeholder" (Crucible rejestruje go po wczytaniu skilli; dziala dopiero po mm reload).
 *
 * <p>Do tego warunki {@code druzyna{}} i {@code celWrogi{}} ({@link Warunki}) oraz listener osieroconych oslon
 * ({@link Oslony}). Opis: README.md.
 */
public final class KlasyBron extends JavaPlugin implements Listener {

  private Warunki warunki;

  @Override
  public void onEnable() {
    warunki = new Warunki(this);
    Oslony oslony = new Oslony(this);
    getServer().getPluginManager().registerEvents(this, this);
    getServer().getPluginManager().registerEvents(oslony, this);
    if (getServer().getPluginManager().isPluginEnabled("MMOProfiles")) {
      getServer().getPluginManager().registerEvents(new OslonyProfile(oslony), this);
    } else {
      getLogger().info("Brak MMOProfiles - oslony zdejmowane tylko przy wyjsciu/wejsciu.");
    }
  }

  @Override
  public void onDisable() {
    // Wylaczenie/restart serwera: MM nie dokonczy delay w skillach, wiec zdejmujemy oslony jak przy wyjsciu.
    for (Player p : getServer().getOnlinePlayers()) Oslony.zdejmij(p);
  }

  @EventHandler
  public void targeter(MythicTargeterLoadEvent e) {
    if (e.getTargeterName().equalsIgnoreCase("celownik")) e.register(new Celownik(this, e.getConfig()));
  }

  @EventHandler
  public void warunek(MythicConditionLoadEvent e) {
    String nazwa = e.getConditionName();
    if (nazwa.equalsIgnoreCase("druzyna")) {
      e.register(warunki.druzyna());
      return;
    }
    if (nazwa.equalsIgnoreCase("celWrogi")) {
      e.register(warunki.celWrogi());
      return;
    }
    if (!nazwa.equalsIgnoreCase("bronRasowa")) return;
    MythicLineConfig cfg = e.getConfig();
    Set<String> typy = Arrays.stream(cfg.getString(new String[] {"typ", "t"}, "").split(","))
        .map(s -> s.trim().toUpperCase(Locale.ROOT)).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
    e.register((IEntityCondition) cel -> cel.getBukkitEntity() instanceof Player p
        && (maTyp(p.getInventory().getItemInMainHand(), typy) || maTyp(p.getInventory().getItemInOffHand(), typy)));
  }

  static boolean maTyp(ItemStack item, Set<String> typy) {
    if (item == null || item.getType().isAir() || typy.isEmpty()) return false;
    String typ = NBTItem.get(item).getString("MMOITEMS_ITEM_TYPE");
    return typ != null && typy.contains(typ.toUpperCase(Locale.ROOT));
  }
}
