package pl.mcmmorpg.rzemioslo;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Komunikaty zielarza w jednym miejscu, cztery sposoby pokazania (config Rzemiosla: komunikaty.zielarz-wariant):
 * <ul>
 *  <li>v7 — jak dotad: pasek akcji z kodami kolorow vanilli, rada w podtytule na srodku ekranu i na czacie;</li>
 *  <li>a  — "jedna linia": tylko pasek akcji (arbiter MythicLib), paleta HUD, klawisze jako ikony, bez tytulu i czatu;</li>
 *  <li>b  — "przy roslinie": etykieta (TextDisplay, widzi ja tylko zbierajacy) nad roslina, zdarzenia na etykiecie
 *           + czasteczki; poza zbiorem (odmowy) pasek jak w A;</li>
 *  <li>c  — "tabliczka HUD": pasek akcji na ciemnej tabliczce w ramce HUD z paskiem tonu po lewej; nazwa rosliny
 *           i klawisz wyjscia stoja przez caly zbior, zdarzenia na chwile ja zastepuja.</li>
 * </ul>
 * Czat we wszystkich nowych wariantach tylko jako dziennik (podsumowanie zbioru — robi to Zielarz.zakoncz).
 * Tekst wejsciowy to stary napis z kodami § (ton z pierwszego koloru) i znacznikami {LPM} {PPM} {SHIFT}.
 * Grafika: MCMMORPG/profesje/zielarz/wyglad/grafika_ui.py (font zielarz:ui) — kody znakow musza sie zgadzac.
 */
final class ZKomunikaty {
    enum Ton { INFO, DOBRZE, UWAGA, ZLE }

    static final Key FONT = Key.key("zielarz", "ui");
    static final char LPM = '', PPM = '', SHIFT = '', TAB_P = '';
    static final int SZER_LPM = 10, SZER_PPM = 10, SZER_SHIFT = 24;   // advance = szerokosc png + 1
    // priorytety arbitra paska (skala ze skilla: MMOCore 10, nawigacja 20, info 30, odmowa 40)
    static final int P_STALY = 20, P_INFO = 30, P_ODMOWA = 40;

    /** Paleta HUD (zmierzona z okno_a.png / l1-okno): kolory starych kodow § przeniesione na HUD. */
    static final Map<Character, TextColor> PALETA = Map.of(
            'f', TextColor.color(0xFFF1CC), '7', TextColor.color(0xD5A96B), '8', TextColor.color(0xA88B66),
            'e', TextColor.color(0xF2D27A), 'a', TextColor.color(0xB5D47A), '2', TextColor.color(0x9CC062),
            '6', TextColor.color(0xE8A050), 'c', TextColor.color(0xE8705A));
    static final TextColor KREM = TextColor.color(0xFFF1CC);

    private final Rzemioslo plugin;
    private final Map<Integer, Integer> szer = new HashMap<>();
    private final Map<UUID, Stan> stany = new HashMap<>();
    /** Numer ostatniego komunikatu gracza: dalsze czesci dlugiego tekstu nie nadpisuja nowszego. */
    private final Map<UUID, Integer> numer = new HashMap<>();

    /** Stan pokazu w trakcie jednego zbioru. */
    private static final class Stan {
        String staly;              // linia stala (C: na tabliczce; B: druga linia etykiety)
        String nazwa;
        TextDisplay etykieta;
        BukkitTask zad;
        long zdarzenieDo;          // B: do kiedy na etykiecie stoi zdarzenie
        String zdarzenie;
    }

    ZKomunikaty(Rzemioslo plugin) {
        this.plugin = plugin;
        try (InputStream in = plugin.getResource("zielarz-szerokosci.txt")) {
            if (in != null) {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String l; (l = r.readLine()) != null; ) {
                    String[] t = l.trim().split(" ");
                    if (t.length == 2) szer.put(Integer.parseInt(t[0], 16), Integer.parseInt(t[1]));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Zielarz: brak tabeli szerokosci fontu: " + e);
        }
    }

    String wariant() {
        String w = plugin.getConfig().getString("komunikaty.zielarz-wariant", "v7");
        return w == null ? "v7" : w.trim().toLowerCase();
    }

    boolean stary() { return wariant().equals("v7"); }

    // ================================================================ wejscie z Zielarza

    /** Komunikat poza zbiorem (odmowa przy kepie): v7 stary pasek, nowe warianty pasek w palecie HUD. */
    void odmowa(Player p, String stary) { odmowa(p, stary, null); }

    void odmowa(Player p, String stary, String nowy) {
        String w = wariant();
        if (w.equals("v7")) { plugin.pasek(p, stary); return; }
        String t = nowy != null ? nowy : zamienKlawisze(stary);
        pasekCzesciami(p, P_ODMOWA, t, w.equals("c"), ton(t));
    }

    /** Zdarzenie w trakcie zbioru (ciecie, sonda, korzen...). Lokalizacja rosliny dla wariantu B. */
    void zdarzenie(Player p, Location roslina, String stary) { zdarzenie(p, roslina, stary, null); }

    void zdarzenie(Player p, Location roslina, String stary, String nowy) {
        String w = wariant();
        if (w.equals("v7")) { plugin.pasek(p, stary); return; }
        String t = nowy != null ? nowy : zamienKlawisze(stary);
        Ton ton = ton(t);
        int prio = ton == Ton.ZLE ? P_ODMOWA : P_INFO;
        switch (w) {
            case "b" -> {
                Stan st = stany.get(p.getUniqueId());
                if (st == null || st.etykieta == null || !st.etykieta.isValid()) { pasekKomponent(p, prio, 60, tekst(t)); return; }
                st.zdarzenie = t;
                st.zdarzenieDo = System.currentTimeMillis() + (ton == Ton.INFO ? 2500 : 3500);
                odswiezEtykiete(st);
                if (roslina != null && (ton == Ton.ZLE || ton == Ton.UWAGA)) {
                    Color c = ton == Ton.ZLE ? Color.fromRGB(0xD04E3A) : Color.fromRGB(0xE08A3C);
                    p.spawnParticle(Particle.DUST, roslina.clone().add(0, 0.35, 0), 10, 0.25, 0.2, 0.25, 0, new Particle.DustOptions(c, 0.9f));
                }
            }
            case "c" -> pasekCzesciami(p, prio, t, true, ton);
            default -> pasekCzesciami(p, prio, t, false, ton);
        }
    }

    /**
     * Poczatek zbioru: nazwa kepy, klawisz wyjscia i (pierwsze zbiory) rada.
     * radaStara = "krotka|pelna" (v7: podtytul + czat), radaNowa = tekst ze znacznikami klawiszy (moze miec "|" = dwa kroki).
     */
    void wejscie(Player p, Location roslina, Vector przod, String nazwa, String radaStara, String radaNowa) {
        String w = wariant();
        if (w.equals("v7")) {
            plugin.pasek(p, "§f" + nazwa + " §7· Shift: odejdź");
            if (radaStara != null) {
                String[] rr = radaStara.split("\\|", 2);
                p.showTitle(net.kyori.adventure.title.Title.title(Component.empty(), Component.text(rr[0], net.kyori.adventure.text.format.NamedTextColor.WHITE),
                        net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(300), java.time.Duration.ofSeconds(4), java.time.Duration.ofMillis(600))));
                p.sendMessage("§2Zielarz: §f" + rr[rr.length - 1]);
            }
            return;
        }
        koniec(p);
        Stan st = new Stan();
        st.nazwa = nazwa;
        stany.put(p.getUniqueId(), st);
        String[] kroki = radaNowa == null ? new String[0] : radaNowa.split("\\|");
        switch (w) {
            case "b" -> {
                st.staly = kroki.length > 0 ? kroki[0] : "§7{SHIFT} wstań";
                st.etykieta = etykieta(p, roslina, przod);
                odswiezEtykiete(st);
                // kolejne kroki rady i powrot do "Shift: wstan" na etykiecie; zdarzenia maja pierwszenstwo (zdarzenieDo)
                final String[] k = kroki;
                final int[] i = {0};
                final long[] nastepny = {System.currentTimeMillis() + 5000};
                st.zad = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    if (!p.isOnline() || st.etykieta == null || !st.etykieta.isValid()) { koniec(p); return; }
                    long t = System.currentTimeMillis();
                    if (st.zdarzenie != null && t > st.zdarzenieDo) { st.zdarzenie = null; odswiezEtykiete(st); }
                    if (t >= nastepny[0] && i[0] < k.length) {
                        i[0]++;
                        st.staly = i[0] < k.length ? k[i[0]] : "§7{SHIFT} wstań";
                        nastepny[0] = t + 5000;
                        odswiezEtykiete(st);
                    }
                }, 5L, 5L);
            }
            case "c" -> {
                st.staly = "§f" + nazwa + "   §7{SHIFT} wstań";
                pasekKomponent(p, P_STALY, 50, tabliczka(st.staly, Ton.INFO));
                final String[] k = kroki;
                final int[] i = {0};
                final int[] tik = {0};
                st.zad = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    if (!p.isOnline()) { koniec(p); return; }
                    tik[0] += 10;
                    // kroki rady: po 2 s nazwy kazdy krok 4 s, potem stala tabliczka z nazwa
                    int faza = (tik[0] - 40) / 80;
                    String linia = tik[0] >= 40 && faza < k.length ? k[faza] : st.staly;
                    if (canShow(p, P_STALY)) pasekKomponent(p, P_STALY, 30, tabliczka(linia, ton(linia)));
                }, 10L, 10L);
            }
            default -> {   // a: nazwa, potem kroki rady; bez stalej linii
                pasekKomponent(p, P_STALY, 50, tekst("§f" + nazwa + "   §7{SHIFT} wstań"));
                for (int j = 0; j < kroki.length; j++) {
                    final String krok = kroki[j];
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (stany.get(p.getUniqueId()) == st && canShow(p, P_STALY)) pasekKomponent(p, P_STALY, 80, tekst(krok));
                    }, 50L + 80L * j);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {   // pasek gasnie po ~3 s u klienta: powtorz krok
                        if (stany.get(p.getUniqueId()) == st && canShow(p, P_STALY)) pasekKomponent(p, P_STALY, 40, tekst(krok));
                    }, 50L + 80L * j + 40L);
                }
            }
        }
    }

    /** Koniec zbioru: zdejmij etykiete/zadanie; komunikat koncowy (moze byc null) jak odmowa. */
    void koniec(Player p, String komunikat) {
        koniec(p);
        if (komunikat != null) odmowa(p, komunikat);
    }

    void koniec(Player p) {
        Stan st = stany.remove(p.getUniqueId());
        if (st == null) return;
        if (st.zad != null) st.zad.cancel();
        if (st.etykieta != null && st.etykieta.isValid()) st.etykieta.remove();
    }

    void zatrzymajWszystko() {
        for (UUID u : new java.util.ArrayList<>(stany.keySet())) {
            Player p = Bukkit.getPlayer(u);
            Stan st = stany.remove(u);
            if (st != null) { if (st.zad != null) st.zad.cancel(); if (st.etykieta != null) st.etykieta.remove(); }
        }
    }

    // ================================================================ wariant B: etykieta przy roslinie

    private TextDisplay etykieta(Player p, Location roslina, Vector przod) {
        // Miejsce liczone od oka gracza, nie od rosliny: kamera przy zbiorze patrzy stromo w dol (30-40 st.), a stala
        // wysokosc nad kepa wypadala nad gorna krawedzia ekranu (kozlek, klient QA 21.10). Do tego zamroz() zeruje
        // predkosc, a klient zweza wtedy pole widzenia o polowe (70 -> 35 st. przy domyslnym fovEffectScale), wiec
        // etykieta stoi 9 st. nad srodkiem widoku (widoczna przy obu polach widzenia), nieco przed roslina; skala
        // rosnie z odlegloscia, wiec litery maja stala wielkosc na ekranie.
        Location oko = p.getEyeLocation();
        Location wyzej = oko.clone();
        wyzej.setPitch(Math.max(-60f, oko.getPitch() - 9f));
        double dl = Math.max(0.9, roslina.clone().add(0, 0.3, 0).distance(oko) * 0.85);
        Location l = oko.clone().add(wyzej.getDirection().multiply(dl));
        if (l.getY() < roslina.getY() + 0.5) l.setY(roslina.getY() + 0.5);
        final float skala = (float) (0.09 * dl);
        TextDisplay d = roslina.getWorld().spawn(l, TextDisplay.class, e -> {
            e.setVisibleByDefault(false);
            e.setPersistent(false);
            e.setBillboard(Display.Billboard.CENTER);
            e.setBackgroundColor(Color.fromARGB(205, 0x25, 0x1C, 0x2B));
            e.setShadowed(false);
            e.setSeeThrough(false);
            e.setLineWidth(150);
            e.setBrightness(new Display.Brightness(15, 15));
            e.setTransformation(new org.bukkit.util.Transformation(new org.joml.Vector3f(), new org.joml.Quaternionf(),
                    new org.joml.Vector3f(skala, skala, skala), new org.joml.Quaternionf()));
        });
        p.showEntity(plugin, d);
        return d;
    }

    private void odswiezEtykiete(Stan st) {
        if (st.etykieta == null || !st.etykieta.isValid()) return;
        String druga = st.zdarzenie != null ? st.zdarzenie : st.staly;
        Component c = Component.text(st.nazwa, KREM);
        if (druga != null && !druga.isEmpty()) c = c.append(Component.newline()).append(tekst(druga));
        st.etykieta.text(c);
    }

    // ================================================================ skladanie tekstu

    /** Stare napisy: klawisze myszy/Shift na znaczniki ikon. */
    static String zamienKlawisze(String s) {
        return s.replace("(LPM)", "{LPM}").replace("(PPM)", "{PPM}")
                .replaceAll("\\bLPM\\b", "{LPM}").replaceAll("\\bPPM\\b", "{PPM}").replaceAll("\\bShift\\b", "{SHIFT}");
    }

    static Ton ton(String s) {
        Matcher m = Pattern.compile("§([0-9a-fk-or])").matcher(s);
        if (!m.find()) return Ton.INFO;
        return switch (m.group(1).charAt(0)) {
            case 'c' -> Ton.ZLE;
            case '6' -> Ton.UWAGA;
            case 'a', 'e', '2' -> Ton.DOBRZE;
            default -> Ton.INFO;
        };
    }

    private static final Pattern ZNACZNIK = Pattern.compile("§([0-9a-fk-or])|\\{(LPM|PPM|SHIFT)\\}");

    /** Napis ze znacznikami -> komponent: kolory w palecie HUD, klawisze jako ikony z fontu zielarz:ui. */
    static Component tekst(String s) {
        TextComponent.Builder b = Component.text();
        TextColor kolor = KREM;
        Matcher m = ZNACZNIK.matcher(s);
        int i = 0;
        while (m.find()) {
            if (m.start() > i) b.append(Component.text(s.substring(i, m.start()), kolor));
            if (m.group(1) != null) { TextColor k = PALETA.get(m.group(1).charAt(0)); if (k != null) kolor = k; }
            else b.append(ikona(m.group(2)));
            i = m.end();
        }
        if (i < s.length()) b.append(Component.text(s.substring(i), kolor));
        return b.build();
    }

    static Component ikona(String klawisz) {
        char c = switch (klawisz) { case "LPM" -> LPM; case "PPM" -> PPM; default -> SHIFT; };
        return Component.text(String.valueOf(c)).font(FONT).color(TextColor.color(0xFFFFFF)).shadowColor(ShadowColor.none());
    }

    /** Szerokosc napisu w px GUI (font domyslny + ikony), do wysrodkowania tabliczki. */
    int szerokosc(String s) {
        int w = 0;
        Matcher m = ZNACZNIK.matcher(s);
        int i = 0;
        while (m.find()) {
            w += szerTekstu(s.substring(i, m.start()));
            if (m.group(2) != null) w += switch (m.group(2)) { case "LPM" -> SZER_LPM; case "PPM" -> SZER_PPM; default -> SZER_SHIFT; };
            i = m.end();
        }
        return w + szerTekstu(s.substring(i));
    }

    private int szerTekstu(String t) {
        int w = 0;
        for (int i = 0; i < t.length(); ) {
            int cp = t.codePointAt(i);
            w += cp == ' ' ? 4 : szer.getOrDefault(cp, 6);
            i += Character.charCount(cp);
        }
        return w;
    }

    /** Przesuniecie o n px w foncie zielarz:ui (znaki E100+ ujemne, E110+ dodatnie, potegi dwojki). */
    static String przesun(int n) {
        StringBuilder sb = new StringBuilder();
        int a = Math.abs(n);
        for (int bit = 8; bit >= 0; bit--) if ((a & (1 << bit)) != 0) sb.append((char) ((n < 0 ? 0xE100 : 0xE110) + bit));
        return sb.toString();
    }

    static final int PAD = 5;

    /** Tabliczka HUD pod napisem: [pasek tonu][srodek o szerokosci napisu][prawy brzeg], napis wysrodkowany na niej. */
    Component tabliczka(String s, Ton ton) {
        int t = szerokosc(s);
        int srodek = Math.max(8, t + 2 * PAD - 1);
        StringBuilder g = new StringBuilder();
        g.append((char) (0xE010 + ton.ordinal())).append(przesun(-1));   // lewy brzeg 4 px (advance 5)
        int reszta = srodek;
        for (; reszta > 255; reszta -= 128) g.append((char) 0xE027).append(przesun(-1));   // kafle sa 1..128 px: dluzsza tabliczka = kilka 128
        for (int bit = 7; bit >= 0; bit--) if ((reszta & (1 << bit)) != 0) g.append((char) (0xE020 + bit)).append(przesun(-1));
        g.append(TAB_P).append(przesun(-1));
        int calosc = 4 + srodek + 3;
        int start = 4 + PAD;
        g.append(przesun(-(calosc - start)));
        Component tlo = Component.text(g.toString()).font(FONT).color(TextColor.color(0xFFFFFF)).shadowColor(ShadowColor.none());
        Component powrot = Component.text(przesun(calosc - start - t)).font(FONT);
        return Component.text().append(tlo).append(tekst(s)).append(powrot).build();
    }

    // ================================================================ pasek przez arbiter MythicLib

    /** Najszerszy tekst w jednej linii paska: ekran 1280x720 przy GUI 3 ma 426 px, tabliczka dodaje 17. */
    static final int LIMIT = 300;

    /** Tekst dluzszy niz LIMIT idzie kilkoma kolejnymi pokazami (po zdaniach), zamiast wyjsc poza ekran przy GUI 3. */
    void pasekCzesciami(Player p, int prio, String t, boolean tab, Ton ton) {
        java.util.List<String> cz = podziel(t);
        int nr = numer.merge(p.getUniqueId(), 1, Integer::sum);
        for (int i = 0; i < cz.size(); i++) {
            String c = cz.get(i);
            boolean ost = i == cz.size() - 1;
            Runnable r = () -> {
                if (!p.isOnline() || numer.getOrDefault(p.getUniqueId(), 0) != nr) return;
                pasekKomponent(p, prio, ost ? 60 : 50, tab ? tabliczka(c, ton) : tekst(c));
            };
            if (i == 0) r.run(); else Bukkit.getScheduler().runTaskLater(plugin, r, 45L * i);
        }
    }

    /** Dzieli po zdaniach (". ", "? ", "! ") tak, by kazda czesc miescila sie w LIMIT; kolor z poprzedniej czesci przechodzi dalej. */
    java.util.List<String> podziel(String t) {
        java.util.List<String> wynik = new java.util.ArrayList<>();
        if (szerokosc(t) <= LIMIT) { wynik.add(t); return wynik; }
        java.util.List<String> zdania = new java.util.ArrayList<>();
        Matcher m = Pattern.compile("(?<=[.?!])\\s+").matcher(t);
        int i = 0;
        while (m.find()) { zdania.add(t.substring(i, m.start())); i = m.end(); }
        zdania.add(t.substring(i));
        StringBuilder biez = new StringBuilder();
        String kolor = "";
        for (String z : zdania) {
            String kand = biez.length() == 0 ? kolor + z : biez + " " + z;
            if (biez.length() > 0 && szerokosc(kand) > LIMIT) {
                wynik.add(biez.toString());
                kolor = ostatniKolor(biez.toString(), kolor);
                biez = new StringBuilder(kolor + z);
            } else biez = new StringBuilder(kand);
        }
        if (biez.length() > 0) wynik.add(biez.toString());
        return wynik;
    }

    private static String ostatniKolor(String s, String dotad) {
        int k = s.lastIndexOf('\u00a7');
        return k >= 0 && k + 1 < s.length() ? s.substring(k, k + 2) : dotad;
    }

    private boolean canShow(Player p, int prio) {
        try { return MMOPlayerData.get(p).getActionBar().canShow(prio); } catch (Throwable t) { return true; }
    }

    /** Arbiter MythicLib zajmuje pasek (priorytet, czas), a sam komponent wysylamy Adventure (kolory hex, font). */
    void pasekKomponent(Player p, int prio, long ticki, Component c) {
        try {
            if (!MMOPlayerData.get(p).getActionBar().show(prio, ticki, " ")) return;
        } catch (Throwable ignored) {
            // bez MythicLib (testy) pasek idzie wprost
        }
        p.sendActionBar(c);
    }

    static String legacy(Component c) { return LegacyComponentSerializer.legacySection().serialize(c); }
}
