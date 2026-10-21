package pl.mcmmorpg.chowance.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import pl.mcmmorpg.chowance.model.Osobnik;

/** Po ewolucji osobnika. */
public final class ChowaniecEwolucjaEvent extends PlayerEvent {
  private static final HandlerList HANDLERS = new HandlerList();
  private final Osobnik osobnik;
  private final String zGatunku;
  public ChowaniecEwolucjaEvent(Player gracz, Osobnik osobnik, String zGatunku) {
    super(gracz);
    this.osobnik = osobnik;
    this.zGatunku = zGatunku;
  }

  public Osobnik getOsobnik() { return osobnik; }
  public String getZGatunku() { return zGatunku; }
  @Override public HandlerList getHandlers() { return HANDLERS; }
  public static HandlerList getHandlerList() { return HANDLERS; }
}
