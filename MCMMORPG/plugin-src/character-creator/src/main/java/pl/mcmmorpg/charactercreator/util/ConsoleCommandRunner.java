package pl.mcmmorpg.charactercreator.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Dispatches console commands with %player% substituted, matching the syntax used
 * throughout the original brief (lp/mmocore/cmi all invoked this way).
 */
public final class ConsoleCommandRunner {

  private ConsoleCommandRunner() {
  }

  public static void run(Plugin plugin, String template, Player player) {
    String command = template.replace("%player%", player.getName());
    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
  }
}
