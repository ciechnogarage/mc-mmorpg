package pl.mcmmorpg.paskimobow;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.entity.Hitbox;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;

import java.util.UUID;

/** Wywolywac tylko wtedy, gdy plugin ModelEngine jest wlaczony. */
final class ModeleME {
  private ModeleME() {}

  static boolean jestModelowany(UUID uuid) { return ModelEngineAPI.isModeledEntity(uuid); }

  /** null = brak aktywnego modelu lub hitboxa. */
  static Double wysokosc(UUID uuid) {
    ModeledEntity encja = ModelEngineAPI.getModeledEntity(uuid);
    if (encja == null || encja.getModels().isEmpty()) return null;
    ActiveModel pierwszy = null;
    for (ActiveModel model : encja.getModels().values()) {
      if (pierwszy == null) pierwszy = model;
      if (model.isMainHitbox()) {
        Double h = wysokosc(model);
        if (h != null) return h;
      }
    }
    return pierwszy == null ? null : wysokosc(pierwszy);
  }

  private static Double wysokosc(ActiveModel model) {
    if (model.getBlueprint() == null || model.getHitboxScale() == null) return null;
    Hitbox hitbox = model.getBlueprint().getMainHitbox();
    if (hitbox == null) return null;
    double h = hitbox.getHeight() * model.getHitboxScale().y();
    return Double.isFinite(h) && h > 0 ? h : null;
  }
}
