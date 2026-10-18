package pl.mcmmorpg.rzemioslo;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
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

/** Powiekszona roslina zielarza: klocki (BlockDisplay) widoczne tylko dla jednego gracza, czesci z hitboxami
 *  w ukladzie lokalnym rosliny (x w prawo gracza, y w gore, z od gracza). Klocki czesci kotwiczone w jednym punkcie
 *  (podstawa pedu), wiec cala czesc mozna przechylic/podniesc jedna transformacja. */
final class ZRoslina {
    static final class Klocek {
        final BlockDisplay d;
        final Vector3f t0;       // translacja wzgledem kotwicy (rog klocka po obrocie)
        final Quaternionf r0;
        final Vector3f s0;
        final float y0, y1;      // zakres wysokosci w ukladzie kotwicy (do ciecia)

        Klocek(BlockDisplay d, Vector3f t0, Quaternionf r0, Vector3f s0, float y0, float y1) {
            this.d = d; this.t0 = t0; this.r0 = r0; this.s0 = s0; this.y0 = y0; this.y1 = y1;
        }
    }

    /** Czesc rosliny, w ktora gracz celuje. */
    static final class Czesc {
        final String typ;            // ped, kwiat, lisc, rozeta, gleba
        final int nr;
        final Vector kotwica;        // lokalnie
        BoundingBox box;             // lokalnie
        final List<Klocek> klocki = new ArrayList<>();
        final Map<String, Object> cechy = new HashMap<>();
        boolean zabrana;

        Czesc(String typ, int nr, Vector kotwica) { this.typ = typ; this.nr = nr; this.kotwica = kotwica; }

        double d(String k) { Object o = cechy.get(k); return o instanceof Number n ? n.doubleValue() : 0; }
        boolean b(String k) { return Boolean.TRUE.equals(cechy.get(k)); }
        String s(String k) { Object o = cechy.get(k); return o == null ? "" : o.toString(); }
    }

    final Plugin plugin;
    final Player p;
    final Location o;            // podstawa rosliny (gora gruntu) — NIGDY nie oddawac na zewnatrz bez clone() (MMOCore przesuwa lokacje przy hologramie XP)
    final Vector f, r;           // przod (od gracza do rosliny) i prawo gracza
    final float yaw;
    final List<Czesc> czesci = new ArrayList<>();
    private Czesc podswietlona;

    ZRoslina(Plugin plugin, Player p, Location o, Vector przod) {
        this.plugin = plugin;
        this.p = p;
        this.o = o.clone();
        this.f = new Vector(przod.getX(), 0, przod.getZ()).normalize();
        this.r = new Vector(-f.getZ(), 0, f.getX());
        this.yaw = (float) Math.toDegrees(Math.atan2(-f.getX(), f.getZ()));
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

    /** Klocek o srodku podstawy w (lx,ly,lz) wzgledem kotwicy czesci, rozmiar (sx,sy,sz), obrot wokol srodka podstawy.
     *  Uwaga: lokalne x klocka to -r (obrot encji o yaw), wiec x podajemy z minusem. */
    Klocek klocek(Czesc c, Material m, double lx, double ly, double lz, double sx, double sy, double sz, Quaternionf rot) {
        Quaternionf q = rot == null ? new Quaternionf() : new Quaternionf(rot);
        Vector3f s = new Vector3f((float) sx, (float) sy, (float) sz);
        Vector3f srodek = new Vector3f((float) sx / 2f, 0, (float) sz / 2f);
        Vector3f t = new Vector3f(srodek).rotate(q).negate().add((float) -lx, (float) ly, (float) lz);
        Location gdzie = swiat(c.kotwica.getX(), c.kotwica.getY(), c.kotwica.getZ());
        gdzie.setYaw(yaw);
        gdzie.setPitch(0);
        BlockDisplay d = o.getWorld().spawn(gdzie, BlockDisplay.class, e -> {
            e.setVisibleByDefault(false);
            e.setPersistent(false);
            e.setBlock(m.createBlockData());
            e.setTransformation(new Transformation(t, q, s, new Quaternionf()));
            e.setViewRange(0.5f);
            e.setShadowRadius(0);
        });
        p.showEntity(plugin, d);
        Klocek k = new Klocek(d, t, q, s, (float) ly, (float) (ly + sy));
        c.klocki.add(k);
        return k;
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

    /** Punkt trafienia (lokalnie) w czesc. */
    Vector trafienie(Czesc c) {
        Location oko = p.getEyeLocation();
        Vector start = lokalnie(oko.toVector());
        Vector kier = kierunekLokalny(oko.getDirection()).normalize();
        RayTraceResult hit = c.box.rayTrace(start, kier, 8);
        return hit == null ? null : hit.getHitPosition();
    }

    void podswietl(Czesc c, Color kolor) {
        if (c == podswietlona) return;
        if (podswietlona != null) for (Klocek k : podswietlona.klocki) k.d.setGlowing(false);
        podswietlona = c;
        if (c != null) for (Klocek k : c.klocki) { k.d.setGlowColorOverride(kolor); k.d.setGlowing(true); }
    }

    /** Obrot calej czesci wokol punktu (0, py, 0) jej kotwicy + przesuniecie; klocki ponizej `odY` zostaja. */
    void przechyl(Czesc c, Quaternionf q, Vector3f przesun, float odY, int ticki) {
        for (Klocek k : c.klocki) {
            if (k.y1 <= odY + 1e-3) continue;
            Vector3f piv = new Vector3f(0, odY, 0);
            Vector3f t = new Vector3f(k.t0).sub(piv).rotate(q).add(piv).add(przesun);
            Quaternionf l = new Quaternionf(q).mul(k.r0);
            animuj(k.d, new Transformation(t, l, k.s0, new Quaternionf()), ticki);
            if (DEBUG) plugin.getLogger().info("przechyl " + c.typ + c.nr + " y0=" + k.y0 + " t0=" + k.t0 + " -> t=" + t + " widziany=" + k.d.getTransformation().getTranslation());
        }
    }
    static boolean DEBUG = false;

    /** Przesuniecie calej czesci (np. wyciaganie korzenia, drzenie). */
    void przesun(Czesc c, Vector3f v, int ticki) {
        for (Klocek k : c.klocki) animuj(k.d, new Transformation(new Vector3f(k.t0).add(v), k.r0, k.s0, new Quaternionf()), ticki);
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

    void usun() {
        for (Czesc c : czesci) for (Klocek k : c.klocki) k.d.remove();
        czesci.clear();
        podswietlona = null;
    }

    static Quaternionf obrotZ(double stopnie) { return new Quaternionf().rotateZ((float) Math.toRadians(stopnie)); }
    static Quaternionf obrotX(double stopnie) { return new Quaternionf().rotateX((float) Math.toRadians(stopnie)); }
    static Quaternionf obrotY(double stopnie) { return new Quaternionf().rotateY((float) Math.toRadians(stopnie)); }
}
