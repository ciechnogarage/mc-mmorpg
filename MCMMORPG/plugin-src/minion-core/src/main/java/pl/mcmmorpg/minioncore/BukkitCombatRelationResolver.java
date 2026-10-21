package pl.mcmmorpg.minioncore;

import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

/** Conservative relation resolver; player legality is completed by the region context adapter. */
public final class BukkitCombatRelationResolver implements CombatRelationResolver {
  private final MinionGroupRegistry registry;

  public BukkitCombatRelationResolver(MinionGroupRegistry registry) {
    this.registry = registry;
  }

  @Override
  public CombatRelation relation(UUID ownerId, Entity candidate) {
    if (ownerId == null || candidate == null || candidate.isDead() || !candidate.isValid()) {
      return CombatRelation.PROTECTED;
    }
    if (ownerId.equals(candidate.getUniqueId())) {
      return CombatRelation.SELF;
    }
    // Every registered minion is a protected ally. Groups must never become
    // hostile to one another merely because their owners differ.
    if (registry.groupIdForEntity(candidate.getUniqueId()).isPresent()) {
      return CombatRelation.OWNED_ALLY;
    }
    if (candidate instanceof Monster) {
      return CombatRelation.HOSTILE_MOB;
    }
    if (candidate instanceof Player) {
      return CombatRelation.HOSTILE_PLAYER;
    }
    return CombatRelation.PROTECTED;
  }
}
