package pl.mcmmorpg.charactercreator.skin;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Skorki postaci: jedna tekstura na pare RASA x KLASA.
 *
 * <p><b>Dlaczego nie "skorka rasy + nakladka klasy".</b> Skorka gracza to JEDEN plik PNG
 * w profilu — protokol nie ma drugiej warstwy, ktora serwer moglby podmienic osobno. Klasa
 * jest wiec wypalona w te sama teksture co rasa (tors, rece, nogi, kaptur/maska), a rasa
 * trzyma glowe i karnacje. Generator: {@code tools/gen_race_skins.py}.
 *
 * <p><b>Skad biora sie podpisy.</b> Klient Javy pobiera skorki WYLACZNIE z
 * textures.minecraft.net — wlasny hosting odpada, nawet przy online-mode=false. Dlatego PNG
 * ida na mineskin.org ({@code tools/upload_skins.sh}, wprost przez jego API), a stamtad
 * wracaja z podpisem Mojanga. Tu leza juz tylko gotowe pary {@code value} +
 * {@code signature}, zapisane w {@code skins.yml}.
 *
 * <p>Plik danych jest czytany Z DYSKU, nie z jara: dodanie rasy albo poprawka skorki to
 * podmiana {@code skins.yml} i {@code /cc reload}, bez przebudowy pluginu.
 */
public final class SkinService {

  /** Nazwa wlasnosci profilu, w ktorej Mojang trzyma skorke. */
  private static final String TEXTURES = "textures";

  /** Nick w przebraniu podgladu. Protokol tnie go na 16 znakach. */
  private static final int MAX_PROFILE_NAME = 16;

  public record Skin(String id, String value, String signature) {
  }

  private final Plugin plugin;
  private final Logger logger;
  private final Map<String, Skin> skins = new LinkedHashMap<>();

  public SkinService(Plugin plugin, Logger logger) {
    this.plugin = plugin;
    this.logger = logger;
  }

  /**
   * Konstruktor dla testow: gotowy zestaw skorek, bez pliku i bez serwera.
   *
   * <p>Kolejnosc szukania skorki ({@link #resolve}) jest jedyna regula w tej klasie, ktora
   * da sie zlamac niezauwazenie — i jednoczesnie jedyna, ktora decyduje o tym, czy postac
   * zachowa swoj wyglad po zmianie configu. Dlatego ma test, mimo ze reszta klasy wymaga
   * zywego serwera.
   */
  SkinService(Map<String, Skin> preloaded) {
    this.plugin = null;
    this.logger = Logger.getLogger(SkinService.class.getName());
    this.skins.putAll(preloaded);
  }

  /** Klucz skorki dla pary rasa+klasa — ten sam ciag trafia do rekordu postaci. */
  public static String idFor(String raceId, String classId) {
    return raceId + "_" + classId;
  }

  public void reload() {
    skins.clear();
    File file = new File(plugin.getDataFolder(), "skins.yml");
    if (!file.isFile() && plugin.getResource("skins.yml") != null) {
      // Jar wiezie komplet 4 ras x 4 klasy — pierwszy start ma dzialac bez recznej roboty.
      // Warunek na getResource jest konieczny: saveResource rzuca wyjatkiem, gdy zasobu nie ma,
      // a brak skorek ma konczyc sie ostrzezeniem, a nie wywaleniem onEnable.
      plugin.saveResource("skins.yml", false);
    }
    if (!file.isFile()) {
      logger.warning("Brak skins.yml w " + plugin.getDataFolder()
          + " — postacie zostana w skorkach kont graczy.");
      return;
    }
    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
    ConfigurationSection section = yaml.getConfigurationSection("skins");
    if (section == null) {
      logger.warning("skins.yml nie ma sekcji 'skins' — postacie zostana w skorkach kont.");
      return;
    }
    for (String id : section.getKeys(false)) {
      ConfigurationSection entry = section.getConfigurationSection(id);
      String value = entry == null ? null : entry.getString("value");
      if (value == null || value.isBlank()) {
        logger.warning("Skorka '" + id + "' w skins.yml nie ma pola 'value' — pomijam.");
        continue;
      }
      String signature = entry.getString("signature");
      skins.put(id, new Skin(id, value, signature == null || signature.isBlank() ? null : signature));
    }
    logger.info("Wczytano " + skins.size() + " skorek postaci z skins.yml.");
  }

  public Set<String> ids() {
    return Collections.unmodifiableSet(skins.keySet());
  }

  /** Skorka po samym identyfikatorze — dla preloadu, ktory nie zna ani rasy, ani klasy. */
  public Skin get(String id) {
    return skins.get(id);
  }

  public boolean isEmpty() {
    return skins.isEmpty();
  }

  /**
   * Skorka dla postaci.
   *
   * <p>Kolejnosc jest celowa: najpierw to, co ZAPISANE W REKORDZIE, a dopiero potem to, co
   * wynika z dzisiejszego configu. Dzieki temu postac nie zmienia wygladu tylko dlatego, ze
   * ktos przemapowal klasy — skorka jest czescia tozsamosci postaci, tak samo jak imie.
   *
   * @param recorded {@code skinSource} z rekordu; {@code null}/puste dla nowej postaci
   * @return skorka albo {@code null}, gdy nic nie pasuje (wtedy gracz zostaje w swojej)
   */
  public Skin resolve(String recorded, String raceId, String classId) {
    if (recorded != null && !recorded.isBlank()) {
      Skin exact = skins.get(recorded);
      if (exact != null) {
        return exact;
      }
    }
    Skin pair = skins.get(idFor(raceId, classId));
    if (pair != null) {
      return pair;
    }
    // Ostatnia deska ratunku: skorka samej rasy, jesli ktos taka dopisal do skins.yml.
    return skins.get(raceId);
  }

  /**
   * Naklada skorke na PRAWDZIWEGO gracza — widza ja wszyscy, lacznie z nim samym.
   *
   * <p>Idempotentnie, i to nie jest ozdobnik: {@code setPlayerProfile} przeladowuje encje
   * gracza u kazdego obserwatora (migniecie), a wolane jest to przy kazdej zmianie swiata
   * i respawnie — patrz RaceModelListener. Porownanie z aktualna wartoscia zamienia te
   * wywolania w nic nie kosztujacy no-op i jednoczesnie pozwala samoczynnie naprawic
   * skorke, gdyby ktos inny ja nadpisal.
   *
   * @return true, jesli skorka faktycznie zostala podmieniona
   */
  public boolean applyTo(Player player, Skin skin) {
    if (skin == null) {
      return false;
    }
    PlayerProfile profile = player.getPlayerProfile();
    for (ProfileProperty property : profile.getProperties()) {
      if (TEXTURES.equals(property.getName()) && skin.value().equals(property.getValue())) {
        return false;
      }
    }
    profile.setProperty(skin.signature() == null
        ? new ProfileProperty(TEXTURES, skin.value())
        : new ProfileProperty(TEXTURES, skin.value(), skin.signature()));
    player.setPlayerProfile(profile);
    return true;
  }

  /**
   * Tekstura, ktora gracz ma na sobie TERAZ — surowa wartosc wlasnosci profilu albo
   * {@code null}, gdy nie ma zadnej.
   *
   * <p>Istnieje dla {@code /cc skin <gracz>}: przy zgloszeniu "mam zla skorke" pierwsze
   * pytanie brzmi, co serwer w ogole trzyma na graczu, a bez tego odpowiadalo sie na nie
   * botem protokolowym.
   */
  public String currentTexture(Player player) {
    for (ProfileProperty property : player.getPlayerProfile().getProperties()) {
      if (TEXTURES.equals(property.getName())) {
        return property.getValue();
      }
    }
    return null;
  }

  /**
   * Nazwa tekstury do raportu: id z skins.yml, gdy to NASZA skorka, a gdy cudza — skrocony
   * hash z adresu textures.minecraft.net.
   *
   * <p>Hash jest po to, zeby dalo sie odroznic dwie obce skorki od siebie (np. skorke konta
   * od skorki nalozonej przez inny plugin) bez schodzenia do base64 recznie.
   */
  public String describeTexture(String value) {
    if (value == null || value.isBlank()) {
      return "brak";
    }
    for (Skin skin : skins.values()) {
      if (skin.value().equals(value)) {
        return skin.id();
      }
    }
    return "obca (" + textureHash(value) + ")";
  }

  /**
   * Hash tekstury wyluskany z zakodowanej wartosci profilu.
   *
   * <p>Pakiet-prywatne i czyste, bo to jedyny kawalek raportu, ktory da sie zlamac po cichu:
   * zly base64 albo inny ksztalt JSON-a nie moze wywalic komendy diagnostycznej.
   */
  static String textureHash(String value) {
    String json;
    try {
      json = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ex) {
      return "nieczytelna";
    }
    int at = json.indexOf("/texture/");
    if (at < 0) {
      return "nieczytelna";
    }
    int start = at + "/texture/".length();
    int end = start;
    while (end < json.length() && isHex(json.charAt(end))) {
      end++;
    }
    if (end == start) {
      return "nieczytelna";
    }
    String hash = json.substring(start, end);
    return hash.length() <= 16 ? hash : hash.substring(0, 16) + "...";
  }

  private static boolean isHex(char c) {
    return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }

  /**
   * Profil dla przebrania podgladu w kreatorze (LibsDisguises -> packetevents).
   *
   * <p>UUID i nick sa WYPROWADZONE Z ID SKORKI, a nie losowe: klient trzyma pobrane skorki
   * w cache'u pod kluczem profilu, wiec dwie rozne skorki pod tym samym profilem
   * pokazywalyby te sama teksture, a ta sama skorka pod losowym profilem byla pobierana
   * od nowa przy kazdym kliknieciu.
   */
  public UserProfile disguiseProfile(Skin skin) {
    UUID id = UUID.nameUUIDFromBytes(("cc-skin:" + skin.id()).getBytes(StandardCharsets.UTF_8));
    UserProfile profile = new UserProfile(id, shortName(skin.id()));
    List<TextureProperty> properties = new ArrayList<>(1);
    properties.add(new TextureProperty(TEXTURES, skin.value(), skin.signature()));
    profile.setTextureProperties(properties);
    return profile;
  }

  /**
   * Profil do GLOWY gracza (PLAYER_HEAD) pokazujacej te skorke — uzywany na ekranie wyboru
   * postaci MMOProfiles (patrz ProfileListHeadsListener).
   *
   * <p>UUID jest ten sam, co w przebraniu podgladu, i to nie przypadek: klient trzyma
   * pobrane skorki pod kluczem profilu, wiec glowa w menu i postac w kreatorze korzystaja
   * z jednego pobrania.
   */
  public PlayerProfile headProfile(Skin skin) {
    PlayerProfile profile = Bukkit.createProfile(
        UUID.nameUUIDFromBytes(("cc-skin:" + skin.id()).getBytes(StandardCharsets.UTF_8)),
        shortName(skin.id()));
    profile.setProperty(skin.signature() == null
        ? new ProfileProperty(TEXTURES, skin.value())
        : new ProfileProperty(TEXTURES, skin.value(), skin.signature()));
    return profile;
  }

  private static String shortName(String skinId) {
    String name = "cc_" + skinId;
    return name.length() <= MAX_PROFILE_NAME ? name : name.substring(0, MAX_PROFILE_NAME);
  }
}
