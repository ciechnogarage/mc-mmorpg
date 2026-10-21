package pl.mcmmorpg.companionbridge;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Read-only HTTP bridge dla bots/packages/mmo-bridge (companion-provider).
 * Bind domyslnie 127.0.0.1 — dostep z hosta wymaga jawnej publikacji portu
 * w docker/compose.yml (patrz plan, sekcja "docker-compose.yml").
 *
 * Zero zewnetrznych zaleznosci runtime poza gson (juz obecny w
 * MCMMORPG/libraries przez adventure-text-serializer-gson, wiec dostepny
 * na classpath serwera) — brak Gradle/shadow-jar w tym workspace, wiec nie
 * mozna bezpiecznie shade'owac wlasnych zaleznosci HTTP (patrz
 * character-stage/tools/BuildCharacterStage.java).
 */
public final class CompanionBridgePlugin extends JavaPlugin {
  private HttpServer server;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    BridgeConfig config = BridgeConfig.load(this);
    MainThreadReader reader = new MainThreadReader(this);
    CapabilitiesService capabilities = new CapabilitiesService();

    try {
      server = HttpServer.create(new InetSocketAddress(config.bind, config.port), 0);
      server.createContext("/v1/health", new HealthHandler(config, reader));
      server.createContext("/v1/capabilities", new CapabilitiesHandler(config, reader, capabilities, this));
      server.createContext("/v1/server/performance", new PerformanceHandler(config, reader));
      server.createContext("/v1/players/", new PlayerSnapshotHandler(config, reader));
      server.setExecutor(Executors.newFixedThreadPool(2));
      server.start();
      getLogger().info("CompanionBridge: HTTP na " + config.bind + ":" + config.port);
    } catch (Exception ex) {
      getLogger().severe("CompanionBridge: nie udalo sie uruchomic HTTP servera: " + ex.getMessage());
    }
  }

  @Override
  public void onDisable() {
    if (server != null) {
      server.stop(0);
    }
  }
}
