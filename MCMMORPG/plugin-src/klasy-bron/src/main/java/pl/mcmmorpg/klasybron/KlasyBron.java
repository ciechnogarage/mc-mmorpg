package pl.mcmmorpg.klasybron;

import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent;
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
 */
public final class KlasyBron extends JavaPlugin implements Listener {

  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(this, this);
  }

  @EventHandler
  public void warunek(MythicConditionLoadEvent e) {
    if (!e.getConditionName().equalsIgnoreCase("bronRasowa")) return;
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
