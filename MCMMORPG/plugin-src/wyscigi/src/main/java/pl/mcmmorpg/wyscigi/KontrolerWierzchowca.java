package pl.mcmmorpg.wyscigi;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.type.Mount;
import com.ticxo.modelengine.api.mount.controller.MountControllerType;
import com.ticxo.modelengine.api.mount.controller.impl.AbstractMountController;
import com.ticxo.modelengine.api.nms.entity.wrapper.LookController;
import com.ticxo.modelengine.api.nms.entity.wrapper.MoveController;
import org.bukkit.entity.Entity;

/**
 * Kontroler ModelEngine dla wyścigu. Sam nie zsiada na Shift (Shift = poślizg), fizykę liczy Kierowca.
 * ModelEngine tworzy kontroler przy każdym dosiadzie, więc kierowcę szukamy po jeźdźcu.
 */
final class KontrolerWierzchowca extends AbstractMountController {
  static MountControllerType TYP;
  private static WyscigiPlugin plugin;

  static void zarejestruj(WyscigiPlugin p) {
    plugin = p;
    TYP = new MountControllerType(KontrolerWierzchowca::new);
    com.ticxo.modelengine.api.ModelEngineAPI.getMountControllerTypeRegistry().register("wyscigi_wierzchowiec", TYP);
  }

  KontrolerWierzchowca(Entity jezdziec, Mount mount) {
    super(jezdziec, mount);
    setCanDamageMount(false);
    setCanInteractMount(false);
  }

  private Kierowca kierowca() { return plugin == null ? null : plugin.kierowca(entity.getUniqueId()); }

  @Override public void updateDriverMovement(MoveController mc, ActiveModel model) {
    Kierowca k = kierowca();
    if (k == null) { mc.setVelocity(0, mc.getVelocity().getY(), 0); return; }
    k.krok(mc, input);
  }

  @Override public void updatePassengerMovement(MoveController mc, ActiveModel model) {}

  @Override public void updateDirection(LookController look, ActiveModel model) {
    Kierowca k = kierowca();
    if (k == null) { super.updateDirection(look, model); return; }
    look.setBodyYaw(k.kierunek);
    look.setHeadYaw(k.kierunek);
    look.setPitch(0);
  }
}
