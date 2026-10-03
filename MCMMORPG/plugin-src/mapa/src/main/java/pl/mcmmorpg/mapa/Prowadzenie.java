package pl.mcmmorpg.mapa;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

final class Prowadzenie {
  record Cel(String world, int x, int z, String name) {}
  private final MapaPlugin plugin;
  private final Map<UUID, Cel> targets = new HashMap<>();
  private volatile Map<UUID, Cel> snapshot = Map.of();
  private final Map<UUID, Boolean> tracked = new HashMap<>();
  private record OstatniTekst(String kierunek, long odleglosc, int tick) {}
  private final Map<UUID, OstatniTekst> ostatnie = new HashMap<>();
  private int tick;

  Prowadzenie(MapaPlugin plugin) {
    this.plugin = plugin;
    Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(), 10L, 10L);
  }

  Cel target(Player player) { return targets.get(player.getUniqueId()); }
  Cel targetAsync(UUID player) { return snapshot.get(player); }

  void toggle(Player player, Cel target) {
    Cel old = target(player);
    if (old != null && old.world().equals(target.world())
        && old.x() == target.x() && old.z() == target.z()) {
      clear(player);
      MMOCoreMost.actionBar(player, "Prowadzenie wyłączone");
    } else {
      set(player, target);
      MMOCoreMost.actionBar(player, "Prowadzę do: " + target.name());
    }
  }

  void set(Player player, Cel target) {
    clear(player);
    targets.put(player.getUniqueId(), target);
    snapshot = Map.copyOf(targets);
    if (sameWorld(player, target)) waypoint(player, target);
    plugin.markDirty(player);
  }

  void clear(Player player) {
    ostatnie.remove(player.getUniqueId());
    Cel old = targets.remove(player.getUniqueId());
    snapshot = Map.copyOf(targets);
    if (old != null && tracked.remove(player.getUniqueId()) != null && plugin.waypointsEnabled()) {
      try { WaypointMost.remove(player, waypointId(player), old.x(), old.z()); }
      catch (RuntimeException ex) { plugin.getLogger().warning("Nie mozna usunac waypointu: " + ex.getMessage()); }
    }
    plugin.markDirty(player);
  }

  void forget(Player player) { clear(player); }

  private boolean sameWorld(Player player, Cel target) {
    return MapaData.klucz(player.getWorld().getName()).equals(MapaData.klucz(target.world()));
  }

  private UUID waypointId(Player player) {
    return UUID.nameUUIDFromBytes(("mapa:cel:" + player.getUniqueId()).getBytes(StandardCharsets.UTF_8));
  }

  private void waypoint(Player player, Cel target) {
    if (!plugin.waypointsEnabled()) return;
    try {
      boolean wasTracked = tracked.containsKey(player.getUniqueId());
      WaypointMost.send(player, waypointId(player), target.x(), target.z(), wasTracked);
      tracked.put(player.getUniqueId(), true);
    } catch (RuntimeException ex) { plugin.getLogger().warning("Nie mozna wyslac waypointu: " + ex.getMessage()); }
  }

  private void tick() {
    tick += 10;
    for (Map.Entry<UUID, Cel> entry : Map.copyOf(targets).entrySet()) {
      Player player = Bukkit.getPlayer(entry.getKey());
      if (player == null || !player.isOnline()) {
        targets.remove(entry.getKey());
        tracked.remove(entry.getKey());
        ostatnie.remove(entry.getKey());
        snapshot = Map.copyOf(targets);
        continue;
      }
      Cel target = entry.getValue();
      if (!sameWorld(player, target)) {
        if (tracked.remove(player.getUniqueId()) != null && plugin.waypointsEnabled())
          try { WaypointMost.remove(player, waypointId(player), target.x(), target.z()); }
          catch (RuntimeException ex) { plugin.getLogger().warning("Nie mozna usunac waypointu: " + ex.getMessage()); }
        continue;
      }
      Location loc = player.getLocation();
      double dx = target.x() + 0.5 - loc.getX(), dz = target.z() + 0.5 - loc.getZ();
      double distance = Math.hypot(dx, dz);
      if (distance <= 8) {
        player.sendMessage("Jesteś na miejscu: " + target.name());
        clear(player);
        continue;
      }
      String kierunek = arrow(loc.getYaw(), dx, dz);
      long odleglosc = Math.round(distance / 5) * 5;
      OstatniTekst stary = ostatnie.get(entry.getKey());
      if (stary == null || tick - stary.tick() >= 50 || tick - stary.tick() >= 20
          && (!stary.kierunek().equals(kierunek) || stary.odleglosc() != odleglosc)) {
        MMOCoreMost.actionBar(player, "➤ " + target.name() + " · " + odleglosc + " m " + kierunek);
        ostatnie.put(entry.getKey(), new OstatniTekst(kierunek, odleglosc, tick));
      }
      waypoint(player, target);
    }
  }

  static String arrow(float yaw, double dx, double dz) {
    double bearing = Math.toDegrees(Math.atan2(-dx, dz));
    int index = Math.floorMod((int) Math.round((bearing - yaw) / 45.0), 8);
    return new String[] {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"}[index];
  }
}
