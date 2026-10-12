package pl.mcmmorpg.mistrzostwa;

import io.lumine.mythic.lib.api.event.session.SessionUpdateEvent;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

/**
 * Kontekst LuckPerms "profil=<uuid profilu postaci>". Dzieki niemu wezly mistrzostw nadane
 * z tym kontekstem dzialaja w hasPermission tylko dla postaci, ktora je zdobyla.
 * Po zmianie postaci sygnalizujemy LuckPermsowi, ze kontekst sie zmienil — inaczej trzymalby
 * w cache stary. Zdarzenie: MythicLib SessionUpdateEvent (ProfileSession.callSessionUpdateEvent);
 * MMOCore-owy PlayerDataLoadEvent jest w 1.13.1 martwy — bajtkod nigdzie go nie tworzy.
 */
final class KontekstProfilu implements ContextCalculator<Player>, Listener {
    private final Mistrzostwa plugin;
    private final LuckPerms lp;

    KontekstProfilu(Mistrzostwa plugin, LuckPerms lp) {
        this.plugin = plugin;
        this.lp = lp;
    }

    @Override
    public void calculate(Player target, ContextConsumer consumer) {
        UUID pid = Stan.profil(target);
        if (pid != null) consumer.accept(plugin.ustawienia().kontekst, pid.toString());
    }

    @Override
    public ContextSet estimatePotentialContexts() {
        return ImmutableContextSet.empty();
    }

    private void sygnal(Player p) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) lp.getContextManager().signalContextUpdate(p);
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void naZmianeSesji(SessionUpdateEvent e) {
        Player p = e.getPlayerData().getPlayer();
        if (p != null) sygnal(p);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void naWejscie(PlayerJoinEvent e) {
        sygnal(e.getPlayer());
    }
}
