package pl.mcmmorpg.chowance.gra;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.util.Vector;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.api.event.ChowaniecPrzywolanyEvent;
import pl.mcmmorpg.chowance.dane.ProfilDane;
import pl.mcmmorpg.chowance.integracja.Mythic;
import pl.mcmmorpg.chowance.logika.Rozwoj;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Osobnik;

/**
 * Aktywny stwór gracza: jeden na gracza (SPEC §5, §12). Despawn przy wyjściu, zmianie profilu, śmierci,
 * zmianie świata, wyłączeniu pluginu i omdleniu. Ciało nieprzetrwałe + sprzątanie sierot przy ładowaniu chunków.
 * Jedno zadanie co 20 ticków (nie co tick): podążanie ciał zastępczych, teleport za daleko, regeneracja.
 */
public final class Przywolania implements Listener {
  private final Chowance c;
  private final Ciala ciala;
  private final Druzyny druzyny;
  /** gracz -> aktywny */
  private final Map<UUID, Aktywny> aktywne = new ConcurrentHashMap<>();
  /** ciało -> gracz */
  private final Map<UUID, UUID> poCiele = new ConcurrentHashMap<>();
  /** walki blokujące przywołanie/odwołanie (ustawiane przez walki) */
  public java.util.function.Predicate<Player> wWalce = p -> false;
  public Towarzysz towarzysz;
  public java.util.function.BiConsumer<UUID, String> poOdwolaniu = (p, o) -> {};

  public static final class Aktywny {
    public final UUID gracz;
    public final String osobnikId;
    public final LivingEntity cialo;
    public final boolean mythic;
    public long ostatniaWalka;
    public Location ostatniaPozycja;
    public long bezruchOd, ambientTermin;
    public boolean podszedl, odpoczal, niskiHpUzbrojony = true;
    public String spoczynekModel, spoczynekAnimacja;
    public org.bukkit.Location spoczynekPozycja;
    Aktywny(UUID gracz, String osobnikId, LivingEntity cialo, boolean mythic) {
      this.gracz = gracz; this.osobnikId = osobnikId; this.cialo = cialo; this.mythic = mythic;
    }
  }

  /** Ciała, których chowańce nie atakują (łapane pieczęcią, w walce turowej); ustawia ChowancePlugin. */
  public java.util.function.Predicate<Entity> nietykalny = e -> false;

  public Przywolania(Chowance c, Ciala ciala, Druzyny druzyny) {
    this.c = c;
    this.ciala = ciala;
    this.druzyny = druzyny;
    druzyny.przywolania = this;
  }

  public Aktywny aktywny(Player p) { return aktywne.get(p.getUniqueId()); }

  public Collection<Aktywny> wszystkie() { return aktywne.values(); }

  public Player wlascicielCiala(Entity e) {
    UUID g = e == null ? null : poCiele.get(e.getUniqueId());
    return g == null ? null : Bukkit.getPlayer(g);
  }

  public Osobnik osobnik(Player p, Aktywny a) {
    ProfilDane d = c.dane(p);
    return d == null || a == null ? null : d.znajdz(a.osobnikId);
  }

  public Osobnik osobnikAktywny(Player p) { return osobnik(p, aktywny(p)); }

  /** HP z ciała, jeśli ten osobnik jest właśnie przywołany. */
  public Double hpCiala(Player p, Osobnik o) {
    Aktywny a = aktywny(p);
    if (a == null || !a.osobnikId.equals(o.id) || !a.cialo.isValid()) return null;
    return a.cialo.getHealth();
  }

  // ------------------------------------------------------------------ przywołanie / odwołanie

  public boolean przywolaj(Player p, Osobnik o) { return przywolaj(p, o, false); }

  /** {@code wPojedynku} = zmiana stwora przez sam pojedynek (blokada „trwa walka” nie dotyczy). */
  public boolean przywolaj(Player p, Osobnik o, boolean wPojedynku) {
    ProfilDane d = c.daneLubBlad(p);
    if (d == null) return false;
    if (!wPojedynku && wWalce.test(p)) { c.blad(p, "Nie możesz teraz zmieniać stwora — trwa walka."); return false; }
    if (!d.druzyna.contains(o)) { c.blad(p, "Przywołać można tylko stwora z drużyny."); return false; }
    if (p.isDead()) return false;
    if (c.ust.swiatyZabronione.contains(p.getWorld().getName())) { c.blad(p, "Tu nie można przywoływać chowańców."); return false; }
    Gatunek g = c.gatunek(o);
    if (g == null) { c.blad(p, "Nieznany gatunek " + o.gatunek + " — zgłoś administracji."); return false; }
    if (druzyny.zemdlony(p, o)) { c.blad(p, c.nazwa(o) + " jest wyczerpany. Ulecz go eliksirem albo odczekaj."); return false; }
    Aktywny stary = aktywny(p);
    if (stary != null) {
      if (stary.osobnikId.equals(o.id)) { c.pasek(p, "<gray>" + c.nazwa(o) + " już jest przy tobie."); return true; }
      odwolaj(p, false);
    }
    double hp = druzyny.hp(p, o);
    Location l = miejsceObok(p);
    Ciala.Cialo cialo = ciala.postaw(g, g.mobOswojony(o.lsniacy), l, o.poziom);
    if (cialo == null) { c.blad(p, "Nie udało się przywołać — to miejsce blokuje stwory (region?)."); return false; }
    LivingEntity e = cialo.encja();
    ciala.oznaczOswojony(e, o, p.getUniqueId());
    ciala.nazwij(e, o, null);
    if (cialo.mythic()) {
      ActiveMob am = Mythic.aktywny(e);
      if (am != null) am.setOwnerUUID(p.getUniqueId());
    } else if (e instanceof Tameable t) {
      t.setOwner(p);
      t.setTamed(true);
    }
    // Zdrowie PO nadaniu właściciela: MythicMobs oswaja ciało-wilka vanillowo, a vanilla ustawia wtedy
    // oswojonemu wilkowi max HP 40 (klon 2026-09-27: Upiorek poz. 5 „40/20”). Drugi raz tick później na wypadek
    // opóźnionych skutków oswojenia.
    ciala.ustawZdrowie(e, druzyny.hpMax(o), hp);
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      var attr = e.getAttribute(Attribute.MAX_HEALTH);
      if (e.isValid() && attr != null && Math.abs(attr.getBaseValue() - druzyny.hpMax(o)) > 0.01) ciala.ustawZdrowie(e, druzyny.hpMax(o), hp);
    }, 1L);
    Aktywny a = new Aktywny(p.getUniqueId(), o.id, e, cialo.mythic());
    aktywne.put(p.getUniqueId(), a);
    poCiele.put(e.getUniqueId(), p.getUniqueId());
    p.getWorld().spawnParticle(Particle.END_ROD, l.clone().add(0, 0.8, 0), 20, 0.3, 0.5, 0.3, 0.02);
    p.playSound(l, Sound.ENTITY_ALLAY_ITEM_GIVEN, 0.8f, 1.2f);
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> { if (e.isValid()) Mythic.sygnal(e, "PRZYWOLANIE", p); }, 1L);
    Bukkit.getPluginManager().callEvent(new ChowaniecPrzywolanyEvent(p, o, e));
    c.pasek(p, "<green>Przywołano: " + c.nazwaKolor(o));
    return true;
  }

  private Location miejsceObok(Player p) {
    Location l = p.getLocation().clone();
    Vector kier = l.getDirection().setY(0);
    if (kier.lengthSquared() < 0.01) kier = new Vector(1, 0, 0);
    kier.normalize().multiply(c.ust.odlegloscPrzywolania);
    Location cel = l.clone().add(kier);
    if (!cel.getBlock().isPassable() || !cel.clone().add(0, 1, 0).getBlock().isPassable()) cel = l.clone();
    cel.setYaw(l.getYaw() + 180);
    return cel;
  }

  /** Odwołanie z zapisem HP. {@code natychmiast} = bez animacji (wyjście, wyłączenie pluginu). */
  public void odwolaj(Player p, boolean natychmiast) {
    Aktywny a = aktywne.remove(p.getUniqueId());
    if (a == null) return;
    poCiele.remove(a.cialo.getUniqueId());
    poOdwolaniu.accept(p.getUniqueId(), a.osobnikId);
    Osobnik o = osobnik(p, a);
    if (o != null && a.cialo.isValid()) {
      druzyny.ustawHp(o, a.cialo.getHealth());
      ProfilDane d = c.dane(p);
      if (d != null) d.brudne = true;
    }
    if (natychmiast || !a.cialo.isValid()) {
      usunCialo(a.cialo);
      return;
    }
    Mythic.sygnal(a.cialo, "ODWOLANIE", p);
    a.cialo.setAI(false);
    a.cialo.setInvulnerable(true);
    a.cialo.getWorld().spawnParticle(Particle.END_ROD, a.cialo.getLocation().add(0, 0.8, 0), 15, 0.3, 0.5, 0.3, 0.02);
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> usunCialo(a.cialo), 10L);
  }

  /** Zmiana osobnika pod aktywnym ciałem (ewolucja/przywrócenie) — przywołuje ponownie. */
  public void przestaw(Player p, Osobnik o) {
    Aktywny a = aktywny(p);
    if (a == null || !a.osobnikId.equals(o.id)) return;
    odwolaj(p, true);
    przywolaj(p, o);
  }

  private void usunCialo(LivingEntity e) {
    if (e == null) return;
    Mythic.usun(e);
  }

  public void odwolajWszystkich() {
    for (UUID g : java.util.List.copyOf(aktywne.keySet())) {
      Player p = Bukkit.getPlayer(g);
      if (p != null) odwolaj(p, true);
      else {
        Aktywny a = aktywne.remove(g);
        if (a != null) { poOdwolaniu.accept(g, a.osobnikId); usunCialo(a.cialo); }
      }
    }
    poCiele.clear();
  }

  public void odswiezZdrowie(Player p, Osobnik o) {
    Aktywny a = aktywny(p);
    if (a == null || !a.osobnikId.equals(o.id) || !a.cialo.isValid()) return;
    double hp = Math.max(0.5, druzyny.hp(null, o));
    ciala.ustawZdrowie(a.cialo, druzyny.hpMax(o), hp);
    ciala.nazwij(a.cialo, o, null);
  }

  public void sygnalAktywnemu(Player p, Osobnik o, String sygnal) {
    Aktywny a = aktywny(p);
    if (a != null && a.osobnikId.equals(o.id) && a.cialo.isValid()) Mythic.sygnal(a.cialo, sygnal, p);
  }

  // ------------------------------------------------------------------ zadanie co 20 ticków

  public void tik() {
    long teraz = System.currentTimeMillis();
    for (Aktywny a : aktywne.values()) {
      Player p = Bukkit.getPlayer(a.gracz);
      if (p == null || !a.cialo.isValid()) continue;
      if (towarzysz != null) towarzysz.tik(a, p, teraz);
      if (wWalce.test(p)) continue;
      if (!a.cialo.getWorld().equals(p.getWorld())) continue;
      double dist = a.cialo.getLocation().distanceSquared(p.getLocation());
      if (dist > c.ust.teleportZaGraczem * c.ust.teleportZaGraczem) {
        a.cialo.teleport(miejsceObok(p));
      } else if (!a.mythic && dist > 36 && a.cialo instanceof Mob m && !(m instanceof Tameable t && t.isTamed())) {
        m.getPathfinder().moveTo(p, 1.2);
      }
      // regeneracja poza walką (10 s od ostatnich obrażeń)
      if (teraz - a.ostatniaWalka > 10_000 && c.ust.regenNaMinute > 0) {
        var attr = a.cialo.getAttribute(Attribute.MAX_HEALTH);
        double max = attr == null ? a.cialo.getHealth() : attr.getValue();
        if (a.cialo.getHealth() < max) a.cialo.setHealth(Math.min(max, a.cialo.getHealth() + max * c.ust.regenNaMinute / 100.0 / 60.0));
      }
    }
  }

  // ------------------------------------------------------------------ zdarzenia

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void obrazenia(EntityDamageEvent e) {
    UUID g = poCiele.get(e.getEntity().getUniqueId());
    if (g == null) return;
    Aktywny a = aktywne.get(g);
    if (a != null) a.ostatniaWalka = System.currentTimeMillis();
  }

  /** Właściciel nie bije swojego stwora i odwrotnie; stwór nie celuje we właściciela. */
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void swojNieBije(EntityDamageByEntityEvent e) {
    Entity damager = e.getDamager();
    if (damager instanceof Projectile pr && pr.getShooter() instanceof Entity s) damager = s;
    UUID wlOfiary = poCiele.get(e.getEntity().getUniqueId());
    UUID wlAtak = poCiele.get(damager.getUniqueId());
    if (wlOfiary != null && damager.getUniqueId().equals(wlOfiary)) e.setCancelled(true);
    if (wlAtak != null && e.getEntity().getUniqueId().equals(wlAtak)) e.setCancelled(true);
    // Poza walką turową i pojedynkiem (tam liczy silnik, bez obrażeń Bukkita) chowaniec nie rani graczy ani
    // cudzych chowańców, a gracz nie rani cudzego chowańca. Na world1 PvP nie jest ograniczone regionami,
    // a owner MythicMobs to nie Tameable — WorldGuard tego nie łapie (sesja właściciela 2026-09-27:
    // „Miau was slain by Upiorek Poz. 5” — stwór innego gracza).
    if (wlAtak != null && (e.getEntity() instanceof Player || wlOfiary != null)) e.setCancelled(true);
    if (wlOfiary != null && damager instanceof Player && !damager.getUniqueId().equals(wlOfiary)) e.setCancelled(true);
    if (e.isCancelled()) return;
    if (wlAtak != null) {
      Aktywny a = aktywne.get(wlAtak);
      if (a != null) a.ostatniaWalka = System.currentTimeMillis();
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void celowanie(EntityTargetLivingEntityEvent e) {
    UUID g = poCiele.get(e.getEntity().getUniqueId());
    if (g == null || e.getTarget() == null) return;
    // ani właściciel, ani inny gracz, ani cudzy chowaniec (zob. swojNieBije)
    if (e.getTarget() instanceof Player || poCiele.containsKey(e.getTarget().getUniqueId())) e.setCancelled(true);
    // stwór wiązany pieczęcią albo w walce turowej — chowaniec nie rzuca się na niego
    if (nietykalny.test(e.getTarget())) e.setCancelled(true);
    // ranny (< 25% HP) nie wraca do walki — wycofuje się (MythicRejestracja, recenzja R-12)
    if (e.getEntity() instanceof LivingEntity le) {
      var attr = le.getAttribute(Attribute.MAX_HEALTH);
      if (attr != null && le.getHealth() < attr.getValue() * 0.25) e.setCancelled(true);
    }
  }

  /** Ciało należy do gracza (przywołane). */
  public boolean oswojoneCialo(Entity e) { return e != null && poCiele.containsKey(e.getUniqueId()); }

  @EventHandler(priority = EventPriority.MONITOR)
  public void smierc(EntityDeathEvent e) {
    UUID g = poCiele.get(e.getEntity().getUniqueId());
    if (g != null) {
      e.getDrops().clear();
      e.setDroppedExp(0);
      Player p = Bukkit.getPlayer(g);
      Aktywny a = aktywne.remove(g);
      poCiele.remove(e.getEntity().getUniqueId());
      if (a != null) poOdwolaniu.accept(g, a.osobnikId);
      if (p != null && a != null) {
        Osobnik o = osobnik(p, a);
        if (o != null) {
          druzyny.ustawHp(o, 0);
          ProfilDane d = c.dane(p);
          if (d != null) d.brudne = true;
          c.msg(p, c.nazwaKolor(o) + " <red>jest wyczerpany i wraca do pieczęci.");
        }
      }
      return;
    }
    // exp PvE dla aktywnego stwora gracza, który (albo którego stwór) zadał ostatni cios
    if (!c.ust.pveExp) return;
    Player zabojca = e.getEntity().getKiller();
    if (e.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent ed) {
      Player w = wlascicielCiala(ed.getDamager());
      if (w != null) zabojca = w;
    }
    if (zabojca == null || e.getEntity() instanceof Player) return;
    Aktywny a = aktywny(zabojca);
    if (a == null) return;
    Osobnik o = osobnik(zabojca, a);
    ProfilDane d = c.dane(zabojca);
    if (o == null || d == null) return;
    long exp = expZaZabicie(e.getEntity());
    if (exp > 0) druzyny.dajExp(zabojca, d, o, exp);
    // dzielony exp dla reszty sprawnej drużyny (recenzja R-04)
    if (exp > 0 && c.ust.expDzielony > 0) {
      long czesc = Math.max(1, Math.round(exp * c.ust.expDzielony));
      for (Osobnik x : java.util.List.copyOf(d.druzyna)) if (x != o && !druzyny.zemdlony(zabojca, x)) druzyny.dajExp(zabojca, d, x, czesc, true);
    }
  }

  private long expZaZabicie(LivingEntity ofiara) {
    Osobnik dziki = Dzikie.osobnikZCiala(c, ofiara);
    if (dziki != null) {
      Gatunek g = c.gatunek(dziki);
      if (g != null) return Math.round(Rozwoj.expZaPokonanie(g, dziki.poziom, false, 1.0) * c.ust.expPve);
    }
    var attr = ofiara.getAttribute(Attribute.MAX_HEALTH);
    double max = attr == null ? 10 : attr.getValue();
    return Math.max(1, Math.round(max / 2.0 * c.ust.expPve));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void usuniete(EntityRemoveFromWorldEvent e) {
    UUID g = poCiele.remove(e.getEntity().getUniqueId());
    if (g == null) return;
    Aktywny a = aktywne.get(g);
    if (a == null || a.cialo != e.getEntity()) return;
    // Ciało zniknęło nie przez nas (rozładowany chunk, /kill, inny plugin): zapis HP i próba powrotu.
    aktywne.remove(g);
    poOdwolaniu.accept(g, a.osobnikId);
    Player p = Bukkit.getPlayer(g);
    if (p == null) return;
    Osobnik o = osobnik(p, a);
    if (o == null) return;
    if (a.cialo.getHealth() > 0) druzyny.ustawHp(o, a.cialo.getHealth());
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      if (p.isOnline() && aktywny(p) == null && !druzyny.zemdlony(p, o) && !wWalce.test(p)) {
        ProfilDane d = c.dane(p);
        if (d != null && d.druzyna.contains(o)) przywolaj(p, o);
      }
    }, 20L);
  }

  /** Sierota z poprzedniej sesji (nie powinna istnieć: ciała są nieprzetrwałe) — usuwamy przy ładowaniu. */
  @EventHandler
  public void sieroty(EntitiesLoadEvent e) {
    for (Entity x : e.getEntities()) {
      if (Ciala.instancja(c, x) != null && !poCiele.containsKey(x.getUniqueId())) {
        c.log.info("Usuwam osierocone ciało chowańca " + x.getUniqueId() + " (" + x.getType() + ")");
        Mythic.usun(x);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void wyjscie(PlayerQuitEvent e) { odwolaj(e.getPlayer(), true); }

  @EventHandler(priority = EventPriority.MONITOR)
  public void smiercGracza(PlayerDeathEvent e) { odwolaj(e.getPlayer(), true); }

  @EventHandler(priority = EventPriority.MONITOR)
  public void zmianaSwiata(PlayerChangedWorldEvent e) {
    if (c.ust.despawnZmianaSwiata) odwolaj(e.getPlayer(), true);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void teleport(PlayerTeleportEvent e) {
    Aktywny a = aktywny(e.getPlayer());
    if (a == null || e.getTo() == null || !e.getTo().getWorld().equals(e.getFrom().getWorld())) return;
    if (e.getFrom().distanceSquared(e.getTo()) < 64) return;
    Player p = e.getPlayer();
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      Aktywny b = aktywny(p);
      if (b != null && b.cialo.isValid() && !wWalce.test(p)) b.cialo.teleport(miejsceObok(p));
    }, 2L);
  }
}
