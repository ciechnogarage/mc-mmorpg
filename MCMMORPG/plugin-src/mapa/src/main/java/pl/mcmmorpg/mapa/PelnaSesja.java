package pl.mcmmorpg.mapa;

import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import de.pianoman911.mapengine.api.drawing.IDrawingSpace;
import java.awt.image.BufferedImage;
import java.util.UUID;
import org.bukkit.entity.Player;

final class PelnaSesja {
  final UUID id;
  final IMapDisplay display;
  final IDrawingSpace drawing;
  final double planeX, planeY, planeZ;
  final float facingYaw;
  final int width, height;
  String world;
  double centerX, centerZ, zoom;
  PelnaMath.Punkt cursor, previousCursor;
  long lastRightTick = -100;
  long lastClickTick = -100;
  long lastSendTick = -2, lastBaseTick = -10;
  boolean dirty = true, busy;
  PelnaMath.Punkt sentCursor;
  BufferedImage base;

  PelnaSesja(Player player, IMapDisplay display, IDrawingSpace drawing,
      double planeX, double planeY, double planeZ, float facingYaw,
      int width, int height, String world, double centerX, double centerZ, double zoom) {
    this.id = player.getUniqueId();
    this.display = display;
    this.drawing = drawing;
    this.planeX = planeX;
    this.planeY = planeY;
    this.planeZ = planeZ;
    this.facingYaw = facingYaw;
    this.width = width;
    this.height = height;
    this.world = world;
    this.centerX = centerX;
    this.centerZ = centerZ;
    this.zoom = zoom;
  }
}
