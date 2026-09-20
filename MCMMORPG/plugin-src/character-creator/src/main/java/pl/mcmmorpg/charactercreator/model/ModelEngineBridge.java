package pl.mcmmorpg.charactercreator.model;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.entity.Entity;

/**
 * Thin wrapper around ModelEngine's public API (com.ticxo.modelengine.api.*), used both
 * for the temporary preview dummy and the permanent model applied to a real player on
 * confirm (see plan: no MythicLib `disguise` skill anywhere in this design).
 *
 * <p>Signatures confirmed with `javap` directly against the installed ModelEngine-4.0.9.jar
 * (not guessed, and not just from bytecode-string inspection — an earlier pass based on
 * DisguiseCommand.class's constant pool misattributed createActiveModel/
 * setBaseEntityVisible to the wrong classes; a real javac compile against the jar caught
 * that, this version is what actually compiles).
 *
 * <p><b>Ostrzezenie o braku blueprintu leci RAZ na model-id.</b> Bez tego limitu w logu
 * produkcyjnym siedzialo 177 identycznych wpisow {@code No ModelEngine blueprint found for
 * model-id 'race_czlowiek'} — sygnal tonal we wlasnym powtorzeniu. Prawdziwym miejscem na te
 * informacje jest walidacja configu przy starcie (CreatorConfig#validate), a nie sciezka
 * wywolywana przy kazdym kliknieciu.
 */
public final class ModelEngineBridge {

  private final Logger logger;
  private final Set<String> warnedModelIds = ConcurrentHashMap.newKeySet();

  public ModelEngineBridge(Logger logger) {
    this.logger = logger;
  }

  /** Czy blueprint o tym id w ogole istnieje — dla walidacji configu przy starcie. */
  public boolean hasBlueprint(String modelId) {
    if (modelId == null || modelId.isBlank()) {
      return false;
    }
    try {
      return ModelEngineAPI.getBlueprint(modelId) != null;
    } catch (RuntimeException ex) {
      // ModelEngine jeszcze nie wczytal blueprintow (kolejnosc startu pluginow).
      return false;
    }
  }

  /**
   * Applies the named model to the given entity (dummy or real player), replacing any
   * model already applied by this bridge. Hides the entity's vanilla rendering so only
   * the ModelEngine model shows.
   *
   * @return true if applied, false if the model id is empty or has no matching blueprint.
   */
  public boolean applyModel(Entity target, String modelId) {
    if (modelId == null || modelId.isBlank()) {
      return false;
    }
    ModelBlueprint blueprint = ModelEngineAPI.getBlueprint(modelId);
    if (blueprint == null) {
      if (warnedModelIds.add(modelId)) {
        logger.warning("Brak blueprintu ModelEngine dla model-id '" + modelId
            + "' — rasa bedzie pokazywana bez modelu 3D. To ostrzezenie nie powtorzy sie "
            + "dla tego id; pelny stan sprawdzisz przez /cc reload.");
      }
      return false;
    }

    ModeledEntity modeledEntity = ModelEngineAPI.getOrCreateModeledEntity(target);
    clearExistingModels(modeledEntity);

    ActiveModel activeModel = ModelEngineAPI.createActiveModel(blueprint);
    modeledEntity.addModel(activeModel, true);
    modeledEntity.setBaseEntityVisible(false);
    return true;
  }

  /**
   * Zdejmuje model i przywraca zwykle renderowanie encji.
   *
   * <p>Bylo to martwym kodem dokladnie tak dlugo, jak martwy byl {@code swapModel} — czyli
   * dopoki wybor rasy nie zmienial modelu. Teraz jest wywolywane przy KAZDEJ zmianie rasy na
   * taka, ktora nie ma jeszcze blueprintu: bez tego dummy zostawalby w modelu poprzedniej
   * rasy i podglad pokazywalby cos innego, niz wybral gracz.
   */
  public void clearModel(Entity target) {
    ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(target);
    if (modeledEntity == null) {
      return;
    }
    clearExistingModels(modeledEntity);
    modeledEntity.setBaseEntityVisible(true);
  }

  /**
   * <b>{@code removeModel} SAMO NIE KASUJE MODELU Z EKRANU.</b> Sprawdzone w bajtkodzie
   * ModelEngine-4.0.9 ({@code ModeledEntityImpl#removeModel}): wyjmuje model z mapy, ustawia
   * mu flage {@code setRemoved(true)} i zwraca go wolajacemu — nie dotyka renderera. Dopiero
   * {@link ActiveModel#destroy()} niszczy kosci i wola {@code ModelRenderer#destroy}, czyli
   * despawnuje encje, ktore klient widzi. Sam ModelEngine robi to tak samo: jego
   * {@code ModeledEntityImpl#destroy} przechodzi po mapie i wola {@code destroy()} na kazdym
   * modelu.
   *
   * <p>Bez tego kazde klikniecie w rase dokladalo nowy model, a poprzedni zostawal na
   * ekranie — po kilku klikach na podium stalo kilka nalozonych na siebie sylwetek. Uwaga:
   * {@code addModel(model, true)} tego NIE ratuje, bo w srodku wola dokladnie ten sam,
   * nieniszczacy {@code removeModel}.
   */
  private void clearExistingModels(ModeledEntity modeledEntity) {
    for (String activeId : modeledEntity.getModels().keySet().stream().toList()) {
      modeledEntity.removeModel(activeId).ifPresent(ActiveModel::destroy);
    }
  }
}
