package pl.mcmmorpg.mapa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
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
    double zoom;
    long lastDraw;
    boolean drawn;
    long renderCount;
    long markersRefresh = -20;
    long markersRevision = -1;
    String markersWorld;
    List<MapaData.Marker> markers = List.of();
    // pamiec podrecznych kursorow: Minecraft wola render() CO TICK dla kazdej mapy w EKWIPUNKU (nie tylko w rece),
    // wiec tanie wyjscie i ponowne uzycie kursorow decyduja o koszcie przy wielu graczach (zmierzone sparkiem 02.10)
    MapCursorCollection cursors;
    int cursorsDir = -1;
    long cursorsKey = Long.MIN_VALUE;
    long cursorsBuilt;
    double zoomCached = -1;
    long zoomRefresh = -20;
    byte[] pixels = new byte[128 * 128];
    boolean pixelsReady;
    int cursorX;
    int cursorZ;
  }

  private final MapaPlugin plugin;
  private final Map<UUID, State> states = new HashMap<>();

  HandMapRenderer(MapaPlugin plugin) {
    super(true);
    this.plugin = plugin;
  }

  void forget(UUID player) { states.remove(player); }
  // po zmianie zoomu/celu przez gracza: przelicz od razu, nie czekaj na odswiezenie co sekunde
  void invalidate(UUID player) {
    State s = states.get(player);
    if (s != null) { s.zoomRefresh = -20; s.cursorsKey = Long.MIN_VALUE; }
  }
  void forgetAll() { states.clear(); }

  @Override
  public void render(MapView view, MapCanvas canvas, Player player) {
    Integer personal = plugin.personalMapId(player.getUniqueId());
    if (personal == null || view.getId() != personal) return;
    // mapa lezy w ekwipunku, ale nie w rece: nic nie licz (klient i tak jej nie pokazuje)
    if (!plugin.trzymaMape(player)) return;
    State state = states.computeIfAbsent(player.getUniqueId(), id -> new State());
    state.renderCount++;
    String world = player.getWorld().getName();
    if (state.zoomCached < 0 || state.renderCount - state.zoomRefresh >= 20 || !world.equals(state.world)) {
      state.zoomCached = plugin.handZoom(player);
      state.zoomRefresh = state.renderCount;
    }
    double zoom = state.zoomCached;
    Location location = player.getLocation();
    int x = location.getBlockX();
    int z = location.getBlockZ();
    MapaData.Terrain terrain = plugin.data().terrain(world);
    boolean worldChanged = !state.drawn || !world.equals(state.world);
    boolean changed = worldChanged || zoom != state.zoom || terrain != null
        && MapaMath.shouldRecenter(location.getX(), location.getZ(), state.centerX, state.centerZ, zoom);
    long now = System.currentTimeMillis();
    if (changed && (!state.drawn || now - state.lastDraw >= plugin.refreshMillis())) {
      if (terrain == null) {
        clear(canvas, state);
        canvas.drawText(39, 64, MinecraftFont.Font, "Brak mapy");
        state.pixelsReady = false;
      } else drawTerrain(canvas, state, terrain, x, z, zoom);
      state.world = world;
      state.centerX = x;
      state.centerZ = z;
      state.zoom = zoom;
      state.lastDraw = now;
      state.drawn = true;
    }

    long revision = plugin.markerRevision();
    Prowadzenie.Cel target = plugin.navigation().target(player);
    int dir = MapaMath.cursorDirection(location.getYaw());
    int cursorX = MapaMath.clamp(MapaMath.cursorCoordinate(location.getX(), state.centerX, state.zoom), -128, 127);
    int cursorZ = MapaMath.clamp(MapaMath.cursorCoordinate(location.getZ(), state.centerZ, state.zoom), -128, 127);
    long key = ((long) state.centerX * 31 + state.centerZ) * 31 + Double.hashCode(state.zoom)
        + revision * 1_000_003L + (target == null ? 0 : target.hashCode()) + world.hashCode() * 7L;
    if (state.cursors != null && key == state.cursorsKey && dir == state.cursorsDir
        && cursorX == state.cursorX && cursorZ == state.cursorZ
        && state.renderCount - state.cursorsBuilt < 20) {
      canvas.setCursors(state.cursors);
      return;
    }
    MapCursorCollection cursors = new MapCursorCollection();
    if (!world.equals(state.markersWorld) || revision != state.markersRevision
        || state.renderCount - state.markersRefresh >= 20) {
      state.markers = plugin.znacznikiDla(player, world);
      state.markersWorld = world;
      state.markersRevision = plugin.markerRevision();
      state.markersRefresh = state.renderCount;
    }
    if (state.drawn && world.equals(state.world)) for (MapaData.Marker marker : state.markers) {
      if (!MapaMath.cursorInRange(marker.x(), state.centerX, state.zoom)
          || !MapaMath.cursorInRange(marker.z(), state.centerZ, state.zoom)) continue;
      cursors.addCursor(new MapCursor((byte) MapaMath.cursorCoordinate(marker.x(), state.centerX, state.zoom),
          (byte) MapaMath.cursorCoordinate(marker.z(), state.centerZ, state.zoom), (byte) 0,
          cursorType(marker), true, Component.text(marker.name())));
    }
    if (target != null && state.drawn && MapaData.klucz(world).equals(MapaData.klucz(target.world()))
        && MapaMath.cursorInRange(target.x(), state.centerX, state.zoom)
        && MapaMath.cursorInRange(target.z(), state.centerZ, state.zoom))
      cursors.addCursor(new MapCursor((byte) MapaMath.cursorCoordinate(target.x(), state.centerX, state.zoom),
          (byte) MapaMath.cursorCoordinate(target.z(), state.centerZ, state.zoom), (byte) 0,
          MapCursor.Type.RED_X, true, Component.text(target.name())));
    // gracz na koncu = rysowany na wierzchu znacznikow
    cursors.addCursor(new MapCursor((byte) cursorX, (byte) cursorZ,
        (byte) dir, MapCursor.Type.PLAYER, true));
    canvas.setCursors(cursors);
    state.cursors = cursors; state.cursorsKey = key; state.cursorsDir = dir; state.cursorsBuilt = state.renderCount;
    state.cursorX = cursorX; state.cursorZ = cursorZ;
  }

  @SuppressWarnings("deprecation")
  private void clear(MapCanvas canvas, State state) {
    for (int py = 0; py < 128; py++) for (int px = 0; px < 128; px++) {
      int offset = py * 128 + px;
      if (!state.pixelsReady || state.pixels[offset] != 0) canvas.setPixel(px, py, (byte) 0);
      state.pixels[offset] = 0;
    }
    state.pixelsReady = true;
  }

  @SuppressWarnings("deprecation")
  private void drawTerrain(MapCanvas canvas, State state, MapaData.Terrain terrain, int x, int z, double zoom) {
    for (int py = 0; py < 128; py++) for (int px = 0; px < 128; px++) {
      int offset = py * 128 + px;
      byte color = terrain.pixel(x, z, zoom, px, py);
      if (!state.pixelsReady || state.pixels[offset] != color) canvas.setPixel(px, py, color);
      state.pixels[offset] = color;
    }
    state.pixelsReady = true;
  }

  private MapCursor.Type cursorType(MapaData.Marker marker) {
    if (marker.type().equals("wlasny")) return switch (marker.icon()) {
      case "pin_czerwona" -> MapCursor.Type.BANNER_RED;
      case "pin_niebieska" -> MapCursor.Type.BANNER_BLUE;
      case "pin_zielona" -> MapCursor.Type.BANNER_LIME;
      case "pin_zolta" -> MapCursor.Type.BANNER_YELLOW;
      case "pin_fioletowa" -> MapCursor.Type.BANNER_PURPLE;
      default -> MapCursor.Type.BANNER_WHITE;
    };
    return switch (marker.type()) {
      case "start" -> MapCursor.Type.BANNER_WHITE;
      case "legendarny" -> MapCursor.Type.BANNER_RED;
      case "miasto" -> MapCursor.Type.BANNER_YELLOW;
      case "loch" -> MapCursor.Type.BANNER_PURPLE;
      default -> MapCursor.Type.BANNER_LIME;
    };
  }
}
