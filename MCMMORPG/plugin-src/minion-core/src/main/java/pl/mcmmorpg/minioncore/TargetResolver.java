package pl.mcmmorpg.minioncore;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;

/**
 * Target authority for the melee slice. Monsters are always local candidates;
 * player candidates additionally require the injected region policy.
 */
public final class TargetResolver implements Listener, GroupMaintenance {
  private static final double SCAN_RADIUS = 16.0d;
  private final MinionGroupRegistry registry;
  private final PlayerTargetingPolicy playerTargets;
  private final RoleBehavior roles = new RoleBehavior();

  public TargetResolver(MinionGroupRegistry registry) {
    this(registry, (group, candidate) -> false);
  }

  public TargetResolver(MinionGroupRegistry registry, PlayerTargetingPolicy playerTargets) {
    this.registry = registry;
    this.playerTargets = playerTargets;
  }

  public boolean assignOwnerTarget(UUID ownerId, Entity candidate) {
    List<MinionGroup> groups = registry.groupsForOwner(ownerId);
    if (groups.isEmpty()) {
      return false;
    }
    boolean assigned = false;
    for (MinionGroup group : groups) {
      if (isEligibleHostileMonster(candidate) || playerTargets.canTarget(group, candidate)) {
        registry.setTarget(group.groupId(), candidate.getUniqueId());
        assigned = true;
      }
    }
    return assigned;
  }

  @Override
  public void validateTarget(MinionGroup group) {
    UUID targetId = group.sharedTargetId();
    Entity target = targetId == null ? null : Bukkit.getEntity(targetId);
    if (targetId != null) {
      if (!isEligibleHostileMonster(target) && !playerTargets.canTarget(group, target)) {
        registry.clearTarget(group.groupId());
      } else {
        registry.setTarget(group.groupId(), targetId);
      }
    }
  }

  @Override
  public void serviceOwnerDistance(MinionGroup group) {
    Player owner = Bukkit.getPlayer(group.ownerId());
    if (owner == null || !owner.isOnline()) {
      registry.dismissAll(group.ownerId());
    }
  }

  @Override
  public void scanForTarget(MinionGroup group, int candidateLimit) {
    if (group.sharedTargetId() != null || candidateLimit <= 0) {
      return;
    }
    Player owner = Bukkit.getPlayer(group.ownerId());
    if (owner == null || !owner.isOnline()) {
      return;
    }
    Collection<Entity> nearby = owner.getWorld().getNearbyEntities(
        owner.getLocation(), SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS, this::isEligibleHostileMonster);
    Entity selected = nearby.stream()
        .limit(candidateLimit)
        .min(Comparator.comparing(Entity::getUniqueId))
        .orElse(null);
    if (selected != null) {
      registry.setTarget(group.groupId(), selected.getUniqueId());
    }
  }

  @Override
  public void serviceRole(MinionGroup group, long currentTick) {
    roles.service(group, currentTick);
  }

  @Override
  public void reconcileGroups(List<MinionGroup> groups) {
    roles.retainGroups(groups.stream().map(MinionGroup::groupId).collect(java.util.stream.Collectors.toSet()));
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void constrainMinionTarget(EntityTargetEvent event) {
    Optional<UUID> groupId = registry.groupIdForEntity(event.getEntity().getUniqueId());
    if (groupId.isEmpty()) {
      return;
    }
    MinionGroup group = registry.group(groupId.get()).orElse(null);
    if (group == null || event.isCancelled()) {
      registry.clearTarget(groupId.get());
      return;
    }
    Entity target = event.getTarget();
    TargetEventDecision decision =
        targetEventDecision(
            group.sharedTargetId(),
            target == null ? null : target.getUniqueId(),
            target != null && (isEligibleHostileMonster(target) || playerTargets.canTarget(group, target)));
    if (decision.cancelEvent()) {
      event.setCancelled(true);
    }
    if (decision.clearGroupTarget()) {
      registry.clearTarget(groupId.get());
    }
  }

  static TargetEventDecision targetEventDecision(UUID sharedTargetId, UUID eventTargetId, boolean eventTargetLegal) {
    if (sharedTargetId == null) {
      return TargetEventDecision.cancelOnly("no-shared-target");
    }
    if (eventTargetId == null) {
      return TargetEventDecision.cancelAndClear("null-target");
    }
    if (!sharedTargetId.equals(eventTargetId)) {
      return TargetEventDecision.cancelOnly("different-target");
    }
    if (!eventTargetLegal) {
      return TargetEventDecision.cancelAndClear("policy-deny");
    }
    return TargetEventDecision.allow();
  }

  @EventHandler(ignoreCancelled = true)
  public void resolveOwnerCombat(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player owner) {
      assignOwnerTarget(owner.getUniqueId(), event.getEntity());
    } else if (event.getEntity() instanceof Player owner) {
      assignOwnerTarget(owner.getUniqueId(), event.getDamager());
    }
  }

  public static boolean isEligibleMonster(Entity candidate) {
    return candidate instanceof Monster && candidate.isValid() && !candidate.isDead();
  }

  private boolean isEligibleHostileMonster(Entity candidate) {
    return isEligibleMonster(candidate) && registry.groupIdForEntity(candidate.getUniqueId()).isEmpty();
  }

  record TargetEventDecision(boolean cancelEvent, boolean clearGroupTarget, String reason) {
    static TargetEventDecision allow() {
      return new TargetEventDecision(false, false, "allow");
    }

    static TargetEventDecision cancelOnly(String reason) {
      return new TargetEventDecision(true, false, reason);
    }

    static TargetEventDecision cancelAndClear(String reason) {
      return new TargetEventDecision(true, true, reason);
    }
  }
}
