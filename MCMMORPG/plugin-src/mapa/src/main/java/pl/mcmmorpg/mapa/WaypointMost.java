package pl.mcmmorpg.mapa;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.color.Color;
import com.github.retrooper.packetevents.protocol.world.waypoint.TrackedWaypoint;
import com.github.retrooper.packetevents.protocol.world.waypoint.Vec3iWaypointInfo;
import com.github.retrooper.packetevents.protocol.world.waypoint.WaypointIcon;
import com.github.retrooper.packetevents.util.Either;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWaypoint;
import java.util.UUID;
import org.bukkit.entity.Player;

final class WaypointMost {
  private WaypointMost() {}

  static void send(Player player, UUID id, int x, int z, boolean tracked) {
    TrackedWaypoint waypoint = new TrackedWaypoint(Either.createLeft(id),
        new WaypointIcon(WaypointIcon.ICON_STYLE_DEFAULT, new Color(255, 0, 0)),
        new Vec3iWaypointInfo(new Vector3i(x, player.getLocation().getBlockY(), z)));
    WrapperPlayServerWaypoint.Operation op = tracked ? WrapperPlayServerWaypoint.Operation.UPDATE
        : WrapperPlayServerWaypoint.Operation.TRACK;
    PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerWaypoint(op, waypoint));
  }

  static void remove(Player player, UUID id, int x, int z) {
    TrackedWaypoint waypoint = new TrackedWaypoint(Either.createLeft(id),
        new WaypointIcon(WaypointIcon.ICON_STYLE_DEFAULT, new Color(255, 0, 0)),
        new Vec3iWaypointInfo(new Vector3i(x, player.getLocation().getBlockY(), z)));
    PacketEvents.getAPI().getPlayerManager().sendPacket(player,
        new WrapperPlayServerWaypoint(WrapperPlayServerWaypoint.Operation.UNTRACK, waypoint));
  }
}
