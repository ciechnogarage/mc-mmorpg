package pl.mcmmorpg.ksiega;

import io.lumine.mythic.lib.api.item.NBTItem;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

final class Przedmioty {
  private Przedmioty() {}

  static boolean empty(ItemStack item) {
    return item == null || item.getType().isAir() || item.getAmount() <= 0;
  }

  static String type(ItemStack item) {
    if (empty(item)) return null;
    NBTItem nbt = NBTItem.get(item);
    return nbt.hasTag("MMOITEMS_ITEM_TYPE") ? nbt.getString("MMOITEMS_ITEM_TYPE") : null;
  }

  static String id(ItemStack item) {
    if (empty(item)) return null;
    NBTItem nbt = NBTItem.get(item);
    return nbt.hasTag("MMOITEMS_ITEM_ID") ? nbt.getString("MMOITEMS_ITEM_ID") : null;
  }

  static boolean placeholder(ItemStack item) {
    if (empty(item) || !item.hasItemMeta()) return false;
    ItemMeta meta = item.getItemMeta();
    if (!meta.hasItemModel()) return false;
    NamespacedKey model = meta.getItemModel();
    return model != null && model.getNamespace().equals("nexo")
        && model.getKey().startsWith("mmo_icon_");
  }

  static ItemStack icon(String model, String name, String description, NamespacedKey marker) {
    ItemStack item = new ItemStack(Material.PAPER);
    ItemMeta meta = item.getItemMeta();
    meta.setItemModel(NamespacedKey.fromString(model));
    meta.setDisplayName(name);
    meta.setLore(java.util.List.of(description));
    meta.getPersistentDataContainer().set(marker, PersistentDataType.BYTE, (byte) 1);
    item.setItemMeta(meta);
    return item;
  }

  static boolean marked(ItemStack item, NamespacedKey key) {
    return !empty(item) && item.hasItemMeta() &&
        item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
  }

  static String lower(String value) { return value.toLowerCase(Locale.ROOT); }
}
