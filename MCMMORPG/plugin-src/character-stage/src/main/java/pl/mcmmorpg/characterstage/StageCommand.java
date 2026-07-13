package pl.mcmmorpg.characterstage;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class StageCommand implements CommandExecutor, TabCompleter {
  private final CharacterStagePlugin plugin;

  StageCommand(CharacterStagePlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) {
      sender.sendMessage("/characterstage <reload|stage|leave>");
      return true;
    }
    return switch (args[0].toLowerCase()) {
      case "reload" -> {
        plugin.reloadStageService();
        sender.sendMessage("CharacterStage reloaded.");
        yield true;
      }
      case "stage" -> {
        if (sender instanceof Player player) {
          plugin.stageService().stage(player);
          yield true;
        }
        sender.sendMessage("Player only.");
        yield true;
      }
      case "leave" -> {
        if (sender instanceof Player player) {
          plugin.stageService().leaveStage(player);
          yield true;
        }
        sender.sendMessage("Player only.");
        yield true;
      }
      default -> false;
    };
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      return List.of("reload", "stage", "leave");
    }
    return List.of();
  }
}
