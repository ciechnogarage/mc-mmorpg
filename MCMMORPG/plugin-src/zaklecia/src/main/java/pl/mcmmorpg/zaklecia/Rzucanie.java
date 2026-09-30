package pl.mcmmorpg.zaklecia;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.lib.player.resource.ResourceUpdateReason;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Rzucanie zaklec koncentracji obiema rekami. Crucible ~onUse obsluguje tylko glowna reke (sonda 2026-09-29:
 * zaklecie w drugiej rece przy mieczu w glownej nie ruszalo), wiec start/stop/tiki prowadzi plugin, na watku
 * glownym, a MythicMobs dostaje tylko skille efektow.
 *
 * Kto rzuca: wszystkie dlonie, w ktorych jest zaklecie. PPM uzywa najpierw glownej reki (zaklecie w prawej),
 * a gdy ta nie ma akcji (np. miecz) — drugiej. Stad tryby: prawa, obie (podwojne albo dwa rozne), miecz + lewa.
 */
final class Rzucanie implements Listener {
  private final Zaklecia plugin;
  private final Ksiega ksiega;
  /** gracz -> licznik tickow od startu rzucania */
  private final Map<UUID, Integer> rzucajacy = new HashMap<>();

  Rzucanie(Zaklecia plugin, Ksiega ksiega) {
    this.plugin = plugin;
    this.ksiega = ksiega;
    this.strumien = new Strumien(plugin, ksiega);
    Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
    Bukkit.getScheduler().runTaskTimer(plugin, this::dlonie, 4, 4);
  }

  private final Strumien strumien;

  /** Wylaczenie pluginu: zdejmij wszystkie strumienie. */
  void wylacz() { strumien.stopWszystkie(); rzucajacy.clear(); }

  boolean rzuca(Player p) { return rzucajacy.containsKey(p.getUniqueId()); }

  private void skill(Player p, String nazwa) {
    try {
      MythicBukkit.inst().getAPIHelper().castSkill(p, nazwa);
    } catch (Throwable t) {
      plugin.getLogger().log(Level.WARNING, "skill " + nazwa + ": " + t, t);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void uzycie(PlayerInteractEvent e) {
    if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    if (ksiega.w(e.getItem()) == null) return;
    Player p = e.getPlayer();
    // Uzycie przedmiotu zaczyna sie po obsludze zdarzenia — sprawdzamy w nastepnym ticku, czy reka jest uniesiona.
    Bukkit.getScheduler().runTask(plugin, () -> {
      if (p.isOnline() && !rzuca(p) && p.isHandRaised() && ksiega.w(p.getActiveItem()) != null) start(p);
    });
  }

  @EventHandler
  public void puszczenie(PlayerStopUsingItemEvent e) {
    if (rzuca(e.getPlayer())) stop(e.getPlayer(), true);
  }

  @EventHandler
  public void wyjscie(PlayerQuitEvent e) { rzucajacy.remove(e.getPlayer().getUniqueId()); strumien.stop(e.getPlayer()); }

  private void start(Player p) {
    rzucajacy.put(p.getUniqueId(), 0);
    skill(p, ksiega.skStart);
    strumien.start(p, ksiega.w(p.getInventory().getItemInMainHand()), ksiega.w(p.getInventory().getItemInOffHand()), zasieg(p));
  }

  void stop(Player p, boolean dzwiek) {
    strumien.stop(p);
    if (rzucajacy.remove(p.getUniqueId()) != null && dzwiek && p.isOnline()) skill(p, ksiega.skStop);
  }

  /** Koszt many za tik: suma rak; to samo zaklecie w obu = jedna reka x mnoznik podwojnego rzucania. */
  double koszt(Player p) {
    Ksiega.Zaklecie prawa = ksiega.w(p.getInventory().getItemInMainHand());
    Ksiega.Zaklecie lewa = ksiega.w(p.getInventory().getItemInOffHand());
    if (prawa != null && lewa != null && prawa.id().equals(lewa.id())) return prawa.manaZaTik() * ksiega.podwojneMnoznik;
    return (prawa == null ? 0 : prawa.manaZaTik()) + (lewa == null ? 0 : lewa.manaZaTik());
  }

  private void tick() {
    for (var it = rzucajacy.entrySet().iterator(); it.hasNext(); ) {
      var wpis = it.next();
      Player p = Bukkit.getPlayer(wpis.getKey());
      if (p == null || p.isDead() || !p.isHandRaised() || ksiega.w(p.getActiveItem()) == null) {
        it.remove();
        if (p != null) strumien.stop(p);
        if (p != null && p.isOnline()) skill(p, ksiega.skStop);
        continue;
      }
      try {
      int n = wpis.getValue();
      wpis.setValue(n + 1);
      strumien.tick(p, zasieg(p), n);
      if (n % 2 == 0 && !ksiega.skObraz.isBlank()) skill(p, ksiega.skObraz);
      if (n % 10 == 0) {
        double koszt = koszt(p);
        PlayerData d = PlayerData.get(p);
        if (d.getMana() < koszt) {
          it.remove();
          strumien.stop(p);
          skill(p, ksiega.skBrakMany);
          continue;
        }
        d.giveMana(-koszt, ResourceUpdateReason.SKILL);
        skill(p, ksiega.skTik);
        List<Entity> cele = stozek(p, podwojne(p) ? ksiega.stozekPodwojny : ksiega.stozek);
        if (!cele.isEmpty()) trafienia(p, cele);
      }
      } catch (Throwable t) {   // np. nieaktualne dane MMOCore po szybkim przelogowaniu: gasimy tylko tego gracza
        it.remove();
        strumien.stop(p);
        plugin.getLogger().warning("rzucanie przerwane (" + p.getName() + "): " + t);
      }
    }
  }

  private double zasieg(Player p) { return (podwojne(p) ? ksiega.stozekPodwojny : ksiega.stozek).zasieg(); }

  /** To samo zaklecie w obu dloniach. */
  private boolean podwojne(Player p) {
    Ksiega.Zaklecie a = ksiega.w(p.getInventory().getItemInMainHand()), b = ksiega.w(p.getInventory().getItemInOffHand());
    return a != null && b != null && a.id().equals(b.id());
  }

  /**
   * Cele stozka liczone tutaj, nie targeterem MM: @EntitiesInCone w skillu wolanym przez API (castSkill) zwracal
   * samego rzucajacego i zadnego moba (sonda 2026-09-29, klon). Istota zywa (bez rzucajacego, stojakow),
   * w zasiegu od oczu, kat do srodka hitboxa <= pol-kat (+ poprawka na szerokosc hitboxa blisko), linia wzroku.
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
    try {
      MythicBukkit.inst().getAPIHelper().castSkill(p, ksiega.skTrafienia, p, p.getLocation(), cele, List.of(), 1f);
    } catch (Throwable t) {
      plugin.getLogger().log(Level.WARNING, "trafienia: " + t, t);
    }
  }

  /** Zar na dloni, gdy zaklecie jest w rece, ale gracz nie rzuca. */
  private void dlonie() {
    if (ksiega.skDlon.isBlank()) return;   // modele dloni animuja sie same; czasteczki przy dloni opcjonalne
    for (Player p : Bukkit.getOnlinePlayers()) {
      if (rzuca(p)) continue;
      if (ksiega.w(p.getInventory().getItemInMainHand()) != null || ksiega.w(p.getInventory().getItemInOffHand()) != null)
        skill(p, ksiega.skDlon);
    }
  }
}
