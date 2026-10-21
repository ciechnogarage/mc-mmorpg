package pl.mcmmorpg.minioncore;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import java.time.Clock;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bootstrap only: registers the WorldGuard capability required before minion
 * gameplay can exist. T3 adds only the technical MythicMobs melee summon path;
 * targeting, combat policy, and lifecycle scheduling remain later tickets.
 */
public final class MinionCorePlugin extends JavaPlugin implements Listener {
  static final String MINION_PVP_FLAG_NAME = "minion-pvp";

  private StateFlag minionPvpFlag;
  private MinionGroupRegistry minions;
  private BukkitTask lifecycleTask;
  private long lifecycleTick;
  private final PaperPerformanceTelemetry paperPerformance = new PaperPerformanceTelemetry();

  @Override
  public void onLoad() {
    try {
      this.minionPvpFlag = registerMinionPvpFlag(WorldGuard.getInstance().getFlagRegistry());
    } catch (RuntimeException exception) {
      throw new IllegalStateException(
          "MinionCore cannot start: WorldGuard flag '" + MINION_PVP_FLAG_NAME + "' was not registered during onLoad.",
          exception);
    }
  }

  @Override
  public void onEnable() {
    if (minionPvpFlag == null) {
      throw new IllegalStateException(
          "MinionCore cannot enable without the WorldGuard '" + MINION_PVP_FLAG_NAME + "' StateFlag.");
    }
    getLogger().info("WorldGuard capability '" + MINION_PVP_FLAG_NAME + "' is registered (default: deny).");
    Plugin mythicMobs = getServer().getPluginManager().getPlugin("MythicMobs");
    if (mythicMobs == null || !mythicMobs.isEnabled()) {
      getLogger().warning("MythicMobs is unavailable; MinionCore summon commands are disabled.");
      registerUnavailableCommand();
      return;
    }

    MinionTestProfile testProfile = MinionTestProfile.load(this);
    if (testProfile.enabled()) {
      getLogger().warning("STAGING TEST PROFILE enabled: global-limit=" + testProfile.globalLimit()
          + ", summon-amount=" + testProfile.minSummonAmount() + "-" + testProfile.maxSummonAmount()
          + ", duration=" + testProfile.duration().toSeconds() + "s");
    }
    Clock lifecycleClock = Clock.systemUTC();
    MythicMinionAdapter adapter = new MythicMinionAdapter(this);
    minions = new MinionGroupRegistry(
        adapter,
        () -> {
          if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("MinionCore lifecycle must run on the Paper main thread");
          }
        },
        lifecycleClock,
        3,
        testProfile.globalLimit());
    PvpCompatibilityGate pvpCompatibility = new PvpCompatibilityGate();
    boolean playerTargeting = pvpCompatibility.verify(() -> BukkitPvpCompatibilityProbe.illegalHitCanRemainCancelled(this));
    WorldGuardPvpContextAdapter pvpContext = new WorldGuardPvpContextAdapter(minionPvpFlag());
    TargetResolver targets =
        new TargetResolver(minions, new RegionPlayerTargetingPolicy(pvpCompatibility, pvpContext));
    DamageGuard damageGuard = new DamageGuard(
        minions,
        new MinionSourceResolver(minions, this),
        pvpContext,
        pvpCompatibility,
        new BukkitCombatRelationResolver(minions));
    GroupLifecycleScheduler scheduler = new GroupLifecycleScheduler(minions, targets);
    getCommand("minion").setExecutor(new MinionCommand(minions, targets,
        testProfile.minSummonAmount(), testProfile.maxSummonAmount(), testProfile.duration()));
    getServer().getPluginManager().registerEvents(this, this);
    getServer().getPluginManager().registerEvents(targets, this);
    getServer().getPluginManager().registerEvents(damageGuard, this);
    lifecycleTask = getServer().getScheduler().runTaskTimer(
        this,
        () -> {
          paperPerformance.sample(System.nanoTime());
          scheduler.tick(++lifecycleTick, lifecycleClock.instant());
          if (lifecycleTick % 1200L == 0L) {
            getLogger().info("Paper performance " + paperPerformance.snapshot()
                + " tps=" + java.util.Arrays.toString(Bukkit.getServer().getTPS())
                + " paperAverageTickMs=" + Bukkit.getServer().getAverageTickTime());
            getLogger().info("Scheduler telemetry " + scheduler.telemetry().serviceTimings());
          }
        },
        1L,
        1L);
    getLogger().info("MythicMobs melee minion control is enabled with one central lifecycle scheduler; player targeting="
        + playerTargeting + ".");
  }

  @Override
  public void onDisable() {
    if (lifecycleTask != null) {
      lifecycleTask.cancel();
      lifecycleTask = null;
    }
    if (minions != null) {
      minions.disable();
      minions = null;
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    if (minions != null) {
      minions.onOwnerQuit(event.getPlayer().getUniqueId());
    }
  }

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    if (minions != null) {
      minions.onOwnerDeath(event.getEntity().getUniqueId());
    }
  }

  @EventHandler
  public void onEntityDeath(EntityDeathEvent event) {
    if (minions != null) {
      minions.onEntityRemoved(event.getEntity().getUniqueId());
    }
  }

  private void registerUnavailableCommand() {
    getCommand("minion").setExecutor((CommandSender sender, org.bukkit.command.Command command, String label, String[] arguments) -> {
      sender.sendMessage("Minion summoning is unavailable because MythicMobs is not enabled.");
      return true;
    });
  }

  StateFlag minionPvpFlag() {
    if (minionPvpFlag == null) {
      throw new IllegalStateException("WorldGuard '" + MINION_PVP_FLAG_NAME + "' StateFlag is unavailable.");
    }
    return minionPvpFlag;
  }

  static StateFlag registerMinionPvpFlag(FlagRegistry registry) {
    try {
      registry.register(new StateFlag(MINION_PVP_FLAG_NAME, false));
    } catch (FlagConflictException conflict) {
      return existingStateFlag(registry, conflict);
    }

    return existingStateFlag(registry, null);
  }

  private static StateFlag existingStateFlag(FlagRegistry registry, FlagConflictException conflict) {
    Flag<?> existing = registry.get(MINION_PVP_FLAG_NAME);
    if (existing instanceof StateFlag stateFlag) {
      return stateFlag;
    }

    String detail = existing == null
        ? "WorldGuard returned no registered flag"
        : "WorldGuard registered incompatible type " + existing.getClass().getName();
    if (conflict != null) {
      throw new IllegalStateException(
          "WorldGuard rejected '" + MINION_PVP_FLAG_NAME + "' as a conflict, but " + detail + ".",
          conflict);
    }
    throw new IllegalStateException(
        "WorldGuard did not retain the '" + MINION_PVP_FLAG_NAME + "' StateFlag: " + detail + ".");
  }
}
