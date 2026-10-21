package pl.mcmmorpg.charactercreator.creator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.bukkit.plugin.Plugin;

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
 *
 * <p><b>Dwie rzeczy zmienione po przegladzie cyklu tworzenia postaci.</b>
 *
 * <p>Po pierwsze, wpis byl kasowany WYLACZNIE przy ponownym wejsciu gracza na serwer, wiec
 * gracz, ktory nie wrocil, zostawial go na zawsze: na produkcji dwa plecaki z 18 wrzesnia
 * wisialy tu przez kazdy z 93 restartow, a serwer przy kazdym starcie wypisywal ostrzezenie
 * o "graczach, ktorych ekwipunek wroci". Teraz wpisy starsze niz {@link #STASH_TTL} sa przy
 * starcie ODKLADANE do {@code backups/sessions-expired-*.yml} — nie kasowane. Wygasniecie
 * jest decyzja o tym, ze wpis przestaje blokowac plik, a nie o tym, ze plecak przepada.
 *
 * <p>Po drugie, {@code write()} bylo synchroniczne na watku glownym i serializowalo CALY
 * plik — z NBT kazdego przedmiotu wszystkich zapisanych graczy — przy kazdym wejsciu i
 * kazdym wyjsciu z kreatora. Serializacja nadal dzieje sie na watku glownym (bo dotyka
 * ItemStackow), ale POD debouncem, a nie przed nim: profil spark pokazal, ze {@code
 * sweepInventory} w watchdogu wolal {@code put()} seriami, a kazde takie wolanie
 * serializowalo caly plik od nowa — samo YAML-owanie zjadalo 1,5 s na 15 min zycia
 * serwera, na watku glownym. Teraz seria zmian w oknie debounce'u to JEDNA serializacja
 * (na koncu okna, gdy yaml ma juz komplet zmian) i JEDEN zapis na dysk, na watku pobocznym.
 */
public final class SessionStateStore {

  /**
   * Po jakim czasie niedokonczona sesja przestaje blokowac plik. Dwa tygodnie: dosc, zeby
   * gracz wrocil z wakacji po swoj plecak, za malo, zeby plik rosl w nieskonczonosc.
   */
  private static final Duration STASH_TTL = Duration.ofDays(14);

  /** 2 sekundy, tak samo jak w CharacterStore — dosc, by zlac serie zmian. */
  private static final long DEBOUNCE_TICKS = 40L;

  private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

  private final Plugin plugin;
  private final File file;
  private final File backupDir;
  private final Logger logger;
  private final YamlConfiguration yaml = new YamlConfiguration();

  private final AtomicBoolean saveQueued = new AtomicBoolean();

  public SessionStateStore(Plugin plugin, Logger logger) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), "sessions.yml");
    this.backupDir = new File(plugin.getDataFolder(), "backups");
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
      return;
    }
    expireStaleEntries();
  }

  /**
   * Przenosi przeterminowane wpisy do osobnego pliku w {@code backups/}.
   *
   * <p>Wpis BEZ pola {@code saved-at} (albo z zerem) jest traktowany jako nieznany wiek
   * i ZOSTAJE. Utrata plecaka jest nieodwracalna, a zbedny wpis w pliku nie jest — przy
   * watpliwosci wybieramy to drugie.
   */
  private void expireStaleEntries() {
    ConfigurationSection sessions = yaml.getConfigurationSection("sessions");
    if (sessions == null) {
      return;
    }
    long cutoff = System.currentTimeMillis() - STASH_TTL.toMillis();
    List<String> stale = new ArrayList<>();
    for (String key : sessions.getKeys(false)) {
      if (isStale(sessions.getLong(key + ".saved-at", 0L), cutoff)) {
        stale.add(key);
      }
    }
    if (stale.isEmpty()) {
      return;
    }
    YamlConfiguration expired = new YamlConfiguration();
    for (String key : stale) {
      expired.set("sessions." + key, sessions.get(key));
      yaml.set("sessions." + key, null);
    }
    Path target = backupDir.toPath()
        .resolve("sessions-expired-" + LocalDateTime.now().format(STAMP) + ".yml");
    try {
      Files.createDirectories(backupDir.toPath());
      Files.writeString(target, expired.saveToString(), StandardCharsets.UTF_8);
      logger.warning(stale.size() + " niedokonczonych sesji kreatora bylo starszych niz "
          + STASH_TTL.toDays() + " dni — ich ekwipunek zostal odlozony do "
          + target.getFileName() + " i NIE wroci juz sam. Gracze: " + stale);
      scheduleSave();
    } catch (IOException ex) {
      // Nie udalo sie odlozyc — wiec nie usuwamy. Lepiej rosnacy plik niz zgubiony plecak.
      logger.log(Level.SEVERE, "Nie udalo sie zapisac " + target.getFileName()
          + " — przeterminowane sesje ZOSTAJA w sessions.yml", ex);
      try {
        yaml.load(file);
      } catch (Exception reloadFailure) {
        logger.log(Level.SEVERE, "Nie udalo sie tez przywrocic sessions.yml z dysku",
            reloadFailure);
      }
    }
  }

  /**
   * Czy wpis przeterminowal sie.
   *
   * <p>{@code savedAt <= 0} to wpis z czasow sprzed tego pola — wiek NIEZNANY, wiec wpis
   * zostaje. Utrata plecaka jest nieodwracalna, zbedny wpis w pliku nie jest.
   * Wydzielone, zeby dalo sie sprawdzic testem bez dzialajacego serwera.
   */
  static boolean isStale(long savedAt, long cutoffMillis) {
    return savedAt > 0L && savedAt < cutoffMillis;
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
    scheduleSave();
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
    scheduleSave();
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

  /**
   * Odklada CALY zapis — i serializacje, i I/O — na koniec okna debounce'u. Pierwsze
   * wolanie w oknie zamawia zadanie, kolejne nie robia nic: yaml jest juz zaktualizowany,
   * wiec jedna serializacja na koncu okna widzi komplet zmian.
   *
   * <p>Serializacja leci zadaniem SYNCHRONICZNYM, bo dotyka ItemStackow, a te nalezy
   * czytac na watku glownym. Na watek poboczny idzie dopiero gotowy string.
   */
  private void scheduleSave() {
    if (!saveQueued.compareAndSet(false, true)) {
      return;
    }
    try {
      Bukkit.getScheduler().runTaskLater(plugin, () -> {
        saveQueued.set(false);
        String snapshot = snapshot();
        try {
          Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> writeAtomically(snapshot));
        } catch (RuntimeException ex) {
          // Wylaczanie pluginu w oknie debounce'u: zapisz tu, na watku glownym.
          writeAtomically(snapshot);
        }
      }, DEBOUNCE_TICKS);
    } catch (RuntimeException ex) {
      // Scheduler niedostepny: plugin juz wylaczany albo brak dzialajacego serwera.
      // Zapisz od razu — lepiej zablokowac watek na milisekunde niz zgubic plecak.
      saveQueued.set(false);
      writeAtomically(snapshot());
    }
  }

  /** Serializacja pod zamkiem: {@code put}/{@code clear} moga w tym czasie zmieniac yaml. */
  private synchronized String snapshot() {
    return yaml.saveToString();
  }

  /**
   * Synchroniczny zapis — dla onDisable. Musi istniec osobno: debounce z {@code scheduleSave}
   * nie zdazylby sie wykonac, a to wlasnie wylaczenie serwera z graczem w kreatorze bylo
   * najdrozszym bledem w calym module.
   */
  public synchronized void saveNow() {
    saveQueued.set(false);
    writeAtomically(yaml.saveToString());
  }

  private void writeAtomically(String contents) {
    if (contents == null) {
      return;
    }
    Path target = file.toPath();
    Path tmp = target.resolveSibling(file.getName() + ".tmp");
    try {
      Files.createDirectories(target.getParent());
      Files.writeString(tmp, contents, StandardCharsets.UTF_8);
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
