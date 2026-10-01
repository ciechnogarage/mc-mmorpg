package pl.mcmmorpg.zaklecia;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.item.AttackRange;
import io.papermc.paper.datacomponent.item.SwingAnimation;
import io.papermc.paper.datacomponent.item.Tool;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.DataComponentTypeKeys;
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
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
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
  private final Zaklecia plugin;
  private final NamespacedKey klucz;
  private final NamespacedKey kluczPustej;
  private final DataComponentType debugStickState;
  /** Slot paska prawej dloni z zakleciem — zostaje po odlozeniu kolkiem ("pusta"), zeby kolkiem wrocic. */
  private final NamespacedKey kluczSlotu;
  /** Lewa dlon byla uzywana do zaklec (Shift+kolko dziala tez przy pustej lewej). */
  private final NamespacedKey kluczLewej;
  /** Bloki, ktore przyjmuja albo zamieniaja przedmiot z reki. */
  private static final Set<Material> BLOKI_BIORACE = Set.of(Material.DECORATED_POT, Material.FLOWER_POT,
      Material.VAULT, Material.CHISELED_BOOKSHELF, Material.LECTERN, Material.JUKEBOX);

  Nosnik(Zaklecia plugin, Ksiega ksiega) {
    this.ksiega = ksiega;
    this.plugin = plugin;
    this.klucz = new NamespacedKey(plugin, "nosnik");
    this.kluczPustej = new NamespacedKey(plugin, "pusta_dlon");
    this.kluczSlotu = new NamespacedKey(plugin, "slot_zaklec");
    this.kluczLewej = new NamespacedKey(plugin, "lewa_zaklec");
    this.debugStickState = RegistryAccess.registryAccess()
        .getRegistry(RegistryKey.DATA_COMPONENT_TYPE).getOrThrow(DataComponentTypeKeys.DEBUG_STICK_STATE);
    org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, this::odswiezPuste, 1, 1);
  }

  boolean jest(ItemStack i) {
    return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(klucz, PersistentDataType.BYTE);
  }

  boolean pusta(ItemStack i) {
    return i != null && i.hasItemMeta()
        && i.getItemMeta().getPersistentDataContainer().has(kluczPustej, PersistentDataType.BYTE);
  }

  ItemStack zrobPusta() {
    ItemStack i = MythicBukkit.inst().getItemManager().getItemStack("ZAKLECIE_PUSTA");
    if (i == null) throw new IllegalStateException("brak przedmiotu MythicMobs ZAKLECIE_PUSTA");
    i.editMeta(m -> {
      m.getPersistentDataContainer().set(klucz, PersistentDataType.BYTE, (byte) 1);
      m.getPersistentDataContainer().set(kluczPustej, PersistentDataType.BYTE, (byte) 1);
    });
    komponenty(i);
    return i;
  }

  /** Niewidoczny nosnik jest wylacznie w aktualnie wybranym pustym slocie przy czarze w lewej. */
  void odswiezPusta(Player p) {
    PlayerInventory inv = p.getInventory();
    int wybrany = inv.getHeldItemSlot();
    for (int s = 0; s < 9; s++) if (s != wybrany && pusta(inv.getItem(s))) inv.setItem(s, null);
    ItemStack w = inv.getItem(wybrany);
    if (jest(w) && w.getType() != Material.DEBUG_STICK) {
      Ksiega.Zaklecie stare = ksiega.w(w);
      if (stare != null) { w = zrob(stare); inv.setItem(wybrany, w); }
    }
    ItemStack lewa = inv.getItemInOffHand();
    if (jest(lewa) && lewa.getType() != Material.DEBUG_STICK) {
      Ksiega.Zaklecie stare = ksiega.w(lewa);
      if (stare != null) inv.setItemInOffHand(zrob(stare));
    }
    if (ksiega.w(inv.getItemInOffHand()) != null) {
      if (w == null || w.getType().isAir()) inv.setItem(wybrany, zrobPusta());
    } else if (pusta(w)) inv.setItem(wybrany, null);
  }

  private void odswiezPuste() { for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) if (!p.isDead()) odswiezPusta(p); }

  void usunPuste(Player p) {
    PlayerInventory inv = p.getInventory();
    for (int s = 0; s < 36; s++) if (pusta(inv.getItem(s))) inv.setItem(s, null);
  }

  private static void pasek(Player p, String tekst) {
    // pasek MMOCore co 5 tickow nadpisuje zwykly actionbar — piszemy przez handler MythicLib z wyzszym priorytetem
    // Dane MMO bywaja nieaktualne po przelogowaniu w <1 s (MMOCore: "Player is offline") — wtedy zwykly pasek.
    try { MMOPlayerData.get(p).getActionBar().show(50, tekst); } catch (Throwable t) { p.sendActionBar(net.kyori.adventure.text.Component.text(tekst.replaceAll("§.", ""))); }
  }

  /** Slot paska prawej dloni z zakleciem albo -1. */
  int slotZaklec(Player p) { return p.getPersistentDataContainer().getOrDefault(kluczSlotu, PersistentDataType.INTEGER, -1); }
  void ustawSlot(Player p, int s) {
    if (s < 0) p.getPersistentDataContainer().remove(kluczSlotu);
    else p.getPersistentDataContainer().set(kluczSlotu, PersistentDataType.INTEGER, s);
  }

  boolean lewaUzywana(Player p) { return p.getPersistentDataContainer().has(kluczLewej, PersistentDataType.BYTE); }
  void ustawLewa(Player p, boolean u) {
    if (u) p.getPersistentDataContainer().set(kluczLewej, PersistentDataType.BYTE, (byte) 1);
    else p.getPersistentDataContainer().remove(kluczLewej);
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
      if (id == null) { inv.setItemInOffHand(null); ustawLewa(p, false); usunPuste(p); return "§7Lewa dłoń jest pusta."; }
      Ksiega.Zaklecie z = ksiega.poId.get(id);
      if (z == null) return "§cNieznane zaklęcie: " + id;
      inv.setItemInOffHand(zrob(z));
      ustawLewa(p, true);
      odswiezPusta(p);
      return "§f" + z.nazwa() + " §7w lewej dłoni.";
    }
    // prawa: jeden nosnik w pasku/plecaku naraz — stary znika, nowy wchodzi w jego miejsce albo w wolny slot
    int miejsce = -1;
    for (int s = 0; s < 36; s++) if (jest(inv.getItem(s))) { inv.setItem(s, null); if (s < 9 && miejsce < 0) miejsce = s; }
    if (id == null) { ustawSlot(p, -1); odswiezPusta(p); return "§7Prawa dłoń jest pusta."; }
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
    ustawSlot(p, miejsce);
    odswiezPusta(p);
    return "§f" + z.nazwa() + " §7w prawej dłoni.";
  }

  ItemStack zrob(Ksiega.Zaklecie z) {
    ItemStack i = MythicBukkit.inst().getItemManager().getItemStack(z.przedmiot());
    if (i == null) throw new IllegalStateException("brak przedmiotu MythicMobs " + z.przedmiot());
    i.editMeta(m -> m.getPersistentDataContainer().set(klucz, PersistentDataType.BYTE, (byte) 1));
    komponenty(i);
    return i;
  }

  private void komponenty(ItemStack i) {
    i.unsetData(DataComponentTypes.CONSUMABLE);
    i.unsetData(DataComponentTypes.USE_EFFECTS);
    i.unsetData(debugStickState);
    i.setData(DataComponentTypes.SWING_ANIMATION,
        SwingAnimation.swingAnimation().type(SwingAnimation.Animation.NONE));
    i.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false);
    i.setData(DataComponentTypes.TOOL, Tool.tool().defaultMiningSpeed(1_000_000f));
    // Klient (LocalPlayer.raycastHitResult) z attack_range szuka istot dopiero od min_reach: przy 64 celownik przechodzi
    // przez moby do terenu za nimi, wiec LPM trzymany na wrogu dalej "kopie" blok za nim (bez tego: pudlo = impuls).
    i.setData(DataComponentTypes.ATTACK_RANGE, AttackRange.attackRange().minReach(64f).maxReach(64f)
        .minCreativeReach(64f).maxCreativeReach(64f).hitboxMargin(0f).build());
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
    if (!jest(e.getItemDrop().getItemStack())) return;
    e.setCancelled(true);   // Q to klawisz menu — bez komunikatu; klient przewidzial wyrzucenie, wiec odswiezamy slot
    Player p = e.getPlayer();
    org.bukkit.Bukkit.getScheduler().runTask(plugin, p::updateInventory);
  }

  @EventHandler
  public void smierc(PlayerDeathEvent e) { e.getDrops().removeIf(this::jest); usunPuste(e.getEntity()); }

  @EventHandler
  public void wyjscie(PlayerQuitEvent e) { usunPuste(e.getPlayer()); }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void zmianaSlotu(PlayerItemHeldEvent e) {
    PlayerInventory inv = e.getPlayer().getInventory();
    if (pusta(inv.getItem(e.getPreviousSlot()))) inv.setItem(e.getPreviousSlot(), null);
    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
      if (e.getPlayer().isOnline() && !e.getPlayer().isDead()) odswiezPusta(e.getPlayer());
    });
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void zamianaRak(PlayerSwapHandItemsEvent e) {
    if (jest(e.getMainHandItem()) || jest(e.getOffHandItem())) e.setCancelled(true);
  }

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

  private static ItemStack reka(Player p, EquipmentSlot h) {
    return h == EquipmentSlot.OFF_HAND ? p.getInventory().getItemInOffHand() : p.getInventory().getItemInMainHand();
  }
}
