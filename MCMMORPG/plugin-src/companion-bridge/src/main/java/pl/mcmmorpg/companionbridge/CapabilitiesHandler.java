package pl.mcmmorpg.companionbridge;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.plugin.java.JavaPlugin;

public final class CapabilitiesHandler implements HttpHandler {
  private final BridgeConfig config;
  private final MainThreadReader reader;
  private final CapabilitiesService capabilities;
  private final JavaPlugin plugin;

  public CapabilitiesHandler(BridgeConfig config, MainThreadReader reader,
      CapabilitiesService capabilities, JavaPlugin plugin) {
    this.config = config;
    this.reader = reader;
    this.capabilities = capabilities;
    this.plugin = plugin;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    if (!HttpSupport.isAuthorized(exchange, config.token)) {
      HttpSupport.writeError(exchange, 401, "unauthorized");
      return;
    }
    try {
      Map<String, Object> integrations = reader.readSync(() -> {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("vault", Map.of("available", capabilities.vaultPresent()));
        m.put("mmocore", Map.of("available", capabilities.mmocorePresent()));
        m.put("mmoprofiles", Map.of("available", capabilities.mmoprofilesPresent()));
        m.put("spark", Map.of("available", capabilities.sparkPresent()));
        return m;
      }, 2000);

      Map<String, Object> body = new LinkedHashMap<>();
      body.put("contractVersion", 1);
      body.put("pluginVersion", plugin.getPluginMeta().getVersion());
      body.put("integrations", integrations);
      HttpSupport.writeJson(exchange, 200, body);
    } catch (Exception ex) {
      HttpSupport.writeError(exchange, 504, "main-thread read timeout: " + ex.getMessage());
    }
  }
}
