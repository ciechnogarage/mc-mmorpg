package pl.mcmmorpg.mieszkancy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Na czas rozmowy z NPC znika cały HUD: warstwy MythicHUD (karta postaci, tracker zadania, kafelki, ramka celu)
 * i minimapa NMinimap. Inaczej karta i tracker leżą na oknie rozmowy (MythicHUD rysuje na niewidzialnym pasku bossa,
 * a okno Dialog ma półprzezroczyste tło — klon 16.11).
 *
 * Wzór: scena zielarza (Rzemioslo ZScena, gałąź zielarz-naprawa). Oba pluginy zapisują zmianę NA STAŁE (MythicHUD:
 * PDC gracza mythichud:hud_layouts, NMinimap: baza), więc to, co schowaliśmy, trzyma marker w PDC gracza
 * (mieszkancy:rozmowa_hud) i wejście do gry przywraca to po awarii albo wyjściu w trakcie rozmowy.
 * HudDane co cykl dokłada graczowi wariant kolumny l3 — flaga huddane:schowany (HudDane od gałęzi hud-dane-schowany)
 * mówi mu, żeby tego nie robił. Bez nowego HudDane kolumna l3 wraca po ~0,25 s (reszta HUD-a zostaje schowana).
 * Do tego napisy w świecie (imię/rola nad NPC, szyldy) w promieniu NAPISY_R bloków: przebijały przez tytuł okna.
 * Chowane tylko dla rozmawiającego (Player#hideEntity, nic się nie zapisuje; po wyjściu z gry wracają same).
 * MythicHUD i NMinimap przez refleksję: bez nich (albo innej wersji API) rozmowa działa, tylko HUD się nie chowa (WARN).
 */
final class Hud {
  private static final String MAPA = "#nminimap";
  private static final double NAPISY_R = 24;
  static final NamespacedKey HUDDANE_SCHOWANY = new NamespacedKey("huddane", "schowany");

  private final Mieszkancy m;
  private final NamespacedKey kMarker;
  private boolean gotowe;
  private final Map<UUID, List<Entity>> napisy = new HashMap<>();
  private Object mhLayouts;   // io.lumine.mythichud.api.manager.LayoutManager
  private Method mhHolderGet, mhGetLayouts, mhGetLayout, mhKey, mhActive, mhAdd, mhRemove, mhSend;
  private Object nmInstance;  // su.nezushin.nminimap.NMinimap
  private Method nmGracze, nmGracz, nmWlaczona, nmUstaw;

  Hud(Mieszkancy m) {
    this.m = m;
    this.kMarker = new NamespacedKey(m, "rozmowa_hud");
  }

  private void przygotuj() {
    if (gotowe) return;
    gotowe = true;
    Plugin mh = Bukkit.getPluginManager().getPlugin("MythicHUD");
    if (mh != null && mh.isEnabled()) try {
      ClassLoader cl = mh.getClass().getClassLoader();
      Class<?> api = Class.forName("io.lumine.mythichud.api.MythicHUD", true, cl);
      Class<?> holder = Class.forName("io.lumine.mythichud.api.HudHolder", true, cl);
      Class<?> layout = Class.forName("io.lumine.mythichud.api.element.layout.HudLayout", true, cl);
      Class<?> lm = Class.forName("io.lumine.mythichud.api.manager.LayoutManager", true, cl);
      mhLayouts = api.getMethod("layouts").invoke(api.getMethod("getInstance").invoke(null));
      mhGetLayouts = lm.getMethod("getLayouts");
      mhGetLayout = lm.getMethod("get", String.class);
      mhKey = layout.getMethod("getKey");
      mhHolderGet = holder.getMethod("get", Player.class);
      mhActive = holder.getMethod("getActiveLayout", String.class);
      mhAdd = holder.getMethod("addLayout", layout);
      mhRemove = holder.getMethod("removeLayout", layout);
      mhSend = holder.getMethod("send");
    } catch (Throwable t) { mhLayouts = null; m.getLogger().warning("MythicHUD bez chowania na czas rozmowy: " + t); }
    Plugin nm = Bukkit.getPluginManager().getPlugin("NMinimap");
    if (nm != null && nm.isEnabled()) try {
      nmInstance = nm.getClass().getMethod("getInstance").invoke(null);
      nmGracze = nmInstance.getClass().getMethod("getPlayersWithMap");
      Class<?> np = Class.forName("su.nezushin.nminimap.player.NMapPlayer", true, nm.getClass().getClassLoader());
      nmGracz = np.getMethod("getPlayer");
      nmWlaczona = np.getMethod("isEnabled");
      nmUstaw = np.getMethod("setEnabled", boolean.class);
    } catch (Throwable t) { nmInstance = null; m.getLogger().warning("NMinimap bez chowania na czas rozmowy: " + t); }
  }

  boolean schowany(Player p) { return p.getPersistentDataContainer().has(kMarker, PersistentDataType.STRING); }

  /** Początek rozmowy: chowa aktywne warstwy i włączoną minimapę; zapisuje, co schowano. Drugie wywołanie nic nie robi. */
  void schowaj(Player p) {
    if (schowany(p)) return;
    przygotuj();
    // Flaga najpierw: HudDane w tym samym ticku nie dołoży z powrotem wariantu l3.
    p.getPersistentDataContainer().set(HUDDANE_SCHOWANY, PersistentDataType.BYTE, (byte) 1);
    List<String> schowane = new ArrayList<>();
    if (mhLayouts != null) try {
      Object h = mhHolderGet.invoke(null, p);
      if (h != null) for (Object l : (Collection<?>) mhGetLayouts.invoke(mhLayouts)) {
        String k = (String) mhKey.invoke(l);
        if (((Optional<?>) mhActive.invoke(h, k)).isPresent() && (boolean) mhRemove.invoke(h, l)) schowane.add(k);
      }
      // removeLayout/addLayout nie wysyłają paska (javap 1.3.4) — bez send() stary obraz zostaje, dopóki coś
      // na HUD-zie się nie zmieni (klon 16.11: pierwsza rozmowa po wejściu, karta została na oknie 2/2).
      if (h != null) mhSend.invoke(h);
    } catch (Throwable t) { m.getLogger().warning("Chowanie HUD: " + t); }
    Object mp = graczMapy(p);
    if (mp != null) try {
      if ((boolean) nmWlaczona.invoke(mp)) { nmUstaw.invoke(mp, false); schowane.add(MAPA); }
    } catch (Throwable t) { m.getLogger().warning("Chowanie minimapy: " + t); }
    List<Entity> ukryte = new ArrayList<>();
    for (Entity e : p.getNearbyEntities(NAPISY_R, NAPISY_R, NAPISY_R))
      if (e instanceof TextDisplay && e.getPersistentDataContainer().has(m.kluczSmiec)) { p.hideEntity(m, e); ukryte.add(e); }
    napisy.put(p.getUniqueId(), ukryte);
    // Pusty marker też zapisujemy: znaczy „rozmowa trwa”, a przywrocenie zdejmie flagę HudDane.
    p.getPersistentDataContainer().set(kMarker, PersistentDataType.STRING, String.join(",", schowane));
    if (m.debug) m.getLogger().info("[debug] HUD schowany " + p.getName() + ": " + schowane);
  }

  /** Koniec rozmowy (albo wejście po przerwanej): oddaje dokładnie to, co schował schowaj(). Czego nie da się oddać
   *  (wyjście z gry: MythicHUD zwalnia HudHolder przed naszym PlayerQuitEvent), zostaje w markerze i wraca przy wejściu. */
  void przywroc(Player p) {
    List<Entity> ukryte = napisy.remove(p.getUniqueId());
    if (ukryte != null) for (Entity e : ukryte) if (e.isValid()) p.showEntity(m, e);
    String mk = p.getPersistentDataContainer().get(kMarker, PersistentDataType.STRING);
    if (mk == null) return;
    przygotuj();
    List<String> zostaje = new ArrayList<>();
    Object h = null;
    for (String k : mk.split(",")) {
      if (k.isEmpty()) continue;
      try {
        if (k.equals(MAPA)) {
          Object mp = graczMapy(p);
          if (mp == null) { zostaje.add(k); continue; }
          if (!(boolean) nmWlaczona.invoke(mp)) nmUstaw.invoke(mp, true);
        } else if (mhLayouts != null) {
          if (h == null) h = mhHolderGet.invoke(null, p);
          Object l = mhGetLayout.invoke(mhLayouts, k);
          if (l == null) continue;            // layoutu już nie ma (zmiana HUD-a) — nie ma czego oddawać
          if (h == null) { zostaje.add(k); continue; }
          if (!((Optional<?>) mhActive.invoke(h, k)).isPresent()) mhAdd.invoke(h, l);
        } else zostaje.add(k);
      } catch (Throwable t) { zostaje.add(k); m.getLogger().warning("Przywracanie HUD " + k + ": " + t); }
    }
    if (h != null) try { mhSend.invoke(h); } catch (Throwable t) { m.getLogger().warning("Odświeżenie HUD: " + t); }
    if (zostaje.isEmpty()) {
      p.getPersistentDataContainer().remove(kMarker);
      p.getPersistentDataContainer().remove(HUDDANE_SCHOWANY);
    } else p.getPersistentDataContainer().set(kMarker, PersistentDataType.STRING, String.join(",", zostaje));
    if (m.debug) m.getLogger().info("[debug] HUD przywrocony " + p.getName() + ", zostaje: " + zostaje);
  }

  private Object graczMapy(Player p) {
    if (nmInstance == null) return null;
    try {
      for (Object mp : (Collection<?>) nmGracze.invoke(nmInstance)) {
        Object g = nmGracz.invoke(mp);
        if (g instanceof Player gp && gp.getUniqueId().equals(p.getUniqueId())) return mp;
      }
    } catch (Throwable t) { m.getLogger().warning("Minimapa gracza: " + t); }
    return null;
  }
}
