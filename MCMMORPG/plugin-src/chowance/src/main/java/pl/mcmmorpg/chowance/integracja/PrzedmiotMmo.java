package pl.mcmmorpg.chowance.integracja;

import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.inventory.ItemStack;

/** Rozpoznanie przedmiotu MMOItems po NBT (MMOITEMS_ITEM_TYPE / MMOITEMS_ITEM_ID) przez MythicLib. */
public final class PrzedmiotMmo {
  private PrzedmiotMmo() {}

  public record Id(String typ, String id) {}

  public static Id z(ItemStack item) {
    if (item == null || item.getType().isAir() || item.getAmount() <= 0) return null;
    try {
      NBTItem nbt = NBTItem.get(item);
      if (!nbt.hasType()) return null;
      String id = nbt.getString("MMOITEMS_ITEM_ID");
      String typ = nbt.getType();
      if (id == null || id.isEmpty() || typ == null) return null;
      return new Id(typ.toUpperCase(java.util.Locale.ROOT), id);
    } catch (RuntimeException e) {
      return null;
    }
  }

  public static String id(ItemStack item) {
    Id i = z(item);
    return i == null ? null : i.id();
  }
}
