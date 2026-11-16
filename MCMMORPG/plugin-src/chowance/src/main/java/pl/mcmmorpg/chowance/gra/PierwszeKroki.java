package pl.mcmmorpg.chowance.gra;

import java.util.Map;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.api.event.ChowaniecPrzywolanyEvent;
import pl.mcmmorpg.chowance.api.event.ChowaniecZlapanyEvent;
import pl.mcmmorpg.chowance.api.event.WalkaZakonczonaEvent;
import pl.mcmmorpg.chowance.dane.ProfilDane;
import pl.mcmmorpg.chowance.logika.Kroki;

/** Cztery zadania zapisane w profilu; kroki można wykonać w dowolnej kolejności. */
public final class PierwszeKroki implements Listener {
  private final Chowance c;
  public PierwszeKroki(Chowance c) { this.c = c; }

  public void inicjuj(Player p, ProfilDane d) {
    if (d.pierwszeKrokiWersja != 0) return;
    d.pierwszeKrokiWersja = 1;
    if (d.starter) d.pierwszeKrokiMaska = Kroki.WSZYSTKIE;
    d.brudne = true;
  }

  public void poWczytaniu(Player p, ProfilDane d) {
    inicjuj(p, d);
    if (!c.ust.pierwszeKrokiWlaczone || !d.starter || Kroki.nastepny(d.pierwszeKrokiMaska) == 0) return;
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      if (p.isOnline() && c.dane(p) == d) wskazowka(p, d);
    }, 100L);
  }

  public void poStarterze(Player p, ProfilDane d) {
    // Nowy profil inicjujemy przed ustawieniem starter=true; nie nadaje to nagród weteranom.
    if (d.pierwszeKrokiWersja == 0) {
      d.pierwszeKrokiWersja = 1;
      d.brudne = true;
    }
    if (c.ust.pierwszeKrokiWlaczone) Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      if (p.isOnline() && c.dane(p) == d) wskazowka(p, d);
    }, 1L);
  }

  public void zalicz(Player p, int krok) {
    if (!c.ust.pierwszeKrokiWlaczone || krok < 1 || krok > 4) return;
    ProfilDane d = c.dane(p);
    if (d == null || !d.starter) return;
    inicjuj(p, d);
    if (Kroki.zaliczony(d.pierwszeKrokiMaska, krok)) return;
    d.pierwszeKrokiMaska = Kroki.zalicz(d.pierwszeKrokiMaska, krok);
    boolean nagroda = d.pierwszeKrokiNagrody.add(krok);
    d.brudne = true;
    // Flagi zapisywane przed komendami, aby ponowny event nie wypłacił ich drugi raz.
    // Nagroda dopiero po zapisie flagi na dysk: crash między wypłatą a zapisem nie da drugiej wypłaty.
    String nick = p.getName();
    c.dane.zapisz(d).whenComplete((v, ex) -> Bukkit.getScheduler().runTask(c.plugin, () -> {
      if (ex != null) { c.log.warning("Pierwsze kroki: zapis profilu " + d.profil + " nieudany — nagroda kroku " + krok + " wstrzymana"); return; }
      if (nagroda) c.komendyKonsoli(c.ust.krokiNagrody.get(krok - 1), Map.of("gracz", nick));
    }));
    String opis = c.ust.krokiOpisy.get(krok - 1);
    int dalej = Kroki.nastepny(d.pierwszeKrokiMaska);
    // Pasek akcji mieści ~60 znaków i znika po 1,5 s (HUD MMOCore) — tylko krótki opis; pełna wskazówka na czacie
    // (klient QA 03.10: długi tekst wychodził poza ekran z obu stron).
    c.pasek(p, dalej == 0 ? "<gold>Wszystkie pierwsze kroki ukończone!" : "<green>Krok " + krok + "/4 ✔ <gray>· Dalej: <white>" + c.ust.krokiOpisy.get(dalej - 1));
    if (dalej > 0) c.msg(p, "<gold>[Pierwsze kroki " + Kroki.postep(d.pierwszeKrokiMaska) + "/4]</gold> <white>" + c.ust.krokiWskazowki.get(dalej - 1));
    p.showTitle(net.kyori.adventure.title.Title.title(Chowance.mm(dalej == 0 ? "<gold>Gratulacje!" : "<green>Krok " + krok + "/4 ukończony"), Chowance.mm(dalej == 0 ? "<gray>Pierwsze kroki za tobą" : "<gray>" + opis)));
    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);
    if (dalej == 0) Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      if (p.isOnline()) c.msg(p, c.ust.krokiCoDalej);
    }, 60L);
  }

  public void wskazowka(Player p, ProfilDane d) {
    if (!c.ust.pierwszeKrokiWlaczone || !d.starter) return;
    int n = Kroki.nastepny(d.pierwszeKrokiMaska);
    if (n == 0) return;
    // MythicHUD pokazuje ten sam bieżący cel przy włączonym ustawieniu ui_goal.
    // Gdy gracz ukryje cel, actionbar pozostaje krótką wskazówką zastępczą.
    if (!celNaHud(p)) c.pasek(p, "<gold>Pierwsze kroki " + Kroki.postep(d.pierwszeKrokiMaska) + "/4: <white>" + c.ust.krokiOpisy.get(n - 1));
    c.msg(p, "<gold>[Pierwsze kroki " + Kroki.postep(d.pierwszeKrokiMaska) + "/4]</gold> <white>" + c.ust.krokiWskazowki.get(n - 1));
  }

  private boolean celNaHud(Player p) {
    if (p.getGameMode() == GameMode.SPECTATOR || !Bukkit.getPluginManager().isPluginEnabled("MythicHUD")
        || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return false;
    return !"off".equalsIgnoreCase(PlaceholderAPI.setPlaceholders(p, "%luckperms_meta_ui_goal%"));
  }

  @EventHandler public void przywolanie(ChowaniecPrzywolanyEvent e) { zalicz(e.getPlayer(), 1); }
  @EventHandler public void zlapany(ChowaniecZlapanyEvent e) {
    if ("DZIKI".equals(e.getSposob()) || "OSWOJONY".equals(e.getSposob())) zalicz(e.getPlayer(), 4);
  }
  @EventHandler public void walka(WalkaZakonczonaEvent e) {
    if (e.getZwyciezca() != null && e.getTrenerId() != null && ("TRENER".equals(e.getRodzaj()) || "POJEDYNEK".equals(e.getRodzaj()))) zalicz(e.getZwyciezca(), 3);
  }
}
