package pl.mcmmorpg.ksiega;

import org.bukkit.event.inventory.InventoryType;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

final class Bron implements Listener {
  private final KsiegaPostaci plugin;
  private final NamespacedKey cloneKey;
  private final NamespacedKey reserveKey;

  Bron(KsiegaPostaci plugin) {
    this.plugin = plugin;
    cloneKey = new NamespacedKey("ksiega", "klon");
    reserveKey = new NamespacedKey(plugin, "zapas");
  }

  boolean isClone(ItemStack item) { return Przedmioty.marked(item, cloneKey); }
  boolean isReserveIcon(ItemStack item) { return Przedmioty.marked(item, reserveKey); }

  boolean weapon(ItemStack item) {
    if (Przedmioty.empty(item)) return true;
    String type = Przedmioty.type(item);
    return type != null && plugin.getConfig().getStringList("typy-broni").contains(type);
  }

  private boolean twoHanded(ItemStack item) {
    if (Przedmioty.empty(item)) return false;
    NBTItem nbt = NBTItem.get(item);
    return nbt.getBoolean("TWO_HANDED") || nbt.getBoolean("MMOITEMS_TWO_HANDED")
        || nbt.getStat("TWO_HANDED") > 0;
  }

  void clearClone(Player player) {
    if (isClone(player.getInventory().getItem(1))) player.getInventory().setItem(1, null);
  }

  void refresh(Player player) {
    if (player.getWorld().getName().equals("creator_limbo")) return;
    PlayerInventory inventory = player.getInventory();
    ItemStack inClone = inventory.getItem(1);
    if (!Przedmioty.empty(inClone) && !isClone(inClone)) Migracja.give(player, inClone);
    ItemStack left = inventory.getItemInOffHand();
    ItemStack clone;
    if (Przedmioty.empty(left)) {
      clone = Przedmioty.icon(plugin.getConfig().getString("ikony.pusta-lewa"),
          plugin.getConfig().getString("nazwy-ikon.pusta-lewa", "§7Lewa ręka"), "§8Pusta", cloneKey);
    } else {
      // Nowy stos kopiuje wyłącznie wygląd; nie przenosi żadnych danych MMOItems.
      clone = new ItemStack(left.getType(), 1);
      ItemMeta source = left.getItemMeta();
      ItemMeta meta = clone.getItemMeta();
      if (source != null) {
        if (source.hasItemModel()) meta.setItemModel(source.getItemModel());
        if (source.hasDisplayName()) meta.setDisplayName(source.getDisplayName());
        if (source.hasItemName()) meta.setItemName(source.getItemName());
      }
      meta.getPersistentDataContainer().set(cloneKey, PersistentDataType.BYTE, (byte) 1);
      clone.setItemMeta(meta);
    }
    inventory.setItem(1, clone);
    for (int slot = 11; slot <= 12; slot++) {
      ItemStack item = inventory.getItem(slot);
      if (!Przedmioty.empty(item) && !isReserveIcon(item) && !weapon(item)) {
        inventory.setItem(slot, null);
        Migracja.give(player, item);
        item = null;
      }
      if (Przedmioty.empty(item)) {
        String path = slot == 11 ? "ikony.zapas-prawa" : "ikony.zapas-lewa";
        inventory.setItem(slot, Przedmioty.icon(plugin.getConfig().getString(path),
            plugin.getConfig().getString(slot == 11 ? "nazwy-ikon.zapas-prawa" : "nazwy-ikon.zapas-lewa"),
            "§8Włóż broń lub przedmiot drugiej ręki.", reserveKey));
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void swap(PlayerSwapHandItemsEvent event) {
    Player player = event.getPlayer();
    if (player.getInventory().getHeldItemSlot() == 1 || isClone(event.getMainHandItem())
        || isClone(event.getOffHandItem())) { event.setCancelled(true); return; }
    if (!player.isSneaking()) {
      Bukkit.getScheduler().runTask(plugin, () -> { if (player.isOnline()) refresh(player); });
      return;
    }
    event.setCancelled(true);
    if (player.getOpenInventory().getType() != InventoryType.CRAFTING) {
      plugin.message(player, "okno"); return;
    }
    PlayerInventory inventory = player.getInventory();
    ItemStack activeRight = inventory.getItem(0), activeLeft = inventory.getItemInOffHand();
    ItemStack spareRight = inventory.getItem(11), spareLeft = inventory.getItem(12);
    if (isReserveIcon(spareRight)) spareRight = null;
    if (isReserveIcon(spareLeft)) spareLeft = null;
    if (!weapon(spareRight) || !weapon(spareLeft)) {
      plugin.message(player, "zly-typ"); return;
    }
    if (!weapon(activeRight) || !weapon(activeLeft)) {
      plugin.message(player, "zly-typ"); return;
    }
    if (twoHanded(spareRight) && !Przedmioty.empty(spareLeft)) {
      plugin.message(player, "dwureczna"); return;
    }
    // Walidacja wszystkich czterech przedmiotów poprzedza pierwszą mutację.
    inventory.setItem(0, spareRight);
    inventory.setItemInOffHand(spareLeft);
    inventory.setItem(11, activeRight);
    inventory.setItem(12, activeLeft);
    refresh(player);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void click(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) return;
    if (event.getHotbarButton() == 1 || event.getClick().isKeyboardClick()
        && event.getHotbarButton() == 1) { event.setCancelled(true); return; }
    if (event.getClickedInventory() == player.getInventory()) {
      int slot = event.getSlot();
      if (slot == 1) { event.setCancelled(true); return; }
      if (slot == 11 || slot == 12) {
        ItemStack incoming = event.getCursor();
        if (!Przedmioty.empty(incoming) && !weapon(incoming)) {
          event.setCancelled(true); plugin.message(player, "zly-typ"); return;
        }
        if (isReserveIcon(event.getCurrentItem())) event.setCurrentItem(null);
      }
    }
    Bukkit.getScheduler().runTask(plugin, () -> { if (player.isOnline()) refresh(player); });
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void drag(InventoryDragEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) return;
    int top = event.getView().getTopInventory().getSize();
    if (event.getRawSlots().stream().anyMatch(raw -> raw >= top &&
        (event.getView().convertSlot(raw) == 1 || event.getView().convertSlot(raw) == 11
            || event.getView().convertSlot(raw) == 12))) event.setCancelled(true);
    Bukkit.getScheduler().runTask(plugin, () -> { if (player.isOnline()) refresh(player); });
  }

  @EventHandler(priority = EventPriority.HIGHEST) public void held(PlayerItemHeldEvent event) {
    Player player = event.getPlayer();
    Bukkit.getScheduler().runTask(plugin, () -> { if (player.isOnline()) refresh(player); });
  }
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void interact(PlayerInteractEvent event) {
    if (event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND
        && isClone(event.getPlayer().getInventory().getItemInMainHand())) event.setCancelled(true);
  }
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void entity(PlayerInteractEntityEvent event) {
    if (isClone(event.getPlayer().getInventory().getItemInMainHand())) event.setCancelled(true);
  }
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void damage(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player player
        && isClone(player.getInventory().getItemInMainHand())) event.setCancelled(true);
  }
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void drop(PlayerDropItemEvent event) {
    if (isClone(event.getItemDrop().getItemStack())) event.setCancelled(true);
  }
  @EventHandler public void death(PlayerDeathEvent event) {
    event.getDrops().removeIf(item -> isClone(item) || isReserveIcon(item));
  }
}
