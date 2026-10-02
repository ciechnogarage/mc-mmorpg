package pl.mcmmorpg.mapa;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

final class WlasneZnaczniki {
  record Entry(String id, String world, int x, int z, String name, String icon) {
    MapaData.Marker marker() {
      return new MapaData.Marker(world, x, z, name, "wlasny", icon, List.of(), id);
    }
  }

  private final File folder;
  private final Logger log;
  private final ExecutorService writer = Executors.newSingleThreadExecutor(task -> {
    Thread thread = new Thread(task, "Mapa-zapis-znacznikow");
    thread.setDaemon(true);
    return thread;
  });
  private final Map<UUID, UUID> playerKeys = new HashMap<>();
  private final Map<UUID, List<Entry>> entries = new HashMap<>();
  private volatile Map<UUID, List<Entry>> snapshot = Map.of();

  WlasneZnaczniki(File folder, Logger log) { this.folder = folder; this.log = log; }

  UUID key(Player player, boolean profiles) {
    UUID id = player.getUniqueId();
    UUID key = profiles ? ProfilMost.key(id) : id;
    UUID previous = playerKeys.put(id, key);
    if (!entries.containsKey(key)) {
      entries.put(key, load(key));
      snapshot = Map.copyOf(entries);
    }
    if (!key.equals(previous)) publishKeys();
    return key;
  }

  void forget(Player player) { playerKeys.remove(player.getUniqueId()); }

  List<MapaData.Marker> forPlayer(UUID player, String world) {
    UUID key = playerKeys.get(player);
    if (key == null) return List.of();
    return snapshot.getOrDefault(key, List.of()).stream()
        .filter(e -> MapaData.klucz(e.world()).equals(MapaData.klucz(world)))
        .map(Entry::marker).toList();
  }

  // Tylko odczyt migawki: wolany z asynchronicznego renderera minimapy.
  List<MapaData.Marker> asyncForPlayer(UUID player, String world) {
    UUID key = asyncKeys.get(player);
    if (key == null) return List.of();
    return snapshot.getOrDefault(key, List.of()).stream()
        .filter(e -> MapaData.klucz(e.world()).equals(MapaData.klucz(world)))
        .map(Entry::marker).toList();
  }

  private volatile Map<UUID, UUID> asyncKeys = Map.of();
  void publishKeys() { asyncKeys = Map.copyOf(playerKeys); }

  Entry find(UUID key, String id) {
    return entries.getOrDefault(key, List.of()).stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
  }

  boolean add(UUID key, Entry entry, int limit) {
    List<Entry> old = entries.getOrDefault(key, List.of());
    if (old.size() >= limit) return false;
    List<Entry> next = new ArrayList<>(old);
    next.add(entry);
    change(key, next);
    return true;
  }

  boolean replace(UUID key, Entry entry) {
    List<Entry> next = new ArrayList<>(entries.getOrDefault(key, List.of()));
    for (int i = 0; i < next.size(); i++) if (next.get(i).id().equals(entry.id())) {
      next.set(i, entry); change(key, next); return true;
    }
    return false;
  }

  boolean remove(UUID key, String id) {
    List<Entry> next = new ArrayList<>(entries.getOrDefault(key, List.of()));
    if (!next.removeIf(e -> e.id().equals(id))) return false;
    change(key, next);
    return true;
  }

  private void change(UUID key, List<Entry> values) {
    List<Entry> copy = List.copyOf(values);
    entries.put(key, copy);
    snapshot = Map.copyOf(entries);
    String yaml = serialize(copy);
    writer.execute(() -> {
      try {
        Files.createDirectories(folder.toPath());
        Path temp = Files.createTempFile(folder.toPath(), key.toString(), ".tmp");
        try {
          Files.writeString(temp, yaml);
          Files.move(temp, new File(folder, key + ".yml").toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
      } catch (IOException ex) { log.warning("Nie mozna zapisac znacznikow " + key + ": " + ex.getMessage()); }
    });
  }

  private List<Entry> load(UUID key) {
    File file = new File(folder, key + ".yml");
    return parse(YamlConfiguration.loadConfiguration(file));
  }

  static String serialize(List<Entry> values) {
    YamlConfiguration yaml = new YamlConfiguration();
    List<Map<String, Object>> list = new ArrayList<>();
    for (Entry e : values) list.add(Map.of("id", e.id(), "swiat", e.world(), "x", e.x(),
        "z", e.z(), "nazwa", e.name(), "ikona", e.icon()));
    yaml.set("znaczniki", list);
    return yaml.saveToString();
  }

  static List<Entry> parse(YamlConfiguration yaml) {
    List<Entry> result = new ArrayList<>();
    for (Map<?, ?> raw : yaml.getMapList("znaczniki")) {
      String id = String.valueOf(raw.get("id"));
      String world = String.valueOf(raw.get("swiat"));
      String name = String.valueOf(raw.get("nazwa"));
      String icon = String.valueOf(raw.get("ikona"));
      if (!id.matches("[0-9a-f]{8}") || !world.matches("[a-z0-9_.-]+")
          || name.isBlank() || name.length() > 24 || !icon.matches("[a-z0-9_-]+")) continue;
      if (!(raw.get("x") instanceof Number x) || !(raw.get("z") instanceof Number z)) continue;
      result.add(new Entry(id, world, x.intValue(), z.intValue(), name, icon));
    }
    return List.copyOf(result);
  }

  void close() {
    writer.shutdown();
    try { if (!writer.awaitTermination(5, TimeUnit.SECONDS)) log.warning("Zapis znacznikow trwa nadal."); }
    catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
  }
}
