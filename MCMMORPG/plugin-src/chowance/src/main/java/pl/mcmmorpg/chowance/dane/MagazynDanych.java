package pl.mcmmorpg.chowance.dane;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Zapis per profil MMOProfiles. JEDEN wątek I/O (kolejność zapisów tego samego pliku zachowana),
 * serializacja do JSON na wątku głównym (migawka bez wyścigów), zapis atomowy tmp + move.
 * Pamięć: w cache siedzą tylko profile graczy online.
 */
public final class MagazynDanych {
  private final Path katalog;
  private final Logger log;
  private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
  private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "Chowance-IO");
    t.setDaemon(true);
    return t;
  });
  /** gracz -> profil aktywny */
  private final Map<UUID, UUID> aktywne = new ConcurrentHashMap<>();
  /** profil -> dane (tylko online) */
  private final Map<UUID, ProfilDane> cache = new ConcurrentHashMap<>();

  public MagazynDanych(Path katalog, Logger log) throws IOException {
    this.katalog = katalog;
    this.log = log;
    Files.createDirectories(katalog.resolve("profile"));
    Files.createDirectories(katalog.resolve("archiwum"));
  }

  public Path katalog() { return katalog; }
  public Gson gson() { return gson; }

  private Path plik(UUID profil) { return katalog.resolve("profile").resolve(profil + ".json"); }

  /** Wczytanie poza wątkiem głównym. Brak pliku = nowy, pusty profil (nowa postać = zero). */
  public CompletableFuture<ProfilDane> wczytaj(UUID profil, UUID gracz, String nick) {
    return CompletableFuture.supplyAsync(() -> {
      Path p = plik(profil);
      ProfilDane d = null;
      if (Files.isRegularFile(p)) {
        try {
          d = gson.fromJson(Files.readString(p, StandardCharsets.UTF_8), ProfilDane.class);
        } catch (Exception e) {
          // Uszkodzony plik NIE może zostać nadpisany pustym profilem — odkładamy go na bok.
          Path bad = katalog.resolve("archiwum").resolve(profil + ".uszkodzony-" + System.currentTimeMillis() + ".json");
          try { Files.copy(p, bad); } catch (IOException ignored) {}
          log.log(Level.SEVERE, "Uszkodzony plik profilu " + p + " — kopia w " + bad, e);
          throw new IllegalStateException("uszkodzony profil " + profil, e);
        }
      }
      if (d == null) {
        d = new ProfilDane();
        d.profil = profil.toString();
      }
      d.gracz = gracz.toString();
      d.nick = nick;
      naprawNulle(d);
      return d;
    }, io);
  }

  private static void naprawNulle(ProfilDane d) {
    if (d.druzyna == null) d.druzyna = new java.util.ArrayList<>();
    if (d.magazyn == null) d.magazyn = new java.util.ArrayList<>();
    if (d.bestiariusz == null) d.bestiariusz = new java.util.LinkedHashMap<>();
    if (d.lsniace == null) d.lsniace = new java.util.LinkedHashSet<>();
    if (d.nagrody == null) d.nagrody = new java.util.LinkedHashSet<>();
    if (d.odznaki == null) d.odznaki = new java.util.LinkedHashSet<>();
    if (d.wieza == null) d.wieza = new java.util.LinkedHashMap<>();
    if (d.nagrodyWiezy == null) d.nagrodyWiezy = new java.util.LinkedHashSet<>();
    if (d.jaja == null) d.jaja = new java.util.ArrayList<>();
    if (d.pierwszeKrokiNagrody == null) d.pierwszeKrokiNagrody = new java.util.LinkedHashSet<>();
    if (d.ostatniePoglaszczanie == null) d.ostatniePoglaszczanie = new java.util.LinkedHashMap<>();
    d.druzyna.removeIf(java.util.Objects::isNull);
    d.magazyn.removeIf(java.util.Objects::isNull);
    for (var o : d.druzyna) if (o.ruchy == null) o.ruchy = new java.util.ArrayList<>();
    for (var o : d.magazyn) if (o.ruchy == null) o.ruchy = new java.util.ArrayList<>();
  }

  public void aktywuj(UUID gracz, ProfilDane d) {
    UUID profil = UUID.fromString(d.profil);
    aktywne.put(gracz, profil);
    cache.put(profil, d);
  }

  /** Zdejmuje profil z pamięci i zapisuje go. Zwraca przyszłość zakończenia zapisu. */
  public CompletableFuture<Void> dezaktywuj(UUID gracz) {
    UUID profil = aktywne.remove(gracz);
    if (profil == null) return CompletableFuture.completedFuture(null);
    ProfilDane d = cache.remove(profil);
    if (d == null) return CompletableFuture.completedFuture(null);
    CompletableFuture<Void> f = zapisz(d);
    // Profil zdjęty z pamięci nie wróci do autosave — jedna dodatkowa próba po chwili (kolejka io).
    f.exceptionally(ex -> {
      CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS, io).execute(() -> zapiszTeraz(profil, gson.toJson(d)));
      return null;
    });
    return f;
  }

  public ProfilDane dane(UUID gracz) {
    UUID p = aktywne.get(gracz);
    return p == null ? null : cache.get(p);
  }

  public UUID profil(UUID gracz) { return aktywne.get(gracz); }

  public Collection<ProfilDane> online() { return cache.values(); }

  /** Migawka JSON na wątku wołającym (główny), zapis w tle. */
  public CompletableFuture<Void> zapisz(ProfilDane d) {
    String json = gson.toJson(d);
    d.brudne = false;
    UUID profil = UUID.fromString(d.profil);
    return CompletableFuture.runAsync(() -> {
      // Nieudany zapis nie może udawać sukcesu: znacznik wraca, autosave ponowi (gdy profil nadal w pamięci).
      if (!zapiszTeraz(profil, json)) {
        d.brudne = true;
        throw new java.util.concurrent.CompletionException(new IOException("zapis profilu " + profil + " nieudany"));
      }
    }, io);
  }

  private boolean zapiszTeraz(UUID profil, String json) {
    Path cel = plik(profil);
    Path tmp = cel.resolveSibling(profil + ".json.tmp");
    for (int proba = 1; ; proba++) {
    try {
      Files.writeString(tmp, json, StandardCharsets.UTF_8);
      try {
        Files.move(tmp, cel, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(tmp, cel, StandardCopyOption.REPLACE_EXISTING);
      }
      return true;
    } catch (IOException e) {
      if (proba >= 3) {
        log.log(Level.SEVERE, "Nie udało się zapisać profilu " + profil + " (3 próby)", e);
        return false;
      }
      try { Thread.sleep(250L * proba); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return false; }
    }
    }
  }

  public void zapiszBrudne() {
    for (ProfilDane d : cache.values()) if (d.brudne) zapisz(d);
  }

  /** Usunięcie postaci w MMOProfiles: plik do archiwum (nigdy nie kasujemy danych gracza). */
  public CompletableFuture<Void> archiwizuj(UUID profil) {
    cache.remove(profil);
    return CompletableFuture.runAsync(() -> {
      Path p = plik(profil);
      if (!Files.isRegularFile(p)) return;
      try {
        Files.move(p, katalog.resolve("archiwum").resolve(profil + ".usuniety-" + System.currentTimeMillis() + ".json"));
      } catch (IOException e) {
        log.log(Level.SEVERE, "Nie udało się zarchiwizować profilu " + profil, e);
      }
    }, io);
  }

  /** Odczyt dowolnego profilu (offline) — do rankingu/turnieju. Wolać poza wątkiem głównym. */
  public CompletableFuture<ProfilDane> czytajOffline(UUID profil) {
    ProfilDane d = cache.get(profil);
    if (d != null) return CompletableFuture.completedFuture(d);
    return CompletableFuture.supplyAsync(() -> {
      try {
        Path p = plik(profil);
        return Files.isRegularFile(p) ? gson.fromJson(Files.readString(p), ProfilDane.class) : null;
      } catch (Exception e) {
        return null;
      }
    }, io);
  }

  public void wykonajIO(Runnable r) { io.execute(r); }

  /** onDisable: wszystko synchronicznie, z limitem czasu. */
  public void zamknij() {
    for (ProfilDane d : cache.values()) {
      String json = gson.toJson(d);
      UUID profil = UUID.fromString(d.profil);
      io.execute(() -> zapiszTeraz(profil, json));
    }
    io.shutdown();
    try {
      if (!io.awaitTermination(10, TimeUnit.SECONDS)) log.severe("Zapis profili nie skończył się w 10 s!");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
