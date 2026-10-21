package pl.mcmmorpg.minioncore;

import java.time.Duration;
import java.util.Objects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/** Technical T4 controls for the current caller's active melee groups. */
public final class MinionCommand implements CommandExecutor {
  private final MinionGroupRegistry minions;
  private final TargetResolver targets;
  private final int minSummonAmount;
  private final int maxSummonAmount;
  private final Duration summonDuration;

  public MinionCommand(MinionGroupRegistry minions, TargetResolver targets) {
    this(minions, targets, 3, 3, Duration.ofSeconds(60));
  }

  public MinionCommand(MinionGroupRegistry minions, TargetResolver targets, int maxSummonAmount,
      Duration summonDuration) {
    this(minions, targets, 3, maxSummonAmount, summonDuration);
  }

  public MinionCommand(MinionGroupRegistry minions, TargetResolver targets, int minSummonAmount,
      int maxSummonAmount, Duration summonDuration) {
    this.minions = Objects.requireNonNull(minions, "minions");
    this.targets = Objects.requireNonNull(targets, "targets");
    if (minSummonAmount < 1 || maxSummonAmount < minSummonAmount
        || summonDuration.isZero() || summonDuration.isNegative()) {
      throw new IllegalArgumentException("invalid summon profile");
    }
    this.minSummonAmount = minSummonAmount;
    this.maxSummonAmount = maxSummonAmount;
    this.summonDuration = summonDuration;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Only an in-game player can summon a minion.");
      return true;
    }
    if (arguments.length == 1) {
      switch (arguments[0].toLowerCase(java.util.Locale.ROOT)) {
        case "follow" -> {
          for (MinionGroup group : minions.groupsForOwner(player.getUniqueId())) {
            minions.clearTarget(group.groupId());
          }
          sender.sendMessage("Minions are in FOLLOW mode; their shared targets were cleared.");
          return true;
        }
        case "hold" -> {
          for (MinionGroup group : minions.groupsForOwner(player.getUniqueId())) {
            minions.holdGroup(group.groupId());
          }
          sender.sendMessage("Minions are in HOLD mode; their shared targets were cleared.");
          return true;
        }
        case "dismiss" -> {
          minions.dismissAll(player.getUniqueId());
          sender.sendMessage("Dismissed your minions.");
          return true;
        }
        case "attack" -> {
          Entity target = player.getTargetEntity(20);
          if (!targets.assignOwnerTarget(player.getUniqueId(), target)) {
            sender.sendMessage("Look directly at a loaded hostile monster within 20 blocks.");
          } else {
            sender.sendMessage("Assigned your minions a shared target.");
          }
          return true;
        }
        default -> { }
      }
    }
    if (arguments.length == 2 && "attack".equalsIgnoreCase(arguments[0])) {
      Player target = org.bukkit.Bukkit.getPlayerExact(arguments[1]);
      boolean assigned = targets.assignOwnerTarget(player.getUniqueId(), target);
      org.bukkit.Bukkit.getLogger().info("[MinionCore] player-target-command owner="
          + player.getName() + " target=" + arguments[1] + " assigned=" + assigned);
      if (!assigned) {
        sender.sendMessage("Cannot assign minions to that player target.");
      } else {
        sender.sendMessage("Assigned minions shared player target.");
      }
      return true;
    }
    if (arguments.length != 3 || !"summon".equalsIgnoreCase(arguments[0])) {
      sender.sendMessage("Usage: /minion summon <melee|ranged|support> <amount> | /minion follow|hold|attack|dismiss");
      return true;
    }
    int amount;
    try {
      amount = Integer.parseInt(arguments[2]);
    } catch (NumberFormatException invalidAmount) {
      amount = -1;
    }
    if (amount < minSummonAmount || amount > maxSummonAmount) {
      sender.sendMessage("Usage: /minion summon <melee|ranged|support> " + minSummonAmount + "-" + maxSummonAmount);
      return true;
    }
    String typeId = switch (arguments[1].toLowerCase(java.util.Locale.ROOT)) {
      case "melee" -> MythicMinionAdapter.MELEE_TIER_1;
      case "ranged" -> MythicMinionAdapter.RANGED_TIER_1;
      case "support" -> MythicMinionAdapter.SUPPORT_TIER_1;
      default -> null;
    };
    if (typeId == null) {
      sender.sendMessage("Usage: /minion summon <melee|ranged|support> " + minSummonAmount + "-" + maxSummonAmount);
      return true;
    }
    SummonResult result = minions.summon(player.getUniqueId(), typeId, amount, summonDuration);
    if (result.success()) {
      sender.sendMessage("Summoned " + amount + " " + arguments[1].toLowerCase(java.util.Locale.ROOT)
          + " minions for " + summonDuration.toSeconds() + " seconds.");
    } else {
      sender.sendMessage("Could not summon melee minions: " + result.failure().name().toLowerCase());
    }
    return true;
  }
}
