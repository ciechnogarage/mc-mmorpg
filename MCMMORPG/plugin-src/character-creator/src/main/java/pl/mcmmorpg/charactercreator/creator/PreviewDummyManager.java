package pl.mcmmorpg.charactercreator.creator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.FlagWatcher;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import pl.mcmmorpg.charactercreator.CreatorConfig;
import pl.mcmmorpg.charactercreator.model.ModelEngineBridge;
import pl.mcmmorpg.charactercreator.skin.SkinService;

/**
 * Podglad postaci w kreatorze: STAJNIA nosicieli, po jednym na skorke, zywych przez cala
 * sesje. Wybrany stoi na kotwicy, reszta czeka pod sala. Zmiana rasy albo klasy to
 * TELEPORT, a nie podmiana wygladu.
 *
 * <p><b>Dlaczego tak, a nie "przebierz jednego dummy'ego".</b> Poprzednia wersja zdejmowala
 * przebranie i zakladala nowe przy kazdym kliknieciu. Na pakietach widac, co to naprawde
 * znaczy: {@code entity_destroy} podgladu, a 44 ms pozniej {@code spawn_entity type=155}
 * z nowym profilem. Klient dostaje WIEC NOWA ENCJE i musi dla niej rozwiazac skorke —
 * a dopoki tego nie zrobi, rysuje skorke DOMYSLNA wyliczona z UUID profilu. UUID jest
 * u nas wyprowadzone z identyfikatora skorki, wiec kazda para rasa x klasa ma inna skorke
 * domyslna: na ekranie wyglada to jak losowa postac migajaca przed wlasciwa.
 *
 * <p>To nie jest kwestia POBIERANIA tekstury — te sa juz na dysku klienta (licznik plikow
 * w {@code assets/skins} nie rosnie przy kliknieciach) i profile nie znikaja z jego listy
 * graczy (zaden {@code player_remove} nie leci). Rozwiazywanie skorki jest po prostu
 * asynchroniczne i dla nowej encji zaczyna sie od zera. Jedyne pewne wyjscie to NIE TWORZYC
 * nowej encji — i dokladnie to robi ta klasa.
 *
 * <p><b>Gdzie czekaja nieuzywane.</b> {@value #PARKING_DROP} blokow pod kotwica, czyli
 * w pustce pod sala: poza kadrem, bez cienia (pod nimi nie ma na czym go narysowac)
 * i w tym samym chunku, wiec caly czas w zasiegu sledzenia klienta — a to znaczy, ze klient
 * ani razu nie musi ich tworzyc od nowa. Wczesniejsza wersja ustawiala je w SZEREGU ZA
 * PODGLADEM, licząc na to, ze perspektywa je schowa; zrzut z prawdziwego klienta pokazal,
 * ze nie chowa: spod sylwetki wystawaly nogi kolejnych i cien byl wyraznie za dlugi.
 *
 * <p><b>Dlaczego to wystarcza do pobrania tekstur.</b> Rozwiazanie skorki startuje przy
 * POJAWIENIU SIE ENCJI, a nie przy jej rysowaniu — zmierzone: przy poprzedniej wersji
 * komplet 16 tekstur ladowal w cache'u klienta, mimo ze nosiciele znikali, zanim klient
 * w ogole narysowal sale (byl jeszcze na ekranie "Loading terrain"). Sam pakiet
 * {@code player_info} z profilami tego NIE daje — sprawdzone osobno, nie pobiera sie z niego nic.
 */
public final class PreviewDummyManager {

  /**
   * O ile blokow pod kotwica czekaja nieuzywani nosiciele. Musi byc mniej niz zasieg
   * sledzenia encji (domyslnie 48), inaczej klient kasowalby je i tworzyl od nowa przy
   * kazdym wjezdzie na kotwice — czyli wracaloby dokladnie to, co ta klasa usuwa.
   */
  private static final int PARKING_DROP = 20;

  /** Klucz nosiciela awaryjnego: para bez wpisu w skins.yml -> skorka patrzacego gracza. */
  private static final String VIEWER_SKIN = "";

  private final ModelEngineBridge modelEngineBridge;
  private final SkinService skinService;

  /** Stajnie po UUID gracza — jedna na sesje kreatora. */
  private final Map<UUID, Stable> stables = new HashMap<>();

  private static final class Stable {
    final Map<String, LivingEntity> bySkin = new LinkedHashMap<>();
    final Map<UUID, PlayerDisguise> disguises = new HashMap<>();
    Location anchor;
    LivingEntity active;
    /** Model ModelEngine zalozony na aktywnego nosiciela (rasy z 'model-id' innym niz none). */
    boolean modeled;
  }

  public PreviewDummyManager(ModelEngineBridge modelEngineBridge, SkinService skinService) {
    this.modelEngineBridge = modelEngineBridge;
    this.skinService = skinService;
  }

  /**
   * Stawia komplet nosicieli i wystawia na kotwice tego, ktory pasuje do startowego wyboru.
   *
   * @return nosiciel stojacy na kotwicy (to on jest "podgladem" dla reszty pluginu)
   */
  public Entity start(Location anchor, Player viewer, CreatorConfig.RaceOption race,
      CreatorConfig.ClassOption clazz) {
    despawnAll(viewer.getUniqueId());
    Stable stable = new Stable();
    stable.anchor = anchor.clone();
    stables.put(viewer.getUniqueId(), stable);

    Location parking = parkingOf(stable);
    for (String id : skinService.ids()) {
      SkinService.Skin skin = skinService.get(id);
      if (skin == null) {
        continue;
      }
      LivingEntity carrier = spawnCarrier(parking);
      disguise(stable, carrier, new PlayerDisguise(skinService.disguiseProfile(skin)), viewer);
      stable.bySkin.put(id, carrier);
    }
    // Nosiciel na wypadek pary bez skorki. Kosztuje jedna encje i zdejmuje z reszty kodu
    // cala galaz "a co, jesli skorki nie ma" — takze ona jest wtedy tylko teleportem.
    LivingEntity fallback = spawnCarrier(parking);
    disguise(stable, fallback, new PlayerDisguise(viewer), viewer);
    stable.bySkin.put(VIEWER_SKIN, fallback);

    select(viewer, race, clazz);
    return stable.active;
  }

  /**
   * Wystawia na kotwice nosiciela pasujacego do wyboru, a poprzedniego odsyla pod sale.
   *
   * @return nowy podglad albo {@code null}, gdy sesja nie ma stajni
   */
  public Entity select(Player viewer, CreatorConfig.RaceOption race,
      CreatorConfig.ClassOption clazz) {
    Stable stable = stables.get(viewer.getUniqueId());
    if (stable == null) {
      return null;
    }
    SkinService.Skin skin = race == null ? null
        : skinService.resolve(null, race.id(), clazz == null ? "" : clazz.id());
    LivingEntity wanted = stable.bySkin.get(skin == null ? VIEWER_SKIN : skin.id());
    if (wanted == null || wanted.isDead()) {
      wanted = stable.bySkin.get(VIEWER_SKIN);
    }
    if (wanted == null || wanted.isDead()) {
      return stable.active;
    }
    if (stable.active != null && !stable.active.equals(wanted)) {
      if (stable.modeled) {
        modelEngineBridge.clearModel(stable.active);
        stable.modeled = false;
      }
      clearCarrierEquipment(stable.active);
      stable.active.teleport(parkingOf(stable));
    }
    if (!wanted.equals(stable.active)) {
      // Kat przejmujemy po poprzedniku. PreviewRotationTask wymusza go co 2 ticki, ale te
      // dwa ticki widac: nosiciel wjezdzalby na kotwice bokiem i dopiero obracal sie do gracza.
      Location target = stable.anchor.clone();
      if (stable.active != null) {
        target.setYaw(stable.active.getLocation().getYaw());
      }
      wanted.teleport(target);
      wanted.setRotation(target.getYaw(), 0f);
      wanted.setBodyYaw(target.getYaw());
    }
    stable.active = wanted;
    // Model 3D rasy zaklada sie NA ZYWA encje, wiec i tutaj nic nie jest tworzone od nowa.
    // Dzis zadna rasa go nie ma (wszystkie maja 'model-id: none'), ale galaz zostaje, bo
    // config nadal pozwala go wpisac — i ma wtedy zadzialac, a nie zostac po cichu zignorowany.
    if (race != null && race.declaresModel()) {
      stable.modeled = modelEngineBridge.applyModel(wanted, race.modelId());
    }
    return wanted;
  }

  /**
   * Przestawia cala stajnie na nowa kotwice — scena ma kilka kadrow (osobna sceneria na
   * rase). Parking idzie RAZEM z kotwica: zostawiony w starym miejscu wypadlby poza zasieg
   * sledzenia klienta, a wtedy wraca problem nowej encji i migajacej skorki domyslnej.
   * Kat obrotu bierzemy z kotwicy — scena liczy go tak, zeby postac patrzyla w kamere.
   */
  public void moveAnchor(Player viewer, Location anchor) {
    Stable stable = stables.get(viewer.getUniqueId());
    if (stable == null) {
      return;
    }
    stable.anchor = anchor.clone();
    Location parking = parkingOf(stable);
    for (LivingEntity carrier : stable.bySkin.values()) {
      if (carrier == null || carrier.isDead()) {
        continue;
      }
      if (carrier.equals(stable.active)) {
        carrier.teleport(stable.anchor);
        carrier.setRotation(stable.anchor.getYaw(), 0f);
        carrier.setBodyYaw(stable.anchor.getYaw());
      } else {
        carrier.teleport(parking);
      }
    }
  }

  /** Wszyscy nosiciele sesji — do chowania przed innymi graczami i do sprzatania. */
  public List<Entity> carriers(UUID viewerId) {
    Stable stable = stables.get(viewerId);
    return stable == null ? List.of() : new ArrayList<>(stable.bySkin.values());
  }

  /**
   * Ubiera podglad tak, zeby wybor BYLO WIDAC NA POSTACI.
   *
   * <p><b>Gdy jest skorka rasy i klasy, ZBROJA SCHODZI</b> — zostaje sam przedmiot w rece.
   * Powod widac na zrzucie z klienta (output/skorki-postaci-01/003-zoom.png): zelazny
   * napiersnik zakrywal caly stroj klasy wypalony w skorce, a barwiony helm rasy zakrywal
   * glowe, czyli dokladnie te dwie rzeczy, ktore skorka ma pokazywac. Przedmiot w rece nie
   * zaslania niczego, a wybor klasy wzmacnia.
   *
   * <p>Bez skorki (para bez wpisu w skins.yml -> przebranie w skorke widza) zostaje stary
   * uklad: KLASA bierze korpus, nogi, buty i reke, RASA helm z barwionej skory. Powod byl
   * zmierzony, nie estetyczny: mediana jasnosci torsu wynosila 0.08 przy pergaminie 0.24,
   * czyli bohater byl najciemniejszym duzym elementem wlasnego ekranu, a jasnosc encji
   * wynika z ALBEDO skorki — tam, gdzie skorki nie ma, podnosi ja zbroja.
   */
  public void applyAppearance(Player viewer, CreatorConfig.RaceOption race,
      CreatorConfig.ClassOption clazz) {
    Stable stable = stables.get(viewer.getUniqueId());
    if (stable == null || stable.active == null) {
      return;
    }
    LivingEntity living = stable.active;
    if (stable.modeled) {
      // Model rasy JEST calym wygladem — nosiciel zostaje goly. setBaseEntityVisible(false)
      // ustawia wylacznie flage niewidzialnosci (sprawdzone w bajtkodzie), a vanilla rysuje
      // ZBROJE i przedmiot w rece nawet na niewidzialnej encji.
      clearCarrierEquipment(living);
      return;
    }
    boolean wearsSkin = !VIEWER_SKIN.equals(skinIdOf(stable, living));
    ItemStack helmet = wearsSkin || race == null || race.helmetColor() == null
        ? null : dyedHelmet(race.helmetColor());
    ItemStack chest = wearsSkin || clazz == null ? null : item(clazz.gear().get("chest"));
    ItemStack legs = wearsSkin || clazz == null ? null : item(clazz.gear().get("legs"));
    ItemStack boots = wearsSkin || clazz == null ? null : item(clazz.gear().get("boots"));
    ItemStack hand = clazz == null ? null : item(clazz.gear().get("hand"));

    PlayerDisguise disguise = stable.disguises.get(living.getUniqueId());
    if (disguise != null) {
      // EKWIPUNEK IDZIE NA PRZEBRANIE, NIE NA NOSICIELA. Wczesniej zbroja siedziala na
      // zombie-nosicielu, a gracz ogladal nalozone na niego przebranie gracza — klient
      // renderowal wtedy DWIE rozne pozy naraz: cialo w pozie gracza i zbroje w pozie
      // zombie, co wygladalo jak dwie postacie nalozone na siebie.
      FlagWatcher watcher = disguise.getWatcher();
      watcher.setHelmet(helmet);
      watcher.setChestplate(chest);
      watcher.setLeggings(legs);
      watcher.setBoots(boots);
      watcher.setItemInMainHand(hand);
      clearCarrierEquipment(living);
      return;
    }
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

  /** Postacie ustawione naraz (scena wyboru) — osobno od stajni kreatora. */
  private final Map<UUID, List<LivingEntity>> crowds = new HashMap<>();

  /**
   * Stawia jedna postac na stale w {@code at}: nosiciel w przebraniu skorki rasy i klasy
   * (albo skorki gracza), z bronia klasy w rece. Scena wyboru stawia tak wszystkie postacie
   * konta naraz — kazda to osobna encja, bo dwie postacie moga miec te sama skorke.
   */
  public LivingEntity spawnFigure(Player viewer, Location at, CreatorConfig.RaceOption race,
      CreatorConfig.ClassOption clazz) {
    LivingEntity carrier = spawnCarrier(at);
    carrier.setRotation(at.getYaw(), at.getPitch());
    carrier.setBodyYaw(at.getYaw());
    SkinService.Skin skin = race == null ? null
        : skinService.resolve(null, race.id(), clazz == null ? "" : clazz.id());
    PlayerDisguise disguise = skin == null ? new PlayerDisguise(viewer)
        : new PlayerDisguise(skinService.disguiseProfile(skin));
    disguise.setNameVisible(false);
    ItemStack hand = clazz == null ? null : item(clazz.gear().get("hand"));
    disguise.getWatcher().setItemInMainHand(hand);
    DisguiseAPI.disguiseToPlayers(carrier, disguise, viewer);
    crowds.computeIfAbsent(viewer.getUniqueId(), k -> new ArrayList<>()).add(carrier);
    return carrier;
  }

  /**
   * Zarys pustego miejsca: niewidzialna postac w skorce gracza ze swieceniem — klient rysuje
   * sam bialy kontur sylwetki. Scena wyboru stawia ja na wolnym miejscu przy ognisku.
   */
  public LivingEntity spawnGhost(Player viewer, Location at) {
    LivingEntity carrier = spawnCarrier(at);
    carrier.setRotation(at.getYaw(), at.getPitch());
    carrier.setBodyYaw(at.getYaw());
    PlayerDisguise disguise = new PlayerDisguise(viewer);
    disguise.setNameVisible(false);
    disguise.getWatcher().setInvisible(true);
    disguise.getWatcher().setGlowing(false);
    DisguiseAPI.disguiseToPlayers(carrier, disguise, viewer);
    ghosts.put(carrier.getUniqueId(), disguise);
    crowds.computeIfAbsent(viewer.getUniqueId(), k -> new ArrayList<>()).add(carrier);
    return carrier;
  }

  private final Map<UUID, PlayerDisguise> ghosts = new HashMap<>();

  /** Kontur swieci przez sciany — zapalamy go tylko, gdy wolne miejsce jest wybrane. */
  public void setGhostGlow(Entity ghost, boolean on) {
    PlayerDisguise disguise = ghost == null ? null : ghosts.get(ghost.getUniqueId());
    if (disguise != null) {
      disguise.getWatcher().setGlowing(on);
    }
  }

  public void despawnFigures(UUID viewerId) {
    List<LivingEntity> crowd = crowds.remove(viewerId);
    if (crowd == null) {
      return;
    }
    for (LivingEntity carrier : crowd) {
      if (carrier != null) {
        ghosts.remove(carrier.getUniqueId());
      }
      if (carrier != null && !carrier.isDead()) {
        DisguiseAPI.undisguiseToAll(carrier);
        carrier.remove();
      }
    }
  }

  /** Sprzata cala stajnie gracza. Wolane z konca sesji — takze wtedy, gdy gracza juz nie ma. */
  public void despawnAll(UUID viewerId) {
    despawnFigures(viewerId);
    Stable stable = stables.remove(viewerId);
    if (stable == null) {
      return;
    }
    for (LivingEntity carrier : stable.bySkin.values()) {
      if (carrier == null || carrier.isDead()) {
        continue;
      }
      // Model schodzi RAZEM z encja i wlasnie tu: to clearModel niszczy ActiveModel,
      // czyli despawnuje to, co widzi klient.
      modelEngineBridge.clearModel(carrier);
      DisguiseAPI.undisguiseToAll(carrier);
      carrier.remove();
    }
    stable.bySkin.clear();
    stable.disguises.clear();
    stable.active = null;
  }

  private String skinIdOf(Stable stable, LivingEntity carrier) {
    for (Map.Entry<String, LivingEntity> entry : stable.bySkin.entrySet()) {
      if (entry.getValue().equals(carrier)) {
        return entry.getKey();
      }
    }
    return VIEWER_SKIN;
  }

  private static Location parkingOf(Stable stable) {
    return stable.anchor.clone().add(0, -PARKING_DROP, 0);
  }

  private void disguise(Stable stable, LivingEntity carrier, PlayerDisguise disguise,
      Player viewer) {
    disguise.setNameVisible(false);
    // Tylko dla tworzacego gracza — reszta sali i tak ma nosicieli schowanych
    // (CreatorSessionManager#hideFromOthers).
    DisguiseAPI.disguiseToPlayers(carrier, disguise, viewer);
    stable.disguises.put(carrier.getUniqueId(), disguise);
  }

  /** Sam nosiciel, bez wygladu. */
  private static LivingEntity spawnCarrier(Location at) {
    // HUSK, nie ZOMBIE: husk z natury nie pali sie w sloncu. setShouldBurnInDay(false) nizej
    // na 1.21.11 NIE wystarcza — zmierzone w scenie na otwartym niebie: aktywny nosiciel mial
    // Fire=90 odnawiane co chwile i plonal na oczach gracza (w zadaszonej sali nie bylo tego
    // widac). Przebranie i tak zaslania wyglad, a Husk to podklasa Zombie, wiec reszta
    // kodu dziala bez zmian.
    LivingEntity entity = (LivingEntity) at.getWorld().spawnEntity(at, EntityType.HUSK);
    entity.setAI(false);
    entity.setSilent(true);
    entity.setInvulnerable(true);
    entity.setPersistent(false);
    entity.setCollidable(false);
    entity.setGravity(false);
    if (entity instanceof Zombie zombie) {
      zombie.setBaby(false);
      // creator_limbo to swiat z otwartym niebem — vanillowy zombie zapala sie w dzien,
      // a setInvulnerable(true) tego NIE blokuje (blokuje obrazenia, nie zaplon). Widziane
      // na zywym kliencie: dummy stal w ogniu mimo nietykalnosci.
      zombie.setShouldBurnInDay(false);
    }
    return entity;
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
}
