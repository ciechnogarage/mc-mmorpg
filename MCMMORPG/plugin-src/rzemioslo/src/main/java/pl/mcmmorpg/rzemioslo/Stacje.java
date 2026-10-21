package pl.mcmmorpg.rzemioslo;

import net.Indyuce.mmoitems.api.event.PlayerUseCraftingStationEvent;
import net.Indyuce.mmoitems.api.event.PlayerUseCraftingStationEvent.StationAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** XP rzemiosla liczy plugin (receptury stacji nie maja exp{} — bramka audytu). XP przy ODBIORZE z kolejki
 *  (anulowanie nic nie daje), z kara za nizszy tier; perk: zwrot najdrozszego skladnika przy rozpoczeciu. */
final class Stacje implements Listener {
    private final Rzemioslo plugin;

    Stacje(Rzemioslo plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void naStacje(PlayerUseCraftingStationEvent e) {
        if (e.getRecipe() == null) return;
        Dane.Rec r = plugin.dane().receptury.get(e.getRecipe().getId());
        if (r == null) return;
        Player p = e.getPlayer();
        StationAction a = e.getInteraction();
        if (a == StationAction.INTERACT_WITH_RECIPE || a == StationAction.INSTANT_RECIPE) {
            if (r.najdrozszy() != null && plugin.profesje().zwrot(p, r.profesja())) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.przedmioty().daj(p, r.najdrozszy(), 1);
                    plugin.pasek(p, plugin.kom("perk-zwrot", "{nazwa}", plugin.dane().nazwa(r.najdrozszy())));
                });
            }
        }
        if (a == StationAction.CRAFTING_QUEUE || a == StationAction.INSTANT_RECIPE) {
            double xp = plugin.profesje().daj(p, r.profesja(), r.tier(), r.xp(), null);
            if (xp > 0) plugin.pasek(p, plugin.kom("xp-rzemiosla", "{xp}", String.valueOf(Math.round(xp)),
                    "{profesja}", plugin.dane().nazwyProfesji.getOrDefault(r.profesja(), r.profesja())));
        }
    }
}
