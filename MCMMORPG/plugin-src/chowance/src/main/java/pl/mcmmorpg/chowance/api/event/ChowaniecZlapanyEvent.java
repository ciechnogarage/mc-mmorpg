package pl.mcmmorpg.chowance.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import pl.mcmmorpg.chowance.model.Osobnik;

/** Stwór trafił do gracza (pieczęć, oswojenie, starter, jajo, handel, admin). */
public final class ChowaniecZlapanyEvent extends PlayerEvent {
  private static final HandlerList HANDLERS = new HandlerList();
  private final Osobnik osobnik;
  private final String sposob;
  public ChowaniecZlapanyEvent(Player gracz, Osobnik osobnik, String sposob) {
    super(gracz);
    this.osobnik = osobnik;
    this.sposob = sposob;
  }

  public Osobnik getOsobnik() { return osobnik; }
  public String getSposob() { return sposob; }
  @Override public HandlerList getHandlers() { return HANDLERS; }
  public static HandlerList getHandlerList() { return HANDLERS; }
}
