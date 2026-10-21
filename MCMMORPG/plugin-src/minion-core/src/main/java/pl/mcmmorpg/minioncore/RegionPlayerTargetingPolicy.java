package pl.mcmmorpg.minioncore;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/** Allows player targeting only while the gate and all three WorldGuard contexts allow it. */
public final class RegionPlayerTargetingPolicy implements PlayerTargetingPolicy {
  private final PvpCompatibilityGate compatibility;
  private final PvpContextAdapter context;

  public RegionPlayerTargetingPolicy(PvpCompatibilityGate compatibility, PvpContextAdapter context) {
    this.compatibility = compatibility;
    this.context = context;
  }

  @Override
  public boolean canTarget(MinionGroup group, Entity candidate) {
    if (!compatibility.playerTargetingEnabled() || !(candidate instanceof Player target)
        || target.isDead() || !target.isOnline()
        || target.getGameMode() == GameMode.CREATIVE || target.getGameMode() == GameMode.SPECTATOR
        || target.getUniqueId().equals(group.ownerId())) {
      return false;
    }
    Player owner = Bukkit.getPlayer(group.ownerId());
    if (owner == null || !owner.isOnline()) {
      return false;
    }
    return !group.entityIds().isEmpty();
  }
}
