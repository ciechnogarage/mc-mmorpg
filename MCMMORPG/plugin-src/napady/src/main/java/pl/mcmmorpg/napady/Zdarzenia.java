package pl.mcmmorpg.napady;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.ServerLoadEvent;

/**
 * Brak friendly fire, brak lupu z cial napadu, strzaly znikaja po trafieniu, cele cial wybiera tylko
 * napad; gracz, ktory padl w napadzie, nic nie traci i oglada do konca jako widz. Ciosy i strzaly cial
 * ida do zapisu ("traf", "strzal") - z nich testy/kalibracja.py liczy liczby symulacji.
 */
final class Zdarzenia implements Listener {
  private final Napady pl;

  Zdarzenia(Napady pl) { this.pl = pl; }

  /** Jednostka ciala (mapa, bez petli po napadach) albo null. */
  private Jednostka jednostka(Entity e) {
    if (e == null || e instanceof Player) return null;
    Jednostka j = pl.cialaJednostek.get(e.getUniqueId());
    return j != null && !j.napad.skonczony() ? j : null;
  }

  private Napad napadEncji(Entity e) {
    if (e instanceof Player p) return pl.napadGracza(p);
    Jednostka j = jednostka(e);
    return j == null ? null : j.napad;
  }

  private static Entity sprawca(EntityDamageByEntityEvent ev) {
    Entity s = ev.getDamager();
    return s instanceof Projectile pr && pr.getShooter() instanceof Entity e ? e : s;
  }

  /**
   * Cialo MM zmienia cel tylko na rozkaz napadu (Mob#setTarget = CUSTOM). Reszta (np. vanilla po ciosie,
   * zapomnienie celu) anulowana, zeby AI nie rozbijalo szyku i rozkladu celow.
   */
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void cel(EntityTargetEvent ev) {
    if (ev.getReason() == EntityTargetEvent.TargetReason.CUSTOM) return;
    if (ev.getReason() == EntityTargetEvent.TargetReason.TARGET_DIED) return;
    if (jednostka(ev.getEntity()) != null) ev.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void obrazenia(EntityDamageByEntityEvent ev) {
    Entity sprawca = sprawca(ev);
    Napad nOfiara = napadEncji(ev.getEntity()), nSprawca = napadEncji(sprawca);
    if (nOfiara == null && nSprawca == null) return;
    // ciala napadu bije tylko druga strona tego samego napadu
    if (nOfiara != nSprawca) { ev.setCancelled(true); return; }
    Strona a = nOfiara.stronaEncji(sprawca), b = nOfiara.stronaEncji(ev.getEntity());
    if (a == null || a == b) ev.setCancelled(true);
  }

  /**
   * Cios gracza z napadu w cialo drugiej strony przepuszczamy mimo ochrony terenu (WorldGuard w regionie
   * bez czlonkostwa anuluje cios na NORMAL). Dotyczy tylko cial napadu.
   */
  @EventHandler(priority = EventPriority.HIGHEST)
  public void przepuscCiosNapadu(EntityDamageByEntityEvent ev) {
    if (!ev.isCancelled() || jednostka(ev.getEntity()) == null) return;
    Entity sprawca = sprawca(ev);
    if (!(sprawca instanceof Player)) return;
    Napad n = napadEncji(ev.getEntity());
    if (napadEncji(sprawca) != n) return;
    Strona a = n.stronaEncji(sprawca), b = n.stronaEncji(ev.getEntity());
    if (a != null && b != null && a != b) ev.setCancelled(false);
  }

  /** Do zapisu (po wszystkich pluginach): ciosy graczy w ciala, cial w graczy i cial miedzy soba. */
  @EventHandler(priority = EventPriority.MONITOR)
  public void cios(EntityDamageByEntityEvent ev) {
    Entity sprawca = sprawca(ev);
    Jednostka ofiara = jednostka(ev.getEntity()), napastnik = jednostka(sprawca);
    String dmg = Napad.r1(ev.getFinalDamage());
    if (sprawca instanceof Player p && ofiara != null) {
      Napad n = ofiara.napad;
      if (ev.isCancelled()) n.ciosyGraczyBlok++; else n.ciosyGraczy++;
      n.zdarzenie(ev.isCancelled() ? "cios-blok" : "cios", p.getName() + ">" + ofiara.id + (ev.isCancelled() ? "" : ":" + dmg));
    } else if (napastnik != null && ev.getEntity() instanceof Player p && !ev.isCancelled()) {
      napastnik.napad.zdarzenie("trafia-gracza", napastnik.id + ">" + p.getName() + ":" + dmg);
    } else if (napastnik != null && ofiara != null && !ev.isCancelled()) {
      double d = sprawca.getLocation().distance(ev.getEntity().getLocation());
      // strzala: piate pole = obrazenia przed pancerzem / atak luku (przyspieszenie z wysokosci, kalibracja strzala-z-muru)
      boolean strzala = ev.getDamager() instanceof Projectile;
      napastnik.napad.zdarzenie("traf", napastnik.id + ">" + ofiara.id + ":" + dmg + ":" + Napad.r1(d) + ":"
          + (strzala ? "strzala:" + Napad.r2(ev.getDamage() / Math.max(0.1, napastnik.zestaw.atak())) : "wrecz"));
    }
  }

  /**
   * Strzal ciala: strzala nietrwala (nie zostaje w chunku), obrazenia = atak luku MMOItems (jak w pelni naciagniety
   * luk gracza; vanilla mnozy baze strzaly przez predkosc, wiec baza = atak / predkosc) i wpis do kalibracji celnosci/tempa.
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void strzal(EntityShootBowEvent ev) {
    Jednostka j = jednostka(ev.getEntity());
    if (j == null) return;
    ev.getProjectile().setPersistent(false);
    if (ev.getProjectile() instanceof AbstractArrow a) {
      double v = a.getVelocity().length();
      if (v > 0.1) a.setDamage(j.zestaw.atak() / v);
      a.setCritical(false);   // krytyk dokladalby losowo do polowy obrazen
    }
    Jednostka c = j.cel;
    j.napad.zdarzenie("strzal", j.id + ">" + (c != null ? String.valueOf(c.id) : j.celGracz != null ? j.celGracz.getName() : "?"));
  }

  /**
   * Obrona (statystyka MythicLib z przedmiotow) dla ciala: MythicLib liczy ja tylko graczom (DamageReduction wymaga
   * MMOPlayerData), wiec cios gracza albo ciala w cialo dostaje ten sam wzor tutaj. Pancerz liczy vanilla po tym.
   */
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void obronaCiala(EntityDamageByEntityEvent ev) {
    Jednostka j = jednostka(ev.getEntity());
    if (j != null && j.zestaw.obrona() > 0) ev.setDamage(Wyposazenie.poObronie(ev.getDamage(), j.zestaw.obrona()));
  }

  @EventHandler
  public void smierc(EntityDeathEvent ev) {
    Jednostka j = jednostka(ev.getEntity());
    if (j == null) return;
    ev.getDrops().clear();
    ev.setDroppedExp(0);
    j.napad.padl(j);
  }

  @EventHandler
  public void strzala(ProjectileHitEvent ev) {
    if (ev.getEntity() instanceof AbstractArrow a && a.getShooter() instanceof Entity s && jednostka(s) != null) a.remove();
  }

  /** Smierc w napadzie: bez utraty rzeczy i poziomu (decyzja wlasciciela 25.10: "gracz jak pada to pada może obserwować"). */
  @EventHandler(priority = EventPriority.HIGHEST)
  public void smiercGracza(PlayerDeathEvent ev) {
    Player p = ev.getEntity();
    var d = pl.dolaczeni.get(p.getUniqueId());
    Napad n = pl.napadGracza(p);
    if (d == null || n == null) return;
    ev.setKeepInventory(true);
    ev.setKeepLevel(true);
    ev.getDrops().clear();
    ev.setDroppedExp(0);
    d.polegl = true;
    n.zdarzenie("gracz-polegl", p.getName());
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void odrodzenie(PlayerRespawnEvent ev) {
    Player p = ev.getPlayer();
    var stare = pl.doPrzywrocenia.remove(p.getUniqueId());
    if (stare != null) {   // napad skonczyl sie w czasie ekranu smierci
      if (stare.powrot != null && stare.powrot.getWorld() != null) ev.setRespawnLocation(stare.powrot);
      Bukkit.getScheduler().runTask(pl, () -> { if (p.isOnline()) p.setGameMode(stare.tryb); });
      return;
    }
    var d = pl.dolaczeni.get(p.getUniqueId());
    Napad n = pl.napadGracza(p);
    if (d == null || n == null || !d.polegl) return;
    ev.setRespawnLocation(n.punktWidza());
    Bukkit.getScheduler().runTask(pl, () -> {
      if (!p.isOnline() || pl.napadGracza(p) != n) return;
      p.setGameMode(GameMode.SPECTATOR);
      p.sendMessage("Polegles. Obserwujesz napad " + n.id + " do konca, potem wrocisz tam, skad przyszedles (/napad opusc - wczesniej).");
    });
  }

  /** Koniec startu serwera: jednorazowy koszt pierwszego ciala MM (Napady#rozgrzej). */
  @EventHandler
  public void start(ServerLoadEvent ev) {
    if (ev.getType() == ServerLoadEvent.LoadType.STARTUP) pl.rozgrzej();
  }

  /** Wyjscie z gry w trakcie napadu: przywroc tryb i miejsce, zeby nie zostac widzem w swiecie napadow. */
  @EventHandler
  public void wyjscie(PlayerQuitEvent ev) {
    Player p = ev.getPlayer();
    var d = pl.dolaczeni.remove(p.getUniqueId());
    if (d == null) d = pl.doPrzywrocenia.remove(p.getUniqueId());
    if (d != null && !p.isDead()) pl.przywroc(p, d, false);
  }
}
