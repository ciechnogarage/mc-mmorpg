package pl.mcmmorpg.zaklecia;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.Indyuce.mmocore.api.event.PlayerEnterCastingModeEvent;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
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
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Szybki wybor jak ulubione w Skyrimie: Shift+Q wlacza pasek wszystkich zaklec Ksiegi (ulubione ★ na poczatku) jedna linia
 * w pasku akcji nad hotbarem (bez okna — gracz dalej chodzi i patrzy), Shift+Q albo Q wylacza, udany wybor tez. Gdy wlaczony: kolko
 * (albo klawisz 1-9) wybiera pozycje, LPM wklada ja do PRAWEJ dloni, PPM do LEWEJ (jak w vanilli). Po 15 s bez ruchu
 * zamyka sie sam.
 * Do 15.11 pasek byl dwoma BossBarami u gory; nachodzil na karte gracza MythicHUD (przy GUI 4 i 1080p u gory nie ma
 * miejsca) — decyzja wlasciciela 15.11 „Nad hotbar”.
 * Przy okazji: Q (wyrzucenie) nad slotem w oknie DeluxeMenus zamyka to okno — Q wlacza i wylacza menu.
 */
final class Wybor implements Listener {
  private static final LegacyComponentSerializer L = LegacyComponentSerializer.legacySection();
  // Priorytety arbitra paska akcji MythicLib (NORMAL 30, HIGH 40): lista ponad zwyklymi komunikatami,
  // komunikat przypisania (40) chwile ponad lista, ruch kolkiem (41) od razu przywraca liste.
  private static final int PRIO_LISTA = 35, PRIO_KOMUNIKAT = 40, PRIO_RUCH = 41;
  private static final long TIKI_LISTY = 30L, TIKI_KOMUNIKATU = 30L;
  private static final class Sesja { int i; String tekst; long ostatnio; }

  private final Zaklecia plugin;
  private final Ksiega ksiega;
  private final Nosnik nosnik;
  private final Ulubione ulubione;
  private final Rzucanie rzucanie;
  private final Map<UUID, Sesja> otwarte = new HashMap<>();

  Wybor(Zaklecia plugin, Ksiega ksiega, Nosnik nosnik, Ulubione ulubione, Rzucanie rzucanie) {
    this.plugin = plugin; this.ksiega = ksiega; this.nosnik = nosnik; this.ulubione = ulubione; this.rzucanie = rzucanie;
    nosnik.wymusPusta = this::otwarty;
    Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      long t = System.currentTimeMillis();
      for (UUID u : List.copyOf(otwarte.keySet())) {
        Player p = Bukkit.getPlayer(u);
        Sesja s = otwarte.get(u);
        if (p == null || t - s.ostatnio > 15_000) zamknij(p, u);
        else if (s.tekst != null) Nosnik.pasek(p, s.tekst, PRIO_LISTA, TIKI_LISTY);   // klient gasi pasek akcji po ~3 s
      }
    }, 20, 20);
  }

  /** Raz na gracza: dopoki nie przypisze nic z paska, podpowiedz klawiszy idzie na czat (w pasku akcji brak miejsca). */
  private NamespacedKey kluczUmie() { return new NamespacedKey(plugin, "pasek_wyboru_umie"); }

  boolean otwarty(Player p) { return otwarte.containsKey(p.getUniqueId()); }

  void przelacz(Player p) { if (otwarty(p)) zamknij(p, p.getUniqueId()); else otworz(p); }

  /** Metadane gracza ustawiane przez Chowance na czas walki turowej i pojedynku (klawisze 1-9 = ruchy chowanca). */
  static final String WALKA_CHOWANCOW = "chowance_walka";

  void otworz(Player p) {
    // Jeden tryb na klawiszach 1-9 naraz (decyzja wlasciciela 15.11): bramka klasy, walka Chowancow blokuje,
    // nowy pasek zamyka tryb skilli MMOCore i pelna mape.
    if (!plugin.bramka.wolno(p)) { nosnik.komunikat(p, Bramka.ODMOWA); return; }
    if (p.hasMetadata(WALKA_CHOWANCOW)) { nosnik.komunikat(p, "§cW walce chowańców pasek zaklęć jest niedostępny."); return; }
    try {
      PlayerData d = PlayerData.get(p);
      if (d != null && d.isCasting()) d.leaveSkillCasting();
    } catch (RuntimeException ignored) { /* dane MMOCore niewczytane (zmiana postaci) */ }
    if (Bukkit.getPluginManager().isPluginEnabled("Mapa"))
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mapa zamknij " + p.getName());
    rzucanie.stop(p, false);
    Sesja s = new Sesja();
    Ksiega.Zaklecie wPrawej = ksiega.w(p.getInventory().getItemInMainHand());
    List<String> k = ulubione.kolo(p);
    s.i = Math.max(0, wPrawej == null ? 0 : k.indexOf(wPrawej.id()));
    s.ostatnio = System.currentTimeMillis();
    otwarte.put(p.getUniqueId(), s);
    if (plugin.getConfig().getBoolean("debug", false)) plugin.getLogger().info("ZAKDBG pasek open gracz=" + p.getName());
    nosnik.odswiezPusta(p);   // PPM z pusta reka nie dociera do serwera — od razu nosnik w dloni
    odswiez(p, s, PRIO_RUCH);
    podpowiedz(p);
    p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);
  }

  void zamknij(Player p, UUID u) {
    Sesja s = otwarte.remove(u);
    if (s != null && plugin.getConfig().getBoolean("debug", false)) plugin.getLogger().info("ZAKDBG pasek close gracz="
        + (p == null ? u : p.getName()) + " online=" + (p != null && p.isOnline()));
    if (s != null && p != null) {
      if (p.isOnline()) Nosnik.pasek(p, " ", PRIO_RUCH, 1L);   // zgas liste od razu, nie po ~3 s zanikania
      p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.6f, 0.8f);
    }
  }

  private void odswiez(Player p, Sesja s, int priorytet) {
    List<String> k = ulubione.kolo(p);
    List<String> ul = ulubione.lista(p);
    s.i = Math.floorMod(s.i, k.size());
    // Trzy pozycje wystarcza nawet przy dlugich nazwach i 15+ zakleciach; cala linia ~60 znakow miesci sie
    // nad hotbarem przy GUI 4 i 1080p (480 jednostek GUI szerokosci).
    int od = Math.max(0, Math.min(s.i - 1, k.size() - 3));
    int doIndeks = Math.min(k.size(), od + 3);
    StringBuilder okno = new StringBuilder(od > 0 ? "§7‹ " : "§8  ");
    for (int j = od; j < doIndeks; j++) {
      if (j > od) okno.append(" §8· ");
      String id = k.get(j);
      String n = id == null ? "pusta" : (ul.contains(id) ? "★ " : "") + ksiega.poId.get(id).nazwa();
      okno.append(j == s.i ? "§f§l[" + n + "]§r" : "§7" + n);
    }
    okno.append(doIndeks < k.size() ? " §7›" : " §8 ");
    String id = k.get(s.i);
    Ksiega.Zaklecie z = id == null ? null : ksiega.poId.get(id);
    String opis = z == null ? "§7bez kosztu" : "§7" + typ(z) + " §b" + koszt(z) + " many" + (z.koncentracja() ? "/s" : "");
    s.tekst = "§5✦ " + okno + "  §8│  " + opis;
    Nosnik.pasek(p, s.tekst, priorytet, TIKI_LISTY);
  }

  private static String kolor(String szkola) {
    return switch (szkola) {
      case "ogien" -> "§6";
      case "mroz" -> "§b";
      case "leczenie" -> "§a";
      case "przywolanie" -> "§d";
      case "iluzja" -> "§5";
      default -> "§9";
    };
  }

  private static String typ(Ksiega.Zaklecie z) {
    return switch (z.typ()) {
      case "koncentracja" -> z.cel().equals("stozek") ? "strumień" : "leczenie";
      case "ladowany" -> z.cel().equals("stozek") ? "pocisk" : "leczenie";
      case "runa" -> "runa";
      case "tarcza" -> "tarcza";
      case "przywolanie" -> "przywołanie";
      case "bron" -> "broń";
      case "na_cel" -> "na cel";
      default -> z.typ();
    };
  }

  /** Koszt dla gracza: strumienie/tarcze w many na sekunde (tik = 10 tickow = 0,5 s), jednorazowe w many za rzut. */
  private static String koszt(Ksiega.Zaklecie z) {
    double k = z.koncentracja() ? z.manaZaTik() * 2 : z.mana();
    return k == Math.rint(k) ? String.valueOf((long) k) : String.format(java.util.Locale.ROOT, "%.1f", k).replace('.', ',');
  }


  private void podpowiedz(Player p) {
    if (p.getPersistentDataContainer().has(kluczUmie(), PersistentDataType.BYTE)) return;
    p.sendMessage(L.deserialize("§5✦ §7Pasek zaklęć: §ekółko§7 wybór · §eLPM§7 → prawa dłoń · §ePPM§7 → lewa · §eQ§7 zamknij"));
  }


  private void przypisz(Player p, boolean prawa) {
    Sesja s = otwarte.get(p.getUniqueId());
    if (s == null) return;
    List<String> k = ulubione.kolo(p);
    String id = k.get(Math.floorMod(s.i, k.size()));
    String wynik = nosnik.przypisz(p, id, prawa);
    boolean sukces = !wynik.startsWith("§c");
    s.ostatnio = System.currentTimeMillis();
    // Udany wybor zamyka pasek (16.11: otwarty pasek po przypisaniu + drugie Shift+Q, ktore go zamykalo, dawaly
    // „nie da się zdjąć” — kolko zmienialo slot, PPM rzucal zamiast zdejmowac). Odmowa zostawia pasek otwarty.
    if (sukces) zamknij(p, p.getUniqueId()); else odswiez(p, s, PRIO_LISTA);
    // „§7… dłoń jest pusta.” = zdjecie (takze ponownym wyborem tego samego zaklecia w tej dloni)
    Nosnik.pasek(p, sukces && wynik.startsWith("§7") ? wynik : sukces ? "§f" + (id == null ? "Pusta" : ksiega.poId.get(id).nazwa())
        + " §7→ " + (prawa ? "prawa" : "lewa") + " dłoń" : wynik,
        sukces ? PRIO_RUCH : PRIO_KOMUNIKAT, TIKI_KOMUNIKATU);   // po zamknieciu (zgaszenie 41) komunikat musi przejsc
    if (sukces) {
      p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.7f, 1.3f);
      p.getPersistentDataContainer().set(kluczUmie(), PersistentDataType.BYTE, (byte) 1);
    }
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
    odswiez(e.getPlayer(), s, PRIO_RUCH);
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
      Sesja kliknietaSesja = otwarte.get(p.getUniqueId());
      Bukkit.getScheduler().runTaskLater(plugin, () -> {
        if (p.isOnline() && otwarte.get(p.getUniqueId()) == kliknietaSesja
            && !rzucanie.swiezeQ(p)) wcisnietoLpm(p);
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
    if (e instanceof io.lumine.mythic.lib.api.event.fake.FakeEntityDamageByEntityEvent) return;
    if (!(e.getDamager() instanceof Player p) || !otwarty(p)) return;
    // tylko prawdziwy cios wrecz; obrazenia zaklec (mmodamage) tez maja gracza jako sprawce
    if (e.getCause() != org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK || rzucanie.trybLpm(p)) return;
    e.setCancelled(true); wcisnietoLpm(p);
  }

  @EventHandler
  public void wyjscie(PlayerQuitEvent e) { zamknij(e.getPlayer(), e.getPlayer().getUniqueId()); }

  /** Tryb skilli MMOCore (F) zamyka pasek: nowy tryb zamyka stary. */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void trybSkilli(PlayerEnterCastingModeEvent e) {
    Player p = e.getPlayer();
    if (otwarty(p)) zamknij(p, p.getUniqueId());
  }

  @EventHandler
  public void smierc(PlayerDeathEvent e) { zamknij(e.getEntity(), e.getEntity().getUniqueId()); }

  @EventHandler
  public void zmianaSwiata(PlayerChangedWorldEvent e) { zamknij(e.getPlayer(), e.getPlayer().getUniqueId()); }

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
