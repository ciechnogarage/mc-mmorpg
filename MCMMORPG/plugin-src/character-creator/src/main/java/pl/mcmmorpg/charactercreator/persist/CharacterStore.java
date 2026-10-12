package pl.mcmmorpg.charactercreator.persist;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import pl.mcmmorpg.charactercreator.creator.NameRules;

/**
 * Flat-file store, keyed by MMOProfiles profile id -> one character record. One record per
 * profile — MMOProfiles itself already owns "which player has which profiles" (its
 * ProfileList/ProfileListViewer), so this store doesn't need to.
 *
 * <p><b>Zapis jest atomowy i odroczony.</b> Poprzednia wersja robila {@code yaml.save(file)}
 * wprost do pliku docelowego, synchronicznie, przy KAZDYM potwierdzeniu postaci. Crash, kill
 * albo brak miejsca w trakcie zapisu zostawialy obciety YAML, czyli utrate WSZYSTKICH postaci
 * na serwerze naraz. Teraz: serializacja do pliku tymczasowego + {@code ATOMIC_MOVE}, poza
 * watkiem glownym, z debounce — w najgorszym razie tracimy ostatnie {@value #DEBOUNCE_TICKS}
 * tickow zmian, nigdy caly plik.
 *
 * <p>Layout in characters.yml:
 * <pre>
 * schema-version: 2
 * profiles:
 *   &lt;profileId&gt;:
 *     name: ...
 *     race: ...
 *     class: ...
 *     skin-source: ...    # klucz w skins.yml, np. 'elf_ninja'; puste = brak skorki
 *     owner-uuid: ...      # od schematu 2
 *     owner-name: ...      # od schematu 2, wylacznie informacyjne
 *     created-at: &lt;epoch millis&gt;
 *     last-used: &lt;epoch millis&gt;   # od schematu 3; brak = postac nigdy nie byla wybrana
 * </pre>
 */
public final class CharacterStore {

  /** Podbijac przy KAZDEJ zmianie ukladu pliku i dopisywac krok w {@link #migrate}. */
  public static final int SCHEMA_VERSION = 3;

  /** 2 sekundy: dosc, by zlac serie zmian, za malo, by strata bolala. */
  private static final long DEBOUNCE_TICKS = 40L;

  /**
   * Ile kopii trzymamy ZAWSZE, niezaleznie od wieku — siatka na wypadek serii awaryjnych
   * restartow tego samego dnia.
   */
  private static final int BACKUPS_KEPT = 10;

  /**
   * Jak stare kopie trzymamy ponad {@link #BACKUPS_KEPT}.
   *
   * <p>Rotacja po samej LICZBIE oznaczala historie dlugosci "ostatnie 10 restartow". W logach
   * jest 93 uruchomien pluginu, a kopia startowa powstawala przy kazdym — wiec na dysku
   * zostawalo niecale dwa dni historii, a jedyna kopia sprzed czyszczenia rekordow
   * (characters-20260918-165118-prune.yml, 18 rekordow) miala wypasc przy najblizszym
   * restarcie. Czas jest tu wlasciwa miara, liczba restartow nie jest.
   */
  private static final java.time.Duration BACKUP_RETENTION = java.time.Duration.ofDays(30);

  private static final DateTimeFormatter STAMP =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

  private final Plugin plugin;
  private final File file;
  private final File backupDir;
  private final Logger logger;
  private final ConcurrentHashMap<UUID, CharacterRecord> cache = new ConcurrentHashMap<>();
  /** Indeks unikalnosci imion: klucz z {@link NameRules#uniqueKey} -> profil, ktory go zajal. */
  private final ConcurrentHashMap<String, UUID> nameIndex = new ConcurrentHashMap<>();
  /** Rekordy wczytane bez pola {@code name} — trzymane, zeby ich NIE skasowac zapisem. */
  private final List<UUID> corrupt = new ArrayList<>();
  private final AtomicBoolean saveQueued = new AtomicBoolean();

  public CharacterStore(Plugin plugin, Logger logger) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), "characters.yml");
    this.backupDir = new File(plugin.getDataFolder(), "backups");
    this.logger = logger;
  }

  public synchronized void load() {
    cache.clear();
    nameIndex.clear();
    corrupt.clear();
    if (!file.exists()) {
      return;
    }
    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
    int version = yaml.getInt("schema-version", 0);
    ConfigurationSection profilesSection = yaml.getConfigurationSection("profiles");
    if (profilesSection == null) {
      return;
    }
    for (String profileIdKey : profilesSection.getKeys(false)) {
      UUID profileId;
      try {
        profileId = UUID.fromString(profileIdKey);
      } catch (IllegalArgumentException ex) {
        logger.warning("Skipping malformed profile id key in characters.yml: " + profileIdKey);
        continue;
      }
      ConfigurationSection c = profilesSection.getConfigurationSection(profileIdKey);
      if (c == null) {
        continue;
      }
      // Brak 'name' to CICHA KORUPCJA, nie brakujace pole: stara wersja podstawiala w to
      // miejsce ID profilu, wiec postac nazywala sie 36-znakowym UUID-em i nikt tego nie
      // zauwazal. Rekord zostaje (zapis go nie skasuje), ale jest jawnie zglaszany.
      String name = c.getString("name");
      if (name == null || name.isBlank()) {
        corrupt.add(profileId);
        logger.severe("characters.yml: rekord " + profileId + " nie ma pola 'name' — "
            + "zostaje zachowany, ale jest niepelny. Sprawdz: /cc info " + profileId);
        name = "";
      }
      UUID ownerUuid = null;
      String ownerRaw = c.getString("owner-uuid");
      if (ownerRaw != null && !ownerRaw.isBlank()) {
        try {
          ownerUuid = UUID.fromString(ownerRaw);
        } catch (IllegalArgumentException ex) {
          logger.warning("characters.yml: rekord " + profileId + " ma bledne 'owner-uuid': " + ownerRaw);
        }
      }
      long lastUsed = c.getLong("last-used", 0L);
      CharacterRecord record = new CharacterRecord(
          profileIdKey,
          name,
          c.getString("race", ""),
          c.getString("class", ""),
          c.getString("skin-source", ""),
          ownerUuid,
          c.getString("owner-name", ""),
          Instant.ofEpochMilli(c.getLong("created-at", System.currentTimeMillis())),
          // 0 = klucza nie ma (rekord sprzed schematu 3). NIE podstawiamy tu 'created-at':
          // rozroznienie "nigdy nie wybrana" od "wybrana wtedy, kiedy powstala" robi
          // CharacterRecord#effectiveLastUsed, a plik ma mowic prawde o tym, co wie.
          lastUsed == 0L ? null : Instant.ofEpochMilli(lastUsed),
          // Opcjonalne od sceny kreatora; brak = kolor domyslny. Bez podbicia schematu:
          // pole tylko dopisane, a stary jar po prostu je pominie.
          c.getString("icon-color", ""));
      cache.put(profileId, record);
      indexName(profileId, record.name());
    }
    migrate(version);
    logger.info("Wczytano " + cache.size() + " postaci (schema " + version + " -> "
        + SCHEMA_VERSION + ", niepelnych: " + corrupt.size() + ")");
  }

  /**
   * Kroki migracji miedzy wersjami schematu. Dzis nie ma czego przeliczac — schemat 2 tylko
   * DOPISAL pola wlasciciela, a ich brak jest poprawnym stanem starego rekordu. Metoda
   * istnieje po to, zeby nastepna zmiana ukladu miala gdzie wyladowac zamiast znow zostawic
   * plik bez wersji.
   */
  private void migrate(int fromVersion) {
    if (fromVersion == SCHEMA_VERSION) {
      return;
    }
    if (fromVersion < 3) {
      // Schemat 3 tylko DOPISAL 'last-used'. Brak tego pola jest poprawnym stanem starego
      // rekordu — wypelni sie sam przy pierwszym wyborze postaci przez gracza, a do tego
      // czasu za znacznik robi 'created-at'.
      logger.info("Migracja schematu " + fromVersion + " -> " + SCHEMA_VERSION
          + ": rekordy bez 'last-used' uzywaja daty utworzenia, dopoki gracz nie wybierze "
          + "postaci.");
    }
    if (fromVersion < 2) {
      long withoutOwner = cache.values().stream().filter(r -> !r.hasOwner()).count();
      if (withoutOwner > 0) {
        logger.info("Migracja schematu " + fromVersion + " -> " + SCHEMA_VERSION + ": "
            + withoutOwner + " rekordow bez wlasciciela. Zostana uzupelnione przy "
            + "najblizszym wyborze profilu przez gracza.");
      }
    }
    scheduleSave();
  }

  private void indexName(UUID profileId, String name) {
    if (name == null || name.isBlank()) {
      return;
    }
    UUID previous = nameIndex.putIfAbsent(NameRules.uniqueKey(name), profileId);
    if (previous != null && !previous.equals(profileId)) {
      // Istniejace duplikaty (na produkcji bylo 12x "Grimtest") NIE sa usuwane po cichu —
      // indeks trzyma pierwszy, a reszta jest zglaszana. Nowe imiona juz nie przejda.
      logger.warning("characters.yml: imie '" + name + "' powtarza sie — profil "
          + profileId + " duplikuje " + previous + ". Nowe postacie o tym imieniu "
          + "beda odrzucane; stare rekordy sprzatnij przez /cc delete.");
    }
  }

  /** True, jesli imie jest juz zajete przez INNY profil niz podany. */
  public boolean isNameTaken(String name, UUID exceptProfileId) {
    UUID owner = nameIndex.get(NameRules.uniqueKey(name));
    return owner != null && !owner.equals(exceptProfileId);
  }

  public CharacterRecord get(UUID profileId) {
    return cache.get(profileId);
  }

  public Collection<CharacterRecord> all() {
    return List.copyOf(cache.values());
  }

  public Map<UUID, CharacterRecord> allByProfile() {
    return Map.copyOf(cache);
  }

  public List<UUID> corruptRecords() {
    return List.copyOf(corrupt);
  }

  /**
   * Postac, ktora ten gracz grał ostatnio — albo {@code null}, gdy nie ma zadnej.
   *
   * <p>Uzywane przy WEJSCIU na serwer, zanim gracz cokolwiek wybierze: wtedy nie ma jeszcze
   * ani eventu MMOProfiles, ani aktywnego profilu, a skorka ma juz byc na graczu.
   */
  public CharacterRecord lastUsedBy(UUID ownerUuid) {
    return pickLastUsed(cache.values(), ownerUuid);
  }

  /**
   * Czysta regula wyboru — bez Bukkita, bo to jedyna czesc tej sciezki, ktora da sie
   * pomylic niezauwazenie (i jedyna, ktora ma test).
   *
   * <p>Najnowszy {@link CharacterRecord#effectiveLastUsed()} wygrywa. Rekordy bez
   * wlasciciela (schemat 1) sa pomijane: nie da sie ich przypisac do gracza, a zgadywanie
   * po nicku konczyloby sie zalozeniem komus cudzej skorki.
   */
  static CharacterRecord pickLastUsed(Collection<CharacterRecord> records, UUID ownerUuid) {
    if (ownerUuid == null) {
      return null;
    }
    CharacterRecord best = null;
    for (CharacterRecord record : records) {
      if (!ownerUuid.equals(record.ownerUuid())) {
        continue;
      }
      if (best == null || record.effectiveLastUsed().isAfter(best.effectiveLastUsed())) {
        best = record;
      }
    }
    return best;
  }

  /** Zapisuje rekord i planuje utrwalenie. Zwraca rekord albo null, gdy imie jest zajete. */
  public CharacterRecord put(CharacterRecord record) {
    UUID profileId = UUID.fromString(record.characterId());
    if (isNameTaken(record.name(), profileId)) {
      return null;
    }
    CharacterRecord previous = cache.put(profileId, record);
    if (previous != null && previous.name() != null) {
      nameIndex.remove(NameRules.uniqueKey(previous.name()), profileId);
    }
    indexName(profileId, record.name());
    scheduleSave();
    return record;
  }

  public boolean remove(UUID profileId) {
    CharacterRecord removed = cache.remove(profileId);
    if (removed == null) {
      return false;
    }
    if (removed.name() != null) {
      nameIndex.remove(NameRules.uniqueKey(removed.name()), profileId);
    }
    corrupt.remove(profileId);
    scheduleSave();
    return true;
  }

  /** Usuwa wiele rekordow naraz (np. /cc prune) — z kopia zapasowa PRZED zmiana. */
  public int removeAll(Collection<UUID> profileIds, String reason) {
    backupNow(reason);
    int removed = 0;
    for (UUID profileId : profileIds) {
      if (remove(profileId)) {
        removed++;
      }
    }
    saveNow();
    return removed;
  }

  /**
   * Odroczony, asynchroniczny zapis. Kazde wywolanie w oknie debounce'u sklada sie w jeden
   * zapis — przy 18 rekordach to nie ma znaczenia, przy 50 tysiacach roznica miedzy pelna
   * serializacja na kazdym potwierdzeniu a jedna co 2 s to roznica miedzy dzialajacym
   * serwerem a zablokowana petla tickow.
   */
  public void scheduleSave() {
    if (!saveQueued.compareAndSet(false, true)) {
      return;
    }
    try {
      Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
        saveQueued.set(false);
        writeAtomically(snapshot());
      }, DEBOUNCE_TICKS);
    } catch (RuntimeException ex) {
      // Scheduler niedostepny: plugin juz wylaczany (IllegalPluginAccessException) albo
      // brak dzialajacego serwera. Zapisz od razu, synchronicznie — lepiej zablokowac
      // watek na milisekunde niz zgubic zapis.
      saveQueued.set(false);
      saveNow();
    }
  }

  /** Synchroniczny zapis — dla onDisable i operacji administracyjnych. */
  public synchronized void saveNow() {
    saveQueued.set(false);
    writeAtomically(snapshot());
  }

  private String snapshot() {
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.set("schema-version", SCHEMA_VERSION);
    Map<UUID, CharacterRecord> copy = new LinkedHashMap<>(cache);
    for (var entry : copy.entrySet()) {
      String path = "profiles." + entry.getKey();
      CharacterRecord record = entry.getValue();
      yaml.set(path + ".name", record.name());
      yaml.set(path + ".race", record.raceId());
      yaml.set(path + ".class", record.classId());
      yaml.set(path + ".skin-source", record.skinSource());
      if (record.ownerUuid() != null) {
        yaml.set(path + ".owner-uuid", record.ownerUuid().toString());
      }
      if (record.ownerName() != null && !record.ownerName().isBlank()) {
        yaml.set(path + ".owner-name", record.ownerName());
      }
      yaml.set(path + ".created-at", record.createdAt().toEpochMilli());
      if (record.lastUsedAt() != null) {
        yaml.set(path + ".last-used", record.lastUsedAt().toEpochMilli());
      }
      if (record.iconColor() != null && !record.iconColor().isBlank()) {
        yaml.set(path + ".icon-color", record.iconColor());
      }
    }
    return yaml.saveToString();
  }

  /**
   * Zapis tymczasowy + {@code ATOMIC_MOVE}. Przerwanie w dowolnym momencie zostawia albo
   * stary, albo nowy plik — nigdy obcietego.
   */
  private synchronized void writeAtomically(String contents) {
    Path target = file.toPath();
    Path tmp = target.resolveSibling(file.getName() + ".tmp");
    try {
      Files.createDirectories(target.getParent());
      Files.writeString(tmp, contents, StandardCharsets.UTF_8);
      try {
        Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException ex) {
        // Niektore montowania (np. przez sie) nie potrafia atomowego move — wtedy
        // zwykly REPLACE_EXISTING jest i tak lepszy niz pisanie wprost do pliku.
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Nie udalo sie zapisac characters.yml — dane zostaja w pamieci", ex);
      try {
        Files.deleteIfExists(tmp);
      } catch (IOException ignored) {
        // Plik tymczasowy zostanie nadpisany przy nastepnym zapisie.
      }
    }
  }

  /**
   * Kopia rotacyjna. Wolana przy starcie i przed kazda operacja masowa — dzis nie bylo
   * zadnej, wiec jedna zla komenda kasowala postacie bezpowrotnie.
   */
  public void backupNow(String reason) {
    if (!file.exists()) {
      return;
    }
    // Kopia pustego pliku to nie kopia. Backup startowy powstawal BEZWARUNKOWO, wiec
    // characters-20260918-174841-startup.yml ma 18 bajtow i zawiera samo
    // "schema-version: 2" — a kazda taka kopia wypychala z rotacji jedna prawdziwa.
    if (!hasAnyProfile()) {
      logger.fine("Pomijam kopie zapasowa (" + reason + "): characters.yml nie ma zadnych "
          + "rekordow.");
      return;
    }
    try {
      Files.createDirectories(backupDir.toPath());
      String label = reason == null || reason.isBlank() ? "manual"
          : reason.replaceAll("[^A-Za-z0-9_-]", "_");
      Path copy = backupDir.toPath()
          .resolve("characters-" + LocalDateTime.now().format(STAMP) + "-" + label + ".yml");
      Files.copy(file.toPath(), copy, StandardCopyOption.REPLACE_EXISTING);
      rotateBackups();
      logger.info("Kopia zapasowa characters.yml: " + copy.getFileName());
    } catch (IOException ex) {
      logger.log(Level.WARNING, "Nie udalo sie zrobic kopii zapasowej characters.yml", ex);
    }
  }

  /**
   * Kasuje kopie, ktore sa JEDNOCZESNIE poza pierwsza dziesiatka i starsze niz
   * {@link #BACKUP_RETENTION}. Oba warunki naraz, bo kazdy z osobna zawodzi w druga
   * strone: sama liczba gubi historie przy serii restartow, sam czas potrafi zostawic
   * katalog pusty po dlugiej przerwie w pracy serwera.
   */
  /**
   * Czy plik na DYSKU ma jakikolwiek rekord. Celowo czyta plik, a nie cache: kopia startowa
   * powstaje przed {@link #load()}, wiec cache jest wtedy jeszcze pusty.
   */
  private boolean hasAnyProfile() {
    ConfigurationSection profiles =
        YamlConfiguration.loadConfiguration(file).getConfigurationSection("profiles");
    return profiles != null && !profiles.getKeys(false).isEmpty();
  }

  private void rotateBackups() throws IOException {
    java.time.Instant cutoff = Instant.now().minus(BACKUP_RETENTION);
    try (Stream<Path> stream = Files.list(backupDir.toPath())) {
      List<Path> backups = stream
          .filter(p -> p.getFileName().toString().startsWith("characters-"))
          .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
          .toList();
      for (int i = BACKUPS_KEPT; i < backups.size(); i++) {
        Path candidate = backups.get(i);
        if (Files.getLastModifiedTime(candidate).toInstant().isBefore(cutoff)) {
          Files.deleteIfExists(candidate);
        }
      }
    }
  }
}
