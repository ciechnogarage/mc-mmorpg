package pl.mcmmorpg.wyscigi;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Zapis plików YAML poza głównym wątkiem: treść (YamlConfiguration.saveToString) zrzucana jest na wątku głównym,
 * a plik zapisuje jeden wątek tła, więc kolejność zapisów jednego pliku jest zachowana. Zapis przez plik tymczasowy + przeniesienie.
 */
final class ZapisAsync {
  private final ExecutorService wykonawca;
  private final Logger log;

  ZapisAsync(String nazwaWatku, Logger log) {
    this.log = log;
    this.wykonawca = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, nazwaWatku); t.setDaemon(true); return t; });
  }

  /** Kolejkuje zapis gotowej treści; wołać po zrzucie danych na wątku głównym. */
  void zapisz(File plik, String tresc, String opis) {
    try {
      wykonawca.execute(() -> zapiszSync(plik, tresc, opis, log));
    } catch (java.util.concurrent.RejectedExecutionException e) {
      zapiszSync(plik, tresc, opis, log);   // po zamknięciu: synchronicznie
    }
  }

  /** onDisable: dokończ kolejkę i zamknij wątek. Ostatni zapis robi wołający synchronicznie (zapiszSync). */
  void zamknij() {
    wykonawca.shutdown();
    try { if (!wykonawca.awaitTermination(10, TimeUnit.SECONDS)) log.warning("Zapis w tle nie skończył się w 10 s."); }
    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
  }

  static void zapiszSync(File plik, String tresc, String opis, Logger log) {
    try {
      File katalog = plik.getParentFile();
      if (katalog != null) katalog.mkdirs();
      File tmp = new File(plik.getPath() + ".tmp");
      Files.writeString(tmp.toPath(), tresc, StandardCharsets.UTF_8);
      try {
        Files.move(tmp.toPath(), plik.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
      } catch (java.nio.file.AtomicMoveNotSupportedException e) {
        Files.move(tmp.toPath(), plik.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      log.warning("Nie zapisano " + opis + ": " + e.getMessage());
    }
  }
}
