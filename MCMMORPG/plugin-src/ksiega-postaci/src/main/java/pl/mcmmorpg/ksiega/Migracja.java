package pl.mcmmorpg.ksiega;

import io.lumine.mythic.lib.api.item.NBTItem;
import java.util.ArrayList;
import java.util.List;
import net.Indyuce.inventory.MMOInventory;
import net.Indyuce.inventory.inventory.VanillaInventory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

final class Migracja {
  private final KsiegaPostaci plugin;
  Migracja(KsiegaPostaci plugin) { this.plugin = plugin; }

  void migrate(Player player, Dane.Record record) {
    if (record.migrated) return;
    PlayerInventory inventory = player.getInventory();
    ItemStack[] source = new ItemStack[6];
    for (int i = 0; i < 6; i++) source[i] = inventory.getItem(9 + i);
    ItemStack old9 = record.jewelry[0], old10 = record.jewelry[1];
    ItemStack old11 = record.jewelry[2], old12 = record.jewelry[3];
    List<ItemStack> leftovers = new ArrayList<>();
    ItemStack gloves = null, belt = null, spareRight = null, spareLeft = null;
    int moved = 0, removed = 0;
    for (int i = 0; i < 6; i++) {
      ItemStack item = source[i];
      if (Przedmioty.empty(item)) continue;
      if (i >= 2 && i <= 3 && plugin.bron.isReserveIcon(item)) {
        if (i == 2) spareRight = item;
        else spareLeft = item;
        continue;
      }
      if ((i == 0 && validPlaceholder(item, "mmo_icon_gloves"))
          || (i == 1 && validPlaceholder(item, "mmo_icon_belt"))) continue;
      if (placeholderWithoutMmoItems(item)) { removed += item.getAmount(); continue; }
      String type = Przedmioty.type(item);
      if (i == 2 && plugin.bron.weapon(item)) spareRight = item;
      else if (i == 3 && plugin.bron.weapon(item)) spareLeft = item;
      else if ("GLOVES".equals(type) && gloves == null) gloves = item;
      else if ("BELT".equals(type) && belt == null) belt = item;
      else {
        int target = vacant(record, type);
        if (target >= 0) { record.jewelry[target] = item.clone(); moved += item.getAmount(); }
        else leftovers.add(item);
      }
    }
    ItemStack slot18 = inventory.getItem(18);
    record.migrated = true;
    if (!plugin.dane.save(record)) {
      record.jewelry[0] = old9; record.jewelry[1] = old10;
      record.jewelry[2] = old11; record.jewelry[3] = old12;
      record.migrated = false;
      plugin.message(player, "migracja-blad");
      plugin.getLogger().severe("Migracja profilu " + record.key + " wymaga recznej kontroli zapisu");
      return;
    }
    // Zapis musi się powieść zanim ruszymy jakikolwiek przedmiot gracza.
    for (int slot = 9; slot <= 14; slot++) inventory.setItem(slot, null);
    inventory.setItem(9, gloves != null ? gloves :
        validPlaceholder(source[0], "mmo_icon_gloves") ? source[0] : null);
    inventory.setItem(10, belt != null ? belt :
        validPlaceholder(source[1], "mmo_icon_belt") ? source[1] : null);
    inventory.setItem(11, spareRight);
    inventory.setItem(12, spareLeft);
    if (gloves != null && source[3] != gloves) moved += gloves.getAmount();
    if (belt != null && source[4] != belt) moved += belt.getAmount();
    for (ItemStack item : leftovers) {
      give(player, item);
      moved += item.getAmount();
    }
    if ("UI_SAKWA".equals(Przedmioty.id(slot18))) {
      inventory.setItem(18, null);
      removed++;
    }
    for (int slot = 0; slot < 36; slot++) {
      ItemStack item = inventory.getItem(slot);
      if (!placeholderWithoutMmoItems(item)) continue;
      if ((slot == 9 && validPlaceholder(item, "mmo_icon_gloves")
              && !plugin.bron.isClone(item) && !plugin.bron.isReserveIcon(item))
          || (slot == 10 && validPlaceholder(item, "mmo_icon_belt")
              && !plugin.bron.isClone(item) && !plugin.bron.isReserveIcon(item))
          || (slot == 1 && plugin.bron.isClone(item))
          || ((slot == 11 || slot == 12) && plugin.bron.isReserveIcon(item))) continue;
      inventory.setItem(slot, null);
      removed += item.getAmount();
    }
    ItemStack offhand = inventory.getItemInOffHand();
    if (placeholderWithoutMmoItems(offhand)) {
      inventory.setItemInOffHand(null);
      removed += offhand.getAmount();
    }
    ItemStack cursor = player.getItemOnCursor();
    if (placeholderWithoutMmoItems(cursor)) {
      player.setItemOnCursor(null);
      removed += cursor.getAmount();
    }
    VanillaInventory vanilla = MMOInventory.plugin.getInventoryManager().getVanilla();
    if (vanilla != null) vanilla.replacePlaceholderItems(player);
    for (int i = 0; i < 4; i++) if (!Przedmioty.empty(record.jewelry[i])) plugin.bizuteria.changed(player, i);
    if (moved != 0 || removed != 0) plugin.getLogger().info("Migracja " + record.key
        + " gracz=" + player.getName() + " przeniesione=" + moved + " zaslepki/przycisk=" + removed);
  }

  private static boolean validPlaceholder(ItemStack item, String model) {
    return placeholderWithoutMmoItems(item)
        && item.getItemMeta().getItemModel().getKey().equals(model);
  }

  private static boolean placeholderWithoutMmoItems(ItemStack item) {
    if (!Przedmioty.placeholder(item)) return false;
    NBTItem nbt = NBTItem.get(item);
    return !nbt.hasTag("MMOITEMS_ITEM_TYPE") && !nbt.hasTag("MMOITEMS_ITEM_ID");
  }

  private int vacant(Dane.Record record, String type) {
    if (type == null) return -1;
    for (int i = 0; i < 4; i++) {
      if (Bizuteria.TYPES[i].equals(type) && Przedmioty.empty(record.jewelry[i])) return i;
    }
    return -1;
  }

  static void give(Player player, ItemStack item) {
    if (Przedmioty.empty(item)) return;
    PlayerInventory inventory = player.getInventory();
    int remaining = item.getAmount();
    for (int slot : safeSlots()) {
      ItemStack current = inventory.getItem(slot);
      if (Przedmioty.empty(current) || !current.isSimilar(item)) continue;
      int amount = Math.min(remaining, current.getMaxStackSize() - current.getAmount());
      if (amount <= 0) continue;
      ItemStack merged = current.clone(); merged.setAmount(current.getAmount() + amount);
      inventory.setItem(slot, merged); remaining -= amount;
      if (remaining == 0) return;
    }
    for (int slot : safeSlots()) {
      if (!Przedmioty.empty(inventory.getItem(slot))) continue;
      int amount = Math.min(remaining, item.getMaxStackSize());
      ItemStack part = item.clone(); part.setAmount(amount);
      inventory.setItem(slot, part); remaining -= amount;
      if (remaining == 0) return;
    }
    while (remaining > 0) {
      int amount = Math.min(remaining, item.getMaxStackSize());
      ItemStack part = item.clone(); part.setAmount(amount);
      player.getWorld().dropItemNaturally(player.getLocation(), part);
      remaining -= amount;
    }
  }

  private static int[] safeSlots() {
    int[] slots = new int[22];
    int next = 0;
    for (int i = 14; i <= 35; i++) slots[next++] = i;
    return slots;
  }
}
