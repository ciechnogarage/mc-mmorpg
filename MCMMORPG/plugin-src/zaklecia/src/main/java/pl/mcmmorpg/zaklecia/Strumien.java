package pl.mcmmorpg.zaklecia;

import io.lumine.mythic.bukkit.MythicBukkit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
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
  private record Wpis(ItemDisplay d, boolean prawa, String id, float[] dlugosc) {}

  private final Zaklecia plugin;
  private final Ksiega ksiega;
  private final Map<UUID, List<Wpis>> aktywne = new HashMap<>();

  Strumien(Zaklecia plugin, Ksiega ksiega) { this.plugin = plugin; this.ksiega = ksiega; }

  void start(Player p, Ksiega.Zaklecie prawa, Ksiega.Zaklecie lewa, double zasieg) {
    stop(p);
    List<Wpis> l = new ArrayList<>();
    if (prawa != null) l.add(zrob(p, prawa, true, zasieg));
    if (lewa != null) l.add(zrob(p, lewa, false, zasieg));
    aktywne.put(p.getUniqueId(), l);
  }

  private Wpis zrob(Player p, Ksiega.Zaklecie z, boolean prawa, double zasieg) {
    Location gdzie = dlon(p, prawa);
    ItemDisplay d = p.getWorld().spawn(gdzie, ItemDisplay.class, e -> {
      ItemStack i = new ItemStack(Material.PAPER);
      i.editMeta(m -> m.setItemModel(new NamespacedKey("zaklecia", "strumien_" + z.id())));
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
    return new Wpis(d, prawa, z.id(), new float[] {0.4f});
  }

  /** Punkt startu strumienia: przed dlonia (poza z rekami przed siebie), prawa/lewa strona. */
  static Location dlon(Player p, boolean prawa) {
    Location oko = p.getEyeLocation();
    double yaw = Math.toRadians(oko.getYaw());
    Vector wPrawo = new Vector(-Math.cos(yaw), 0, -Math.sin(yaw));
    Vector przod = oko.getDirection();
    return oko.clone().add(wPrawo.multiply(prawa ? 0.38 : -0.38)).add(przod.multiply(0.45)).add(0, -0.3, 0);
  }

  private static Transformation ksztalt(float dlugosc) {
    // model 1 blok dlugi w osi z, wysrodkowany; przesuniecie +dl/2 stawia jego poczatek w dloni
    return new Transformation(new Vector3f(0, 0, dlugosc / 2), new AxisAngle4f(), new Vector3f(1, 1, dlugosc), new AxisAngle4f());
  }

  /** Co tick: dlon, kierunek, dlugosc; co 4 ticki efekt uderzenia w punkcie trafienia (skill MM Zak_Uderzenie_<id>). */
  void tick(Player p, double zasieg, int n) {
    List<Wpis> l = aktywne.get(p.getUniqueId());
    if (l == null) return;
    for (Wpis w : l) {
      if (!w.d().isValid()) continue;
      Location start = dlon(p, w.prawa());
      Vector kier = p.getEyeLocation().getDirection();
      RayTraceResult r = p.getWorld().rayTrace(start, kier, zasieg, FluidCollisionMode.NEVER, true, 0.25,
          e -> e != p && e instanceof LivingEntity && !(e instanceof ArmorStand));
      float dl = (float) (r == null ? zasieg : r.getHitPosition().distance(start.toVector()));
      start.setYaw(p.getEyeLocation().getYaw());
      start.setPitch(p.getEyeLocation().getPitch());
      w.d().teleport(start);
      if (Math.abs(dl - w.dlugosc()[0]) > 0.15f) {       // metadane tylko przy zmianie dlugosci
        w.d().setInterpolationDelay(0);
        w.d().setTransformation(ksztalt(dl));
        w.dlugosc()[0] = dl;
      }
      if (r != null && n % 4 == 0) {                    // male uderzenie w miejscu trafienia
        Location hit = r.getHitPosition().toLocation(p.getWorld());
        try { MythicBukkit.inst().getAPIHelper().castSkill(p, "Zak_Uderzenie_" + w.id(), hit); } catch (Throwable ignored) {}
      }
    }
  }

  void stop(Player p) {
    List<Wpis> l = aktywne.remove(p.getUniqueId());
    if (l != null) for (Wpis w : l) w.d().remove();
  }

  void stopWszystkie() {
    for (List<Wpis> l : aktywne.values()) for (Wpis w : l) w.d().remove();
    aktywne.clear();
  }
}
