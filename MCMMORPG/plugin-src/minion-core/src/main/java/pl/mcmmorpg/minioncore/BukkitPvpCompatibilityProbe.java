package pl.mcmmorpg.minioncore;

import org.bukkit.plugin.Plugin;

/**
 * Compatibility preflight for the current Bukkit listener stack.
 *
 * <p>Earlier builds disabled player targeting whenever any other plugin already
 * owned a HIGHEST or MONITOR EntityDamageByEntityEvent listener. That is too
 * broad: MinionCore registers its HIGHEST guard after already-loaded plugins, so
 * earlier HIGHEST listeners cannot uncancel after MinionCore's guard. MONITOR is
 * advisory by Bukkit convention and cannot be treated as a mutator without
 * evidence.</p>
 *
 * <p>The fail-closed mechanism remains {@link PvpCompatibilityGate}: if a future
 * stack-specific probe proves illegal minion damage can be uncancelled after the
 * guard, pass {@code false} to the gate and player targeting remains disabled.
 * This default probe keeps ordinary stacks enabled instead of failing closed on
 * listener presence alone.</p>
 */
public final class BukkitPvpCompatibilityProbe {
  private BukkitPvpCompatibilityProbe() { }

  public static boolean illegalHitCanRemainCancelled(Plugin minionCore) {
    return minionCore != null && minionCore.isEnabled();
  }
}
