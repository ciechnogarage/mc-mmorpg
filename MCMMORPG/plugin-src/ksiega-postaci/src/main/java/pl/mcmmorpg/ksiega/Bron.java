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
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

final class Bron implements Listener {
  private static final int RIGHT = 0, LEFT_IMAGE = 1, RIGHT_SPARE = 11;
  private static final int LEFT_SPARE = 12, RIGHT_IMAGE = 13, LEFT = 40;
  private static final int HEALTH = 2, MANA = 3, UTILITY = 4;
  private final KsiegaPostaci plugin;
  private final NamespacedKey cloneKey;
  private final NamespacedKey imageKey;
  private final NamespacedKey reserveKey;
  private final NamespacedKey consumableKey;

  Bron(KsiegaPostaci plugin) {
    this.plugin = plugin;
    cloneKey = new NamespacedKey("ksiega", "klon");
    imageKey = new NamespacedKey("ksiega", "obraz");
    reserveKey = new NamespacedKey(plugin, "zapas");
    consumableKey = new NamespacedKey(plugin, "zuzywalny-slot");
    // Inne pluginy mogą przestawić wybrany slot bez PlayerItemHeldEvent.
    Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      for (Player player : Bukkit.getOnlinePlayers()) ensureRightSelected(player);
    }, 1L, 1L);
  }

  // Scena kreatora sama wybiera pusty slot (LPM w scenie); widz nie walczy.
  private boolean freeSelection(Player player) {
    return player.getWorld().getName().equals("creator_limbo")
        || player.getGameMode() == org.bukkit.GameMode.SPECTATOR;
  }

  private void ensureRightSelected(Player player) {
    if (freeSelection(player)) return;
    if (player.getInventory().getHeldItemSlot() == LEFT_IMAGE)
      player.getInventory().setHeldItemSlot(RIGHT);
  }

  boolean isClone(ItemStack item) { return Przedmioty.marked(item, cloneKey); }
  boolean isImage(ItemStack item) { return isClone(item) || Przedmioty.marked(item, imageKey); }
  boolean isReserveIcon(ItemStack item) { return Przedmioty.marked(item, reserveKey); }
  private boolean isConsumableIcon(ItemStack item) { return Przedmioty.marked(item, consumableKey); }
  private boolean virtual(ItemStack item) {
    return isImage(item) || isReserveIcon(item) || isConsumableIcon(item);
  }

  private boolean consumableSlot(int slot) { return slot >= HEALTH && slot <= UTILITY; }

  boolean consumable(int slot, ItemStack item) {
    if (Przedmioty.empty(item)) return true;
    if (virtual(item) || !plugin.getConfig().getStringList("typy-mikstur-i-uzytkowych")
        .contains(Przedmioty.type(item))) return false;
    NBTItem nbt = NBTItem.get(item);
    return switch (slot) {
      case HEALTH -> nbt.getStat(plugin.getConfig().getString("staty-mikstur.zycie")) > 0;
      case MANA -> nbt.getStat(plugin.getConfig().getString("staty-mikstur.mana")) > 0;
      case UTILITY -> true;
      default -> false;
    };
  }

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

  private boolean canStore(PlayerInventory inventory, ItemStack source, int first, int last) {
    if (Przedmioty.empty(source)) return true;
    int capacity = 0;
    for (int slot = first; slot <= last; slot++) {
      ItemStack current = inventory.getItem(slot);
      if (Przedmioty.empty(current)) capacity += source.getMaxStackSize();
      else if (current.isSimilar(source)) capacity += Math.max(0,
          current.getMaxStackSize() - current.getAmount());
      if (capacity >= source.getAmount()) return true;
    }
    return false;
  }

  // Plan przeniesienia lewej broni jest liczony przed pierwszą zmianą ekwipunku.
  private int leftDestination(Player player, ItemStack incoming, boolean spareOnly) {
    PlayerInventory inventory = player.getInventory();
    if (!twoHanded(incoming) || Przedmioty.empty(real(inventory, LEFT))) return -1;
    if (Przedmioty.empty(real(inventory, LEFT_SPARE))
        && !twoHanded(real(inventory, RIGHT_SPARE))) return LEFT_SPARE;
    if (!spareOnly && canStore(inventory, real(inventory, LEFT), 14, 35)) return 14;
    plugin.message(player, spareOnly ? "brak-zapasu-lewej" : "brak-miejsca-lewej");
    return -2;
  }

  private void moveLeft(PlayerInventory inventory, int destination) {
    if (destination == LEFT_SPARE) inventory.setItem(LEFT_SPARE, real(inventory, LEFT));
    else if (destination == 14) store(inventory, real(inventory, LEFT), 14, 35);
    if (destination >= 0) inventory.setItem(LEFT, null);
  }

  private int spareLeftDestination(Player player, ItemStack incoming) {
    PlayerInventory inventory = player.getInventory();
    ItemStack spareLeft = real(inventory, LEFT_SPARE);
    if (!twoHanded(incoming) || Przedmioty.empty(spareLeft)) return -1;
    if (Przedmioty.empty(real(inventory, LEFT))
        && !twoHanded(real(inventory, RIGHT))) return LEFT;
    if (canStore(inventory, spareLeft, 14, 35)) return 14;
    plugin.message(player, "brak-miejsca-lewej");
    return -2;
  }

  private void moveSpareLeft(PlayerInventory inventory, int destination) {
    if (destination == LEFT) inventory.setItem(LEFT, real(inventory, LEFT_SPARE));
    else if (destination == 14) store(inventory, real(inventory, LEFT_SPARE), 14, 35);
    if (destination >= 0) inventory.setItem(LEFT_SPARE, null);
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
    for (int slot = HEALTH; slot <= UTILITY; slot++)
      if (isConsumableIcon(inventory.getItem(slot))) inventory.setItem(slot, null);
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
      case "zajeta-lewa" -> configured == null ? "§7Lewa ręka – zajęta (broń dwuręczna)" : configured;
      case "zajety-zapas-lewa" -> configured == null ? "§7Zapasowa lewa – zajęta (broń dwuręczna)" : configured;
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
    if (!right) meta.setDisplayName("§eLewa ręka §8| §f" +
        (meta.hasDisplayName() ? meta.getDisplayName() : source.getType().name()));
    meta.setLore(right ? List.of("§6W ręce (prawa)")
        : List.of("§eObraz broni w lewej ręce", "§8Kliknij, aby zmienić broń"));
    meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
    result.setItemMeta(meta);
    return result;
  }

  private ItemStack emptyIcon(String side, NamespacedKey key, String lore) {
    return Przedmioty.icon(plugin.getConfig().getString("ikony." + side,
        "nexo:mmo_icon_gloves"), iconName(side), lore, key);
  }

  private ItemStack consumableIcon(int slot) {
    String id = switch (slot) {
      case HEALTH -> "mikstura-zycia";
      case MANA -> "mikstura-many";
      default -> "przedmiot-uzytkowy";
    };
    return Przedmioty.icon(plugin.getConfig().getString("ikony." + id),
        plugin.getConfig().getString("nazwy-ikon." + id),
        plugin.getConfig().getString("opisy-ikon." + id), consumableKey);
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
    ensureRightSelected(player);
    if (player.getWorld().getName().equals("creator_limbo")) return;
    PlayerInventory inventory = player.getInventory();
    recover(player, LEFT_IMAGE);
    recover(player, RIGHT_IMAGE);
    for (int slot = HEALTH; slot <= UTILITY; slot++) {
      ItemStack item = inventory.getItem(slot);
      if (isConsumableIcon(item)) inventory.setItem(slot, null);
      else if (!Przedmioty.empty(item) && !consumable(slot, item)) {
        inventory.setItem(slot, null);
        if (!virtual(item) && !(Przedmioty.placeholder(item)
            && Przedmioty.type(item) == null && Przedmioty.id(item) == null))
          Migracja.give(player, item);
      }
    }
    for (int slot = 0; slot < 36; slot++) {
      if (slot == LEFT_IMAGE || slot == RIGHT_IMAGE) continue;
      ItemStack item = inventory.getItem(slot);
      if (virtual(item) && slot != RIGHT_SPARE && slot != LEFT_SPARE)
        inventory.setItem(slot, null);
    }
    if (virtual(inventory.getItemInOffHand())) inventory.setItemInOffHand(null);
    if (virtual(player.getItemOnCursor())) player.setItemOnCursor(null);
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
    if (twoHanded(inventory.getItem(RIGHT_SPARE))
        && !Przedmioty.empty(real(inventory, LEFT_SPARE))) {
      ItemStack left = real(inventory, LEFT_SPARE);
      inventory.setItem(LEFT_SPARE, null);
      Migracja.give(player, left);
    }
    inventory.setItem(LEFT_IMAGE, twoHanded(inventory.getItem(RIGHT))
        ? emptyIcon("zajeta-lewa", cloneKey, "§8Zajęta")
        : image(inventory.getItemInOffHand(), false));
    inventory.setItem(RIGHT_IMAGE, image(inventory.getItem(RIGHT), true));
    for (int slot : new int[] {RIGHT_SPARE, LEFT_SPARE}) {
      if (!Przedmioty.empty(inventory.getItem(slot))) continue;
      String side = slot == RIGHT_SPARE ? "zapas-prawa"
          : twoHanded(inventory.getItem(RIGHT_SPARE)) ? "zajety-zapas-lewa" : "zapas-lewa";
      inventory.setItem(slot, emptyIcon(side, reserveKey,
          slot == LEFT_SPARE && twoHanded(inventory.getItem(RIGHT_SPARE))
              ? "§8Zajęta" : "§8Puste"));
    }
    for (int slot = HEALTH; slot <= UTILITY; slot++)
      if (Przedmioty.empty(inventory.getItem(slot))) inventory.setItem(slot, consumableIcon(slot));
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
    if (slot == RIGHT) return leftDestination(player, incoming, false) != -2;
    if (slot == LEFT) return validPair(player, real(inventory, RIGHT), incoming);
    if (slot == RIGHT_SPARE) return spareLeftDestination(player, incoming) != -2;
    if (slot == LEFT_SPARE) return validPair(player, real(inventory, RIGHT_SPARE), incoming);
    return true;
  }

  private void clickWeapon(Player player, int slot, ItemStack cursor) {
    if (!allowed(player, slot, cursor)) return;
    PlayerInventory inventory = player.getInventory();
    ItemStack old = real(inventory, slot);
    if (slot == RIGHT) moveLeft(inventory, leftDestination(player, cursor, false));
    if (slot == RIGHT_SPARE) moveSpareLeft(inventory, spareLeftDestination(player, cursor));
    inventory.setItem(slot, Przedmioty.empty(cursor) ? null : cursor);
    player.setItemOnCursor(Przedmioty.empty(old) ? null : old);
    refreshLater(player);
  }

  private void clickConsumable(Player player, int slot, ItemStack cursor) {
    if (!consumable(slot, cursor)) { plugin.message(player, "zly-typ"); return; }
    PlayerInventory inventory = player.getInventory();
    ItemStack old = real(inventory, slot);
    inventory.setItem(slot, Przedmioty.empty(cursor) ? null : cursor);
    player.setItemOnCursor(Przedmioty.empty(old) ? null : old);
    refreshLater(player);
  }

  private void hotbarConsumableSwap(Player player, int slot, int button) {
    if (button < 0 || button > 8 || button == LEFT_IMAGE || button == RIGHT
        || button == slot) return;
    PlayerInventory inventory = player.getInventory();
    ItemStack incoming = real(inventory, button), outgoing = real(inventory, slot);
    if (!consumable(slot, incoming) || consumableSlot(button)
        && !consumable(button, outgoing)) {
      plugin.message(player, "zly-typ"); return;
    }
    inventory.setItem(slot, incoming);
    inventory.setItem(button, outgoing);
    refreshLater(player);
  }

  private void hotbarSwap(Player player, int slot, int button) {
    if (button == LEFT_IMAGE || button < 0 || button > 8) return;
    PlayerInventory inventory = player.getInventory();
    if (slot == button) return;
    if (button == RIGHT && slot == RIGHT_SPARE) {
      swapRight(player); return;
    }
    if (button == RIGHT || consumableSlot(button)) {
      plugin.message(player, "zly-typ"); return;
    }
    ItemStack incoming = real(inventory, button);
    ItemStack outgoing = real(inventory, slot);
    if (!allowed(player, slot, incoming) || !weapon(outgoing)) {
      plugin.message(player, "zly-typ"); return;
    }
    if (slot == RIGHT) moveLeft(inventory, leftDestination(player, incoming, false));
    if (slot == RIGHT_SPARE) moveSpareLeft(inventory, spareLeftDestination(player, incoming));
    inventory.setItem(slot, incoming);
    inventory.setItem(button, outgoing);
    refreshLater(player);
  }

  private void swapMessage(Player player, String side, ItemStack item) {
    String name = "pusta";
    if (!Przedmioty.empty(item)) {
      ItemMeta meta = item.getItemMeta();
      name = meta != null && meta.hasDisplayName() ? meta.getDisplayName()
          : item.getType().name();
    }
    player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
        .legacySection().deserialize("§6" + side + ": §f" + name));
  }

  private void swapRight(Player player) {
    PlayerInventory inventory = player.getInventory();
    ItemStack incoming = real(inventory, RIGHT_SPARE);
    ItemStack outgoing = real(inventory, RIGHT);
    ItemStack spareLeft = real(inventory, LEFT_SPARE);
    ItemStack activeLeft = real(inventory, LEFT);
    if (!weapon(incoming) || !weapon(outgoing) || !weapon(activeLeft)
        || !weapon(spareLeft)) {
      plugin.message(player, "zly-typ"); return;
    }
    if (twoHanded(incoming) && twoHanded(outgoing)
        && !Przedmioty.empty(activeLeft) && !Przedmioty.empty(spareLeft)) {
      plugin.message(player, "dwureczna"); return;
    }
    int destination = -1;
    if (twoHanded(incoming) && !Przedmioty.empty(activeLeft)) {
      if (Przedmioty.empty(spareLeft) && !twoHanded(outgoing)) destination = LEFT_SPARE;
      else if (canStore(inventory, activeLeft, 14, 35)) destination = 14;
      else { plugin.message(player, "brak-miejsca-lewej"); return; }
    }
    int spareDestination = -1;
    if (twoHanded(outgoing) && !Przedmioty.empty(spareLeft)) {
      if (Przedmioty.empty(activeLeft) && !twoHanded(incoming)) spareDestination = LEFT;
      else if (canStore(inventory, spareLeft, 14, 35)) spareDestination = 14;
      else { plugin.message(player, "brak-miejsca-lewej"); return; }
    }
    moveLeft(inventory, destination);
    moveSpareLeft(inventory, spareDestination);
    inventory.setItem(RIGHT, incoming);
    inventory.setItem(RIGHT_SPARE, outgoing);
    refreshLater(player);
    swapMessage(player, "Prawa ręka", incoming);
  }

  private void swapLeft(Player player) {
    PlayerInventory inventory = player.getInventory();
    if (twoHanded(real(inventory, RIGHT)) || twoHanded(real(inventory, RIGHT_SPARE))) {
      plugin.message(player, "dwureczna"); return;
    }
    ItemStack active = real(inventory, LEFT), spare = real(inventory, LEFT_SPARE);
    if (!weapon(active) || !weapon(spare)) { plugin.message(player, "zly-typ"); return; }
    inventory.setItem(LEFT, spare);
    inventory.setItem(LEFT_SPARE, active);
    refreshLater(player);
    swapMessage(player, "Lewa ręka", spare);
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
      rest = store(inventory, rest, 5, 7);
      event.setCurrentItem(rest);
      refreshLater(player);
      return;
    }
    if (slot >= 14 && slot <= 35) {
      if (weapon(source)) {
        for (int target : new int[] {RIGHT, LEFT, RIGHT_SPARE, LEFT_SPARE}) {
          if (!Przedmioty.empty(real(inventory, target))) continue;
          if (target == LEFT && twoHanded(real(inventory, RIGHT))) {
            plugin.message(player, "dwureczna"); return;
          }
          if (target == LEFT_SPARE && twoHanded(real(inventory, RIGHT_SPARE))) {
            plugin.message(player, "dwureczna"); return;
          }
          if (target == RIGHT_SPARE && !validPair(player, source,
              real(inventory, LEFT_SPARE))) return;
          if (target == RIGHT && twoHanded(source)
              && !Przedmioty.empty(real(inventory, LEFT))) {
            int destination = Przedmioty.empty(real(inventory, LEFT_SPARE))
                && !twoHanded(real(inventory, RIGHT_SPARE)) ? LEFT_SPARE : 14;
            // Ten slot plecaka zwalnia się wraz z założeniem broni.
            event.setCurrentItem(null);
            if (destination == LEFT_SPARE) moveLeft(inventory, destination);
            else {
              ItemStack left = real(inventory, LEFT);
              inventory.setItem(LEFT, null);
              store(inventory, left, 14, 35);
            }
          } else event.setCurrentItem(null);
          inventory.setItem(target, source);
          refreshLater(player);
          return;
        }
      }
      for (int target = HEALTH; target <= UTILITY; target++) {
        if (!consumable(target, source) || !Przedmioty.empty(real(inventory, target))) continue;
        inventory.setItem(target, source);
        event.setCurrentItem(null);
        refreshLater(player);
        return;
      }
      event.setCurrentItem(store(inventory, source, 5, 7));
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
        || isImage(event.getOffHandItem())) {
      event.setCancelled(true); return;
    }
    if (!player.isSneaking()) {
      if (inventory.getHeldItemSlot() != RIGHT
          || !validPair(player, event.getMainHandItem(), event.getOffHandItem())) {
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
    if (plugin.ukrywanie.isBookView(event.getView())) return;
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
    if (isConsumableIcon(current) && (!playerSlot || !consumableSlot(slot))) {
      event.setCancelled(true);
      event.setCurrentItem(null);
      refreshLater(player);
      return;
    }
    if (event.getHotbarButton() == LEFT_IMAGE) { event.setCancelled(true); return; }
    if (playerSlot && consumableSlot(slot)) {
      event.setCancelled(true);
      if (event.isShiftClick()) {
        ItemStack source = real(inventory, slot);
        if (!Przedmioty.empty(source)) {
          inventory.setItem(slot, store(inventory, source, 14, 35));
          refreshLater(player);
        }
      } else if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT) {
        clickConsumable(player, slot, cursor);
      } else if (event.getClick() == ClickType.NUMBER_KEY) {
        hotbarConsumableSwap(player, slot, event.getHotbarButton());
      } else refreshLater(player);
      return;
    }
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
      if ((slot == RIGHT_SPARE || slot == LEFT_SPARE)
          && event.getView().getType() == InventoryType.CRAFTING
          && event.getClick() == ClickType.RIGHT && Przedmioty.empty(cursor)) {
        if (slot == RIGHT_SPARE) swapRight(player);
        else swapLeft(player);
        return;
      }
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
      moveLeft(inventory, leftDestination(player, current, false));
      clicked.setItem(slot, active);
      inventory.setItem(RIGHT, current);
      refreshLater(player);
    } else if (event.getClick() == ClickType.NUMBER_KEY
        && consumableSlot(event.getHotbarButton())) {
      event.setCancelled(true);
      if (event.getSlotType() == InventoryType.SlotType.RESULT
          || clicked == event.getView().getTopInventory()
              && event.getView().getType() == InventoryType.CRAFTING
          || playerSlot && !(slot >= 5 && slot <= 8 || slot >= 14 && slot <= 35)) return;
      int target = event.getHotbarButton();
      if (!consumable(target, current)) { plugin.message(player, "zly-typ"); return; }
      ItemStack outgoing = real(inventory, target);
      clicked.setItem(slot, outgoing);
      inventory.setItem(target, current);
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
    if (plugin.ukrywanie.isBookView(event.getView())) return;
    int top = event.getView().getTopInventory().getSize();
    if (virtual(event.getOldCursor()) || event.getRawSlots().stream().anyMatch(raw -> {
      if (raw < top) return false;
      int slot = event.getView().convertSlot(raw);
      return slot == LEFT_IMAGE || slot == RIGHT_IMAGE || slot == RIGHT
          || slot == LEFT || slot == RIGHT_SPARE || slot == LEFT_SPARE
          || consumableSlot(slot) && !consumable(slot, event.getNewItems().get(raw));
    })) event.setCancelled(true);
    refreshLater(player);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void held(PlayerItemHeldEvent event) {
    if (event.getNewSlot() == LEFT_IMAGE && !freeSelection(event.getPlayer())) {
      boolean allowed = !event.isCancelled();
      event.setCancelled(true);
      int previous = event.getPreviousSlot();
      int next = previous == RIGHT ? HEALTH : RIGHT;
      if (allowed) Bukkit.getScheduler().runTask(plugin, () -> {
        Player player = event.getPlayer();
        if (player.isOnline() && player.getInventory().getHeldItemSlot() == previous)
          player.getInventory().setHeldItemSlot(next);
      });
    }
    refreshLater(event.getPlayer());
  }
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void interact(PlayerInteractEvent event) {
    if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
    PlayerInventory inventory = event.getPlayer().getInventory();
    ItemStack main = inventory.getItemInMainHand();
    if (isImage(main) || isConsumableIcon(main) || inventory.getHeldItemSlot() != RIGHT
        && !Przedmioty.empty(main) && weapon(main)) event.setCancelled(true);
    if (consumableSlot(inventory.getHeldItemSlot())) refreshLater(event.getPlayer());
  }
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void entity(PlayerInteractEntityEvent event) {
    ItemStack main = event.getPlayer().getInventory().getItemInMainHand();
    if (isImage(main) || isConsumableIcon(main)) event.setCancelled(true);
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
  public void consume(PlayerItemConsumeEvent event) { refreshLater(event.getPlayer()); }
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void damageItem(PlayerItemDamageEvent event) { refreshLater(event.getPlayer()); }
  @EventHandler public void death(PlayerDeathEvent event) {
    event.getDrops().removeIf(this::virtual);
  }
}
