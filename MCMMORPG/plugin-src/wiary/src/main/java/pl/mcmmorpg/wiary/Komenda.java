package pl.mcmmorpg.wiary;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * /wiary — wołane przez menu DeluxeMenus jako KONSOLA ([console] wiary ... %player_name%) albo przez admina.
 * Gracz nigdy nie wpisuje tego sam (zasada "menu zamiast komend"); permisja wiary.admin ma domyślnie tylko OP.
 */
final class Komenda implements CommandExecutor, TabCompleter {
  private static final List<String> POD = List.of("wybierz", "modlitwa", "info", "reset", "reload");
  private final WiaryPlugin plugin;

  Komenda(WiaryPlugin plugin) { this.plugin = plugin; }

  @Override
  public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
    if (a.length == 0) {
      s.sendMessage("/wiary wybierz <gracz> <bóstwo> | modlitwa <gracz> | info <gracz> | reset <gracz> | reload");
      return true;
    }
    String sub = a[0].toLowerCase(Locale.ROOT);
    if (sub.equals("reload")) {
      int n = plugin.przeladuj();
      s.sendMessage("Wiary przeładowane, problemów: " + n + " (szczegóły w logu).");
      return true;
    }
    if (a.length < 2) {
      s.sendMessage("Podaj gracza.");
      return true;
    }
    Player p = Bukkit.getPlayerExact(a[1]);
    if (p == null) {
      if (sub.equals("info") || sub.equals("reset")) offline(s, sub, a[1]);
      else s.sendMessage("Gracz " + a[1] + " nie jest online.");
      return true;
    }
    WiaryPlugin.Wynik w;
    switch (sub) {
      case "wybierz" -> {
        if (a.length < 3) {
          s.sendMessage("Podaj bóstwo.");
          return true;
        }
        w = plugin.wybierz(p, a[2]);
      }
      case "modlitwa" -> w = plugin.modlitwa(p);
      case "reset" -> w = plugin.reset(p);
      case "info" -> {
        info(s, p);
        return true;
      }
      default -> {
        s.sendMessage("Nieznana podkomenda " + sub + ".");
        return true;
      }
    }
    plugin.wyslij(p, w.graczowi());
    plugin.getLogger().info("[" + sub + "] " + p.getName() + ": " + (w.ok() ? "OK " : "ODMOWA ") + w.konsoli());
    if (s != Bukkit.getConsoleSender()) s.sendMessage((w.ok() ? "OK: " : "ODMOWA: ") + w.konsoli());
    return true;
  }

  /** info/reset dla gracza offline: dane są w profile/<uuid-profilu>.yml z polem "gracz" (nick); aktywnej postaci offline nie znamy. */
  private void offline(CommandSender s, String sub, String nick) {
    Map<UUID, String> profile = plugin.magazyn().profileGracza(nick);
    if (profile.isEmpty()) {
      s.sendMessage("Gracz " + nick + " nie jest online i nie ma zapisanej wiary (brak pliku profile/ z tym nickiem).");
      return;
    }
    if (sub.equals("reset")) {
      if (profile.size() > 1) {
        s.sendMessage("Gracz " + nick + " jest offline i ma " + profile.size() + " postaci (profile " + profile.keySet()
            + ") — nie wiem, która jest aktywna. Poczekaj, aż wejdzie na serwer, i powtórz.");
        return;
      }
      Map.Entry<UUID, String> e = profile.entrySet().iterator().next();
      try {
        plugin.magazyn().zapisz(e.getKey(), e.getValue(), new Magazyn.Dane());
        plugin.getLogger().info("[reset] " + e.getValue() + " (offline, profil " + e.getKey() + "): wyczyszczono");
        s.sendMessage("OK: wyczyszczono wiarę " + e.getValue() + " (offline, profil " + e.getKey() + ").");
      } catch (IOException ex) {
        plugin.getLogger().severe("Zapis wiary " + e.getValue() + " (" + e.getKey() + "): " + ex);
        s.sendMessage("Błąd zapisu: " + ex.getMessage());
      }
      return;
    }
    s.sendMessage("Wiary " + nick + " (offline, bez klasy/rasy/wiary i statów): postaci z zapisaną wiarą: " + profile.size());
    for (Map.Entry<UUID, String> e : profile.entrySet()) {
      Magazyn.Dane d = plugin.magazyn().wczytaj(e.getKey());
      long zostalo = d.laskaDo - System.currentTimeMillis();
      s.sendMessage(" profil=" + e.getKey() + " patron=" + d.patron + " łaska=" + d.laskaBostwo + " ("
          + (zostalo > 0 ? Panteon.czas(zostalo) : "brak") + ", wiara " + d.laskaWiara + ")");
    }
  }

  private void info(CommandSender s, Player p) {
    WiaryPlugin.Stan st = plugin.stan(p);
    Panteon pan = plugin.panteon();
    s.sendMessage("Wiary " + p.getName() + ": klasa=" + plugin.klasa(p) + " rasa=" + plugin.rasa(p) + " wiara=" + plugin.wiara(p)
        + " profil=" + (st == null ? "brak" : st.profil));
    if (st == null) return;
    long zostalo = st.dane.laskaDo - System.currentTimeMillis();
    s.sendMessage(" patron=" + st.dane.patron + " łaska=" + st.dane.laskaBostwo + " (" + (zostalo > 0 ? Panteon.czas(zostalo) : "brak")
        + ", wiara " + st.dane.laskaWiara + ") modyfikatorów=" + st.mody.size());
    Panteon.Bostwo b = pan.bostwo(st.dane.laskaBostwo != null ? st.dane.laskaBostwo : st.dane.patron);
    if (b == null) return;
    MMOPlayerData mmo = MMOPlayerData.get(p);
    for (Panteon.Efekt e : b.efekty()) {
      s.sendMessage(" stat " + e.stat() + " = " + mmo.getStatMap().getStat(e.stat()));
    }
  }

  @Override
  public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
    List<String> out = new ArrayList<>();
    if (a.length == 1) {
      for (String x : POD) if (x.startsWith(a[0].toLowerCase(Locale.ROOT))) out.add(x);
    } else if (a.length == 2 && !a[0].equalsIgnoreCase("reload")) {
      for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase(Locale.ROOT).startsWith(a[1].toLowerCase(Locale.ROOT))) out.add(p.getName());
    } else if (a.length == 3 && a[0].equalsIgnoreCase("wybierz")) {
      for (String id : plugin.panteon().bostwa().keySet()) if (id.startsWith(a[2].toLowerCase(Locale.ROOT))) out.add(id);
    }
    return out;
  }
}
