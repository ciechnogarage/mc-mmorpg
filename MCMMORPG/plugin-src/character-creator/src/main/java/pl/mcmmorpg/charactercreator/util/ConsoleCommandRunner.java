package pl.mcmmorpg.charactercreator.util;

import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Dispatches console commands with %player% substituted, matching the syntax used
 * throughout the original brief (lp/mmocore/cmi all invoked this way).
 *
 * <p><b>Nazwa gracza jest walidowana przed podstawieniem.</b> Wczesniej szla do
 * {@code dispatchCommand} bez zadnego sprawdzenia, a ten serwer ma {@code online-mode=false}
 * (tak samo proxy: {@code velocity.toml -> online-mode = false}), wiec nick podaje klient.
 * Velocity z wlaczonym forwardingiem odsiewa wiekszosc smieci, ale opieranie bezpieczenstwa
 * komendy konsolowej na tym, ze ktos inny sprawdzil argument, jest zalozeniem, nie
 * zabezpieczeniem — zwlaszcza ze te komendy leca jako KONSOLA, czyli z pelnymi prawami.
 */
public final class ConsoleCommandRunner {

  /**
   * Dozwolony nick: znaki, ktore Mojang dopuszcza w nazwach kont. Wszystko poza nimi —
   * spacja, srednik, cudzyslow — pozwalaloby dokleic do komendy konsolowej wlasna tresc.
   */
  private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

  private ConsoleCommandRunner() {
  }

  /** Wydzielone, zeby dalo sie sprawdzic testem bez dzialajacego serwera. */
  static boolean isSafeName(String name) {
    return name != null && SAFE_NAME.matcher(name).matches();
  }

  public static void run(Plugin plugin, String template, Player player) {
    String name = player.getName();
    if (!isSafeName(name)) {
      Logger logger = plugin.getLogger();
      logger.severe("ODMOWA wykonania komendy konsolowej dla gracza o nazwie '" + name
          + "': nazwa zawiera znaki spoza [A-Za-z0-9_] albo jest za dluga. Szablon: "
          + template + ". Sprawdz online-mode i konfiguracje proxy.");
      return;
    }
    String command = template.replace("%player%", name);
    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
  }
}
