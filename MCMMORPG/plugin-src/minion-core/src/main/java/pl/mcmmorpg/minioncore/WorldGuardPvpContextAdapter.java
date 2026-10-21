package pl.mcmmorpg.minioncore;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/** WorldGuard 7.0.15 three-context PvP decision; every unavailable/error path denies. */
public final class WorldGuardPvpContextAdapter implements PvpContextAdapter {
  private final StateFlag minionPvpFlag;

  public WorldGuardPvpContextAdapter(StateFlag minionPvpFlag) {
    this.minionPvpFlag = Objects.requireNonNull(minionPvpFlag, "minionPvpFlag");
  }

  @Override
  public PvpDecision evaluate(PvpAttempt attempt) {
    try {
      if (attempt == null || attempt.ownerId() == null || attempt.ownerLocation() == null
          || attempt.minion() == null || attempt.target() == null) {
        return PvpDecision.DENY;
      }
      Player owner = Bukkit.getPlayer(attempt.ownerId());
      Entity minion = attempt.minion();
      Player target = attempt.target();
      if (owner == null || !owner.isOnline() || !minion.isValid() || minion.isDead()
          || !target.isOnline() || target.isDead() || !target.isValid()
          || owner.getWorld() != minion.getWorld() || owner.getWorld() != target.getWorld()
          || attempt.ownerLocation().getWorld() != owner.getWorld()) {
        return PvpDecision.DENY;
      }
      WorldGuardPlugin plugin = WorldGuardPlugin.inst();
      if (plugin == null) {
        return PvpDecision.DENY;
      }
      LocalPlayer ownerAssociable = plugin.wrapPlayer(owner);
      LocalPlayer targetAssociable = plugin.wrapPlayer(target);
      RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
      Location ownerLocation = BukkitAdapter.adapt(attempt.ownerLocation());
      Location minionLocation = BukkitAdapter.adapt(minion.getLocation());
      Location targetLocation = BukkitAdapter.adapt(target.getLocation());
      boolean ownerAllowed = query.testState(ownerLocation, ownerAssociable, Flags.PVP, minionPvpFlag);
      boolean minionAllowed = query.testState(minionLocation, ownerAssociable, Flags.PVP, minionPvpFlag);
      boolean targetAllowed = query.testState(targetLocation, targetAssociable, Flags.PVP, minionPvpFlag);
      return ownerAllowed && minionAllowed && targetAllowed ? PvpDecision.ALLOW : PvpDecision.DENY;
    } catch (RuntimeException ignored) {
      return PvpDecision.DENY;
    }
  }
}
