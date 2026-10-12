package pl.mcmmorpg.ksiega;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Pierwszy prawy klik ostrzega, drugi potwierdza anulowanie w oknie MMOCore. */
final class PotwierdzenieZadania implements Listener {
  private static final long CZAS = 10_000_000_000L;
  private final KsiegaPostaci plugin;
  private final Map<UUID, Pending> pending = new HashMap<>();

  PotwierdzenieZadania(KsiegaPostaci plugin) { this.plugin = plugin; }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
  public void click(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)
        || event.getClick() != ClickType.RIGHT
        || event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()
        || !questView(event)) return;
    ItemStack item = event.getCurrentItem();
    if (!activeQuest(item)) return;
    String quest = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
    UUID uuid = player.getUniqueId();
    Pending previous = pending.get(uuid);
    if (previous != null && previous.quest.equals(quest) && System.nanoTime() < previous.until) {
      pending.remove(uuid);
      return; // Dopiero teraz natywne MMOCore otrzymuje PPM i anuluje zadanie.
    }
    event.setCancelled(true);
    // Navigator MythicLib wywołuje obsługę zadania nawet dla anulowanego zdarzenia.
    // Usunięcie bieżącego przedmiotu tylko na czas tego zdarzenia zatrzymuje jego handler.
    ItemStack saved = item.clone();
    int slot = event.getRawSlot();
    var inventory = event.getView().getTopInventory();
    event.setCurrentItem(null);
    Bukkit.getScheduler().runTask(plugin, () -> {
      if (player.isOnline() && player.getOpenInventory().getTopInventory() == inventory) {
        ItemStack now = inventory.getItem(slot);
        if (now == null || now.getType() == Material.AIR) inventory.setItem(slot, saved);
      }
    });
    pending.put(uuid, new Pending(quest, System.nanoTime() + CZAS));
    player.sendMessage(Component.text("Anulowanie zadania „" + quest + "” usunie bieżący postęp. "
        + "Kliknij je PPM ponownie w ciągu 10 sekund, aby potwierdzić.", NamedTextColor.YELLOW));
    if (PlayerData.has(player)) MMOPlayerData.get(player).getActionBar().show(40, 60L,
        "§ePonów PPM, aby anulować zadanie i utracić postęp.");
  }

  private static boolean questView(InventoryClickEvent event) {
    return PlainTextComponentSerializer.plainText().serialize(event.getView().title()).contains("䪌");
  }

  private static boolean activeQuest(ItemStack item) {
    if (item == null || !item.hasItemMeta()) return false;
    ItemMeta meta = item.getItemMeta();
    if (!meta.hasDisplayName() || meta.lore() == null) return false;
    return meta.lore().stream().map(PlainTextComponentSerializer.plainText()::serialize)
        .anyMatch(line -> line.contains("Prawy klik") && line.contains("anul"));
  }

  @EventHandler public void close(InventoryCloseEvent event) {
    if (PlainTextComponentSerializer.plainText().serialize(event.getView().title()).contains("䪌"))
      pending.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler public void quit(PlayerQuitEvent event) { pending.remove(event.getPlayer().getUniqueId()); }

  private record Pending(String quest, long until) {}
}
