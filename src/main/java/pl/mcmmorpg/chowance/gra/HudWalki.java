package pl.mcmmorpg.chowance.gra;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.logika.Wojownik;
import pl.mcmmorpg.chowance.model.Ruch;

/**
 * HUD walki. Dwa tryby:
 * <ul>
 *   <li>{@code walka.hud_mythichud: true} (domyślnie od 2026-09-30) — panele stworów (nazwa, poziom, typy, pasek HP)
 *   i PIONOWĄ listę klawiszy rysuje MythicHUD z placeholderów {@code %chowance_walka_*%}; u góry zostaje jeden pasek
 *   z tym, co się teraz dzieje, i czasem. Właściciel: „hud i ui dalej źle wygląda, mało przejrzysty” — trzy cienkie
 *   paski z drobnym tekstem i cała klawiszologia w jednej linii były nie do przeczytania w ferworze walki.</li>
 *   <li>{@code false} — dawne trzy paski BossBar (przeciwnik, twój stwór, klawisze w jednej linii).</li>
 * </ul>
 * Pasek akcji jest zajęty przez HUD MMOCore, dlatego nie on.
 */
public final class HudWalki {
  private final Chowance c;
  private final Map<UUID, BossBar[]> paski = new ConcurrentHashMap<>();
  private final Map<UUID, Stan> stany = new ConcurrentHashMap<>();

  /** Co widzi gracz — czytane przez placeholdery (MythicHUD odświeża co 5 ticków). */
  public static final class Stan {
    public volatile String onNazwa = "", onOpis = "", jaNazwa = "", jaOpis = "", naglowek = "", kto = "";
    public volatile double onHp, jaHp;
    public volatile int onMax = 1, jaMax = 1;
    public volatile List<String> klawisze = List.of();
  }

  public HudWalki(Chowance c) { this.c = c; }

  /** Stan HUD walki gracza albo null, gdy nie walczy. */
  public Stan stan(UUID u) { return stany.get(u); }

  private Stan st(Player p) { return stany.computeIfAbsent(p.getUniqueId(), k -> new Stan()); }

  private boolean mythic() { return c.ust.hudMythic; }

  private BossBar[] paski(Player p) {
    return paski.computeIfAbsent(p.getUniqueId(), k -> {
      BossBar[] b = {
          BossBar.bossBar(Chowance.mm(""), 1f, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10),
          BossBar.bossBar(Chowance.mm(""), 1f, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10),
          BossBar.bossBar(Chowance.mm(""), 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS)};
      // w trybie MythicHUD tylko pasek zdarzeń; panele stworów rysuje MythicHUD
      if (mythic()) p.showBossBar(b[2]);
      else for (BossBar x : b) p.showBossBar(x);
      return b;
    });
  }

  /** Stan obu stworów (hp pokazywane — w trakcie odtwarzania nie końcowe). */
  public void stwory(Player p, Wojownik ja, double hpJa, Wojownik on, double hpOn, String nazwaPrzeciwnika) {
    if (p == null || !p.isOnline()) return;
    Stan s = st(p);
    s.kto = nazwaPrzeciwnika;
    s.onNazwa = c.nazwaKolor(on.o) + " <gray>Poz. <white>" + on.o.poziom;
    s.onOpis = typy(on) + (on.status != null ? " <light_purple>" + on.status.nazwa() : "") + etapy(on);
    s.onHp = Math.max(0, hpOn); s.onMax = Math.max(1, on.hpMax);
    s.jaNazwa = c.nazwaKolor(ja.o) + " <gray>Poz. <white>" + ja.o.poziom;
    s.jaOpis = typy(ja) + (ja.status != null ? " <light_purple>" + ja.status.nazwa() : "") + etapy(ja);
    s.jaHp = Math.max(0, hpJa); s.jaMax = Math.max(1, ja.hpMax);
    BossBar[] b = paski(p);
    if (mythic()) return;
    b[0].name(Chowance.mm("<red>" + nazwaPrzeciwnika + ": </red>" + c.nazwaKolor(on.o) + " <gray>Poz. " + on.o.poziom + " " + typy(on) + "  <white>" + (int) Math.ceil(hpOn) + "/" + on.hpMax
        + (on.status != null ? " <light_purple>[" + on.status.nazwa() + "]" : "") + etapy(on)));
    b[0].progress(ulamek(hpOn, on.hpMax));
    b[1].name(Chowance.mm("<green>Ty: </green>" + c.nazwaKolor(ja.o) + " <gray>Poz. " + ja.o.poziom + " " + typy(ja) + "  <white>" + (int) Math.ceil(hpJa) + "/" + ja.hpMax
        + (ja.status != null ? " <light_purple>[" + ja.status.nazwa() + "]" : "") + etapy(ja)));
    b[1].progress(ulamek(hpJa, ja.hpMax));
    b[1].color(hpJa * 5 < ja.hpMax ? BossBar.Color.RED : hpJa * 2 < ja.hpMax ? BossBar.Color.YELLOW : BossBar.Color.GREEN);
  }

  /** Typy stwora w kolorach (recenzja R-24: bez nich ▲▼ przy ruchach było zgadywaniem). */
  private static String typy(Wojownik w) {
    StringBuilder sb = new StringBuilder();
    for (var t : w.g.typy()) sb.append(t.kolor()).append(t.nazwa()).append(" ");
    return sb.toString().trim() + "<gray>";
  }

  /** Zmiany statystyk w walce: ATK+1, OBR-2 … */
  private static String etapy(Wojownik w) {
    StringBuilder sb = new StringBuilder();
    String[] skr = {"ATK", "OBR", "ATK.S", "OBR.S", "SZYB", "CEL", "UNIK"};
    for (int i = 0; i < w.etapy.length && i < skr.length; i++) {
      int e = w.etapy[i];
      if (e != 0) sb.append(e > 0 ? " <green>" : " <red>").append(skr[i]).append(e > 0 ? "+" : "").append(e);
    }
    return sb.toString();
  }

  /**
   * Pasek u góry: co się dzieje + postęp (czas tury / pojedynku). W trybie klasycznym dokleja do niego klawisze
   * ustawione przez {@link #klawisze} (jak dawniej, jedna linia).
   */
  public void menu(Player p, String tekst, float postep, BossBar.Color kolor) {
    if (p == null || !p.isOnline()) return;
    Stan s = st(p);
    s.naglowek = tekst;
    BossBar b = paski(p)[2];
    String t = tekst;
    if (!mythic() && !s.klawisze.isEmpty()) t = tekst + "  " + String.join("  ", s.klawisze);
    b.name(Chowance.mm(t));
    b.progress(Math.max(0f, Math.min(1f, postep)));
    b.color(kolor);
  }

  /** Lista klawiszy (pionowo w MythicHUD, w klasycznym — w linii paska). Pusta = nic do wciśnięcia. */
  public void klawisze(Player p, List<String> linie) {
    if (p == null) return;
    st(p).klawisze = List.copyOf(linie);
  }

  /** Jak {@link #ruchy}, ale odnowienie w TURACH (walka turowa). */
  public List<String> ruchyTury(Wojownik ja, Wojownik on, java.util.function.ToIntFunction<Ruch> tury) {
    return ruchy(ja, on, r -> tury.applyAsInt(r) * 1000L - 1, " t.");
  }

  public List<String> ruchy(Wojownik ja, Wojownik on, java.util.function.ToLongFunction<Ruch> odnowienieMs) {
    return ruchy(ja, on, odnowienieMs, " s");
  }

  /** „[1] Iskra ▲”, „[2] Uderzenie · 3 s” — nazwa w kolorze typu, szara z odliczaniem, gdy się odnawia. */
  private List<String> ruchy(Wojownik ja, Wojownik on, java.util.function.ToLongFunction<Ruch> odnowienieMs, String jedn) {
    List<String> l = new ArrayList<>(4);
    for (int i = 0; i < 4; i++) {
      StringBuilder sb = new StringBuilder("<yellow>[").append(i + 1).append("]</yellow> ");
      if (i >= ja.o.ruchy.size()) { l.add(sb.append("<dark_gray>—").toString()); continue; }
      Ruch r = c.rej.ruch(ja.o.ruchy.get(i));
      if (r == null) { l.add(sb.append("<dark_gray>?").toString()); continue; }
      long od = odnowienieMs.applyAsLong(r);
      double sk = r.kategoria() == pl.mcmmorpg.chowance.model.Kategoria.STATUS ? 1 : c.rej.typy().mnoznik(r.typ(), on.g.typy());
      if (od > 0) sb.append("<dark_gray>").append(r.nazwa()).append(" · ").append(od / 1000 + 1).append(jedn);
      else sb.append(r.typ().kolor()).append(r.nazwa()).append(sk >= 2 ? " <green>▲" : sk < 1 ? " <red>▼" : "");
      l.add(sb.toString());
    }
    return l;
  }

  public void ukryj(Player p) {
    stany.remove(p.getUniqueId());
    BossBar[] b = paski.remove(p.getUniqueId());
    if (b != null) for (BossBar x : b) p.hideBossBar(x);
  }

  public void zapomnij(UUID u) { paski.remove(u); stany.remove(u); }

  private static float ulamek(double hp, int max) {
    return max <= 0 ? 0f : (float) Math.max(0, Math.min(1, hp / max));
  }
}
