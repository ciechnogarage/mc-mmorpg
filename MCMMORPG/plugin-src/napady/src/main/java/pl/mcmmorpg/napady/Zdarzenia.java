package pl.mcmmorpg.napady;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Brak friendly fire, brak lupu z manekinow, strzaly znikaja po trafieniu. */
final class Zdarzenia implements Listener {
  private final Napady pl;

  Zdarzenia(Napady pl) { this.pl = pl; }

  private Napad napadEncji(Entity e) {
    for (Napad n : pl.aktywne.values()) {
      if (e instanceof Player p) { if (pl.napadGracza(p) == n) return n; }
      else for (Jednostka j : n.jednostki) if (j.cialo == e) return n;
    }
    return null;
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
    if (!ev.isCancelled() || !(ev.getEntity() instanceof Mannequin)) return;
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
    if (sprawca instanceof Player p && ev.getEntity() instanceof Mannequin) {
      Napad n = napadEncji(ev.getEntity());
      if (n == null) return;
      if (ev.isCancelled()) n.ciosyGraczyBlok++; else n.ciosyGraczy++;
      n.zdarzenie(ev.isCancelled() ? "cios-blok" : "cios", p.getName() + ">" + n.idJednostki(ev.getEntity())
          + (ev.isCancelled() ? "" : ":" + Math.round(ev.getFinalDamage() * 10) / 10.0));
    } else if (sprawca instanceof Mannequin && ev.getEntity() instanceof Player p && !ev.isCancelled()) {
      Napad n = napadEncji(sprawca);
      if (n != null) n.zdarzenie("trafia-gracza", n.idJednostki(sprawca) + ">" + p.getName() + ":" + Math.round(ev.getFinalDamage() * 10) / 10.0);
    }
  }

  @EventHandler
  public void smierc(EntityDeathEvent ev) {
    if (!(ev.getEntity() instanceof Mannequin)) return;
    Napad n = napadEncji(ev.getEntity());
    if (n == null) return;
    ev.getDrops().clear();
    ev.setDroppedExp(0);
    for (Jednostka j : n.jednostki) if (j.cialo == ev.getEntity()) n.zdarzenie("padl", String.valueOf(j.id));
  }

  @EventHandler
  public void strzala(ProjectileHitEvent ev) {
    if (ev.getEntity() instanceof Arrow a && a.getShooter() instanceof Mannequin) a.remove();
  }

  @EventHandler
  public void wyjscie(PlayerQuitEvent ev) { pl.dolaczeni.remove(ev.getPlayer().getUniqueId()); }
}
