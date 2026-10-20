package pl.mcmmorpg.huddane;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Dane HUD-a, ktorych nie daje zaden inny plugin: czlonkowie druzyny MMOCore (HP, zasob, poziom, zasieg),
 * przywolance gracza z MythicMobs (HP, czas do konca) i efekty mikstur z pozostalym czasem.
 * Wszystko jako placeholdery PAPI %hud_...% dla MythicHUD; migawka na gracza zyje {@link #WAZNOSC_MS}.
 */
public final class HudDane extends JavaPlugin implements Listener {
  static final long WAZNOSC_MS = 250;

  final Map<UUID, Migawka> migawki = new ConcurrentHashMap<>();
  final Zdarzenia zdarzenia = new Zdarzenia();
  Nawigacja nawigacja;
  int zasieg;
  double niskieHp;
  int limitSlug;
  final Map<String, Integer> czasZycia = new LinkedHashMap<>();
  final Map<String, String> efekty = new LinkedHashMap<>();

  @Override public void onEnable() {
    saveDefaultConfig();
    zasieg = getConfig().getInt("druzyna.zasieg", 64);
    niskieHp = getConfig().getDouble("druzyna.niskie-hp", 0.3);
    limitSlug = getConfig().getInt("slugi.limit", 3);
    ConfigurationSection cz = getConfig().getConfigurationSection("slugi.czas-zycia");
    if (cz != null) for (String k : cz.getKeys(false)) czasZycia.put(k, cz.getInt(k));
    ConfigurationSection ef = getConfig().getConfigurationSection("efekty");
    if (ef != null) for (String k : ef.getKeys(false)) efekty.put(k, ef.getString(k));
    getServer().getPluginManager().registerEvents(this, this);
    getServer().getPluginManager().registerEvents(zdarzenia, this);
    nawigacja = new Nawigacja(getConfig().getConfigurationSection("zadania"));
    getServer().getPluginManager().registerEvents(nawigacja, this);
    new Placeholdery(this).register();
    getLogger().info("HudDane: druzyna zasieg " + zasieg + ", " + czasZycia.size() + " typow slug, " + efekty.size() + " efektow");
  }

  Migawka migawka(Player p) {
    long teraz = System.currentTimeMillis();
    Migawka m = migawki.get(p.getUniqueId());
    if (m == null || teraz - m.czas > WAZNOSC_MS) {
      m = Migawka.zbierz(this, p, teraz);
      migawki.put(p.getUniqueId(), m);
    }
    return m;
  }

  @EventHandler public void wyjscie(PlayerQuitEvent e) { migawki.remove(e.getPlayer().getUniqueId()); }
}
