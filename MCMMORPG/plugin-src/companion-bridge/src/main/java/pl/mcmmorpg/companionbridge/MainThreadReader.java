package pl.mcmmorpg.companionbridge;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * HttpServer worker thread nie moze wywolywac Bukkit/Paper API bezposrednio —
 * kazdy odczyt stanu gry musi wrocic na glowny watek serwera (patrz komentarz
 * w bots/packages/mmo-bridge/index.js sekcja "Poziom 2").
 */
public final class MainThreadReader {
  private final JavaPlugin plugin;

  public MainThreadReader(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public <T> T readSync(Supplier<T> action, long timeoutMs) throws TimeoutException {
    CompletableFuture<T> future = new CompletableFuture<>();
    plugin.getServer().getScheduler().runTask(plugin, () -> {
      try {
        future.complete(action.get());
      } catch (RuntimeException ex) {
        future.completeExceptionally(ex);
      }
    });
    try {
      return future.get(timeoutMs, TimeUnit.MILLISECONDS);
    } catch (TimeoutException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException("MainThreadReader: odczyt na glownym watku nie powiodl sie", ex);
    }
  }
}
