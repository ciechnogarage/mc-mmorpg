package pl.mcmmorpg.arena;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Diagnostyka PvP (tylko gdy sonda-pvp: true): na ktorym priorytecie cios gracz->gracz zostaje anulowany. */
final class Sonda implements Listener {
  private final JavaPlugin p;
  Sonda(JavaPlugin p) { this.p = p; }
  private boolean pvp(EntityDamageByEntityEvent e) { return e.getEntity() instanceof Player && (e.getDamager() instanceof Player || e.getDamager() instanceof org.bukkit.entity.Projectile pr && pr.getShooter() instanceof Player); }
  private void log(String etap, EntityDamageByEntityEvent e) {
    if (pvp(e)) p.getLogger().info("SONDA " + etap + " anul=" + e.isCancelled() + " dmg=" + e.getDamage() + " przyczyna=" + e.getCause() + " od=" + e.getDamager().getType());
  }
  @EventHandler(priority = EventPriority.MONITOR) public void pre(PrePlayerAttackEntityEvent e) {
    if (e.getAttacked() instanceof Player) p.getLogger().info("SONDA pre-attack willAttack=" + e.willAttack() + " anul=" + e.isCancelled());
  }
  private long ostatnio;
  @EventHandler(priority = EventPriority.LOWEST) public void a(EntityDamageByEntityEvent e) {
    log("LOWEST", e);
  }
  private void kto(EntityDamageByEntityEvent e) {
    if (!pvp(e) || !e.isCancelled() || System.currentTimeMillis() - ostatnio < 3000) return;
    ostatnio = System.currentTimeMillis();
    // kto anuluje: kazdy sluchacz (procz MONITOR i samej sondy) osobno na swiezej kopii zdarzenia
    for (org.bukkit.plugin.RegisteredListener rl : EntityDamageByEntityEvent.getHandlerList().getRegisteredListeners()) {
      if (rl.getListener() == this || rl.getPriority() == EventPriority.MONITOR) continue;
      @SuppressWarnings({"deprecation", "removal"})
      EntityDamageByEntityEvent kopia = new EntityDamageByEntityEvent(e.getDamager(), e.getEntity(), e.getCause(), e.getDamageSource(), e.getDamage());
      try { rl.callEvent(kopia); } catch (Throwable t) { p.getLogger().info("SONDA-KTO " + rl.getPlugin().getName() + " wyjatek " + t); continue; }
      if (kopia.isCancelled()) p.getLogger().info("SONDA-KTO ANULUJE: " + rl.getPlugin().getName() + " " + rl.getListener().getClass().getName() + " " + rl.getPriority());
    }
  }
  @EventHandler(priority = EventPriority.LOW) public void b(EntityDamageByEntityEvent e) { log("LOW", e); }
  @EventHandler(priority = EventPriority.NORMAL) public void c(EntityDamageByEntityEvent e) { log("NORMAL", e); }
  @EventHandler(priority = EventPriority.HIGH) public void d(EntityDamageByEntityEvent e) { log("HIGH", e); }
  @EventHandler(priority = EventPriority.HIGHEST) public void f(EntityDamageByEntityEvent e) { log("HIGHEST", e); }
  @EventHandler(priority = EventPriority.MONITOR) public void g(EntityDamageByEntityEvent e) { log("MONITOR", e); }  // kto() wylaczone: kopie zdarzen zostawiaja metadane ataku MythicLib na ofierze

  private void la(String etap, io.lumine.mythic.lib.api.event.PlayerAttackEvent e) {
    if (e.getEntity() instanceof Player) p.getLogger().info("SONDA-ATK " + etap + " anul=" + e.isCancelled() + " typy=" + e.getDamage().collectTypes() + " dmg=" + e.getDamage().getDamage());
  }
  @EventHandler(priority = EventPriority.LOWEST) public void la1(io.lumine.mythic.lib.api.event.PlayerAttackEvent e) { la("LOWEST", e); }
  @EventHandler(priority = EventPriority.LOW) public void la2(io.lumine.mythic.lib.api.event.PlayerAttackEvent e) { la("LOW", e); }
  @EventHandler(priority = EventPriority.NORMAL) public void la3(io.lumine.mythic.lib.api.event.PlayerAttackEvent e) { la("NORMAL", e); }
  @EventHandler(priority = EventPriority.HIGH) public void la4(io.lumine.mythic.lib.api.event.PlayerAttackEvent e) { la("HIGH", e); }
  @EventHandler(priority = EventPriority.HIGHEST) public void la5(io.lumine.mythic.lib.api.event.PlayerAttackEvent e) { la("HIGHEST", e); }
  @EventHandler(priority = EventPriority.MONITOR) public void la6(io.lumine.mythic.lib.api.event.PlayerAttackEvent e) { la("MONITOR", e); }
}
