package pl.mcmmorpg.charactercreator.persist;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Kosz postaci: usuniecie z ekranu "Zarzadzaj postaciami" tylko UKRYWA postac na {@code ttl}
 * (domyslnie 7 dni). Profil MMOProfiles zostaje nietkniety — przywrocenie to wykreslenie z kosza.
 * Prawdziwe {@code removeProfile} idzie dopiero po terminie (przy nastepnym wejsciu wlasciciela
 * do wyboru postaci) albo na wyrazne "usun na zawsze" z kosza.
 *
 * <p>Osobny plik {@code trash.yml}, nie pole w characters.yml: w koszu moga lezec tez postacie
 * sprzed kreatora (profil bez naszego rekordu), a zapis kosza nie moze konkurowac z debounce
 * CharacterStore. Zapis przy kazdej zmianie, atomowo (plik tymczasowy + move) — kosz jest maly,
 * a utrata wpisu po restarcie znaczylaby, ze "usunieta" postac nagle wraca na liste.
 */
public final class TrashStore {

  /** Postac w koszu. {@code name} tylko do wyswietlania (np. w /cc kosz przy wylogowanym). */
  public record Entry(UUID profileId, UUID ownerId, String name, Instant trashedAt) {

    public Instant expiresAt(Duration ttl) {
      return trashedAt.plus(ttl);
    }

    public boolean expired(Instant now, Duration ttl) {
      return !now.isBefore(expiresAt(ttl));
    }

    /** Pelne dni do skasowania, zaokraglone w gore (zostalo 1 h = "1 dzien"); 0 po terminie. */
    public long daysLeft(Instant now, Duration ttl) {
      long ms = Duration.between(now, expiresAt(ttl)).toMillis();
      if (ms <= 0) {
        return 0;
      }
      long day = Duration.ofDays(1).toMillis();
      return (ms + day - 1) / day;
    }
  }

  private final File file;
  private final Logger logger;
  private final Map<UUID, Entry> entries = new LinkedHashMap<>();

  public TrashStore(File file, Logger logger) {
    this.file = file;
    this.logger = logger;
  }

  public synchronized void load() {
    entries.clear();
    if (!file.isFile()) {
      return;
    }
    YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
    ConfigurationSection root = yml.getConfigurationSection("trash");
    if (root == null) {
      return;
    }
    for (String key : root.getKeys(false)) {
      ConfigurationSection s = root.getConfigurationSection(key);
      try {
        UUID profile = UUID.fromString(key);
        UUID owner = UUID.fromString(s.getString("owner", ""));
        entries.put(profile, new Entry(profile, owner, s.getString("name", "?"),
            Instant.ofEpochMilli(s.getLong("at"))));
      } catch (RuntimeException ex) {
        logger.warning("trash.yml: pomijam niepoprawny wpis " + key + ": " + ex);
      }
    }
  }

  public synchronized boolean isTrashed(UUID profileId) {
    return entries.containsKey(profileId);
  }

  public synchronized Entry get(UUID profileId) {
    return entries.get(profileId);
  }

  public synchronized void trash(Entry entry) {
    entries.put(entry.profileId(), entry);
    save();
  }

  /** @return true, gdy postac byla w koszu */
  public synchronized boolean restore(UUID profileId) {
    boolean had = entries.remove(profileId) != null;
    if (had) {
      save();
    }
    return had;
  }

  /** Wpisy jednego gracza, najdawniej wyrzucone pierwsze (te wygasaja najszybciej). */
  public synchronized List<Entry> byOwner(UUID ownerId) {
    List<Entry> out = new ArrayList<>();
    for (Entry e : entries.values()) {
      if (e.ownerId().equals(ownerId)) {
        out.add(e);
      }
    }
    out.sort(Comparator.comparing(Entry::trashedAt));
    return out;
  }

  public synchronized List<Entry> all() {
    List<Entry> out = new ArrayList<>(entries.values());
    out.sort(Comparator.comparing(Entry::trashedAt));
    return out;
  }

  /** Wpisy gracza po terminie — do prawdziwego usuniecia. */
  public synchronized List<Entry> expired(UUID ownerId, Instant now, Duration ttl) {
    return expired(byOwner(ownerId), now, ttl);
  }

  static List<Entry> expired(Collection<Entry> of, Instant now, Duration ttl) {
    List<Entry> out = new ArrayList<>();
    for (Entry e : of) {
      if (e.expired(now, ttl)) {
        out.add(e);
      }
    }
    return out;
  }

  private void save() {
    YamlConfiguration yml = new YamlConfiguration();
    yml.options().setHeader(List.of(
        "Kosz postaci CharacterCreatora — postacie ukryte przez gracza, do przywrocenia.",
        "Po terminie (wybor-postaci.kosz-dni) profil jest naprawde usuwany. Nie edytowac recznie",
        "przy wlaczonym serwerze: plik jest zapisywany przy kazdej zmianie."));
    for (Entry e : entries.values()) {
      String base = "trash." + e.profileId();
      yml.set(base + ".owner", e.ownerId().toString());
      yml.set(base + ".name", e.name());
      yml.set(base + ".at", e.trashedAt().toEpochMilli());
    }
    File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
    try {
      file.getParentFile().mkdirs();
      yml.save(tmp);
      Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException ex) {
      logger.severe("Nie udalo sie zapisac " + file + ": " + ex);
    }
  }
}
