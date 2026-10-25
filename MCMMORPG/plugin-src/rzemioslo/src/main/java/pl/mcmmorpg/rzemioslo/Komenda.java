package pl.mcmmorpg.rzemioslo;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** /rzemioslo — wolane przez menu z konsoli (DeluxeMenus [console]) i przez admina. */
final class Komenda implements TabExecutor {
    private final Rzemioslo plugin;

    Komenda(Rzemioslo plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 0) { pomoc(s); return true; }
        switch (a[0].toLowerCase()) {
            case "reload" -> {
                plugin.przeladuj();
                s.sendMessage("[Rzemioslo] przeladowano: " + plugin.dane().wezly.size() + " wezlow, " + plugin.dane().receptury.size()
                        + " receptur, " + plugin.dane().buffy.size() + " buffow; problemy: " + plugin.dane().problemy);
            }
            case "zlecenie" -> {           // zlecenie oddaj <gracz> <prof> <n>
                if (a.length < 5 || !a[1].equalsIgnoreCase("oddaj")) { pomoc(s); return true; }
                Player p = Bukkit.getPlayerExact(a[2]);
                if (p == null) return offline(s, a[2]);
                try { plugin.zlecenia().oddaj(p, a[3].toLowerCase(), Integer.parseInt(a[4])); }
                catch (NumberFormatException e) { s.sendMessage("[Rzemioslo] <n> musi byc liczba calkowita: " + a[4]); }
            }
            case "start" -> {              // start <gracz> <prof>
                if (a.length < 3) { pomoc(s); return true; }
                Player p = Bukkit.getPlayerExact(a[1]);
                if (p == null) return offline(s, a[1]);
                plugin.zlecenia().start(p, a[2].toLowerCase());
            }
            case "przeprawa" -> {          // przeprawa <gracz> <id> <komenda po sukcesie...>
                if (a.length < 4) { pomoc(s); return true; }
                Player p = Bukkit.getPlayerExact(a[1]);
                if (p == null) return offline(s, a[1]);
                String id = a[2].toUpperCase();
                if (!"przeprawa".equals(plugin.dane().uzycia.get(id))) { s.sendMessage("[Rzemioslo] to nie jest zestaw przeprawy: " + id); return true; }
                if (!Przedmioty.zabierz(p, id, 1)) {
                    plugin.pasek(p, plugin.kom("przeprawa-brak", "{nazwa}", plugin.dane().nazwa(id)));
                    return true;
                }
                String cmd = String.join(" ", Arrays.copyOfRange(a, 3, a.length)).replace("{gracz}", p.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                plugin.pasek(p, plugin.kom("przeprawa-uzyta", "{nazwa}", plugin.dane().nazwa(id)));
            }
            case "narzedzie" -> {          // narzedzie <gracz> <rodzaj> <tier>
                if (a.length < 4) { pomoc(s); return true; }
                Player p = Bukkit.getPlayerExact(a[1]);
                if (p == null) return offline(s, a[1]);
                String id = "PROF_" + a[2].toUpperCase() + "_" + a[3];
                if (!plugin.dane().narzedzia.containsKey(id)) { s.sendMessage("[Rzemioslo] brak narzedzia " + id); return true; }
                plugin.przedmioty().daj(p, id, 1);
            }
            case "info" -> {               // info <gracz>
                if (a.length < 2) { pomoc(s); return true; }
                Player p = Bukkit.getPlayerExact(a[1]);
                if (p == null) return offline(s, a[1]);
                StringBuilder b = new StringBuilder("[Rzemioslo] " + p.getName() + ":");
                for (String prof : plugin.dane().nazwyProfesji.keySet()) {
                    b.append(" ").append(prof).append("=").append(Profesje.poziom(p, prof));
                }
                for (String g : List.of("mikstura", "eliksir", "olej", "posilek")) {
                    String o = plugin.buffy().opis(p, g);
                    if (!o.isEmpty()) b.append(" | ").append(g).append(": ").append(o);
                }
                s.sendMessage(b.toString());
            }
            case "doreki" -> {             // doreki <gracz> <itemId> [slot paska] — admin/test: przedmiot na pasek i do reki
                if (a.length < 3) { pomoc(s); return true; }
                Player p = Bukkit.getPlayerExact(a[1]);
                if (p == null) return offline(s, a[1]);
                int cel = 5;
                if (a.length > 3) {
                    try { cel = Integer.parseInt(a[3]); }
                    catch (NumberFormatException e) { s.sendMessage("[Rzemioslo] slot musi byc liczba 0-8: " + a[3]); return true; }
                }
                if (cel < 0 || cel > 8) { s.sendMessage("[Rzemioslo] slot paska musi byc w zakresie 0-8: " + cel); return true; }
                var inv = p.getInventory();
                for (int i = 0; i < 36; i++) {
                    if (a[2].equalsIgnoreCase(Przedmioty.id(inv.getItem(i)))) {
                        var x = inv.getItem(i);
                        var y = inv.getItem(cel);
                        inv.setItem(cel, x);
                        inv.setItem(i, y);
                        inv.setHeldItemSlot(cel);
                        s.sendMessage("[Rzemioslo] doreki " + a[2] + " z " + i + " na " + cel);
                        return true;
                    }
                }
                s.sendMessage("[Rzemioslo] brak " + a[2]);
            }
            case "oboz" -> {               // oboz <gracz> <id> — rozbija oboz pod graczem (test logiki bez PPM)
                if (a.length < 3) { pomoc(s); return true; }
                Player p = Bukkit.getPlayerExact(a[1]);
                Dane.Oboz o = plugin.dane().obozowiska.get(a[2].toUpperCase());
                if (p == null || o == null) { s.sendMessage("[Rzemioslo] brak gracza albo obozowiska"); return true; }
                plugin.buffy().rozbij(p, o, p.getLocation().getBlock().getLocation());
                s.sendMessage("[Rzemioslo] oboz " + o.id() + " dla " + p.getName());
            }
            case "postaw" -> {             // postaw <nexoId> <swiat> <x> <y> <z> — wezel/lowisko z MythicMobs (lochy, wyspy)
                if (a.length < 6) { pomoc(s); return true; }
                String nid = a[1];
                if (!plugin.dane().wezly.containsKey(nid) && !plugin.dane().lowiskoBloki.containsKey(nid)) {
                    s.sendMessage("[Rzemioslo] nieznany wezel " + nid);
                    return true;
                }
                var w = Bukkit.getWorld(a[2]);
                if (w == null) { s.sendMessage("[Rzemioslo] brak swiata " + a[2]); return true; }
                try {
                    Location loc = new Location(w, Double.parseDouble(a[3]), Double.parseDouble(a[4]), Double.parseDouble(a[5]));
                    Block b = loc.getBlock();
                    if (nid.equals(Zbieranie.nexoId(b))) return true;               // juz stoi
                    if (!b.getType().isAir() && !b.isReplaceable()) { s.sendMessage("[Rzemioslo] zajete: " + b.getType()); return true; }
                    com.nexomc.nexo.api.NexoBlocks.place(nid, b.getLocation());
                } catch (NumberFormatException e) {
                    s.sendMessage("[Rzemioslo] zle wspolrzedne");
                }
            }
            case "wezel" -> {              // wezel <x> <y> <z> [swiat] — co to za blok (diagnostyka dla projektantow)
                if (a.length < 4) { pomoc(s); return true; }
                try {
                    var w = a.length > 4 ? Bukkit.getWorld(a[4]) : (s instanceof Player pl ? pl.getWorld() : Bukkit.getWorlds().get(0));
                    if (w == null) { s.sendMessage("[Rzemioslo] brak swiata " + a[4]); return true; }
                    Block b = new Location(w, Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3])).getBlock();
                    String nid = Zbieranie.nexoId(b);
                    Dane.Zrodlo z = nid == null ? null : plugin.dane().wezly.get(nid);
                    s.sendMessage("[Rzemioslo] " + b.getType() + " nexo=" + nid + (z == null ? " (nie wezel)"
                            : " -> " + z.profesja() + " T" + z.tier() + " " + z.glowny() + "/" + z.poboczny()));
                } catch (NumberFormatException e) {
                    s.sendMessage("[Rzemioslo] zle wspolrzedne");
                }
            }
            default -> pomoc(s);
        }
        return true;
    }

    // gracz offline/zly nick: komunikat dla konsoli zamiast ciszy (komendy wolaja menu i NPC z %player_name%)
    private static boolean offline(CommandSender s, String nick) {
        s.sendMessage("[Rzemioslo] gracz nie jest online: " + nick);
        return true;
    }

    private void pomoc(CommandSender s) {
        s.sendMessage("/rzemioslo reload | info <gracz> | zlecenie oddaj <gracz> <prof> <n> | start <gracz> <prof>"
                + " | przeprawa <gracz> <id> <komenda...> | narzedzie <gracz> <rodzaj> <tier> | wezel <x> <y> <z> [swiat]"
                + " | postaw <nexoId> <swiat> <x> <y> <z> | doreki <gracz> <itemId> [slot 0-8] | oboz <gracz> <id>");
    }

    private static final List<String> PODKOMENDY = List.of("reload", "info", "zlecenie", "start", "przeprawa", "narzedzie", "wezel",
            "postaw", "doreki", "oboz");

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        List<String> w = new ArrayList<>();
        if (!s.hasPermission("rzemioslo.admin")) return w;
        String sub = a[0].toLowerCase();
        if (a.length == 1) w.addAll(PODKOMENDY);
        else if (a.length == 2) {
            switch (sub) {
                case "info", "start", "przeprawa", "narzedzie", "doreki", "oboz" -> Bukkit.getOnlinePlayers().forEach(p -> w.add(p.getName()));
                case "zlecenie" -> w.add("oddaj");
                case "postaw" -> { w.addAll(plugin.dane().wezly.keySet()); w.addAll(plugin.dane().lowiskoBloki.keySet()); }
                default -> { }
            }
        } else if (a.length == 3) {
            switch (sub) {
                case "zlecenie" -> Bukkit.getOnlinePlayers().forEach(p -> w.add(p.getName()));
                case "start" -> w.addAll(plugin.dane().startowe.keySet());
                case "narzedzie" -> plugin.dane().narzedzia.values().forEach(n -> { if (!w.contains(n.rodzaj())) w.add(n.rodzaj()); });
                case "oboz" -> w.addAll(plugin.dane().obozowiska.keySet());
                case "postaw" -> Bukkit.getWorlds().forEach(x -> w.add(x.getName()));
                default -> { }
            }
        } else if (a.length == 4 && sub.equals("zlecenie")) w.addAll(plugin.dane().nazwyProfesji.keySet());
        String pref = a[a.length - 1].toLowerCase();
        w.removeIf(x -> !x.toLowerCase().startsWith(pref));
        return w;
    }
}
