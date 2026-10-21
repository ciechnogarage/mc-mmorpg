package pl.mcmmorpg.chowance.integracja;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Czytelne nazwy przedmiotów MMOItems (ID → „Kamień Przemiany Powietrza”) do komunikatów dla gracza.
 * Czytane z plików plugins/MMOItems/item/*.yml (pole base.name), bez tagów MiniMessage i kodów &amp;.
 * Brak pliku/nazwy = ID zamienione na zwykłe słowa, nigdy surowe ID z podkreślnikami.
 */
public final class NazwyMmo {
  private final Map<String, String> nazwy = new HashMap<>();

  public void wczytaj(File katalogPluginow) {
    nazwy.clear();
    File[] pliki = new File(katalogPluginow, "MMOItems/item").listFiles((d, n) -> n.endsWith(".yml"));
    if (pliki == null) return;
    for (File f : pliki) {
      YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
      for (String id : y.getKeys(false)) {
        String n = y.getString(id + ".base.name");
        if (n != null && !n.isBlank()) nazwy.put(id, czysc(n));
      }
    }
  }

  static String czysc(String s) {
    return s.replaceAll("<[^>]*>", "").replaceAll("[&§][0-9a-fk-orA-FK-OR]", "").trim();
  }

  public String nazwa(String id) {
    if (id == null) return "";
    String n = nazwy.get(id);
    if (n != null) return n;
    String t = id.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    return t.isEmpty() ? id : Character.toUpperCase(t.charAt(0)) + t.substring(1);
  }

  public int rozmiar() { return nazwy.size(); }
}
