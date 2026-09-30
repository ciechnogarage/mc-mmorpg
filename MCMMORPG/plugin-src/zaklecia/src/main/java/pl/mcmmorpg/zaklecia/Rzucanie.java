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
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * Rzucanie zaklec koncentracji, KAZDA DLON OSOBNO (jak w Skyrimie: PPM = prawa, LPM = lewa, oba = polaczone).
 *
 * <ul>
 *   <li>PPM: uzywanie przedmiotu (Consumable) — aktywna ta reka, ktora gra uzywa (zaklecie w prawej; przy broni
 *       w prawej PPM uzywa zaklecia z lewej). Crucible ~onUse nie widzi drugiej reki, wiec prowadzi to plugin.</li>
 *   <li>LPM: serwer nie widzi przytrzymania LPM w powietrze. Widzi za to KOPANIE: poczatek (BlockDamageEvent)
 *       i przerwanie (BlockDamageAbortEvent). W trybie LPM (zaklecie w lewej, a prawa pusta albo z zakleciem)
 *       gracz dostaje przejsciowo: zasieg celowania w bloki 64, w istoty 0 (kursor na mobie trafia w teren za nim),
 *       predkosc kopania 0 (blok nie peka). Przytrzymany LPM = "kopie" teren przed soba = lewa dlon rzuca.
 *       Przeskok kursora miedzy blokami to przerwanie+start w jednym ticku — stad 3 ticki tolerancji. Klik LPM
 *       w niebo (brak bloku w 64) daje impuls 0,5 s.</li>
 * </ul>
 * Tiki co 1 tick na watku glownym; mana co 10 tickow z aktywnych dloni; cele stozka liczy plugin.
 */
final class Rzucanie implements Listener {
  static final class Stan { int n; boolean prawa, lewa; }

  private final Zaklecia plugin;
  private final Ksiega ksiega;
  private final Strumien strumien;
  private final Map<UUID, Stan> rzucajacy = new HashMap<>();
  /** LPM: do ktorego ticku lewa dlon jest "wcisnieta" (kopanie trwa = Long.MAX_VALUE). */
  private final Map<UUID, Long> lpmDo = new HashMap<>();
  private long teraz;
  /** Tick ostatniego PPM gracza: machniecie reka po PPM Paper zglasza tez jako LEFT_CLICK_AIR — ignorujemy 6 tickow. */
  private final Map<UUID, Long> ostatniPpm = new HashMap<>();

  boolean swiezyPpm(Player p) { Long t = ostatniPpm.get(p.getUniqueId()); return t != null && teraz - t <= 6; }
  private Wybor wybor;   // szybki wybor (Shift+Q) — gdy otwarty, LPM/PPM przypisuja, nie rzucaja

  private final NamespacedKey kZasieg, kIstoty, kKopanie;

  Rzucanie(Zaklecia plugin, Ksiega ksiega) {
    this.plugin = plugin;
    this.ksiega = ksiega;
    this.strumien = new Strumien(plugin, ksiega);
    kZasieg = new NamespacedKey(plugin, "lpm_zasieg");
    kIstoty = new NamespacedKey(plugin, "lpm_istoty");
    kKopanie = new NamespacedKey(plugin, "lpm_kopanie");
    Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
    Bukkit.getScheduler().runTaskTimer(plugin, this::dlonie, 4, 4);
    Bukkit.getScheduler().runTaskTimer(plugin, this::trybyLpm, 5, 5);
  }

  void ustawWybor(Wybor w) { this.wybor = w; }

  /** Wylaczenie pluginu: zdejmij strumienie i przejsciowe atrybuty. */
  void wylacz() {
    strumien.stopWszystkie(); rzucajacy.clear();
    for (Player p : Bukkit.getOnlinePlayers()) atrybutyLpm(p, false);
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

  /** Zaklecie w lewej, a prawa pusta albo z zakleciem (z bronia w prawej LPM dalej bije bronia). */
  boolean trybLpm(Player p) {
    ItemStack prawa = p.getInventory().getItemInMainHand();
    return ksiega.w(p.getInventory().getItemInOffHand()) != null && (prawa.getType().isAir() || ksiega.w(prawa) != null);
  }

  private void trybyLpm() { for (Player p : Bukkit.getOnlinePlayers()) atrybutyLpm(p, trybLpm(p)); }

  private void atrybutyLpm(Player p, boolean wl) {
    mod(p, Attribute.BLOCK_INTERACTION_RANGE, kZasieg, 59.5, AttributeModifier.Operation.ADD_NUMBER, wl);   // 4,5 -> 64
    mod(p, Attribute.ENTITY_INTERACTION_RANGE, kIstoty, -3, AttributeModifier.Operation.ADD_NUMBER, wl);   // 3 -> 0
    mod(p, Attribute.BLOCK_BREAK_SPEED, kKopanie, -1, AttributeModifier.Operation.ADD_SCALAR, wl);          // x0
  }

  private static void mod(Player p, Attribute a, NamespacedKey k, double v, AttributeModifier.Operation op, boolean wl) {
    AttributeInstance ai = p.getAttribute(a);
    if (ai == null) return;
    boolean jest = ai.getModifier(k) != null;
    if (wl && !jest) ai.addTransientModifier(new AttributeModifier(k, v, op, EquipmentSlotGroup.ANY));
    else if (!wl && jest) ai.removeModifier(k);
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void lpmStart(BlockDamageEvent e) {
    Player p = e.getPlayer();
    if (!trybLpm(p)) return;
    e.setInstaBreak(false);
    if (wybor != null && wybor.otwarty(p)) return;
    lpmDo.put(p.getUniqueId(), Long.MAX_VALUE);
    wejdz(p);
  }

  @EventHandler
  public void lpmKoniec(BlockDamageAbortEvent e) {
    lpmDo.computeIfPresent(e.getPlayer().getUniqueId(), (k, v) -> v == Long.MAX_VALUE ? teraz + 3 : v);
  }

  /** Klik LPM w niebo (nic w zasiegu 64): impuls 0,5 s. */
  @EventHandler(priority = EventPriority.MONITOR)
  public void lpmPowietrze(PlayerInteractEvent e) {
    if (e.getAction() != Action.LEFT_CLICK_AIR || !trybLpm(e.getPlayer()) || swiezyPpm(e.getPlayer())) return;
    if (wybor != null && wybor.otwarty(e.getPlayer())) return;
    lpmDo.merge(e.getPlayer().getUniqueId(), teraz + 10, Math::max);
    wejdz(e.getPlayer());
  }

  /** W trybie LPM blok nie moze pasc (tryb kreatywny lamie od razu) ani byc "uzyty" z 64 blokow. */
  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void bezNiszczenia(BlockBreakEvent e) { if (trybLpm(e.getPlayer())) e.setCancelled(true); }

  @EventHandler(priority = EventPriority.LOW)
  public void bezDalekiegoUzycia(PlayerInteractEvent e) {
    if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null || !trybLpm(e.getPlayer())) return;
    if (e.getClickedBlock().getLocation().add(0.5, 0.5, 0.5).distance(e.getPlayer().getEyeLocation()) > 5)
      e.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
  }

  // ------------------------------------------------------------------ PPM

  @EventHandler(priority = EventPriority.MONITOR)
  public void uzycie(PlayerInteractEvent e) {
    if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    ostatniPpm.put(e.getPlayer().getUniqueId(), teraz);
    if (ksiega.w(e.getItem()) == null) return;
    Player p = e.getPlayer();
    // Uzycie przedmiotu zaczyna sie po obsludze zdarzenia — sprawdzamy w nastepnym ticku, czy reka jest uniesiona.
    Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline() && p.isHandRaised()) wejdz(p); });
  }

  @EventHandler
  public void wyjscie(PlayerQuitEvent e) {
    rzucajacy.remove(e.getPlayer().getUniqueId()); lpmDo.remove(e.getPlayer().getUniqueId()); ostatniPpm.remove(e.getPlayer().getUniqueId());
    strumien.stop(e.getPlayer()); atrybutyLpm(e.getPlayer(), false);
  }

  private void wejdz(Player p) { rzucajacy.putIfAbsent(p.getUniqueId(), new Stan()); }

  void stop(Player p, boolean dzwiek) {
    Stan s = rzucajacy.get(p.getUniqueId());
    lpmDo.remove(p.getUniqueId());
    if (s == null) return;
    if (dzwiek && p.isOnline() && (s.prawa || s.lewa)) skill(p, ksiega.skStop);   // przed usunieciem: warunki widza dlonie
    rzucajacy.remove(p.getUniqueId());
    strumien.stop(p);
  }

  // ------------------------------------------------------------------ tik

  /** Ktorys przycisk rzucania jest trzymany (PPM = uzywanie przedmiotu, LPM = kopanie/impuls). */
  private boolean trzyma(Player p) {
    Long d = lpmDo.get(p.getUniqueId());
    return (p.isHandRaised() && ksiega.w(p.getActiveItem()) != null) || (d != null && d >= teraz);
  }

  /**
   * Ctrl + LPM/PPM = obie dlonie naraz (polaczone; to samo zaklecie = podwojne). Doslowne "oba przyciski naraz" jest
   * niemozliwe: klient Minecrafta przy trzymanym PPM ignoruje LPM, a przy kopaniu nie zaczyna uzywania — serwer
   * nigdy nie dostaje drugiego przycisku (zmierzone 30.09 prawdziwym klientem).
   */
  private boolean razem(Player p) { return p.getCurrentInput().isSprint() && trzyma(p); }

  private boolean prawaTeraz(Player p) {
    if (ksiega.w(p.getInventory().getItemInMainHand()) == null) return false;
    return (p.isHandRaised() && p.getActiveItemHand() == EquipmentSlot.HAND) || razem(p);
  }

  private boolean lewaTeraz(Player p) {
    if (ksiega.w(p.getInventory().getItemInOffHand()) == null) return false;
    if (p.isHandRaised() && p.getActiveItemHand() == EquipmentSlot.OFF_HAND) return true;   // bron w prawej + PPM
    if (razem(p)) return true;
    Long d = lpmDo.get(p.getUniqueId());
    return d != null && d >= teraz && trybLpm(p);
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
      if (p == null || !p.isOnline() || p.isDead()) { it.remove(); lpmDo.remove(wpis.getKey()); if (p != null) strumien.stop(p); continue; }
      try {
        boolean pr = prawaTeraz(p), lw = lewaTeraz(p);
        if (!pr && !lw) {                                        // obie dlonie puszczone — koniec
          if (s.prawa || s.lewa) skill(p, ksiega.skStop);
          it.remove(); lpmDo.remove(p.getUniqueId()); strumien.stop(p);
          continue;
        }
        if (pr != s.prawa || lw != s.lewa) {                     // zmiana aktywnych dloni
          boolean nowa = (pr && !s.prawa) || (lw && !s.lewa);
          s.prawa = pr; s.lewa = lw;
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
            it.remove(); lpmDo.remove(p.getUniqueId()); strumien.stop(p);
            continue;
          }
          d.giveMana(-koszt, ResourceUpdateReason.SKILL);
          skill(p, ksiega.skTik);
          List<Entity> cele = stozek(p, podwojne(p, s) ? ksiega.stozekPodwojny : ksiega.stozek);
          if (!cele.isEmpty()) trafienia(p, cele);
        }
      } catch (Throwable t) {   // np. nieaktualne dane MMOCore po szybkim przelogowaniu: gasimy tylko tego gracza
        it.remove(); lpmDo.remove(p.getUniqueId()); strumien.stop(p);
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
