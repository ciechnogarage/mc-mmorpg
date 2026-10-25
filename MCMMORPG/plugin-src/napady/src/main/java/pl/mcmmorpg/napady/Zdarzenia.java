package pl.mcmmorpg.napady;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Brak friendly fire, brak lupu z cial napadu, strzaly znikaja po trafieniu, cele cial wybiera tylko
 * napad; gracz, ktory padl w napadzie, nic nie traci i oglada do konca jako widz.
 */
final class Zdarzenia implements Listener {
  private final Napady pl;

  Zdarzenia(Napady pl) { this.pl = pl; }

  private Napad napadEncji(Entity e) {
    if (e == null) return null;
    for (Napad n : pl.aktywne.values()) {
      if (e instanceof Player p) { if (pl.napadGracza(p) == n) return n; }
      else for (Jednostka j : n.jednostki) if (j.cialo == e) return n;
    }
    return null;
  }

  private boolean cialo(Entity e) { return e != null && !(e instanceof Player) && napadEncji(e) != null; }

  /**
   * Cialo MM zmienia cel tylko na rozkaz napadu (Mob#setTarget = CUSTOM). Reszta (np. vanilla po ciosie,
   * zapomnienie celu) anulowana, zeby AI nie rozbijalo szyku i rozkladu celow.
   */
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void cel(EntityTargetEvent ev) {
    if (ev.getReason() == EntityTargetEvent.TargetReason.CUSTOM) return;
    if (ev.getReason() == EntityTargetEvent.TargetReason.TARGET_DIED) return;
    if (cialo(ev.getEntity())) ev.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void obrazenia(EntityDamageByEntityEvent ev) {
    Entity sprawca = ev.getDamager();
    if (sprawca instanceof Projectile pr && pr.getShooter() instanceof Entity s) sprawca = s;
    Napad nOfiara = napadEncji(ev.getEntity()), nSprawca = napadEncji(sprawca);
    if (nOfiara == null && nSprawca == null) return;
    // manekiny napadu bije tylko druga strona tego samego napadu
    if (nOfiara != nSprawca) { ev.setCancelled(true); return; }
    Strona a = nOfiara.stronaEncji(sprawca), b = nOfiara.stronaEncji(ev.getEntity());
    if (a == null || a == b) ev.setCancelled(true);
  }

  /**
   * Cios gracza z napadu w manekina drugiej strony przepuszczamy mimo ochrony terenu: WorldGuard
   * traktuje manekina jak zwierze (nie potwora) i w regionie bez czlonkostwa anuluje cios na NORMAL.
   * Dotyczy tylko manekinow napadu, nic poza nimi.
   */
  @EventHandler(priority = EventPriority.HIGHEST)
  public void przepuscCiosNapadu(EntityDamageByEntityEvent ev) {
    if (!ev.isCancelled() || !cialo(ev.getEntity())) return;
    Entity sprawca = ev.getDamager();
    if (sprawca instanceof Projectile pr && pr.getShooter() instanceof Entity s) sprawca = s;
    if (!(sprawca instanceof Player)) return;
    Napad n = napadEncji(ev.getEntity());
    if (n == null || napadEncji(sprawca) != n) return;
    Strona a = n.stronaEncji(sprawca), b = n.stronaEncji(ev.getEntity());
    if (a != null && b != null && a != b) ev.setCancelled(false);
  }

  /** Do zapisu: ciosy graczy w manekiny (po wszystkich pluginach) i ciosy manekinow w graczy. */
  @EventHandler(priority = EventPriority.MONITOR)
  public void ciosGracza(EntityDamageByEntityEvent ev) {
    Entity sprawca = ev.getDamager();
    if (sprawca instanceof Projectile pr && pr.getShooter() instanceof Entity s) sprawca = s;
    if (sprawca instanceof Player p && cialo(ev.getEntity())) {
      Napad n = napadEncji(ev.getEntity());
      if (n == null) return;
      if (ev.isCancelled()) n.ciosyGraczyBlok++; else n.ciosyGraczy++;
      n.zdarzenie(ev.isCancelled() ? "cios-blok" : "cios", p.getName() + ">" + n.idJednostki(ev.getEntity())
          + (ev.isCancelled() ? "" : ":" + Math.round(ev.getFinalDamage() * 10) / 10.0));
    } else if (cialo(sprawca) && ev.getEntity() instanceof Player p && !ev.isCancelled()) {
      Napad n = napadEncji(sprawca);
      if (n != null) n.zdarzenie("trafia-gracza", n.idJednostki(sprawca) + ">" + p.getName() + ":" + Math.round(ev.getFinalDamage() * 10) / 10.0);
    }
  }

  @EventHandler
  public void smierc(EntityDeathEvent ev) {
    if (ev.getEntity() instanceof Player) return;
    Napad n = napadEncji(ev.getEntity());
    if (n == null) return;
    ev.getDrops().clear();
    ev.setDroppedExp(0);
    for (Jednostka j : n.jednostki) if (j.cialo == ev.getEntity()) n.zdarzenie("padl", String.valueOf(j.id));
  }

  @EventHandler
  public void strzala(ProjectileHitEvent ev) {
    if (ev.getEntity() instanceof Arrow a && a.getShooter() instanceof Entity s && cialo(s)) a.remove();
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

  /** Wyjscie z gry w trakcie napadu: przywroc tryb i miejsce, zeby nie zostac widzem w swiecie napadow. */
  @EventHandler
  public void wyjscie(PlayerQuitEvent ev) {
    Player p = ev.getPlayer();
    var d = pl.dolaczeni.remove(p.getUniqueId());
    if (d == null) d = pl.doPrzywrocenia.remove(p.getUniqueId());
    if (d != null && !p.isDead()) pl.przywroc(p, d);
  }
}
