package pl.mcmmorpg.zaklecia;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.RegisteredListener;

/** Tymczasowa sonda debug: stan anulowania na kazdym priorytecie i pelna kolejnosc listenerow. */
final class DiagnostykaObrazen {
  private final Zaklecia plugin;
  private boolean spisane;

  DiagnostykaObrazen(Zaklecia plugin) {
    this.plugin = plugin;
    Listener listener = new Listener() {};
    for (EventPriority priority : EventPriority.values()) {
      plugin.getServer().getPluginManager().registerEvent(EntityDamageByEntityEvent.class, listener, priority,
          (unused, event) -> {
            if (!(event instanceof EntityDamageByEntityEvent damage)
                || !(damage.getDamager() instanceof org.bukkit.entity.Player)) return;
            if (!spisane) {
              spisane = true;
              String order = Arrays.stream(EntityDamageByEntityEvent.getHandlerList().getRegisteredListeners())
                  .map(this::name).collect(Collectors.joining(" | "));
              plugin.getLogger().info("ZAKDBG damage-listeners " + order);
            }
            plugin.getLogger().info("ZAKDBG damage-priority id=" + System.identityHashCode(damage)
                + " priority=" + priority + " canceled=" + damage.isCancelled()
                + " cause=" + damage.getCause() + " amount=" + damage.getDamage()
                + " damager=" + damage.getDamager().getType() + " victim=" + damage.getEntity().getType());
          }, plugin, false);
    }
  }

  private String name(RegisteredListener listener) {
    return listener.getPriority() + ":" + listener.getPlugin().getName() + ":"
        + listener.getListener().getClass().getName();
  }
}
