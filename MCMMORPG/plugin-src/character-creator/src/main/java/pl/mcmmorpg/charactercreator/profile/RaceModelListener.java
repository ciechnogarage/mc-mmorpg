package pl.mcmmorpg.charactercreator.profile;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Trzyma model rasy na graczu przez cale zycie sesji.
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
 */
public final class RaceModelListener implements Listener {

  private final CharacterProfileModule module;

  public RaceModelListener(CharacterProfileModule module) {
    this.module = module;
  }

  /** Kazde przeniesienie miedzy swiatami — wlasne, MMOProfiles, dungeon queue, /tp. */
  @EventHandler
  public void onWorldChange(PlayerChangedWorldEvent event) {
    module.scheduleRaceModel(event.getPlayer());
  }

  @EventHandler
  public void onRespawn(PlayerRespawnEvent event) {
    module.scheduleRaceModel(event.getPlayer());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    module.forget(event.getPlayer());
  }
}
