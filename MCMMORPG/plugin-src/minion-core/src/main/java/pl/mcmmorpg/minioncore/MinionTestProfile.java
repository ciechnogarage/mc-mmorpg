package pl.mcmmorpg.minioncore;

import java.io.File;
import java.time.Duration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Optional staging-only profile. Production defaults remain 3 per owner/30 global/60s. */
public record MinionTestProfile(boolean enabled, int globalLimit, int minSummonAmount,
    int maxSummonAmount, Duration duration) {
  public static MinionTestProfile load(JavaPlugin plugin) {
    File file = new File(plugin.getDataFolder(), "test-profile.yml");
    if (!file.isFile()) return defaults();
    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
    if (!config.getBoolean("enabled", false)) return defaults();
    int global = bounded(config.getInt("global-limit", 30), 30, 200);
    int minimumAmount = bounded(config.getInt("min-summon-amount", 3), 1, 3);
    int amount = bounded(config.getInt("max-summon-amount", 3), minimumAmount, 3);
    long seconds = Math.max(1L, Math.min(config.getLong("duration-seconds", 60L), 900L));
    return new MinionTestProfile(true, global, minimumAmount, amount, Duration.ofSeconds(seconds));
  }

  public static MinionTestProfile defaults() {
    return new MinionTestProfile(false, 30, 3, 3, Duration.ofSeconds(60));
  }

  private static int bounded(int value, int minimum, int maximum) {
    return Math.max(minimum, Math.min(maximum, value));
  }
}
