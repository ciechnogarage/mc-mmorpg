package pl.mcmmorpg.companionbridge;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class PlayerSnapshotHandler implements HttpHandler {
  private static final Pattern PATH = Pattern.compile("^/v1/players/([0-9a-fA-F-]{36})/snapshot$");

  private final BridgeConfig config;
  private final MainThreadReader reader;
  private final VaultAdapter vaultAdapter = new VaultAdapter();
  private final MmoCoreAdapter mmoCoreAdapter = new MmoCoreAdapter();
  private final MmoProfilesAdapter mmoProfilesAdapter = new MmoProfilesAdapter();
  private final AtomicLong sequence = new AtomicLong(0);

  public PlayerSnapshotHandler(BridgeConfig config, MainThreadReader reader) {
    this.config = config;
    this.reader = reader;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    if (!HttpSupport.isAuthorized(exchange, config.token)) {
      HttpSupport.writeError(exchange, 401, "unauthorized");
      return;
    }
    Matcher matcher = PATH.matcher(exchange.getRequestURI().getPath());
    if (!matcher.matches()) {
      HttpSupport.writeError(exchange, 404, "expected /v1/players/{uuid}/snapshot");
      return;
    }
    UUID uuid;
    try {
      uuid = UUID.fromString(matcher.group(1));
    } catch (IllegalArgumentException ex) {
      HttpSupport.writeError(exchange, 400, "invalid uuid");
      return;
    }

    try {
      Map<String, Object> snapshot = reader.readSync(() -> buildSnapshot(uuid), 2000);

      Map<String, Object> body = new LinkedHashMap<>();
      body.put("contractVersion", 1);
      body.put("snapshotSequence", sequence.incrementAndGet());
      body.put("capturedAt", Instant.now().toString());
      body.put("serverTick", Bukkit.getServer().getCurrentTick());
      body.putAll(snapshot);
      HttpSupport.writeJson(exchange, 200, body);
    } catch (Exception ex) {
      HttpSupport.writeError(exchange, 504, "main-thread read timeout: " + ex.getMessage());
    }
  }

  private Map<String, Object> buildSnapshot(UUID uuid) {
    OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
    Player player = Bukkit.getPlayer(uuid);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("player", describePlayer(uuid, player));
    result.put("integrations", Map.of(
        "vault", vaultAdapter.describe(offline),
        "mmocore", mmoCoreAdapter.describe(offline, uuid),
        "mmoprofiles", mmoProfilesAdapter.describe(offline, uuid)));
    return result;
  }

  private Map<String, Object> describePlayer(UUID uuid, Player player) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("uuid", uuid.toString());
    if (player == null || !player.isOnline()) {
      data.put("name", null);
      data.put("online", false);
      data.put("world", null);
      data.put("location", null);
      data.put("health", null);
      data.put("food", null);
      data.put("gameMode", null);
      return data;
    }
    Location loc = player.getLocation();
    data.put("name", player.getName());
    data.put("online", true);
    data.put("world", player.getWorld().getName());
    data.put("location", Map.of("x", loc.getX(), "y", loc.getY(), "z", loc.getZ()));
    data.put("health", player.getHealth());
    data.put("food", player.getFoodLevel());
    data.put("gameMode", player.getGameMode().name());
    return data;
  }
}
