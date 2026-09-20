package pl.mcmmorpg.charactercreator.creator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.FlagWatcher;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import pl.mcmmorpg.charactercreator.CreatorConfig;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import pl.mcmmorpg.charactercreator.model.ModelEngineBridge;

/**
 * Spawns/despawns the server-controlled preview entity that stands in for the player's
 * character while they're in the creator room. A plain, AI-disabled Zombie is used as the
 * carrier entity purely because it's a normal LivingEntity ModelEngine can bind to and hide
 * (setBaseEntityVisible(false)) — the vanilla zombie is never actually visible to anyone,
 * only the ModelEngine model attached to it is.
 *
 * Fallback: a live test with a real player showed this matters — right now NO ModelEngine
 * race blueprints exist yet (content not authored), so applyModel always fails and the
 * carrier zombie stays visible as-is: a raw zombie mob standing in for "your character",
 * which reads as broken, not as a placeholder. LibsDisguises (already in the stack) is
 * used to disguise the carrier as a Player — using the VIEWING player's own already-loaded
 * skin, so no extra Mojang lookups/failure modes — whenever the real race model isn't
 * available. Only the creating player sees it (disguiseToPlayers), matching the
 * per-viewer-only visibility the rotation packets already use.
 */
public final class PreviewDummyManager {

  private final ModelEngineBridge modelEngineBridge;

  /**
   * Przebranie zalozone na danego dummy'ego, trzymane po NASZEJ stronie.
   *
   * <p>Potrzebne, bo {@code DisguiseAPI} w tej wersji (11.0.15) nie ma
   * {@code getDisguise(Entity)} zwracajacego obiekt przebrania — sprawdzone w deskryptorach
   * prawdziwego jara, nie zalozone. A obiekt jest potrzebny, bo to na jego watcherze, a nie
   * na encji-nosicielu, musi wyladowac ekwipunek (patrz applyAppearance).
   */
  private final Map<UUID, PlayerDisguise> disguises = new HashMap<>();

  /**
   * Dummy, na ktorych siedzi PRAWDZIWY model ModelEngine.
   *
   * <p>Potrzebne, zeby {@link #applyAppearance} wiedzialo, ze nie wolno ubierac nosiciela:
   * {@code setBaseEntityVisible(false)} ustawia wylacznie flage niewidzialnosci (sprawdzone
   * w bajtkodzie: {@code EntityHandlerImpl#setForcedInvisible} wysyla sam
   * {@code ClientboundSetEntityDataPacket} z bitem 5), a vanilla renderuje ZBROJE
   * i przedmiot w rece nawet na niewidzialnej encji. Zbroja klasy zalozona na nosiciela
   * unosila sie wiec w pozie zombie NA modelu rasy — druga warstwa tego samego "syfu",
   * co niezniszczone modele.
   */
  private final Set<UUID> modeled = new HashSet<>();

  public PreviewDummyManager(ModelEngineBridge modelEngineBridge) {
    this.modelEngineBridge = modelEngineBridge;
  }

  public Entity spawn(Location at, CreatorConfig.RaceOption race, Player viewer) {
    LivingEntity entity = (LivingEntity) at.getWorld().spawnEntity(at, EntityType.ZOMBIE);
    entity.setAI(false);
    entity.setSilent(true);
    entity.setInvulnerable(true);
    entity.setPersistent(false);
    entity.setCollidable(false);
    entity.setGravity(false);
    if (entity instanceof Zombie zombie) {
      zombie.setBaby(false);
      // creator_limbo is an open-sky void world — a vanilla zombie ignites in daylight
      // otherwise. setInvulnerable(true) alone does NOT stop this (it blocks the damage
      // tick, not the ignition itself) — a live test with a real client showed the dummy
      // visibly on fire despite that.
      zombie.setShouldBurnInDay(false);
    }
    applyModelOrFallback(entity, race, viewer);
    return entity;
  }

  /**
   * Podmienia model podgladu po zmianie rasy.
   *
   * <p>Do tej pory byl to MARTWY KOD — jedyne wystapienie w calym {@code src/} bylo jego
   * wlasna definicja, wiec model ustawial sie raz, w {@code start()}, dla rasy domyslnej,
   * i wybor rasy nie zmienial sylwetki. Teraz wola go CreatorRoomListener#refreshAppearance.
   */
  public void swapModel(Entity dummy, CreatorConfig.RaceOption race, Player viewer) {
    applyModelOrFallback(dummy, race, viewer);
  }

  /**
   * Rasa z {@code model-id: none} nie ma modelu Z ZALOZENIA, wiec nie pytamy o nia
   * ModelEngine — inaczej w logu ladowalo ostrzezenie "Brak blueprintu dla model-id 'none'"
   * o czyms, co jest poprawna konfiguracja, a nie brakiem.
   */
  private void applyModelOrFallback(Entity dummy, CreatorConfig.RaceOption race, Player viewer) {
    String modelId = race != null && race.declaresModel() ? race.modelId() : "";
    if (modelEngineBridge.applyModel(dummy, modelId)) {
      modeled.add(dummy.getUniqueId());
      // Prawdziwy model rasy wygrywa z przebraniem zastepczym.
      if (DisguiseAPI.isDisguised(dummy)) {
        DisguiseAPI.undisguiseToAll(dummy);
      }
      disguises.remove(dummy.getUniqueId());
      return;
    }
    // Rasa bez blueprintu: zdejmij model POPRZEDNIEJ rasy, inaczej podglad pokazywalby
    // sylwetke, ktorej gracz wlasnie nie wybral.
    modeled.remove(dummy.getUniqueId());
    modelEngineBridge.clearModel(dummy);
    // Fallback zawsze pokazuje TO SAMO (skorke patrzacego gracza), niezaleznie od rasy,
    // wiec ponowne przebieranie przy kazdym kliknieciu to czysty koszt pakietow
    // LibsDisguises bez zadnej zmiany na ekranie.
    if (DisguiseAPI.isDisguised(dummy) && disguises.containsKey(dummy.getUniqueId())) {
      return;
    }
    PlayerDisguise disguise = new PlayerDisguise(viewer);
    disguise.setNameVisible(false);
    DisguiseAPI.disguiseToPlayers(dummy, disguise, viewer);
    disguises.put(dummy.getUniqueId(), disguise);
  }

  /**
   * Ubiera dummy'ego tak, zeby wybor BYLO WIDAC NA POSTACI — bez ModelEngine.
   *
   * <p>Powod jest zmierzony, nie estetyczny: mediana jasnosci torsu postaci wynosila 0.08
   * przy pergaminie 0.24, czyli bohater byl najciemniejszym duzym elementem wlasnego ekranu.
   * Samo doswietlanie nic nie dawalo — jasnosc encji wynika z ALBEDO skorki, a tej nie da
   * sie zmienic bez skinow/blueprintow. Jasna zbroja podnosi wartosc tonalna sylwetki
   * i jednoczesnie robi z wyboru klasy widoczna zmiane.
   *
   * <p>Podzial slotow jest celowy: KLASA bierze korpus, nogi, buty i reke, a RASA helm
   * (barwiona skora). Dzieki temu oba wybory sa widoczne naraz i nie walcza o ten sam slot.
   */
  public void applyAppearance(Entity dummy, CreatorConfig.RaceOption race,
      CreatorConfig.ClassOption clazz) {
    if (!(dummy instanceof LivingEntity living)) {
      return;
    }
    if (modeled.contains(dummy.getUniqueId())) {
      // Model rasy JEST calym wygladem. Nosiciel zostaje goly — patrz pole 'modeled'.
      clearCarrierEquipment(living);
      return;
    }
    ItemStack helmet = race == null || race.helmetColor() == null
        ? null : dyedHelmet(race.helmetColor());
    ItemStack chest = clazz == null ? null : item(clazz.gear().get("chest"));
    ItemStack legs = clazz == null ? null : item(clazz.gear().get("legs"));
    ItemStack boots = clazz == null ? null : item(clazz.gear().get("boots"));
    ItemStack hand = clazz == null ? null : item(clazz.gear().get("hand"));

    PlayerDisguise disguise = disguises.get(dummy.getUniqueId());
    if (disguise != null) {
      // EKWIPUNEK IDZIE NA PRZEBRANIE, NIE NA NOSICIELA — i to jest cala poprawka.
      //
      // Wczesniej zbroja siedziala na zombie-nosicielu, a gracz ogladal nalozone na niego
      // przebranie gracza. Klient renderowal wtedy DWIE rozne pozy naraz: cialo w pozie
      // gracza (rece opuszczone) i zbroje w pozie zombie (rece wyciagniete przed siebie),
      // co na ekranie wygladalo jak dwie postacie nalozone na siebie. Zrzut z prawdziwego
      // klienta + F3+B pokazal JEDEN hitboks, wiec to nie byly dwie encje, tylko dwie
      // warstwy jednej encji sterowane z dwoch roznych zrodel.
      FlagWatcher watcher = disguise.getWatcher();
      watcher.setHelmet(helmet);
      watcher.setChestplate(chest);
      watcher.setLeggings(legs);
      watcher.setBoots(boots);
      watcher.setItemInMainHand(hand);
      // Nosiciel musi zostac GOLY, inaczej jego wlasna warstwa zbroi dalej sie doklada.
      clearCarrierEquipment(living);
      return;
    }
    // Bez przebrania (jest model ModelEngine albo goly nosiciel) — ekwipunek na encji.
    EntityEquipment eq = living.getEquipment();
    if (eq == null) {
      return;
    }
    eq.setHelmet(helmet);
    eq.setChestplate(chest);
    eq.setLeggings(legs);
    eq.setBoots(boots);
    eq.setItemInMainHand(hand);
    zeroDropChances(eq);
  }

  private static void clearCarrierEquipment(LivingEntity living) {
    EntityEquipment eq = living.getEquipment();
    if (eq == null) {
      return;
    }
    eq.setHelmet(null);
    eq.setChestplate(null);
    eq.setLeggings(null);
    eq.setBoots(null);
    eq.setItemInMainHand(null);
    zeroDropChances(eq);
  }

  private static void zeroDropChances(EntityEquipment eq) {
    // Dummy nic nie upuszcza — to tylko wieszak na ubrania.
    eq.setHelmetDropChance(0f);
    eq.setChestplateDropChance(0f);
    eq.setLeggingsDropChance(0f);
    eq.setBootsDropChance(0f);
    eq.setItemInMainHandDropChance(0f);
  }

  private static ItemStack item(Material material) {
    return material == null ? null : new ItemStack(material);
  }

  private static ItemStack dyedHelmet(Color color) {
    ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
    if (helmet.getItemMeta() instanceof LeatherArmorMeta meta) {
      meta.setColor(color);
      helmet.setItemMeta(meta);
    }
    return helmet;
  }

  public void despawn(Entity dummy) {
    if (dummy == null) {
      return;
    }
    disguises.remove(dummy.getUniqueId());
    modeled.remove(dummy.getUniqueId());
    if (!dummy.isDead()) {
      // Model schodzi RAZEM z encja i wlasnie tu, a nie "samo sie zrobi": to clearModel
      // niszczy ActiveModel, czyli despawnuje to, co widzi klient.
      modelEngineBridge.clearModel(dummy);
      DisguiseAPI.undisguiseToAll(dummy);
      dummy.remove();
    }
  }
}
