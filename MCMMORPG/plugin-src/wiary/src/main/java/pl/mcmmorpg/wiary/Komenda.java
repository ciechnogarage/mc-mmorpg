package pl.mcmmorpg.wiary;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
      s.sendMessage("Gracz " + a[1] + " nie jest online.");
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
