package pl.mcmmorpg.rzemioslo;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
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
        d.put("dziurawiec_chory", "Pędów z ciemnymi plamkami nie brać: ziele z nich do niczego.");
        STRONY.put("dziurawiec", new Strona("Dziurawiec", d));
        Map<String, String> k = new java.util.LinkedHashMap<>();
        k.put("kozlek_widziany", "Wysoka łodyga z różowym baldachem, przy wodzie.");
        k.put("kozlek_zbior", "Cenny jest korzeń. Siedzi głęboko.");
        k.put("kozlek_wyborny", "Korzeń z korzonkami: najpierw rozluźnić ziemię tam, gdzie biegną. Ciągnąć zrywami: napiąć, popuścić, gdy trzeszczy, napiąć znowu.");
        k.put("kozlek_urwany", "Ciągnięty na siłę urywa się w ziemi.");
        STRONY.put("kozlek", new Strona("Kozłek", k));
        Map<String, String> s = new java.util.LinkedHashMap<>();
        s.put("szerokolistna_widziana", "W cieniu drzew rosną rośliny o szerokich liściach. Z daleka wszystkie wyglądają tak samo.");
        s.put("czosnek_zapach", "Czosnek niedźwiedzi: roztarty liść pachnie czosnkiem. Każdy liść na własnym ogonku.");
        s.put("konwalia_zapach", "Konwalia: roztarty liść nie pachnie, gorzki sok. Liście wyrastają z jednej pochwy. Trująca.");
        s.put("czosnek_wyborny", "Liście czosnku najlepsze młode, zanim zakwitnie.");
        STRONY.put("szerokolistna", new Strona("Rośliny szerokolistne", s));
    }

    private final Rzemioslo plugin;
    private final Map<UUID, Set<String>> odkrycia = new HashMap<>();
    private final File katalog;

    ZZielnik(Rzemioslo plugin) {
        this.plugin = plugin;
        this.katalog = new File(plugin.getDataFolder(), "zielnik");
    }

    boolean ma(Player p, String wpis) { return dla(Zlecenia.profil(p)).contains(wpis); }

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

    void wypiszZaleglosci(Player p) {
        List<String> l = zalegle.remove(p.getUniqueId());
        if (l == null || l.isEmpty()) return;
        Component c = Component.text("✎ Zielnik: ", NamedTextColor.DARK_GREEN);
        for (int i = 0; i < l.size(); i++) c = c.append(Component.text((i > 0 ? " · " : "") + l.get(i), NamedTextColor.GRAY));
        p.sendMessage(c);
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.7f, 1.1f);
    }

    private Set<String> dla(UUID u) {
        return odkrycia.computeIfAbsent(u, k -> {
            Set<String> s = new LinkedHashSet<>();
            File f = new File(katalog, k + ".yml");
            if (f.exists()) s.addAll(YamlConfiguration.loadConfiguration(f).getStringList("odkrycia"));
            return s;
        });
    }

    void zapisz(UUID u) {
        Set<String> s = odkrycia.get(u);
        if (s == null) return;
        katalog.mkdirs();
        YamlConfiguration y = new YamlConfiguration();
        y.set("odkrycia", new ArrayList<>(s));
        try { y.save(new File(katalog, u + ".yml")); } catch (IOException e) { plugin.getLogger().warning("zielnik: " + e); }
    }

    void zapiszWszystko() { for (UUID u : new ArrayList<>(odkrycia.keySet())) zapisz(u); }

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
