package pl.mcmmorpg.zaklecia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Strumien zaklecia jako JEDEN obiekt wyswietlany na dlon (item_display z animowanym modelem
 * {@code zaklecia:strumien_<id>}), zamiast chmury czasteczek z pociskow MM (setki pakietow/s na gracza, zaslanialy
 * ekran). Co tick: teleport do dloni z obrotem gracza (klient interpoluje, teleport_duration 2) i dlugosc z promienia
 * do pierwszej przeszkody/istoty. Model: os +Z = kierunek strumienia, dlugosc modelu 1 blok (0..16 w z).
 */
final class Strumien {
  private record Wpis(ItemDisplay d, ItemDisplay wlasny, boolean prawa, Ksiega.Zaklecie zaklecie, float[] dlugosc) {}

  private final Zaklecia plugin;
  private final Skille skille;
  private final Zaslanianie zaslanianie;
  private final Map<UUID, List<Wpis>> aktywne = new HashMap<>();

  Strumien(Zaklecia plugin, Skille skille, Zaslanianie zaslanianie) {
    this.plugin = plugin; this.skille = skille; this.zaslanianie = zaslanianie;
  }

  /** Wlacza/wylacza strumien jednej dloni (z == null = zgas). Wolane przy kazdej zmianie aktywnych dloni. */
  void ustaw(Player p, boolean prawa, Ksiega.Zaklecie z, double zasieg) {
    List<Wpis> l = aktywne.computeIfAbsent(p.getUniqueId(), k -> new ArrayList<>());
    for (var it = l.iterator(); it.hasNext(); ) {
      Wpis w = it.next();
      if (w.prawa() != prawa) continue;
      if (z != null && w.zaklecie().id().equals(z.id()) && w.d().isValid()) return;   // juz jest ten sam
      w.d().remove();
      if (w.wlasny() != null) w.wlasny().remove();
      it.remove();
    }
    if (z != null) l.add(zrob(p, z, prawa, zasieg));
  }

  private Wpis zrob(Player p, Ksiega.Zaklecie z, boolean prawa, double zasieg) {
    Location gdzie = dlon(p, prawa);
    ItemDisplay d = p.getWorld().spawn(gdzie, ItemDisplay.class, e -> {
      ItemStack i = new ItemStack(Material.PAPER);
      i.editMeta(m -> m.setItemModel(new NamespacedKey("zaklecia", z.modelStrumienia())));
      e.setItemStack(i);
      e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
      e.setBillboard(Display.Billboard.FIXED);
      e.setBrightness(new Display.Brightness(15, 15));
      e.setTeleportDuration(2);
      e.setInterpolationDuration(2);
      e.setViewRange(0.6f);            // ~40 blokow przy domyslnym mnozniku
      e.setPersistent(false);          // restart/crash nie zostawi strumieni w swiecie
      e.setTransformation(ksztalt(0.4f));
    });
    ItemDisplay wlasny = null;
    if (plugin.getConfig().getBoolean("strumien.waski-dla-rzucajacego", true)) {
      p.hideEntity(plugin, d);
      wlasny = p.getWorld().spawn(dlonWlasna(p, prawa), ItemDisplay.class, e -> {
        ItemStack i = new ItemStack(Material.PAPER);
        i.editMeta(m -> m.setItemModel(new NamespacedKey("zaklecia", z.modelStrumienia())));
        e.setItemStack(i);
        e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
        e.setBillboard(Display.Billboard.FIXED);
        e.setBrightness(new Display.Brightness(15, 15));
        e.setTeleportDuration(2);
        e.setInterpolationDuration(2);
        e.setViewRange(0.6f);
        e.setPersistent(false);
        e.setVisibleByDefault(false);
        e.setTransformation(ksztaltWlasny(0.4f));
      });
      p.showEntity(plugin, wlasny);
    }
    return new Wpis(d, wlasny, prawa, z, new float[] {0.4f});
  }

  /** Punkt startu strumienia: przed dlonia (poza z rekami przed siebie), prawa/lewa strona. */
  static Location dlon(Player p, boolean prawa) {
    Location oko = p.getEyeLocation();
    double yaw = Math.toRadians(oko.getYaw());
    Vector wPrawo = new Vector(-Math.cos(yaw), 0, -Math.sin(yaw));
    Vector przod = oko.getDirection();
    return oko.clone().add(wPrawo.multiply(prawa ? 0.38 : -0.38)).add(przod.multiply(0.45)).add(0, -0.3, 0);
  }

  private static Location dlonWlasna(Player p, boolean prawa) {
    return dlon(p, prawa).add(p.getEyeLocation().getDirection().multiply(0.6));
  }

  private static Transformation ksztalt(float dlugosc) {
    // model 1 blok dlugi w osi z, wysrodkowany; przesuniecie +dl/2 stawia jego poczatek w dloni
    return new Transformation(new Vector3f(0, 0, dlugosc / 2), new AxisAngle4f(), new Vector3f(1, 1, dlugosc), new AxisAngle4f());
  }

  private static Transformation ksztaltWlasny(float dlugosc) {
    float krotki = Math.min(dlugosc, 0.9f);
    return new Transformation(new Vector3f(0, 0, krotki / 2), new AxisAngle4f(),
        new Vector3f(0.3f, 0.3f, krotki), new AxisAngle4f());
  }

  /** Co tick: dlon, kierunek, dlugosc; co 4 ticki skill uderzenia danej dloni w punkcie trafienia. */
  void tick(Player p, double zasieg, int n) {
    List<Wpis> l = aktywne.get(p.getUniqueId());
    if (l == null) return;
    for (Wpis w : l) {
      if (!w.d().isValid()) continue;
      Location oko = p.getEyeLocation();
      Location start = dlon(p, w.prawa());
      Vector kier = oko.getDirection();
      RayTraceResult r = zaslanianie.wlaczone()
          ? p.getWorld().rayTrace(oko, kier, zasieg, FluidCollisionMode.NEVER, true, 0.15,
              e -> zaslanianie.istota(p, e))
          : p.getWorld().rayTraceBlocks(oko, kier, zasieg, FluidCollisionMode.NEVER, true);
      float dl = (float) (r == null ? zasieg : r.getHitPosition().distance(oko.toVector()));
      start.setYaw(p.getEyeLocation().getYaw());
      start.setPitch(p.getEyeLocation().getPitch());
      w.d().teleport(start);
      if (w.wlasny() != null) {
        Location wlasnyStart = dlonWlasna(p, w.prawa());
        wlasnyStart.setYaw(start.getYaw());
        wlasnyStart.setPitch(start.getPitch());
        w.wlasny().teleport(wlasnyStart);
      }
      if (Math.abs(dl - w.dlugosc()[0]) > 0.15f) {       // metadane tylko przy zmianie dlugosci
        w.d().setInterpolationDelay(0);
        w.d().setTransformation(ksztalt(dl));
        if (w.wlasny() != null) {
          w.wlasny().setInterpolationDelay(0);
          w.wlasny().setTransformation(ksztaltWlasny(dl));
        }
        w.dlugosc()[0] = dl;
      }
      if (r != null && n % 4 == 0) {                    // male uderzenie w miejscu trafienia
        Location hit = r.getHitPosition().toLocation(p.getWorld());
        skille.punkt(p, w.zaklecie().skille().reka(w.zaklecie().skille().uderzenie(), w.prawa()), hit);
      }
    }
  }

  void stop(Player p) {
    List<Wpis> l = aktywne.remove(p.getUniqueId());
    if (l != null) for (Wpis w : l) {
      w.d().remove();
      if (w.wlasny() != null) w.wlasny().remove();
    }
  }

  void stopWszystkie() {
    for (List<Wpis> l : aktywne.values()) for (Wpis w : l) {
      w.d().remove();
      if (w.wlasny() != null) w.wlasny().remove();
    }
    aktywne.clear();
  }
}
