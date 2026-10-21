package pl.mcmmorpg.companionbridge;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Bukkit;

public final class HealthHandler implements HttpHandler {
  private final BridgeConfig config;
  private final MainThreadReader reader;

  public HealthHandler(BridgeConfig config, MainThreadReader reader) {
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
      int tick = reader.readSync(() -> Bukkit.getServer().getCurrentTick(), 2000);
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("contractVersion", 1);
      body.put("status", "ok");
      body.put("serverTick", tick);
      body.put("capturedAt", Instant.now().toString());
      HttpSupport.writeJson(exchange, 200, body);
    } catch (Exception ex) {
      HttpSupport.writeError(exchange, 504, "main-thread read timeout: " + ex.getMessage());
    }
  }
}
