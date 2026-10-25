package pl.mcmmorpg.huddane;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Pattern;

import io.lumine.mythichud.api.HudHolder;
import io.lumine.mythichud.api.manager.ParserManager;
import io.lumine.mythichud.api.util.ComponentUtil;
import me.clip.placeholderapi.PlaceholderAPI;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Zamiennik ParserManagerImpl z MythicHUD 1.3.4 - ten sam wynik, bez kosztu kwadratowego.
 * Oryginal (PlaceholderAPIParser.parse) przy KAZDYM warunku/tekscie/pasku przechodzi po calym cache gracza
 * (klucz = kazdy tekst z '%' uzyty w HUD-zie) i robi String.replace na kazdym wpisie, do tego liniowo szuka w
 * kolejce unparsedText. Przy l3-kolumna (524 warunki) to ~0,7-1 rdzenia na gracza (spark 25.10, klon hud-koszt).
 * Tu: wartosc = mapa tekst -> wynik PAPI (O(1)), PAPI liczone na glownym watku co check-update tikow dla tekstow
 * uzytych w tym cyklu - jak w oryginale. Wynik fromLegacy (czysta funkcja tekstu) w cache.
 */
final class SzybkiParser implements ParserManager {
  private static final Pattern PLACEHOLDER = Pattern.compile("[%]([^%]+)[%]");   // jak ParserManagerImpl
  private static final int LIMIT_LEGACY = 50_000;
  private static final long POKOLENIE_TIKOW = 1200;   // tekst nieuzywany przez 1-2 min wypada z mapy gracza

  /** Dwie mapy na gracza: teksty z '%' bywaja zmienne (asset.parse wstawia liczby), wiec stare pokolenie wypada. */
  private static final class Wartosci {
    volatile Map<String, String> nowe = new ConcurrentHashMap<>();
    volatile Map<String, String> stare = Map.of();
    String get(String k) { String v = nowe.get(k); return v != null ? v : stare.get(k); }
  }

  private final Map<UUID, Wartosci> wartosci = new ConcurrentHashMap<>();
  private long cykle, cyklePokolenia;
  private final Set<String> doLiczenia = ConcurrentHashMap.newKeySet();
  private final Map<String, String> legacy = new ConcurrentHashMap<>();

  /** Podmienia parser MythicHUD. null = inna wersja MythicHUD, zostaje oryginal (wolniej, ale dziala). */
  static SzybkiParser zainstaluj(HudDane plugin, Plugin hud) {
    try {
      Field pole = hud.getClass().getDeclaredField("parserManager");
      pole.setAccessible(true);
      Object stary = pole.get(hud);
      if (stary == null || !stary.getClass().getName().equals("io.lumine.mythichud.manager.ParserManagerImpl")) {
        plugin.getLogger().warning("HudDane: nieznany parser MythicHUD (" + stary + "), zostaje oryginalny");
        return null;
      }
      SzybkiParser nowy = new SzybkiParser();
      long co = Math.max(1, hud.getConfig().getLong("check-update", 5));   // jak ParserManagerImpl (checkUpdateInterval)
      nowy.cyklePokolenia = Math.max(1, POKOLENIE_TIKOW / co);
      new BukkitRunnable() { @Override public void run() { nowy.licz(); } }.runTaskTimer(plugin, 1L, co);
      pole.set(hud, nowy);
      zatrzymajStary(plugin, stary);
      plugin.getLogger().info("HudDane: parser MythicHUD podmieniony (O(1), PAPI co " + co + " t)");
      return nowy;
    } catch (ReflectiveOperationException | RuntimeException ex) {
      plugin.getLogger().log(Level.WARNING, "HudDane: nie podmieni parsera MythicHUD (" + ex + "), zostaje oryginalny");
      return null;
    }
  }

  /** Zegar starego PlaceholderAPIParser - po podmianie nikt mu nie dosyla tekstow, ale nie zostawiamy go tykajacego. */
  private static void zatrzymajStary(HudDane plugin, Object stary) {
    try {
      Field f = stary.getClass().getDeclaredField("parsers");
      f.setAccessible(true);
      for (Object p : (Iterable<?>) f.get(stary)) if (p instanceof BukkitRunnable r) r.cancel();
    } catch (ReflectiveOperationException | RuntimeException ex) {
      plugin.getLogger().fine("HudDane: stary zegar PlaceholderAPIParser zostaje (" + ex + ")");
    }
  }

  @Override public String parse(HudHolder holder, String text) {
    String s = text;
    if (s.indexOf('%') >= 0) {
      doLiczenia.add(s);
      Wartosci w = wartosci.get(holder.player().getUniqueId());
      if (w != null) {
        String v = w.get(s);
        if (v != null) s = v;
      }
    }
    String g = legacy.get(s);
    if (g == null) {
      g = ComponentUtil.fromLegacy(s);
      if (legacy.size() >= LIMIT_LEGACY) legacy.clear();
      legacy.put(s, g);
    }
    return g;
  }

  @Override public boolean hasPlaceholder(String text) {
    return text != null && PLACEHOLDER.matcher(text).find();
  }

  /** Glowny watek: PAPI dla tekstow uzytych od ostatniego cyklu, dla kazdego gracza online (jak PlaceholderAPIParser.run). */
  private void licz() {
    if (++cykle % cyklePokolenia == 0) for (Wartosci w : wartosci.values()) { w.stare = w.nowe; w.nowe = new ConcurrentHashMap<>(); }
    if (doLiczenia.isEmpty()) return;
    String[] teksty = new String[doLiczenia.size()];
    int n = 0;
    for (Iterator<String> it = doLiczenia.iterator(); it.hasNext() && n < teksty.length; ) { teksty[n++] = it.next(); it.remove(); }
    for (Player p : Bukkit.getOnlinePlayers()) {
      Map<String, String> w = wartosci.computeIfAbsent(p.getUniqueId(), u -> new Wartosci()).nowe;
      for (int i = 0; i < n; i++) {
        try {
          w.put(teksty[i], PlaceholderAPI.setPlaceholders(p, teksty[i]));
        } catch (NullPointerException ex) {
          Bukkit.getLogger().warning("[HudDane] Failed to parse placeholder '" + teksty[i] + "' for '" + p.getName() + "'");
        }
      }
    }
  }

  /** Wartosci od razu przy wejsciu (cele glow druzyny), zanim zegar je policzy - inaczej head dostaje surowy tekst. */
  void zasiej(Player p, String text) {
    wartosci.computeIfAbsent(p.getUniqueId(), u -> new Wartosci()).nowe.put(text, PlaceholderAPI.setPlaceholders(p, text));
  }

  void wyjscie(UUID u) { wartosci.remove(u); }
}
