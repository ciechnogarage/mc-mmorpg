package pl.mcmmorpg.zaklecia;

import io.lumine.mythic.lib.player.resource.ResourceUpdateReason;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/** LPM w MISS jest pojedynczym kliknieciem; PPM trzymany ponawia pakiet co 4 ticki. */
final class Rzucanie implements Listener {
  static final class Stan {
    int n;
    boolean prawa, lewa, przelacznik, razem;
    long oczekujeLpm = -1;
    boolean cofnijPrzelacznik, cofnijRazem;
    long cofnijOd = -99;
    double oslonaP, oslonaL;
    boolean oplaconeP, oplaconeL;
  }

  private final Zaklecia plugin;
  private final Ksiega ksiega;
  private final Strumien strumien;
  private final Skille skille;
  private final RzutyOdrzutowe odrzutowe;
  private final Map<UUID, Stan> rzucajacy = new HashMap<>();
  private final Map<UUID, Integer> generacja = new HashMap<>();
  private final Map<UUID, Long> ppmPrawa = new HashMap<>(), ppmLewa = new HashMap<>();
  private long teraz;
  /** Zachowane dla paska Wybor, ktory ma wlasny odsiew LPM po PPM. */
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
    this.skille = new Skille(plugin);
    this.odrzutowe = new RzutyOdrzutowe(this, ksiega, skille);
    this.strumien = new Strumien(skille);
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
    odrzutowe.wylacz(); strumien.stopWszystkie(); rzucajacy.clear();
    for (Player p : Bukkit.getOnlinePlayers()) { atrybutyLpm(p, false); ruch(p, false); }
  }

  boolean rzuca(Player p) { return rzucajacy.containsKey(p.getUniqueId()) || odrzutowe.rzuca(p); }

  boolean przelacznikLewej(Player p) {
    Stan s = rzucajacy.get(p.getUniqueId());
    return s != null && s.przelacznik && trybLpm(p);
  }

  String stanDloni(Player p, boolean prawa) {
    Ksiega.Zaklecie z = ksiega.w(prawa ? p.getInventory().getItemInMainHand() : p.getInventory().getItemInOffHand());
    if (z == null) return "nic";
    if (z.odrzutowe()) {
      String faza = odrzutowe.stan(p, prawa);
      return faza == null ? "gotowa" : faza;
    }
    Stan s = rzucajacy.get(p.getUniqueId());
    return s != null && (prawa ? s.prawa : s.lewa) ? "rzuca" : "gotowa";
  }

  /** Czy dana dlon jest aktywna (w trakcie rzucania); poza rzucaniem — czy trzyma zaklecie. Dla warunkow MM. */
  boolean aktywna(Player p, boolean prawa) {
    Ksiega.Zaklecie z = zak(p, prawa);
    if (z != null && z.odrzutowe()) return odrzutowe.aktywna(p, prawa);
    Stan s = rzucajacy.get(p.getUniqueId());
    if (s == null) return ksiega.w(prawa ? p.getInventory().getItemInMainHand() : p.getInventory().getItemInOffHand()) != null;
    return prawa ? s.prawa : s.lewa;
  }

  private void skill(Player p, String nazwa) { skille.cast(p, nazwa); }

  // ------------------------------------------------------------------ tryb LPM

  /** Nosnik w prawej oznacza tryb zaklec, takze niewidoczna pusta dlon. */
  boolean trybLpm(Player p) {
    return trybHud(p) && (ksiega.w(p.getInventory().getItemInMainHand()) != null
        || ksiega.w(p.getInventory().getItemInOffHand()) != null);
  }

  /** HUD pokazuje tryb rowniez wtedy, gdy nosnik jest chwilowo pusty. */
  boolean trybHud(Player p) {
    if (Nosnik.widz(p)) return false;
    ItemStack prawa = p.getInventory().getItemInMainHand();
    return prawa.getType() == org.bukkit.Material.DEBUG_STICK && prawa.hasItemMeta()
        && prawa.getItemMeta().getPersistentDataContainer().has(kNosnik);
  }

  private void trybyLpm() { for (Player p : Bukkit.getOnlinePlayers()) atrybutyLpm(p, trybLpm(p)); }

  private void atrybutyLpm(Player p, boolean wl) {
    modDo(p, Attribute.BLOCK_INTERACTION_RANGE, kZasieg, plugin.getConfig().getDouble("zasieg-blokow", 0), wl);
    modDo(p, Attribute.ENTITY_INTERACTION_RANGE, kIstoty, 0, wl);
    // Usun oba klucze przyspieszenia kopania pozostale po poprzednich wersjach.
    mod(p, Attribute.BLOCK_BREAK_SPEED, kSzybkosc, 0, AttributeModifier.Operation.ADD_NUMBER, false);
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
    AttributeModifier stary = ai.getModifier(k);
    if (stary != null && (!wl || stary.getAmount() != v || stary.getOperation() != op)) ai.removeModifier(k);
    if (wl && ai.getModifier(k) == null) ai.addTransientModifier(new AttributeModifier(k, v, op, EquipmentSlotGroup.ANY));
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void lpm(PlayerInteractEvent e) {
    if (e.getHand() != EquipmentSlot.HAND || !trybLpm(e.getPlayer())) return;
    if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
      e.setCancelled(true);
      Ksiega.Zaklecie lewa = zak(e.getPlayer(), false);
      if (lewa != null && lewa.odrzutowe() && (wybor == null || !wybor.otwarty(e.getPlayer())))
        odrzutowe.lpm(e.getPlayer(), lewa, rowno());
    } else if (e.getAction() == Action.LEFT_CLICK_AIR && (wybor == null || !wybor.otwarty(e.getPlayer()))) {
      // LPM dziala od razu (naraz z PPM = obie w tym samym ticku). Klawisz Q tez macha reka = LEFT_CLICK_AIR, a MenuKey
      // woła "zaklecia q" chwile pozniej — wtedy oznaczQ cofa skutek tego klikniecia (stan sprzed klikniecia).
      Player p = e.getPlayer();
      if (swiezeQ(p)) return;
      Ksiega.Zaklecie lewa = zak(p, false);
      if (lewa != null && lewa.odrzutowe()) {
        odrzutowe.lpm(p, lewa, rowno());
        return;
      }
      Stan s = rzucajacy.computeIfAbsent(p.getUniqueId(), k -> new Stan());
      s.cofnijPrzelacznik = s.przelacznik; s.cofnijRazem = s.razem; s.cofnijOd = teraz;
      boolean bylaPrawa = prawaTeraz(p);
      klikLpm(p, teraz, bylaPrawa, (s.razem || s.przelacznik) && bylaPrawa);
    }
  }

  /** Ticki, przez ktore machniecie reka uznajemy za skutek klawisza Q, a nie LPM. */
  static final int Q_OKNO = 3;
  private final Map<UUID, Long> ostatnieQ = new HashMap<>();
  void oznaczQ(Player p) {
    odrzutowe.przerwij(p);
    ostatnieQ.put(p.getUniqueId(), teraz);
    Stan s = rzucajacy.get(p.getUniqueId());
    if (s != null && teraz - s.cofnijOd <= Q_OKNO + 1) {   // machniecie bylo od Q, nie od LPM: cofnij jego skutek
      s.oczekujeLpm = -1; s.przelacznik = s.cofnijPrzelacznik; s.razem = s.cofnijRazem; s.cofnijOd = -99;
      log(p, "Q: cofniete LPM");
    }
  }
  boolean swiezeQ(Player p) { Long t = ostatnieQ.get(p.getUniqueId()); return t != null && teraz - t <= Q_OKNO + 1; }

  private int rowno() { return Math.max(0, plugin.getConfig().getInt("rowno", 4)); }

  private void klikLpm(Player p, long czasLpm, boolean bylaPrawa, boolean byloObie) {
    if (ksiega.w(p.getInventory().getItemInOffHand()) == null) return;
    UUID u = p.getUniqueId();
    Stan s = rzucajacy.computeIfAbsent(u, k -> new Stan());
    Long ostatniePpm = ppmPrawa.get(u);
    boolean prawaTrzymana = bylaPrawa || (ostatniePpm != null && ostatniePpm >= czasLpm
        && ostatniePpm - czasLpm <= rowno() && prawaTeraz(p));
    if (byloObie || s.razem || (s.przelacznik && prawaTrzymana)) {
      s.przelacznik = false; s.razem = false; s.oczekujeLpm = -1;
      log(p, "LPM: koniec lewej przy obu");
    } else if (prawaTrzymana) {
      s.razem = true; s.oczekujeLpm = -1;
      log(p, "LPM: razem");
    } else {
      s.oczekujeLpm = czasLpm;
      log(p, "LPM: czeka na PPM");
    }
  }

  void wcisnietoPpm(Player p) {
    if (!trybLpm(p)) return;
    ostatniPpm.put(p.getUniqueId(), teraz);
    if (wybor != null && wybor.otwarty(p)) { wybor.wcisnietoPpm(p); return; }
    Ksiega.Zaklecie prawa = zak(p, true);
    if (prawa != null && prawa.odrzutowe()) {
      odrzutowe.ppm(p, prawa, true, rowno());
      return;
    }
    if (ksiega.w(p.getInventory().getItemInMainHand()) == null) {
      // Prawa pusta, zaklecie tylko w lewej: trzymany PPM prowadzi LEWA (puszczenie PPM serwer widzi zawsze).
      if (ksiega.w(p.getInventory().getItemInOffHand()) != null) {
        Ksiega.Zaklecie lewa = zak(p, false);
        if (lewa.odrzutowe()) { odrzutowe.ppm(p, lewa, false, rowno()); return; }
        ppmLewa.put(p.getUniqueId(), teraz);
        rzucajacy.computeIfAbsent(p.getUniqueId(), u -> new Stan());
      }
      return;
    }
    {
      ppmPrawa.put(p.getUniqueId(), teraz);
      Stan s = rzucajacy.computeIfAbsent(p.getUniqueId(), u -> new Stan());
      if (s.oczekujeLpm >= 0 && teraz - s.oczekujeLpm <= rowno() && !swiezeQ(p)) {
        s.razem = !s.przelacznik;
        s.oczekujeLpm = -1;
        log(p, "PPM po LPM: razem");
      }
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
      Ksiega.Zaklecie lewa = ksiega.w(e.getItem());
      if (lewa.odrzutowe()) { odrzutowe.ppm(p, lewa, false, rowno()); return; }
      ppmLewa.put(p.getUniqueId(), teraz);
      rzucajacy.computeIfAbsent(p.getUniqueId(), u -> new Stan());
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void animacja(PlayerAnimationEvent e) { if (trybLpm(e.getPlayer())) e.setCancelled(true); }

  // Uwaga: NIE traktowac EntityDamageByEntityEvent z graczem jako LPM — obrazenia zaklec (mmodamage) tez maja gracza
  // jako sprawce: anulowalo to trafienia i odpalalo kolejny strzal z lewej (petla). Wrecz w trybie zaklec i tak nie ma (zasieg istot 0).

  @EventHandler
  public void smierc(PlayerDeathEvent e) { stop(e.getPlayer(), false); }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void zmianaSlotu(PlayerItemHeldEvent e) {
    stop(e.getPlayer(), false);
    atrybutyLpm(e.getPlayer(), false);
  }

  @EventHandler
  public void wyjscie(PlayerQuitEvent e) {
    UUID u = e.getPlayer().getUniqueId();
    rzucajacy.remove(u); generacja.remove(u); ppmPrawa.remove(u); ppmLewa.remove(u); ostatniPpm.remove(u); ostatnieQ.remove(u);
    strumien.stop(e.getPlayer()); atrybutyLpm(e.getPlayer(), false); ruch(e.getPlayer(), false);
    odrzutowe.wyjscie(e.getPlayer());
  }

  void stop(Player p, boolean dzwiek) {
    odrzutowe.przerwij(p);
    UUID u = p.getUniqueId();
    generacja.merge(u, 1, Integer::sum); // uniewaznij LPM oczekujacy na odsiew Q
    Stan s = rzucajacy.get(u);
    ppmPrawa.remove(u); ppmLewa.remove(u);
    if (s == null) return;
    if (dzwiek && p.isOnline()) przejscie(p, s.prawa ? zak(p, true) : null,
        s.lewa ? zak(p, false) : null, null, null);
    if (s.przelacznik || s.razem || s.prawa || s.lewa) log(p, "stop: prawa=false lewa=false razem=false przelacznik=false");
    rzucajacy.remove(u);
    strumien.stop(p);
    ruch(p, false);
  }

  // ------------------------------------------------------------------ tik

  private boolean swiezy(Map<UUID, Long> m, Player p, int tolerancja) {
    Long t = m.get(p.getUniqueId());
    return t != null && teraz - t <= tolerancja;
  }

  private boolean prawaTeraz(Player p) {
    if (zak(p, true) == null || !zak(p, true).koncentracja()) return false;
    return trybLpm(p) && swiezy(ppmPrawa, p, plugin.getConfig().getInt("ppm-tolerancja", 6));
  }

  private boolean lewaTeraz(Player p, Stan s, boolean pr) {
    if (zak(p, false) == null || !zak(p, false).koncentracja()) return false;
    if (!trybLpm(p)) return swiezy(ppmLewa, p, plugin.getConfig().getInt("ppm-tolerancja", 6));
    boolean ppmTrzymaLewa = ksiega.w(p.getInventory().getItemInMainHand()) == null
        && swiezy(ppmLewa, p, plugin.getConfig().getInt("ppm-tolerancja", 6));
    return s.przelacznik || (s.razem && pr) || ppmTrzymaLewa;
  }

  /** Koszt many za tik z aktywnych dloni; to samo zaklecie w obu = jedna reka x mnoznik podwojnego. */
  double koszt(Player p, Stan s) {
    Ksiega.Zaklecie a = s.prawa ? ksiega.w(p.getInventory().getItemInMainHand()) : null;
    Ksiega.Zaklecie b = s.lewa ? ksiega.w(p.getInventory().getItemInOffHand()) : null;
    if (a != null && b != null && a.id().equals(b.id()))
      return a.manaZaTik() * (a.typ().equals("koncentracja") && !a.cel().equals("siebie")
          && !plugin.getConfig().contains("zaklecia." + a.id() + ".oburacz.mana")
          ? ksiega.podwojneMnoznik : a.manaOburacz());
    return (a == null ? 0 : a.manaZaTik()) + (b == null ? 0 : b.manaZaTik());
  }

  private boolean podwojne(Player p, Stan s) {
    if (!s.prawa || !s.lewa) return false;
    Ksiega.Zaklecie a = ksiega.w(p.getInventory().getItemInMainHand()), b = ksiega.w(p.getInventory().getItemInOffHand());
    return a != null && b != null && a.id().equals(b.id());
  }

  private double zasieg(Player p, Stan s) { return (podwojne(p, s) ? ksiega.stozekPodwojny : ksiega.stozek).zasieg(); }

  private Ksiega.Zaklecie zak(Player p, boolean prawa) {
    return ksiega.w(prawa ? p.getInventory().getItemInMainHand() : p.getInventory().getItemInOffHand());
  }

  private static boolean toSamo(Ksiega.Zaklecie a, Ksiega.Zaklecie b) {
    return a != null && b != null && a.id().equals(b.id());
  }

  private void przejscie(Player p, Ksiega.Zaklecie staraP, Ksiega.Zaklecie staraL,
      Ksiega.Zaklecie nowaP, Ksiega.Zaklecie nowaL) {
    if (staraP != null && !toSamo(staraP, nowaP) && !toSamo(staraP, nowaL)) skill(p, staraP.skille().stop());
    if (staraL != null && !toSamo(staraL, staraP) && !toSamo(staraL, nowaP) && !toSamo(staraL, nowaL))
      skill(p, staraL.skille().stop());
    if (staraP != null && !toSamo(staraP, nowaP)) skill(p, staraP.skille().reka(staraP.skille().stop(), true));
    if (staraL != null && !toSamo(staraL, nowaL)) skill(p, staraL.skille().reka(staraL.skille().stop(), false));
    if (nowaP != null && !toSamo(nowaP, staraP) && !toSamo(nowaP, staraL)) skill(p, nowaP.skille().start());
    if (nowaL != null && !toSamo(nowaL, staraP) && !toSamo(nowaL, staraL)
        && !toSamo(nowaL, nowaP)) skill(p, nowaL.skille().start());
    if (nowaP != null && !toSamo(nowaP, staraP)) skill(p, nowaP.skille().reka(nowaP.skille().start(), true));
    if (nowaL != null && !toSamo(nowaL, staraL)) skill(p, nowaL.skille().reka(nowaL.skille().start(), false));
    if (toSamo(nowaP, nowaL) && !toSamo(staraP, staraL)) skill(p, nowaP.skille().obie(nowaP.skille().start()));
  }

  private void tikZaklec(Player p, Ksiega.Zaklecie prawa, Ksiega.Zaklecie lewa) {
    if (toSamo(prawa, lewa)) {
      skill(p, prawa.skille().obie(prawa.skille().tik()));
      return;
    }
    if (prawa != null) skill(p, prawa.skille().reka(prawa.skille().tik(), true));
    if (lewa != null) skill(p, lewa.skille().reka(lewa.skille().tik(), false));
  }

  private void obraz(Player p, Ksiega.Zaklecie prawa, Ksiega.Zaklecie lewa) {
    if (prawa != null) skill(p, prawa.skille().reka(prawa.skille().obraz(), true));
    if (lewa != null) skill(p, lewa.skille().reka(lewa.skille().obraz(), false));
    if (toSamo(prawa, lewa)) skill(p, prawa.skille().obie(prawa.skille().obraz()));
  }

  private void tick() {
    teraz++;
    odrzutowe.tick(teraz, plugin.getConfig().getInt("ppm-tolerancja", 6), rowno());
    for (var it = rzucajacy.entrySet().iterator(); it.hasNext(); ) {
      var wpis = it.next();
      Stan s = wpis.getValue();
      Player p = Bukkit.getPlayer(wpis.getKey());
      if (p == null || !p.isOnline() || p.isDead()) { it.remove(); if (p != null) { strumien.stop(p); ruch(p, false); } continue; }
      try {
        boolean tryb = trybLpm(p);
        boolean pr = prawaTeraz(p);
        if (!tryb || zak(p, false) == null || !zak(p, false).koncentracja()) {
          s.przelacznik = false; s.razem = false; s.oczekujeLpm = -1;
        }
        if (!pr && s.razem) {
          s.razem = false; // puszczenie PPM konczy "razem", ale nie przelacznik lewej
          log(p, "razem=false (PPM puszczony)");
        }
        if (s.oczekujeLpm >= 0 && teraz - s.oczekujeLpm > rowno()) {
          s.oczekujeLpm = -1;
          s.przelacznik = !s.przelacznik;
          log(p, "przelacznik lewej=" + s.przelacznik);
        }
        boolean lw = lewaTeraz(p, s, pr);
        if (!pr && !lw) {                                        // obie dlonie puszczone — koniec
          if (s.oczekujeLpm >= 0) continue;
          if (s.prawa || s.lewa) przejscie(p, s.prawa ? zak(p, true) : null,
              s.lewa ? zak(p, false) : null, null, null);
          if (s.prawa || s.lewa) log(p, "dlonie: prawa=false lewa=false");
          it.remove(); strumien.stop(p); ruch(p, false);
          continue;
        }
        if (pr != s.prawa || lw != s.lewa) {                     // zmiana aktywnych dloni
          Ksiega.Zaklecie staraP = s.prawa ? zak(p, true) : null;
          Ksiega.Zaklecie staraL = s.lewa ? zak(p, false) : null;
          Ksiega.Zaklecie nowaP = pr ? zak(p, true) : null;
          Ksiega.Zaklecie nowaL = lw ? zak(p, false) : null;
          log(p, "dlonie: prawa=" + pr + " lewa=" + lw);
          if (pr != s.prawa) s.oplaconeP = false;
          if (lw != s.lewa) s.oplaconeL = false;
          s.prawa = pr; s.lewa = lw;
          if (!pr) s.oslonaP = 0;
          if (!lw) s.oslonaL = 0;
          ruch(p, true);
          double z = zasieg(p, s);
          strumien.ustaw(p, true, pr && zak(p, true).typ().equals("koncentracja")
              && !zak(p, true).cel().equals("siebie") ? zak(p, true) : null, z);
          strumien.ustaw(p, false, lw && zak(p, false).typ().equals("koncentracja")
              && !zak(p, false).cel().equals("siebie") ? zak(p, false) : null, z);
          przejscie(p, staraP, staraL, nowaP, nowaL);
        }
        int n = s.n++;
        strumien.tick(p, zasieg(p, s), n);
        Ksiega.Zaklecie prawa = s.prawa ? zak(p, true) : null;
        Ksiega.Zaklecie lewa = s.lewa ? zak(p, false) : null;
        if (n % 2 == 0) obraz(p, prawa, lewa);
        if (n % 10 == 0) {
          double koszt = koszt(p, s);
          PlayerData d = PlayerData.get(p);
          if (d.getMana() < koszt) {
            skill(p, ksiega.skBrakMany);
            s.przelacznik = false; s.razem = false; s.oczekujeLpm = -1;
            it.remove(); strumien.stop(p); ruch(p, false);
            continue;
          }
          d.giveMana(-koszt, ResourceUpdateReason.SKILL);
          if (prawa != null) s.oplaconeP = true;
          if (lewa != null) s.oplaconeL = true;
          tikZaklec(p, prawa, lewa);
          if ((prawa != null && prawa.cel().equals("stozek")) || (lewa != null && lewa.cel().equals("stozek"))) {
            List<Entity> cele = stozek(p, podwojne(p, s) ? ksiega.stozekPodwojny : ksiega.stozek);
            if (!cele.isEmpty()) trafienia(p, cele,
                prawa != null && prawa.cel().equals("stozek") ? prawa : null,
                lewa != null && lewa.cel().equals("stozek") ? lewa : null);
          }
        }
        if (prawa != null && prawa.typ().equals("tarcza") && s.oplaconeP) s.oslonaP = Math.min(
            prawa.sila() * (podwojne(p, s) ? prawa.efektOburacz() : 1),
            s.oslonaP + prawa.sila() / prawa.narastanie());
        if (lewa != null && lewa.typ().equals("tarcza") && !toSamo(prawa, lewa) && s.oplaconeL)
          s.oslonaL = Math.min(lewa.sila(), s.oslonaL + lewa.sila() / lewa.narastanie());
      } catch (Throwable t) {   // np. nieaktualne dane MMOCore po szybkim przelogowaniu: gasimy tylko tego gracza
        s.przelacznik = false; s.razem = false; s.oczekujeLpm = -1;
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

  private void trafienia(Player p, List<Entity> cele, Ksiega.Zaklecie prawa, Ksiega.Zaklecie lewa) {
    if (toSamo(prawa, lewa)) {
      skille.cele(p, prawa.skille().obie(prawa.skille().traf()), cele);
      return;
    }
    if (prawa != null) skille.cele(p, prawa.skille().reka(prawa.skille().traf(), true), cele);
    if (lewa != null) skille.cele(p, lewa.skille().reka(lewa.skille().traf(), false), cele);
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void obrazenia(EntityDamageEvent e) {
    if (!(e.getEntity() instanceof Player p)) return;
    Stan s = rzucajacy.get(p.getUniqueId());
    if (s == null || e.getDamage() <= 0) return;
    Ksiega.Zaklecie zp = s.prawa ? zak(p, true) : null;
    Ksiega.Zaklecie zl = s.lewa ? zak(p, false) : null;
    if (zp != null && zp.typ().equals("tarcza") && s.oslonaP > 0) {
      double blok = Math.min(e.getDamage() * Math.max(0, Math.min(1, zp.redukcja())), s.oslonaP);
      s.oslonaP -= blok; e.setDamage(Math.max(0, e.getDamage() - blok));
      if (s.oslonaP <= 0) pekla(p, s, zp, true, toSamo(zp, zl));
    }
    if (zl != null && zl.typ().equals("tarcza") && !toSamo(zp, zl) && s.oslonaL > 0) {
      double blok = Math.min(e.getDamage() * Math.max(0, Math.min(1, zl.redukcja())), s.oslonaL);
      s.oslonaL -= blok; e.setDamage(Math.max(0, e.getDamage() - blok));
      if (s.oslonaL <= 0) pekla(p, s, zl, false, false);
    }
  }

  private void pekla(Player p, Stan s, Ksiega.Zaklecie z, boolean prawa, boolean obie) {
    skill(p, "Zak_" + Character.toUpperCase(z.id().charAt(0)) + z.id().substring(1) + "_Peka");
    Ksiega.Zaklecie staraP = s.prawa ? zak(p, true) : null;
    Ksiega.Zaklecie staraL = s.lewa ? zak(p, false) : null;
    if (prawa) { s.prawa = false; s.oslonaP = 0; ppmPrawa.remove(p.getUniqueId()); }
    else { s.lewa = false; s.oslonaL = 0; s.przelacznik = false; ppmLewa.remove(p.getUniqueId()); }
    if (obie) { s.lewa = false; s.oslonaL = 0; s.razem = false; ppmLewa.remove(p.getUniqueId()); }
    przejscie(p, staraP, staraL, s.prawa ? zak(p, true) : null, s.lewa ? zak(p, false) : null);
  }

  /** Zar na dloni, gdy zaklecie jest w rece, ale gracz nie rzuca (domyslnie wylaczony — modele animuja sie same). */
  private void dlonie() {
    for (Player p : Bukkit.getOnlinePlayers()) {
      if (rzuca(p)) continue;
      Ksiega.Zaklecie prawa = zak(p, true), lewa = zak(p, false);
      if (prawa != null) skill(p, prawa.skille().reka(prawa.skille().dlon(), true));
      if (lewa != null) skill(p, lewa.skille().reka(lewa.skille().dlon(), false));
    }
  }
}
