package pl.mcmmorpg.wyscigi;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Wszystko, co leży na torze poza fizyką: strzałki pól przyspieszenia, skrzynki z bonusami (tylko wyścig z innymi),
 * pociski, błoto, cios LPM. Bonusy są takie same dla każdej klasy i każdego wierzchowca — równe szanse.
 */
final class Bonusy {
  static final Map<String, String> NAZWY = Map.of("iskra", "Iskra", "kula", "Kula ognia", "bloto", "Błoto", "tarcza", "Skorupa", "ryk", "Ryk");

  private record Skrzynka(Location miejsce, ItemDisplay[] encja, int[] wrocDo) {}
  private static final class Pocisk { ItemDisplay e; Kierowca wlasciciel, cel; double x, y, z, kx, kz; int zycie; }
  private static final class Bloto { BlockDisplay e; Kierowca wlasciciel; Location l; int od; }

  private final WyscigiPlugin plugin;
  private final NamespacedKey klucz;
  private final Random los = new Random();
  private final List<Entity> strzalki = new ArrayList<>();
  private final List<Skrzynka> skrzynki = new ArrayList<>();
  private final List<Pocisk> pociski = new ArrayList<>();
  private final List<Bloto> bloto = new ArrayList<>();

  Bonusy(WyscigiPlugin plugin, NamespacedKey klucz) {
    this.plugin = plugin;
    this.klucz = klucz;
  }

  private <T extends Entity> T oznacz(T e) {
    e.getPersistentDataContainer().set(klucz, PersistentDataType.STRING, "dekoracja");
    e.setPersistent(false);
    return e;
  }

  // ------------------------------------------------------------------ strzałki pól (zawsze, gdy tor załadowany)
  void postawStrzalki(World w, Tor tor) {
    if (!strzalki.isEmpty()) return;
    for (Tor.Pole pole : tor.pola) for (int k = -1; k <= 1; k++) for (int b = -1; b <= 1; b++) {
      Location l = tor.punkt(w, pole.s() + k * 1.3, pole.bok() + b * 1.4);
      l.setY(tor.y + 0.03);
      float yaw = l.getYaw();
      TextDisplay t = oznacz(w.spawn(l, TextDisplay.class, x -> {
        x.text(net.kyori.adventure.text.Component.text("»", net.kyori.adventure.text.format.TextColor.color(0xF4C657)));
        x.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        x.setShadowed(true);
        x.setBillboard(Display.Billboard.FIXED);
        x.setBrightness(new Display.Brightness(15, 15));
        // Leży płasko na torze, grot w kierunku jazdy.
        Quaternionf q = new Quaternionf().rotateY((float) Math.toRadians(-yaw)).rotateX((float) Math.toRadians(-90)).rotateZ((float) Math.toRadians(-90));
        x.setTransformation(new Transformation(new Vector3f(0, 0, 0), q, new Vector3f(6f, 6f, 6f), new Quaternionf()));
        x.setRotation(0, 0);
      }));
      strzalki.add(t);
    }
  }

  void zdejmijStrzalki() { strzalki.forEach(Entity::remove); strzalki.clear(); }

  // ------------------------------------------------------------------ skrzynki (wyścig z innymi)
  void postawSkrzynki(World w, Tor tor) {
    if (!skrzynki.isEmpty()) return;
    for (double s : tor.skrzynki) {
      double[] boki = {-3, 0, 3};
      ItemDisplay[] e = new ItemDisplay[boki.length];
      int[] wroc = new int[boki.length];
      Location srodek = tor.punkt(w, s, 0);
      for (int i = 0; i < boki.length; i++) e[i] = skrzynka(tor.punkt(w, s, boki[i]).add(0, 0.9, 0));
      skrzynki.add(new Skrzynka(srodek, e, wroc));
    }
  }

  private ItemDisplay skrzynka(Location l) {
    return oznacz(l.getWorld().spawn(l, ItemDisplay.class, x -> {
      x.setItemStack(new ItemStack(Material.BARREL));
      x.setGlowing(true);
      x.setGlowColorOverride(Color.fromRGB(0xF4C657));
      x.setBrightness(new Display.Brightness(15, 15));
      x.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0.8f, 0.8f, 0.8f), new Quaternionf()));
      x.setInterpolationDuration(10);
    }));
  }

  void zdejmijSkrzynki() {
    for (Skrzynka s : skrzynki) for (ItemDisplay e : s.encja()) if (e != null) e.remove();
    skrzynki.clear();
    pociski.forEach(p -> p.e.remove()); pociski.clear();
    bloto.forEach(b -> b.e.remove()); bloto.clear();
  }

  boolean saSkrzynki() { return !skrzynki.isEmpty(); }

  /** Co tick: obrót skrzynek, odrodzenie, zbieranie, pociski, błoto. */
  void tick(List<Wyscig> wyscigi) {
    int teraz = Bukkit.getCurrentTick();
    for (Skrzynka s : skrzynki) for (int i = 0; i < s.encja().length; i++) {
      ItemDisplay e = s.encja()[i];
      if (e == null && teraz >= s.wrocDo()[i]) {
        Tor tor = plugin.u.tor;
        double[] boki = {-3, 0, 3};
        Tor.Rzut r = tor.rzut(s.miejsce().getX(), s.miejsce().getZ(), -1, 0);
        s.encja()[i] = skrzynka(tor.punkt(s.miejsce().getWorld(), r.s(), boki[i]).add(0, 0.9, 0));
      } else if (e != null && teraz % 10 == 0) {
        Transformation t = e.getTransformation();
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(t.getTranslation(), new Quaternionf(t.getLeftRotation()).rotateY((float) Math.toRadians(90)), t.getScale(), t.getRightRotation()));
      }
    }
    for (Wyscig w : wyscigi) {
      if (w.tryb != Wyscig.Tryb.WYSCIG || w.faza != Wyscig.Faza.JAZDA) continue;
      for (Kierowca k : List.copyOf(w.kierowcy)) {
        if (k.mob == null || k.naMecie()) continue;
        Location l = k.mob.getLocation();
        for (Skrzynka s : skrzynki) for (int i = 0; i < s.encja().length; i++) {
          ItemDisplay e = s.encja()[i];
          if (e == null || e.getLocation().distanceSquared(l.clone().add(0, 0.9, 0)) > 2.4) continue;
          e.getWorld().spawnParticle(Particle.WAX_OFF, e.getLocation(), 14, 0.4, 0.4, 0.4, 0.5);
          e.getWorld().playSound(e.getLocation(), Sound.BLOCK_BARREL_OPEN, 0.8f, 1.4f);
          e.remove();
          s.encja()[i] = null;
          s.wrocDo()[i] = teraz + 60;
          if (k.bonus == null) wylosuj(k, w);
        }
      }
    }
    pociskiTick(teraz);
    blotoTick(teraz, wyscigi);
  }

  private void wylosuj(Kierowca k, Wyscig w) {
    int n = Math.max(1, w.kierowcy.size());
    double tyl = n <= 1 ? 0.5 : (k.pozycja - 1) / (double) (n - 1);   // 0 = prowadzi, 1 = ostatni
    String[] rodzaje = {"iskra", "kula", "bloto", "tarcza", "ryk"};
    double[] wagi = {15 + 35 * tyl, 10 + 35 * tyl, 35 - 25 * tyl, 30 - 20 * tyl, 10 + 5 * tyl};
    double suma = 0; for (double x : wagi) suma += x;
    double r = los.nextDouble() * suma;
    String wybrany = rodzaje[0];
    for (int i = 0; i < rodzaje.length; i++) { r -= wagi[i]; if (r <= 0) { wybrany = rodzaje[i]; break; } }
    k.bonus = wybrany;
    Player p = k.gracz();
    if (p != null) {
      Wyscig.tytul(p, "", "<gold>" + NAZWY.get(wybrany) + "</gold> <gray>— wciśnij</gray> <white>F</white>", Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.3f);
    }
    plugin.debug(k.nick + " bonus-zebrany " + wybrany);
  }

  // ------------------------------------------------------------------ użycie (F)
  void uzyj(Kierowca k) {
    if (k.bonus == null || k.mob == null) return;
    String b = k.bonus;
    k.bonus = null;
    Location l = k.mob.getLocation();
    World w = l.getWorld();
    plugin.debug(k.nick + " bonus-uzyty " + b);
    switch (b) {
      case "iskra" -> k.daj(34, 1.38, "iskra");
      case "tarcza" -> {
        k.tarczaDo = Bukkit.getCurrentTick() + 200;
        w.playSound(l, Sound.ITEM_ARMOR_EQUIP_TURTLE, 1f, 1f);
      }
      case "ryk" -> {
        w.playSound(l, Sound.ENTITY_RAVAGER_ROAR, 1f, 1.3f);
        w.spawnParticle(Particle.SWEEP_ATTACK, l.clone().add(0, 0.8, 0), 16, 2.5, 0.3, 2.5, 0);
        for (Kierowca inny : List.copyOf(k.wyscig.kierowcy)) {
          if (inny == k || inny.mob == null || inny.naMecie()) continue;
          Location il = inny.mob.getLocation();
          if (il.distanceSquared(l) > 36) continue;
          inny.zachwiej(14, il.getX() - l.getX(), il.getZ() - l.getZ(), 0.8, "ryk");
        }
      }
      case "bloto" -> {
        double kat = Math.toRadians(k.kierunek);
        Location gdzie = l.clone().add(Math.sin(kat) * 2.6, 0, -Math.cos(kat) * 2.6);
        gdzie.setY(plugin.u.tor.y + 0.01);
        BlockDisplay e = oznacz(w.spawn(gdzie.clone().add(-1.1, 0, -1.1), BlockDisplay.class, x -> {
          x.setBlock(Material.MUD.createBlockData());
          x.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(2.2f, 0.07f, 2.2f), new Quaternionf()));
        }));
        Bloto bl = new Bloto(); bl.e = e; bl.wlasciciel = k; bl.l = gdzie; bl.od = Bukkit.getCurrentTick();
        bloto.add(bl);
        w.playSound(gdzie, Sound.BLOCK_MUD_PLACE, 1f, 0.8f);
      }
      case "kula" -> {
        Pocisk p = new Pocisk();
        p.wlasciciel = k;
        p.cel = celPrzed(k);
        p.x = l.getX(); p.y = l.getY() + 0.9; p.z = l.getZ();
        double kat = Math.toRadians(k.kierunek);
        p.kx = -Math.sin(kat); p.kz = Math.cos(kat);
        p.zycie = 110;
        p.e = oznacz(w.spawn(new Location(w, p.x, p.y, p.z), ItemDisplay.class, x -> {
          x.setItemStack(new ItemStack(Material.FIRE_CHARGE));
          x.setBrightness(new Display.Brightness(15, 15));
          x.setTeleportDuration(1);
          x.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0.9f, 0.9f, 0.9f), new Quaternionf()));
        }));
        pociski.add(p);
        w.playSound(l, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.9f);
      }
      default -> {}
    }
  }

  private Kierowca celPrzed(Kierowca k) {
    Kierowca najlepszy = null;
    for (Kierowca inny : k.wyscig.kierowcy) {
      if (inny == k || inny.naMecie() || inny.mob == null || inny.postep <= k.postep) continue;
      if (najlepszy == null || inny.postep < najlepszy.postep) najlepszy = inny;
    }
    return najlepszy;
  }

  private void pociskiTick(int teraz) {
    for (Iterator<Pocisk> it = pociski.iterator(); it.hasNext(); ) {
      Pocisk p = it.next();
      if (--p.zycie <= 0 || !p.e.isValid()) { p.e.remove(); it.remove(); continue; }
      if (p.cel != null && p.cel.mob != null && p.cel.mob.isValid() && !p.cel.naMecie()) {
        Location c = p.cel.mob.getLocation();
        double dx = c.getX() - p.x, dz = c.getZ() - p.z, d = Math.hypot(dx, dz);
        if (d > 1e-3) { p.kx = p.kx * 0.75 + dx / d * 0.25; p.kz = p.kz * 0.75 + dz / d * 0.25; double n = Math.hypot(p.kx, p.kz); p.kx /= n; p.kz /= n; }
      }
      p.x += p.kx * 1.05; p.z += p.kz * 1.05;
      p.e.teleport(new Location(p.e.getWorld(), p.x, p.y, p.z));
      p.e.getWorld().spawnParticle(Particle.FLAME, p.x, p.y, p.z, 3, 0.1, 0.1, 0.1, 0.01);
      for (Kierowca k : p.wlasciciel.wyscig.kierowcy) {
        if (k.mob == null || k.naMecie() || (k == p.wlasciciel && p.zycie > 95)) continue;
        Location kl = k.mob.getLocation();
        if (Math.hypot(kl.getX() - p.x, kl.getZ() - p.z) < 1.4) {
          p.e.getWorld().spawnParticle(Particle.EXPLOSION, p.x, p.y, p.z, 1);
          p.e.getWorld().playSound(kl, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.4f);
          k.zachwiej(30, p.kx, p.kz, 0.4, "kula");
          p.e.remove(); it.remove();
          break;
        }
      }
    }
  }

  private void blotoTick(int teraz, List<Wyscig> wyscigi) {
    for (Iterator<Bloto> it = bloto.iterator(); it.hasNext(); ) {
      Bloto b = it.next();
      if (teraz - b.od > 600 || !b.e.isValid()) { b.e.remove(); it.remove(); continue; }
      for (Kierowca k : b.wlasciciel.wyscig.kierowcy) {
        if (k.mob == null || k.naMecie() || (k == b.wlasciciel && teraz - b.od < 30)) continue;
        Location kl = k.mob.getLocation();
        if (Math.hypot(kl.getX() - b.l.getX(), kl.getZ() - b.l.getZ()) < 1.5) {
          kl.getWorld().playSound(kl, Sound.BLOCK_MUD_STEP, 1f, 0.6f);
          kl.getWorld().spawnParticle(Particle.BLOCK, kl, 20, 0.5, 0.2, 0.5, Material.MUD.createBlockData());
          k.zachwiej(18, 0, 0, 0, "bloto");
          b.e.remove(); it.remove();
          break;
        }
      }
    }
  }

  // ------------------------------------------------------------------ cios (LPM)
  void cios(Kierowca k) {
    int teraz = Bukkit.getCurrentTick();
    if (k.mob == null || teraz - k.ciosOd < 40 || k.wyscig.faza != Wyscig.Faza.JAZDA) return;
    k.ciosOd = teraz;
    Location l = k.mob.getLocation();
    l.getWorld().playSound(l, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.1f);
    Kierowca cel = null; double najblizej = 3.4 * 3.4;
    for (Kierowca inny : k.wyscig.kierowcy) {
      if (inny == k || inny.mob == null || inny.naMecie()) continue;
      double d = inny.mob.getLocation().distanceSquared(l);
      if (d < najblizej) { najblizej = d; cel = inny; }
    }
    if (cel == null) return;
    Location c = cel.mob.getLocation();
    l.getWorld().spawnParticle(Particle.SWEEP_ATTACK, c.clone().add(0, 1, 0), 1);
    l.getWorld().playSound(c, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 1f);
    cel.zachwiej(10, c.getX() - l.getX(), c.getZ() - l.getZ(), 0.6, "cios:" + k.nick);
  }

  /** Iskry tarczy wokół wierzchowca (wołane z HUD co kilka ticków). */
  void efektTarczy(Kierowca k) {
    if (k.mob == null || Bukkit.getCurrentTick() >= k.tarczaDo) return;
    Location l = k.mob.getLocation().add(0, 0.9, 0);
    for (int i = 0; i < 8; i++) {
      double a = i * Math.PI / 4 + Bukkit.getCurrentTick() * 0.2;
      l.getWorld().spawnParticle(Particle.DUST, l.getX() + Math.cos(a) * 1.3, l.getY(), l.getZ() + Math.sin(a) * 1.3, 1, 0, 0, 0, 0,
          new Particle.DustOptions(Color.fromRGB(110, 200, 220), 1.0f));
    }
  }
}
