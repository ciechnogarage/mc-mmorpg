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
                if (p == null) return true;
                try { plugin.zlecenia().oddaj(p, a[3].toLowerCase(), Integer.parseInt(a[4])); } catch (NumberFormatException ignored) { }
            }
            case "start" -> {              // start <gracz> <prof>
                if (a.length < 3) { pomoc(s); return true; }
                Player p = Bukkit.getPlayerExact(a[1]);
                if (p != null) plugin.zlecenia().start(p, a[2].toLowerCase());
            }
            case "przeprawa" -> {          // przeprawa <gracz> <id> <komenda po sukcesie...>
                if (a.length < 4) { pomoc(s); return true; }
                Player p = Bukkit.getPlayerExact(a[1]);
                if (p == null) return true;
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
                if (p == null) return true;
                String id = "PROF_" + a[2].toUpperCase() + "_" + a[3];
                if (!plugin.dane().narzedzia.containsKey(id)) { s.sendMessage("[Rzemioslo] brak narzedzia " + id); return true; }
                plugin.przedmioty().daj(p, id, 1);
            }
            case "info" -> {               // info <gracz>
                if (a.length < 2) { pomoc(s); return true; }
                Player p = Bukkit.getPlayerExact(a[1]);
                if (p == null) return true;
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
                if (p == null) return true;
                int cel = a.length > 3 ? Integer.parseInt(a[3]) : 5;
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
                boolean kepaZielarza = nid.startsWith("zielarz_") && com.nexomc.nexo.api.NexoBlocks.isCustomBlock(nid);
                if (!kepaZielarza && !plugin.dane().wezly.containsKey(nid) && !plugin.dane().lowiskoBloki.containsKey(nid)) {
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
            case "kepa" -> {               // kepa <gatunek|usun> — kepa zielarza na bloku pod celownikiem (admin, teren testowy)
                if (!(s instanceof Player p) || a.length < 2) { s.sendMessage("kepa <" + String.join("|", Zielarz.GATUNKI) + "|usun>"); return true; }
                Block b = p.getTargetBlockExact(6);
                if (b == null) { s.sendMessage("Patrz na blok rosliny."); return true; }
                s.sendMessage("[Rzemioslo] " + plugin.zielarz().ustawKepe(b, a[1].equalsIgnoreCase("usun") ? null : a[1].toLowerCase()));
            }
            case "wzor" -> {               // wzor <A|B|C|usun> <swiat> <x> <y> <z> <skala> — warianty wygladu dziurawca (admin, test)
                if (a.length >= 2 && a[1].equalsIgnoreCase("usun")) { plugin.zielarz().wzory.usunWszystko(); s.sendMessage("[Rzemioslo] wzory usuniete"); return true; }
                if (a.length < 7) { s.sendMessage("wzor <A|B|C|usun> <swiat> <x> <y> <z> <skala>"); return true; }
                var w = Bukkit.getWorld(a[2]);
                if (w == null) { s.sendMessage("brak swiata"); return true; }
                s.sendMessage("[Rzemioslo] " + plugin.zielarz().wzory.postaw(a[1].toUpperCase(), w, Double.parseDouble(a[3]), Double.parseDouble(a[4]), Double.parseDouble(a[5]), Float.parseFloat(a[6])));
            }
            case "kepa-xyz" -> {           // kepa-xyz <gatunek> <swiat> <x> <y> <z> — z konsoli (skrypt terenu)
                if (a.length < 6) { s.sendMessage("kepa-xyz <gatunek> <swiat> <x> <y> <z>"); return true; }
                var w = Bukkit.getWorld(a[2]);
                if (w == null) { s.sendMessage("brak swiata"); return true; }
                Block b = w.getBlockAt(Integer.parseInt(a[3]), Integer.parseInt(a[4]), Integer.parseInt(a[5]));
                s.sendMessage("[Rzemioslo] " + plugin.zielarz().ustawKepe(b, a[1].toLowerCase()));
            }
            case "suszarnia" -> {          // suszarnia [usun] — suszarnia zielarza na bloku pod celownikiem (admin)
                if (!(s instanceof Player p)) return true;
                Block b = p.getTargetBlockExact(6);
                if (b == null) { s.sendMessage("Patrz na blok."); return true; }
                s.sendMessage("[Rzemioslo] " + plugin.zielarz().suszarnia.ustaw(b, a.length > 1 && a[1].equalsIgnoreCase("usun")));
            }
            case "suszarnia-xyz" -> {      // suszarnia-xyz <swiat> <x> <y> <z> — z konsoli
                if (a.length < 5) return true;
                var w = Bukkit.getWorld(a[1]);
                if (w == null) { s.sendMessage("brak swiata"); return true; }
                s.sendMessage("[Rzemioslo] " + plugin.zielarz().suszarnia.ustaw(w.getBlockAt(Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4])), false));
            }
            case "kepa-start" -> {         // kepa-start <gracz> <x> <y> <z> — test: otwiera kepe bez celowania w drobny kwiat
                Player p = a.length > 4 ? Bukkit.getPlayerExact(a[1]) : null;
                if (p == null) return true;
                Block b = p.getWorld().getBlockAt(Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4]));
                s.sendMessage("[Rzemioslo] " + plugin.zielarz().startTest(p, b));
            }
            case "karty" -> {              // karty przedmiot|blok — test wygladu kart zblizenia
                ZRoslina.JAKO_PRZEDMIOT = a.length < 2 || a[1].equalsIgnoreCase("przedmiot");
                s.sendMessage("karty jako przedmiot=" + ZRoslina.JAKO_PRZEDMIOT);
            }
            case "celuj" -> {              // celuj <gracz> <typ> <nr> [y] — test: kamera na czesc rosliny w trakcie zbioru
                Player p = a.length > 3 ? Bukkit.getPlayerExact(a[1]) : null;
                if (p == null) return true;
                s.sendMessage(plugin.zielarz().celuj(p, a[2], Integer.parseInt(a[3]), a.length > 4 ? Double.parseDouble(a[4]) : 0.5));
            }
            case "okaz" -> {               // okaz <gracz> — test: czesci okazu i ich cechy
                Player p = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : null;
                if (p != null) s.sendMessage(plugin.zielarz().opis(p));
            }
            case "zielnik" -> {            // zielnik <gracz> — okno zielnika (menu Q wola z konsoli)
                Player p = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : (s instanceof Player pp ? pp : null);
                if (p == null) { s.sendMessage("brak gracza"); return true; }
                plugin.zielarz().zielnik.pokaz(p);
            }
            case "wezel" -> {              // wezel <x> <y> <z> [swiat] — co to za blok (diagnostyka dla projektantow)
                if (a.length < 4) { pomoc(s); return true; }
                try {
                    var w = a.length > 4 ? Bukkit.getWorld(a[4]) : (s instanceof Player pl ? pl.getWorld() : Bukkit.getWorlds().get(0));
                    Block b = new Location(w, Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3])).getBlock();
                    String nid = Zbieranie.nexoId(b);
                    Dane.Zrodlo z = nid == null ? null : plugin.dane().wezly.get(nid);
                    s.sendMessage("[Rzemioslo] " + b.getType() + " nexo=" + nid + (z == null ? " (nie wezel)"
                            : " -> " + z.profesja() + " T" + z.tier() + " " + z.glowny() + "/" + z.poboczny()));
                } catch (Exception e) {
                    s.sendMessage("[Rzemioslo] zle wspolrzedne");
                }
            }
            default -> pomoc(s);
        }
        return true;
    }

    private void pomoc(CommandSender s) {
        s.sendMessage("/rzemioslo reload | info <gracz> | zlecenie oddaj <gracz> <prof> <n> | start <gracz> <prof>"
                + " | przeprawa <gracz> <id> <komenda...> | narzedzie <gracz> <rodzaj> <tier> | wezel <x> <y> <z> [swiat]"
                + " | postaw <nexoId> <swiat> <x> <y> <z>");
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        List<String> w = new ArrayList<>();
        if (a.length == 1) w.addAll(List.of("reload", "info", "zlecenie", "start", "przeprawa", "narzedzie", "wezel", "postaw"));
        return w;
    }
}
