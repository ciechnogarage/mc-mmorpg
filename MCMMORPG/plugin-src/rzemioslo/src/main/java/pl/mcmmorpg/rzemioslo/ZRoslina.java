package pl.mcmmorpg.rzemioslo;

import com.nexomc.nexo.api.NexoItems;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Roslina zielarza z bliska: karty z pixel-artem (ItemDisplay, te same tekstury co blok kepy w swiecie) i klocki,
 *  widoczne tylko dla jednego gracza. WSZYSTKIE encje stoja w jednym punkcie (podstawa kepy) — pozycje siedza
 *  w translacji, wiec cala roslina skaluje sie jedna animacja: od rozmiaru bloku w swiecie do zblizenia i z powrotem.
 *  Uklad lokalny rosliny: x w prawo gracza, y w gore, z od gracza; uklad encji ma x odwrocony (obrot o yaw). */
final class ZRoslina {
    static final class Klocek {
        final Display d;
        final Vector3f t0;       // translacja wzgledem podstawy rosliny (uklad encji), przy skali 1
        final Quaternionf r0;
        final Vector3f s0;
        final float y0, y1;      // zakres wysokosci wzgledem kotwicy czesci (do ciecia/przechylu)
        int seg = -1;            // numer segmentu pedu (karty)

        Klocek(Display d, Vector3f t0, Quaternionf r0, Vector3f s0, float y0, float y1) {
            this.d = d; this.t0 = t0; this.r0 = r0; this.s0 = s0; this.y0 = y0; this.y1 = y1;
        }
    }

    /** Czesc rosliny, w ktora gracz celuje. */
    static final class Czesc {
        final String typ;            // ped, kwiat, lisc, rozeta, gleba, pochwa
        final int nr;
        final Vector kotwica;        // uklad rosliny
        final Vector3f kotwicaE;     // to samo w ukladzie encji
        BoundingBox box;             // uklad rosliny
        final List<Klocek> klocki = new ArrayList<>();
        final Map<String, Object> cechy = new HashMap<>();
        boolean zabrana;

        Czesc(String typ, int nr, Vector kotwica) {
            this.typ = typ; this.nr = nr; this.kotwica = kotwica;
            this.kotwicaE = new Vector3f((float) -kotwica.getX(), (float) kotwica.getY(), (float) kotwica.getZ());
        }

        double d(String k) { Object o = cechy.get(k); return o instanceof Number n ? n.doubleValue() : 0; }
        boolean b(String k) { return Boolean.TRUE.equals(cechy.get(k)); }
        String s(String k) { Object o = cechy.get(k); return o == null ? "" : o.toString(); }
    }

    final Plugin plugin;
    final Player p;
    final Location o;            // podstawa rosliny — NIGDY nie oddawac na zewnatrz bez clone() (MMOCore przesuwa lokacje)
    final Vector f, r;           // przod (od gracza do rosliny) i prawo gracza
    final float yaw;
    final List<Czesc> czesci = new ArrayList<>();
    float skalaStartu = 1f;      // nowe encje rodza sie w tej skali (rozmiar kepy w swiecie), potem przyblizenie
    /** Swiatlo jak blok kepy (bez tego karty ItemDisplay sa ciemniejsze od rosliny w swiecie). */
    final Display.Brightness jasnosc;
    static final Display.Brightness PODSWIETLENIE = new Display.Brightness(15, 15);
    private Czesc podswietlona;
    /** Niewidzialna encja nad roslina: celownik trafia w nia, nie w blok trawy — klient nie rysuje obrysu bloku. */
    private org.bukkit.entity.Interaction zaslona;

    ZRoslina(Plugin plugin, Player p, Location o, Vector przod) {
        this.plugin = plugin;
        this.p = p;
        this.o = o.clone();
        this.f = new Vector(przod.getX(), 0, przod.getZ()).normalize();
        this.r = new Vector(-f.getZ(), 0, f.getX());
        this.yaw = (float) Math.toDegrees(Math.atan2(-f.getX(), f.getZ()));
        this.o.setYaw(yaw);
        this.o.setPitch(0);
        var b = this.o.getBlock();
        this.jasnosc = new Display.Brightness(Math.min(15, b.getLightFromBlocks() + 2), Math.min(15, b.getLightFromSky()));
    }

    Location swiat(double lx, double ly, double lz) {
        return o.clone().add(r.clone().multiply(lx)).add(0, ly, 0).add(f.clone().multiply(lz));
    }

    Vector lokalnie(Vector swiatowy) {
        Vector d = swiatowy.clone().subtract(o.toVector());
        return new Vector(d.dot(r), d.getY(), d.dot(f));
    }

    private Vector kierunekLokalny(Vector kier) { return new Vector(kier.dot(r), kier.getY(), kier.dot(f)); }

    Czesc czesc(String typ, int nr, double kx, double ky, double kz) {
        Czesc c = new Czesc(typ, nr, new Vector(kx, ky, kz));
        czesci.add(c);
        return c;
    }

    private <T extends Display> T zrodz(Class<T> typ, java.util.function.Consumer<T> ustaw, Vector3f t, Quaternionf q, Vector3f s) {
        T d = o.getWorld().spawn(o.clone(), typ, e -> {
            e.setVisibleByDefault(false);
            e.setPersistent(false);
            ustaw.accept(e);
            e.setTransformation(new Transformation(new Vector3f(t).mul(skalaStartu), q, new Vector3f(s).mul(skalaStartu), new Quaternionf()));
            e.setViewRange(0.5f);
            e.setShadowRadius(0);
            e.setBrightness(jasnosc);
        });
        p.showEntity(plugin, d);
        return d;
    }

    /** Encje dodatkowe chwili (lecacy przedmiot, napis nagrody): widoczne tylko dla gracza, usuwane razem z roslina. */
    final List<Display> dodatki = new ArrayList<>();

    <T extends Display> T dodatek(Class<T> typ, Location gdzie, java.util.function.Consumer<T> ustaw) {
        T d = o.getWorld().spawn(gdzie.clone(), typ, e -> {
            e.setVisibleByDefault(false);
            e.setPersistent(false);
            e.setViewRange(0.5f);
            e.setShadowRadius(0);
            ustaw.accept(e);
        });
        dodatki.add(d);
        p.showEntity(plugin, d);
        return d;
    }

    /** Klocek (BlockDisplay) o srodku podstawy w (lx,ly,lz) wzgledem kotwicy czesci, rozmiar (sx,sy,sz), obrot wokol srodka podstawy. */
    Klocek klocek(Czesc c, Material m, double lx, double ly, double lz, double sx, double sy, double sz, Quaternionf rot) {
        Quaternionf q = rot == null ? new Quaternionf() : new Quaternionf(rot);
        Vector3f s = new Vector3f((float) sx, (float) sy, (float) sz);
        Vector3f t = new Vector3f((float) sx / 2f, 0, (float) sz / 2f).rotate(q).negate()
                .add((float) -lx, (float) ly, (float) lz).add(c.kotwicaE);
        BlockDisplay d = zrodz(BlockDisplay.class, e -> e.setBlock(m.createBlockData()), t, q, s);
        Klocek k = new Klocek(d, t, q, s, (float) ly, (float) (ly + sy));
        c.klocki.add(k);
        return k;
    }

    /** Karta z tekstura (przedmiot Nexo: krzyzowy jak kwiat albo plaszczyzna), kwadrat o boku s.
     *  (lx,ly,lz) = srodek DOLNEJ krawedzi karty wzgledem kotwicy; obrot wokol tego punktu. */
    Klocek karta(Czesc c, String nexoId, double lx, double ly, double lz, double s, Quaternionf rot) {
        // lekkie odchylenie od gracza: normalne kart ku gornemu swiatlu encji (pionowa karta wychodzi ciemna)
        Quaternionf q = new Quaternionf().rotateX((float) Math.toRadians(ODCHYL)).mul(rot == null ? new Quaternionf() : rot);
        Vector3f t = new Vector3f(0, (float) s / 2f, 0).rotate(q).add((float) -lx, (float) ly, (float) lz).add(c.kotwicaE);
        return kartaW(c, nexoId, t, (float) s, q, (float) ly, (float) (ly + s));
    }

    /** Karta o srodku w (lx,ly,lz) wzgledem kotwicy (np. lisc lezacy, placek ziemi). */
    Klocek kartaSrodek(Czesc c, String nexoId, double lx, double ly, double lz, double s, Quaternionf rot) {
        Quaternionf q = rot == null ? new Quaternionf() : new Quaternionf(rot);
        Vector3f t = new Vector3f((float) -lx, (float) ly, (float) lz).add(c.kotwicaE);
        return kartaW(c, nexoId, t, (float) s, q, (float) (ly - s / 2), (float) (ly + s / 2));
    }

    /** t = srodek karty. Karta to BlockDisplay wariantu bloku Nexo (model od rogu 0..1, wiec przesuwamy o pol boku). */
    /** Odchylenie kart od gracza [st.]: swiatlo encji = 0,4 + 0,6*(n.L0+n.L1); pionowa karta ~0,74, odchylona o 28 st. ~0,9. */
    static double ODCHYL = 28;
    static boolean JAKO_PRZEDMIOT = true;   // test A/B: karta jako ItemDisplay (true) albo BlockDisplay bloku Nexo

    private Klocek kartaW(Czesc c, String nexoId, Vector3f t, float s, Quaternionf q, float y0, float y1) {
        if (JAKO_PRZEDMIOT) {
            ItemStack it = przedmiot(nexoId);
            Vector3f sk = new Vector3f(s, s, s);
            ItemDisplay d = zrodz(ItemDisplay.class, e -> {
                e.setItemStack(it);
                e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            }, t, q, sk);
            Klocek k = new Klocek(d, t, q, sk, y0, y1);
            c.klocki.add(k);
            return k;
        }
        org.bukkit.block.data.BlockData bd = blok(nexoId);
        Vector3f sk = new Vector3f(s, s, s);
        Vector3f rog = new Vector3f(t).sub(new Vector3f(s / 2f, s / 2f, s / 2f).rotate(q));
        BlockDisplay d = zrodz(BlockDisplay.class, e -> e.setBlock(bd), rog, q, sk);
        Klocek k = new Klocek(d, rog, q, sk, y0, y1);
        c.klocki.add(k);
        return k;
    }

    static org.bukkit.block.data.BlockData blok(String nexoId) {
        try {
            var b = com.nexomc.nexo.api.NexoBlocks.blockData(nexoId);
            if (b != null) return b;
        } catch (Throwable ignored) { }
        return Material.SHORT_GRASS.createBlockData();
    }

    static ItemStack przedmiot(String nexoId) {
        try {
            var b = NexoItems.itemFromId(nexoId);
            if (b != null) return b.build();
        } catch (Throwable ignored) { }
        return new ItemStack(Material.SHORT_GRASS);
    }

    /** Cala roslina do skali k (1 = zblizenie) w `ticki` tikow — przyblizenie i oddalenie. */
    void skala(float k, int ticki) {
        for (Czesc c : czesci) for (Klocek kl : c.klocki)
            animuj(kl.d, new Transformation(new Vector3f(kl.t0).mul(k), kl.r0, new Vector3f(kl.s0).mul(k), new Quaternionf()), ticki);
    }

    /** Czesc pod celownikiem gracza (najblizsza), albo null. */
    Czesc celowana(double zasieg) {
        Location oko = p.getEyeLocation();
        Vector start = lokalnie(oko.toVector());
        Vector kier = kierunekLokalny(oko.getDirection()).normalize();
        Czesc best = null;
        double bd = zasieg;
        for (Czesc c : czesci) {
            if (c.zabrana || c.box == null) continue;
            RayTraceResult hit = c.box.rayTrace(start, kier, bd);
            if (hit != null) {
                double dd = hit.getHitPosition().distance(start);
                if (dd < bd) { bd = dd; best = c; }
            }
        }
        return best;
    }

    /** Punkt trafienia (uklad rosliny) w czesc. */
    Vector trafienie(Czesc c) {
        Location oko = p.getEyeLocation();
        Vector start = lokalnie(oko.toVector());
        Vector kier = kierunekLokalny(oko.getDirection()).normalize();
        RayTraceResult hit = c.box == null ? null : c.box.rayTrace(start, kier, 8);
        return hit == null ? null : hit.getHitPosition();
    }

    /** Czesc pod celownikiem swieci pelnym swiatlem (puls skali 7% byl niewidoczny na kadrach). */
    void podswietl(Czesc c, Color kolor) {
        if (c == podswietlona) return;
        podswietlona = c;
        // w sloncu pelne swiatlo niczego nie zmienialo: wskazana czesc jasna, reszta rosliny przygaszona
        // lekki cien (-2): mocniejsze przygaszenie (-6) robilo z kwitnacej kepy brunatna (klient v6)
        Display.Brightness przygas = new Display.Brightness(Math.max(0, jasnosc.getBlockLight() - 1), Math.max(0, jasnosc.getSkyLight() - 2));
        for (Czesc x : czesci) {
            if (x.typ.equals("gleba")) continue;
            Display.Brightness b = c == null ? jasnosc : x == c ? PODSWIETLENIE : przygas;
            for (Klocek k : x.klocki) k.d.setBrightness(b);
        }
    }

    /** Linia ciecia: cienka kreska w poprzek pedu na wysokosci celownika, w kolorze strefy jakosci (null = schowaj). */
    private Klocek linia;
    private Material liniaKolor;

    void linia(Czesc c, double y, Material kolor) {
        if (c == null || kolor == null) {
            if (linia != null) linia.d.setViewRange(0f);
            return;
        }
        float sx = 0.12f, sy = 0.012f, sz = 0.012f;   // na samej lodydze (0,34 przecinalo 3-4 pedy naraz)
        Vector3f t = new Vector3f(-sx / 2f - (float) c.kotwica.getX(), (float) y, -sz / 2f + (float) c.kotwica.getZ() - 0.05f);
        if (linia == null || kolor != liniaKolor) {
            if (linia != null) linia.d.remove();
            Czesc tmp = new Czesc("linia", 0, new Vector());
            linia = klocek(tmp, kolor, 0, 0, 0, sx, sy, sz, null);
            linia.d.setBrightness(PODSWIETLENIE);
            liniaKolor = kolor;
        }
        linia.d.setViewRange(0.5f);
        animuj(linia.d, new Transformation(t, new Quaternionf(), new Vector3f(sx, sy, sz), new Quaternionf()), 1);
    }

    /** Obrot czesci wokol punktu (0, odY, 0) jej kotwicy + przesuniecie; klocki konczace sie ponizej odY zostaja. */
    void przechyl(Czesc c, Quaternionf q, Vector3f przesun, float odY, int ticki) {
        Vector3f piv = new Vector3f(c.kotwicaE).add(0, odY, 0);
        for (Klocek k : c.klocki) {
            if (k.y1 <= odY + 1e-3) continue;
            Vector3f t = new Vector3f(k.t0).sub(piv).rotate(q).add(piv).add(przesun);
            animuj(k.d, new Transformation(t, new Quaternionf(q).mul(k.r0), k.s0, new Quaternionf()), ticki);
        }
    }

    /** Wiatr: wspolna fala przechodzi przez kepe od lewej do prawej (faza z polozenia), ~5 st., co kilka sekund podmuch. */
    void wiatr(int t, Czesc pomin, Czesc pomin2) {
        double podmuch = Math.max(0, Math.sin(t * 0.021)) > 0.85 ? 4 : 0;
        for (Czesc c : czesci) {
            if (c.zabrana || c == pomin || c == pomin2 || c.klocki.isEmpty()) continue;
            if (!c.typ.equals("ped") && !c.typ.equals("lisc") && !c.typ.equals("kwiat")) continue;
            double a = (4.5 + podmuch) * Math.sin(t * 0.11 - c.kotwica.getX() * 5);
            przechyl(c, obrotZ(a).mul(obrotX(a * 0.35)), new Vector3f(), 0f, 10);
        }
    }

    /** Przesuniecie calej czesci (wyciaganie korzenia, drzenie). */
    void przesun(Czesc c, Vector3f v, int ticki) {
        for (Klocek k : c.klocki) animuj(k.d, new Transformation(new Vector3f(k.t0).add(v), k.r0, k.s0, new Quaternionf()), ticki);
    }

    /**
     * Cala czesc (z korzeniami): obrot q wokol kotwicy i powiekszenie kCzesc, potem skala calej rosliny kSwiat
     * (zgodna z {@link #skala}) i przesuniecie v — wyjecie korzenia „w reke”.
     */
    void przesunObrot(Czesc c, Vector3f v, Quaternionf q, float kSwiat, float kCzesc, int ticki) {
        Vector3f piv = new Vector3f(c.kotwicaE);
        for (Klocek kl : c.klocki) {
            Vector3f t = new Vector3f(kl.t0).sub(piv).rotate(q).mul(kCzesc).add(piv).mul(kSwiat).add(v);
            animuj(kl.d, new Transformation(t, new Quaternionf(q).mul(kl.r0), new Vector3f(kl.s0).mul(kSwiat * kCzesc), new Quaternionf()), ticki);
        }
    }

    static void animuj(Display d, Transformation t, int ticki) {
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(ticki);
        d.setTransformation(t);
    }

    void usunCzesc(Czesc c) {
        if (podswietlona == c) podswietlona = null;
        for (Klocek k : c.klocki) k.d.remove();
        c.klocki.clear();
        c.zabrana = true;
    }

    static final String ZASLONA = "rzemioslo_zaslona";

    void zaslon(float szer, float wys) {
        zaslona = o.getWorld().spawn(o.clone().subtract(f.clone().multiply(0.1)), org.bukkit.entity.Interaction.class, e -> {
            e.setVisibleByDefault(false);
            e.setPersistent(false);
            e.setInteractionWidth(szer);
            e.setInteractionHeight(wys);
            e.setResponsive(false);
            e.addScoreboardTag(ZASLONA);   // PPM w zaslone nie moze trafic do WorldGuard („can't use that here”)
        });
        p.showEntity(plugin, zaslona);
    }

    void usun() {
        if (zaslona != null) { zaslona.remove(); zaslona = null; }
        if (linia != null) { linia.d.remove(); linia = null; }
        for (Czesc c : czesci) for (Klocek k : c.klocki) k.d.remove();
        czesci.clear();
        for (Display d : dodatki) d.remove();
        dodatki.clear();
        podswietlona = null;
    }

    static Quaternionf obrotZ(double stopnie) { return new Quaternionf().rotateZ((float) Math.toRadians(stopnie)); }
    static Quaternionf obrotX(double stopnie) { return new Quaternionf().rotateX((float) Math.toRadians(stopnie)); }
    static Quaternionf obrotY(double stopnie) { return new Quaternionf().rotateY((float) Math.toRadians(stopnie)); }
}
