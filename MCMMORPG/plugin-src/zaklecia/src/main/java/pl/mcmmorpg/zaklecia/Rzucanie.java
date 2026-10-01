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

/** LPM w MISS jest pojedynczym kliknieciem; PPM trzymany ponawia pakiet co 4 ticki. */
final class Rzucanie implements Listener {
  static final class Stan {
    int n;
    boolean prawa, lewa, przelacznik, razem;
    long oczekujeLpm = -1;
    boolean cofnijPrzelacznik, cofnijRazem;
    long cofnijOd = -99;
  }

  private final Zaklecia plugin;
  private final Ksiega ksiega;
  private final Strumien strumien;
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
    if (Nosnik.widz(p)) return false;   // widz: bez zasiegu istot 0, kotwicy i rzucania (scena kreatora)
    ItemStack prawa = p.getInventory().getItemInMainHand();
    return prawa.getType() == org.bukkit.Material.DEBUG_STICK && prawa.hasItemMeta()
        && prawa.getItemMeta().getPersistentDataContainer().has(kNosnik)
        && (ksiega.w(p.getInventory().getItemInMainHand()) != null
            || ksiega.w(p.getInventory().getItemInOffHand()) != null);
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
      // LPM dziala od razu (naraz z PPM = obie w tym samym ticku). Klawisz Q tez macha reka = LEFT_CLICK_AIR, a MenuKey
      // woła "zaklecia q" chwile pozniej — wtedy oznaczQ cofa skutek tego klikniecia (stan sprzed klikniecia).
      Player p = e.getPlayer();
      if (swiezeQ(p)) return;
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
    if (ksiega.w(p.getInventory().getItemInMainHand()) == null) {
      // Prawa pusta, zaklecie tylko w lewej: trzymany PPM prowadzi LEWA (puszczenie PPM serwer widzi zawsze).
      if (ksiega.w(p.getInventory().getItemInOffHand()) != null) {
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
      ppmLewa.put(p.getUniqueId(), teraz);
      rzucajacy.computeIfAbsent(p.getUniqueId(), u -> new Stan());
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void animacja(PlayerAnimationEvent e) { if (trybLpm(e.getPlayer())) e.setCancelled(true); }

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
  }

  void stop(Player p, boolean dzwiek) {
    UUID u = p.getUniqueId();
    generacja.merge(u, 1, Integer::sum); // uniewaznij LPM oczekujacy na odsiew Q
    Stan s = rzucajacy.get(u);
    ppmPrawa.remove(u); ppmLewa.remove(u);
    if (s == null) return;
    if (dzwiek && p.isOnline() && (s.prawa || s.lewa)) skill(p, ksiega.skStop);   // przed usunieciem: warunki widza dlonie
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
    if (ksiega.w(p.getInventory().getItemInMainHand()) == null) return false;
    return trybLpm(p) && swiezy(ppmPrawa, p, plugin.getConfig().getInt("ppm-tolerancja", 6));
  }

  private boolean lewaTeraz(Player p, Stan s, boolean pr) {
    if (ksiega.w(p.getInventory().getItemInOffHand()) == null) return false;
    if (!trybLpm(p)) return swiezy(ppmLewa, p, plugin.getConfig().getInt("ppm-tolerancja", 6));
    boolean ppmTrzymaLewa = ksiega.w(p.getInventory().getItemInMainHand()) == null
        && swiezy(ppmLewa, p, plugin.getConfig().getInt("ppm-tolerancja", 6));
    return s.przelacznik || (s.razem && pr) || ppmTrzymaLewa;
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
        boolean tryb = trybLpm(p);
        boolean pr = prawaTeraz(p);
        if (!tryb || ksiega.w(p.getInventory().getItemInOffHand()) == null) {
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
          if (s.prawa || s.lewa) skill(p, ksiega.skStop);
          if (s.prawa || s.lewa) log(p, "dlonie: prawa=false lewa=false");
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
            s.przelacznik = false; s.razem = false; s.oczekujeLpm = -1;
            it.remove(); strumien.stop(p); ruch(p, false);
            continue;
          }
          d.giveMana(-koszt, ResourceUpdateReason.SKILL);
          skill(p, ksiega.skTik);
          List<Entity> cele = stozek(p, podwojne(p, s) ? ksiega.stozekPodwojny : ksiega.stozek);
          if (!cele.isEmpty()) trafienia(p, cele);
        }
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
