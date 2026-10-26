package pl.mcmmorpg.huddane;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.lumine.mythichud.api.HudHolder;
import io.lumine.mythichud.api.MythicHUD;
import io.lumine.mythichud.api.element.layout.HudLayout;
import io.lumine.mythichud.api.manager.LayoutManager;
import me.clip.placeholderapi.PlaceholderAPI;

import org.bukkit.entity.Player;

/**
 * Kolumna l3 HUD-a w 12 layoutach-wariantach, po jednym na stan {@code %hud_uklad%} (tools/hud/l3/warianty.py).
 * MythicHUD liczy co check-update KAZDY element aktywnego layoutu, takze ukryty (ActiveElement.update), wiec stara
 * l3 z 12 polozeniami jako elementami z warunkiem kosztowala 148 elementow na gracza; tu aktywny jest jeden wariant
 * (8-19 elementow). Wywolywane na glownym watku w cyklu {@link SzybkiParser} - wariant i wartosci placeholderow
 * pochodza z tego samego cyklu, wiec panele nie skacza. Uzgadnia stan od zera (usuwa obce warianty, dodaje biezacy),
 * wiec sam naprawia duplikaty po wejsciu (flagi w PDC hud_layouts) i po /hud reload.
 */
final class Uklady {
  /** Stan %hud_uklad% -> klucz layoutu; te same co KLUCZE w warianty.py (sufiks = kolejnosc rysowania). */
  static final Map<String, String> WARIANTY = Map.ofEntries(
      Map.entry("e0c0t0", "l3-e0c0t0a"), Map.entry("e0c0t1", "l3-e0c0t1a"), Map.entry("e0c0t2", "l3-e0c0t2b"),
      Map.entry("e0c1t0", "l3-e0c1t0a"), Map.entry("e0c1t1", "l3-e0c1t1b"), Map.entry("e0c1t2", "l3-e0c1t2c"),
      Map.entry("e1c0t0", "l3-e1c0t0f"), Map.entry("e1c0t1", "l3-e1c0t1g"), Map.entry("e1c0t2", "l3-e1c0t2h"),
      Map.entry("e1c1t0", "l3-e1c1t0g"), Map.entry("e1c1t1", "l3-e1c1t1h"), Map.entry("e1c1t2", "l3-e1c1t2i"));
  /** Tyle ms po wejsciu nie ruszamy layoutow: MythicHUD w tym czasie asynchronicznie buduje je z PDC (initialize). */
  private static final long ZWLOKA_MS = 1500;

  private final java.util.logging.Logger log;
  private final Map<UUID, Long> wejscie = new ConcurrentHashMap<>();

  Uklady(java.util.logging.Logger log) { this.log = log; }
  private final Set<String> wartosci = Set.copyOf(WARIANTY.values());

  void wszedl(Player p) { wejscie.put(p.getUniqueId(), System.currentTimeMillis()); }
  void wyszedl(UUID u) { wejscie.remove(u); }

  void odswiez(Player p) {
    Long od = wejscie.get(p.getUniqueId());
    if (od != null) {
      if (System.currentTimeMillis() - od < ZWLOKA_MS) return;
      wejscie.remove(p.getUniqueId());
    }
    HudHolder h = HudHolder.get(p);
    if (h == null || !h.isInitialized()) return;
    BezDuplikatow.owin(h, log);
    LayoutManager lm = MythicHUD.getInstance().layouts();
    String chce = WARIANTY.get(PlaceholderAPI.setPlaceholders(p, "%hud_uklad%"));
    for (String k : wartosci) {
      if (k.equals(chce) || h.getActiveLayout(k).isEmpty()) continue;
      HudLayout l = lm.get(k);
      if (l != null) h.removeLayout(l);
    }
    if (chce != null && h.getActiveLayout(chce).isEmpty()) {
      HudLayout l = lm.get(chce);
      if (l != null) h.addLayout(l);
    }
  }
}
