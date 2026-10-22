package pl.mcmmorpg.trafienia;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Odcięta część: kopie wyświetlanych kości (ten sam przedmiot ME i ta sama macierz co w chwili cięcia),
 * wszystkie zakotwiczone w jednym punkcie i animowane wspólną fizyką: rzut, obrót, odbicie, leżenie, zanik.
 */
final class Odpad extends BukkitRunnable {
    record Kawalek(ItemStack item, Matrix4f macierz) {}

    private final List<ItemDisplay> displaye = new ArrayList<>();
    private final List<Matrix4f> bazowe = new ArrayList<>();
    private final Vector3f srodekLokalny;
    private final World swiat;
    private final Location kotwica;
    private final Vector predkosc;
    private final Vector3f osObrotu;
    private final Quaternionf obrot = new Quaternionf();
    private final BlockData krew;
    private final int zycie;
    private float predkoscKatowa;
    private boolean naZiemi;
    private int tick, lezy;

    /** kotwica = miejsce bazy modelu (tam ME stawia swoje displaye), srodek = środek części w układzie kotwicy. */
    Odpad(Location kotwica, List<Kawalek> kawalki, Vector3f srodek, Vector kierunek, BlockData krew, int zycie) {
        this(kotwica, kawalki, srodek, kierunek, krew, zycie, false);
    }

    /** lezy = szczątki po śmierci: od razu leżą tam, gdzie skończyła animacja (bez rzutu). */
    Odpad(Location kotwica, List<Kawalek> kawalki, Vector3f srodek, Vector kierunek, BlockData krew, int zycie, boolean lezy) {
        this.swiat = kotwica.getWorld();
        this.kotwica = kotwica.clone();
        this.kotwica.setYaw(0); this.kotwica.setPitch(0);
        this.srodekLokalny = srodek;
        this.krew = krew;
        this.zycie = zycie;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Vector poziom = kierunek.clone().setY(0);
        if (poziom.lengthSquared() < 1e-6) poziom = new Vector(r.nextDouble(-1, 1), 0, r.nextDouble(-1, 1));
        this.predkosc = poziom.normalize().multiply(.16 + r.nextDouble(.08)).setY(.32 + r.nextDouble(.1));
        this.osObrotu = new Vector3f((float) -poziom.getZ(), (float) r.nextDouble(-.3, .3), (float) poziom.getX()).normalize();
        this.predkoscKatowa = (float) (.22 + r.nextDouble(.12));
        if (lezy) { naZiemi = true; predkosc.zero(); predkoscKatowa = 0; }
        for (Kawalek k : kawalki) {
            ItemDisplay d = swiat.spawn(this.kotwica, ItemDisplay.class, e -> {
                e.setItemStack(k.item());
                e.setTransformationMatrix(k.macierz());
                e.setPersistent(false);
                e.setInterpolationDuration(1);
                e.setTeleportDuration(1);
            });
            displaye.add(d);
            bazowe.add(new Matrix4f(k.macierz()));
        }
    }

    @Override public void run() {
        tick++;
        if (displaye.isEmpty() || displaye.get(0).isDead()) { cancel(); return; }
        if (!naZiemi) {
            predkosc.setY(predkosc.getY() - .045).multiply(.98);
            Location nast = punkt().add(predkosc);
            if (nast.getBlock().isPassable()) {
                kotwica.add(predkosc);
            } else {
                // odbicie: słabe, z tarciem; za drugim dotknięciem leży
                if (Math.abs(predkosc.getY()) > .12) {
                    predkosc.setY(-predkosc.getY() * .3); predkosc.setX(predkosc.getX() * .5); predkosc.setZ(predkosc.getZ() * .5);
                    predkoscKatowa *= .5f;
                    if (krew != null) swiat.spawnParticle(Particle.BLOCK, punkt(), 8, .15, .05, .15, krew);
                } else {
                    naZiemi = true;
                }
            }
            obrot.premul(new Quaternionf().fromAxisAngleRad(osObrotu, predkoscKatowa));
        } else {
            lezy++;
        }
        float skala = 1;
        if (lezy > zycie - 15) skala = Math.max(0, (zycie - lezy) / 15f);
        for (int i = 0; i < displaye.size(); i++) {
            ItemDisplay d = displaye.get(i);
            Matrix4f m = new Matrix4f()
                    .translate(srodekLokalny)
                    .rotate(obrot)
                    .scale(skala)
                    .translate(-srodekLokalny.x, -srodekLokalny.y, -srodekLokalny.z)
                    .mul(bazowe.get(i));
            d.setInterpolationDelay(0);
            d.setTransformationMatrix(m);
            if (!naZiemi) d.teleport(kotwica);
        }
        if (lezy >= zycie || tick > zycie + 200) usun();
    }

    private Location punkt() {
        return kotwica.clone().add(srodekLokalny.x, srodekLokalny.y - .1, srodekLokalny.z);
    }

    void usun() {
        for (ItemDisplay d : displaye) d.remove();
        displaye.clear();
        if (!isCancelled()) cancel();
    }
}
