package pl.mcmmorpg.chowance.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Koniec walki turowej albo pojedynku. przegrany/zwyciezca mogą być null (NPC/dziki). */
public final class WalkaZakonczonaEvent extends Event {
  private static final HandlerList HANDLERS = new HandlerList();
  private final String rodzaj;
  private final Player zwyciezca;
  private final Player przegrany;
  private final String przeciwnik;
  private final String trenerId;

  public WalkaZakonczonaEvent(String rodzaj, Player zwyciezca, Player przegrany, String przeciwnik) {
    this(rodzaj, zwyciezca, przegrany, przeciwnik, null);
  }

  public WalkaZakonczonaEvent(String rodzaj, Player zwyciezca, Player przegrany, String przeciwnik, String trenerId) {
    this.rodzaj = rodzaj;
    this.zwyciezca = zwyciezca;
    this.przegrany = przegrany;
    this.przeciwnik = przeciwnik;
    this.trenerId = trenerId;
  }

  public String getRodzaj() { return rodzaj; }
  public Player getZwyciezca() { return zwyciezca; }
  public Player getPrzegrany() { return przegrany; }
  public String getPrzeciwnik() { return przeciwnik; }
  public String getTrenerId() { return trenerId; }
  @Override public HandlerList getHandlers() { return HANDLERS; }
  public static HandlerList getHandlerList() { return HANDLERS; }
}
