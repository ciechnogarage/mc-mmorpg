package pl.mcmmorpg.minioncore;

import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** HIGHEST/MONITOR vanilla melee-and-projectile policy guard for marked minions. */
public final class DamageGuard implements Listener {
  public static final double MINION_VS_PLAYER_DAMAGE_MULTIPLIER = 0.35d;
  private final MinionGroupRegistry registry;
  private final MinionSourceResolver sources;
  private final PvpContextAdapter pvp;
  private final PvpCompatibilityGate compatibility;
  private final CombatRelationResolver relations;

  public DamageGuard(MinionGroupRegistry registry, MinionSourceResolver sources,
      PvpContextAdapter pvp, PvpCompatibilityGate compatibility, CombatRelationResolver relations) {
    this.registry = registry;
    this.sources = sources;
    this.pvp = pvp;
    this.compatibility = compatibility;
    this.relations = relations;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void guard(EntityDamageByEntityEvent event) {
    Optional<MinionSource> source = sources.resolve(event.getDamager());
    Optional<MinionSource> victim = sources.resolve(event.getEntity());
    if (source.isEmpty()) {
      Player attacker = playerSource(event.getDamager());
      if (victim.isPresent() && attacker != null
          && attacker.getUniqueId().equals(victim.get().ownerId())) {
        event.setCancelled(true);
      }
      return;
    }
    MinionSource minion = source.get();
    if (mustCancelProvisional(minion)) {
      event.setCancelled(true);
      return;
    }
    Entity target = event.getEntity();
    CombatRelation relation = relations.relation(minion.ownerId(), target);
    if (relation == CombatRelation.SELF || relation == CombatRelation.OWNED_ALLY
        || relation == CombatRelation.PROTECTED) {
      deny(minion, event);
      return;
    }
    if (relation == CombatRelation.HOSTILE_PLAYER && target instanceof Player player) {
      if (!compatibility.playerTargetingEnabled() || player.getUniqueId().equals(minion.ownerId())
          || !allowedPlayerHit(minion, player)) {
        deny(minion, event);
        return;
      }
      if (!event.isCancelled()) {
        event.setDamage(event.getDamage() * MINION_VS_PLAYER_DAMAGE_MULTIPLIER);
      }
      return;
    }
    if (relation != CombatRelation.HOSTILE_MOB) {
      deny(minion, event);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
  public void observe(EntityDamageByEntityEvent event) {
    sources.resolve(event.getDamager()).ifPresent(source -> {
      if (event.isCancelled()) {
        registry.clearTarget(source.groupId());
      }
    });
  }

  private boolean allowedPlayerHit(MinionSource source, Player target) {
    Player owner = Bukkit.getPlayer(source.ownerId());
    return target.getGameMode() != GameMode.CREATIVE && target.getGameMode() != GameMode.SPECTATOR
        && owner != null && owner.isOnline() && pvp.evaluate(new PvpAttempt(
        source.ownerId(), owner.getLocation(), source.rootEntity(), target)) == PvpDecision.ALLOW;
  }

  private void deny(MinionSource source, EntityDamageByEntityEvent event) {
    event.setCancelled(true);
    registry.clearTarget(source.groupId());
  }

  static boolean mustCancelProvisional(MinionSource source) {
    return source.provisional();
  }

  private static Player playerSource(Entity damager) {
    if (damager instanceof Player player) {
      return player;
    }
    if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
      return player;
    }
    return null;
  }
}
