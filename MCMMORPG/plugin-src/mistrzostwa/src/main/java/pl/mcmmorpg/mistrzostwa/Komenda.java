package pl.mcmmorpg.mistrzostwa;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /mistrzostwo — wywolywane przez menu DeluxeMenus Z KONSOLI ([console] ... %player_name%),
 * gracz nigdy nie wpisuje nic w czacie. Wszystkie reguly sa sprawdzane TUTAJ, nie w menu:
 * menu tylko pokazuje stan, wiec obejscie menu nic nie daje.
 *
 * Podwojne klikniecie: komendy ida na watku glownym jedna po drugiej, a zapis stanu (LuckPerms
 * User.data()) jest natychmiastowy w pamieci — druga komenda widzi juz skutek pierwszej. Dodatkowo
 * blokada 1,5 s po udanej operacji tego gracza, zeby druga komenda nie zamykala nowo otwartego menu.
 *
 * Okna potwierdzen nie dostaja argumentow: DeluxeMenus 1.14.1 'dm open' przyjmuje tylko
 * <menu> [gracz] [-p:gracz] (bajtkod OpenCommand) — 'args' dzialaja wylacznie przez open_command.
 * Dlatego plugin ZAPAMIETUJE cel okna per gracz (i per profil postaci) w chwili jego otwarcia,
 * okno czyta go z %mistrz_cel_*% / %mistrz_z_*%, a przycisk "Tak" wola 'potwierdz' bez argumentow.
 */
final class Komenda implements CommandExecutor, TabCompleter, Listener {
    private final Mistrzostwa plugin;
    private final Map<UUID, Long> ostatniaOperacja = new ConcurrentHashMap<>();
    /** cel otwartego okna: profesja do zdobycia, porzucana (albo null) i profil, dla ktorego okno otwarto */
    record Oczekujace(String cel, String z, UUID profil) {}
    private final Map<UUID, Oczekujace> oczekujace = new ConcurrentHashMap<>();

    Oczekujace oczekujace(Player p) {
        Oczekujace o = oczekujace.get(p.getUniqueId());
        return o != null && java.util.Objects.equals(o.profil(), Stan.profil(p)) ? o : null;
    }

    Komenda(Mistrzostwa plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void naWyjscie(PlayerQuitEvent e) {
        ostatniaOperacja.remove(e.getPlayer().getUniqueId());
        oczekujace.remove(e.getPlayer().getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        // komendy wola menu z konsoli albo admin (gracz / RCON); blok komend, minecart itp. nie
        if (!(s instanceof Player) && !(s instanceof ConsoleCommandSender) && !(s instanceof RemoteConsoleCommandSender)) {
            s.sendMessage("[Mistrzostwa] komenda dostepna tylko z konsoli albo dla gracza");
            return true;
        }
        if (a.length == 0) { pomoc(s); return true; }
        String sub = a[0].toLowerCase(Locale.ROOT);
        if (sub.equals("reload")) { plugin.przeladuj(); s.sendMessage("[Mistrzostwa] przeladowano config.yml"); return true; }
        if (a.length < 2) { pomoc(s); return true; }
        Player p = Bukkit.getPlayerExact(a[1]);
        if (p == null) { s.sendMessage("[Mistrzostwa] gracz offline: " + a[1]); return true; }
        Ustawienia u = plugin.ustawienia();
        switch (sub) {
            case "okno" -> { if (a.length >= 3) okno(s, p, a[2].toLowerCase(Locale.ROOT), u); else pomoc(s); }
            case "wybierz" -> { if (a.length >= 3) wybierz(s, p, a[2].toLowerCase(Locale.ROOT), u); else pomoc(s); }
            case "zmien" -> { if (a.length >= 4) zmien(s, p, a[2].toLowerCase(Locale.ROOT), a[3].toLowerCase(Locale.ROOT), u); else pomoc(s); }
            case "info" -> info(s, p, u);
            case "porzuc" -> { if (a.length >= 3) porzuc(s, p, a[2], u); else pomoc(s); }
            case "potwierdz" -> potwierdz(s, p, u);
            case "nadaj" -> {
                if (a.length < 3) { pomoc(s); break; }
                String id = a[2].toLowerCase(Locale.ROOT);
                if (!u.mistrzowskie.contains(id)) { s.sendMessage("[Mistrzostwa] nie mistrzowska: " + id); break; }
                s.sendMessage("[Mistrzostwa] nadaj " + id + ": " + (plugin.stan().dodaj(p, id) ? "OK" : "BRAK PROFILU"));
                log("ADMIN nadaj", p, id, "-", 0);
            }
            case "odbierz" -> {
                if (a.length < 3) { pomoc(s); break; }
                String id = a[2].toLowerCase(Locale.ROOT);
                s.sendMessage("[Mistrzostwa] odbierz " + id + ": " + (plugin.stan().usun(p, id) ? "OK" : "nie bylo"));
                log("ADMIN odbierz", p, id, "-", 0);
            }
            case "kamien" -> { if (a.length >= 4) kamien(s, p, a[2].toLowerCase(Locale.ROOT), a[3], u); else pomoc(s); }
            case "zdejmij-cooldown" -> s.sendMessage("[Mistrzostwa] cooldown: " + (plugin.stan().zdejmijCooldown(p) ? "zdjety" : "nie bylo"));
            default -> pomoc(s);
        }
        return true;
    }

    private void pomoc(CommandSender s) {
        s.sendMessage("/mistrzostwo okno <gracz> <profesja> | wybierz <gracz> <profesja> | zmien <gracz> <z> <na>");
        s.sendMessage("/mistrzostwo porzuc <gracz> <nr 1..limit> | potwierdz <gracz> | kamien <gracz> <prof> <poziom>");
        s.sendMessage("/mistrzostwo info <gracz> | nadaj <gracz> <prof> | odbierz <gracz> <prof> | zdejmij-cooldown <gracz> | reload");
    }

    // ------------------------------------------------------------------ okno

    /** PPM na kafelku profesji: plugin decyduje, ktore okno (lub jaki komunikat) pokazac. */
    private void okno(CommandSender s, Player p, String id, Ustawienia u) {
        if (u.ogolne.contains(id)) { odmowa(p, "&f" + Stan.nazwa(id) + "&7 to profesja &aogólna&7 — rośnie do 100 bez wyboru."); return; }
        if (!u.mistrzowskie.contains(id)) { odmowa(p, "Ta profesja nie ma mistrzostwa."); return; }
        if (Stan.profil(p) == null) { odmowa(p, "Najpierw wybierz postać."); return; }
        Stan st = plugin.stan();
        Set<String> m = st.mistrzostwa(p);
        if (m.contains(id)) { odmowa(p, "&f" + Stan.nazwa(id) + "&7 już jest twoim mistrzostwem."); return; }
        int lvl = Stan.poziom(p, id);
        if (lvl < u.prog) { odmowa(p, "Mistrzostwo można wybrać od poziomu &f" + u.prog + "&7 (&f" + Stan.nazwa(id) + "&7: " + lvl + ")."); return; }
        oczekujace.put(p.getUniqueId(), new Oczekujace(id, null, Stan.profil(p)));
        String menu = m.size() < u.limit ? u.menuWybor : u.menuZmiana;
        otworz(p, menu + " " + p.getName());
    }

    /** Okno zamiany: gracz wskazal, ktore (1..limit) mistrzostwo porzuca -> okno "Czy na pewno?". */
    private void porzuc(CommandSender s, Player p, String nr, Ustawienia u) {
        Oczekujace o = oczekujace(p);
        if (o == null) { if (s != p) s.sendMessage("[Mistrzostwa] " + p.getName() + " nie ma oczekujacej zmiany."); zamknij(p); return; }
        java.util.List<String> m = new java.util.ArrayList<>(plugin.stan().mistrzostwa(p));
        int n;
        try { n = Integer.parseInt(nr); } catch (NumberFormatException e) { s.sendMessage("[Mistrzostwa] <nr> musi byc liczba calkowita: " + nr); return; }
        if (n < 1 || n > m.size()) { odmowa(p, "Brak mistrzostwa na tej pozycji."); zamknij(p); return; }
        oczekujace.put(p.getUniqueId(), new Oczekujace(o.cel(), m.get(n - 1), o.profil()));
        otworz(p, u.menuZmiana + "_potw " + p.getName());
    }

    /** Przycisk "Tak" w oknie wyboru albo zamiany. Bez oczekujacego celu = cisza (np. drugie klikniecie). */
    private void potwierdz(CommandSender s, Player p, Ustawienia u) {
        Oczekujace o = oczekujace(p);
        if (o == null) { if (s != p) s.sendMessage("[Mistrzostwa] " + p.getName() + " nie ma oczekujacej zmiany."); if (!zablokowane(p)) zamknij(p); return; }
        if (!u.sab("klikniecia")) oczekujace.remove(p.getUniqueId());
        if (o.z() == null) wybierz(s, p, o.cel(), u);
        else zmien(s, p, o.z(), o.cel(), u);
    }

    // --------------------------------------------------------------- wybierz

    private void wybierz(CommandSender s, Player p, String id, Ustawienia u) {
        if (zablokowane(p) && !u.sab("klikniecia")) return;
        String blad = walidujCel(p, id, u);
        Stan st = plugin.stan();
        Set<String> m = st.mistrzostwa(p);
        if (blad == null && m.size() >= u.limit && !u.sab("limit"))
            blad = "Masz już &f" + m.size() + "/" + u.limit + "&7 mistrzostw. Możesz tylko &ezmienić&7 jedno z nich.";
        if (blad != null) { odmowa(p, blad); zamknij(p); return; }
        if (!st.dodaj(p, id)) { odmowa(p, "Nie udało się zapisać wyboru (brak profilu)."); zamknij(p); return; }
        ostatniaOperacja.put(p.getUniqueId(), System.currentTimeMillis());
        log("WYBOR", p, "-", id, 0);
        sukces(p, "&f" + Stan.nazwa(id) + "&7 jest teraz twoim mistrzostwem (&f" + (m.size() + 1) + "/" + u.limit + "&7). Rozwój do &f100&7 odblokowany.");
        otworz(p, u.menuGlowne + " " + p.getName());
    }

    // ----------------------------------------------------------------- zmien

    private void zmien(CommandSender s, Player p, String z, String na, Ustawienia u) {
        if (zablokowane(p) && !u.sab("klikniecia")) return;
        Stan st = plugin.stan();
        Set<String> m = st.mistrzostwa(p);
        String blad = u.sab("klikniecia") ? null : walidujCel(p, na, u);
        if (blad == null && !m.contains(z) && !u.sab("klikniecia")) blad = "&f" + Stan.nazwa(z) + "&7 nie jest twoim mistrzostwem.";
        if (blad == null && !u.sab("cooldown") && !u.sab("klikniecia")) {
            Instant cd = st.cooldownDo(p);
            if (cd != null && cd.isAfter(Instant.now()))
                blad = "Mistrzostwo można zmieniać raz na &f" + czas(Duration.ofHours(u.cooldownGodziny)) + "&7. Następna zmiana za &f" + czas(Duration.between(Instant.now(), cd)) + "&7.";
        }
        Economy eco = plugin.ekonomia();
        double koszt = u.sab("koszt") ? 0 : u.koszt;
        if (blad == null && koszt > 0 && eco == null) blad = "Ekonomia niedostępna — zmiana wstrzymana.";
        if (blad == null && koszt > 0) {
            double saldo = eco.getBalance(p);
            if (saldo + 1e-9 < koszt)
                blad = "Zmiana kosztuje &f" + kwota(u.koszt) + " $&7, a masz &f" + kwota(saldo) + " $&7. Brakuje &c" + kwota(u.koszt - saldo) + " $&7.";
        }
        if (blad != null) { odmowa(p, blad); zamknij(p); return; }
        if (koszt > 0) {
            EconomyResponse r = eco.withdrawPlayer(p, koszt);
            if (r == null || !r.transactionSuccess()) {
                odmowa(p, "Płatność odrzucona" + (r != null && r.errorMessage != null ? ": " + r.errorMessage : "") + ". Nic nie zmieniono.");
                zamknij(p);
                return;
            }
        }
        if (!st.zamien(p, z, na, u.sab("cooldown") ? Duration.ZERO : Duration.ofHours(u.cooldownGodziny))) {
            if (koszt > 0) eco.depositPlayer(p, koszt); // zwrot — nie zostawiamy gracza bez pieniedzy i bez zmiany
            odmowa(p, "Nie udało się zapisać zmiany (brak profilu). Pieniądze zwrócone.");
            zamknij(p);
            return;
        }
        ostatniaOperacja.put(p.getUniqueId(), System.currentTimeMillis());
        log("ZMIANA", p, z, na, koszt);
        sukces(p, "Zamieniono mistrzostwo: &f" + Stan.nazwa(z) + "&7 → &f" + Stan.nazwa(na) + "&7. "
                + "&f" + Stan.nazwa(z) + "&7 zostaje na poziomie &f" + Stan.poziom(p, z) + "&7, ale dalej nie rośnie. "
                + "Pobrano &f" + kwota(u.koszt) + " $&7. Następna zmiana za &f" + czas(Duration.ofHours(u.cooldownGodziny)) + "&7.");
        otworz(p, u.menuGlowne + " " + p.getName());
    }

    /** Wspolne warunki dla profesji, ktora ma ZOSTAC mistrzostwem. */
    private String walidujCel(Player p, String id, Ustawienia u) {
        if (u.ogolne.contains(id)) return "&f" + Stan.nazwa(id) + "&7 to profesja ogólna — nie zajmuje miejsca mistrzostwa.";
        if (!u.mistrzowskie.contains(id)) return "Nieznana profesja: " + id;
        if (Stan.profil(p) == null) return "Najpierw wybierz postać.";
        if (plugin.stan().jestMistrzem(p, id)) return "&f" + Stan.nazwa(id) + "&7 już jest twoim mistrzostwem.";
        int lvl = Stan.poziom(p, id);
        if (lvl < u.prog) return "Mistrzostwo można wybrać od poziomu &f" + u.prog + "&7 (" + Stan.nazwa(id) + ": " + lvl + ").";
        return null;
    }

    /** Wolane z exp-table MMOCore (command{}) przy kamieniu milowym; oglasza nowy tytul. */
    private void kamien(CommandSender s, Player p, String id, String poziomTxt, Ustawienia u) {
        int poz;
        try { poz = Integer.parseInt(poziomTxt); } catch (NumberFormatException e) { s.sendMessage("[Mistrzostwa] <poziom> musi byc liczba calkowita: " + poziomTxt); return; }
        String t = u.tytul(id, poz);
        if (t.isEmpty() || !u.tytuly.containsKey(poz)) return;
        if (poz >= u.ogloszenieOd)
            Bukkit.broadcast(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(
                    Ustawienia.kolor("&6&l✦ &e" + p.getName() + " &7zdobywa tytuł &6" + t + "&7 (" + Stan.nazwa(id) + " " + poz + ")")));
        else sukces(p, "Nowy tytuł: &6" + t + "&7.");
    }

    private void info(CommandSender s, Player p, Ustawienia u) {
        Stan st = plugin.stan();
        UUID pid = Stan.profil(p);
        Instant cd = st.cooldownDo(p);
        s.sendMessage("[Mistrzostwa] " + p.getName() + " profil=" + pid + " mistrzostwa=" + st.mistrzostwa(p)
                + " cooldown=" + (cd == null ? "brak" : cd + " (" + czas(Duration.between(Instant.now(), cd)) + ")")
                + " saldo=" + (plugin.ekonomia() == null ? "?" : kwota(plugin.ekonomia().getBalance(p))));
        List<String> bez = st.wezlyBezKontekstu(p);
        if (!bez.isEmpty()) s.sendMessage("[Mistrzostwa] UWAGA: wezly bez kontekstu profilu (ignorowane): " + bez);
    }

    // --------------------------------------------------------------- pomocnicze

    private boolean zablokowane(Player p) {
        Long t = ostatniaOperacja.get(p.getUniqueId());
        return t != null && System.currentTimeMillis() - t < 1500;
    }

    private void otworz(Player p, String argumenty) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (p.isOnline()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dm open " + argumenty);
        });
    }

    private void zamknij(Player p) {
        Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) p.closeInventory(); });
    }

    private void odmowa(Player p, String tekst) {
        p.sendMessage(plugin.ustawienia().prefiks + Ustawienia.kolor("&7" + tekst));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f);
    }

    private void sukces(Player p, String tekst) {
        p.sendMessage(plugin.ustawienia().prefiks + Ustawienia.kolor("&7" + tekst));
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);
    }

    private void log(String co, Player p, String z, String na, double koszt) {
        plugin.historia(co + " gracz=" + p.getName() + " uuid=" + p.getUniqueId() + " profil=" + Stan.profil(p)
                + " z=" + z + " na=" + na + " koszt=" + kwota(koszt) + " mistrzostwa=" + plugin.stan().mistrzostwa(p));
    }

    static String kwota(double d) {
        return (d == Math.rint(d)) ? String.valueOf((long) d) : String.format(Locale.ROOT, "%.2f", d);
    }

    static String czas(Duration d) {
        long min = Math.max(1, (d.toSeconds() + 59) / 60);
        long dni = min / 1440, h = (min % 1440) / 60, m = min % 60;
        if (dni > 0) return dni + " d" + (h > 0 ? " " + h + " h" : "");
        if (h > 0) return h + " h" + (m > 0 ? " " + m + " min" : "");
        return m + " min";
    }

    private static final List<String> PODKOMENDY = List.of("okno", "porzuc", "potwierdz", "wybierz", "zmien", "info", "nadaj", "odbierz",
            "zdejmij-cooldown", "kamien", "reload");

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        List<String> w = new ArrayList<>();
        if (!s.hasPermission("mistrzostwa.admin")) return w;
        String sub = a[0].toLowerCase(Locale.ROOT);
        if (a.length == 1) w.addAll(PODKOMENDY);
        else if (a.length == 2) { if (!sub.equals("reload")) Bukkit.getOnlinePlayers().forEach(p -> w.add(p.getName())); }
        else if (a.length == 3) {
            switch (sub) {
                case "okno", "wybierz", "zmien", "nadaj", "odbierz", "kamien" -> w.addAll(plugin.ustawienia().mistrzowskie);
                case "porzuc" -> { for (int i = 1; i <= plugin.ustawienia().limit; i++) w.add(String.valueOf(i)); }
                default -> { }
            }
        } else if (a.length == 4) {
            switch (sub) {
                case "zmien" -> w.addAll(plugin.ustawienia().mistrzowskie);
                case "kamien" -> plugin.ustawienia().tytuly.keySet().forEach(k -> w.add(String.valueOf(k)));
                default -> { }
            }
        }
        String pref = a[a.length - 1].toLowerCase(Locale.ROOT);
        w.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(pref));
        return w;
    }
}
