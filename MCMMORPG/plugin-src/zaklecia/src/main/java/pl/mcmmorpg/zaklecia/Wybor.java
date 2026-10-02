package pl.mcmmorpg.zaklecia;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Szybki wybor jak ulubione w Skyrimie: Shift+Q wlacza pasek ulubionych u gory ekranu (BossBar, bez okna — gracz dalej
 * chodzi i patrzy), Shift+Q albo Q wylacza. Gdy wlaczony: kolko (albo klawisz 1-9) wybiera pozycje, LPM wklada ja do
 * PRAWEJ dloni, PPM do LEWEJ (jak w vanilli). Po 15 s bez ruchu zamyka sie sam.
 * Przy okazji: Q (wyrzucenie) nad slotem w oknie DeluxeMenus zamyka to okno — Q wlacza i wylacza menu.
 */
final class Wybor implements Listener {
  private static final LegacyComponentSerializer L = LegacyComponentSerializer.legacySection();
  private static final class Sesja { int i; BossBar bar; long ostatnio; }

  private final Zaklecia plugin;
  private final Ksiega ksiega;
  private final Nosnik nosnik;
  private final Ulubione ulubione;
  private final Rzucanie rzucanie;
  private final Map<UUID, Sesja> otwarte = new HashMap<>();

  Wybor(Zaklecia plugin, Ksiega ksiega, Nosnik nosnik, Ulubione ulubione, Rzucanie rzucanie) {
    this.plugin = plugin; this.ksiega = ksiega; this.nosnik = nosnik; this.ulubione = ulubione; this.rzucanie = rzucanie;
    Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      long t = System.currentTimeMillis();
      for (UUID u : List.copyOf(otwarte.keySet())) {
        Player p = Bukkit.getPlayer(u);
        if (p == null || t - otwarte.get(u).ostatnio > 15_000) zamknij(p, u);
      }
    }, 20, 20);
  }

  boolean otwarty(Player p) { return otwarte.containsKey(p.getUniqueId()); }

  void przelacz(Player p) { if (otwarty(p)) zamknij(p, p.getUniqueId()); else otworz(p); }

  void otworz(Player p) {
    rzucanie.stop(p, false);
    Sesja s = new Sesja();
    Ksiega.Zaklecie wPrawej = ksiega.w(p.getInventory().getItemInMainHand());
    List<String> k = ulubione.kolo(p);
    s.i = Math.max(0, wPrawej == null ? 0 : k.indexOf(wPrawej.id()));
    s.bar = BossBar.bossBar(L.deserialize(""), 1f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
    s.ostatnio = System.currentTimeMillis();
    otwarte.put(p.getUniqueId(), s);
    p.showBossBar(s.bar);
    odswiez(p, s);
    p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);
  }

  void zamknij(Player p, UUID u) {
    Sesja s = otwarte.remove(u);
    if (s != null && p != null) { p.hideBossBar(s.bar); p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.6f, 0.8f); }
  }

  private void odswiez(Player p, Sesja s) {
    List<String> k = ulubione.kolo(p);
    s.i = Math.floorMod(s.i, k.size());
    String dlonie = "§7L: §f" + nazwa(ksiega.w(p.getInventory().getItemInOffHand())) + "  §7P: §f" + nazwa(ksiega.w(p.getInventory().getItemInMainHand()));
    s.bar.name(L.deserialize("§5✦ §f" + ulubione.pasek(p, k, k.get(s.i)) + "   §8│ " + dlonie
        + "   §8│ §7kółko: wybór · §eLPM§7: prawa · §ePPM§7: lewa · §eQ§7: zamknij"));
  }

  private static String nazwa(Ksiega.Zaklecie z) { return z == null ? "pusta" : z.nazwa(); }

  private void przypisz(Player p, boolean prawa) {
    Sesja s = otwarte.get(p.getUniqueId());
    if (s == null) return;
    List<String> k = ulubione.kolo(p);
    String id = k.get(Math.floorMod(s.i, k.size()));
    nosnik.komunikat(p, nosnik.przypisz(p, id, prawa));
    p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.7f, 1.3f);
    s.ostatnio = System.currentTimeMillis();
    odswiez(p, s);
  }

  void wcisnietoLpm(Player p) { przypisz(p, true); }
  void wcisnietoPpm(Player p) { przypisz(p, false); }

  // ------------------------------------------------------------------ wejscie gracza przy otwartym pasku

  @EventHandler(priority = EventPriority.LOWEST)
  public void kolko(PlayerItemHeldEvent e) {
    Sesja s = otwarte.get(e.getPlayer().getUniqueId());
    if (s == null) return;
    e.setCancelled(true);
    int r = (e.getNewSlot() - e.getPreviousSlot() + 9) % 9;
    if (r == 1) s.i++; else if (r == 8) s.i--; else s.i = e.getNewSlot();   // klawisz 1-9 = pozycja wprost
    s.ostatnio = System.currentTimeMillis();
    e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.8f);
    odswiez(e.getPlayer(), s);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void klik(PlayerInteractEvent e) {
    if (!otwarty(e.getPlayer()) || e.getHand() == EquipmentSlot.OFF_HAND) return;
    boolean prawy = e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK;
    boolean lewy = e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK;
    if (!prawy && !lewy) return;
    // Machniecie po PPM Paper moze zglosic jako LEFT_CLICK_AIR.
    if (e.getAction() == Action.LEFT_CLICK_AIR && rzucanie.swiezyPpm(e.getPlayer())) { e.setCancelled(true); return; }
    if (e.getAction() == Action.LEFT_CLICK_AIR) {   // moze to machniecie po klawiszu Q (zamykajacym pasek) — sprawdz pozniej
      e.setCancelled(true);
      Player p = e.getPlayer();
      Bukkit.getScheduler().runTaskLater(plugin, () -> {
        if (p.isOnline() && otwarty(p) && !rzucanie.swiezeQ(p)) wcisnietoLpm(p);
      }, Rzucanie.Q_OKNO);
      return;
    }
    e.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
    e.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
    if (prawy) wcisnietoPpm(e.getPlayer()); else wcisnietoLpm(e.getPlayer());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void kliknietaIstota(PlayerInteractEntityEvent e) {
    if (!otwarty(e.getPlayer()) || e.getHand() != EquipmentSlot.HAND) return;
    e.setCancelled(true); wcisnietoPpm(e.getPlayer());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void uderzenie(EntityDamageByEntityEvent e) {
    if (!(e.getDamager() instanceof Player p) || !otwarty(p)) return;
    // tylko prawdziwy cios wrecz; obrazenia zaklec (mmodamage) tez maja gracza jako sprawce
    if (e.getCause() != org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK || rzucanie.trybLpm(p)) return;
    e.setCancelled(true); wcisnietoLpm(p);
  }

  @EventHandler
  public void wyjscie(PlayerQuitEvent e) { otwarte.remove(e.getPlayer().getUniqueId()); }

  /** Q nad slotem okna DeluxeMenus = zamknij okno (Q wlacza i wylacza menu). */
  @EventHandler(priority = EventPriority.LOWEST)
  public void qWMenu(InventoryClickEvent e) {
    if (e.getClick() != ClickType.DROP && e.getClick() != ClickType.CONTROL_DROP) return;
    Object h = e.getView().getTopInventory().getHolder(false);
    if (h == null || !h.getClass().getName().toLowerCase().contains("deluxemenus")) return;
    e.setCancelled(true);
    Bukkit.getScheduler().runTask(plugin, () -> e.getWhoClicked().closeInventory());
  }
}
