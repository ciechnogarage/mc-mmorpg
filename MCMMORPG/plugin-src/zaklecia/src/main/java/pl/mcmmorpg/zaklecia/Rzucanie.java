package pl.mcmmorpg.zaklecia;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.lib.player.resource.ResourceUpdateReason;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * STRUCTURE_VOID ma postep kopania bez przejscia w isDestroying, wiec klient moze
 * wysylac LPM i PPM rownolegle. Znaczniki przyciskow gasna niezaleznie.
 */
final class Rzucanie implements Listener {
  static final class Stan { int n; boolean prawa, lewa; }

  private final Zaklecia plugin;
  private final Ksiega ksiega;
  private final Strumien strumien;
  private final Map<UUID, Stan> rzucajacy = new HashMap<>();
  private final Map<UUID, Long> lpmBlok = new HashMap<>(), lpmPowietrze = new HashMap<>();
  private final Map<UUID, Long> ppmPrawa = new HashMap<>(), ppmLewa = new HashMap<>();
  private long teraz;
  /** Tick ostatniego PPM gracza: machniecie reka po PPM Paper zglasza tez jako LEFT_CLICK_AIR — ignorujemy 6 tickow. */
  private final Map<UUID, Long> ostatniPpm = new HashMap<>();

  boolean swiezyPpm(Player p) {
    Long t = ostatniPpm.get(p.getUniqueId());
    return t != null && teraz - t <= plugin.getConfig().getInt("ppm-tolerancja", 6);
  }
  private Wybor wybor;   // szybki wybor (Shift+Q) — gdy otwarty, LPM/PPM przypisuja, nie rzucaja

  private final NamespacedKey kZasieg, kIstoty, kSzybkosc, kRuch, kNosnik, kStareKopanie;

  Rzucanie(Zaklecia plugin, Ksiega ksiega) {
    this.plugin = plugin;
    this.ksiega = ksiega;
    this.strumien = new Strumien(plugin, ksiega);
    kZasieg = new NamespacedKey(plugin, "lpm_zasieg");
    kIstoty = new NamespacedKey(plugin, "lpm_istoty");
    kSzybkosc = new NamespacedKey(plugin, "lpm_szybkosc");
    kRuch = new NamespacedKey(plugin, "rzucanie_ruch");
    kNosnik = new NamespacedKey(plugin, "nosnik");
    kStareKopanie = new NamespacedKey(plugin, "lpm_kopanie");
    Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
    Bukkit.getScheduler().runTaskTimer(plugin, this::dlonie, 4, 4);
    Bukkit.getScheduler().runTaskTimer(plugin, this::trybyLpm, 1, 1);
  }

  void ustawWybor(Wybor w) { this.wybor = w; }

  private void log(Player p, String co) {
    if (plugin.getConfig().getBoolean("debug-wejscie", false)) plugin.getLogger().info("[wejscie] t=" + teraz + " " + p.getName() + " " + co);
  }

  /** Wylaczenie pluginu: zdejmij strumienie i przejsciowe atrybuty. */
  void wylacz() {
    strumien.stopWszystkie(); rzucajacy.clear();
    for (Player p : Bukkit.getOnlinePlayers()) { atrybutyLpm(p, false); ruch(p, false); }
  }

  boolean rzuca(Player p) { return rzucajacy.containsKey(p.getUniqueId()); }

  /** Czy dana dlon jest aktywna (w trakcie rzucania); poza rzucaniem — czy trzyma zaklecie. Dla warunkow MM. */
  boolean aktywna(Player p, boolean prawa) {
    Stan s = rzucajacy.get(p.getUniqueId());
    if (s == null) return ksiega.w(prawa ? p.getInventory().getItemInMainHand() : p.getInventory().getItemInOffHand()) != null;
    return prawa ? s.prawa : s.lewa;
  }

  private void skill(Player p, String nazwa) {
    try { MythicBukkit.inst().getAPIHelper().castSkill(p, nazwa); }
    catch (Throwable t) { plugin.getLogger().log(Level.WARNING, "skill " + nazwa + ": " + t, t); }
  }

  // ------------------------------------------------------------------ tryb LPM

  /** Nosnik w prawej oznacza tryb zaklec, takze niewidoczna pusta dlon. */
  boolean trybLpm(Player p) {
    ItemStack prawa = p.getInventory().getItemInMainHand();
    return prawa.getType() == org.bukkit.Material.DEBUG_STICK && prawa.hasItemMeta()
        && prawa.getItemMeta().getPersistentDataContainer().has(kNosnik)
        && (ksiega.w(p.getInventory().getItemInMainHand()) != null
            || ksiega.w(p.getInventory().getItemInOffHand()) != null);
  }

  private void trybyLpm() { for (Player p : Bukkit.getOnlinePlayers()) atrybutyLpm(p, trybLpm(p)); }

  private void atrybutyLpm(Player p, boolean wl) {
    modDo(p, Attribute.BLOCK_INTERACTION_RANGE, kZasieg, plugin.getConfig().getDouble("lpm-zasieg-blokow", 8), wl);
    modDo(p, Attribute.ENTITY_INTERACTION_RANGE, kIstoty, 0, wl);
    modDo(p, Attribute.BLOCK_BREAK_SPEED, kSzybkosc, 1024, wl);
    // Usun pozostalosci po poprzedniej wersji podczas ponownego zaladowania.
    mod(p, Attribute.BLOCK_BREAK_SPEED, kStareKopanie, 0, AttributeModifier.Operation.ADD_NUMBER, false);
  }

  private void modDo(Player p, Attribute a, NamespacedKey k, double cel, boolean wl) {
    AttributeInstance ai = p.getAttribute(a);
    if (ai != null) mod(p, a, k, cel - ai.getBaseValue(), AttributeModifier.Operation.ADD_NUMBER, wl);
  }

  private void ruch(Player p, boolean wl) {
    mod(p, Attribute.MOVEMENT_SPEED, kRuch, plugin.getConfig().getDouble("spowolnienie", -0.5),
        AttributeModifier.Operation.MULTIPLY_SCALAR_1, wl);
  }

  private static void mod(Player p, Attribute a, NamespacedKey k, double v, AttributeModifier.Operation op, boolean wl) {
    AttributeInstance ai = p.getAttribute(a);
    if (ai == null) return;
    boolean jest = ai.getModifier(k) != null;
    if (wl && !jest) ai.addTransientModifier(new AttributeModifier(k, v, op, EquipmentSlotGroup.ANY));
    else if (!wl && jest) ai.removeModifier(k);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void lpm(PlayerInteractEvent e) {
    if (e.getHand() != EquipmentSlot.HAND || !trybLpm(e.getPlayer())) return;
    if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
      e.setCancelled(true);
      if (wybor == null || !wybor.otwarty(e.getPlayer())) wcisnietoLpm(e.getPlayer());
    } else if (e.getAction() == Action.LEFT_CLICK_AIR && !swiezyPpm(e.getPlayer())
        && (wybor == null || !wybor.otwarty(e.getPlayer()))) {
      // Klawisz Q tez macha reka (klient po wyrzuceniu) = LEFT_CLICK_AIR; MenuKey woła "zaklecia q" chwile pozniej.
      Player p = e.getPlayer();
      Bukkit.getScheduler().runTaskLater(plugin, () -> {
        if (!p.isOnline() || swiezeQ(p) || !trybLpm(p) || (wybor != null && wybor.otwarty(p))) return;
        lpmPowietrze.put(p.getUniqueId(), teraz);
        wejdz(p);
      }, Q_OKNO);
    }
  }

  /** Ticki, przez ktore machniecie reka uznajemy za skutek klawisza Q, a nie LPM. */
  static final int Q_OKNO = 3;
  private final Map<UUID, Long> ostatnieQ = new HashMap<>();
  void oznaczQ(Player p) { ostatnieQ.put(p.getUniqueId(), teraz); }
  boolean swiezeQ(Player p) { Long t = ostatnieQ.get(p.getUniqueId()); return t != null && teraz - t <= Q_OKNO + 1; }

  /** Ta sama sciezka dla realnego bloku i pakietu na kotwicy. Wybor w zdarzeniu obsluguje wlasny listener. */
  void wcisnietoLpm(Player p) {
    if (!trybLpm(p)) return;
    if (wybor != null && wybor.otwarty(p)) { wybor.wcisnietoLpm(p); return; }
    lpmBlok.put(p.getUniqueId(), teraz);
    wejdz(p);
  }

  void wcisnietoPpm(Player p) {
    if (!trybLpm(p)) return;
    ostatniPpm.put(p.getUniqueId(), teraz);
    if (wybor != null && wybor.otwarty(p)) { wybor.wcisnietoPpm(p); return; }
    if (ksiega.w(p.getInventory().getItemInMainHand()) != null) {
      ppmPrawa.put(p.getUniqueId(), teraz);
      wejdz(p);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void bezNiszczenia(BlockBreakEvent e) { if (trybLpm(e.getPlayer())) e.setCancelled(true); }

  @EventHandler(priority = EventPriority.LOWEST)
  public void uzycie(PlayerInteractEvent e) {
    if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    Player p = e.getPlayer();
    if (trybLpm(p)) {
      e.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
      e.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
      if (e.getHand() != EquipmentSlot.HAND) return;
      if (wybor == null || !wybor.otwarty(p)) wcisnietoPpm(p);
      else ostatniPpm.put(p.getUniqueId(), teraz);
    } else if (e.getHand() == EquipmentSlot.OFF_HAND
        && ksiega.w(e.getItem()) != null && (wybor == null || !wybor.otwarty(p))) {
      ppmLewa.put(p.getUniqueId(), teraz);
      wejdz(p);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void animacja(PlayerAnimationEvent e) { if (trybLpm(e.getPlayer())) e.setCancelled(true); }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void zmianaSlotu(PlayerItemHeldEvent e) {
    stop(e.getPlayer(), false);
    atrybutyLpm(e.getPlayer(), false);   // bez tego kilof wziety w tym samym ticku kopalby z predkoscia 1024; tik oddaje je nosnikowi
  }

  @EventHandler
  public void wyjscie(PlayerQuitEvent e) {
    UUID u = e.getPlayer().getUniqueId();
    rzucajacy.remove(u); lpmBlok.remove(u); lpmPowietrze.remove(u); ppmPrawa.remove(u); ppmLewa.remove(u); ostatniPpm.remove(u); ostatnieQ.remove(u);
    strumien.stop(e.getPlayer()); atrybutyLpm(e.getPlayer(), false); ruch(e.getPlayer(), false);
  }

  private void wejdz(Player p) { rzucajacy.putIfAbsent(p.getUniqueId(), new Stan()); }

  void stop(Player p, boolean dzwiek) {
    Stan s = rzucajacy.get(p.getUniqueId());
    lpmBlok.remove(p.getUniqueId()); lpmPowietrze.remove(p.getUniqueId());
    ppmPrawa.remove(p.getUniqueId()); ppmLewa.remove(p.getUniqueId());
    if (s == null) return;
    if (dzwiek && p.isOnline() && (s.prawa || s.lewa)) skill(p, ksiega.skStop);   // przed usunieciem: warunki widza dlonie
    rzucajacy.remove(p.getUniqueId());
    strumien.stop(p);
    ruch(p, false);
  }

  // ------------------------------------------------------------------ tik

  private boolean swiezy(Map<UUID, Long> m, Player p, int tolerancja) {
    Long t = m.get(p.getUniqueId());
    return t != null && teraz - t <= tolerancja;
  }

  private boolean prawaTeraz(Player p) {
    if (ksiega.w(p.getInventory().getItemInMainHand()) == null) return false;
    return trybLpm(p) && swiezy(ppmPrawa, p, plugin.getConfig().getInt("ppm-tolerancja", 6));
  }

  private boolean lewaTeraz(Player p) {
    if (ksiega.w(p.getInventory().getItemInOffHand()) == null) return false;
    if (!trybLpm(p)) return swiezy(ppmLewa, p, plugin.getConfig().getInt("ppm-tolerancja", 6));
    return swiezy(lpmBlok, p, plugin.getConfig().getInt("lpm-tolerancja", 3))
        || swiezy(lpmPowietrze, p, plugin.getConfig().getInt("lpm-impuls", 10));
  }

  /** Koszt many za tik z aktywnych dloni; to samo zaklecie w obu = jedna reka x mnoznik podwojnego. */
  double koszt(Player p, Stan s) {
    Ksiega.Zaklecie a = s.prawa ? ksiega.w(p.getInventory().getItemInMainHand()) : null;
    Ksiega.Zaklecie b = s.lewa ? ksiega.w(p.getInventory().getItemInOffHand()) : null;
    if (a != null && b != null && a.id().equals(b.id())) return a.manaZaTik() * ksiega.podwojneMnoznik;
    return (a == null ? 0 : a.manaZaTik()) + (b == null ? 0 : b.manaZaTik());
  }

  private boolean podwojne(Player p, Stan s) {
    if (!s.prawa || !s.lewa) return false;
    Ksiega.Zaklecie a = ksiega.w(p.getInventory().getItemInMainHand()), b = ksiega.w(p.getInventory().getItemInOffHand());
    return a != null && b != null && a.id().equals(b.id());
  }

  private double zasieg(Player p, Stan s) { return (podwojne(p, s) ? ksiega.stozekPodwojny : ksiega.stozek).zasieg(); }

  private void tick() {
    teraz++;
    for (var it = rzucajacy.entrySet().iterator(); it.hasNext(); ) {
      var wpis = it.next();
      Stan s = wpis.getValue();
      Player p = Bukkit.getPlayer(wpis.getKey());
      if (p == null || !p.isOnline() || p.isDead()) { it.remove(); if (p != null) { strumien.stop(p); ruch(p, false); } continue; }
      try {
        boolean pr = prawaTeraz(p), lw = lewaTeraz(p);
        if (!pr && !lw) {                                        // obie dlonie puszczone — koniec
          if (s.prawa || s.lewa) skill(p, ksiega.skStop);
          it.remove(); strumien.stop(p); ruch(p, false);
          continue;
        }
        if (pr != s.prawa || lw != s.lewa) {                     // zmiana aktywnych dloni
          boolean nowa = (pr && !s.prawa) || (lw && !s.lewa);
          log(p, "dlonie: prawa=" + pr + " lewa=" + lw);
          s.prawa = pr; s.lewa = lw;
          ruch(p, true);
          double z = zasieg(p, s);
          strumien.ustaw(p, true, pr ? ksiega.w(p.getInventory().getItemInMainHand()) : null, z);
          strumien.ustaw(p, false, lw ? ksiega.w(p.getInventory().getItemInOffHand()) : null, z);
          if (nowa) skill(p, ksiega.skStart);
        }
        int n = s.n++;
        strumien.tick(p, zasieg(p, s), n);
        if (n % 2 == 0 && !ksiega.skObraz.isBlank()) skill(p, ksiega.skObraz);
        if (n % 10 == 0) {
          double koszt = koszt(p, s);
          PlayerData d = PlayerData.get(p);
          if (d.getMana() < koszt) {
            skill(p, ksiega.skBrakMany);
            it.remove(); strumien.stop(p); ruch(p, false);
            continue;
          }
          d.giveMana(-koszt, ResourceUpdateReason.SKILL);
          skill(p, ksiega.skTik);
          List<Entity> cele = stozek(p, podwojne(p, s) ? ksiega.stozekPodwojny : ksiega.stozek);
          if (!cele.isEmpty()) trafienia(p, cele);
        }
      } catch (Throwable t) {   // np. nieaktualne dane MMOCore po szybkim przelogowaniu: gasimy tylko tego gracza
        it.remove(); strumien.stop(p); ruch(p, false);
        plugin.getLogger().warning("rzucanie przerwane (" + p.getName() + "): " + t);
      }
    }
  }

  /**
   * Cele stozka liczone tutaj, nie targeterem MM. Istota zywa (bez rzucajacego, stojakow), w zasiegu od oczu,
   * kat do srodka hitboxa <= pol-kat (+ poprawka na szerokosc hitboxa blisko), linia wzroku.
   */
  static List<Entity> stozek(Player p, Ksiega.Stozek st) {
    Location oko = p.getEyeLocation();
    Vector os = oko.getDirection().normalize();
    List<Entity> wynik = new ArrayList<>();
    for (Entity e : p.getNearbyEntities(st.zasieg(), st.zasieg(), st.zasieg())) {
      if (!(e instanceof LivingEntity le) || e instanceof ArmorStand || le.isDead() || e.equals(p)) continue;
      BoundingBox bb = le.getBoundingBox();
      Vector doCelu = bb.getCenter().subtract(oko.toVector());
      double d = doCelu.length();
      if (d > st.zasieg() + bb.getWidthX() / 2) continue;
      double kat = Math.toDegrees(os.angle(doCelu));
      double margines = Math.toDegrees(Math.atan2(bb.getWidthX() / 2, Math.max(d, 0.1)));
      if (kat > st.polKat() + margines) continue;
      if (!p.hasLineOfSight(e)) continue;
      wynik.add(e);
    }
    return wynik;
  }

  private void trafienia(Player p, List<Entity> cele) {
    try { MythicBukkit.inst().getAPIHelper().castSkill(p, ksiega.skTrafienia, p, p.getLocation(), cele, List.of(), 1f); }
    catch (Throwable t) { plugin.getLogger().log(Level.WARNING, "trafienia: " + t, t); }
  }

  /** Zar na dloni, gdy zaklecie jest w rece, ale gracz nie rzuca (domyslnie wylaczony — modele animuja sie same). */
  private void dlonie() {
    if (ksiega.skDlon.isBlank()) return;
    for (Player p : Bukkit.getOnlinePlayers()) {
      if (rzuca(p)) continue;
      if (ksiega.w(p.getInventory().getItemInMainHand()) != null || ksiega.w(p.getInventory().getItemInOffHand()) != null)
        skill(p, ksiega.skDlon);
    }
  }
}
