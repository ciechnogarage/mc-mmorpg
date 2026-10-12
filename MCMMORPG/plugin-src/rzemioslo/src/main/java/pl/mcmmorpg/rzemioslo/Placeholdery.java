package pl.mcmmorpg.rzemioslo;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * %rzemioslo_...% dla menu (DeluxeMenus, generowane przez MCMMORPG/profesje/menu.py):
 *  zlecenie_<prof>_<n>_{tytul,opis,masz,ilosc,xp,kasa,akcja,id,typ}
 *  start_<prof>_{opis,postep,akcja}
 *  buff_<grupa>          aktywny buff grupy z pozostalym czasem (pusty = brak)
 *  tier_<prof>           biezacy tier (rzymska)
 */
final class Placeholdery extends PlaceholderExpansion {
    private final Rzemioslo plugin;

    Placeholdery(Rzemioslo plugin) { this.plugin = plugin; }

    @Override public String getIdentifier() { return "rzemioslo"; }
    @Override public String getAuthor() { return "mcmmorpg"; }
    @Override public String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer off, String params) {
        if (!(off instanceof Player p) || !p.isOnline()) return "";
        String k = params.toLowerCase(Locale.ROOT);
        Dane d = plugin.dane();
        if (k.startsWith("buff_")) return plugin.buffy().opis(p, k.substring(5));
        if (k.startsWith("tier_")) return d.rzymska(Profesje.tier(p, k.substring(5)));
        if (k.startsWith("zlecenie_")) {
            String[] c = k.substring(9).split("_");
            if (c.length < 3) return null;
            String prof = c[0];
            int n;
            try { n = Integer.parseInt(c[1]); } catch (NumberFormatException e) { return null; }
            String pole = c[2];
            List<Zlecenia.Pozycja> l = plugin.zlecenia().dzisiejsze(p, prof);
            if (n < 1 || n > l.size()) return pole.equals("tytul") ? "§8Brak zlecenia" : "";
            Zlecenia.Pozycja poz = l.get(n - 1);
            int masz = Przedmioty.ile(p, poz.z().id());
            return switch (pole) {
                case "tytul" -> (poz.oddane() ? "§8" : "§6§l") + "Dostarcz " + poz.z().ilosc() + "× " + d.nazwa(poz.z().id());
                case "opis" -> "§7Tier " + d.rzymska(poz.tier()) + " · " + d.tiery.get(poz.tier()).nazwa();
                case "masz" -> String.valueOf(Math.min(masz, poz.z().ilosc()));
                case "ilosc" -> String.valueOf(poz.z().ilosc());
                case "id" -> poz.z().id();
                case "typ" -> d.typy.getOrDefault(poz.z().id(), "MATERIAL");
                case "xp" -> String.valueOf(poz.z().xp());
                case "kasa" -> Zlecenia.kwota(poz.z().pieniadze());
                case "akcja" -> poz.oddane() ? "§8Oddane — nowe zlecenia jutro."
                        : masz >= poz.z().ilosc() ? "§aKliknij, aby oddać." : "§cBrakuje: " + (poz.z().ilosc() - masz);
                default -> null;
            };
        }
        if (k.startsWith("start_")) {
            String[] c = k.substring(6).split("_");
            if (c.length < 2) return null;
            String prof = c[0], pole = c[1];
            Dane.Start s = d.startowe.get(prof);
            if (s == null) return "";
            String stan = plugin.zlecenia().stanStartu(p, prof);
            int post = plugin.zlecenia().postepStartu(p, prof);
            return switch (pole) {
                case "opis" -> "§7Weź " + d.nazwa(s.narzedzie()) + " i zbierz " + s.ilosc() + "× " + d.nazwa(s.zbierz()) + ".";
                case "postep" -> switch (stan) {
                    case "aktywne" -> "§7Postęp: §f" + post + "/" + s.ilosc();
                    case "odebrane" -> "§8Ukończone.";
                    default -> "§7Nagroda: narzędzie i §a" + s.xp() + " XP";
                };
                case "akcja" -> switch (stan) {
                    case "brak" -> "§eKliknij: przyjmij i weź narzędzie";
                    case "aktywne" -> post >= s.ilosc() ? "§aKliknij: odbierz nagrodę" : "§7Zbieraj dalej (PPM w źródło).";
                    default -> "";
                };
                default -> null;
            };
        }
        return null;
    }
}
