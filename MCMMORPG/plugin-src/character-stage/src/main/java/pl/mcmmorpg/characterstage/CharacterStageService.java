package pl.mcmmorpg.characterstage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class CharacterStageService {
  private final JavaPlugin plugin;
  private final StageConfig config;
  private final ProfileBackendAdapter profileBackendAdapter;
  private final Map<UUID, String> focusedSlots = new HashMap<>();
  private final Map<UUID, Boolean> stagedPlayers = new HashMap<>();
  private final MiniMessage miniMessage = MiniMessage.miniMessage();

  CharacterStageService(JavaPlugin plugin, StageConfig config, ProfileBackendAdapter profileBackendAdapter) {
    this.plugin = plugin;
    this.config = config;
    this.profileBackendAdapter = profileBackendAdapter;
  }

  public StageConfig config() {
    return config;
  }

  public boolean isStaged(Player player) {
    return stagedPlayers.getOrDefault(player.getUniqueId(), false);
  }

  public void stage(Player player) {
    if (!config.enabled()) {
      return;
    }
    stagedPlayers.put(player.getUniqueId(), true);
    focusedSlots.remove(player.getUniqueId());
    player.teleport(location(config.scene().spawn()));
    player.setGameMode(GameMode.ADVENTURE);
    player.setInvulnerable(true);
    sendStageTitle(player);
  }

  public void leaveStage(Player player) {
    stagedPlayers.remove(player.getUniqueId());
    focusedSlots.remove(player.getUniqueId());
    player.setInvulnerable(false);
    player.teleport(location(config.scene().exit()));
    player.sendMessage(text(config.messages().leaveStage(), null));
  }

  public void clearSession(Player player) {
    stagedPlayers.remove(player.getUniqueId());
    focusedSlots.remove(player.getUniqueId());
  }

  public boolean tryHandleAnchor(Player player, org.bukkit.block.Block block) {
    if (!isStaged(player)) {
      return false;
    }
    if (config.scene().confirmAnchor().matches(block)) {
      confirmFocusedSlot(player);
      return true;
    }
    if (config.scene().backAnchor().matches(block)) {
      focusedSlots.remove(player.getUniqueId());
      sendStageTitle(player);
      player.teleport(location(config.scene().spawn()));
      return true;
    }
    for (StageConfig.StageSlot slot : config.slots().values()) {
      if (slot.anchor().matches(block)) {
        focusSlot(player, resolveSlot(player, slot));
        return true;
      }
    }
    return false;
  }

  public void blockStageEscape(Player player, Location to) {
    if (!config.lockStageMovement() || !isStaged(player)) {
      return;
    }
    Location spawn = location(config.scene().spawn());
    if (!spawn.getWorld().equals(to.getWorld())) {
      player.teleport(spawn);
      player.sendMessage(text(config.messages().blockedMovement(), null));
      return;
    }
    if (spawn.distanceSquared(to) > (config.focusRadius() * config.focusRadius() * 36.0D)) {
      player.teleport(spawn);
      player.sendMessage(text(config.messages().blockedMovement(), null));
    }
  }

  private void focusSlot(Player player, ResolvedSlot slot) {
    String current = focusedSlots.get(player.getUniqueId());
    if (slot.key().equals(current)) {
      runActions(player, slot);
      return;
    }
    focusedSlots.put(player.getUniqueId(), slot.key());
    player.teleport(location(slot.focus()));
    if (slot.occupied()) {
      sendTitle(player, config.messages().focusedTitle(), config.messages().focusedSubtitle(), slot);
    } else {
      sendTitle(player, config.messages().emptyTitle(), config.messages().emptySubtitle(), slot);
    }
  }

  private void confirmFocusedSlot(Player player) {
    String key = focusedSlots.get(player.getUniqueId());
    if (key == null) {
      sendStageTitle(player);
      return;
    }
    StageConfig.StageSlot slot = config.slots().get(key);
    if (slot == null) {
      return;
    }
    runActions(player, resolveSlot(player, slot));
  }

  private void runActions(Player player, ResolvedSlot slot) {
    player.sendMessage(text(config.messages().enterReady(), slot));
    List<String> actions = slot.occupied() ? slot.template().occupiedActions() : slot.template().createActions();
    for (String action : actions) {
      if (action.startsWith("console:")) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), inject(action.substring("console:".length()), player, slot));
      } else if (action.startsWith("player:")) {
        player.performCommand(inject(action.substring("player:".length()), player, slot));
      }
    }
  }

  private Location location(StageConfig.ScenePoint point) {
    World world = Bukkit.getWorld(point.world());
    if (world == null) {
      throw new IllegalStateException("Unknown world for CharacterStage: " + point.world());
    }
    return new Location(world, point.x(), point.y(), point.z(), point.yaw(), point.pitch());
  }

  private void sendStageTitle(Player player) {
    sendTitle(player, config.messages().stageTitle(), config.messages().stageSubtitle(), null);
  }

  private void sendTitle(Player player, String title, String subtitle, ResolvedSlot slot) {
    player.showTitle(Title.title(text(title, slot), text(subtitle, slot)));
  }

  private Component text(String template, ResolvedSlot slot) {
    return miniMessage.deserialize(inject(template, null, slot));
  }

  private String inject(String template, Player player, ResolvedSlot slot) {
    String value = template;
    if (player != null) {
      value = value.replace("%player%", player.getName());
    }
    if (slot != null) {
      value = value.replace("%slot_name%", slot.displayName());
      value = value.replace("%slot_key%", slot.key());
      value = value.replace("%profile_index%", Integer.toString(slot.profileIndex()));
      value = value.replace("%profile_name%", slot.profileName());
    }
    return value;
  }

  private ResolvedSlot resolveSlot(Player player, StageConfig.StageSlot slot) {
    return profileBackendAdapter.getSlot(player, slot.profileIndex())
        .map(data -> new ResolvedSlot(
            slot,
            slot.key(),
            data.occupied() ? data.profileName() : slot.displayName(),
            slot.profileIndex(),
            data.profileName(),
            slot.focus(),
            data.occupied()
        ))
        .orElseGet(() -> new ResolvedSlot(
            slot,
            slot.key(),
            slot.displayName(),
            slot.profileIndex(),
            slot.displayName(),
            slot.focus(),
            slot.occupied()
        ));
  }

  record ResolvedSlot(
      StageConfig.StageSlot template,
      String key,
      String displayName,
      int profileIndex,
      String profileName,
      StageConfig.ScenePoint focus,
      boolean occupied
  ) {}
}
