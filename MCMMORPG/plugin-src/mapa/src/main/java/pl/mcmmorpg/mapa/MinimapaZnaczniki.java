package pl.mcmmorpg.mapa;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import su.nezushin.nminimap.api.events.AsyncMarkerRenderEvent;
import su.nezushin.nminimap.markers.impl.LocationMarker;

final class MinimapaZnaczniki implements Listener {
  private final MapaPlugin plugin;
  private volatile int radius;

  MinimapaZnaczniki(MapaPlugin plugin) {
    this.plugin = plugin;
    reloadRadius();
  }

  void reloadRadius() {
    radius = Math.max(0, plugin.getConfig().getInt("minimapa.promien", 256));
  }

  @EventHandler
  public void onMarkers(AsyncMarkerRenderEvent event) {
    Player player = event.getPlayer().getPlayer();
    if (player == null) return;
    World world = player.getWorld();
    Location position = player.getLocation();
    double maxDistanceSquared = (double) radius * radius;
    // Zdarzenie jest asynchroniczne: odczytujemy tylko pozycje gracza i niezmienne dane mapy.
    for (MapaData.Marker marker : plugin.znacznikiDlaAsync(player.getUniqueId(), world.getName())) {
      if (marker.icon().isBlank()) continue;
      double x = marker.x() + 0.5, z = marker.z() + 0.5;
      double dx = x - position.getX(), dz = z - position.getZ();
      if (dx * dx + dz * dz > maxDistanceSquared) continue;
      event.getMarkers().add(new LocationMarker(marker.icon(),
          new Location(world, x, position.getY(), z)));
    }
    Prowadzenie.Cel target = plugin.navigation().targetAsync(player.getUniqueId());
    if (target != null && MapaData.klucz(world.getName()).equals(MapaData.klucz(target.world()))) {
      double dx = target.x() + 0.5 - position.getX(), dz = target.z() + 0.5 - position.getZ();
      if (dx * dx + dz * dz <= maxDistanceSquared)
        event.getMarkers().add(new LocationMarker("cel", new Location(world,
            target.x() + 0.5, position.getY(), target.z() + 0.5)));
    }
  }
}
