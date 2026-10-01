package pl.mcmmorpg.ksiega;

import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.player.EquipmentSlot;
import java.util.Optional;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.inventory.EquippedItem;
import net.Indyuce.mmoitems.inventory.InventoryResolver;
import net.Indyuce.mmoitems.inventory.InventorySupplier;
import net.Indyuce.mmoitems.inventory.InventoryWatcher;
import net.Indyuce.mmoitems.inventory.ItemUpdate;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

final class Bizuteria implements Listener {
  static final String[] TYPES = {"NECKLACE", "RING", "RING", "CHARM"};
  private static final int INVENTORY_ID = 217;
  private final KsiegaPostaci plugin;
  private final NamespacedKey iconKey;
  private final InventorySupplier supplier = resolver -> new Watcher(resolver);

  Bizuteria(KsiegaPostaci plugin) {
    this.plugin = plugin;
    iconKey = new NamespacedKey(plugin, "ikona_bizuterii");
  }

  InventorySupplier supplier() { return supplier; }

  boolean usable(Player player, ItemStack item, int index) {
    if (Przedmioty.empty(item) || item.getAmount() != 1 || !TYPES[index].equals(Przedmioty.type(item))) return false;
    PlayerData data = PlayerData.getOrNull(player);
    return data != null && new EquippedItem(INVENTORY_ID, EquipmentSlot.ACCESSORY, index,
        NBTItem.get(item)).isUsable(data.getRPG());
  }

  void changed(Player player, int index) {
    PlayerData data = PlayerData.getOrNull(player);
    if (data == null) return;
    data.getInventory().watch(Watcher.class, w -> w.watchSlot(index));
    data.resolveModifiersLater();
  }

  void show(Player player) {
    if (player.getOpenInventory().getType() != InventoryType.CRAFTING) return;
    Dane.Record record = plugin.dane.current(player);
    if (record == null) return;
    Inventory top = player.getOpenInventory().getTopInventory();
    for (int i = 0; i < 4; i++) {
      ItemStack item = record.jewelry[i];
      if (Przedmioty.empty(item)) {
        String type = Przedmioty.lower(TYPES[i]);
        String nameKey = i == 0 ? "necklace" : i == 3 ? "charm" : "ring-" + i;
        String name = plugin.getConfig().getString("nazwy-ikon." + nameKey, TYPES[i]);
        top.setItem(i + 1, Przedmioty.icon(plugin.getConfig().getString("ikony." + type),
            name, "§7Kliknij przedmiotem, aby założyć.", iconKey));
      } else top.setItem(i + 1, item.clone());
    }
  }

  void clearView(Player player) {
    if (player.getOpenInventory().getType() != InventoryType.CRAFTING) return;
    Inventory top = player.getOpenInventory().getTopInventory();
    for (int i = 1; i <= 4; i++) top.setItem(i, null);
    top.setItem(0, null);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void click(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)
        || event.getView().getType() != InventoryType.CRAFTING) return;
    if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) { event.setCancelled(true); return; }
    int raw = event.getRawSlot();
    if (raw >= 1 && raw <= 4 && event.getSlotType() == InventoryType.SlotType.CRAFTING) {
      event.setCancelled(true);
      if (event.getClick().isShiftClick() || event.getClick().isKeyboardClick()
          || event.getClick().isCreativeAction() || !event.getClick().isLeftClick()) return;
      Dane.Record record = plugin.dane.current(player);
      if (record == null) return;
      int i = raw - 1;
      ItemStack cursor = event.getCursor();
      ItemStack old = record.jewelry[i];
      if (Przedmioty.empty(cursor) && Przedmioty.empty(old)) return;
      if (!Przedmioty.empty(cursor)) {
        if (!TYPES[i].equals(Przedmioty.type(cursor))) { plugin.message(player, "zly-typ"); return; }
        if (!usable(player, cursor, i)) { plugin.message(player, "wymagania"); return; }
      }
      record.jewelry[i] = Przedmioty.empty(cursor) ? null : cursor.clone();
      if (!plugin.dane.save(record)) {
        record.jewelry[i] = old;
        plugin.message(player, "migracja-blad");
        return;
      }
      player.setItemOnCursor(Przedmioty.empty(old) ? null : old.clone());
      show(player);
      changed(player, i);
      return;
    }
    if (raw < event.getView().getTopInventory().getSize() || !event.isShiftClick()) return;
    ItemStack from = event.getCurrentItem();
    if (Przedmioty.empty(from)) return;
    Dane.Record record = plugin.dane.current(player);
    if (record == null) return;
    for (int i = 0; i < 4; i++) {
      if (TYPES[i].equals(Przedmioty.type(from)) && Przedmioty.empty(record.jewelry[i])) {
        event.setCancelled(true);
        if (!usable(player, from, i)) { plugin.message(player, "wymagania"); return; }
        record.jewelry[i] = from.clone();
        if (!plugin.dane.save(record)) {
          record.jewelry[i] = null;
          plugin.message(player, "migracja-blad");
          return;
        }
        event.setCurrentItem(null);
        show(player);
        changed(player, i);
        return;
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void drag(InventoryDragEvent event) {
    if (event.getView().getType() == InventoryType.CRAFTING
        && event.getRawSlots().stream().anyMatch(slot -> slot >= 0 && slot <= 4)) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void close(InventoryCloseEvent event) {
    if (event.getView().getType() != InventoryType.CRAFTING) return;
    Player player = (Player) event.getPlayer();
    // Vanilla oddaje zawartość siatki przy zamknięciu; usuwamy widok przed tym krokiem.
    Inventory top = event.getView().getTopInventory();
    for (int i = 0; i <= 4; i++) top.setItem(i, null);
    Bukkit.getScheduler().runTask(plugin, () -> {
      if (player.isOnline() && player.getOpenInventory().getType() == InventoryType.CRAFTING) {
        show(player);
        plugin.sakwa.show(player);
      }
    });
  }

  private final class Watcher extends InventoryWatcher {
    private final InventoryResolver resolver;
    private final EquippedItem[] equipped = new EquippedItem[4];
    Watcher(InventoryResolver resolver) { this.resolver = resolver; }

    ItemUpdate watchSlot(int i) {
      Player player = resolver.getPlayerData().getRPG().getPlayer();
      Dane.Record record = plugin.dane.current(player);
      ItemStack item = record == null ? null : record.jewelry[i];
      ItemUpdate update = checkForUpdate(item, equipped[i], EquipmentSlot.ACCESSORY, i, INVENTORY_ID);
      if (update != null) equipped[i] = update.getNew();
      return update;
    }

    @Override public ItemUpdate watchInventory(int slot, Optional<ItemStack> item) { return null; }
    @Override public void watchAll(java.util.function.Consumer<ItemUpdate> consumer) {
      for (int i = 0; i < 4; i++) {
        ItemUpdate update = watchSlot(i);
        if (update != null) consumer.accept(update);
      }
    }
  }
}
