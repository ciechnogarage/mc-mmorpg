package pl.mcmmorpg.trafienia;

import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/** Odczyt config.yml: łuki ciosów, rodzaje broni i części ciała per model ModelEngine. */
final class Konfig {

    /** Kapsuła między dwiema kośćmi (ta sama kość dwa razy = kula), promień w pikselach modelu. */
    record Kapsula(String a, String b, double promienPx) {}

    /**
     * Skutek wyczerpania puli części. Odcięcie: ukryj odpada, kikut się pokazuje. Zmiażdżenie: kończyna zostaje,
     * ale wiotczeje (animacja hold), upuszcza to, co trzymała (ukryj -> odpad bez kikuta), albo przyklęka (ogluszenie).
     * przez = które pule wyzwalają skutek: "rana" (cięcie, rąbanie) i/lub "zgniot" (obuch).
     */
    record Skutek(double wytrzymalosc, List<String> przez, String kikut, List<String> ukryj, List<String> tagi,
                  String animacja, double predkosc, int ogluszenie, String chod) {}

    /**
     * Skutek ciosu danego rodzaju w dany materiał dla CIAŁA: rana/zgniot = jaka część siły ciosu zużywa część.
     * Materiał NIE zmienia obrażeń dla HP (balans: każda broń zabija równie szybko) — to robią mnożniki miejsca w części.
     */
    record Wsp(double rana, double zgniot) {}

    /** Materiał: współczynniki per rodzaj ciosu + palność (0 = nie płonie, 1 = słoma). */
    record Material(Map<String, Wsp> wsp, double palnosc) {}

    /** opor = ile energii ciosu zabiera przecięcie części; twarda = ostrze odbija się (drewno, stal) zamiast grzęznąć. */
    /**
     * mnozniki = obrażenia dla HP zależne od MIEJSCA i rodzaju ciosu ("domyslny" + wyjątki, np. pchniecie w oko).
     * przedmiot = trzymany/noszony (kosa, kapelusz): cios w niego nie rani moba; bron = przedmiot, o który broń się zderza.
     * zachwianie = waga części dla paska zachwiania (głowa, nogi więcej), sasiedzi = drogi ognia,
     * spalenie = sekundy palenia do przepalenia (0 = nie odpada od ognia).
     */
    record Czesc(String id, List<Kapsula> kapsuly, List<String> kosci, Map<String, Double> mnozniki, int priorytet,
                 String efekt, Color blysk, String material, Skutek odciecie, Skutek zgniecenie,
                 double opor, boolean twarda, boolean przedmiot, boolean bron, double zachwianie,
                 List<String> sasiedzi, double spalenie) {
        double mnoznik(String rodzaj) { return mnozniki.getOrDefault(rodzaj, mnozniki.getOrDefault("domyslny", 1.0)); }
    }

    /** Zachwianie: pasek ładowany siłą ciosów; pełny = mob traci równowagę na `czas` ticków, próg rośnie po każdym razie. */
    record Zachwianie(double prog, double wzrost, int spadekPo, double spadekNaS, int czas, Map<String, Double> wklad) {}

    record ModelDef(String id, Map<String, Czesc> czesci) {}

    /** Łuk: oś zamachu i połowa kąta (stopnie) dla lekkiego i mocnego ciosu. */
    record Luk(String os, double lekki, double mocny, double krok) {}

    record Bron(List<String> zawiera, String rodzaj) {}

    final Map<String, ModelDef> modele = new LinkedHashMap<>();
    final Map<String, Luk> luki = new LinkedHashMap<>();
    final List<Bron> bronie = new ArrayList<>();
    final Map<String, Material> materialy = new LinkedHashMap<>();
    final Map<String, String> bronieMmo = new LinkedHashMap<>();
    static final Wsp NEUTRALNY = new Wsp(0, 0);
    Zachwianie zachw = new Zachwianie(.25, 1.5, 60, .1, 30, Map.of());
    double zderzenieOdrzutMoba = .35, zderzenieOdrzutGracza = .25;
    int odtracenieTicki = 12;
    String rodzajDomyslny = "piesc";
    double mocnyOd = .9;
    double zasieg = 4.5;
    double udzialPoboczny = .5;
    int blyskTicki = 4;
    int odpadTicki = 120;
    boolean log = true;
    double energiaLekki = .8, energiaMocny = 1.6;
    double niskoDo = .45, glownia = .35;

    static Konfig wczytaj(FileConfiguration c, Logger log) {
        Konfig k = new Konfig();
        k.log = c.getBoolean("log", true);
        k.mocnyOd = c.getDouble("mocny-od-naladowania", .9);
        k.zasieg = c.getDouble("zasieg", 4.5);
        k.udzialPoboczny = c.getDouble("udzial-pobocznej-czesci", .5);
        k.blyskTicki = c.getInt("blysk-ticki", 4);
        k.odpadTicki = c.getInt("odpad-ticki", 120);
        k.energiaLekki = c.getDouble("energia.lekki", .8);
        k.energiaMocny = c.getDouble("energia.mocny", 1.6);
        k.niskoDo = c.getDouble("nisko-do", .45);
        k.glownia = c.getDouble("glownia", .35);
        ConfigurationSection l = c.getConfigurationSection("luki");
        if (l != null) for (String r : l.getKeys(false)) {
            ConfigurationSection s = l.getConfigurationSection(r);
            k.luki.put(r, new Luk(s.getString("os", "brak"), s.getDouble("lekki"), s.getDouble("mocny"), Math.max(1, s.getDouble("krok", 8))));
        }
        for (Map<?, ?> m : c.getMapList("bronie")) {
            @SuppressWarnings("unchecked") List<String> z = (List<String>) m.get("zawiera");
            k.bronie.add(new Bron(z.stream().map(x -> x.toUpperCase(Locale.ROOT)).toList(), String.valueOf(m.get("rodzaj"))));
        }
        k.rodzajDomyslny = c.getString("bron-domyslna", "piesc");
        ConfigurationSection mat = c.getConfigurationSection("materialy");
        if (mat != null) for (String m : mat.getKeys(false)) {
            Map<String, Wsp> w = new LinkedHashMap<>();
            ConfigurationSection ms = mat.getConfigurationSection(m);
            for (String r : ms.getKeys(false)) {
                ConfigurationSection x = ms.getConfigurationSection(r);
                if (x != null) w.put(r, new Wsp(x.getDouble("rana", 0), x.getDouble("zgniot", 0)));
            }
            k.materialy.put(m, new Material(w, ms.getDouble("palnosc", 0)));
        }
        ConfigurationSection bm = c.getConfigurationSection("bronie-mmoitems");
        if (bm != null) for (String t : bm.getKeys(false)) k.bronieMmo.put(t.toUpperCase(Locale.ROOT), bm.getString(t));
        ConfigurationSection z = c.getConfigurationSection("zachwianie");
        if (z != null) {
            Map<String, Double> wk = new LinkedHashMap<>();
            ConfigurationSection ws = z.getConfigurationSection("wklad");
            if (ws != null) for (String r : ws.getKeys(false)) wk.put(r, ws.getDouble(r));
            k.zachw = new Zachwianie(z.getDouble("prog", .25), z.getDouble("wzrost", 1.5), z.getInt("spadek-po", 60),
                    z.getDouble("spadek-na-s", .1), z.getInt("czas", 30), wk);
        }
        k.zderzenieOdrzutMoba = c.getDouble("zderzenie.odrzut-moba", .35);
        k.zderzenieOdrzutGracza = c.getDouble("zderzenie.odrzut-gracza", .25);
        k.odtracenieTicki = c.getInt("zderzenie.odtracenie-ticki", 12);
        ConfigurationSection ms = c.getConfigurationSection("modele");
        if (ms != null) for (String mid : ms.getKeys(false)) {
            ConfigurationSection cz = ms.getConfigurationSection(mid + ".czesci");
            Map<String, Czesc> czesci = new LinkedHashMap<>();
            if (cz != null) for (String id : cz.getKeys(false)) {
                ConfigurationSection s = cz.getConfigurationSection(id);
                List<Kapsula> kap = new ArrayList<>();
                for (Object o : s.getList("kapsuly", List.of())) {
                    List<?> x = (List<?>) o;
                    kap.add(new Kapsula(String.valueOf(x.get(0)), String.valueOf(x.get(1)), ((Number) x.get(2)).doubleValue()));
                }
                Map<String, Double> mn = new LinkedHashMap<>();
                ConfigurationSection mns = s.getConfigurationSection("mnozniki");
                if (mns != null) for (String r : mns.getKeys(false)) mn.put(r, mns.getDouble(r));
                else mn.put("domyslny", s.getDouble("mnoznik", 1));
                czesci.put(id, new Czesc(id, kap, s.getStringList("kosci"), mn, s.getInt("priorytet", 0),
                        s.getString("efekt", "domyslny"), kolor(s.getString("blysk", "FF6A5A")), s.getString("material", "cialo"),
                        skutek(s.getConfigurationSection("odciecie"), List.of("rana")),
                        skutek(s.getConfigurationSection("zgniecenie"), List.of("zgniot")),
                        s.getDouble("opor", .35), s.getBoolean("twarda", false), s.getBoolean("przedmiot", false),
                        s.getBoolean("bron", false), s.getDouble("zachwianie", 1), s.getStringList("sasiedzi"),
                        s.getDouble("spalenie", 0)));
            }
            k.modele.put(mid, new ModelDef(mid, czesci));
            log.info("model " + mid + ": " + czesci.size() + " czesci");
        }
        return k;
    }

    private static Skutek skutek(ConfigurationSection os, List<String> domyslnie) {
        if (os == null) return null;
        List<String> tagi = new ArrayList<>(os.getStringList("tagi"));
        if (os.getString("tag") != null) tagi.add(0, os.getString("tag"));
        List<String> przez = os.getStringList("przez");
        return new Skutek(os.getDouble("wytrzymalosc", .3), przez.isEmpty() ? domyslnie : przez, os.getString("kikut"),
                os.getStringList("ukryj"), tagi, os.getString("animacja"), os.getDouble("predkosc", 1), os.getInt("ogluszenie", 0),
                os.getString("chod"));
    }

    /** Współczynniki rodzaju ciosu w materiał części; brak wpisu = obrażenia bez zmian i bez skutków. */
    Wsp wsp(String material, String rodzaj) {
        Material m = materialy.get(material);
        return m == null ? NEUTRALNY : m.wsp().getOrDefault(rodzaj, NEUTRALNY);
    }

    double palnosc(String material) {
        Material m = materialy.get(material);
        return m == null ? 0 : m.palnosc();
    }

    String rodzajBroni(String material) {
        for (Bron b : bronie) for (String z : b.zawiera()) if (material.contains(z)) return b.rodzaj();
        return rodzajDomyslny;
    }

    static Color kolor(String hex) {
        return Color.fromRGB(Integer.parseInt(hex.replace("#", ""), 16));
    }
}
