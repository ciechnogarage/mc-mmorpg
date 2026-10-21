package pl.mcmmorpg.minioncore;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/** Group-level role cooldowns; invoked only by the existing central scheduler. */
public final class RoleBehavior {
  private static final long RANGED_COOLDOWN_TICKS = 40L;
  private static final long SUPPORT_COOLDOWN_TICKS = 100L;
  /** Ranged minions do not shoot beyond the core's normal 25-block chase envelope. */
  static final double RANGED_MAX_ATTACK_DISTANCE_SQUARED = 25.0d * 25.0d;
  /** A target closer than four blocks triggers the deliberately small retreat. */
  static final double RANGED_RETREAT_DISTANCE_SQUARED = 4.0d * 4.0d;
  private final Map<UUID, Long> nextActionTick = new HashMap<>();

  public void service(MinionGroup group, long tick) {
    long cooldown = MythicMinionAdapter.RANGED_TIER_1.equals(group.typeId()) ? RANGED_COOLDOWN_TICKS
        : MythicMinionAdapter.SUPPORT_TIER_1.equals(group.typeId()) ? SUPPORT_COOLDOWN_TICKS : 0L;
    if (cooldown == 0L || tick < nextActionTick.getOrDefault(group.groupId(), 0L)) return;
    if (MythicMinionAdapter.RANGED_TIER_1.equals(group.typeId())) ranged(group);
    else support(group);
    nextActionTick.put(group.groupId(), tick + cooldown);
  }

  public void retainGroups(Set<UUID> activeGroups) {
    nextActionTick.keySet().retainAll(activeGroups);
  }

  private void ranged(MinionGroup group) {
    if (group.sharedTargetId() == null) return;
    Entity target = Bukkit.getEntity(group.sharedTargetId());
    if (!(target instanceof LivingEntity living) || target.isDead() || !target.isValid()) return;
    for (UUID id : group.entityIds()) {
      Entity entity = Bukkit.getEntity(id);
      if (!(entity instanceof LivingEntity minion) || minion.isDead() || !minion.isValid()) continue;
      if (!minion.getWorld().equals(living.getWorld())) continue;
      Vector away = minion.getLocation().toVector().subtract(living.getLocation().toVector());
      double distanceSquared = away.lengthSquared();
      if (!withinRangedAttackDistance(distanceSquared)) continue;
      if (shouldRetreat(distanceSquared)) minion.setVelocity(away.normalize().multiply(0.25d));
      minion.launchProjectile(Arrow.class, living.getEyeLocation().toVector().subtract(minion.getEyeLocation().toVector()).normalize().multiply(1.4d));
    }
  }

  private void support(MinionGroup group) {
    Player owner = Bukkit.getPlayer(group.ownerId());
    if (owner == null || !owner.isOnline()) return;
    PotionEffect buff = new PotionEffect(PotionEffectType.SPEED, 80, 0, true, false, true);
    owner.addPotionEffect(buff);
    for (UUID id : group.entityIds()) {
      Entity entity = Bukkit.getEntity(id);
      if (entity instanceof LivingEntity member && member.isValid() && !member.isDead()) member.addPotionEffect(buff);
    }
  }

  static boolean withinRangedAttackDistance(double distanceSquared) {
    return distanceSquared >= 0.0001d && distanceSquared <= RANGED_MAX_ATTACK_DISTANCE_SQUARED;
  }

  static boolean shouldRetreat(double distanceSquared) {
    return distanceSquared >= 0.0001d && distanceSquared < RANGED_RETREAT_DISTANCE_SQUARED;
  }
}
