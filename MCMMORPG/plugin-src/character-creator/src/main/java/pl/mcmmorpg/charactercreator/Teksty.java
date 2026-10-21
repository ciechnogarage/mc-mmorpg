package pl.mcmmorpg.charactercreator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Teksty dla gracza z sekcji {@code teksty:} w config.yml (wlasciciel 30.09: "zeby nie przerabiac
 * jara za kazdym razem"). Kazde wywolanie niesie klucz i DOMYSLNA tresc — brak wpisu w configu
 * = tekst jak w kodzie, wiec config moze byc niepelny. Kolory kodami {@code &}, wstawki
 * {@code {nazwa}} podmieniane parami klucz/wartosc. Przeladowanie: {@code /cc reload}.
 */
public final class Teksty {

  /** Leniwie: NameRules (testy bez serwera) woła tylko plain(), bez parsera kolorow. */
  private static final class Legacy {
    static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
  }
  private static final Map<String, String> OVERRIDES = new ConcurrentHashMap<>();

  private Teksty() {
  }

  /** Wczytuje (od nowa) sekcje {@code teksty} — klucze splaszczone kropkami. */
  public static void load(FileConfiguration config) {
    OVERRIDES.clear();
    ConfigurationSection sec = config.getConfigurationSection("teksty");
    if (sec == null) {
      return;
    }
    for (String key : sec.getKeys(true)) {
      if (!sec.isConfigurationSection(key)) {
        String value = sec.getString(key);
        if (value != null) {
          OVERRIDES.put(key, value);
        }
      }
    }
  }

  /** Surowy tekst (z kodami &) z podmienionymi wstawkami. */
  public static String s(String key, String def, Object... kv) {
    String out = OVERRIDES.getOrDefault(key, def);
    for (int i = 0; i + 1 < kv.length; i += 2) {
      out = out.replace("{" + kv[i] + "}", String.valueOf(kv[i + 1]));
    }
    return out;
  }

  /** Tekst jako komponent (kody & -> kolory), bez kursywy. */
  public static Component t(String key, String def, Object... kv) {
    return Legacy.LEGACY.deserialize(s(key, def, kv)).decoration(TextDecoration.ITALIC, false);
  }

  /** Tekst bez kodow kolorow — tam, gdzie kolor nadaje kod (np. etykiety podpowiedzi). */
  public static String plain(String key, String def, Object... kv) {
    return s(key, def, kv).replaceAll("&[0-9a-fk-orA-FK-OR]", "");
  }
}
