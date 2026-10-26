package pl.mcmmorpg.huddane;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Przycina mape placeholderow kazdego HudAsset MythicHUD 1.3.4 do wpisow, ktorych asset naprawde uzywa.
 *
 * HudAsset.parse(holder, text) liczy KAZDY wpis mapy (3 na kazda warstwe z listenerem: -value/-max/-percent,
 * czyli parser + toDouble + DecimalFormat + String.replace) przy kazdym wywolaniu, takze gdy tekst nie ma
 * zadnego %...%. Wolaja go warunki placeholder (2x na warunek) i teksty dynamiczne, w kazdym cyklu check-update.
 * Statycznie (26.10, uklady prod): ~1980 wpisow liczonych na ocene gracza, potrzebnych ~53.
 *
 * Wynik jest identyczny: zamiana placeholdera, ktorego nie ma w tekscie, nic nie zmienia, a tekst parsowany
 * przez asset pochodzi wylacznie z jego wlasnej sekcji YAML (warunki assetu, warunki i tekst jego warstw).
 * Zostaje wpis, ktorego "%klucz%" wystepuje gdziekolwiek w sekcji (nadzbior), w kolejnosci oryginalnej mapy.
 * Asset z "%arg-" (popupy) albo bez sekcji w plikach zostaje nietkniety.
 * Po /mythichud reload assety sa nowymi obiektami - zegar co 30 s przycina je ponownie (koszt: ~50 sprawdzen mapy).
 */
final class Przycinacz {
  private final Plugin hud;
  private final Set<Object> przyciete = Collections.newSetFromMap(new WeakHashMap<>());
  private Field pole;
  private boolean wylaczony;

  Przycinacz(Plugin hud) { this.hud = hud; }

  /** Zwraca liczbe nowo przycietych assetow; loguje podsumowanie, gdy cos przycial. */
  @SuppressWarnings("unchecked")
  int przytnij(java.util.logging.Logger log) {
    if (wylaczony) return 0;
    try {
      Object menedzer = hud.getClass().getMethod("assets").invoke(hud);
      Collection<Object> assety = (Collection<Object>) menedzer.getClass().getMethod("getAssets").invoke(menedzer);
      Map<String, String> teksty = null;
      int nowe = 0, przed = 0, po = 0, pominiete = 0;
      for (Object a : assety) {
        if (przyciete.contains(a)) continue;
        if (pole == null) {
          pole = a.getClass().getDeclaredField("placeholders");
          pole.setAccessible(true);
        }
        if (teksty == null) teksty = wczytaj();
        przyciete.add(a);
        String klucz = ((String) a.getClass().getMethod("getKey").invoke(a)).toLowerCase(Locale.ROOT);
        String t = teksty.get(klucz);
        Map<String, Object> stara = (Map<String, Object>) pole.get(a);
        if (t == null || t.contains("%arg-") || stara.isEmpty()) { pominiete++; continue; }
        Map<String, Object> nowa = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : stara.entrySet())
          if (t.contains("%" + e.getKey() + "%")) nowa.put(e.getKey(), e.getValue());
        przed += stara.size(); po += nowa.size(); nowe++;
        if (nowa.size() != stara.size()) pole.set(a, Collections.unmodifiableMap(nowa));
      }
      if (nowe > 0 || pominiete > 0)
        log.info("HudDane: placeholdery assetow MythicHUD przyciete " + przed + " -> " + po
            + " (" + nowe + " assetow, " + pominiete + " pominietych)");
      return nowe;
    } catch (ReflectiveOperationException | RuntimeException ex) {
      wylaczony = true;   // inna wersja MythicHUD: zostaje oryginalna mapa, nic sie nie psuje
      log.log(Level.WARNING, "HudDane: nie przytne placeholderow assetow MythicHUD (" + ex + ")");
      return 0;
    }
  }

  /** Klucz assetu (lower-case, jak AssetManager) -> wszystkie wartosci jego sekcji sklejone w jeden tekst. */
  private Map<String, String> wczytaj() {
    Map<String, String> wynik = new HashMap<>();
    File[] pliki = new File(hud.getDataFolder(), "hud_assets/hud").listFiles((d, n) -> n.endsWith(".yml"));
    if (pliki == null) return wynik;
    for (File f : pliki) {
      YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
      for (String k : y.getKeys(false)) {
        ConfigurationSection s = y.getConfigurationSection(k);
        if (s == null) continue;
        StringBuilder sb = new StringBuilder();
        for (Object v : s.getValues(true).values())
          if (!(v instanceof ConfigurationSection)) sb.append(v).append('\n');
        wynik.put(k.toLowerCase(Locale.ROOT), sb.toString());
      }
    }
    return wynik;
  }
}
