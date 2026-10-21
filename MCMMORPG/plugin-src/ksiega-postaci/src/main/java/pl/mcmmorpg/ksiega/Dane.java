package pl.mcmmorpg.ksiega;

import fr.phoenixdevt.profiles.ProfileList;
import fr.phoenixdevt.profiles.ProfileProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

final class Dane {
  static final class Record {
    final UUID key;
    final ItemStack[] jewelry = new ItemStack[4];
    boolean migrated;
    Record(UUID key) { this.key = key; }
  }

  private final KsiegaPostaci plugin;
  private final Map<UUID, Record> active = new HashMap<>();

  Dane(KsiegaPostaci plugin) { this.plugin = plugin; }

  UUID profileKey(Player player) {
    if (!Bukkit.getPluginManager().isPluginEnabled("MMOProfiles")) return player.getUniqueId();
    ProfileProvider provider = (ProfileProvider) Bukkit.getPluginManager().getPlugin("MMOProfiles");
    ProfileList list = provider.getPlayerData(player.getUniqueId());
    return list == null || list.getCurrent() == null ? null : list.getCurrent().getUniqueId();
  }

  Record current(Player player) {
    UUID key = profileKey(player);
    if (key == null) return null;
    Record old = active.get(player.getUniqueId());
    if (old != null && old.key.equals(key)) return old;
    if (old != null) save(old);
    Record loaded = load(key);
    active.put(player.getUniqueId(), loaded);
    return loaded;
  }

  void unload(Player player) {
    Record old = active.remove(player.getUniqueId());
    if (old != null) save(old);
  }

  void saveAll() { for (Record record : active.values()) save(record); }

  private File file(UUID key) { return new File(plugin.getDataFolder(), "profiles/" + key + ".yml"); }

  private Record load(UUID key) {
    Record result = new Record(key);
    File file = file(key);
    if (!file.isFile()) return result;
    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
    result.migrated = yaml.getBoolean("migrated", false);
    for (int i = 0; i < 4; i++) result.jewelry[i] = yaml.getItemStack("jewelry." + i);
    return result;
  }

  boolean save(Record record) {
    File target = file(record.key);
    target.getParentFile().mkdirs();
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.set("migrated", record.migrated);
    for (int i = 0; i < 4; i++) yaml.set("jewelry." + i, record.jewelry[i]);
    File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
    try {
      yaml.save(tmp);
      try {
        Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE);
      } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
        Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
      return true;
    } catch (IOException ex) {
      plugin.getLogger().severe("Nie mozna zapisac profilu " + record.key + ": " + ex);
      return false;
    }
  }
}
