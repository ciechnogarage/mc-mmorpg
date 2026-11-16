package pl.mcmmorpg.charactercreator.command;

import fr.phoenixdevt.mmoprofiles.bukkit.profile.ProfileListImpl;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.mcmmorpg.charactercreator.CreatorConfig;

/**
 * {@code /cc testpostac <gracz> <rasa> <archetyp> [imie]} — wejscie bota testowego z PRAWDZIWA
 * postacia, z pominieciem klikania sceny (2026-09-29, przebudowa klas, etap 1: poligon).
 *
 * <p>Po co: bez kreatora MMOProfiles nie wczytuje postaci — statystyki klasy nie trafiaja do
 * atrybutow, a po relogu klasa wraca do domyslnej (zmierzone na klonie). Komenda robi to samo
 * co "Zatwierdz" w scenie: tworzy profil ({@code createProfile -> applyProfile}, jak okno
 * MMOProfiles), a {@code ProfileCreateEvent} idzie ta sama sciezka zatwierdzenia co ksiega
 * "Nowa postac" (rasa w LuckPerms, klasa MMOCore, rekord, wyjscie na spawn).
 *
 * <p><b>Zablokowana potrojnie</b> — decyzja wlasciciela: "zeby nikt nie exploitowal". Dziala
 * tylko, gdy WSZYSTKIE trzy warunki sa spelnione, inaczej udaje, ze jej nie ma:
 * <ol>
 *   <li>wola konsola albo RCON — gracz, nawet z OP, nigdy (OP i tak przechodzi hasPermission);</li>
 *   <li>{@code test-wejscie.enabled: true} w config.yml (domyslnie false; na world1 zawsze false);</li>
 *   <li>zmienna srodowiskowa {@code CC_TEST_WEJSCIE=1} — ustawiana tylko przy {@code docker run}
 *       klonow testowych. Kontener produkcyjny jej nie ma, wiec przypadkowe {@code true}
 *       w configu na produkcji nic nie odblokowuje.</li>
 * </ol>
 * Kazde uzycie trafia do logu jako WARN. Komendy nie ma w podpowiedziach.
 */
public final class TestEntry {

  public static final String ENV = "CC_TEST_WEJSCIE";
  public static final String CONFIG_KEY = "test-wejscie.enabled";

  /** Zlecenie czeka, az ProfileCreateEvent dla tego gracza je odbierze. */
  public record Request(String raceId, String classId, String name) {}

  private final Plugin plugin;
  private final Logger logger;
  private final CreatorConfig config;
  private final Map<UUID, Request> requests = new ConcurrentHashMap<>();

  public TestEntry(Plugin plugin, Logger logger, CreatorConfig config) {
    this.plugin = plugin;
    this.logger = logger;
    this.config = config;
  }

  static boolean envSet() {
    return "1".equals(System.getenv(ENV));
  }

  private boolean configEnabled() {
    return plugin.getConfig().getBoolean(CONFIG_KEY, false);
  }

  /** Ostrzezenie przy starcie: wlaczone w configu, ale bez zmiennej srodowiskowej = zablokowane. */
  public void warnOnStartup() {
    if (configEnabled() && !envSet()) {
      logger.warning(CONFIG_KEY + ": true w config.yml, ale brak " + ENV + "=1 w srodowisku — "
          + "/cc testpostac ZABLOKOWANE (tak ma byc na produkcji).");
    } else if (configEnabled()) {
      logger.warning("/cc testpostac WLACZONE (" + CONFIG_KEY + " + " + ENV + ") — to ma byc "
          + "klon testowy, NIGDY world1.");
    }
  }

  /** Wszystkie trzy zamki naraz. */
  public boolean allowed(CommandSender sender) {
    boolean console = sender instanceof ConsoleCommandSender
        || sender instanceof RemoteConsoleCommandSender;
    return console && configEnabled() && envSet();
  }

  /**
   * Zleca utworzenie postaci. Zwraca komunikat dla wolajacego. Wolac tylko po {@link #allowed}.
   */
  public String run(CommandSender sender, String[] args) {
    if (args.length < 4) {
      return "Uzycie: /cc testpostac <gracz> <rasa> <archetyp> [imie]";
    }
    Player player = Bukkit.getPlayerExact(args[1]);
    if (player == null) {
      return "Gracz " + args[1] + " nie jest online.";
    }
    String raceId = args[2];
    String classId = args[3];
    if (config.races().get(raceId) == null) {
      return "Nie ma rasy '" + raceId + "' (" + String.join(", ", config.races().keySet()) + ").";
    }
    if (config.classes().get(classId) == null) {
      return "Nie ma archetypu '" + classId + "' (" + String.join(", ", config.classes().keySet())
          + ").";
    }
    String name = args.length >= 5 ? args[4] : player.getName();
    ProfileListImpl impl = ProfileListImpl.getOrNull(player);
    if (impl == null) {
      return "MMOProfiles nie ma danych gracza " + player.getName() + " (jeszcze sie wczytuje?).";
    }
    logger.warning("/cc testpostac od " + sender.getName() + ": " + player.getName() + " -> "
        + raceId + " + " + classId + " (imie " + name + ")");
    requests.put(player.getUniqueId(), new Request(raceId, classId, name));
    createAndSelect(player, impl);
    return "Zlecono postac testowa dla " + player.getName() + ": " + raceId + " + " + classId + ".";
  }

  /** Odbiera zlecenie gracza (raz). Wola to modul profili w ProfileCreateEvent. */
  public Request take(UUID playerId) {
    return requests.remove(playerId);
  }

  /**
   * To samo co CharacterSelect#createNew: createProfile tylko dopisuje profil do listy, wybor
   * (a z nim ProfileSelectEvent i dane modulow) odpala dopiero applyProfile.
   */
  private void createAndSelect(Player player, ProfileListImpl list) {
    UUID playerId = player.getUniqueId();
    int n = list.getProfiles().size() + 1;
    list.createProfile("Postac " + n).thenAccept(profile -> {
      Runnable select = () -> {
        Player online = Bukkit.getPlayer(playerId);
        if (online != null && online.isOnline()) {
          list.applyProfile(profile);
        }
      };
      if (Bukkit.isPrimaryThread()) {
        select.run();
      } else {
        Bukkit.getScheduler().runTask(plugin, select);
      }
    }).exceptionally(ex -> {
      requests.remove(playerId);
      logger.warning("/cc testpostac: nie udalo sie utworzyc profilu dla " + player.getName()
          + ": " + ex);
      return null;
    });
  }
}
