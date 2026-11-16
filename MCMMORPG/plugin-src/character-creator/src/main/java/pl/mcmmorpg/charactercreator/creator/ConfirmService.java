package pl.mcmmorpg.charactercreator.creator;

import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.mcmmorpg.charactercreator.CreatorConfig;
import pl.mcmmorpg.charactercreator.model.ModelEngineBridge;
import pl.mcmmorpg.charactercreator.persist.CharacterRecord;
import pl.mcmmorpg.charactercreator.skin.SkinService;
import pl.mcmmorpg.charactercreator.util.ConsoleCommandRunner;

/**
 * Runs the confirm sequence: LuckPerms race permission (console) and MMOCore class select
 * (console). Model rasy zaklada osobna metoda ({@link #applyRaceModel}) — patrz jej javadoc,
 * kolejnosc wzgledem teleportu ma tu znaczenie.
 *
 * <p><b>Ta klasa juz NIE zapisuje rekordu.</b> Stara wersja robila {@code store.put()} —
 * z natychmiastowym zapisem na dysk — PRZED zatwierdzeniem profilu w MMOProfiles, wiec kazda
 * awaria po tym pierwszym kroku zostawiala rekord postaci bez odpowiadajacego mu profilu.
 * To jest dokladnie ta fabryka, ktora wyprodukowala 9 sierot na 18 rekordow. Kolejnosc
 * commitu nalezy teraz do CharacterProfileModule#onConfirm i brzmi: validate -> apply ->
 * zapis -> teleport.
 */
public final class ConfirmService {

  private final Plugin plugin;
  private final CreatorConfig config;
  private final ModelEngineBridge modelEngineBridge;
  private final SkinService skinService;
  private final Logger logger;

  public ConfirmService(Plugin plugin, CreatorConfig config,
      ModelEngineBridge modelEngineBridge, SkinService skinService, Logger logger) {
    this.plugin = plugin;
    this.config = config;
    this.modelEngineBridge = modelEngineBridge;
    this.skinService = skinService;
    this.logger = logger;
  }

  /**
   * Buduje rekord postaci z wyborow gracza. NIC nie zapisuje i nic nie stosuje.
   *
   * @return rekord albo {@code null}, gdy config nie pozwala dokonczyc tworzenia.
   */
  public CharacterRecord buildRecord(Player player, CreatorSession session, UUID profileId) {
    return buildRecord(player, session.currentRaceId(), session.currentClassId(),
        session.hasName() ? session.pendingName() : player.getName(), "", profileId);
  }

  /** To samo bez sesji starego kreatora — dla sceny (ksiega "Nowa postac"). */
  public CharacterRecord buildRecord(Player player, String raceId, String classId, String name,
      String iconColor, UUID profileId) {

    CreatorConfig.ClassOption classOption = config.classes().get(classId);
    if (classOption == null || classOption.mmocoreId().isBlank()) {
      logger.severe("Klasa '" + classId + "' nie ma 'mmocore-id' — odmowa utworzenia postaci "
          + "dla " + player.getName() + " do czasu poprawienia config.yml. "
          + "To powinno zostac wylapane przez walidacje przy starcie (/cc reload).");
      return null;
    }
    if (config.races().get(raceId) == null) {
      logger.severe("Rasa '" + raceId + "' zniknela z configu miedzy wejsciem do kreatora "
          + "a potwierdzeniem — odmowa utworzenia postaci dla " + player.getName() + ".");
      return null;
    }

    // skinSource zapisujemy RAZ, przy tworzeniu postaci, i od tej pory to ON decyduje
    // o wygladzie — patrz SkinService#resolve. Puste pole = dla tej pary rasa/klasa nie ma
    // jeszcze skorki; postac dostanie ja sama, gdy skorka pojawi sie w skins.yml.
    SkinService.Skin skin = skinService.resolve(null, raceId, classId);
    Instant now = Instant.now();
    // 'last-used' od razu, a nie dopiero przy nastepnym wyborze: swiezo utworzona postac
    // JEST ta ostatnio graną, wiec przy najblizszym wejsciu ma to byc jej skorka.
    return new CharacterRecord(profileId.toString(), name, raceId, classId,
        skin == null ? "" : skin.id(), player.getUniqueId(), player.getName(), now, now,
        iconColor == null ? "" : iconColor);
  }

  /**
   * Zaklada graczowi model 3D jego rasy. Wolac DOPIERO wtedy, gdy gracz stoi juz tam, gdzie
   * ma stac.
   *
   * <p><b>Dlaczego osobno od {@link #applyToRealPlayer}.</b> Model nie przezywa przenoszenia
   * gracza. ModeledEntity przestaje byc tykany, gdy encja bazowa zglasza {@code isRemoved()}
   * ({@code ModeledEntityImpl#tick} zwraca wtedy false), a gracz zmieniajacy swiat jest przez
   * moment dokladnie w tym stanie. Stara wersja zakladala model w samym srodku dwoch takich
   * przenosin: przy tworzeniu postaci tuz PRZED teleportem z {@code creator_limbo} na spawn,
   * a przy wyborze profilu PRZED {@code event.validate()}, po ktorym MMOProfiles stawia
   * gracza na zapisanej pozycji profilu ({@code synced-data.location: true}). Efekt: postac
   * ladowala w swiecie bez modelu rasy.
   *
   * <p>Model jest tez najpierw ZDEJMOWANY. Bez tego przelaczenie profilu dokladalo model
   * nowej rasy do modelu starej — ten sam mechanizm, co nakladajace sie sylwetki w podgladzie
   * (patrz {@code ModelEngineBridge#clearExistingModels}).
   */
  public void applyDeferredAppearance(Player player, CharacterRecord record) {
    applyRaceModel(player, record);
    applySkin(player, record);
  }

  /**
   * Naklada graczowi skorke jego postaci.
   *
   * <p>Razem z modelem, w tym samym opoznionym zadaniu, i to nie jest przypadek: jedno
   * i drugie trzeba zrobic PO tym, jak gracz stoi juz na miejscu. {@code setPlayerProfile}
   * przeladowuje encje gracza u obserwatorow, a robienie tego w trakcie teleportu miedzy
   * swiatami konczylo sie u innych pluginow znikajacym graczem.
   *
   * <p>Samo nalozenie jest idempotentne (SkinService#applyTo), wiec powtorne wywolania po
   * respawnie czy zmianie swiata nic nie kosztuja — a jednoczesnie naprawiaja skorke,
   * gdyby ktos ja nadpisal.
   */
  public void applySkin(Player player, CharacterRecord record) {
    if (!config.applySkinToPlayer()) {
      return;
    }
    SkinService.Skin skin =
        skinService.resolve(record.skinSource(), record.raceId(), record.classId());
    if (skin == null) {
      return;
    }
    if (skinService.applyTo(player, skin)) {
      logger.fine("Skorka '" + skin.id() + "' nalozona na " + player.getName() + ".");
    }
  }

  public void applyRaceModel(Player player, CharacterRecord record) {
    modelEngineBridge.clearModel(player);
    CreatorConfig.RaceOption race = config.races().get(record.raceId());
    if (race == null || !race.declaresModel()) {
      // Rasa swiadomie bez modelu 3D (model-id: none). Gracz zostaje w swojej skorce —
      // to nie jest blad, tylko stan contentu: blueprintow dla tych ras po prostu nie ma.
      return;
    }
    modelEngineBridge.applyModel(player, race.modelId());
  }

  /**
   * Called whenever a character (new or returning) becomes the active one for a player.
   *
   * <p><b>O permissionach {@code race.*}.</b> Grep po wszystkich pluginach w tym stacku
   * pokazuje, ze JEDYNYM plikiem zawierajacym np. {@code race.czlowiek} jest nasz wlasny
   * config — dzis nikt tej flagi NIE CZYTA. Zostaje mimo to, swiadomie: jest tania,
   * idempotentna i stanowi jedyny punkt zaczepienia dla przyszlych warunkow LuckPerms,
   * meta i uprawnien miedzyserwerowych. Jesli po jakims czasie nadal nikt jej nie uzywa —
   * usunac razem z sekcja {@code permission} w config.yml, a nie pojedynczo.
   */
  public void applyToRealPlayer(Player player, CharacterRecord record) {
    applyToRealPlayer(player, record, true);
  }

  /**
   * @param newCharacter true przy TWORZENIU postaci. Tylko wtedy nadajemy klase MMOCore.
   *   Przy wyborze istniejacej postaci klase trzyma MMOProfiles razem z reszta danych MMOCore
   *   profilu — wymuszanie archetypu przy kazdym wyborze cofalo klase rasowa i odpalalo
   *   mapowanie klas rasowych od nowa, a z nim zestaw startowy (zmierzone na klonie 27.09:
   *   powrot na Grokthara = "You are now a Mage/Szaman" + druga rozdzka i zbroja).
   */
  public void applyToRealPlayer(Player player, CharacterRecord record, boolean newCharacter) {
    // KAZDA z tych komend jest osobnym runTask + osobnym dispatchem konsolowym, ktory
    // parsuje komende LuckPerms i dotyka jego bazy. Wolane jest to przy KAZDYM wyborze
    // profilu, czyli przy kazdym logowaniu: przy 4 rasach bylo 6 dispatchow na gracza,
    // a przy 20 graczach wchodzacych po restarcie 120 w oknie kilku tickow. W logu widac
    // to jako trzy linie "does not have race.X set in context global" za kazdym razem.
    //
    // Pytamy lokalnie i za darmo, wiec wysylamy wylacznie komendy, ktore cos ZMIENIAJA.
    // Stan koncowy jest identyczny.
    //
    // <b>Pytaniem jest isPermissionSet, a NIE hasPermission</b> — i to nie jest kosmetyka.
    // hasPermission zwraca true dla KAZDEJ permisji, gdy gracz ma OP, wiec dla operatora
    // warunek ponizej byl zawsze falszywy i komenda nadajaca rase NIGDY nie szla. W logu
    // world1 widac to wprost: przy tworzeniu postaci przez wlasciciela serwera poszly trzy
    // 'unset' (bo hasPermission na kazda obca rase tez zwracalo true), a ani jeden 'set'.
    // Konto z OP konczylo wiec kreator bez permisji swojej rasy. isPermissionSet pyta o to,
    // co naprawde chcemy wiedziec: czy ten wezel jest graczowi PRZYPISANY.
    for (String otherId : config.races().keySet()) {
      if (!otherId.equals(record.raceId())) {
        String otherPermission = config.races().get(otherId).permission();
        if (player.isPermissionSet(otherPermission)) {
          ConsoleCommandRunner.run(plugin, "lp user %player% permission unset " + otherPermission, player);
        }
      }
    }
    CreatorConfig.RaceOption race = config.races().get(record.raceId());
    if (race != null && !player.isPermissionSet(race.permission())) {
      ConsoleCommandRunner.run(plugin, "lp user %player% permission set " + race.permission() + " true", player);
    }

    CreatorConfig.ClassOption classOption = config.classes().get(record.classId());
    if (newCharacter && classOption != null && !classOption.mmocoreId().isBlank()) {
      ConsoleCommandRunner.run(plugin, "mmocore class select %player% " + classOption.mmocoreId(), player);
    }
    // SKORKI: nie ma tu zadnej galezi i nie bedzie, dopoki nie powstanie prawdziwy wybor
    // skorki per postac. Poprzednia wersja wolala `cmi skin <gracz> <gracz> -s`, co jest
    // LOGICZNYM NO-OPEM (naklada graczowi jego wlasna skorke), przy kazdym wyborze profilu,
    // czyli przy kazdym logowaniu — na serwerze z online-mode=false i przy limicie Mojanga
    // 600 zapytan / 10 min / IP. Konfiguracyjna "abstrakcja SkinProvider" odsylala do klasy,
    // ktorej nigdy nie bylo w zrodlach.
  }
}
