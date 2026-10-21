package pl.mcmmorpg.ksiega;

import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

final class Sakwa implements Listener {
  private final KsiegaPostaci plugin;
  Sakwa(KsiegaPostaci plugin) { this.plugin = plugin; }

  private ItemStack icon() { return MMOItems.plugin.getItem("MISCELLANEOUS", "UI_SAKWA"); }

  void show(Player player) {
    if (player.getOpenInventory().getType() != InventoryType.CRAFTING) return;
    ItemStack item = icon();
    if (!Przedmioty.empty(item)) player.getOpenInventory().getTopInventory().setItem(0, item);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void prepare(PrepareItemCraftEvent event) {
    if (event.getView().getType() == InventoryType.CRAFTING) event.getInventory().setResult(icon());
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void click(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)
        || event.getView().getType() != InventoryType.CRAFTING || event.getRawSlot() != 0) return;
    event.setCancelled(true);
    if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) return;
    Bukkit.getScheduler().runTask(plugin, () -> {
      if (player.isOnline()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
          "core-playervault sakwa " + player.getName());
    });
  }
}
