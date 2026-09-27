package pl.mcmmorpg.charactercreator.command;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.mcmmorpg.charactercreator.CreatorConfig;
import pl.mcmmorpg.charactercreator.creator.CreatorSession;
import pl.mcmmorpg.charactercreator.creator.CreatorSessionManager;
import pl.mcmmorpg.charactercreator.model.ModelEngineBridge;
import pl.mcmmorpg.charactercreator.persist.CharacterRecord;
import pl.mcmmorpg.charactercreator.persist.CharacterStore;
import pl.mcmmorpg.charactercreator.profile.CharacterProfileModule;
import pl.mcmmorpg.charactercreator.skin.SkinService;
import pl.mcmmorpg.charactercreator.persist.OrphanScanner;

/**
 * Komendy administracyjne.
 *
 * <p>Dlaczego w ogole istnieja: bez nich ZADNEGO problemu z tego planu nie dalo sie naprawic
 * bez restartu serwera. {@code CreatorConfig.reload()} nie mial ani jednego wywolujacego poza
 * konstruktorem, {@code charactercreator.admin} byl zadeklarowany i nigdzie niesprawdzany
 * (zero wywolan {@code hasPermission()} w calym pluginie), a komenda {@code /charactercreator}
 * byla atrapa — zadeklarowana w plugin.yml, bez {@code setExecutor} w kodzie.
 */
public final class CreatorCommand implements CommandExecutor, TabCompleter {

  public static final String PERMISSION = "charactercreator.admin";

  private static final DateTimeFormatter WHEN =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

  private final CreatorConfig config;
  private final CharacterStore store;
  private final CreatorSessionManager sessions;
  private final OrphanScanner orphanScanner;
  private final ModelEngineBridge modelEngineBridge;
  private final SkinService skinService;
  private final CharacterProfileModule profileModule;
  private final Runnable onReload;
  /** Etap 1 sceny: podglad kreatora w scenie dla admina. Null = scena wylaczona. */
  private java.util.function.BiFunction<Player, Boolean, String> sceneOpener;

  public CreatorCommand(CreatorConfig config, CharacterStore store, CreatorSessionManager sessions,
      OrphanScanner orphanScanner, ModelEngineBridge modelEngineBridge, SkinService skinService,
      CharacterProfileModule profileModule, Runnable onReload) {
    this.config = config;
    this.store = store;
    this.sessions = sessions;
    this.orphanScanner = orphanScanner;
    this.modelEngineBridge = modelEngineBridge;
    this.skinService = skinService;
    this.profileModule = profileModule;
    this.onReload = onReload;
  }

  /**
   * Podpina {@code /cc scena}. Funkcja dostaje gracza i flage "zamknij" i zwraca komunikat
   * dla wolajacego. Osobny setter, bo scena to opcjonalny modul z wlasnym configiem.
   */
  public void setSceneOpener(java.util.function.BiFunction<Player, Boolean, String> sceneOpener) {
    this.sceneOpener = sceneOpener;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission(PERMISSION)) {
      sender.sendMessage(Component.text("Brak uprawnień (" + PERMISSION + ").", NamedTextColor.RED));
      return true;
    }
    if (args.length == 0) {
      usage(sender, label);
      return true;
    }
    switch (args[0].toLowerCase(Locale.ROOT)) {
      case "reload" -> reload(sender);
      case "info" -> info(sender, args);
      case "delete" -> delete(sender, args);
      case "prune" -> prune(sender, args);
      case "sessions" -> listSessions(sender);
      case "skin" -> skin(sender, args);
      case "scena" -> scene(sender, args);
      default -> usage(sender, label);
    }
    return true;
  }

  private void usage(CommandSender sender, String label) {
    sender.sendMessage(Component.text("/" + label + " reload — przeladuj config.yml i sprawdz go",
        NamedTextColor.GRAY));
    sender.sendMessage(Component.text("/" + label + " info <gracz|profileId> — pokaz postacie",
        NamedTextColor.GRAY));
    sender.sendMessage(Component.text("/" + label + " delete <profileId> — usun rekord postaci",
        NamedTextColor.GRAY));
    sender.sendMessage(Component.text("/" + label + " prune [confirm] — sieroty bez profilu",
        NamedTextColor.GRAY));
    sender.sendMessage(Component.text("/" + label + " sessions — kto jest teraz w kreatorze",
        NamedTextColor.GRAY));
    sender.sendMessage(Component.text("/" + label + " skin <gracz> [id|rasa klasa|auto] "
        + "[sesja] — pokaż albo nałóż skórkę (domyślnie NA STAŁE)", NamedTextColor.GRAY));
    sender.sendMessage(Component.text("/" + label + " scena [gracz] [zamknij] — kreator w scenie "
        + "(podgląd etapu 1)", NamedTextColor.GRAY));
  }

  /** {@code /cc scena [gracz] [zamknij]} — kreator w scenie (etap 1, tylko do testow). */
  private void scene(CommandSender sender, String[] args) {
    if (sceneOpener == null) {
      sender.sendMessage(Component.text("Scena jest wylaczona (brak sekcji scene w config.yml).",
          NamedTextColor.RED));
      return;
    }
    boolean close = args.length >= 2 && "zamknij".equalsIgnoreCase(args[args.length - 1]);
    String name = args.length >= 2 && !"zamknij".equalsIgnoreCase(args[1]) ? args[1] : null;
    Player target = name != null ? Bukkit.getPlayerExact(name)
        : sender instanceof Player p ? p : null;
    if (target == null) {
      sender.sendMessage(Component.text("Podaj gracza online.", NamedTextColor.RED));
      return;
    }
    sender.sendMessage(Component.text(sceneOpener.apply(target, close), NamedTextColor.GRAY));
  }

  private void reload(CommandSender sender) {
    config.reload();
    onReload.run();
    List<CreatorConfig.Problem> problems = config.validate(modelEngineBridge, skinService);
    sender.sendMessage(Component.text("Config przeladowany: " + config.races().size()
        + " ras, " + config.classes().size() + " klas.", NamedTextColor.GREEN));
    if (problems.isEmpty()) {
      sender.sendMessage(Component.text("Bez zastrzeżeń.", NamedTextColor.GREEN));
      return;
    }
    for (CreatorConfig.Problem problem : problems) {
      sender.sendMessage(Component.text("[" + problem.severity() + "] " + problem.message(),
          switch (problem.severity()) {
            case FATAL -> NamedTextColor.RED;
            case WARNING -> NamedTextColor.GOLD;
            case INFO -> NamedTextColor.GRAY;
          }));
    }
    sender.sendMessage(Component.text("Uwaga: FATAL nie wyłącza pluginu w locie — poprawki "
        + "wymagające świata kreatora działają dopiero po restarcie.", NamedTextColor.GRAY));
  }

  private void info(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(Component.text("Podaj gracza albo id profilu.", NamedTextColor.RED));
      return;
    }
    String target = args[1];
    UUID asProfile = parseUuid(target);
    if (asProfile != null) {
      CharacterRecord record = store.get(asProfile);
      if (record == null) {
        sender.sendMessage(Component.text("Brak rekordu dla profilu " + asProfile, NamedTextColor.RED));
        return;
      }
      sender.sendMessage(describe(record));
      return;
    }

    OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(target);
    if (offline == null) {
      sender.sendMessage(Component.text("Nie znam gracza '" + target + "'.", NamedTextColor.RED));
      return;
    }
    List<CharacterRecord> owned = store.all().stream()
        .filter(r -> offline.getUniqueId().equals(r.ownerUuid()))
        .toList();
    if (owned.isEmpty()) {
      sender.sendMessage(Component.text("Gracz " + target + " nie ma zapisanych postaci "
          + "(rekordy sprzed schematu 2 nie znają właściciela — uzupełnią się przy "
          + "najbliższym wyborze profilu).", NamedTextColor.GRAY));
    }
    owned.forEach(record -> sender.sendMessage(describe(record)));

    CreatorSession session = sessions.get(offline.getUniqueId());
    if (session != null) {
      sender.sendMessage(Component.text("W KREATORZE: profil " + session.profileId()
          + ", rasa " + session.currentRaceId() + ", klasa " + session.currentClassId()
          + ", imię " + session.displayName() + (session.confirming() ? " (potwierdza)" : ""),
          NamedTextColor.AQUA));
    }
  }

  private Component describe(CharacterRecord record) {
    String owner = record.hasOwner()
        ? record.ownerName() + " (" + record.ownerUuid() + ")" : "nieznany";
    // Skorka jest w tym wierszu, bo to pierwsze pytanie przy zgloszeniu "moja postac
    // wyglada nie tak": czy rekord w ogole ma skorke i czy ta skorka jest jeszcze
    // w skins.yml.
    String skin = record.skinSource() == null || record.skinSource().isBlank()
        ? "brak"
        : record.skinSource() + (skinService.ids().contains(record.skinSource())
            ? "" : " (BRAK W skins.yml)");
    return Component.text(record.characterId() + " — " + displayName(record) + " | "
        + record.raceId() + "/" + record.classId() + " | skórka: " + skin
        + " | właściciel: " + owner
        + " | utworzona: " + WHEN.format(record.createdAt()), NamedTextColor.WHITE);
  }

  private static String displayName(CharacterRecord record) {
    return record.name() == null || record.name().isBlank() ? "<BRAK IMIENIA>" : record.name();
  }

  private void delete(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(Component.text("Podaj id profilu.", NamedTextColor.RED));
      return;
    }
    UUID profileId = parseUuid(args[1]);
    if (profileId == null) {
      sender.sendMessage(Component.text("'" + args[1] + "' to nie jest poprawne UUID.", NamedTextColor.RED));
      return;
    }
    CharacterRecord record = store.get(profileId);
    if (record == null) {
      sender.sendMessage(Component.text("Brak rekordu dla " + profileId, NamedTextColor.RED));
      return;
    }
    store.backupNow("delete");
    store.remove(profileId);
    store.saveNow();
    sender.sendMessage(Component.text("Usunięto rekord " + displayName(record) + " ("
        + profileId + "). Kopia zapasowa w plugins/CharacterCreator/backups/.",
        NamedTextColor.GREEN));
  }

  /**
   * Sieroty: rekordy bez odpowiadajacego profilu MMOProfiles. Bez {@code confirm} komenda
   * tylko RAPORTUJE — usuwanie 9 rekordow na slepo jest dokladnie tym rodzajem operacji,
   * ktora doprowadzila do tego, ze trzeba bylo je liczyc recznie.
   */
  private void prune(CommandSender sender, String[] args) {
    OrphanScanner.Scan scan = orphanScanner.scan(store.allByProfile().keySet());
    if (!scan.userdataAvailable()) {
      sender.sendMessage(Component.text("Skan niepewny (" + scan.filesFailed()
          + " nieczytelnych plików userdata) — nic nie usunięto.", NamedTextColor.RED));
      return;
    }
    List<UUID> corrupt = store.corruptRecords();
    sender.sendMessage(Component.text("Rekordów: " + store.all().size()
        + " | profili MMOProfiles: " + scan.profilesSeen()
        + " (z " + scan.filesRead() + " plików) | sierot: " + scan.orphans().size()
        + " | bez rekordu: " + scan.pending().size()
        + " | niepełnych: " + corrupt.size(), NamedTextColor.AQUA));
    for (UUID orphan : scan.orphans()) {
      CharacterRecord record = store.get(orphan);
      sender.sendMessage(Component.text("  sierota: " + orphan + " — "
          + (record == null ? "?" : displayName(record)), NamedTextColor.GOLD));
    }
    // Drugi kierunek: profile MMOProfiles bez naszego rekordu. Te NIE sa usuwane — nalezą
    // do MMOProfiles. Sa tu, bo dotad nie widzial ich nikt: skan liczyl wylacznie rekordy
    // bez profilu, a produkcja miala 0 takich i 9 odwrotnych. Kazdy z nich to profil,
    // ktorego gracz nie dokonczyl albo ktorego usuniecie zawislo.
    for (UUID pendingProfile : scan.pending()) {
      boolean inCreator = sessions.activeSessions().values().stream()
          .anyMatch(session -> pendingProfile.equals(session.profileId()));
      if (inCreator) {
        sender.sendMessage(Component.text("  w kreatorze teraz: " + pendingProfile,
            NamedTextColor.GRAY));
      } else {
        sender.sendMessage(Component.text("  profil bez rekordu: " + pendingProfile
            + " — gracz wejdzie bez rasy i klasy", NamedTextColor.RED));
      }
    }
    for (UUID broken : corrupt) {
      sender.sendMessage(Component.text("  niepełny: " + broken + " (brak pola 'name')",
          NamedTextColor.GOLD));
    }
    if (scan.orphans().isEmpty()) {
      return;
    }
    if (args.length < 2 || !"confirm".equalsIgnoreCase(args[1])) {
      sender.sendMessage(Component.text("Nic nie usunięto. Powtórz z 'confirm', żeby usunąć "
          + scan.orphans().size() + " sierot (kopia zapasowa robi się automatycznie).",
          NamedTextColor.YELLOW));
      return;
    }
    int removed = store.removeAll(scan.orphans(), "prune");
    sender.sendMessage(Component.text("Usunięto " + removed + " sierot. Kopia zapasowa w "
        + "plugins/CharacterCreator/backups/.", NamedTextColor.GREEN));
  }

  private void listSessions(CommandSender sender) {
    Map<UUID, CreatorSession> active = sessions.activeSessions();
    if (active.isEmpty()) {
      sender.sendMessage(Component.text("Nikt nie jest w kreatorze.", NamedTextColor.GRAY));
      return;
    }
    for (Map.Entry<UUID, CreatorSession> entry : active.entrySet()) {
      Player player = Bukkit.getPlayer(entry.getKey());
      CreatorSession session = entry.getValue();
      sender.sendMessage(Component.text(
          (player == null ? entry.getKey().toString() : player.getName())
          + " — profil " + session.profileId() + ", " + session.currentRaceId() + "/"
          + session.currentClassId() + ", imię " + session.displayName(), NamedTextColor.AQUA));
    }
  }

  /**
   * Skorka odgornie: podglad tego, co gracz MA, i nalozenie tego, co ma miec.
   *
   * <p><b>Po co w ogole komenda.</b> Skorki nakladal dotad wylacznie sam plugin — przy
   * wejsciu i przy wyborze postaci. Przy zgloszeniu "mam nie te skorke" nie bylo wiec zadnej
   * drogi, zeby (a) zapytac serwera, co naprawde trzyma na graczu, (b) nalozyc konkretna
   * skorke i zobaczyc, czy klient ja w ogole pokaze.
   *
   * <p><b>Nalozenie jest TRWALE.</b> Skorka idzie na profil gracza ORAZ do pola
   * {@code skin-source} w rekordzie postaci, czyli tam, skad bierze ja kazde wejscie,
   * kazdy respawn i kazda zmiana swiata. Bez tego zapisu komenda dzialala do pierwszego
   * respawnu i wracala stara skorka — co jest dokladnie tym, czego nikt nie chce
   * tlumaczyc graczowi. {@code sesja} na koncu = po staremu, tylko do nastepnego respawnu.
   *
   * <p>Trwale zapisac da sie tylko graczowi, ktory MA wybrana postac — inaczej nie ma do
   * czego przybic skorki i komenda mowi to wprost, zamiast udawac, ze zapisala.
   */
  private void skin(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(Component.text("Podaj gracza: /cc skin <gracz> [id|rasa klasa|auto] "
          + "[sesja]", NamedTextColor.RED));
      return;
    }
    Player target = Bukkit.getPlayerExact(args[1]);
    if (target == null) {
      sender.sendMessage(Component.text("Gracz '" + args[1] + "' nie jest online — skórkę "
          + "nakłada się na profil żywego gracza.", NamedTextColor.RED));
      return;
    }
    if (args.length == 2) {
      skinReport(sender, target);
      return;
    }

    List<String> rest = new ArrayList<>(Arrays.asList(args).subList(2, args.length));
    boolean sessionOnly = "sesja".equalsIgnoreCase(rest.get(rest.size() - 1));
    if (sessionOnly) {
      rest.remove(rest.size() - 1);
    }
    if (rest.isEmpty()) {
      sender.sendMessage(Component.text("Podaj skórkę: id, 'rasa klasa' albo 'auto'.",
          NamedTextColor.RED));
      return;
    }

    String wanted;
    if (rest.size() >= 2) {
      // 'rasa klasa' — tak, jak te pary nazywa config.yml; sklejanie id zostawiamy
      // SkinService, zeby nie bylo drugiego miejsca, ktore wie o podkresleniu.
      wanted = SkinService.idFor(rest.get(0).toLowerCase(Locale.ROOT),
          rest.get(1).toLowerCase(Locale.ROOT));
    } else if ("auto".equalsIgnoreCase(rest.get(0))) {
      CharacterRecord record = expectedRecord(target);
      if (record == null) {
        sender.sendMessage(Component.text("Nie wiem, jaka skórka mu się należy — nie ma "
            + "wybranej postaci ani żadnej zapisanej.", NamedTextColor.RED));
        return;
      }
      SkinService.Skin resolved =
          skinService.resolve(record.skinSource(), record.raceId(), record.classId());
      if (resolved == null) {
        sender.sendMessage(Component.text("Postać " + displayName(record) + " nie ma skórki "
            + "ani w rekordzie, ani w skins.yml.", NamedTextColor.RED));
        return;
      }
      wanted = resolved.id();
    } else {
      wanted = rest.get(0).toLowerCase(Locale.ROOT);
    }

    SkinService.Skin skin = skinService.get(wanted);
    if (skin == null) {
      sender.sendMessage(Component.text("Nie znam skórki '" + wanted + "'. Są: "
          + String.join(", ", skinService.ids()), NamedTextColor.RED));
      return;
    }
    boolean changed = skinService.applyTo(target, skin);
    sender.sendMessage(Component.text(changed
        ? "Nałożono skórkę " + skin.id() + " na gracza " + target.getName() + "."
        : target.getName() + " już miał skórkę " + skin.id() + " — nic nie zmieniono.",
        NamedTextColor.GREEN));
    persistSkin(sender, target, skin, sessionOnly);
    if (sessions.isActive(target)) {
      sender.sendMessage(Component.text("Uwaga: gracz jest teraz w kreatorze i ogląda dummy'ego "
          + "— własnej skórki tam nie zobaczy.", NamedTextColor.GOLD));
    }
  }

  /**
   * Przybija skorke do POSTACI, a nie do sesji.
   *
   * <p>Po zapisie odswiezamy tez rekord trzymany w pamieci przez modul profili: to z niego
   * korzysta przywracanie wygladu po respawnie i zmianie swiata, wiec bez tego kroku
   * najblizszy respawn cofnalby skorke do poprzedniej, mimo poprawnego pliku.
   */
  private void persistSkin(CommandSender sender, Player target, SkinService.Skin skin,
      boolean sessionOnly) {
    if (sessionOnly) {
      sender.sendMessage(Component.text("Tylko na sesję — przy respawnie albo ponownym "
          + "wejściu wróci skórka postaci.", NamedTextColor.GRAY));
      return;
    }
    CharacterRecord active = profileModule.activeCharacter(target.getUniqueId());
    if (active == null) {
      sender.sendMessage(Component.text("Gracz nie ma wybranej postaci, więc nie ma do czego "
          + "przybić skórki na stałe — została tylko na tę sesję.", NamedTextColor.GOLD));
      return;
    }
    if (skin.id().equals(active.skinSource())) {
      sender.sendMessage(Component.text("Postać " + displayName(active)
          + " już ma tę skórkę zapisaną na stałe.", NamedTextColor.GRAY));
      return;
    }
    CharacterRecord stored = store.put(active.withSkinSource(skin.id()));
    if (stored == null) {
      sender.sendMessage(Component.text("Nie udało się zapisać rekordu postaci "
          + displayName(active) + " — skórka została tylko na tę sesję.", NamedTextColor.RED));
      return;
    }
    store.saveNow();
    profileModule.trackAndApplyAppearance(target, stored);
    sender.sendMessage(Component.text("Zapisane NA STAŁE: postać " + displayName(stored)
        + " ma teraz skin-source=" + skin.id() + " (przeżyje respawn, relog i restart).",
        NamedTextColor.GREEN));
  }

  /** Co serwer TRZYMA na graczu kontra co POWINIEN — cztery wiersze i jedno rozstrzygniecie. */
  private void skinReport(CommandSender sender, Player target) {
    String current = skinService.currentTexture(target);
    String has = skinService.describeTexture(current);
    CharacterRecord record = expectedRecord(target);
    SkinService.Skin expected = record == null ? null
        : skinService.resolve(record.skinSource(), record.raceId(), record.classId());

    sender.sendMessage(Component.text(target.getName() + " — skórka", NamedTextColor.AQUA));
    sender.sendMessage(Component.text("  ma teraz: " + has, NamedTextColor.WHITE));
    if (record == null) {
      sender.sendMessage(Component.text("  powinien mieć: nie wiadomo — żadnej postaci "
          + "(ani wybranej, ani zapisanej)", NamedTextColor.GRAY));
      return;
    }
    boolean chosen = profileModule.activeCharacter(target.getUniqueId()) != null;
    sender.sendMessage(Component.text("  powinien mieć: "
        + (expected == null ? "brak skórki dla " + record.raceId() + "/" + record.classId() : expected.id())
        + " (postać " + displayName(record) + (chosen ? ", wybrana" : ", ostatnio grana — "
        + "postaci jeszcze nie wybrał") + ")", NamedTextColor.WHITE));
    if (expected == null) {
      return;
    }
    boolean match = current != null && current.equals(expected.value());
    sender.sendMessage(Component.text(match
        ? "  zgadza się: TAK — serwer trzyma na nim dokładnie tę teksturę"
        : "  zgadza się: NIE — napraw przez /cc skin " + target.getName() + " auto",
        match ? NamedTextColor.GREEN : NamedTextColor.RED));
    if (match) {
      sender.sendMessage(Component.text("  jeśli gracz i tak widzi inną skórkę, to już jego "
          + "klient (cache albo mod podmieniający skórkę lokalnego gracza).",
          NamedTextColor.GRAY));
    }
  }

  /** Postac wybrana, a gdy jeszcze nie wybral — ta ostatnio grana (to ja naklada wejscie). */
  private CharacterRecord expectedRecord(Player target) {
    CharacterRecord active = profileModule.activeCharacter(target.getUniqueId());
    return active != null ? active : store.lastUsedBy(target.getUniqueId());
  }

  private static UUID parseUuid(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (!sender.hasPermission(PERMISSION)) {
      return List.of();
    }
    if (args.length == 1) {
      return prefixed(List.of("reload", "info", "delete", "prune", "sessions", "skin", "scena"), args[0]);
    }
    if (args.length == 2 && "skin".equalsIgnoreCase(args[0])) {
      return prefixed(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
    }
    if (args.length == 3 && "skin".equalsIgnoreCase(args[0])) {
      List<String> options = new ArrayList<>(skinService.ids());
      options.add("auto");
      return prefixed(options, args[2]);
    }
    if (args.length == 4 && "skin".equalsIgnoreCase(args[0])) {
      return prefixed(List.of("sesja"), args[3]);
    }
    if (args.length == 2 && "scena".equalsIgnoreCase(args[0])) {
      List<String> options = new ArrayList<>(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
      options.add("zamknij");
      return prefixed(options, args[1]);
    }
    if (args.length == 3 && "scena".equalsIgnoreCase(args[0])) {
      return prefixed(List.of("zamknij"), args[2]);
    }
    if (args.length == 2 && "info".equalsIgnoreCase(args[0])) {
      return prefixed(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
    }
    if (args.length == 2 && "delete".equalsIgnoreCase(args[0])) {
      return prefixed(store.allByProfile().keySet().stream().map(UUID::toString).toList(), args[1]);
    }
    if (args.length == 2 && "prune".equalsIgnoreCase(args[0])) {
      return prefixed(List.of("confirm"), args[1]);
    }
    return List.of();
  }

  private static List<String> prefixed(List<String> candidates, String prefix) {
    String lower = prefix.toLowerCase(Locale.ROOT);
    List<String> out = new ArrayList<>();
    for (String candidate : candidates) {
      if (candidate.toLowerCase(Locale.ROOT).startsWith(lower)) {
        out.add(candidate);
      }
    }
    return out;
  }
}
