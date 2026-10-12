package pl.mcmmorpg.rzemioslo;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/** Dane z Rzemioslo/config.yml (GENEROWANY przez MCMMORPG/profesje/generuj.py). Tylko odczyt. */
final class Dane {
    record Tier(String nazwa, int start, String rzymska) {}
    record Narz(String id, String rodzaj, int tier, int wytrzymalosc, String profesja) {}
    record Zrodlo(String profesja, int tier, String glowny, String poboczny, String rzadki, double szansaPoboczny,
                  double szansaRzadki, String nazwa, BlockData wyczerpany, String dzwiek, String dzwiekKoniec) {}
    record Lowisko(int tier, String glowny, String poboczny, double szansaPoboczny, List<Map.Entry<String, Double>> rzadkie) {}
    record Buff(String id, String rodzaj, String grupa, int czas, int cooldown, Map<String, Double> staty, int tier,
                String nazwa, boolean druzyna) {}
    record Oboz(String id, int tier, int czas, double promien, Map<String, Double> staty) {}
    record Rec(String id, String profesja, int tier, int xp, String stacja, String wynik, String najdrozszy) {}
    record Zlec(String id, int ilosc, int xp, double pieniadze) {}
    record Start(String zbierz, int ilosc, String narzedzie, int xp) {}

    final double gT1, gWzrost, kara;
    final double czasBazowy, czasOprawiania, czasMin, czasNaTierNarzedzia, czasNaPoziomWTierze, zasiegRuchu;
    final int wyczerpanieS, zwlokiS, machniecieCo;
    final Map<Integer, Tier> tiery = new HashMap<>();
    final Map<String, String> nazwyProfesji = new HashMap<>();
    final Map<String, String> narzedzieProfesji = new HashMap<>();
    final Set<String> surowce = new HashSet<>();
    final Map<String, Narz> narzedzia = new HashMap<>();
    final Map<String, Zrodlo> wezly = new HashMap<>();
    final Map<Integer, Lowisko> lowiska = new HashMap<>();
    final Map<String, Integer> lowiskoBloki = new HashMap<>();
    final Map<String, Zrodlo> zwierzyna = new HashMap<>();
    final Map<String, Buff> buffy = new HashMap<>();
    final Map<String, Oboz> obozowiska = new HashMap<>();
    final Map<String, String> uzycia = new HashMap<>();
    final Map<String, Rec> receptury = new HashMap<>();
    final double perkPodwojny, perkCzas, perkZwrot;
    final int perkCo;
    final Map<String, Map<Integer, List<Zlec>>> zlecenia = new HashMap<>();
    final int zleceniaDziennie;
    final Map<String, Start> startowe = new LinkedHashMap<>();
    final Map<String, String> nazwy = new HashMap<>();
    final Map<String, String> typy = new HashMap<>();
    final List<String> problemy = new ArrayList<>();

    Dane(FileConfiguration c) {
        gT1 = c.getDouble("krzywa.g_t1", 10);
        gWzrost = c.getDouble("krzywa.g_wzrost", 1.25);
        kara = c.getDouble("krzywa.kara_nizszego_tieru", 0.5);
        czasBazowy = c.getDouble("zbieranie.czas-bazowy", 3.0);
        czasOprawiania = c.getDouble("zbieranie.czas-oprawiania", 2.5);
        czasMin = c.getDouble("zbieranie.czas-min", 1.2);
        czasNaTierNarzedzia = c.getDouble("zbieranie.czas-na-tier-narzedzia", 0.08);
        czasNaPoziomWTierze = c.getDouble("zbieranie.czas-na-poziom-w-tierze", 0.01);
        zasiegRuchu = c.getDouble("zbieranie.zasieg-ruchu", 1.5);
        wyczerpanieS = c.getInt("zbieranie.wyczerpanie-s", 45);
        zwlokiS = c.getInt("zbieranie.zwloki-s", 30);
        machniecieCo = c.getInt("zbieranie.machniecie-co-tickow", 7);
        sekcja(c, "tiery", (k, s) -> tiery.put(Integer.parseInt(k), new Tier(s.getString("nazwa"), s.getInt("start"), s.getString("rzymska"))));
        sekcja(c, "profesje", (k, s) -> {
            nazwyProfesji.put(k, s.getString("nazwa"));
            if (s.getString("narzedzie") != null) narzedzieProfesji.put(k, s.getString("narzedzie"));
        });
        surowce.addAll(c.getStringList("surowce"));
        sekcja(c, "narzedzia", (k, s) -> narzedzia.put(k, new Narz(k, s.getString("rodzaj"), s.getInt("tier"), s.getInt("wytrzymalosc"), s.getString("profesja"))));
        sekcja(c, "wezly", (k, s) -> wezly.put(k, zrodlo(s, true)));
        sekcja(c, "zwierzyna", (k, s) -> zwierzyna.put(k, zrodlo(s, false)));
        sekcja(c, "lowiska", (k, s) -> {
            List<Map.Entry<String, Double>> rz = new ArrayList<>();
            for (Object o : s.getList("rzadkie", List.of())) {
                if (o instanceof List<?> l && l.size() == 2) rz.add(Map.entry(String.valueOf(l.get(0)), ((Number) l.get(1)).doubleValue()));
            }
            int t = Integer.parseInt(k);
            lowiska.put(t, new Lowisko(t, s.getString("glowny"), s.getString("poboczny"), s.getDouble("szansa_poboczny"), rz));
        });
        sekcja(c, "lowisko-bloki", null);
        ConfigurationSection lb = c.getConfigurationSection("lowisko-bloki");
        if (lb != null) for (String k : lb.getKeys(false)) lowiskoBloki.put(k, lb.getInt(k));
        sekcja(c, "buffy", (k, s) -> buffy.put(k, new Buff(k, s.getString("rodzaj"), s.getString("grupa"), s.getInt("czas"),
                s.getInt("cooldown"), liczby(s.getConfigurationSection("staty")), s.getInt("tier"), s.getString("nazwa"), s.getBoolean("druzyna"))));
        sekcja(c, "obozowisko", (k, s) -> obozowiska.put(k, new Oboz(k, s.getInt("tier"), s.getInt("czas"), s.getDouble("promien"),
                liczby(s.getConfigurationSection("staty")))));
        ConfigurationSection u = c.getConfigurationSection("uzycia");
        if (u != null) for (String k : u.getKeys(false)) uzycia.put(k, u.getString(k));
        sekcja(c, "receptury", (k, s) -> receptury.put(k, new Rec(k, s.getString("profesja"), s.getInt("tier"), s.getInt("xp"),
                s.getString("stacja"), s.getString("wynik"), s.getString("najdrozszy"))));
        perkCo = c.getInt("perki.zbieractwo.co", 10);
        perkPodwojny = c.getDouble("perki.zbieractwo.podwojny", 0.04);
        perkCzas = c.getDouble("perki.zbieractwo.czas", 0.01);
        perkZwrot = c.getDouble("perki.rzemioslo.zwrot", 0.02);
        ConfigurationSection z = c.getConfigurationSection("zlecenia");
        if (z != null) for (String prof : z.getKeys(false)) {
            Map<Integer, List<Zlec>> po = new HashMap<>();
            ConfigurationSection zp = z.getConfigurationSection(prof);
            for (String t : zp.getKeys(false)) {
                List<Zlec> l = new ArrayList<>();
                for (Map<?, ?> m : zp.getMapList(t)) {
                    l.add(new Zlec(String.valueOf(m.get("id")), ((Number) m.get("ilosc")).intValue(),
                            ((Number) m.get("xp")).intValue(), ((Number) m.get("pieniadze")).doubleValue()));
                }
                po.put(Integer.parseInt(t), l);
            }
            zlecenia.put(prof, po);
        }
        zleceniaDziennie = c.getInt("zlecenia-dziennie", 3);
        sekcja(c, "zlecenia-startowe", (k, s) -> startowe.put(k, new Start(s.getString("zbierz"), s.getInt("ilosc"),
                s.getString("narzedzie"), s.getInt("xp"))));
        ConfigurationSection n = c.getConfigurationSection("nazwy");
        if (n != null) for (String k : n.getKeys(false)) nazwy.put(k, n.getString(k));
        ConfigurationSection ty = c.getConfigurationSection("typy");
        if (ty != null) for (String k : ty.getKeys(false)) typy.put(k, ty.getString(k));
        if (wezly.isEmpty()) problemy.add("brak sekcji wezly — config nie pochodzi z generatora profesji v2");
    }

    private Zrodlo zrodlo(ConfigurationSection s, boolean blok) {
        BlockData w = null;
        if (blok) {
            String m = s.getString("wyczerpany", "STONE");
            try {
                w = Bukkit.createBlockData(Material.valueOf(m));
            } catch (Exception e) {
                problemy.add("zly blok wyczerpania " + m);
                w = Bukkit.createBlockData(Material.STONE);
            }
        }
        return new Zrodlo(s.getString("profesja", "hunting"), s.getInt("tier"), s.getString("glowny"), s.getString("poboczny"),
                s.getString("rzadki"), s.getDouble("szansa_poboczny"), s.getDouble("szansa_rzadki"), s.getString("nazwa", ""),
                w, s.getString("dzwiek", "entity.player.attack.sweep"), s.getString("dzwiek_koniec", "entity.item.pickup"));
    }

    private static Map<String, Double> liczby(ConfigurationSection s) {
        Map<String, Double> m = new LinkedHashMap<>();
        if (s != null) for (String k : s.getKeys(false)) m.put(k, s.getDouble(k));
        return m;
    }

    private interface Wpis { void wez(String klucz, ConfigurationSection s); }

    private static void sekcja(FileConfiguration c, String sciezka, Wpis w) {
        if (w == null) return;
        ConfigurationSection s = c.getConfigurationSection(sciezka);
        if (s == null) return;
        for (String k : s.getKeys(false)) {
            ConfigurationSection x = s.getConfigurationSection(k);
            if (x != null) w.wez(k, x);
        }
    }

    double g(int tier) { return gT1 * Math.pow(gWzrost, tier - 1); }

    static int tierPoziomu(int poziom) { return Math.min(10, Math.max(1, (poziom - 1) / 10 + 1)); }

    String nazwa(String id) { return nazwy.getOrDefault(id, id); }

    String rzymska(int t) { Tier x = tiery.get(t); return x == null ? String.valueOf(t) : x.rzymska(); }

    int start(int t) { Tier x = tiery.get(t); return x == null ? 10 * t - 9 : x.start(); }
}
