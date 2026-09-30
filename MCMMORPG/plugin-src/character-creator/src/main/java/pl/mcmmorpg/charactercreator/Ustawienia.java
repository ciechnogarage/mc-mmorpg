package pl.mcmmorpg.charactercreator;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Pokretla wygladu, czasu i wlaczniki z sekcji {@code ustawienia:} w config.yml (wlasciciel 30.09:
 * zmiany bez nowego jara). Kazde wywolanie niesie DOMYSLNA wartosc — brak wpisu = jak w kodzie.
 * Stale techniczne (sloty ekwipunku, schemat danych, zapis na dysk) zostaja w kodzie celowo.
 * Przeladowanie: {@code /cc reload}.
 */
public final class Ustawienia {

  private static volatile ConfigurationSection section;

  private Ustawienia() {
  }

  public static void load(FileConfiguration config) {
    section = config.getConfigurationSection("ustawienia");
  }

  public static int i(String key, int def) {
    ConfigurationSection s = section;
    return s == null ? def : s.getInt(key, def);
  }

  public static long l(String key, long def) {
    ConfigurationSection s = section;
    return s == null ? def : s.getLong(key, def);
  }

  public static double d(String key, double def) {
    ConfigurationSection s = section;
    return s == null ? def : s.getDouble(key, def);
  }

  public static boolean b(String key, boolean def) {
    ConfigurationSection s = section;
    return s == null ? def : s.getBoolean(key, def);
  }
}
