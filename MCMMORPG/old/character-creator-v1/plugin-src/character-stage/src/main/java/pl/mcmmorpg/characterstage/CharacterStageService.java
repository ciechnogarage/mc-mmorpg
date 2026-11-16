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
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class CharacterStageService {
  private static final double MOVEMENT_RADIUS_MULTIPLIER = 6.0D;
  private static final String SLOT_RESOLVER_MALFORMED_DETAIL =
      "character slot resolver returned malformed result";

  private final JavaPlugin plugin;
  private final ProfileBackendAdapter profileBackendAdapter;
  private final CharacterStageSlotResolver slotResolver;
  private final Map<UUID, String> focusedSlots = new HashMap<>();
  private final Map<UUID, Boolean> stagedPlayers = new HashMap<>();
  private final MiniMessage miniMessage = MiniMessage.miniMessage();

  private StageConfig config;

  CharacterStageService(
      JavaPlugin plugin,
      StageConfig config,
      ProfileBackendAdapter profileBackendAdapter,
      CharacterStageSlotResolver slotResolver
  ) {
    this.plugin = plugin;
    this.config = config;
    this.profileBackendAdapter = profileBackendAdapter;
    this.slotResolver = slotResolver;
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

  public boolean tryHandleAnchor(Player player, Block block) {
    if (!isStaged(player)) {
      return false;
    }

    if (config.scene().confirmAnchor().matches(block)) {
      confirmFocusedSlot(player);
      return true;
    }

    if (config.scene().backAnchor().matches(block)) {
      focusedSlots.remove(player.getUniqueId());
      player.teleport(location(config.scene().spawn()));
      sendStageTitle(player);
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

  public boolean shouldBlockMovement(Player player, Location from, Location to) {
    if (!config.lockStageMovement() || !isStaged(player) || from == null || to == null) {
      return false;
    }

    if (sameBlock(from, to)) {
      return false;
    }

    Location spawn = location(config.scene().spawn());
    if (!sameWorld(spawn, to)) {
      player.sendMessage(text(config.messages().blockedMovement(), null));
      return true;
    }

    if (spawn.distanceSquared(to) <= allowedMovementDistanceSquared()) {
      return false;
    }

    player.sendMessage(text(config.messages().blockedMovement(), null));
    return true;
  }

  private boolean sameBlock(Location from, Location to) {
    return sameWorld(from, to)
        && from.getBlockX() == to.getBlockX()
        && from.getBlockY() == to.getBlockY()
        && from.getBlockZ() == to.getBlockZ();
  }

  private boolean sameWorld(Location left, Location right) {
    return left.getWorld() != null
        && right.getWorld() != null
        && left.getWorld().getUID().equals(right.getWorld().getUID());
  }

  private double allowedMovementDistanceSquared() {
    double radius = config.focusRadius() * MOVEMENT_RADIUS_MULTIPLIER;
    return radius * radius;
  }

  private void focusSlot(Player player, ResolvedSlot slot) {
    String current = focusedSlots.get(player.getUniqueId());
    if (slot.key().equals(current)) {
      runActions(player, slot);
      return;
    }

    focusedSlots.put(player.getUniqueId(), slot.key());
    player.teleport(location(slot.focus()));
    switch (slot.state()) {
      case READY -> sendTitle(
          player,
          config.messages().focusedTitle(),
          config.messages().focusedSubtitle(),
          slot);
      case EMPTY -> sendTitle(
          player,
          config.messages().emptyTitle(),
          config.messages().emptySubtitle(),
          slot);
      case INCOMPLETE -> sendTitle(
          player,
          config.messages().incompleteTitle(),
          config.messages().incompleteSubtitle(),
          slot);
      case BLOCKED -> sendTitle(
          player,
          config.messages().blockedTitle(),
          config.messages().blockedSubtitle(),
          slot);
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
      sendStageTitle(player);
      return;
    }

    runActions(player, resolveSlot(player, slot));
  }

  private void runActions(Player player, ResolvedSlot slot) {
    switch (slot.state()) {
      case READY -> {
        player.sendMessage(text(config.messages().enterReady(), slot));
        dispatchActions(player, slot, slot.template().occupiedActions());
      }
      case EMPTY -> {
        player.sendMessage(text(config.messages().enterReady(), slot));
        dispatchActions(player, slot, slot.template().createActions());
      }
      case INCOMPLETE -> player.sendMessage(text(config.messages().incompleteSelection(), slot));
      case BLOCKED -> player.sendMessage(text(config.messages().blockedSelection(), slot));
    }
  }

  private void dispatchActions(Player player, ResolvedSlot slot, List<String> actions) {
    for (String action : actions) {
      if (action.startsWith("console:")) {
        Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(),
            inject(action.substring("console:".length()), player, slot));
      } else if (action.startsWith("player:")) {
        player.performCommand(inject(action.substring("player:".length()), player, slot));
      }
    }
  }

  private Location location(StageConfig.ScenePoint point) {
    World world = Bukkit.getWorld(point.world());
    if (world == null) {
      throw new IllegalStateException("Unknown CharacterStage world: " + point.world());
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
      value = value.replace("%profile_name%", slot.profileName() == null ? "" : slot.profileName());
      value = value.replace("%character_id%", slot.characterId() == null ? "" : slot.characterId());
    }
    return value;
  }

  private ResolvedSlot resolveSlot(Player player, StageConfig.StageSlot slot) {
    ProfileBackendAdapter.ProfileSlotData profileSlotData;
    try {
      java.util.Optional<ProfileBackendAdapter.ProfileSlotData> profileSlot =
          profileBackendAdapter.getSlot(player, slot.profileIndex());
      profileSlotData = profileSlot == null ? null : profileSlot.orElse(null);
    } catch (RuntimeException exception) {
      profileSlotData = null;
    }
    if (profileSlotData == null) {
      profileSlotData = ProfileBackendAdapter.ProfileSlotData.blocked(
          slot.profileIndex(),
          slot.displayName());
    }

    CharacterStageSlotResolver.SlotResolution slotResolution;
    try {
      slotResolution = slotResolver.resolve(slot, profileSlotData);
    } catch (RuntimeException exception) {
      slotResolution = null;
    }
    if (isMalformed(slotResolution)) {
      slotResolution = CharacterStageSlotResolver.SlotResolution.blocked(
          SLOT_RESOLVER_MALFORMED_DETAIL);
    }

    String displayName = profileSlotData.occupied() ? profileSlotData.profileName() : slot.displayName();
    return new ResolvedSlot(
        slot,
        slot.key(),
        displayName,
        slot.profileIndex(),
        profileSlotData.profileName(),
        slot.focus(),
        slotResolution.state(),
        slotResolution.character() == null ? null : slotResolution.character().characterId());
  }

  private boolean isMalformed(CharacterStageSlotResolver.SlotResolution slotResolution) {
    if (slotResolution == null || slotResolution.state() == null) {
      return true;
    }
    return switch (slotResolution.state()) {
      case READY, INCOMPLETE -> slotResolution.character() == null
          || slotResolution.character().characterId() == null
          || slotResolution.character().characterId().isBlank();
      case EMPTY, BLOCKED -> slotResolution.character() != null;
    };
  }

  private record ResolvedSlot(
      StageConfig.StageSlot template,
      String key,
      String displayName,
      int profileIndex,
      String profileName,
      StageConfig.ScenePoint focus,
      CharacterStageSlotResolver.SlotState state,
      String characterId
  ) {}
}
