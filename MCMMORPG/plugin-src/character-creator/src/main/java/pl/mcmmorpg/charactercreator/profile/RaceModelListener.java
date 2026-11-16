package pl.mcmmorpg.charactercreator.profile;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Trzyma wyglad postaci (model rasy + skorka) na graczu przez cale zycie sesji.
 *
 * <p><b>Dlaczego to w ogole musi istniec.</b> Model ModelEngine nie jest trwaly: ModeledEntity
 * przestaje byc tykany, gdy encja bazowa zglasza {@code isRemoved()}
 * ({@code ModeledEntityImpl#tick} zwraca wtedy false), a gracz zmieniajacy swiat albo
 * respawniacy sie jest przez moment dokladnie w tym stanie. Jednorazowe zalozenie modelu przy
 * wyborze profilu wystarczalo wiec tylko do pierwszego teleportu — a pierwszy teleport robi
 * sam MMOProfiles, stawiajac gracza na zapisanej pozycji profilu.
 *
 * <p>Nie ma tu zadnego tickera: model wraca po ZDARZENIU, ktore go zdjelo, a nie co sekunde
 * na wszelki wypadek.
 *
 * <p>Od 2026-09-20 lapie tez WEJSCIE na serwer — nie po to, by zalozyc model (nie ma jeszcze
 * wybranej postaci), tylko skorke tej granej ostatnio. Patrz
 * {@link CharacterProfileModule#scheduleJoinSkin}.
 */
public final class RaceModelListener implements Listener {

  private final CharacterProfileModule module;

  public RaceModelListener(CharacterProfileModule module) {
    this.module = module;
  }

  /**
   * Wejscie na serwer. Postaci jeszcze nie ma (ekran wyboru dopiero sie otworzy), wiec
   * nakladamy sama skorke ostatnio granej — zeby gracz nie ogladal sie w skorce konta.
   */
  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    module.scheduleJoinSkin(event.getPlayer());
  }

  /** Kazde przeniesienie miedzy swiatami — wlasne, MMOProfiles, dungeon queue, /tp. */
  @EventHandler
  public void onWorldChange(PlayerChangedWorldEvent event) {
    module.scheduleAppearance(event.getPlayer());
  }

  @EventHandler
  public void onRespawn(PlayerRespawnEvent event) {
    module.scheduleAppearance(event.getPlayer());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    module.forget(event.getPlayer());
  }
}
