package pl.mcmmorpg.zaklecia;

import io.lumine.mythic.lib.player.resource.ResourceUpdateReason;
import io.lumine.mythic.lib.api.event.AttackEvent;
import io.lumine.mythic.lib.damage.DamageType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
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
import org.bukkit.entity.Projectile;
import org.bukkit.entity.AbstractArrow;
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
    boolean prawa, lewa, przelacznik;
    double oslonaP, oslonaL;
    boolean oplaconeP, oplaconeL;
  }

  private final Zaklecia plugin;
  private final Ksiega ksiega;
  private final Strumien strumien;
  private final Skille skille;
  private final RzutyOdrzutowe odrzutowe;
  private final EfektyCzasowe efekty;
  private final Map<UUID, Stan> rzucajacy = new HashMap<>();
  private final Map<EntityDamageEvent, Boolean> typyMythicLib = new WeakHashMap<>();
  private final Map<UUID, Long> ppmLewa = new HashMap<>();
  private final Map<UUID, Long> czekaLpm = new HashMap<>();
  private long teraz;
  /** Zachowane dla paska Wybor, ktory ma wlasny odsiew LPM po PPM. */
  private final Map<UUID, Long> ostatniPpm = new HashMap<>();

  boolean swiezyPpm(Player p) {
    Long t = ostatniPpm.get(p.getUniqueId());
    // Tylko ten sam/nastepny tik: animacja PPM nie moze przypisac prawej,
    // ale gracz ma moc kliknac LPM zaraz potem, by przypisac obie dlonie.
    return t != null && teraz - t <= 1;
  }
  private Wybor wybor;   // szybki wybor (Shift+Q) — gdy otwarty, LPM/PPM przypisuja, nie rzucaja

  private final NamespacedKey kZasieg, kIstoty, kSzybkosc, kRuch, kNosnik, kStareKopanie;

  Rzucanie(Zaklecia plugin, Ksiega ksiega, Nosnik nosnik) {
    this.plugin = plugin;
    this.ksiega = ksiega;
    this.skille = new Skille(plugin);
    this.efekty = new EfektyCzasowe(plugin, ksiega, nosnik, skille);
    this.odrzutowe = new RzutyOdrzutowe(this, ksiega, skille, efekty);
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
  void zdejmijBronie(Player p) { efekty.zdejmijBronie(p); }
  void uspokoj(LivingEntity cel, int ticki) { efekty.uspokoj(cel, ticki); }

  private void log(Player p, String co) {
    if (plugin.getConfig().getBoolean("debug-wejscie", false)) plugin.getLogger().info("[wejscie] t=" + teraz + " " + p.getName() + " " + co);
  }

  /** Wylaczenie pluginu: zdejmij strumienie i przejsciowe atrybuty. */
  void wylacz() {
    efekty.wylacz(); odrzutowe.wylacz(); strumien.stopWszystkie(); rzucajacy.clear();
    czekaLpm.clear(); ppmLewa.clear(); ostatniPpm.clear(); ostatnieQ.clear();
    for (Player p : Bukkit.getOnlinePlayers()) { atrybutyLpm(p, false); ruch(p, false); }
  }

  boolean rzuca(Player p) { return rzucajacy.containsKey(p.getUniqueId()) || odrzutowe.rzuca(p); }

  boolean przelacznikPrawej(Player p) {
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
    } else if (e.getAction() == Action.LEFT_CLICK_AIR && (wybor == null || !wybor.otwarty(e.getPlayer()))) {
      // Q tez macha reka = LEFT_CLICK_AIR. Przelacznik czeka na odsiew Q;
      // ladunek startuje od razu, a oznaczQ go przerywa przed gotowoscia.
      Player p = e.getPlayer();
      if (swiezeQ(p)) return;
      Ksiega.Zaklecie prawa = zak(p, true);
      if (prawa != null && prawa.odrzutowe()) {
        odrzutowe.lpm(p, prawa, rowno());
        return;
      }
      if (prawa == null || !prawa.koncentracja()) return;
      UUID u = p.getUniqueId();
      if (czekaLpm.containsKey(u)) czekaLpm.remove(u);
      else czekaLpm.put(u, teraz);
      log(p, "LPM: oczekuje na odsiew Q");
    }
  }

  /** Ticki, przez ktore machniecie reka uznajemy za skutek klawisza Q, a nie LPM. */
  static final int Q_OKNO = 3;
  private final Map<UUID, Long> ostatnieQ = new HashMap<>();
  void oznaczQ(Player p) {
    odrzutowe.przerwij(p);
    ostatnieQ.put(p.getUniqueId(), teraz);
    Long klik = czekaLpm.remove(p.getUniqueId());
    if (klik != null && teraz - klik <= Q_OKNO + 1) {
      log(p, "Q: cofniete LPM");
    }
  }
  boolean swiezeQ(Player p) { Long t = ostatnieQ.get(p.getUniqueId()); return t != null && teraz - t <= Q_OKNO + 1; }

  private int rowno() { return Math.max(0, plugin.getConfig().getInt("rowno", 4)); }

  void wcisnietoPpm(Player p) {
    if (!trybLpm(p)) return;
    ostatniPpm.put(p.getUniqueId(), teraz);
    Ksiega.Zaklecie lewa = zak(p, false);
    if (lewa == null) return;
    if (lewa.odrzutowe()) { odrzutowe.ppm(p, lewa, rowno()); return; }
    ppmLewa.put(p.getUniqueId(), teraz);
    rzucajacy.computeIfAbsent(p.getUniqueId(), u -> new Stan());
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
      if (lewa.odrzutowe()) { odrzutowe.ppm(p, lewa, rowno()); return; }
      ppmLewa.put(p.getUniqueId(), teraz);
      rzucajacy.computeIfAbsent(p.getUniqueId(), u -> new Stan());
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void animacja(PlayerAnimationEvent e) {
    if (trybLpm(e.getPlayer()) || wybor != null && wybor.otwarty(e.getPlayer())) e.setCancelled(true);
  }

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
    rzucajacy.remove(u); czekaLpm.remove(u); ppmLewa.remove(u); ostatniPpm.remove(u); ostatnieQ.remove(u);
    strumien.stop(e.getPlayer()); atrybutyLpm(e.getPlayer(), false); ruch(e.getPlayer(), false);
    odrzutowe.wyjscie(e.getPlayer());
  }

  void stop(Player p, boolean dzwiek) {
    odrzutowe.przerwij(p);
    UUID u = p.getUniqueId();
    czekaLpm.remove(u);
    Stan s = rzucajacy.get(u);
    ppmLewa.remove(u);
    if (s == null) return;
    if (dzwiek && p.isOnline()) przejscie(p, s.prawa ? zak(p, true) : null,
        s.lewa ? zak(p, false) : null, null, null);
    if (s.przelacznik || s.prawa || s.lewa) log(p, "stop: prawa=false lewa=false przelacznik=false");
    rzucajacy.remove(u);
    strumien.stop(p);
    ruch(p, false);
  }

  // ------------------------------------------------------------------ tik

  private boolean swiezy(Map<UUID, Long> m, Player p, int tolerancja) {
    Long t = m.get(p.getUniqueId());
    return t != null && teraz - t <= tolerancja;
  }

  private boolean prawaTeraz(Player p, Stan s) {
    if (zak(p, true) == null || !zak(p, true).koncentracja()) return false;
    return trybLpm(p) && s.przelacznik;
  }

  private boolean lewaTeraz(Player p) {
    if (zak(p, false) == null || !zak(p, false).koncentracja()) return false;
    return swiezy(ppmLewa, p, plugin.getConfig().getInt("ppm-tolerancja", 6));
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
    odrzutowe.ustawCzas(teraz);
    for (var it = czekaLpm.entrySet().iterator(); it.hasNext();) {
      var e = it.next();
      if (teraz - e.getValue() <= Q_OKNO + 1) continue;
      Player p = Bukkit.getPlayer(e.getKey());
      if (p != null && p.isOnline() && !p.isDead() && trybLpm(p) && zak(p, true) != null
          && zak(p, true).koncentracja()) {
        Stan s = rzucajacy.computeIfAbsent(e.getKey(), u -> new Stan());
        s.przelacznik = !s.przelacznik;
        log(p, "LPM: przelacznik prawej=" + s.przelacznik);
      }
      it.remove();
    }
    odrzutowe.tick(teraz, plugin.getConfig().getInt("ppm-tolerancja", 6));
    for (var it = rzucajacy.entrySet().iterator(); it.hasNext(); ) {
      var wpis = it.next();
      Stan s = wpis.getValue();
      Player p = Bukkit.getPlayer(wpis.getKey());
      if (p == null || !p.isOnline() || p.isDead()) { it.remove(); if (p != null) { strumien.stop(p); ruch(p, false); } continue; }
      try {
        boolean tryb = trybLpm(p);
        if (!tryb || zak(p, true) == null || !zak(p, true).koncentracja()) s.przelacznik = false;
        boolean pr = prawaTeraz(p, s);
        boolean lw = lewaTeraz(p);
        if (!pr && !lw) {                                        // obie dlonie puszczone — koniec
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
              && zak(p, true).cel().equals("stozek") ? zak(p, true) : null, z);
          strumien.ustaw(p, false, lw && zak(p, false).typ().equals("koncentracja")
              && zak(p, false).cel().equals("stozek") ? zak(p, false) : null, z);
          przejscie(p, staraP, staraL, nowaP, nowaL);
        }
        int n = s.n++;
        strumien.tick(p, zasieg(p, s), n);
        Ksiega.Zaklecie prawa = s.prawa ? zak(p, true) : null;
        Ksiega.Zaklecie lewa = s.lewa ? zak(p, false) : null;
        if (n % 2 == 0) obraz(p, prawa, lewa);
        boolean prawaInni = prawa != null && prawa.cel().equals("inni");
        boolean lewaInni = lewa != null && lewa.cel().equals("inni");
        if (n % 10 == 0) {
          double koszt = koszt(p, s);
          PlayerData d = PlayerData.get(p);
          if (d.getMana() < koszt) {
            skill(p, ksiega.skBrakMany);
            s.przelacznik = false;
            it.remove(); strumien.stop(p); ruch(p, false);
            continue;
          }
          if (koszt > 0) d.giveMana(-koszt, ResourceUpdateReason.SKILL);
          if (prawa != null) s.oplaconeP = true;
          if (lewa != null) s.oplaconeL = true;
          tikZaklec(p, prawaInni ? null : prawa, lewaInni ? null : lewa);
          if ((prawa != null && prawa.cel().equals("stozek")) || (lewa != null && lewa.cel().equals("stozek"))) {
            List<Entity> cele = stozek(p, podwojne(p, s) ? ksiega.stozekPodwojny : ksiega.stozek);
            if (!cele.isEmpty()) trafienia(p, cele,
                prawa != null && prawa.cel().equals("stozek") ? prawa : null,
                lewa != null && lewa.cel().equals("stozek") ? lewa : null);
          }
        }
        if (prawaInni && s.oplaconeP || lewaInni && s.oplaconeL) {
          LivingEntity sojusznik = EfektyCzasowe.cel(p,
              Math.max(prawaInni ? prawa.zasieg() : 0, lewaInni ? lewa.zasieg() : 0),
              e -> efekty.sojusznik(p, e));
          if (sojusznik != null) {
            if (toSamo(prawa, lewa) && s.oplaconeP && s.oplaconeL)
              skille.cele(p, prawa.skille().obie(prawa.skille().traf()), List.of(sojusznik));
            else {
              if (prawaInni && s.oplaconeP) skille.cele(p, prawa.skille().reka(prawa.skille().traf(), true), List.of(sojusznik));
              if (lewaInni && s.oplaconeL) skille.cele(p, lewa.skille().reka(lewa.skille().traf(), false), List.of(sojusznik));
            }
          }
        }
        if (prawa != null && prawa.typ().equals("tarcza") && s.oplaconeP) s.oslonaP = Math.min(
            prawa.sila() * (podwojne(p, s) ? prawa.efektOburacz() : 1),
            s.oslonaP + prawa.sila() / prawa.narastanie());
        if (lewa != null && lewa.typ().equals("tarcza") && !toSamo(prawa, lewa) && s.oplaconeL)
          s.oslonaL = Math.min(lewa.sila(), s.oslonaL + lewa.sila() / lewa.narastanie());
      } catch (Throwable t) {   // np. nieaktualne dane MMOCore po szybkim przelogowaniu: gasimy tylko tego gracza
        s.przelacznik = false;
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

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void typMythicLib(AttackEvent e) {
    typyMythicLib.put(e.toBukkit(), e.getAttack().getDamage().hasType(DamageType.MAGIC)
        || e.getAttack().getDamage().hasType(DamageType.SKILL));
  }

  private boolean magiczne(EntityDamageEvent e) {
    Boolean typ = typyMythicLib.remove(e);
    if (Boolean.TRUE.equals(typ)) return true;
    switch (e.getCause()) {
      case MAGIC, DRAGON_BREATH, FIRE, FIRE_TICK, LAVA, HOT_FLOOR, FREEZE,
          BLOCK_EXPLOSION, ENTITY_EXPLOSION, SONIC_BOOM -> { return true; }
      default -> { }
    }
    if (e instanceof EntityDamageByEntityEvent by && by.getDamager() instanceof Projectile pocisk)
      return !(pocisk instanceof AbstractArrow);
    String typZrodla = e.getDamageSource().getDamageType().getKey().getKey();
    return typZrodla.contains("magic") || typZrodla.contains("spell") || typZrodla.contains("fireball")
        || typZrodla.contains("explosion");
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void obrazenia(EntityDamageEvent e) {
    if (!(e.getEntity() instanceof Player p)) return;
    Stan s = rzucajacy.get(p.getUniqueId());
    if (s == null || e.getDamage() <= 0) return;
    Ksiega.Zaklecie zp = s.prawa ? zak(p, true) : null;
    Ksiega.Zaklecie zl = s.lewa ? zak(p, false) : null;
    boolean magia = magiczne(e);
    if (zp != null && zp.typ().equals("tarcza") && s.oslonaP > 0) {
      double redukcja = plugin.getConfig().getDouble("zaklecia." + zp.id() + (magia ? ".redukcja-magia" : ".redukcja-fizyczne"), zp.redukcja());
      double blok = Math.min(e.getDamage() * Math.max(0, Math.min(1, redukcja)), s.oslonaP);
      s.oslonaP -= blok; e.setDamage(Math.max(0, e.getDamage() - blok));
      if (s.oslonaP <= 0) pekla(p, s, zp, true, toSamo(zp, zl));
    }
    if (zl != null && zl.typ().equals("tarcza") && !toSamo(zp, zl) && s.oslonaL > 0) {
      double redukcja = plugin.getConfig().getDouble("zaklecia." + zl.id() + (magia ? ".redukcja-magia" : ".redukcja-fizyczne"), zl.redukcja());
      double blok = Math.min(e.getDamage() * Math.max(0, Math.min(1, redukcja)), s.oslonaL);
      s.oslonaL -= blok; e.setDamage(Math.max(0, e.getDamage() - blok));
      if (s.oslonaL <= 0) pekla(p, s, zl, false, false);
    }
  }

  private void pekla(Player p, Stan s, Ksiega.Zaklecie z, boolean prawa, boolean obie) {
    skill(p, "Zak_" + Character.toUpperCase(z.id().charAt(0)) + z.id().substring(1) + "_Peka");
    Ksiega.Zaklecie staraP = s.prawa ? zak(p, true) : null;
    Ksiega.Zaklecie staraL = s.lewa ? zak(p, false) : null;
    if (prawa) { s.prawa = false; s.oslonaP = 0; s.przelacznik = false; }
    else { s.lewa = false; s.oslonaL = 0; ppmLewa.remove(p.getUniqueId()); }
    if (obie) { s.lewa = false; s.oslonaL = 0; ppmLewa.remove(p.getUniqueId()); }
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
