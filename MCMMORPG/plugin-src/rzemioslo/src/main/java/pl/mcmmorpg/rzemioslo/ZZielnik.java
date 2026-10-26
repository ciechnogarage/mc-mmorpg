package pl.mcmmorpg.rzemioslo;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Zielnik pisany przez gracza: wpis pojawia sie dopiero, gdy cos sam odkryje. Nic nie podpowiada z gory. */
final class ZZielnik {
    /** Strona rosliny: wpisy w kolejnosci odkrywania (klucz -> tekst). */
    record Strona(String tytul, Map<String, String> wpisy) {}

    static final Map<String, Strona> STRONY = new java.util.LinkedHashMap<>();
    static {
        Map<String, String> d = new java.util.LinkedHashMap<>();
        d.put("dziurawiec_widziany", "Krzaczek o żółtych gwiazdkach kwiatów, na słonecznej łące.");
        d.put("dziurawiec_zbior", "Ścięty pęd daje ziele lecznicze.");
        d.put("dziurawiec_wyborny", "Najlepsze ziele: ścinać tuż pod kwiatami, póki kwitnie.");
        d.put("dziurawiec_chory", "Pędów z ciemnymi plamkami nie brać: ziele z nich do niczego. Plamki widać dopiero z bliska.");
        d.put("dziurawiec_faza", "Kępa kwitnie kilka dni: najpierw pąki, potem pełnia, potem przekwita. Ziele bierze się w pełni.");
        d.put("dziurawiec_chciwosc", "Rada zielarzy: kto zetnie więcej niż pół kępy, ten długo czeka, a odrastają cienkie pędy.");
        d.put("dziurawiec_rosa", "Przesąd zielarzy: ziele cięte w deszczu albo rannej rosie pleśnieje w suszarni.");
        STRONY.put("dziurawiec", new Strona("Dziurawiec", d));
        Map<String, String> k = new java.util.LinkedHashMap<>();
        k.put("kozlek_widziany", "Wysoka łodyga z różowym baldachem, przy wodzie.");
        k.put("kozlek_zbior", "Cenny jest korzeń. Siedzi głęboko.");
        k.put("kozlek_wyborny", "Korzeń z korzonkami: najpierw rozluźnić ziemię tam, gdzie biegną. Ciągnąć zrywami: napiąć, popuścić, gdy trzeszczy, napiąć znowu.");
        k.put("kozlek_urwany", "Ciągnięty na siłę urywa się w ziemi.");
        k.put("kozlek_sonda", "Ziemię najpierw opukać łopatką: nad korzonkiem odgłos głuchy, nad pustym miejscem sypki. Kopać tylko tam, gdzie głucho.");
        k.put("kozlek_woda", "Przy wodzie korzeń idzie łatwo, ale jest kruchy. W suchej ziemi trzyma mocno.");
        STRONY.put("kozlek", new Strona("Kozłek", k));
        Map<String, String> s = new java.util.LinkedHashMap<>();
        s.put("szerokolistna_widziana", "W cieniu drzew rosną rośliny o szerokich liściach. Z daleka wszystkie wyglądają tak samo.");
        s.put("czosnek_zapach", "Czosnek niedźwiedzi: roztarty liść pachnie czosnkiem. Każdy liść na własnym ogonku.");
        s.put("konwalia_zapach", "Konwalia: roztarty liść nie pachnie, gorzki sok. Liście wyrastają z jednej pochwy. Trująca.");
        s.put("czosnek_wyborny", "Liście czosnku najlepsze młode, zanim zakwitnie.");
        s.put("czosnek_rozcieranie", "Rada zielarzy: kto każdy liść rozciera, ten gniecie zbiór. Pewny czosnek bierze się od razu.");
        s.put("konwalia_skaza", "Konwalia wmieszana w czosnek: skup nie weźmie partii, a wyrób z niej szkodzi. Przebierz liście w suszarni.");
        STRONY.put("szerokolistna", new Strona("Rośliny szerokolistne", s));
    }

    private final Rzemioslo plugin;
    private final Map<UUID, Set<String>> odkrycia = new HashMap<>();
    private final File katalog;
    private final ZMagazyn magazyn;
    /** Postacie ze zmianami jeszcze nie oddanymi na dysk (liczniki bez zapisu od razu). */
    private final Set<UUID> brudne = new java.util.HashSet<>();

    ZZielnik(Rzemioslo plugin, ZMagazyn magazyn) {
        this.plugin = plugin;
        this.magazyn = magazyn;
        this.katalog = new File(plugin.getDataFolder(), "zielnik");
    }

    /** Wejscie gracza: zielnik postaci z dysku w tle, zanim zacznie zbierac (F1). */
    void wczytaj(UUID u) {
        if (odkrycia.containsKey(u)) return;
        magazyn.czytaj(new File(katalog, u + ".yml")).whenComplete((y, blad) -> {
            if (blad != null) plugin.getLogger().severe("zielnik: odczyt " + u + ": " + blad);
            if (!plugin.isEnabled() || blad != null) return;   // po bledzie dla() sprobuje jeszcze raz przy pierwszym uzyciu
            Bukkit.getScheduler().runTask(plugin, () -> { if (!odkrycia.containsKey(u)) wloz(u, y); });
        });
    }

    private Set<String> wloz(UUID u, YamlConfiguration y) {
        Set<String> s = new LinkedHashSet<>();
        Map<String, Integer> l = new HashMap<>();
        if (y != null) {
            s.addAll(y.getStringList("odkrycia"));
            var sek = y.getConfigurationSection("liczniki");
            if (sek != null) for (String x : sek.getKeys(false)) l.put(x, sek.getInt(x));
        }
        liczniki.put(u, l);
        odkrycia.put(u, s);
        return s;
    }

    boolean ma(Player p, String wpis) { return dla(Zlecenia.profil(p)).contains(wpis); }

    /** Ukryty wpis (bez ogloszenia): np. rozpoznany okaz kepy. */
    void zapamietaj(Player p, String wpis) { if (dla(Zlecenia.profil(p)).add(wpis)) zapisz(Zlecenia.profil(p)); }

    /** Ile razy gracz widzial juz rade do gatunku (ukryte wpisy, zielnik ich nie pokazuje); zwraca nowa liczbe. */
    int rada(Player p, String gatunek) {
        Set<String> s = dla(Zlecenia.profil(p));
        int n = 0;
        while (s.contains("_rada_" + gatunek + "_" + (n + 1))) n++;
        s.add("_rada_" + gatunek + "_" + (n + 1));
        return n + 1;
    }

    /** Odkrycia z trwajacego zbioru czekaja do konca sesji — w trakcie czat zaslanial rosline. */
    private final Map<UUID, List<String>> zalegle = new HashMap<>();

    /** Zalegle odkrycia jako krotki dopisek do jednej linii dziennika (feeling: „zielnik +N”, tresc czeka w zielniku); null = brak. */
    String zaleglosciTekst(Player p) {
        List<String> l = zalegle.remove(p.getUniqueId());
        if (l == null || l.isEmpty()) return null;
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.7f, 1.1f);
        return "zielnik +" + l.size();
    }

    void wypiszZaleglosci(Player p) {
        List<String> l = zalegle.remove(p.getUniqueId());
        if (l == null || l.isEmpty()) return;
        Component c = Component.text("✎ Zielnik: ", NamedTextColor.DARK_GREEN);
        for (int i = 0; i < l.size(); i++) c = c.append(Component.text((i > 0 ? " · " : "") + l.get(i), NamedTextColor.GRAY));
        p.sendMessage(c);
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.7f, 1.1f);
    }

    /** Zwykle juz w pamieci (wczytaj przy wejsciu); odczyt z dysku tutaj tylko, gdy postac zmieniono w trakcie gry. */
    private Set<String> dla(UUID u) {
        Set<String> s = odkrycia.get(u);
        return s != null ? s : wloz(u, magazyn.czytajTeraz(new File(katalog, u + ".yml")));
    }

    /** Ukryte liczniki postaci (wprawa, rady, skaza partii): zielnik ich nie pokazuje. */
    private final Map<UUID, Map<String, Integer>> liczniki = new HashMap<>();

    private Map<String, Integer> lic(UUID u) { dla(u); return liczniki.computeIfAbsent(u, k -> new HashMap<>()); }

    int licznik(Player p, String k) { return lic(Zlecenia.profil(p)).getOrDefault(k, 0); }

    /** Dodaje d (moze byc ujemne, dol 0) i zwraca nowa wartosc; zapis od razu, gdy zapisz=true (skaza, wprawa). */
    int dolicz(Player p, String k, int d, boolean zapisz) {
        UUID u = Zlecenia.profil(p);
        Map<String, Integer> m = lic(u);
        int v = Math.max(0, m.getOrDefault(k, 0) + d);
        if (v == 0) m.remove(k); else m.put(k, v);
        if (zapisz) zapisz(u); else brudne.add(u);
        return v;
    }

    /** Test/admin: zapomina pokazane rady (wejscie i po ruchu), zeby porownac kadry wariantow komunikatow. */
    int zapomnijRady(Player p) {
        UUID u = Zlecenia.profil(p);
        int n = 0;
        n += lic(u).keySet().removeIf(k -> k.startsWith("_r_")) ? 1 : 0;
        n += dla(u).removeIf(k -> k.startsWith("_rada_")) ? 1 : 0;
        zapisz(u);
        return n;
    }

    /** Rada po ruchu: tylko 3 pierwsze razy na postac i rodzaj. */
    boolean rada3(Player p, String rodzaj) { return dolicz(p, "_r_" + rodzaj, 1, false) <= 3; }

    /** Migawka na watku serwera, zapis w kolejce dysku (od razu: odkrycia, skaza, wprawa). */
    void zapisz(UUID u) {
        Set<String> s = odkrycia.get(u);
        if (s == null) return;
        brudne.remove(u);
        YamlConfiguration y = new YamlConfiguration();
        y.set("odkrycia", new ArrayList<>(s));
        Map<String, Integer> l = liczniki.get(u);
        if (l != null) for (Map.Entry<String, Integer> e : l.entrySet()) y.set("liczniki." + e.getKey(), e.getValue());
        magazyn.zapisz(new File(katalog, u + ".yml"), y.saveToString());
    }

    void zapiszWszystko() { for (UUID u : new ArrayList<>(odkrycia.keySet())) zapisz(u); }

    void zapiszZmienione() { for (UUID u : new ArrayList<>(brudne)) zapisz(u); }

    /** Wyjscie: zapis, a pamiec zwalniamy dopiero PO zapisie (dla() czytajacy dysk nie zobaczy starszego pliku). */
    void zwolnij(UUID gracz, UUID profil) {
        zapisz(profil);
        magazyn.potem(() -> {
            if (!plugin.isEnabled()) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (Bukkit.getPlayer(gracz) != null) return;
                odkrycia.remove(profil);
                liczniki.remove(profil);
                zalegle.remove(gracz);
            });
        });
    }

    /** Zwraca true, gdy to nowy wpis (i oglasza go graczowi). */
    boolean odkryj(Player p, String klucz) {
        UUID u = Zlecenia.profil(p);
        if (!dla(u).add(klucz)) return false;
        String tekst = null;
        for (Strona st : STRONY.values()) if (st.wpisy().containsKey(klucz)) tekst = st.wpisy().get(klucz);
        if (tekst != null && plugin.zielarz().wTrakcie(p)) zalegle.computeIfAbsent(p.getUniqueId(), k -> new ArrayList<>()).add(tekst);
        else if (tekst != null) {
            p.sendMessage(Component.text("✎ Zielnik: ", NamedTextColor.DARK_GREEN).append(Component.text(tekst, NamedTextColor.GRAY)));
            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.7f, 1.1f);
        }
        zapisz(u);
        return true;
    }

    void zobaczono(Player p, String widoczny) {
        switch (widoczny) {
            case "dziurawiec" -> odkryj(p, "dziurawiec_widziany");
            case "kozlek" -> odkryj(p, "kozlek_widziany");
            default -> odkryj(p, "szerokolistna_widziana");
        }
    }

    void zbior(Player p, String gatunek, double q) {
        switch (gatunek) {
            case "dziurawiec" -> { odkryj(p, "dziurawiec_zbior"); if (q >= 0.85) odkryj(p, "dziurawiec_wyborny"); else if (q == 0) odkryj(p, "dziurawiec_chory"); }
            case "kozlek" -> { odkryj(p, "kozlek_zbior"); if (q >= 0.85) odkryj(p, "kozlek_wyborny"); }   // urwanie odkrywa Zielarz.zerwijKorzen (nie z jakosci)
            case "czosnek" -> { if (q >= 0.85) odkryj(p, "czosnek_wyborny"); }
            default -> { }
        }
    }

    int liczba(Player p) { return dla(Zlecenia.profil(p)).size(); }

    /** Okno zielnika (dialog Papera): tylko to, co gracz odkryl. */
    void pokaz(Player p) {
        Set<String> moje = dla(Zlecenia.profil(p));
        List<DialogBody> cialo = new ArrayList<>();
        int strony = 0;
        for (Strona st : STRONY.values()) {
            List<String> w = new ArrayList<>();
            for (Map.Entry<String, String> e : st.wpisy().entrySet()) if (moje.contains(e.getKey())) w.add("• " + e.getValue());   // ukryte "_rada_" nie sa w STRONY
            if (w.isEmpty()) continue;
            strony++;
            cialo.add(DialogBody.plainMessage(Component.text(st.tytul(), NamedTextColor.DARK_GREEN, TextDecoration.BOLD), 300));
            cialo.add(DialogBody.plainMessage(Component.text(String.join("\n", w), NamedTextColor.GRAY), 300));
        }
        if (strony == 0) cialo.add(DialogBody.plainMessage(Component.text("Puste strony. Zielnik zapisze to, co sam odkryjesz.", NamedTextColor.GRAY), 300));
        Dialog dlg = Dialog.create(b -> b.empty()
                .base(DialogBase.builder(Component.text("Zielnik")).canCloseWithEscape(true).pause(false).body(cialo).build())
                .type(DialogType.notice(ActionButton.builder(Component.text("Zamknij")).build())));
        p.showDialog(dlg);
    }
}
