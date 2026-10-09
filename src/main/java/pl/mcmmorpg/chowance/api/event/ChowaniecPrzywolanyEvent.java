package pl.mcmmorpg.chowance.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import pl.mcmmorpg.chowance.model.Osobnik;

/** Po przywołaniu ciała. */
public final class ChowaniecPrzywolanyEvent extends PlayerEvent {
  private static final HandlerList HANDLERS = new HandlerList();
  private final Osobnik osobnik;
  private final org.bukkit.entity.LivingEntity cialo;
  public ChowaniecPrzywolanyEvent(Player gracz, Osobnik osobnik, org.bukkit.entity.LivingEntity cialo) {
    super(gracz);
    this.osobnik = osobnik;
    this.cialo = cialo;
  }

  public Osobnik getOsobnik() { return osobnik; }
  public org.bukkit.entity.LivingEntity getCialo() { return cialo; }
  @Override public HandlerList getHandlers() { return HANDLERS; }
  public static HandlerList getHandlerList() { return HANDLERS; }
}
