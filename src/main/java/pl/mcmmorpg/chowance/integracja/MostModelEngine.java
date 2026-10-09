package pl.mcmmorpg.chowance.integracja;

import com.ticxo.modelengine.api.events.BaseEntityInteractEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import pl.mcmmorpg.chowance.gra.Interakcje;
import pl.mcmmorpg.chowance.gra.Lapanie;

/**
 * Most ModelEngine → reguły PPM Chowańców. Ciało z modelem MEG ma dla klienta hitbox (encja `interaction`);
 * MEG przechwytuje pakiet interakcji i woła WŁASNE BaseEntityInteractEvent (bajtkod
 * PatchedServerGamePacketListener$2), a Bukkitowy PlayerInteractEntityEvent na encji bazowej nie dochodzi
 * (sprawdzone botem na chw-test, PROBLEMY A-014). Atak MEG przekazuje sam (obrażenia dochodzą do bazy).
 * Rejestrowane tylko przy włączonym ModelEngine — klasa nie ładuje się bez niego.
 */
public final class MostModelEngine implements Listener {
  private final Interakcje interakcje;
  private final Lapanie lapanie;

  public MostModelEngine(Interakcje interakcje, Lapanie lapanie) {
    this.interakcje = interakcje;
    this.lapanie = lapanie;
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void ppm(BaseEntityInteractEvent e) {
    Object orig = e.getBaseEntity().getOriginal();
    if (!(orig instanceof Entity ent) || !(ent instanceof LivingEntity le)) return;
    // LPM pieczęcią w dzikiego = rzut (obrażenia anuluje Lapanie.uderzeniePieczecia, odstęp rzutu chroni przed dublem)
    if (e.getAction() == BaseEntityInteractEvent.Action.ATTACK) { lapanie.pieczecNaDzikim(e.getPlayer(), le); return; }
    if (e.getSlot() != EquipmentSlot.HAND) return;
    if (!interakcje.ppm(e.getPlayer(), le)) lapanie.ppmDziki(e.getPlayer(), le);
  }
}
