package pl.mcmmorpg.eqmigracja;

import fr.phoenixdevt.profiles.event.ProfileSelectedEvent;
import io.lumine.mythic.lib.api.item.NBTItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.Indyuce.inventory.MMOInventory;
import net.Indyuce.inventory.inventory.VanillaInventory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class EqMigracja extends JavaPlugin {
  private static final String[] SLOT_TYPES = {
      "NECKLACE", "RING", "RING", "GLOVES", "BELT", "CHARM"
  };
  private boolean warnedNoVanilla;

  @Override
  public void onEnable() {
    if (Bukkit.getPluginManager().isPluginEnabled("MMOProfiles")) {
      Bukkit.getPluginManager().registerEvents(new ProfileListener(this), this);
    } else {
      Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);
    }
  }

  private void schedule(Player player) {
    Bukkit.getScheduler().runTaskLater(this, () -> {
      if (player.isOnline() && !player.getWorld().getName().equals("creator_limbo")) {
        migrate(player);
      }
    }, 2L);
  }

  private void migrate(Player player) {
    PlayerInventory inventory = player.getInventory();
    Stats stats = new Stats();

    // CoreTools reserves 8 and 18. Never readjust or overwrite them.
    for (int slot = 0; slot < 36; slot++) {
      if (slot == 8 || slot == 18) {
        continue;
      }
      ItemStack item = inventory.getItem(slot);
      if (validPlaceholder(item, slot)) {
        continue; // poprawna ikona nowego ukladu: zostaje, nic nie liczymy
      }
      if (shouldRemove(item, stats)) {
        inventory.setItem(slot, null);
      }
    }

    ItemStack[] armor = inventory.getArmorContents();
    boolean armorChanged = false;
    for (int slot = 0; slot < armor.length; slot++) {
      if (shouldRemove(armor[slot], stats)) {
        armor[slot] = null;
        armorChanged = true;
      }
    }
    if (armorChanged) {
      inventory.setArmorContents(armor);
    }
    if (shouldRemove(inventory.getItemInOffHand(), stats)) {
      inventory.setItemInOffHand(new ItemStack(Material.AIR));
    }
    if (shouldRemove(player.getItemOnCursor(), stats)) {
      player.setItemOnCursor(new ItemStack(Material.AIR));
    }

    List<ItemStack> displaced = new ArrayList<>();
    for (int slot = 9; slot <= 14; slot++) {
      ItemStack item = inventory.getItem(slot);
      if (!empty(item) && !validPlaceholder(item, slot)
          && !SLOT_TYPES[slot - 9].equals(itemType(item))) {
        inventory.setItem(slot, null);
        displaced.add(item);
      }
    }

    // The old equipped row has priority over accessories found in 15-17.
    for (ItemStack item : displaced) {
      int target = vacantAccessorySlot(inventory, itemType(item));
      if (target >= 0) {
        inventory.setItem(target, item);
      } else {
        addToBackpackOrDrop(player, inventory, item);
      }
      stats.moved += item.getAmount();
    }

    for (int slot = 15; slot <= 17; slot++) {
      ItemStack item = inventory.getItem(slot);
      if (empty(item)) {
        continue;
      }
      int target = vacantAccessorySlot(inventory, itemType(item));
      if (target >= 0) {
        inventory.setItem(slot, null);
        inventory.setItem(target, item);
        stats.moved += item.getAmount();
      }
      // An occupied target leaves this item safely in its backpack slot.
    }

    MMOInventory plugin = (MMOInventory) Bukkit.getPluginManager().getPlugin("MMOInventory");
    VanillaInventory vanilla = plugin.getInventoryManager().getVanilla();
    if (vanilla != null) {
      vanilla.replacePlaceholderItems(player);
    } else if (!warnedNoVanilla) {
      warnedNoVanilla = true;
      getLogger().warning("Brak aktywnego VanillaInventory; nie mozna odtworzyc ikon slotow.");
    }

    if (stats.placeholders != 0 || stats.removed != 0 || stats.moved != 0) {
      getLogger().info(player.getName() + ": zaslepki=" + stats.placeholders
          + ", usuniete=" + stats.removed + " " + stats.removedIds
          + ", przeniesione=" + stats.moved);
    }
  }

  private static boolean shouldRemove(ItemStack item, Stats stats) {
    if (empty(item)) {
      return false;
    }
    if (placeholder(item)) {
      stats.placeholders += item.getAmount();
      return true;
    }
    NBTItem nbt = NBTItem.get(item);
    if (!nbt.hasTag("MMOITEMS_ITEM_TYPE")) {
      return false;
    }
    String type = nbt.getString("MMOITEMS_ITEM_TYPE");
    if (!"EARRING".equals(type) && !"PAULDRONS".equals(type) && !"SHIRT".equals(type)) {
      return false;
    }
    String id = nbt.hasTag("MMOITEMS_ITEM_ID") ? nbt.getString("MMOITEMS_ITEM_ID") : "?";
    String label = type + "/" + (id == null ? "?" : id.replace('\n', '_').replace('\r', '_'));
    stats.removed += item.getAmount();
    stats.removedIds.merge(label, item.getAmount(), Integer::sum);
    return true;
  }

  private static boolean placeholder(ItemStack item) {
    ItemMeta meta = item.getItemMeta();
    if (meta == null || !meta.hasItemModel()) {
      return false;
    }
    NamespacedKey model = meta.getItemModel();
    return model != null && model.getNamespace().equals("nexo")
        && model.getKey().startsWith("mmo_icon_");
  }

  // Ikona zgodna z NOWYM ukladem (slot 9-14 -> nexo:mmo_icon_<typ>) to nie resztka migracji.
  private static boolean validPlaceholder(ItemStack item, int slot) {
    if (empty(item) || slot < 9 || slot > 14 || !placeholder(item)) {
      return false;
    }
    return item.getItemMeta().getItemModel().getKey()
        .equals("mmo_icon_" + SLOT_TYPES[slot - 9].toLowerCase(java.util.Locale.ROOT));
  }

  private static String itemType(ItemStack item) {
    if (empty(item)) {
      return null;
    }
    NBTItem nbt = NBTItem.get(item);
    return nbt.hasTag("MMOITEMS_ITEM_TYPE") ? nbt.getString("MMOITEMS_ITEM_TYPE") : null;
  }

  private static int vacantAccessorySlot(PlayerInventory inventory, String type) {
    if (type == null) {
      return -1;
    }
    for (int slot = 9; slot <= 14; slot++) {
      if (SLOT_TYPES[slot - 9].equals(type) && empty(inventory.getItem(slot))) {
        return slot;
      }
    }
    return -1;
  }

  private static boolean backpackSlot(int slot) {
    return slot >= 0 && slot < 36 && slot != 8 && slot != 18
        && (slot < 9 || slot > 14);
  }

  private static void addToBackpackOrDrop(Player player, PlayerInventory inventory, ItemStack item) {
    // Bukkit addItem can fill reserved UI slots and empty accessory slots, so restrict targets.
    int remaining = item.getAmount();
    for (int slot = 0; slot < 36 && remaining > 0; slot++) {
      if (!backpackSlot(slot)) {
        continue;
      }
      ItemStack present = inventory.getItem(slot);
      if (empty(present) || !present.isSimilar(item)) {
        continue;
      }
      int capacity = Math.min(present.getMaxStackSize(), inventory.getMaxStackSize())
          - present.getAmount();
      if (capacity > 0) {
        int added = Math.min(remaining, capacity);
        ItemStack merged = present.clone();
        merged.setAmount(present.getAmount() + added);
        inventory.setItem(slot, merged);
        remaining -= added;
      }
    }
    for (int slot = 0; slot < 36 && remaining > 0; slot++) {
      if (!backpackSlot(slot) || !empty(inventory.getItem(slot))) {
        continue;
      }
      int added = Math.min(remaining, Math.min(item.getMaxStackSize(), inventory.getMaxStackSize()));
      ItemStack part = item.clone();
      part.setAmount(added);
      inventory.setItem(slot, part);
      remaining -= added;
    }
    while (remaining > 0) {
      int dropped = Math.min(remaining, item.getMaxStackSize());
      ItemStack part = item.clone();
      part.setAmount(dropped);
      player.getWorld().dropItemNaturally(player.getLocation(), part);
      remaining -= dropped;
    }
  }

  private static boolean empty(ItemStack item) {
    return item == null || item.getType().isAir() || item.getAmount() <= 0;
  }

  private static final class Stats {
    int placeholders;
    int removed;
    int moved;
    final Map<String, Integer> removedIds = new LinkedHashMap<>();
  }

  private static final class ProfileListener implements Listener {
    private final EqMigracja plugin;

    private ProfileListener(EqMigracja plugin) {
      this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProfileSelected(ProfileSelectedEvent event) {
      plugin.schedule(event.getPlayer());
    }
  }

  private static final class JoinListener implements Listener {
    private final EqMigracja plugin;

    private JoinListener(EqMigracja plugin) {
      this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
      plugin.schedule(event.getPlayer());
    }
  }
}
