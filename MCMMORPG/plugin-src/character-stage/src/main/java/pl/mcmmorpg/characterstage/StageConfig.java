package pl.mcmmorpg.characterstage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public record StageConfig(
    boolean enabled,
    boolean interceptJoin,
    boolean lockStageMovement,
    double focusRadius,
    MessageBundle messages,
    StageScene scene,
    Map<String, StageSlot> slots
) {
  static StageConfig load(JavaPlugin plugin) {
    FileConfiguration config = plugin.getConfig();
    MessageBundle messages = MessageBundle.load(config.getConfigurationSection("messages"));
    StageScene scene = StageScene.load(config.getConfigurationSection("stage"));
    Map<String, StageSlot> slots = new LinkedHashMap<>();
    ConfigurationSection slotsSection = config.getConfigurationSection("slots");
    if (slotsSection != null) {
      for (String key : slotsSection.getKeys(false)) {
        ConfigurationSection slotSection = slotsSection.getConfigurationSection(key);
        if (slotSection == null) {
          continue;
        }
        slots.put(key, StageSlot.load(key, slotSection));
      }
    }
    return new StageConfig(
        config.getBoolean("enabled", true),
        config.getBoolean("intercept_join", true),
        config.getBoolean("lock_stage_movement", true),
        config.getDouble("focus_radius", 3.0),
        messages,
        scene,
        slots
    );
  }

  public record MessageBundle(
      String stageTitle,
      String stageSubtitle,
      String focusedTitle,
      String focusedSubtitle,
      String emptyTitle,
      String emptySubtitle,
      String enterReady,
      String leaveStage,
      String blockedMovement
  ) {
    static MessageBundle load(ConfigurationSection section) {
      if (section == null) {
        return new MessageBundle("", "", "", "", "", "", "", "", "");
      }
      return new MessageBundle(
          section.getString("stage_title", ""),
          section.getString("stage_subtitle", ""),
          section.getString("focused_title", ""),
          section.getString("focused_subtitle", ""),
          section.getString("empty_title", ""),
          section.getString("empty_subtitle", ""),
          section.getString("enter_ready", ""),
          section.getString("leave_stage", ""),
          section.getString("blocked_movement", "")
      );
    }
  }

  public record StageScene(ScenePoint spawn, ScenePoint exit, BlockAnchor confirmAnchor, BlockAnchor backAnchor) {
    static StageScene load(ConfigurationSection section) {
      if (section == null) {
        throw new IllegalStateException("Missing stage config section");
      }
      return new StageScene(
          ScenePoint.load(section.getConfigurationSection("spawn")),
          ScenePoint.load(section.getConfigurationSection("exit")),
          BlockAnchor.load(section.getConfigurationSection("confirm_anchor")),
          BlockAnchor.load(section.getConfigurationSection("back_anchor"))
      );
    }
  }

  public record StageSlot(
      String key,
      String displayName,
      int profileIndex,
      boolean occupied,
      BlockAnchor anchor,
      ScenePoint focus,
      List<String> occupiedActions,
      List<String> createActions
  ) {
    static StageSlot load(String key, ConfigurationSection section) {
      return new StageSlot(
          key,
          section.getString("display_name", key),
          section.getInt("profile_index", 0),
          section.getBoolean("occupied", false),
          BlockAnchor.load(section.getConfigurationSection("anchor")),
          ScenePoint.load(section.getConfigurationSection("focus")),
          new ArrayList<>(section.getStringList("occupied_actions")),
          new ArrayList<>(section.getStringList("create_actions"))
      );
    }
  }

  public record ScenePoint(String world, double x, double y, double z, float yaw, float pitch) {
    static ScenePoint load(ConfigurationSection section) {
      if (section == null) {
        throw new IllegalStateException("Missing scene point config");
      }
      return new ScenePoint(
          section.getString("world", "world"),
          section.getDouble("x"),
          section.getDouble("y"),
          section.getDouble("z"),
          (float) section.getDouble("yaw"),
          (float) section.getDouble("pitch")
      );
    }
  }

  public record BlockAnchor(String world, int x, int y, int z) {
    static BlockAnchor load(ConfigurationSection section) {
      if (section == null) {
        throw new IllegalStateException("Missing block anchor config");
      }
      return new BlockAnchor(
          section.getString("world", "world"),
          section.getInt("x"),
          section.getInt("y"),
          section.getInt("z")
      );
    }

    boolean matches(Block block) {
      World worldRef = block.getWorld();
      return worldRef.getName().equals(world)
          && block.getX() == x
          && block.getY() == y
          && block.getZ() == z;
    }
  }
}
