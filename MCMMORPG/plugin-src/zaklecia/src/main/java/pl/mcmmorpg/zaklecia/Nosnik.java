package pl.mcmmorpg.zaklecia;

import io.lumine.mythic.bukkit.MythicBukkit;
import java.util.Set;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Allay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

/**
 * Nosnik zaklecia: przedmiot MythicMobs w dloni, zwiazany z postacia. Nie da sie go wyrzucic, przeniesc,
 * wlozyc do pojemnika, ramki, doniczki ani skarbca. Zmienia sie go tylko przez Ksiege zaklec
 * (komenda {@code zaklecia przypisz}, wolana z menu jako konsola).
 *
 * Prawa dlon = slot paska (wybrany, jesli pusty; inaczej pierwszy wolny 1-8), lewa dlon = slot drugiej reki.
 * Nosnik nigdy nie nadpisuje cudzego przedmiotu: bez miejsca przypisanie jest odrzucane z komunikatem.
 */
final class Nosnik implements Listener {
  private final Ksiega ksiega;
  private final NamespacedKey klucz;
  /** Bloki, ktore przyjmuja albo zamieniaja przedmiot z reki. */
  private static final Set<Material> BLOKI_BIORACE = Set.of(Material.DECORATED_POT, Material.FLOWER_POT,
      Material.VAULT, Material.CHISELED_BOOKSHELF, Material.LECTERN, Material.JUKEBOX);

  Nosnik(Zaklecia plugin, Ksiega ksiega) {
    this.ksiega = ksiega;
    this.klucz = new NamespacedKey(plugin, "nosnik");
  }

  boolean jest(ItemStack i) {
    return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(klucz, PersistentDataType.BYTE);
  }

  private static void pasek(Player p, String tekst) {
    // pasek MMOCore co 5 tickow nadpisuje zwykly actionbar — piszemy przez handler MythicLib z wyzszym priorytetem
    MMOPlayerData.get(p).getActionBar().show(50, tekst);
  }

  /** @return komunikat dla gracza (co sie stalo). */
  String przypisz(Player p, String id, boolean prawa) {
    PlayerInventory inv = p.getInventory();
    if (!prawa) {
      ItemStack teraz = inv.getItemInOffHand();
      if (!teraz.getType().isAir() && !jest(teraz)) {
        if (!inv.addItem(teraz.clone()).isEmpty()) return "§cZwolnij miejsce w ekwipunku — lewa dłoń jest zajęta.";
        inv.setItemInOffHand(null);
      }
      if (id == null) { inv.setItemInOffHand(null); return "§7Lewa dłoń jest pusta."; }
      Ksiega.Zaklecie z = ksiega.poId.get(id);
      if (z == null) return "§cNieznane zaklęcie: " + id;
      inv.setItemInOffHand(zrob(z));
      return "§f" + z.nazwa() + " §7w lewej dłoni.";
    }
    // prawa: jeden nosnik w pasku/plecaku naraz — stary znika, nowy wchodzi w jego miejsce albo w wolny slot
    int miejsce = -1;
    for (int s = 0; s < 36; s++) if (jest(inv.getItem(s))) { inv.setItem(s, null); if (s < 9 && miejsce < 0) miejsce = s; }
    if (id == null) return "§7Prawa dłoń jest pusta.";
    Ksiega.Zaklecie z = ksiega.poId.get(id);
    if (z == null) return "§cNieznane zaklęcie: " + id;
    if (miejsce < 0) {
      int wybrany = inv.getHeldItemSlot();
      ItemStack w = inv.getItem(wybrany);
      if (w == null || w.getType().isAir()) miejsce = wybrany;
      else for (int s = 0; s < 8 && miejsce < 0; s++) {   // slot 9 (8) to Dziennik — stale sloty
        ItemStack x = inv.getItem(s);
        if (x == null || x.getType().isAir()) miejsce = s;
      }
    }
    if (miejsce < 0) return "§cZwolnij miejsce w pasku, żeby wziąć zaklęcie do prawej dłoni.";
    inv.setItem(miejsce, zrob(z));
    inv.setHeldItemSlot(miejsce);
    return "§f" + z.nazwa() + " §7w prawej dłoni.";
  }

  private ItemStack zrob(Ksiega.Zaklecie z) {
    ItemStack i = MythicBukkit.inst().getItemManager().getItemStack(z.przedmiot());
    if (i == null) throw new IllegalStateException("brak przedmiotu MythicMobs " + z.przedmiot());
    i.editMeta(m -> m.getPersistentDataContainer().set(klucz, PersistentDataType.BYTE, (byte) 1));
    return i;
  }

  void komunikat(Player p, String tekst) { pasek(p, tekst); }

  // ------------------------------------------------------------------ blokady

  private void odmowa(Player p) { pasek(p, "§7Zaklęcie jest związane z dłonią — zmienisz je w §fKsiędze zaklęć§7."); }

  @EventHandler(priority = EventPriority.LOW)
  public void klik(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player p)) return;
    boolean dotyka = jest(e.getCurrentItem()) || jest(e.getCursor());
    if (e.getClick() == ClickType.NUMBER_KEY && e.getHotbarButton() >= 0) dotyka |= jest(p.getInventory().getItem(e.getHotbarButton()));
    if (e.getClick() == ClickType.SWAP_OFFHAND) dotyka |= jest(p.getInventory().getItemInOffHand());
    if (dotyka) { e.setCancelled(true); odmowa(p); }
  }

  @EventHandler(priority = EventPriority.LOW)
  public void przeciagniecie(InventoryDragEvent e) {
    if (jest(e.getOldCursor()) || e.getNewItems().values().stream().anyMatch(this::jest)) e.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void wyrzucenie(PlayerDropItemEvent e) {
    if (jest(e.getItemDrop().getItemStack())) { e.setCancelled(true); odmowa(e.getPlayer()); }
  }

  @EventHandler
  public void smierc(PlayerDeathEvent e) { e.getDrops().removeIf(this::jest); }

  @EventHandler(priority = EventPriority.LOW)
  public void istota(PlayerInteractEntityEvent e) {
    if (!(e.getRightClicked() instanceof ItemFrame || e.getRightClicked() instanceof Allay)) return;
    if (jest(reka(e.getPlayer(), e.getHand()))) e.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.LOW)
  public void stojak(PlayerArmorStandManipulateEvent e) { if (jest(e.getPlayerItem())) e.setCancelled(true); }

  @EventHandler(priority = EventPriority.LOW)
  public void blok(PlayerInteractEvent e) {
    if (e.getAction() != Action.RIGHT_CLICK_BLOCK || !jest(e.getItem())) return;
    Block b = e.getClickedBlock();
    if (b != null && (BLOKI_BIORACE.contains(b.getType()) || b.getType().name().endsWith("_SHELF")))
      e.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
  }

  /** Consumable 3600 s: gdyby ktos wytrzymal godzine, nosnik nie moze sie "zjesc". */
  @EventHandler(priority = EventPriority.LOW)
  public void zjedzenie(PlayerItemConsumeEvent e) { if (ksiega.w(e.getItem()) != null) e.setCancelled(true); }

  private static ItemStack reka(Player p, EquipmentSlot h) {
    return h == EquipmentSlot.OFF_HAND ? p.getInventory().getItemInOffHand() : p.getInventory().getItemInMainHand();
  }
}
