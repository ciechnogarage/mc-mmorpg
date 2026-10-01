package pl.mcmmorpg.mapa;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

final class HandMapRenderer extends MapRenderer {
  private static final class State {
    String world;
    int centerX;
    int centerZ;
    int zoom;
    long lastDraw;
    boolean drawn;
  }

  private final MapaPlugin plugin;
  private final Map<UUID, State> states = new HashMap<>();

  HandMapRenderer(MapaPlugin plugin) {
    super(true);
    this.plugin = plugin;
  }

  void forget(UUID player) { states.remove(player); }
  void forgetAll() { states.clear(); }

  @Override
  public void render(MapView view, MapCanvas canvas, Player player) {
    State state = states.computeIfAbsent(player.getUniqueId(), id -> new State());
    String world = player.getWorld().getName();
    int zoom = plugin.handZoom(player);
    int x = player.getLocation().getBlockX();
    int z = player.getLocation().getBlockZ();
    MapaData.Terrain terrain = plugin.data().terrain(world);
    boolean worldChanged = !state.drawn || !world.equals(state.world);
    boolean changed = worldChanged || (terrain != null && (zoom != state.zoom
        || Math.abs((long) x - state.centerX) >= zoom || Math.abs((long) z - state.centerZ) >= zoom));
    long now = System.currentTimeMillis();
    if (changed && (!state.drawn || now - state.lastDraw >= plugin.refreshMillis())) {
      if (terrain == null) {
        clear(canvas);
        canvas.drawText(39, 64, MinecraftFont.Font, "Brak mapy");
      } else drawTerrain(canvas, terrain, x, z, zoom);
      state.world = world;
      state.centerX = x;
      state.centerZ = z;
      state.zoom = zoom;
      state.lastDraw = now;
      state.drawn = true;
    }

    MapCursorCollection cursors = new MapCursorCollection();
    if (state.drawn && world.equals(state.world)) for (MapaData.Marker marker : plugin.data().markers()) {
      if (!world.equals(marker.world()) || !MapaMath.cursorInRange(marker.x(), state.centerX, state.zoom)
          || !MapaMath.cursorInRange(marker.z(), state.centerZ, state.zoom)) continue;
      cursors.addCursor(new MapCursor((byte) MapaMath.cursorCoordinate(marker.x(), state.centerX, state.zoom),
          (byte) MapaMath.cursorCoordinate(marker.z(), state.centerZ, state.zoom), (byte) 0,
          cursorType(marker.type()), true, Component.text(marker.name())));
    }
    // gracz na koncu = rysowany na wierzchu znacznikow
    cursors.addCursor(new MapCursor((byte) 0, (byte) 0,
        (byte) MapaMath.cursorDirection(player.getLocation().getYaw()), MapCursor.Type.PLAYER, true));
    canvas.setCursors(cursors);
  }

  @SuppressWarnings("deprecation")
  private void clear(MapCanvas canvas) {
    for (int py = 0; py < 128; py++) for (int px = 0; px < 128; px++) canvas.setPixel(px, py, (byte) 0);
  }

  @SuppressWarnings("deprecation")
  private void drawTerrain(MapCanvas canvas, MapaData.Terrain terrain, int x, int z, int zoom) {
    for (int py = 0; py < 128; py++) for (int px = 0; px < 128; px++) {
      canvas.setPixel(px, py, terrain.color(x + (px - 64) * zoom, z + (py - 64) * zoom));
    }
  }

  private MapCursor.Type cursorType(String type) {
    return switch (type) {
      case "start" -> MapCursor.Type.BANNER_WHITE;
      case "legendarny" -> MapCursor.Type.BANNER_RED;
      case "miasto" -> MapCursor.Type.BANNER_YELLOW;
      case "loch" -> MapCursor.Type.BANNER_PURPLE;
      default -> MapCursor.Type.BANNER_LIME;
    };
  }
}
