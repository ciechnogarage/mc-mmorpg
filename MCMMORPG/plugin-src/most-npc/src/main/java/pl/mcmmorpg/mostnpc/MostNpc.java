package pl.mcmmorpg.mostnpc;

import com.ticxo.modelengine.api.events.BaseEntityInteractEvent;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicMobInteractEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Most ModelEngine -> MythicMobs dla prawego kliku.
 *
 * PPM w hitbox modelu ME idzie jako BaseEntityInteractEvent; MM mostkuje z ME tylko DISMOUNTED, wiec
 * MythicMobInteractEvent (~onInteract, NpcInteractCatcher BetonQuesta) nie powstaje
 * (skill mc-mythicmobs, javap ME 4.0.9). Tu wysylamy go sami, raz na klik.
 *
 * Jeden PPM klienta to INTERACT_AT + INTERACT, czasem tez dla drugiej reki -> jeden klik na gracza w oknie 250 ms.
 */
public final class MostNpc extends JavaPlugin implements Listener {

  private static final long OKNO_MS = 250;
  private final Map<UUID, Long> ostatni = new HashMap<>();

  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(this, this);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onModelInteract(BaseEntityInteractEvent e) {
    if (e.getAction() == BaseEntityInteractEvent.Action.ATTACK) return;
    Player p = e.getPlayer();
    UUID mob = e.getBaseEntity().getUUID();
    if (Bukkit.isPrimaryThread()) przekaz(p, mob);
    else Bukkit.getScheduler().runTask(this, () -> przekaz(p, mob));
  }

  private void przekaz(Player p, UUID mob) {
    long teraz = System.currentTimeMillis();
    Long poprz = ostatni.get(p.getUniqueId());
    if (poprz != null && teraz - poprz < OKNO_MS) return;
    Optional<ActiveMob> am = MythicBukkit.inst().getMobManager().getActiveMob(mob);
    if (am.isEmpty()) return;
    ostatni.put(p.getUniqueId(), teraz);
    Bukkit.getPluginManager().callEvent(new MythicMobInteractEvent(p, am.get()));
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    ostatni.remove(e.getPlayer().getUniqueId());
  }
}
