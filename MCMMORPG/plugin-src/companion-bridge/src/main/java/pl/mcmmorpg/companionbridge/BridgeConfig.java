package pl.mcmmorpg.companionbridge;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class BridgeConfig {
  public final String bind;
  public final int port;
  public final String token;

  private BridgeConfig(String bind, int port, String token) {
    this.bind = bind;
    this.port = port;
    this.token = token;
  }

  public static BridgeConfig load(JavaPlugin plugin) {
    FileConfiguration cfg = plugin.getConfig();
    String bind = cfg.getString("bind", "0.0.0.0");
    int port = cfg.getInt("port", 25580);
    String token = cfg.getString("token", "CHANGE_ME");
    if ("CHANGE_ME".equals(token)) {
      plugin.getLogger().warning(
          "CompanionBridge: token w config.yml nie zostal zmieniony z domyslnego "
              + "CHANGE_ME — ustaw realny token przed wystawieniem portu poza localhost.");
    }
    return new BridgeConfig(bind, port, token);
  }
}
