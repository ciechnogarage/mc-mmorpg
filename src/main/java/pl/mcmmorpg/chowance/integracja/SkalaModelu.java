package pl.mcmmorpg.chowance.integracja;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

/**
 * Skala modelu ModelEngine ciała. {@code setInvisible} chowa tylko encję bazową — model MEG zostaje na
 * miejscu, więc „wciągnięcie” w pieczęć trzeba pokazać zmniejszeniem modelu (ActiveModel#setScale, ME 4.0.9).
 * Klasa ładuje typy ModelEngine dopiero przy pierwszym wywołaniu; bez pluginu metody nic nie robią.
 */
public final class SkalaModelu {
  private static Boolean jest;

  private SkalaModelu() {}

  private static boolean me() {
    if (jest == null) jest = Bukkit.getPluginManager().isPluginEnabled("ModelEngine");
    return jest;
  }

  /** true, gdy ciało ma model MEG (wtedy setInvisible go nie ukryje). */
  public static boolean maModel(Entity e) {
    return me() && Me.model(e) != null;
  }

  /** Bieżąca skala pierwszego modelu ciała (1.0, gdy brak modelu). */
  public static double skala(Entity e) {
    return me() ? Me.skala(e) : 1.0;
  }

  /** Ustawia skalę wszystkich modeli ciała (1.0 = rozmiar z blueprintu). */
  public static void ustaw(Entity e, double skala) {
    if (me()) Me.ustaw(e, skala);
  }

  /** Izoluje odwołania do klas ModelEngine. */
  private static final class Me {
    static ModeledEntity model(Entity e) {
      try {
        ModeledEntity m = com.ticxo.modelengine.api.ModelEngineAPI.getModeledEntity(e);
        return m == null || m.getModels().isEmpty() ? null : m;
      } catch (RuntimeException | LinkageError ex) {
        return null;
      }
    }

    static double skala(Entity e) {
      ModeledEntity m = model(e);
      if (m == null) return 1.0;
      for (ActiveModel am : m.getModels().values()) return am.getScale().x();
      return 1.0;
    }

    static void ustaw(Entity e, double skala) {
      ModeledEntity m = model(e);
      if (m == null) return;
      for (ActiveModel am : m.getModels().values()) {
        am.setScale(skala);
        am.setHitboxScale(Math.max(0.05, skala));
      }
    }
  }
}
