package pl.mcmmorpg.minioncore;

import java.util.UUID;
import org.bukkit.entity.Entity;

public interface CombatRelationResolver {
  CombatRelation relation(UUID ownerId, Entity candidate);
}
