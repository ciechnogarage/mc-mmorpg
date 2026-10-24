package pl.mcmmorpg.rzemioslo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * „Feeling” chwili zbioru kozlka: swiat reaguje zamiast tekstu. Trzy natezenia z configu `zielarz.feeling`
 * (subtelny | mmo | rytual | brak; bez klucza = mmo, decyzja wlasciciela 24.10). Wszystko tylko dla gracza w minigrze (cząstki i dzwieki
 * wysylane do niego, encje widoczne tylko dla niego), encje dodatkowe w ZRoslina.dodatki — znikaja z rosliną
 * (koniec, wyjscie, smierc, wylaczenie pluginu). Zadnych wlasnych zadan co tick: tik() wola sesja zielarza.
 */
final class ZFeeling {
    enum Tryb {
        BRAK, SUBTELNY, MMO, RYTUAL;
        boolean jest() { return this != BRAK; }
        /** Blysk w kolorze jakosci, dzwiek nagrody, napis +N i liczba EXP. */
        boolean nagroda() { return this == MMO || this == RYTUAL; }
        boolean rytual() { return this == RYTUAL; }
    }

    // paleta HUD (ZKomunikaty)
    static final int KREM = 0xFFF1CC, ZLOTO = 0xD5A96B, WYBORNY = 0xF2D27A, ZWYKLY = 0xB5D47A, SLABY = 0xE8A050, ZEPSUTY = 0xE8705A;
    private static final Particle.DustOptions PYLEK = new Particle.DustOptions(Color.fromRGB(0xF4EBC0), 0.35f);
    private static final Particle.DustOptions PYLEK_R = new Particle.DustOptions(Color.fromRGB(0xE9F5B0), 0.45f);
    private static final Particle.DustOptions POPIOL = new Particle.DustOptions(Color.fromRGB(0x6B6158), 0.6f);
    // iskry rytualu: maly DUST zamiast END_ROD (END_ROD z bliska = biale krzyze wielkosci dloni, kadr ryt-zep 22.10)
    private static final Particle.DustOptions ISKRA = new Particle.DustOptions(Color.fromRGB(0xF4F9D2), 0.3f);
    private static final Particle.DustOptions KORZEN_SWIATLO = new Particle.DustOptions(Color.fromRGB(0xC8E68C), 0.5f);
    private static final BlockData B_ROOTED = Material.ROOTED_DIRT.createBlockData(), B_COARSE = Material.COARSE_DIRT.createBlockData(),
            B_DIRT = Material.DIRT.createBlockData();
    static final NamespacedKey WINIETA = new NamespacedKey("zielarz", "misc/winieta"), WINIETA_MOCNA = new NamespacedKey("zielarz", "misc/winieta_mocna");

    private final Rzemioslo plugin;

    ZFeeling(Rzemioslo plugin) { this.plugin = plugin; }

    Tryb tryb() {
        String w = plugin.getConfig().getString("zielarz.feeling", "mmo");   // domyslnie mmo (wlasciciel 24.10: „mmo, rób model 3D i dźwięki”)
        return switch (w == null ? "" : w.trim().toLowerCase()) {
            case "brak", "off", "nie" -> Tryb.BRAK;
            case "subtelny" -> Tryb.SUBTELNY;
            case "rytual", "rytuał" -> Tryb.RYTUAL;
            default -> Tryb.MMO;
        };
    }

    /** Id przedmiotu Nexo modelu 3D korzenia (zielarz.korzen3d: a|b|c, „karta” = dwie stare karty). Warianty: grafika/korzen3d.py. */
    String korzen3d() {
        String w = plugin.getConfig().getString("zielarz.korzen3d", "b");
        w = w == null ? "b" : w.trim().toLowerCase();
        return switch (w) {
            case "a", "b", "c" -> "zielarz_k_kozlek_korzen3d_" + w;
            default -> null;
        };
    }

    /** Wlasne dzwieki (paczka Nexo, assets/zielarz/sounds.json) zamiast odpowiednikow vanilli: natezenie mmo i rytual. */
    static boolean wlasne(Zielarz.Sesja s) { return s.fx.nagroda(); }

    /** Wlasny dzwiek z paczki (klucz zielarz:kozlek.*) przy roslinie, tylko dla gracza w minigrze. */
    static void dzW(Zielarz.Sesja s, String klucz, float vol, float pitch) {
        s.p.playSound(s.r.o.clone(), "zielarz:kozlek." + klucz, SoundCategory.BLOCKS, vol, pitch);
    }

    // ================================================================ pomocnicze
    private static void dz(Zielarz.Sesja s, Sound d, float vol, float pitch) {
        s.p.playSound(s.r.o.clone(), d, SoundCategory.BLOCKS, vol, pitch);
    }

    private void pozniej(Zielarz.Sesja s, long ticki, Runnable r) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (!s.konczy && s.p.isOnline()) r.run(); }, ticki);
    }

    static Color kolor(int klasa) { return Color.fromRGB(klasa == 2 ? WYBORNY : klasa == 1 ? ZWYKLY : SLABY); }

    // ================================================================ wejscie
    /** Spojne podloze: pierscien ziemi wokol kepy (tylko u gracza; przywracane na koncu). Kafle zblizenia lezą na srodku. */
    void podloze(Zielarz.Sesja s, boolean pokaz) {
        Block pod = s.blok.getRelative(0, -1, 0);
        int[][] wokol = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        for (int i = 0; i < wokol.length; i++) {
            Block b = pod.getRelative(wokol[i][0], 0, wokol[i][1]);
            if (!pokaz) { s.p.sendBlockChange(b.getLocation(), b.getBlockData()); continue; }
            Material m = b.getType();
            if (m != Material.GRASS_BLOCK && m != Material.DIRT && m != Material.PODZOL && m != Material.COARSE_DIRT && m != Material.ROOTED_DIRT) continue;
            if (!b.getRelative(0, 1, 0).isPassable() && i > 0) continue;
            // srodek i krzyz: ziemia z korzeniami / gruboziarnista (jak rozgrzebana nasada), narozniki zostaja trawa
            BlockData d = i == 0 ? B_ROOTED : i <= 4 ? ((b.getX() * 31 + b.getZ() * 17 & 1) == 0 ? B_COARSE : B_ROOTED) : null;
            if (d != null) s.p.sendBlockChange(b.getLocation(), d);
        }
    }

    void wejscie(Zielarz.Sesja s) {
        podloze(s, true);
        if (wlasne(s)) dzW(s, "szelest", 0.5f, 1f); else dz(s, Sound.BLOCK_AZALEA_LEAVES_STEP, 0.5f, 0.85f);   // szelest rozety pod dlonia
        s.p.spawnParticle(Particle.DUST, s.r.swiat(0, 0.75, 0), 4, 0.15, 0.08, 0.15, PYLEK);
        if (s.fx.rytual()) {
            winieta(s, WINIETA);
            dz(s, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.45f, 0.7f);
            dz(s, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.35f, 0.55f);
            s.p.spawnParticle(Particle.DUST, s.r.swiat(0, 0.2, 0), 4, 0.25, 0.05, 0.25, ISKRA);
        }
    }

    /** Przyciemnione krawedzie ekranu: nakladka kamery (jak dynia), wyslana tylko klientowi — w ekwipunku nic sie nie zmienia. */
    void winieta(Zielarz.Sesja s, NamespacedKey tekstura) {
        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta m = it.getItemMeta();
        var eq = m.getEquippable();
        eq.setSlot(EquipmentSlot.HEAD);
        eq.setCameraOverlay(tekstura);
        m.setEquippable(eq);
        it.setItemMeta(m);
        s.p.sendEquipmentChange(s.p, EquipmentSlot.HEAD, it);
        s.winieta = tekstura;
    }

    void zdejmijWinieta(Player p) {
        p.sendEquipmentChange(p, EquipmentSlot.HEAD, p.getInventory().getHelmet());
    }

    // ================================================================ co 1 tik w trakcie sesji (wolane z Zielarz.tik)
    void tik(Zielarz.Sesja s, ZRoslina.Czesc roz, boolean ciagnie) {
        if (roz == null || roz.zabrana) return;
        // kolysanie rozety (wczesniej stala jak wycinanka): tylko gdy nikt jej nie trzyma i korzen nie wraca
        if (s.t % 10 == 0 && !ciagnie && !s.trzyma && s.napiecie <= 0) {
            double a = (s.fx.rytual() ? 2.2 : 3.0) * Math.sin(s.t * (s.fx.rytual() ? 0.06 : 0.09)), b = 1.6 * Math.sin(s.t * 0.05 + 1.3);
            s.r.przechyl(roz, ZRoslina.obrotZ(a).mul(ZRoslina.obrotX(b)), new Vector3f(0, (float) (s.postep * 0.5), 0), 0f, 10);
        }
        // pylek z baldachu
        if (s.t % (s.fx.rytual() ? 14 : 24) == 7)
            s.p.spawnParticle(Particle.DUST, s.r.swiat(0, 0.78, 0), s.fx.rytual() ? 2 : 1, 0.14, 0.06, 0.14, s.fx.rytual() ? PYLEK_R : PYLEK);
        if (s.fx.rytual()) {
            if (s.t % 30 == 15) s.p.spawnParticle(Particle.DUST, s.r.swiat(0, 0.08, 0), 2, 0.2, 0.02, 0.2, ISKRA);
            if (s.t % 40 == 0 && s.winieta != null) winieta(s, s.winieta);   // pelna synchronizacja ekwipunku zdejmuje nakladke
        }
    }

    // ================================================================ sonda i kopanie
    void sonda(Zielarz.Sesja s, Location l, boolean korzonek) {
        if (korzonek) {
            dz(s, Sound.BLOCK_ROOTED_DIRT_STEP, 0.6f, 0.5f);
            s.p.spawnParticle(Particle.BLOCK_CRUMBLE, l, 5, 0.06, 0.02, 0.06, B_ROOTED);
            // korzonek pod spodem drga: liscie rozety nad nim krotko sie przechylaja
            if (s.fx.rytual()) s.p.spawnParticle(Particle.DUST, l.clone().add(0, 0.05, 0), 5, 0.07, 0.08, 0.07, KORZEN_SWIATLO);
        } else {
            dz(s, Sound.BLOCK_SAND_STEP, 0.45f, 1.25f);
            s.p.spawnParticle(Particle.BLOCK_CRUMBLE, l, 4, 0.08, 0.01, 0.08, B_COARSE);
        }
    }

    void kop(Zielarz.Sesja s, Location l, boolean korzonek) {
        s.p.spawnParticle(Particle.BLOCK_CRUMBLE, l, 14, 0.12, 0.04, 0.12, B_COARSE);
        s.p.spawnParticle(Particle.BLOCK_CRUMBLE, l, 8, 0.1, 0.03, 0.1, B_ROOTED);
        dz(s, Sound.BLOCK_GRAVEL_HIT, 0.5f, 0.7f);
        if (korzonek) {
            pozniej(s, 3, () -> dz(s, Sound.BLOCK_HANGING_ROOTS_PLACE, 0.6f, 0.9f));   // korzonek wychodzi spod ziemi
            if (s.fx.rytual()) s.p.spawnParticle(Particle.DUST, l.clone().add(0, 0.06, 0), 8, 0.08, 0.06, 0.08, KORZEN_SWIATLO);
        } else {
            // pusto: ziemia osypuje sie z powrotem do dolka i na nasade (ciagnac bedzie trudniej) — widac i slychac, bez zdania
            pozniej(s, 4, () -> {
                dz(s, Sound.BLOCK_GRAVEL_FALL, 0.7f, 0.6f);
                s.p.spawnParticle(Particle.BLOCK_CRUMBLE, s.r.swiat(0, 0.06, 0), 16, 0.1, 0.03, 0.1, B_DIRT);
                s.p.spawnParticle(Particle.FALLING_DUST, s.r.swiat(0, 0.35, 0), 6, 0.12, 0.05, 0.12, B_DIRT);
            });
        }
    }

    // ================================================================ ciagniecie
    void ciagnij(Zielarz.Sesja s, int dl, boolean wPasmie, boolean zaMocno) {
        if (wPasmie && dl % 5 == 0) {   // rozeta szarpie liscmi
            if (wlasne(s)) dzW(s, "szelest", 0.3f, 0.9f); else dz(s, Sound.BLOCK_AZALEA_LEAVES_STEP, 0.3f, 0.75f);
        }
        if (wPasmie && dl % 6 == 3) s.p.spawnParticle(Particle.DUST, s.r.swiat(0, 0.75 + s.postep * 0.5, 0), 1, 0.12, 0.05, 0.12, PYLEK);
        if (zaMocno && dl % 6 == 0) {   // lodyga trzeszczy pod palcami
            if (wlasne(s)) dzW(s, "napiecie", 0.45f, 0.95f + (dl % 12) * 0.01f); else dz(s, Sound.BLOCK_BAMBOO_HIT, 0.45f, 0.55f);
        }
        if (s.fx.rytual() && wPasmie && dl % 8 == 0)
            s.p.spawnParticle(Particle.DUST, s.r.swiat(0, 0.04, 0), 2, 0.12, 0.02, 0.12, KORZEN_SWIATLO);
    }

    /** Ostrzezenie (dawniej „Korzen trzeszczy… popusc”): lodyga gnie sie na bok i trzeszczy. */
    void trzeszczy(Zielarz.Sesja s) {
        if (wlasne(s)) dzW(s, "napiecie", 0.7f, 0.85f); else dz(s, Sound.BLOCK_BAMBOO_BREAK, 0.55f, 0.6f);
        dz(s, Sound.BLOCK_HANGING_ROOTS_BREAK, 0.7f, 0.5f);
        s.p.spawnParticle(Particle.BLOCK_CRUMBLE, s.r.swiat(0, 0.05, 0), 10, 0.15, 0.03, 0.15, B_ROOTED);
    }

    void naderwany(Zielarz.Sesja s) {
        dz(s, Sound.BLOCK_ROOTED_DIRT_BREAK, 0.9f, 0.5f);
        s.p.spawnParticle(Particle.DUST, s.r.swiat(0, 0.06, 0), 8, 0.12, 0.03, 0.12, POPIOL);
    }

    // ================================================================ wynik
    /**
     * Korzen wyszedl. Zwraca, ile tikow trwa chwila (sesja nie konczy sie wczesniej).
     * klasa 2 wyborny / 1 zwykly / 0 slaby; zostaly = korzonki w ziemi (ich pekanie slychac i widac).
     */
    int wyciagniety(Zielarz.Sesja s, ZRoslina.Czesc roz, int klasa, int ile, String nazwa, double xp, int zostaly) {
        boolean r = s.fx.rytual();
        int unies = r ? 16 : 8, wisi = r ? 18 : 6;
        if (r) {
            winieta(s, WINIETA_MOCNA);
            s.r.skala(ZOOM_RYTUAL, 14);   // chwila zwalnia: wyrazne przyblizenie (1.1 bylo niewidoczne na nagraniu)
            for (ZRoslina.Klocek k : roz.klocki) if (k.y1 <= 1e-3) {   // swiecace korzenie
                k.d.setBrightness(ZRoslina.PODSWIETLENIE);
                k.d.setGlowing(true);
                k.d.setGlowColorOverride(Color.fromRGB(klasa == 2 ? 0xE9F5B0 : klasa == 1 ? 0xC8E68C : 0xB89070));
            }
            dz(s, Sound.BLOCK_BEACON_POWER_SELECT, 0.35f, klasa == 2 ? 1.3f : 0.9f);
        }
        // korzen wychodzi przed oczy, rosnie i kolysze sie jak wyrwany za rozete (obrot w plaszczyznie ekranu:
        // karty sa plaskie, obrot w bok pokazalby ich krawedz)
        float kz = r ? ZOOM_RYTUAL : 1f, kc = r ? 1.25f : 1.2f;
        s.r.przesunObrot(roz, new Vector3f(0, r ? 0.48f : 0.56f, -0.3f), ZRoslina.obrotZ(-16), kz, kc, unies);
        int pol = Math.max(3, wisi / 2);
        pozniej(s, unies + 1, () -> s.r.przesunObrot(roz, new Vector3f(0, r ? 0.53f : 0.61f, -0.33f), ZRoslina.obrotZ(11).mul(ZRoslina.obrotX(-6)), kz, kc, pol));
        pozniej(s, unies + 1 + pol, () -> s.r.przesunObrot(roz, new Vector3f(0, r ? 0.51f : 0.59f, -0.32f), ZRoslina.obrotZ(-4), kz, kc, wisi - pol + 2));
        // korzonki, ktore zostaly w ziemi: pekaja po kolei (widac grudki i slychac trzask), zamiast zdania
        for (int i = 0, n = 0; i < 4; i++) {
            if (!s.boczne[i] || s.poluzowane[i]) continue;
            int ii = i;
            double[] sx = {0, 0.3, 0, -0.3}, sz = {-0.3, 0, 0.3, 0};
            pozniej(s, 4 + 3L * n++, () -> {
                dz(s, Sound.BLOCK_HANGING_ROOTS_BREAK, 0.8f, 0.7f + ii * 0.08f);
                s.p.spawnParticle(Particle.BLOCK_CRUMBLE, s.r.swiat(sx[ii] * 0.8, 0.05, sz[ii] * 0.8), 8, 0.05, 0.02, 0.05, B_ROOTED);
            });
        }
        int lot = unies + wisi;
        pozniej(s, lot, () -> {
            for (ZRoslina.Klocek k : roz.klocki) k.d.setViewRange(0f);   // korzen „staje sie” przedmiotem, ktory leci do reki
            lotDoReki(s, klasa, ile, nazwa, xp, s.r.swiat(0, 0.36, -0.3), false);
        });
        return lot + czasLotu(s);
    }

    static final float ZOOM_RYTUAL = 1.3f;

    /** Tiki od startu lotu do konca chwili: lot do reki, potem (mmo/rytual) napis nagrody do zgaszenia. */
    private static int czasLotu(Zielarz.Sesja s) {
        boolean r = s.fx.rytual();
        int lot = 4 * (r ? 5 : 3) + 2;
        return lot + (s.fx.nagroda() ? (r ? 44 : 32) + 8 : 10);
    }

    /** Korzen sie urwal: gluchy trzask, rozeta wiednie i opada na bok, szary pyl; kawalek (gdy jest) leci do reki. */
    int urwany(Zielarz.Sesja s, ZRoslina.Czesc roz, boolean kawalek, int klasa, String nazwa, double xp) {
        dz(s, Sound.BLOCK_BAMBOO_BREAK, 0.8f, 0.45f);
        if (wlasne(s)) dzW(s, "wyrwanie", 0.8f, 0.7f); else dz(s, Sound.BLOCK_ROOTED_DIRT_BREAK, 0.8f, 0.45f);   // urwany: gluche, nizsze pekniecie
        s.p.spawnParticle(Particle.DUST, s.r.swiat(0, 0.1, 0), 18, 0.18, 0.08, 0.18, POPIOL);
        s.p.spawnParticle(Particle.BLOCK_CRUMBLE, s.r.swiat(0, 0.05, 0), 16, 0.16, 0.03, 0.16, B_DIRT);
        // wiedniecie: liscie ciemnieja i kladą sie (obrot na bok, rozeta opada)
        Display.Brightness zwiedla = new Display.Brightness(0, Math.max(0, s.r.jasnosc.getSkyLight() - 7));
        pozniej(s, 5, () -> {
            for (ZRoslina.Klocek k : roz.klocki) k.d.setBrightness(zwiedla);
            s.r.przechyl(roz, ZRoslina.obrotZ(-38).mul(ZRoslina.obrotX(12)), new Vector3f(0.22f, -0.02f, -0.12f), 0f, s.fx.rytual() ? 16 : 10);
        });
        if (s.fx.rytual()) {
            winieta(s, WINIETA_MOCNA);
            dz(s, Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 0.7f);
            dz(s, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.6f, 0.5f);
            pozniej(s, 30, () -> winieta(s, WINIETA));
        }
        if (kawalek) pozniej(s, 12, () -> lotDoReki(s, klasa, 1, nazwa, xp, s.r.swiat(0, 0.12, -0.1), true));
        return kawalek ? 12 + czasLotu(s) : 26;
    }

    /** Przedmiot zbioru leci lukiem z rosliny do dloni; mmo/rytual: blysk w kolorze jakosci, dzwiek nagrody, napis +N i EXP. */
    private void lotDoReki(Zielarz.Sesja s, int klasa, int ile, String nazwa, double xp, Location start, boolean kawalek) {
        boolean r = s.fx.rytual();
        Player p = s.p;
        // model 3D korzenia (gdy jest): leci bryla, ktora gracz wlasnie wyrwal; kawalek urwanego = mniejszy. Inaczej plaski obrazek przedmiotu.
        boolean m3d = s.korzen3d != null;
        float s0 = m3d ? (kawalek ? 0.3f : 0.5f) : 0.32f, s1 = m3d ? (kawalek ? 0.12f : 0.18f) : 0.12f;
        ItemStack it = m3d ? ZRoslina.przedmiot(s.korzen3d) : s.ostatniPrzedmiot != null ? s.ostatniPrzedmiot.clone() : new ItemStack(Material.SHORT_GRASS);
        it.setAmount(1);
        ItemDisplay d = s.r.dodatek(ItemDisplay.class, start, e -> {
            e.setItemStack(it);
            e.setItemDisplayTransform(m3d ? ItemDisplay.ItemDisplayTransform.NONE : ItemDisplay.ItemDisplayTransform.GUI);
            e.setBillboard(m3d ? Display.Billboard.FIXED : Display.Billboard.CENTER);
            e.setBrightness(ZRoslina.PODSWIETLENIE);
            // maly: kamera stoi tuz przy roslinie (zblizenie), 0,42 zaslanialo pol ekranu; bez obrysu glow (gruba, kanciasta ramka)
            e.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(s0), new Quaternionf()));
            e.setTeleportDuration(r ? 5 : 3);
        });
        dz(s, Sound.BLOCK_MOSS_STEP, 0.5f, 1.2f);
        // luk Beziera: start -> wierzcholek nad srodkiem -> dlon (prawa, przed kamera)
        int kroki = 4, co = r ? 5 : 3;
        for (int k = 1; k <= kroki; k++) {
            double tt = k / (double) kroki;
            int kk = k;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!d.isValid() || !p.isOnline()) return;
                Location oko = p.getEyeLocation();
                Vector dir = oko.getDirection();
                Vector prawo = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
                // dlon w kadrze (0,5 m przed okiem lecialo poza dolna krawedz ekranu i bylo uciete)
                Location reka = oko.clone().add(dir.clone().multiply(0.95)).add(prawo.multiply(0.38)).add(0, -0.34, 0);
                Location szczyt = start.clone().add(reka).multiply(0.5).add(0, r ? 0.7 : 0.55, 0);
                double a = (1 - tt) * (1 - tt), b = 2 * tt * (1 - tt), c = tt * tt;
                Location l = start.clone().multiply(a).add(szczyt.toVector().multiply(b)).add(reka.toVector().multiply(c));
                d.teleport(l);
                // krec sie w locie (<180 st. na krok, zeby interpolacja nie zawracala) i maleje do dloni; bryla 3D obraca sie wokol
                // pionu z lekkim przechylem (widac korzenie z kazdej strony), plaski obrazek w plaszczyznie ekranu
                Quaternionf obr = m3d ? ZRoslina.obrotY(kk * (r ? 110 : 85)).mul(ZRoslina.obrotX(-18 * kk / (double) kroki))
                        : ZRoslina.obrotZ(-kk * (r ? 120 : 95));
                ZRoslina.animuj(d, new Transformation(new Vector3f(), obr, new Vector3f((float) (s0 + (s1 - s0) * tt)), new Quaternionf()), co);
                if (kk == kroki) Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    d.remove();
                    if (!p.isOnline()) return;
                    p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.6f, klasa == 2 ? 1.4f : 1.0f);
                    // mmo/rytual: nagroda w chwili, gdy zbior trafia do reki (blysk w kolorze jakosci, dzwiek, napis)
                    if (s.fx.nagroda()) nagroda(s, klasa, ile, nazwa, xp);
                }, co);
            }, 1L + (long) (k - 1) * co);
        }
    }

    private void nagroda(Zielarz.Sesja s, int klasa, int ile, String nazwa, double xp) {
        boolean r = s.fx.rytual();
        Player p = s.p;
        Location ucho = p.getLocation();
        switch (klasa) {
            case 2 -> {
                if (wlasne(s)) p.playSound(ucho, "zielarz:kozlek.wyborny", SoundCategory.PLAYERS, 0.9f, 1.0f);
                else {
                    p.playSound(ucho, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 1.0f);
                    p.playSound(ucho, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.35f, 1.7f);
                }
                if (r) {   // akord: kwinta i oktawa na dzwonku
                    pozniej(s, 3, () -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 0.5f, 1.0f));
                    pozniej(s, 6, () -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 0.5f, 1.5f));
                    pozniej(s, 9, () -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 0.5f, 2.0f));
                }
            }
            case 1 -> p.playSound(ucho, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.6f, 1.1f);
            default -> p.playSound(ucho, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 0.6f, 0.8f);
        }
        String jak = klasa == 2 ? "Wyborny" : klasa == 1 ? "Zwykły" : "Słaby";
        TextColor kol = TextColor.color(klasa == 2 ? WYBORNY : klasa == 1 ? ZWYKLY : SLABY);
        Component tekst = Component.text("+" + ile + " " + nazwa, kol)
                .append(Component.newline())
                .append(Component.text(jak, kol))
                .append(Component.text(" · ", TextColor.color(ZLOTO)))
                .append(Component.text("+" + Math.round(xp) + " EXP", TextColor.color(KREM)));
        // przed oczami, lekko nad srodkiem ekranu: gracz siedzi tuz przy roslinie, punkt nad roslina wypadal 0,5 m od kamery (olbrzymie litery)
        Location oko = p.getEyeLocation();
        Location gdzie = oko.clone().add(oko.getDirection().multiply(2.4)).add(0, -0.04, 0);   // na srodku ekranu, pod lista zadan HUD
        // blysk w kolorze jakosci wokol napisu (nie przy dloni: czastka 0,5 m od kamery to wielki kwadrat na pol ekranu)
        // drobny pyl (0,4): END_ROD i wiekszy DUST na 2,4 m to biale kleksy (sprawdzone w kliencie QA 22.10)
        p.spawnParticle(Particle.DUST, gdzie, klasa == 2 ? 14 : 8, 0.5, 0.14, 0.25, new Particle.DustOptions(kolor(klasa), 0.4f));
        if (klasa == 2) p.spawnParticle(Particle.DUST, gdzie, r ? 10 : 6, 0.45, 0.12, 0.25, new Particle.DustOptions(Color.fromRGB(ZLOTO), 0.35f));
        TextDisplay t = s.r.dodatek(TextDisplay.class, gdzie, e -> {
            e.text(tekst);
            e.setBillboard(Display.Billboard.CENTER);
            e.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            e.setShadowed(true);
            e.setAlignment(TextDisplay.TextAlignment.CENTER);
            e.setBrightness(ZRoslina.PODSWIETLENIE);
            e.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0.1f), new Quaternionf()));
            e.setTeleportDuration(r ? 40 : 28);
        });
        // wyskakuje (0,1 -> 0,5), plynie w gore i znika
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!t.isValid()) return;
            ZRoslina.animuj(t, new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0.42f), new Quaternionf()), 4);
            t.teleport(gdzie.clone().add(0, r ? 0.14 : 0.1, 0));
        }, 1L);
        int trwa = r ? 44 : 32;
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (t.isValid()) ZRoslina.animuj(t, new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0f), new Quaternionf()), 5); }, trwa);
        Bukkit.getScheduler().runTaskLater(plugin, t::remove, trwa + 6L);
        if (r) pozniej(s, trwa, () -> { winieta(s, WINIETA); s.r.skala(1f, 12); });
    }

    /** Koniec sesji: przywroc podloze i helm (nakladka); encje dodatkowe sprzata ZRoslina.usun(). */
    void koniec(Zielarz.Sesja s) {
        Player p = s.p;
        if (!p.isOnline() || p.getWorld() != s.blok.getWorld()) return;
        podloze(s, false);
        if (s.winieta != null) { zdejmijWinieta(p); s.winieta = null; }
    }
}
