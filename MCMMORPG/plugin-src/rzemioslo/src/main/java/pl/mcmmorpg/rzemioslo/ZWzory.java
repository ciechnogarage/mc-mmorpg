package pl.mcmmorpg.rzemioslo;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Warianty wygladu dziurawca do wyboru (20.10): ten sam model w swiecie i z bliska, rozni sie tylko skala.
 *  A = karty krzyzowe jak krzewy vanilli, B = malowane warstwy zwrocone do patrzacego, C = bryly (lodygi, listki, kwiaty).
 *  Model opisany w skali zblizenia (1 = ok. 0,9 bloku wysokosci); w swiecie ok. 0,62. Widoczne dla wszystkich, nietrwale. */
final class ZWzory {
    private final Map<String, List<Display>> postawione = new HashMap<>();

    String postaw(String wariant, World w, double x, double y, double z, float k) {
        usun(wariant);
        Location o = new Location(w, x + 0.5, y, z + 0.5);
        Block b = o.getBlock();
        Display.Brightness swiatlo = new Display.Brightness(Math.min(15, b.getLightFromBlocks() + 2), b.getLightFromSky());
        List<Display> l = new ArrayList<>();
        // ziemia pod roslina: plat gleby z cieniem, lezy plasko (wspolny dla wariantow)
        l.add(karta(o, "zielarz_k_wzor_ziemia", new Vector3f(0, 0.012f, 0), 1.25f, new Quaternionf().rotateX((float) Math.toRadians(-90)), k, swiatlo, false));
        switch (wariant) {
            case "A" -> {
                float[][] kepy = {{0, 0, 0.95f, 0}, {0.2f, 0.15f, 0.75f, 25}, {-0.22f, 0.1f, 0.7f, 50}, {0.05f, -0.2f, 0.8f, 70}};
                for (float[] c : kepy)
                    l.add(karta(o, "zielarz_k_wzora_dziurawiec", new Vector3f(c[0], c[2] / 2, c[1]), c[2], new Quaternionf().rotateY((float) Math.toRadians(c[3])), k, swiatlo, false));
            }
            case "B" -> {
                String[] warstwy = {"tyl", "srodek", "przod"};
                for (int i = 0; i < 3; i++)   // warstwy zawsze twarza do patrzacego, rozsuniete w glab
                    l.add(karta(o, "zielarz_k_wzorb_" + warstwy[i], new Vector3f(0, 0.5f, -0.06f + 0.06f * i), 1.0f, new Quaternionf(), k, swiatlo, true));
            }
            case "C" -> bryly(o, l, k);
            default -> { return "wariant A, B albo C"; }
        }
        postawione.put(wariant, l);
        return "wzor " + wariant + " (" + l.size() + " encji, skala " + k + ")";
    }

    void usun(String wariant) {
        List<Display> l = postawione.remove(wariant);
        if (l != null) for (Display d : l) d.remove();
    }

    void usunWszystko() { for (String w : new ArrayList<>(postawione.keySet())) usun(w); }

    /** Karta Nexo (ItemDisplay): t = srodek karty wzgledem podstawy, s = bok. */
    private static ItemDisplay karta(Location o, String nexoId, Vector3f t, float s, Quaternionf q, float k, Display.Brightness swiatlo, boolean billboard) {
        var it = ZRoslina.przedmiot(nexoId);
        return o.getWorld().spawn(o.clone(), ItemDisplay.class, e -> {
            e.setPersistent(false);
            e.setItemStack(it);
            e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            e.setTransformation(new Transformation(new Vector3f(t).mul(k), q, new Vector3f(s * k), new Quaternionf()));
            e.setBrightness(swiatlo);
            if (billboard) e.setBillboard(Display.Billboard.VERTICAL);
        });
    }

    /** Prostopadloscian (BlockDisplay) obrocony wokol punktu "piv" (wzgledem rogu bryly), osadzony tak, by piv trafil w "gdzie". */
    private static BlockDisplay blok(Location o, Material m, Vector3f gdzie, Vector3f roz, Vector3f piv, Quaternionf q, float k) {
        Vector3f t = new Vector3f(piv).rotate(q).negate().add(gdzie);
        return o.getWorld().spawn(o.clone(), BlockDisplay.class, e -> {
            e.setPersistent(false);
            e.setBlock(m.createBlockData());
            e.setTransformation(new Transformation(t.mul(k), q, new Vector3f(roz).mul(k), new Quaternionf()));
        });
    }

    /** C: kepa z bryl — 5 lodyg spirala (wyzsze w srodku), listki naprzemianlegle pod katem, u gory 2-3 kwiaty. */
    private static void bryly(Location o, List<Display> l, float k) {
        Random r = new Random(11);
        for (int i = 0; i < 7; i++) {
            double kat = Math.toRadians(i * 137.5), rr = 0.04 + 0.26 * Math.sqrt((i + 0.5) / 7);
            float x = (float) (Math.cos(kat) * rr), z = (float) (Math.sin(kat) * rr * 0.9);
            float h = (float) (0.62 + 0.25 * (1 - rr / 0.29) + r.nextDouble() * 0.05);
            float lean = (float) Math.toRadians(r.nextDouble() * 10 - 5);
            Quaternionf ql = new Quaternionf().rotateZ(lean);
            l.add(blok(o, Material.GREEN_TERRACOTTA, new Vector3f(x, 0, z), new Vector3f(0.036f, h, 0.036f), new Vector3f(0.018f, 0, 0.018f), ql, k));
            double faza = r.nextDouble() * 180;
            int n = 0;
            for (float y = 0.08f; y < h - 0.12f; y += 0.085f + (float) r.nextDouble() * 0.03f, n++) {
                float dl = (float) (0.17 - 0.07 * y / h);   // lisc dluzszy nisko, krotszy u gory
                Material m = y < h * 0.35 ? Material.GREEN_CONCRETE : y < h * 0.7 ? Material.LIME_TERRACOTTA : Material.LIME_CONCRETE;
                Quaternionf q = new Quaternionf().rotateY((float) Math.toRadians(faza + n * 180 + r.nextDouble() * 30)).rotateZ((float) Math.toRadians(28));
                l.add(blok(o, m, new Vector3f(x - (float) Math.sin(lean) * y, y, z), new Vector3f(dl, 0.014f, 0.058f), new Vector3f(0, 0.007f, 0.029f), q, k));
            }
            int kw = 2 + r.nextInt(2);
            for (int j = 0; j < kw; j++) {   // kwiat: zolty kwadrat obrocony o 45 st. (5 platkow w skrocie) + pomaranczowy srodek
                double a = Math.toRadians(j * 120 + r.nextDouble() * 40);
                float fx = x - (float) Math.sin(lean) * h + (float) Math.cos(a) * 0.05f, fz = z + (float) Math.sin(a) * 0.05f, fy = h - 0.02f + (float) r.nextDouble() * 0.04f;
                float s = 0.07f + (float) r.nextDouble() * 0.025f;
                Quaternionf qk = new Quaternionf().rotateY((float) Math.toRadians(45 + r.nextDouble() * 20)).rotateX((float) Math.toRadians(-12));
                l.add(blok(o, Material.YELLOW_CONCRETE, new Vector3f(fx, fy, fz), new Vector3f(s, 0.012f, s), new Vector3f(s / 2, 0, s / 2), qk, k));
                Quaternionf qk2 = new Quaternionf(qk).rotateY((float) Math.toRadians(45));   // drugi kwadrat pod katem = gwiazdka platkow
                l.add(blok(o, Material.YELLOW_WOOL, new Vector3f(fx, fy + 0.004f, fz), new Vector3f(s * 0.8f, 0.012f, s * 0.8f), new Vector3f(s * 0.4f, 0, s * 0.4f), qk2, k));
                l.add(blok(o, Material.ORANGE_CONCRETE, new Vector3f(fx, fy + 0.008f, fz), new Vector3f(0.022f, 0.016f, 0.022f), new Vector3f(0.011f, 0, 0.011f), qk, k));
            }
        }
    }
}
