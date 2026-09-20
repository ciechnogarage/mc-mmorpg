package pl.mcmmorpg.charactercreator.command;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
  private final Runnable onReload;

  public CreatorCommand(CreatorConfig config, CharacterStore store, CreatorSessionManager sessions,
      OrphanScanner orphanScanner, ModelEngineBridge modelEngineBridge, Runnable onReload) {
    this.config = config;
    this.store = store;
    this.sessions = sessions;
    this.orphanScanner = orphanScanner;
    this.modelEngineBridge = modelEngineBridge;
    this.onReload = onReload;
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
  }

  private void reload(CommandSender sender) {
    config.reload();
    onReload.run();
    List<CreatorConfig.Problem> problems = config.validate(modelEngineBridge);
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
    return Component.text(record.characterId() + " — " + displayName(record) + " | "
        + record.raceId() + "/" + record.classId() + " | właściciel: " + owner
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
        + " | niepełnych: " + corrupt.size(), NamedTextColor.AQUA));
    for (UUID orphan : scan.orphans()) {
      CharacterRecord record = store.get(orphan);
      sender.sendMessage(Component.text("  sierota: " + orphan + " — "
          + (record == null ? "?" : displayName(record)), NamedTextColor.GOLD));
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
      return prefixed(List.of("reload", "info", "delete", "prune", "sessions"), args[0]);
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
