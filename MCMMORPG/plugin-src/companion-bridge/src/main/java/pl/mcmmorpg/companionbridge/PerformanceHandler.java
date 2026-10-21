package pl.mcmmorpg.companionbridge;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Bukkit;

public final class PerformanceHandler implements HttpHandler {
  private final BridgeConfig config;
  private final MainThreadReader reader;

  public PerformanceHandler(BridgeConfig config, MainThreadReader reader) {
    this.config = config;
    this.reader = reader;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    if (!HttpSupport.isAuthorized(exchange, config.token)) {
      HttpSupport.writeError(exchange, 401, "unauthorized");
      return;
    }
    try {
      Map<String, Object> snapshot = reader.readSync(() -> {
        double[] tps = Bukkit.getServer().getTPS();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("serverTick", Bukkit.getServer().getCurrentTick());
        m.put("tps", Map.of("last1m", tps[0], "last5m", tps[1], "last15m", tps[2]));
        m.put("averageTickTimeMs", Bukkit.getServer().getAverageTickTime());
        m.put("onlinePlayers", Bukkit.getOnlinePlayers().size());
        return m;
      }, 2000);

      Map<String, Object> body = new LinkedHashMap<>(snapshot);
      body.put("contractVersion", 1);
      body.put("capturedAt", Instant.now().toString());
      HttpSupport.writeJson(exchange, 200, body);
    } catch (Exception ex) {
      HttpSupport.writeError(exchange, 504, "main-thread read timeout: " + ex.getMessage());
    }
  }
}
