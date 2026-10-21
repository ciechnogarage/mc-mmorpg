package pl.mcmmorpg.mistrzostwa;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * %mistrz_...% dla menu. Menu NICZEGO nie rozstrzyga — tylko pokazuje; reguly sa w Komenda.
 *
 *  liczba, limit, koszt, cooldown, saldo
 *  stan_<id>   rozwoj | do_wyboru | do_zmiany | mistrz | wstrzymana | ogolna | brak
 *  max_<id>    50 albo 100 (sufit, do ktorego profesja TERAZ moze rosnac)
 *  opis_<id>, akcja_<id>   gotowe, kolorowe linie lore
 *  nazwa_<id>, ikona_<id>, poziom_<id>, tytul_<id>, znak_<id>
 *  cel_{id,nazwa,poziom,ikona}, z_{...}   cel otwartego okna potwierdzenia (Komenda.Oczekujace)
 *  aktywne_<n>, aktywne_nazwa_<n>, aktywne_poziom_<n>, aktywne_ikona_<n>   (n = 1..limit)
 */
final class Placeholdery extends PlaceholderExpansion {
    private static final Map<String, String> IKONY = Map.ofEntries(
            Map.entry("mining", "IRON_PICKAXE"), Map.entry("woodworking", "IRON_AXE"), Map.entry("herbalism", "FERN"),
            Map.entry("farming", "WHEAT"), Map.entry("fishing", "FISHING_ROD"), Map.entry("hunting", "BOW"),
            Map.entry("jewelcrafting", "AMETHYST_SHARD"), Map.entry("smithing", "ANVIL"),
            Map.entry("tailoring", "LEATHER_CHESTPLATE"), Map.entry("alchemy", "BREWING_STAND"),
            Map.entry("enchanting", "ENCHANTING_TABLE"), Map.entry("cooking", "SMOKER"), Map.entry("building", "BRICKS"));

    private final Mistrzostwa plugin;

    Placeholdery(Mistrzostwa plugin) {
        this.plugin = plugin;
    }

    @Override public String getIdentifier() { return "mistrz"; }
    @Override public String getAuthor() { return "mcmmorpg"; }
    @Override public String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    static String ikona(String id) { return IKONY.getOrDefault(id, "BOOK"); }

    String stan(Player p, String id, Ustawienia u) {
        if (u.ogolne.contains(id)) return "ogolna";
        if (!u.mistrzowskie.contains(id)) return "brak";
        java.util.Set<String> m = plugin.stan().mistrzostwa(p);
        if (m.contains(id)) return "mistrz";
        int lvl = Stan.poziom(p, id);
        if (lvl > u.prog) return "wstrzymana";
        if (lvl == u.prog) return m.size() < u.limit ? "do_wyboru" : "do_zmiany";
        return "rozwoj";
    }

    @Override
    public String onRequest(OfflinePlayer off, String params) {
        if (!(off instanceof Player p) || !p.isOnline()) return "";
        Ustawienia u = plugin.ustawienia();
        String k = params.toLowerCase(Locale.ROOT);
        switch (k) {
            case "liczba": return String.valueOf(plugin.stan().mistrzostwa(p).size());
            case "limit": return String.valueOf(u.limit);
            case "koszt": return Komenda.kwota(u.koszt);
            case "cooldown_godziny": return String.valueOf(u.cooldownGodziny);
            case "cooldown": {
                Instant cd = plugin.stan().cooldownDo(p);
                return cd == null || !cd.isAfter(Instant.now()) ? "gotowe" : Komenda.czas(Duration.between(Instant.now(), cd));
            }
            case "saldo": return plugin.ekonomia() == null ? "?" : Komenda.kwota(plugin.ekonomia().getBalance(p));
            case "lista": {
                List<String> n = new ArrayList<>();
                for (String id : plugin.stan().mistrzostwa(p)) n.add(Stan.nazwa(id));
                return n.isEmpty() ? "brak" : String.join(", ", n);
            }
            default: break;
        }
        if (k.startsWith("cel_") || k.startsWith("z_")) {
            Komenda.Oczekujace o = plugin.komenda().oczekujace(p);
            String id = o == null ? null : (k.startsWith("cel_") ? o.cel() : o.z());
            String pole = k.substring(k.indexOf('_') + 1);
            if (id == null) return pole.equals("ikona") ? "BARRIER" : "—";
            switch (pole) {
                case "id": return id;
                case "nazwa": return Stan.nazwa(id);
                case "poziom": return String.valueOf(Stan.poziom(p, id));
                case "ikona": return ikona(id);
                default: return null;
            }
        }
        if (k.startsWith("aktywne_")) {
            String reszta = k.substring("aktywne_".length());
            String pole = "id";
            int us = reszta.lastIndexOf('_');
            if (us >= 0) { pole = reszta.substring(0, us); reszta = reszta.substring(us + 1); }
            int n;
            try { n = Integer.parseInt(reszta); } catch (NumberFormatException e) { return null; }
            List<String> m = new ArrayList<>(plugin.stan().mistrzostwa(p));
            String id = n >= 1 && n <= m.size() ? m.get(n - 1) : null;
            switch (pole) {
                case "id": return id == null ? "brak" : id;
                case "nazwa": return id == null ? "—" : Stan.nazwa(id);
                case "poziom": return id == null ? "0" : String.valueOf(Stan.poziom(p, id));
                case "ikona": return id == null ? "BARRIER" : ikona(id);
                default: return null;
            }
        }
        int us = k.indexOf('_');
        if (us < 0) return null;
        String pole = k.substring(0, us), id = k.substring(us + 1);
        switch (pole) {
            case "nazwa": return Stan.nazwa(id);
            case "ikona": return ikona(id);
            case "poziom": return String.valueOf(Stan.poziom(p, id));
            case "tytul": return u.tytul(id, Stan.poziom(p, id));
            case "znak": {
                String s = stan(p, id, u);
                return Ustawienia.kolor(s.equals("mistrz") ? "&6✦" : s.equals("ogolna") ? "&a◆" : "");
            }
            case "stan": return stan(p, id, u);
            case "max": {
                String s = stan(p, id, u);
                return (s.equals("ogolna") || s.equals("mistrz")) ? "100" : String.valueOf(u.prog);
            }
            case "opis": {
                String s = stan(p, id, u);
                return Ustawienia.kolor(switch (s) {
                    case "ogolna" -> "&aProfesja ogólna &7— rośnie do &f100&7 bez wyboru";
                    case "mistrz" -> "&6✦ Twoje mistrzostwo &7— rośnie do &f100";
                    case "do_wyboru", "do_zmiany" -> "&cStoi na " + u.prog + " &7— bez mistrzostwa nie rośnie";
                    case "wstrzymana" -> "&8Porzucone mistrzostwo &7— stoi na &f" + Stan.poziom(p, id);
                    case "rozwoj" -> "&7Rozwój swobodny do &f" + u.prog;
                    default -> "";
                });
            }
            case "akcja": {
                String s = stan(p, id, u);
                return Ustawienia.kolor(switch (s) {
                    case "ogolna" -> {
                        String t = u.tytul(id, Stan.poziom(p, id));
                        yield t.isEmpty() ? "&8Nie zajmuje miejsca mistrzostwa" : "&6Tytuł: &f" + t;
                    }
                    case "mistrz" -> {
                        String t = u.tytul(id, Stan.poziom(p, id));
                        yield t.isEmpty() ? "&8Mistrzostwo aktywne" : "&6Tytuł: &f" + t;
                    }
                    case "do_wyboru" -> "&e&lPPM: wybierz mistrzostwo";
                    case "do_zmiany", "wstrzymana" -> plugin.stan().mistrzostwa(p).size() < u.limit
                            ? "&e&lPPM: wybierz mistrzostwo" : "&ePPM: zmień mistrzostwo &7(" + Komenda.kwota(u.koszt) + " $)";
                    case "rozwoj" -> "&8Mistrzostwo do wyboru od poziomu " + u.prog;
                    default -> "";
                });
            }
            default: return null;
        }
    }
}
