package pl.mcmmorpg.dwiebronie;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.nms.entity.HitboxEntity;
import org.bukkit.entity.Entity;

/**
 * Jedyna klasa z odwolaniami do ModelEngine API - tworzona tylko, gdy ModelEngine jest wlaczony (softdepend),
 * wiec bez ModelEngine DwieBronie laduje sie bez NoClassDefFoundError.
 *
 * <p>Pod-hitbox ME 4 (kosc z hitboxem, np. ob_ostrze Kosiarza) to osobna encja serwera (HitboxEntityImpl),
 * ktora przy trafieniu przekazuje obrazenia bazie modelu; tu: pod-hitbox -> bazowa encja Bukkit.
 */
final class MostModelEngine {
  Entity baza(Entity e) {
    HitboxEntity h = ModelEngineAPI.getInteractionTracker().getHitbox(e.getUniqueId());
    if (h == null || h.getBone() == null) return null;
    var model = h.getBone().getActiveModel();
    if (model == null || model.getModeledEntity() == null) return null;
    Object o = model.getModeledEntity().getBase().getOriginal();
    return o instanceof Entity b ? b : null;
  }
}
