package pl.mcmmorpg.rzemioslo;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Zblizenie zbioru bez interfejsu MMO: warstwy MythicHUD (karta postaci, zadanie, paski...) i minimapa NMinimap znikaja
 * na czas sceny i wracaja po niej. Oba pluginy zapisuja zmiane na stale (MythicHUD: PDC gracza mythichud:hud_layouts,
 * NMinimap: NMapPlayer.setEnabled -> saveAsync do bazy), wiec co schowalismy, zapisuje marker w PDC gracza
 * (rzemioslo:scena_hud) i wejscie do gry przywraca to po awarii albo wyjsciu w trakcie sceny.
 * Oba pluginy nie sa zaleznoscia kompilacji (softdepend): wywolania przez refleksje, metody szukane raz.
 */
final class ZScena {
    private static final String MAPA = "#nminimap";
    private final Rzemioslo plugin;
    private final NamespacedKey kMarker;
    private boolean gotowe;
    private Object mhLayouts;   // io.lumine.mythichud.api.manager.LayoutManager
    private Method mhHolderGet, mhGetLayouts, mhGetLayout, mhKey, mhActive, mhAdd, mhRemove;
    private Object nmInstance;  // su.nezushin.nminimap.NMinimap
    private Method nmGracze, nmGracz, nmWlaczona, nmUstaw;

    ZScena(Rzemioslo plugin) {
        this.plugin = plugin;
        this.kMarker = new NamespacedKey(plugin, "scena_hud");
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
        } catch (Throwable t) { mhLayouts = null; plugin.getLogger().warning("Zielarz: MythicHUD bez ukrywania w scenie: " + t); }
        Plugin nm = Bukkit.getPluginManager().getPlugin("NMinimap");
        if (nm != null && nm.isEnabled()) try {
            nmInstance = nm.getClass().getMethod("getInstance").invoke(null);
            nmGracze = nmInstance.getClass().getMethod("getPlayersWithMap");
            Class<?> np = Class.forName("su.nezushin.nminimap.player.NMapPlayer", true, nm.getClass().getClassLoader());
            nmGracz = np.getMethod("getPlayer");
            nmWlaczona = np.getMethod("isEnabled");
            nmUstaw = np.getMethod("setEnabled", boolean.class);
        } catch (Throwable t) { nmInstance = null; plugin.getLogger().warning("Zielarz: NMinimap bez ukrywania w scenie: " + t); }
    }

    /** Poczatek sceny: chowa aktywne warstwy i wlaczona minimape; zapisuje, co schowano. */
    void schowaj(Player p) {
        przygotuj();
        if (p.getPersistentDataContainer().has(kMarker, PersistentDataType.STRING)) return;   // juz schowane (nie nadpisuj listy)
        List<String> schowane = new ArrayList<>();
        if (mhLayouts != null) try {
            Object h = mhHolderGet.invoke(null, p);
            for (Object l : (Collection<?>) mhGetLayouts.invoke(mhLayouts)) {
                String k = (String) mhKey.invoke(l);
                if (((Optional<?>) mhActive.invoke(h, k)).isPresent() && (boolean) mhRemove.invoke(h, l)) schowane.add(k);
            }
        } catch (Throwable t) { plugin.getLogger().warning("Zielarz: chowanie HUD: " + t); }
        Object mp = graczMapy(p);
        if (mp != null) try {
            if ((boolean) nmWlaczona.invoke(mp)) { nmUstaw.invoke(mp, false); schowane.add(MAPA); }
        } catch (Throwable t) { plugin.getLogger().warning("Zielarz: chowanie minimapy: " + t); }
        if (!schowane.isEmpty()) p.getPersistentDataContainer().set(kMarker, PersistentDataType.STRING, String.join(",", schowane));
    }

    /** Koniec sceny (albo wejscie po przerwanej): oddaje dokladnie to, co schowal schowaj(). Czego nie dalo sie oddac
     *  (wyjscie z gry: MythicHUD zwalnia HudHolder przed naszym PlayerQuitEvent -> null; test 24.10 kick w scenie zabral
     *  caly HUD na stale), zostaje w markerze i wraca przy wejsciu. */
    void przywroc(Player p) {
        String m = p.getPersistentDataContainer().get(kMarker, PersistentDataType.STRING);
        if (m == null) return;
        przygotuj();
        List<String> zostaje = new ArrayList<>();
        Object h = null;
        for (String k : m.split(",")) {
            if (k.isEmpty()) continue;
            try {
                if (k.equals(MAPA)) {
                    Object mp = graczMapy(p);
                    if (mp == null) { zostaje.add(k); continue; }
                    if (!(boolean) nmWlaczona.invoke(mp)) nmUstaw.invoke(mp, true);
                } else if (mhLayouts != null) {
                    if (h == null) h = mhHolderGet.invoke(null, p);
                    Object l = mhGetLayout.invoke(mhLayouts, k);
                    if (h == null || l == null) { zostaje.add(k); continue; }
                    if (!((Optional<?>) mhActive.invoke(h, k)).isPresent()) mhAdd.invoke(h, l);
                } else zostaje.add(k);
            } catch (Throwable t) { zostaje.add(k); plugin.getLogger().warning("Zielarz: przywracanie " + k + ": " + t); }
        }
        if (zostaje.isEmpty()) p.getPersistentDataContainer().remove(kMarker);
        else p.getPersistentDataContainer().set(kMarker, PersistentDataType.STRING, String.join(",", zostaje));
    }

    private Object graczMapy(Player p) {
        if (nmInstance == null) return null;
        try {
            for (Object mp : (Collection<?>) nmGracze.invoke(nmInstance)) {
                Object g = nmGracz.invoke(mp);
                if (g instanceof Player gp && gp.getUniqueId().equals(p.getUniqueId())) return mp;
            }
        } catch (Throwable t) { plugin.getLogger().warning("Zielarz: minimapa gracza: " + t); }
        return null;
    }
}
