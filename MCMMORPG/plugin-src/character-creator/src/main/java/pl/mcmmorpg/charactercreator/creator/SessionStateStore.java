package pl.mcmmorpg.charactercreator.creator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

/**
 * Utrwala {@link EntryState} miedzy restartami serwera.
 *
 * <p>Plik {@code sessions.yml} jest z zalozenia PUSTY w stanie spoczynku: wpis pojawia sie
 * przy wejsciu do kreatora i znika przy wyjsciu. Kazdy wpis, ktory przetrwa restart, oznacza
 * gracza, ktory nie dokonczyl tworzenia postaci — i to on jest podstawa przywrocenia jego
 * rzeczy przy najblizszym wejsciu na serwer.
 *
 * <p>Zapis jest atomowy z tego samego powodu co w CharacterStore: obciety plik to plecaki
 * wszystkich graczy, ktorzy akurat byli w kreatorze.
 */
public final class SessionStateStore {

  private final File file;
  private final Logger logger;
  private final YamlConfiguration yaml = new YamlConfiguration();

  public SessionStateStore(File dataFolder, Logger logger) {
    this.file = new File(dataFolder, "sessions.yml");
    this.logger = logger;
  }

  public synchronized void load() {
    if (!file.exists()) {
      return;
    }
    try {
      yaml.load(file);
    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Nie udalo sie wczytac sessions.yml — stany wejscia do "
          + "kreatora sa nieodzyskiwalne dla tego restartu", ex);
    }
  }

  public synchronized Set<UUID> pendingPlayers() {
    ConfigurationSection section = yaml.getConfigurationSection("sessions");
    if (section == null) {
      return Set.of();
    }
    return section.getKeys(false).stream()
        .map(key -> {
          try {
            return UUID.fromString(key);
          } catch (IllegalArgumentException ex) {
            return null;
          }
        })
        .filter(java.util.Objects::nonNull)
        .collect(Collectors.toUnmodifiableSet());
  }

  public synchronized void put(EntryState state) {
    String path = "sessions." + state.player();
    yaml.set(path + ".gamemode", state.gameMode().name());
    yaml.set(path + ".saved-at", state.savedAt());
    yaml.set(path + ".inventory", toList(state.inventory()));
    yaml.set(path + ".armor", toList(state.armor()));
    yaml.set(path + ".off-hand", state.offHand());
    Location origin = state.origin();
    if (origin != null && origin.getWorld() != null) {
      yaml.set(path + ".origin.world", origin.getWorld().getName());
      yaml.set(path + ".origin.x", origin.getX());
      yaml.set(path + ".origin.y", origin.getY());
      yaml.set(path + ".origin.z", origin.getZ());
      yaml.set(path + ".origin.yaw", origin.getYaw());
      yaml.set(path + ".origin.pitch", origin.getPitch());
    }
    write();
  }

  public synchronized EntryState get(UUID player) {
    ConfigurationSection s = yaml.getConfigurationSection("sessions." + player);
    if (s == null) {
      return null;
    }
    GameMode gameMode;
    try {
      gameMode = GameMode.valueOf(s.getString("gamemode", "SURVIVAL"));
    } catch (IllegalArgumentException ex) {
      gameMode = GameMode.SURVIVAL;
    }
    Location origin = null;
    ConfigurationSection o = s.getConfigurationSection("origin");
    if (o != null) {
      World world = Bukkit.getWorld(o.getString("world", ""));
      if (world != null) {
        origin = new Location(world, o.getDouble("x"), o.getDouble("y"), o.getDouble("z"),
            (float) o.getDouble("yaw"), (float) o.getDouble("pitch"));
      }
    }
    return new EntryState(player,
        fromList(s.getList("inventory"), 36),
        fromList(s.getList("armor"), 4),
        s.getItemStack("off-hand"),
        gameMode,
        origin,
        s.getLong("saved-at"));
  }

  public synchronized void clear(UUID player) {
    if (yaml.get("sessions." + player) == null) {
      return;
    }
    yaml.set("sessions." + player, null);
    write();
  }

  private static List<ItemStack> toList(ItemStack[] items) {
    List<ItemStack> out = new ArrayList<>();
    if (items != null) {
      // Celowo przez Collections.addAll, a nie List.of — sloty puste to null, a List.of
      // nulli nie przyjmuje i wywalilaby zapis calego ekwipunku na pierwszej dziurze.
      java.util.Collections.addAll(out, items);
    }
    return out;
  }

  private static ItemStack[] fromList(List<?> raw, int size) {
    ItemStack[] out = new ItemStack[size];
    if (raw == null) {
      return out;
    }
    for (int i = 0; i < raw.size() && i < size; i++) {
      Object value = raw.get(i);
      out[i] = value instanceof ItemStack item ? item : null;
    }
    return out;
  }

  private void write() {
    Path target = file.toPath();
    Path tmp = target.resolveSibling(file.getName() + ".tmp");
    try {
      Files.createDirectories(target.getParent());
      Files.writeString(tmp, yaml.saveToString(), StandardCharsets.UTF_8);
      try {
        Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException ex) {
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Nie udalo sie zapisac sessions.yml — ekwipunek gracza w "
          + "kreatorze NIE przetrwa restartu", ex);
    }
  }
}
