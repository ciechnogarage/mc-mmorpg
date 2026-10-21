package pl.mcmmorpg.mistrzostwa;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Niezmienna migawka config.yml. */
final class Ustawienia {
    final int prog;
    final int limit;
    final Set<String> ogolne;
    final Set<String> mistrzowskie;
    final double koszt;
    final long cooldownGodziny;
    final String kontekst;
    final String prefiks;
    final String blokada;
    final long blokadaCoMs;
    final String menuGlowne, menuWybor, menuZmiana;
    final java.util.TreeMap<Integer, String> tytuly = new java.util.TreeMap<>();
    final java.util.Map<String, String> dopelniacz = new java.util.HashMap<>();
    final int ogloszenieOd;
    final Set<String> sabotaz;

    Ustawienia(FileConfiguration c) {
        prog = c.getInt("prog", 50);
        limit = c.getInt("limit", 2);
        ogolne = lower(c.getStringList("ogolne"));
        mistrzowskie = lower(c.getStringList("mistrzowskie"));
        koszt = c.getDouble("zmiana.koszt", 2500);
        cooldownGodziny = c.getLong("zmiana.cooldown-godziny", 168);
        kontekst = c.getString("kontekst", "profil");
        prefiks = kolor(c.getString("komunikaty.prefiks", ""));
        blokada = kolor(c.getString("komunikaty.blokada", "{profesja} {prog}"));
        blokadaCoMs = c.getLong("komunikaty.blokada-co-sekund", 20) * 1000L;
        menuGlowne = c.getString("menu.glowne", "profession_main");
        menuWybor = c.getString("menu.wybor", "mistrz_wybor");
        menuZmiana = c.getString("menu.zmiana", "mistrz_zmiana");
        var t = c.getConfigurationSection("tytuly");
        if (t != null) for (String k : t.getKeys(false)) tytuly.put(Integer.parseInt(k), t.getString(k));
        var d = c.getConfigurationSection("dopelniacz");
        if (d != null) for (String k : d.getKeys(false)) dopelniacz.put(k.toLowerCase(Locale.ROOT), d.getString(k));
        ogloszenieOd = c.getInt("ogloszenie-od", 75);
        sabotaz = lower(c.getStringList("sabotaz-testowy"));
        for (String o : ogolne)
            if (mistrzowskie.contains(o)) throw new IllegalArgumentException("Profesja " + o + " jest jednoczesnie ogolna i mistrzowska");
    }

    private static Set<String> lower(List<String> l) {
        Set<String> s = new LinkedHashSet<>();
        for (String x : l) s.add(x.toLowerCase(Locale.ROOT));
        return s;
    }

    boolean sab(String co) { return sabotaz.contains(co); }

    /** "Mistrz Gornictwa" albo "" ponizej pierwszego progu tytulu. */
    String tytul(String id, int poziom) {
        var e = tytuly.floorEntry(poziom);
        if (e == null) return "";
        return e.getValue() + " " + dopelniacz.getOrDefault(id, id);
    }

    static String kolor(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
