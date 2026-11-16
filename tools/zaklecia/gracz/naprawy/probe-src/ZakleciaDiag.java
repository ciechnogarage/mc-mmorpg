package pl.mcmmorpg.zakleciadiag;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZakleciaDiag extends JavaPlugin {
  private boolean dumped;

  @Override public void onEnable() {
    Listener marker = new Listener() {};
    for (EventPriority priority : EventPriority.values()) {
      getServer().getPluginManager().registerEvent(EntityDamageByEntityEvent.class, marker, priority,
          (unused, event) -> {
            if (!(event instanceof EntityDamageByEntityEvent damage)
                || !(damage.getDamager() instanceof org.bukkit.entity.Player)
                || damage.getCause() != org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || damage.getDamage() <= 0) return;
            if (!dumped) {
              dumped = true;
              String order = Arrays.stream(EntityDamageByEntityEvent.getHandlerList().getRegisteredListeners())
                  .map(this::name).collect(Collectors.joining(" | "));
              getLogger().info("ZAKDIAG handler-list " + order);
            }
            getLogger().info("ZAKDIAG damage id=" + System.identityHashCode(damage)
                + " priority=" + priority + " canceled=" + damage.isCancelled()
                + " cause=" + damage.getCause() + " amount=" + damage.getDamage()
                + " damager=" + damage.getDamager().getType() + " victim=" + damage.getEntity().getType());
          }, this, false);
    }
  }

  private String name(RegisteredListener listener) {
    return listener.getPriority() + ":" + listener.getPlugin().getName() + ":"
        + listener.getListener().getClass().getName();
  }
}
