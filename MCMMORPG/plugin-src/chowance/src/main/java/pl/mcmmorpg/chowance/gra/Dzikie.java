package pl.mcmmorpg.chowance.gra;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.lumine.mythic.bukkit.events.MythicMobLootDropEvent;
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.dane.ProfilDane;
import pl.mcmmorpg.chowance.integracja.Mythic;
import pl.mcmmorpg.chowance.logika.Generator;
import pl.mcmmorpg.chowance.logika.Statystyki;
import pl.mcmmorpg.chowance.logika.Wybieg;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Osobnik;
import pl.mcmmorpg.chowance.model.Stat;
import pl.mcmmorpg.chowance.model.Status;

/**
 * Dzikie stwory: rozpoznanie po typie moba (KONTRAKT §10.1), osobnik zapisany w PDC ciała (przeżywa
 * rozładowanie chunka i restart), lśniące 1/1000, statusy łapania, „zbiegli”, okno legendarne,
 * skan „widzianych” do Bestiariusza (co bestiariusz.skan_s sekund, nie co tick).
 */
public final class Dzikie implements Listener {
  private final Chowance c;
  private final Ciala ciala;
  private final NamespacedKey kWybiegBezLupu;
  private final Map<UUID, Osobnik> cache = new ConcurrentHashMap<>();
  private final Map<UUID, Status> statusy = new ConcurrentHashMap<>();
  private final Map<UUID, Long> statusDo = new ConcurrentHashMap<>();
  private final Map<UUID, UUID> zbiegli = new ConcurrentHashMap<>();
  private final Map<UUID, Long> zbiegliDo = new ConcurrentHashMap<>();
  private final Map<UUID, Long> oknoLegendarne = new ConcurrentHashMap<>();
  /** ciała w trakcie łapania albo walki turowej — nietykalne dla innych */
  public final java.util.Set<UUID> zajete = ConcurrentHashMap.newKeySet();

  public Dzikie(Chowance c, Ciala ciala) {
    this.c = c;
    this.ciala = ciala;
    this.kWybiegBezLupu = new NamespacedKey(c.plugin, "wybieg_bez_lupu");
  }

  public static Osobnik osobnikZCiala(Chowance c, Entity e) {
    if (e == null) return null;
    String json = e.getPersistentDataContainer().get(c.kDziki, PersistentDataType.STRING);
    if (json == null) return null;
    try {
      return c.dane.gson().fromJson(json, Osobnik.class);
    } catch (RuntimeException ex) {
      return null;
    }
  }

  public Osobnik osobnik(Entity e) {
    if (e == null) return null;
    Osobnik o = cache.get(e.getUniqueId());
    if (o != null) return o;
    o = osobnikZCiala(c, e);
    if (o != null) cache.put(e.getUniqueId(), o);
    return o;
  }

  public boolean dziki(Entity e) { return osobnik(e) != null; }

  public void zapisz(Entity e, Osobnik o) {
    e.getPersistentDataContainer().set(c.kDziki, PersistentDataType.STRING, c.dane.gson().toJson(o));
    cache.put(e.getUniqueId(), o);
  }

  // ------------------------------------------------------------------ spawn

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void spawn(MythicMobSpawnEvent e) {
    String[] g = c.rej.poMobie().get(e.getMobType().getInternalName());
    if (g == null || !(g[1].equals("D") || g[1].equals("DL"))) return;
    Entity ent = e.getEntity();
    var spawner = e.getMythicSpawner();
    if (spawner != null && Wybieg.spawnerPasuje(spawner.getInternalName(), c.ust.wybiegSpawneryPrefiksy)) {
      ent.getPersistentDataContainer().set(kWybiegBezLupu, PersistentDataType.BYTE, (byte) 1);
    }
    int poziom = (int) Math.max(1, Math.min(100, Math.round(e.getMobLevel())));
    Bukkit.getScheduler().runTask(c.plugin, () -> przygotuj(ent, g[0], poziom, g[1].equals("DL")));
  }

  /** Nadaje dzikiemu ciału osobnika (albo losuje lśnienie i podmienia ciało). */
  public void przygotuj(Entity ent, String gatunekId, int poziom, boolean lsniacy) {
    if (!(ent instanceof LivingEntity le) || !le.isValid()) return;
    Gatunek g = c.rej.gatunek(gatunekId);
    if (g == null || osobnik(le) != null) return;
    if (!lsniacy && Generator.losujLsniacy(c.ust.szansaLsniacego, ThreadLocalRandom.current())) {
      String mobL = g.mobDziki(true);
      if (Mythic.mobIstnieje(mobL) && !mobL.equals(g.mobDziki(false))) {
        Location l = le.getLocation();
        boolean bezLupu = le.getPersistentDataContainer().has(kWybiegBezLupu, PersistentDataType.BYTE);
        Mythic.usun(le);
        var am = Mythic.spawn(mobL, l, poziom);
        if (am != null && am.getEntity() != null) {
          Entity nowe = am.getEntity().getBukkitEntity();
          if (bezLupu) nowe.getPersistentDataContainer().set(kWybiegBezLupu, PersistentDataType.BYTE, (byte) 1);
          przygotuj(nowe, gatunekId, poziom, true);
          Mythic.sygnal(nowe, "LSNIACY", null);
        }
        return;
      }
      lsniacy = true; // brak osobnego ciała: to samo ciało, oznaczone i podświetlone
    }
    Osobnik o = Generator.nowy(c.rej, g, poziom, lsniacy, ThreadLocalRandom.current());
    o.pochodzenie = "DZIKI";
    zapisz(le, o);
    ciala.ustawZdrowie(le, Statystyki.stat(g, o, c.rej.charakter(o.charakter), Stat.ZDROWIE, 0, c.rej.cecha(o.cecha)), Double.MAX_VALUE);
    ciala.nazwij(le, o, null);
    if (lsniacy) {
      le.setGlowing(true);
      le.getWorld().spawnParticle(Particle.FIREWORK, le.getLocation().add(0, 1, 0), 40, 0.4, 0.6, 0.4, 0.05);
      ogloszenieLsniacego(le, o, g);
    }
  }

  private void ogloszenieLsniacego(LivingEntity le, Osobnik o, Gatunek g) {
    Location l = le.getLocation();
    if (c.ust.ogloszenieLsniacego) {
      for (Player p : l.getWorld().getPlayers()) {
        if (p.getLocation().distanceSquared(l) < 96 * 96) {
          c.msg(p, "<gold>✦ W pobliżu pojawił się LŚNIĄCY " + g.nazwa() + "! <gray>(" + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() + ")");
          p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1.4f);
        }
      }
    }
    c.zdarzenie("LSNIACY_POJAWIL_SIE", Map.of("gatunek", g.id(), "nazwa", g.nazwa(), "x", String.valueOf(l.getBlockX()),
        "y", String.valueOf(l.getBlockY()), "z", String.valueOf(l.getBlockZ()), "swiat", l.getWorld().getName()));
  }

  /** /chw admin spawn — dziki z ciałem B albo zastępczym. */
  public LivingEntity postaw(Gatunek g, Location l, int poziom, boolean lsniacy) {
    Ciala.Cialo cialo = ciala.postaw(g, g.mobDziki(lsniacy).isBlank() ? g.mobOswojony(lsniacy) : g.mobDziki(lsniacy), l, poziom);
    if (cialo == null) return null;
    cialo.encja().setPersistent(true);
    Osobnik o = Generator.nowy(c.rej, g, poziom, lsniacy, ThreadLocalRandom.current());
    o.pochodzenie = "DZIKI";
    zapisz(cialo.encja(), o);
    ciala.ustawZdrowie(cialo.encja(), Statystyki.stat(g, o, c.rej.charakter(o.charakter), Stat.ZDROWIE, 0, c.rej.cecha(o.cecha)), Double.MAX_VALUE);
    ciala.nazwij(cialo.encja(), o, null);
    if (lsniacy) cialo.encja().setGlowing(true);
    return cialo.encja();
  }

  // ------------------------------------------------------------------ statusy, zbiegli, szał

  public void nalozStatus(Entity e, Status s, int sekund) {
    statusy.put(e.getUniqueId(), s);
    statusDo.put(e.getUniqueId(), System.currentTimeMillis() + sekund * 1000L);
    if (e instanceof LivingEntity le) {
      if (s == Status.SEN || s == Status.SPETANIE) le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, sekund * 20, s == Status.SEN ? 6 : 2, false, true));
      le.getWorld().spawnParticle(s == Status.SEN ? Particle.CLOUD : Particle.CRIT, le.getLocation().add(0, 1.2, 0), 8, 0.2, 0.2, 0.2, 0.01);
    }
  }

  public Status status(Entity e) {
    Long d = statusDo.get(e.getUniqueId());
    if (d == null) return null;
    if (d < System.currentTimeMillis()) {
      statusDo.remove(e.getUniqueId());
      statusy.remove(e.getUniqueId());
      return null;
    }
    return statusy.get(e.getUniqueId());
  }

  public void oznaczZbieglego(Entity e, Player od) {
    zbiegli.put(e.getUniqueId(), od.getUniqueId());
    zbiegliDo.put(e.getUniqueId(), System.currentTimeMillis() + c.ust.zbieglySek * 1000L);
  }

  /** Gracz, od którego stwór zbiegł (i okno jeszcze trwa), albo null. */
  public UUID zbieglyOd(Entity e) {
    Long d = zbiegliDo.get(e.getUniqueId());
    if (d == null || d < System.currentTimeMillis()) {
      zbiegli.remove(e.getUniqueId());
      zbiegliDo.remove(e.getUniqueId());
      return null;
    }
    return zbiegli.get(e.getUniqueId());
  }

  public void szal(LivingEntity e, Player p) {
    int t = c.ust.szalSek * 20;
    e.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, t, 0, false, true));
    e.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, t, 1, false, true));
    e.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, e.getLocation().add(0, 1.4, 0), 6, 0.3, 0.2, 0.3, 0);
    if (e instanceof org.bukkit.entity.Mob m) m.setTarget(p);
    Mythic.sygnal(e, "SZAL", p);
  }

  /**
   * Gracz bije dzikiego słabiej niż zwykłego moba (dzikie_walka.obrazenia_od_gracza): HP dzikich jest w skali
   * silnika (poz. 5 ≈ 20 HP), a kamienny miecz zdejmował ~30% na cios — walka kończyła się w 3 ciosy, zanim dziki
   * zdążył odpowiedzieć. Po zmianie walka trwa kilka wymian, a dziki realnie oddaje (MythicRejestracja.wGracza).
   */
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void graczBijeDzikiego(EntityDamageByEntityEvent e) {
    Entity d = e.getDamager();
    if (d instanceof org.bukkit.entity.Projectile pr && pr.getShooter() instanceof Entity s) d = s;
    if (!(d instanceof Player) || !(e.getEntity() instanceof LivingEntity le) || osobnik(le) == null) return;
    e.setDamage(e.getDamage() * c.ust.graczWDzikiego);
  }

  /** MythicMobs 5.11.2: LootDropEvent nie jest Cancellable; czyścimy fizyczny LootBag, zachowując EXP. */
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void bezLupuMythic(MythicMobLootDropEvent e) {
    if (!c.ust.wybiegBezLupu || e.getEntity() == null || e.getDrops() == null
        || !e.getEntity().getPersistentDataContainer().has(kWybiegBezLupu, PersistentDataType.BYTE)) return;
    e.getDrops().setLootTable(new ArrayList<>());
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void bezLupuVanilla(EntityDeathEvent e) {
    if (c.ust.wybiegBezLupu && e.getEntity().getPersistentDataContainer().has(kWybiegBezLupu, PersistentDataType.BYTE)) {
      e.getDrops().clear();
    }
  }

  // ------------------------------------------------------------------ legendarne

  public boolean wOknieLegendarnym(Entity e) {
    Long d = oknoLegendarne.get(e.getUniqueId());
    return d != null && d > System.currentTimeMillis();
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void legendarnyPokonany(EntityDamageEvent e) {
    if (!(e.getEntity() instanceof LivingEntity le)) return;
    // /kill i pustka zawsze działają — admin musi móc usunąć zablokowane ciało (PROBLEMY A-018).
    if (e.getCause() == EntityDamageEvent.DamageCause.KILL || e.getCause() == EntityDamageEvent.DamageCause.VOID) return;
    // W trakcie łapania, walki turowej albo w oknie legendarnym ciało jest nietykalne dla świata.
    if (zajete.contains(le.getUniqueId()) || oknoLegendarne.containsKey(le.getUniqueId())) {
      e.setCancelled(true);
      return;
    }
    Osobnik o = osobnik(le);
    if (o == null) return;
    Gatunek g = c.gatunek(o);
    if (g == null || !g.legendarny()) return;
    if (e.getFinalDamage() < le.getHealth()) return;
    e.setCancelled(true);
    le.setHealth(1);
    le.setAI(false);
    c.debug("legendarny " + g.id() + " pokonany (" + e.getCause() + ", dmg=" + e.getFinalDamage() + ") — okno " + c.ust.oknoLegendarneS + " s");
    oknoLegendarne.put(le.getUniqueId(), System.currentTimeMillis() + c.ust.oknoLegendarneS * 1000L);
    Player sprawca = null;
    if (e instanceof EntityDamageByEntityEvent ed) {
      if (ed.getDamager() instanceof Player p) sprawca = p;
    }
    Mythic.sygnal(le, "POKONANY", sprawca);
    for (Player p : le.getWorld().getPlayers()) {
      if (p.getLocation().distanceSquared(le.getLocation()) < 64 * 64) {
        c.msg(p, "<gold>" + g.nazwa() + " słabnie! <yellow>Masz " + c.ust.oknoLegendarneS + " s, by użyć: " + c.nazwyMmo.nazwa(g.wymaganaPieczec()) + ".");
      }
    }
    UUID id = le.getUniqueId();
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      if (oknoLegendarne.remove(id) != null && le.isValid() && !zajete.contains(id)) {
        le.getWorld().spawnParticle(Particle.REVERSE_PORTAL, le.getLocation().add(0, 1, 0), 60, 0.5, 1, 0.5, 0.1);
        for (Player p : le.getWorld().getPlayers()) {
          if (p.getLocation().distanceSquared(le.getLocation()) < 64 * 64) c.msg(p, "<gray>" + g.nazwa() + " odlatuje, zanim ktokolwiek go związał.");
        }
        Mythic.usun(le);
      }
    }, c.ust.oknoLegendarneS * 20L);
  }

  public void zamknijOkno(Entity e) { oknoLegendarne.remove(e.getUniqueId()); }

  // ------------------------------------------------------------------ Bestiariusz: widziani

  public void skan() {
    double r = c.ust.skanZasieg;
    for (Player p : Bukkit.getOnlinePlayers()) {
      ProfilDane d = c.dane(p);
      if (d == null) continue;
      for (Entity e : p.getNearbyEntities(r, r, r)) {
        if (!(e instanceof LivingEntity) || e instanceof Player) continue;
        Osobnik o = osobnik(e);
        if (o != null && d.oznaczWidziany(o.gatunek)) {
          Gatunek g = c.gatunek(o);
          if (g != null && !g.testowy()) c.pasek(p, "<aqua>Bestiariusz: nowy wpis — " + g.nazwa());
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void usuniete(EntityRemoveFromWorldEvent e) {
    UUID id = e.getEntity().getUniqueId();
    cache.remove(id);
    statusy.remove(id);
    statusDo.remove(id);
    zbiegli.remove(id);
    zbiegliDo.remove(id);
    // oknoLegendarne NIE tutaj: event leci też przy rozładowaniu chunka; wpis usuwa zaplanowane zamknięcie okna.
  }

  public int rozmiarCache() { return cache.size(); }

  /** /chw admin sprzatnij — usuwa dzikie ciała w promieniu (bez obrażeń, więc także legendarne w oknie). */
  public int sprzatnij(Location l, double r) {
    int n = 0;
    for (Entity e : l.getWorld().getNearbyEntities(l, r, r, r)) {
      if (e instanceof LivingEntity && !(e instanceof Player) && osobnik(e) != null && !zajete.contains(e.getUniqueId())) {
        oknoLegendarne.remove(e.getUniqueId());
        Mythic.usun(e);
        n++;
      }
    }
    return n;
  }
}
