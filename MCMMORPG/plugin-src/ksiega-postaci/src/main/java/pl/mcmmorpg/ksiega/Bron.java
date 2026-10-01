package pl.mcmmorpg.ksiega;

import io.lumine.mythic.lib.api.item.NBTItem;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

final class Bron implements Listener {
  private static final int RIGHT = 0, LEFT_IMAGE = 1, RIGHT_SPARE = 11;
  private static final int LEFT_SPARE = 12, RIGHT_IMAGE = 13, LEFT = 40;
  private final KsiegaPostaci plugin;
  private final NamespacedKey cloneKey;
  private final NamespacedKey imageKey;
  private final NamespacedKey reserveKey;

  Bron(KsiegaPostaci plugin) {
    this.plugin = plugin;
    cloneKey = new NamespacedKey("ksiega", "klon");
    imageKey = new NamespacedKey("ksiega", "obraz");
    reserveKey = new NamespacedKey(plugin, "zapas");
  }

  boolean isClone(ItemStack item) { return Przedmioty.marked(item, cloneKey); }
  boolean isImage(ItemStack item) { return isClone(item) || Przedmioty.marked(item, imageKey); }
  boolean isReserveIcon(ItemStack item) { return Przedmioty.marked(item, reserveKey); }
  private boolean virtual(ItemStack item) { return isImage(item) || isReserveIcon(item); }

  boolean weapon(ItemStack item) {
    if (Przedmioty.empty(item)) return true;
    if (virtual(item)) return false;
    String type = Przedmioty.type(item);
    return type != null && plugin.getConfig().getStringList("typy-broni").contains(type);
  }

  private boolean twoHanded(ItemStack item) {
    if (Przedmioty.empty(item)) return false;
    NBTItem nbt = NBTItem.get(item);
    return nbt.getBoolean("TWO_HANDED") || nbt.getBoolean("MMOITEMS_TWO_HANDED")
        || nbt.getStat("TWO_HANDED") > 0;
  }

  private boolean validPair(Player player, ItemStack right, ItemStack left) {
    if (!weapon(right) || !weapon(left)) {
      plugin.message(player, "zly-typ"); return false;
    }
    if (twoHanded(right) && !Przedmioty.empty(left)) {
      plugin.message(player, "dwureczna"); return false;
    }
    return true;
  }

  void clearClone(Player player) {
    PlayerInventory inventory = player.getInventory();
    if (isImage(inventory.getItem(LEFT_IMAGE))) inventory.setItem(LEFT_IMAGE, null);
    if (isImage(inventory.getItem(RIGHT_IMAGE))) inventory.setItem(RIGHT_IMAGE, null);
  }

  private String iconName(String side) {
    String configured = plugin.getConfig().getString("nazwy-ikon." + side);
    return switch (side) {
      case "pusta-prawa" -> configured == null ? "§7Prawa ręka – aktywna" : configured;
      case "pusta-lewa" -> configured == null || configured.equals("§7Lewa ręka")
          ? "§7Lewa ręka – aktywna" : configured;
      case "zapas-prawa" -> configured == null || configured.equals("§7Zapasowa prawa")
          ? "§7Prawa ręka – zapas" : configured;
      case "zapas-lewa" -> configured == null || configured.equals("§7Zapasowa lewa")
          ? "§7Lewa ręka – zapas" : configured;
      default -> configured;
    };
  }

  private ItemStack image(ItemStack source, boolean right) {
    NamespacedKey key = right ? imageKey : cloneKey;
    if (Przedmioty.empty(source)) {
      String side = right ? "pusta-prawa" : "pusta-lewa";
      return Przedmioty.icon(plugin.getConfig().getString("ikony." + side,
              "nexo:mmo_icon_gloves"),
          iconName(side), "§8Puste", key);
    }
    // Nowy stos kopiuje tylko wygląd, nigdy NBT/PDC MMOItems.
    ItemStack result = new ItemStack(source.getType(), 1);
    ItemMeta sourceMeta = source.getItemMeta();
    ItemMeta meta = result.getItemMeta();
    if (sourceMeta != null) {
      if (sourceMeta.hasItemModel()) meta.setItemModel(sourceMeta.getItemModel());
      if (sourceMeta.hasDisplayName()) meta.setDisplayName(sourceMeta.getDisplayName());
      else if (sourceMeta.hasItemName()) meta.setDisplayName(sourceMeta.getItemName());
    }
    meta.setLore(List.of(right ? "§6W ręce (prawa)" : "§6W ręce (lewa)"));
    meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
    result.setItemMeta(meta);
    return result;
  }

  private void recover(Player player, int slot) {
    PlayerInventory inventory = player.getInventory();
    ItemStack item = inventory.getItem(slot);
    if (Przedmioty.empty(item) || virtual(item)) return;
    inventory.setItem(slot, null);
    // Dawne zaślepki nie są przedmiotami gracza.
    if (Przedmioty.placeholder(item) && Przedmioty.type(item) == null
        && Przedmioty.id(item) == null) return;
    Migracja.give(player, item);
  }

  void refresh(Player player) {
    if (player.getWorld().getName().equals("creator_limbo")) return;
    PlayerInventory inventory = player.getInventory();
    recover(player, LEFT_IMAGE);
    recover(player, RIGHT_IMAGE);
    for (int slot = 0; slot < 36; slot++) {
      if (slot == LEFT_IMAGE || slot == RIGHT_IMAGE) continue;
      if (isImage(inventory.getItem(slot))) inventory.setItem(slot, null);
    }
    if (isImage(inventory.getItemInOffHand())) inventory.setItemInOffHand(null);
    for (int slot : new int[] {RIGHT, RIGHT_SPARE, LEFT_SPARE, LEFT}) {
      ItemStack item = inventory.getItem(slot);
      if ((slot == RIGHT_SPARE || slot == LEFT_SPARE) && isReserveIcon(item)) {
        inventory.setItem(slot, null); item = null;
      }
      if (!Przedmioty.empty(item) && !weapon(item)) {
        inventory.setItem(slot, null);
        if (!virtual(item)) Migracja.give(player, item);
      }
    }
    if (twoHanded(inventory.getItem(RIGHT)) && !Przedmioty.empty(inventory.getItemInOffHand())) {
      ItemStack left = inventory.getItemInOffHand();
      inventory.setItemInOffHand(null);
      Migracja.give(player, left);
    }
    inventory.setItem(LEFT_IMAGE, image(inventory.getItemInOffHand(), false));
    inventory.setItem(RIGHT_IMAGE, image(inventory.getItem(RIGHT), true));
    for (int slot : new int[] {RIGHT_SPARE, LEFT_SPARE}) {
      if (!Przedmioty.empty(inventory.getItem(slot))) continue;
      String side = slot == RIGHT_SPARE ? "zapas-prawa" : "zapas-lewa";
      inventory.setItem(slot, Przedmioty.icon(plugin.getConfig().getString("ikony." + side),
          iconName(side), "§8Puste", reserveKey));
    }
  }

  private void refreshLater(Player player) {
    Bukkit.getScheduler().runTask(plugin, () -> { if (player.isOnline()) refresh(player); });
  }

  private ItemStack real(PlayerInventory inventory, int slot) {
    ItemStack item = inventory.getItem(slot);
    return virtual(item) ? null : item;
  }

  private boolean allowed(Player player, int slot, ItemStack incoming) {
    if (!weapon(incoming)) { plugin.message(player, "zly-typ"); return false; }
    PlayerInventory inventory = player.getInventory();
    if (slot == RIGHT) return validPair(player, incoming, real(inventory, LEFT));
    if (slot == LEFT) return validPair(player, real(inventory, RIGHT), incoming);
    return true;
  }

  private void clickWeapon(Player player, int slot, ItemStack cursor) {
    if (!allowed(player, slot, cursor)) return;
    PlayerInventory inventory = player.getInventory();
    ItemStack old = real(inventory, slot);
    inventory.setItem(slot, Przedmioty.empty(cursor) ? null : cursor);
    player.setItemOnCursor(Przedmioty.empty(old) ? null : old);
    refreshLater(player);
  }

  private void hotbarSwap(Player player, int slot, int button) {
    if (button == LEFT_IMAGE || button < 0 || button > 8) return;
    PlayerInventory inventory = player.getInventory();
    if (slot == button) return;
    ItemStack incoming = real(inventory, button);
    ItemStack outgoing = real(inventory, slot);
    if (!weapon(incoming) || !weapon(outgoing)) {
      plugin.message(player, "zly-typ"); return;
    }
    ItemStack right = slot == RIGHT ? incoming : button == RIGHT ? outgoing : real(inventory, RIGHT);
    ItemStack left = slot == LEFT ? incoming : real(inventory, LEFT);
    if (!validPair(player, right, left)) return;
    inventory.setItem(slot, incoming);
    inventory.setItem(button, outgoing);
    refreshLater(player);
  }

  // Zwraca resztę stosu. Źródło jest aktualizowane dopiero po zapisie celu.
  private ItemStack store(PlayerInventory inventory, ItemStack source, int first, int last) {
    if (Przedmioty.empty(source) || virtual(source)) return source;
    int remaining = source.getAmount();
    for (int slot = first; slot <= last; slot++) {
      ItemStack current = inventory.getItem(slot);
      if (Przedmioty.empty(current) || !current.isSimilar(source)) continue;
      int count = Math.min(remaining, current.getMaxStackSize() - current.getAmount());
      if (count <= 0) continue;
      ItemStack merged = current.clone();
      merged.setAmount(current.getAmount() + count);
      inventory.setItem(slot, merged);
      remaining -= count;
      if (remaining == 0) return null;
    }
    for (int slot = first; slot <= last; slot++) {
      if (!Przedmioty.empty(inventory.getItem(slot))) continue;
      int count = Math.min(remaining, source.getMaxStackSize());
      ItemStack part = source.clone();
      part.setAmount(count);
      inventory.setItem(slot, part);
      remaining -= count;
      if (remaining == 0) return null;
    }
    ItemStack rest = source.clone();
    rest.setAmount(remaining);
    return rest;
  }

  private void shift(Player player, InventoryClickEvent event) {
    ItemStack source = event.getCurrentItem();
    if (Przedmioty.empty(source) || virtual(source)) return;
    PlayerInventory inventory = player.getInventory();
    int slot = event.getSlot();
    if (event.getClickedInventory() != inventory) {
      if (event.getSlotType() == InventoryType.SlotType.RESULT) return;
      // Shift z obcego okna nie może wypełnić zarezerwowanych slotów.
      ItemStack rest = store(inventory, source, 14, 35);
      rest = store(inventory, rest, 2, 7);
      event.setCurrentItem(rest);
      refreshLater(player);
      return;
    }
    if (slot >= 14 && slot <= 35) {
      if (weapon(source)) {
        for (int target : new int[] {RIGHT, LEFT, RIGHT_SPARE, LEFT_SPARE}) {
          if (!Przedmioty.empty(real(inventory, target))) continue;
          if (target == RIGHT && twoHanded(source)
              && !Przedmioty.empty(real(inventory, LEFT))) {
            plugin.message(player, "dwureczna"); return;
          }
          if (target == LEFT && twoHanded(real(inventory, RIGHT))) continue;
          inventory.setItem(target, source);
          event.setCurrentItem(null);
          refreshLater(player);
          return;
        }
      }
      event.setCurrentItem(store(inventory, source, 2, 7));
    } else {
      event.setCurrentItem(store(inventory, source, 14, 35));
    }
    refreshLater(player);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void swap(PlayerSwapHandItemsEvent event) {
    Player player = event.getPlayer();
    PlayerInventory inventory = player.getInventory();
    if (inventory.getHeldItemSlot() == LEFT_IMAGE || isImage(event.getMainHandItem())
        || isImage(event.getOffHandItem())) { event.setCancelled(true); return; }
    if (!player.isSneaking()) {
      if (!validPair(player, inventory.getHeldItemSlot() == RIGHT
          ? event.getMainHandItem() : inventory.getItem(RIGHT), event.getOffHandItem())) {
        event.setCancelled(true); return;
      }
      refreshLater(player);
      return;
    }
    event.setCancelled(true);
    if (player.getOpenInventory().getType() != InventoryType.CRAFTING) {
      plugin.message(player, "okno"); return;
    }
    ItemStack activeRight = real(inventory, RIGHT), activeLeft = real(inventory, LEFT);
    ItemStack spareRight = real(inventory, RIGHT_SPARE), spareLeft = real(inventory, LEFT_SPARE);
    if (!validPair(player, activeRight, activeLeft)
        || !validPair(player, spareRight, spareLeft)) return;
    inventory.setItem(RIGHT, spareRight);
    inventory.setItem(LEFT, spareLeft);
    inventory.setItem(RIGHT_SPARE, activeRight);
    inventory.setItem(LEFT_SPARE, activeLeft);
    refreshLater(player);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void click(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) return;
    ItemStack cursor = event.getCursor(), current = event.getCurrentItem();
    if (virtual(cursor)) {
      event.setCancelled(true);
      player.setItemOnCursor(null);
      refreshLater(player);
      return;
    }
    Inventory clicked = event.getClickedInventory();
    if (clicked == null) return;
    PlayerInventory inventory = player.getInventory();
    int slot = event.getSlot();
    boolean playerSlot = clicked == inventory;
    if (isImage(current) && (!playerSlot || slot != LEFT_IMAGE && slot != RIGHT_IMAGE)) {
      event.setCancelled(true);
      event.setCurrentItem(null);
      refreshLater(player);
      return;
    }
    if (isReserveIcon(current) && (!playerSlot || slot != RIGHT_SPARE && slot != LEFT_SPARE)) {
      event.setCancelled(true);
      event.setCurrentItem(null);
      refreshLater(player);
      return;
    }
    if (event.getHotbarButton() == LEFT_IMAGE) { event.setCancelled(true); return; }
    if (playerSlot && (slot == LEFT_IMAGE || slot == RIGHT_IMAGE)) {
      event.setCancelled(true);
      if (!event.isShiftClick() && (event.getClick() == ClickType.LEFT
          || event.getClick() == ClickType.RIGHT))
        clickWeapon(player, slot == RIGHT_IMAGE ? RIGHT : LEFT, cursor);
      else refreshLater(player);
      return;
    }
    if (playerSlot && (slot == RIGHT || slot == LEFT || slot == RIGHT_SPARE || slot == LEFT_SPARE)) {
      event.setCancelled(true);
      if (event.isShiftClick()) {
        ItemStack source = real(inventory, slot);
        if (!Przedmioty.empty(source)) {
          inventory.setItem(slot, store(inventory, source, 14, 35));
          refreshLater(player);
        }
      } else if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT) {
        clickWeapon(player, slot, cursor);
      } else if (event.getClick() == ClickType.NUMBER_KEY) {
        hotbarSwap(player, slot, event.getHotbarButton());
      } else refreshLater(player);
      return;
    }
    if (event.isCancelled()) return;
    if (event.isShiftClick()) {
      event.setCancelled(true);
      if (playerSlot && slot == 8) return;
      shift(player, event);
      return;
    }
    if (event.getClick() == ClickType.NUMBER_KEY && event.getHotbarButton() == RIGHT) {
      event.setCancelled(true);
      if (event.getSlotType() == InventoryType.SlotType.RESULT
          || clicked == event.getView().getTopInventory()
              && event.getView().getType() == InventoryType.CRAFTING) return;
      if (virtual(inventory.getItem(RIGHT))) return;
      if (!allowed(player, RIGHT, current)) return;
      ItemStack active = inventory.getItem(RIGHT);
      clicked.setItem(slot, active);
      inventory.setItem(RIGHT, current);
      refreshLater(player);
    } else if (event.getClick() == ClickType.SWAP_OFFHAND) {
      event.setCancelled(true);
      if (event.getSlotType() == InventoryType.SlotType.RESULT
          || clicked == event.getView().getTopInventory()
              && event.getView().getType() == InventoryType.CRAFTING) return;
      if (virtual(inventory.getItemInOffHand())) return;
      if (!allowed(player, LEFT, current)) return;
      ItemStack active = inventory.getItemInOffHand();
      clicked.setItem(slot, active);
      inventory.setItemInOffHand(current);
      refreshLater(player);
    } else {
      refreshLater(player);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void drag(InventoryDragEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) return;
    int top = event.getView().getTopInventory().getSize();
    if (virtual(event.getOldCursor()) || event.getRawSlots().stream().anyMatch(raw -> {
      if (raw < top) return false;
      int slot = event.getView().convertSlot(raw);
      return slot == LEFT_IMAGE || slot == RIGHT_IMAGE || slot == RIGHT
          || slot == LEFT || slot == RIGHT_SPARE || slot == LEFT_SPARE;
    })) event.setCancelled(true);
    refreshLater(player);
  }

  @EventHandler(priority = EventPriority.HIGHEST) public void held(PlayerItemHeldEvent event) {
    refreshLater(event.getPlayer());
  }
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void interact(PlayerInteractEvent event) {
    if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
    PlayerInventory inventory = event.getPlayer().getInventory();
    ItemStack main = inventory.getItemInMainHand();
    if (isImage(main) || inventory.getHeldItemSlot() != RIGHT
        && !Przedmioty.empty(main) && weapon(main)) event.setCancelled(true);
  }
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void entity(PlayerInteractEntityEvent event) {
    if (isImage(event.getPlayer().getInventory().getItemInMainHand())) event.setCancelled(true);
  }
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void damage(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player player
        && (player.getInventory().getHeldItemSlot() != RIGHT
            || isImage(player.getInventory().getItemInMainHand()))) event.setCancelled(true);
  }
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void shoot(EntityShootBowEvent event) {
    if (event.getEntity() instanceof Player player
        && player.getInventory().getHeldItemSlot() != RIGHT
        && event.getBow() != null
        && event.getBow().isSimilar(player.getInventory().getItemInMainHand()))
      event.setCancelled(true);
  }
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void drop(PlayerDropItemEvent event) {
    if (virtual(event.getItemDrop().getItemStack())) event.setCancelled(true);
    refreshLater(event.getPlayer());
  }
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void pickup(EntityPickupItemEvent event) {
    if (event.getEntity() instanceof Player player) refreshLater(player);
  }
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void breakItem(PlayerItemBreakEvent event) { refreshLater(event.getPlayer()); }
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void damageItem(PlayerItemDamageEvent event) { refreshLater(event.getPlayer()); }
  @EventHandler public void death(PlayerDeathEvent event) {
    event.getDrops().removeIf(this::virtual);
  }
}
