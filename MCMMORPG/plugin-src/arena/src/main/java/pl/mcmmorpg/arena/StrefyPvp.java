package pl.mcmmorpg.arena;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import org.bukkit.Location;

/** Jedyne miejsce z API WorldGuarda: tryb `pvp-poza-ringiem: strefy` (D9=B, 21.10). */
final class StrefyPvp {
  /** true tylko gdy region WG w tym miejscu ma JAWNIE `pvp: allow` (brak flagi = zakaz). */
  boolean dozwolone(Location location) {
    if (location == null || location.getWorld() == null) return false;
    var query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
    return query.queryValue(BukkitAdapter.adapt(location), null, Flags.PVP) == StateFlag.State.ALLOW;
  }
}
