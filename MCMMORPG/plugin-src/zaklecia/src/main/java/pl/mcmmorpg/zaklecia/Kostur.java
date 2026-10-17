package pl.mcmmorpg.zaklecia;

import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.api.stat.modifier.StatModifier;
import io.lumine.mythic.lib.player.modifier.ModifierType;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Kostur = ognisko zaklec (wlasciciel 13.10, wariant A). Kostur jest dwureczny (ksiega-postaci), wiec w dloni zostaje
 * jedno zaklecie - w lewej, rzucane PPM; LPM to atak kosturem. W zamian kostur wzmacnia kazde zaklecie: dopoki jest
 * w prawej rece, gracz ma premie do statystyki MAGIC_DAMAGE (zaklecia bija mmodamage t=SKILL,MAGIC, wiec MythicLib
 * dolicza ja sam - tak jak magic-damage samego kostura).
 */
final class Kostur implements Listener {
  private static final String KLUCZ = "zaklecia.kostur";
  private static final org.bukkit.NamespacedKey NOSNIK = new org.bukkit.NamespacedKey("zaklecia", "nosnik");
  private final Zaklecia plugin;
  private final Map<UUID, StatModifier> aktywne = new HashMap<>();

  Kostur(Zaklecia plugin) {
    this.plugin = plugin;
    plugin.getServer().getScheduler().runTaskTimer(plugin,
        () -> plugin.getServer().getOnlinePlayers().forEach(this::odswiez), 20L, 10L);
  }

  boolean kostur(ItemStack item) {
    if (item == null || item.getType().isAir()) return false;
    String typ = NBTItem.get(item).getString("MMOITEMS_ITEM_TYPE");
    List<String> typy = plugin.getConfig().getStringList("kostur.typy");
    if (typy.isEmpty()) typy = List.of("STAFF", "GREATSTAFF");
    return typ != null && typy.contains(typ.toUpperCase(Locale.ROOT));
  }

  double premia() { return plugin.getConfig().getDouble("kostur.premia-magii", 50); }

  void odswiez(Player p) {
    // stare nosniki (sprzed 13.10) bez flagi MMOITEMS_HANDWORN - podmien, zeby MMOItems nie karal "zajetymi rekami"
    ItemStack lewa = p.getInventory().getItemInOffHand();
    if (lewa.hasItemMeta() && lewa.getItemMeta().getPersistentDataContainer().has(NOSNIK)) {
      ItemStack nowa = Nosnik.naDloni(lewa);
      if (nowa != lewa) p.getInventory().setItemInOffHand(nowa);
    }
    boolean ma = premia() > 0 && kostur(p.getInventory().getItemInMainHand());
    StatModifier m = aktywne.get(p.getUniqueId());
    if (ma && m == null) {
      m = new StatModifier(KLUCZ, "MAGIC_DAMAGE", premia(), ModifierType.FLAT);
      m.register(MMOPlayerData.get(p));
      aktywne.put(p.getUniqueId(), m);
      MMOPlayerData.get(p).getActionBar().show(20, 50L,
          "§x§E§8§D§5§9§6Kostur skupia zaklęcie §7- §fobrażenia magiczne +" + (int) premia() + "%");
    } else if (!ma && m != null) {
      m.unregister(MMOPlayerData.get(p));
      aktywne.remove(p.getUniqueId());
    }
  }

  void wylacz() {
    for (Player p : plugin.getServer().getOnlinePlayers()) {
      StatModifier m = aktywne.remove(p.getUniqueId());
      if (m != null) m.unregister(MMOPlayerData.get(p));
    }
  }

  @EventHandler
  public void wyjscie(PlayerQuitEvent e) {
    StatModifier m = aktywne.remove(e.getPlayer().getUniqueId());
    if (m != null) m.unregister(MMOPlayerData.get(e.getPlayer()));
  }
}
