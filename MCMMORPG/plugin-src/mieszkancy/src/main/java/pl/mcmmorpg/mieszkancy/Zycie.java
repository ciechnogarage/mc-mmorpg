package pl.mcmmorpg.mieszkancy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Życie miasta: napisy zamiast tabliczek, cząsteczki i dźwięki (kominy, kuźnie, Nexus, świetliki nocą, dzwon),
 * przechodnie chodzący po trasach, zwierzęta, towary na straganach. Każda pętla liczy się tylko, gdy w Stolicy jest
 * gracz ({@link Mieszkancy#aktywne}), a efekt — gdy gracz jest w jego zasięgu.
 */
public final class Zycie implements Listener {
  private final Mieszkancy m;

  record Napis(Location l, String tekst, float skala, float zasieg, boolean tlo) {}
  record Efekt(String id, Location l, Particle p, Object dane, int co, int ilosc, double[] rozrzut, double predkosc,
      String dzwiek, float glosnosc, float ton, int dzwiekCo, Pora pora, double zasieg) {}
  enum Pora { ZAWSZE, NOC, DZIEN }
  record Towar(Location l, ItemStack it, float skala, float pochylenie, boolean blok) {}
  record Zwierze(Location l, EntityType typ, String wariant, boolean siedzi, double promien) {}
  record Przechodzien(String id, String skorka, Map<EquipmentSlot, ItemStack> stroj, List<double[]> trasa, double predkosc,
      int postoj, List<String> kwestie, String imie, boolean wPetli) {}

  static final class ZywyP {
    final Przechodzien d;
    Mannequin e;
    int cel = 1, kierunek = 1, czeka;
    double x, y, z;
    ZywyP(Przechodzien d) { this.d = d; }
  }

  private final List<Napis> napisy = new ArrayList<>();
  private final List<Efekt> efekty = new ArrayList<>();
  private final List<Towar> towary = new ArrayList<>();
  private final List<Zwierze> zwierzeta = new ArrayList<>();
  private final List<ZywyP> przechodnie = new ArrayList<>();
  private final List<int[]> tabliczki = new ArrayList<>();
  private final List<Entity> stale = new ArrayList<>();
  private final List<Entity> zywe = new ArrayList<>();
  private final java.util.Set<Long> postawione = new java.util.HashSet<>();
  private final Map<Entity, Location> domy = new java.util.HashMap<>();
  private final Map<java.util.UUID, Integer> zaczepkiP = new java.util.HashMap<>();
  private int usunieteTabliczki;

  public Zycie(Mieszkancy m) { this.m = m; }

  public String opis() {
    return napisy.size() + " napisów, " + efekty.size() + " efektów, " + przechodnie.size() + " przechodniów ("
        + przechodnie.stream().filter(p -> p.e != null && p.e.isValid()).count() + " idzie), " + zwierzeta.size()
        + " zwierząt, " + towary.size() + " towarów, tabliczek zdjętych: " + usunieteTabliczki;
  }

  void wczytaj(ConfigurationSection c) {
    napisy.clear(); efekty.clear(); towary.clear(); zwierzeta.clear(); przechodnie.clear(); tabliczki.clear();
    for (Map<?, ?> x : c.getMapList("napisy")) {
      ConfigurationSection s = sek(x);
      Location l = m.miejsce(s.getList("miejsce"));
      if (l == null) continue;
      napisy.add(new Napis(l, s.getString("tekst", ""), (float) s.getDouble("skala", 1.0), (float) s.getDouble("zasieg", 0.5), s.getBoolean("tlo", true)));
    }
    int i = 0;
    for (Map<?, ?> x : c.getMapList("efekty")) {
      ConfigurationSection s = sek(x);
      String id = s.getString("id", "efekt" + i++);
      Location l = m.miejsce(s.getList("miejsce"));
      if (l == null) continue;
      Particle p = m.czastka(s.getString("czastka"), "efekty." + id);
      List<Double> r = s.getDoubleList("rozrzut");
      Pora pora;
      try { pora = Pora.valueOf(s.getString("pora", "ZAWSZE").toUpperCase()); } catch (IllegalArgumentException e) { pora = Pora.ZAWSZE; }
      efekty.add(new Efekt(id, l, p, m.daneCzastki(p, s), Math.max(1, s.getInt("co-tickow", 10)), s.getInt("ilosc", 1),
          new double[] {r.size() > 0 ? r.get(0) : 0.1, r.size() > 1 ? r.get(1) : 0.1, r.size() > 2 ? r.get(2) : 0.1},
          s.getDouble("predkosc", 0.01), m.dzwiek(s.getString("dzwiek"), "efekty." + id), (float) s.getDouble("glosnosc", 0.5),
          (float) s.getDouble("ton", 1.0), Math.max(20, s.getInt("dzwiek-co-tickow", 100)), pora, s.getDouble("zasieg", 32)));
    }
    for (Map<?, ?> x : c.getMapList("towary")) {
      ConfigurationSection s = sek(x);
      Location l = m.miejsce(s.getList("miejsce"));
      ItemStack it = m.przedmiot(s.getString("przedmiot"));
      if (l == null || it == null) continue;
      towary.add(new Towar(l, it, (float) s.getDouble("skala", 0.45), (float) s.getDouble("pochylenie", 0), s.getBoolean("blok", it.getType().isBlock())));
    }
    for (Map<?, ?> x : c.getMapList("zwierzeta")) {
      ConfigurationSection s = sek(x);
      Location l = m.miejsce(s.getList("miejsce"));
      EntityType t;
      try { t = EntityType.valueOf(s.getString("typ", "CAT").toUpperCase()); } catch (IllegalArgumentException e) { m.getLogger().warning("zwierzeta: zły typ " + s.getString("typ")); continue; }
      if (l == null) continue;
      zwierzeta.add(new Zwierze(l, t, s.getString("wariant"), s.getBoolean("siedzi", false), s.getDouble("promien", 0)));
    }
    for (Map<?, ?> x : c.getMapList("przechodnie")) {
      ConfigurationSection s = sek(x);
      List<double[]> trasa = new ArrayList<>();
      for (Object o : s.getList("trasa", List.of())) {
        if (o instanceof List<?> pt && pt.size() >= 2) trasa.add(new double[] {Mieszkancy.num(pt.get(0)), Mieszkancy.num(pt.get(1))});
      }
      if (trasa.size() < 2) { m.getLogger().warning("przechodnie: trasa < 2 punktów"); continue; }
      Map<EquipmentSlot, ItemStack> stroj = new java.util.HashMap<>();
      ConfigurationSection st = s.getConfigurationSection("stroj");
      if (st != null) for (String k : st.getKeys(false)) {
        EquipmentSlot slot = switch (k) {
          case "glowa" -> EquipmentSlot.HEAD; case "tors" -> EquipmentSlot.CHEST; case "nogi" -> EquipmentSlot.LEGS;
          case "buty" -> EquipmentSlot.FEET; case "reka" -> EquipmentSlot.HAND; case "lewa" -> EquipmentSlot.OFF_HAND; default -> null;
        };
        ItemStack it = m.przedmiot(st.getString(k));
        if (slot != null && it != null) stroj.put(slot, it);
      }
      przechodnie.add(new ZywyP(new Przechodzien(s.getString("id", "p" + przechodnie.size()), s.getString("skorka"), stroj, trasa,
          s.getDouble("predkosc", 0.11), s.getInt("postoj", 60), s.getStringList("kwestie"), s.getString("imie", "Mieszkaniec"),
          s.getBoolean("petla", false))));
    }
    for (Object o : c.getList("usun-tabliczki", List.of())) {
      if (o instanceof List<?> pt && pt.size() >= 3) tabliczki.add(new int[] {(int) Mieszkancy.num(pt.get(0)), (int) Mieszkancy.num(pt.get(1)), (int) Mieszkancy.num(pt.get(2))});
    }
    postawione.clear();
    m.getLogger().info("Życie: " + opis());
  }

  /** Mapa z getMapList → sekcja, żeby czytać getDouble/getList z domyślnymi. */
  private static ConfigurationSection sek(Map<?, ?> x) {
    MemoryConfiguration c = new MemoryConfiguration();
    for (var e : x.entrySet()) c.set(String.valueOf(e.getKey()), e.getValue() instanceof Map<?, ?> mm ? sekMapa(mm) : e.getValue());
    return c;
  }
  private static Object sekMapa(Map<?, ?> x) { return sek(x); }

  // ---------- stawianie ----------

  private static long klucz(int cx, int cz) { return ((long) cx << 32) ^ (cz & 0xffffffffL); }

  void dostaw(World w) {
    // Stałe dekoracje (napisy, towary, zwierzęta) — per chunk: gdy chunk się wczytał od nowa, encje zniknęły z nim.
    for (var it = postawione.iterator(); it.hasNext(); ) {
      long k = it.next();
      if (!w.isChunkLoaded((int) (k >> 32), (int) k)) it.remove();
    }
    stale.removeIf(e -> !e.isValid());
    zywe.removeIf(e -> { boolean d = !e.isValid(); if (d) domy.remove(e); return d; });
    stawiane.clear();
    for (Napis n : napisy) {
      if (!gotowy(w, n.l())) continue;
      stale.add(w.spawn(n.l(), TextDisplay.class, t -> {
        Npcs.ustawNapis(t, n.skala(), n.zasieg());
        if (!n.tlo()) t.setBackgroundColor(Color.fromARGB(0));
        t.setLineWidth(220);
        t.text(Mieszkancy.mm(n.tekst()));
        tag(t);
      }));
    }
    for (Towar t : towary) {
      if (!gotowy(w, t.l())) continue;
      stale.add(w.spawn(t.l(), ItemDisplay.class, d -> {
        d.setPersistent(false);
        d.setItemStack(t.it());
        d.setViewRange(0.4f);
        d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        Quaternionf obrot = new Quaternionf().rotateY((float) Math.toRadians(-t.l().getYaw())).rotateX((float) Math.toRadians(t.pochylenie()));
        d.setTransformation(new Transformation(new Vector3f(), obrot, new Vector3f(t.skala(), t.skala(), t.skala()), new Quaternionf()));
        tag(d);
      }));
    }
    for (Zwierze z : zwierzeta) {
      if (!gotowy(w, z.l())) continue;
      Location l = Mieszkancy.naZiemi(z.l());
      Entity e = w.spawn(l, z.typ().getEntityClass(), x -> {
        x.setPersistent(false);
        x.setInvulnerable(true);
        x.setSilent(false);
        if (x instanceof LivingEntity le) { le.setRemoveWhenFarAway(false); le.setCollidable(false); }
        if (x instanceof Cat cat && z.wariant() != null) {
          var typ = org.bukkit.Registry.CAT_VARIANT.get(org.bukkit.NamespacedKey.minecraft(z.wariant().toLowerCase()));
          if (typ != null) cat.setCatType(typ);
        }
        if (x instanceof Sittable s && z.siedzi()) s.setSitting(true);
        if (x instanceof Cat cat && "lezy".equals(z.wariant())) cat.setLyingDown(true);
        if (x instanceof Mob mob && z.promien() <= 0) mob.setAI(false);
        tag(x);
      });
      zywe.add(e);
      domy.put(e, l);
    }
    // Przechodnie: stawiani, gdy chunk ich pierwszego punktu jest wczytany.
    for (ZywyP p : przechodnie) {
      if (p.e != null && p.e.isValid()) continue;
      double[] start = p.d.trasa().get(0);
      if (!w.isChunkLoaded((int) Math.floor(start[0]) >> 4, (int) Math.floor(start[1]) >> 4)) continue;
      double g = Mieszkancy.grunt(w, start[0], start[1], 96, 110);
      if (Double.isNaN(g)) { m.getLogger().warning("Przechodzień " + p.d.id() + ": brak podłoża w " + start[0] + "," + start[1]); continue; }
      p.x = start[0]; p.y = g; p.z = start[1]; p.cel = 1; p.kierunek = 1; p.czeka = 0;
      Location l = new Location(w, p.x, p.y, p.z);
      p.e = w.spawn(l, Mannequin.class, x -> {
        x.setPersistent(false);
        x.setInvulnerable(true);
        x.setImmovable(true);
        x.setGravity(false);
        x.setSilent(true);
        x.setCollidable(false);
        x.setDescription(null);
        var prof = m.skorka(p.d.skorka());
        if (prof != null) x.setProfile(prof);
        for (var e : p.d.stroj().entrySet()) x.getEquipment().setItem(e.getKey(), e.getValue());
        tag(x);
      });
    }
    // Tabliczki z 1.21 (stary JSON, bez polskich znaków) — zdejmowane, gdy ich chunk jest wczytany; napisy je zastępują.
    for (var it = tabliczki.iterator(); it.hasNext(); ) {
      int[] t = it.next();
      if (!w.isChunkLoaded(t[0] >> 4, t[2] >> 4)) continue;
      Block b = w.getBlockAt(t[0], t[1], t[2]);
      if (b.getState(false) instanceof Sign) {
        b.setType(Material.AIR, false);
        usunieteTabliczki++;
        m.getLogger().info("Zdjęta tabliczka " + t[0] + "," + t[1] + "," + t[2]);
      }
      it.remove();
    }
    postawione.addAll(stawiane);
  }

  /** Czy dekoracja w tym miejscu jest do postawienia (chunk wczytany, jeszcze nie postawiony w tym wczytaniu). */
  private boolean gotowy(World w, Location l) {
    int cx = l.getBlockX() >> 4, cz = l.getBlockZ() >> 4;
    if (!w.isChunkLoaded(cx, cz)) return false;
    long k = klucz(cx, cz);
    if (postawione.contains(k)) return false;
    stawiane.add(k);
    return true;
  }
  private final java.util.Set<Long> stawiane = new java.util.HashSet<>();

  @EventHandler
  public void wczytanieChunka(ChunkLoadEvent e) {
    if (!e.getWorld().getName().equals(m.swiatNazwa)) return;
    postawione.remove(klucz(e.getChunk().getX(), e.getChunk().getZ()));
  }

  private void tag(Entity e) { e.getPersistentDataContainer().set(m.kluczSmiec, PersistentDataType.BYTE, (byte) 1); }

  void usunWszystkie() {
    for (Entity e : stale) if (e.isValid()) e.remove();
    for (Entity e : zywe) if (e.isValid()) e.remove();
    for (ZywyP p : przechodnie) if (p.e != null && p.e.isValid()) { p.e.remove(); p.e = null; }
    stale.clear(); zywe.clear(); domy.clear(); postawione.clear();
  }

  // ---------- tick ----------

  void tick() {
    World w = m.swiat();
    if (w == null) return;
    int now = Bukkit.getCurrentTick();
    List<Player> gracze = w.getPlayers();
    long czas = w.getTime();
    boolean noc = czas > 12800 && czas < 23200;
    for (Efekt e : efekty) {
      if (e.pora() == Pora.NOC && !noc || e.pora() == Pora.DZIEN && noc) continue;
      boolean czastka = e.p() != null && now % e.co() == 0;
      boolean dzwiek = e.dzwiek() != null && now % e.dzwiekCo() == (e.id().hashCode() & 0x7fff) % e.dzwiekCo();
      if (!czastka && !dzwiek) continue;
      if (!ktos(gracze, e.l(), e.zasieg())) continue;
      if (czastka) w.spawnParticle(e.p(), e.l(), e.ilosc(), e.rozrzut()[0], e.rozrzut()[1], e.rozrzut()[2], e.predkosc(), e.dane());
      if (dzwiek) w.playSound(e.l(), e.dzwiek(), SoundCategory.AMBIENT, e.glosnosc(), e.ton() * (0.92f + ThreadLocalRandom.current().nextFloat() * 0.16f));
    }
    for (ZywyP p : przechodnie) idz(p, w, gracze, now);
    if (now % 40 == 0) pilnujZwierzat();
  }

  private static boolean ktos(List<Player> gracze, Location l, double r) {
    double r2 = r * r;
    for (Player p : gracze) if (p.getLocation().distanceSquared(l) < r2) return true;
    return false;
  }

  private void idz(ZywyP p, World w, List<Player> gracze, int now) {
    if (p.e == null || !p.e.isValid()) return;
    // Gracz tuż obok: przechodzień przystaje i patrzy na niego (raz na jakiś czas rzuci słowo).
    Player blisko = null;
    for (Player g : gracze) if (g.getLocation().distanceSquared(p.e.getLocation()) < 2.6 * 2.6) { blisko = g; break; }
    if (blisko != null) {
      if (now % 2 == 0) p.e.lookAt(blisko.getEyeLocation(), io.papermc.paper.entity.LookAnchor.EYES);
      if (!p.d.kwestie().isEmpty()) {
        Integer t = zaczepkiP.get(blisko.getUniqueId());
        if (t == null || now - t > 400) {
          zaczepkiP.put(blisko.getUniqueId(), now);
          m.dymki.powiedz(blisko, p.e, "<#c9b98f>" + p.d.imie(), p.d.kwestie().get(ThreadLocalRandom.current().nextInt(p.d.kwestie().size())), null);
        }
      }
      return;
    }
    if (!ktos(gracze, p.e.getLocation(), 48)) return; // nikt nie patrzy — stoi (nie liczymy trasy)
    if (p.czeka > 0) { p.czeka--; return; }
    double[] cel = p.d.trasa().get(p.cel);
    double dx = cel[0] - p.x, dz = cel[1] - p.z;
    double dyst = Math.sqrt(dx * dx + dz * dz);
    double v = p.d.predkosc();
    if (dyst <= v) {
      p.x = cel[0]; p.z = cel[1];
      int n = p.d.trasa().size();
      boolean koniec = p.cel == n - 1 || p.cel == 0;
      if (p.d.wPetli()) p.cel = (p.cel + 1) % n;
      else {
        if (p.cel + p.kierunek >= n || p.cel + p.kierunek < 0) p.kierunek = -p.kierunek;
        p.cel += p.kierunek;
      }
      if (koniec || ThreadLocalRandom.current().nextInt(4) == 0) p.czeka = p.d.postoj() + ThreadLocalRandom.current().nextInt(p.d.postoj() / 2 + 1);
    } else {
      p.x += dx / dyst * v; p.z += dz / dyst * v;
    }
    double g = Mieszkancy.grunt(w, p.x, p.z, (int) Math.floor(p.y) - 2, (int) Math.floor(p.y) + 1);
    if (!Double.isNaN(g)) p.y += Math.max(-0.25, Math.min(0.25, g - p.y)); // schodek/płyta: płynnie, bez skoku
    float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
    Location l = new Location(w, p.x, p.y, p.z, yaw, 0);
    p.e.teleport(l);
    p.e.setBodyYaw(yaw);
  }

  private void pilnujZwierzat() {
    for (Entity e : zywe) {
      if (!(e instanceof Mob mob) || !mob.hasAI() || !e.isValid()) continue;
      Location dom = domy.get(e);
      if (dom == null) continue;
      double r = 0;
      for (Zwierze z : zwierzeta) if (z.l().getWorld().equals(dom.getWorld()) && Math.abs(z.l().getX() - dom.getX()) < 1 && Math.abs(z.l().getZ() - dom.getZ()) < 1) r = z.promien();
      if (e.getLocation().distanceSquared(dom) > r * r) {
        mob.getPathfinder().moveTo(dom, 1.0);
      } else if (ThreadLocalRandom.current().nextInt(6) == 0) {
        double a = ThreadLocalRandom.current().nextDouble() * Math.PI * 2, d = ThreadLocalRandom.current().nextDouble() * r;
        mob.getPathfinder().moveTo(dom.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d), 0.8);
      }
    }
  }

  // ---------- ochrona ----------

  /** WorldGuard (block-plugin-spawning) i inne pluginy mogą zablokować spawn — nasze encje wracają. */
  @EventHandler(priority = EventPriority.HIGHEST)
  public void spawn(EntitySpawnEvent e) {
    if (e.isCancelled() && e.getEntity().getPersistentDataContainer().has(m.kluczSmiec, PersistentDataType.BYTE)) e.setCancelled(false);
  }

  @EventHandler(ignoreCancelled = true)
  public void obrazenia(EntityDamageEvent e) {
    if (e.getEntity().getPersistentDataContainer().has(m.kluczSmiec, PersistentDataType.BYTE)) e.setCancelled(true);
  }

  /** Kura znosząca jajko na bruk Stolicy = śmieć na ziemi; nasze zwierzęta niczego nie upuszczają. */
  @EventHandler(ignoreCancelled = true)
  public void upuszczenie(org.bukkit.event.entity.EntityDropItemEvent e) {
    if (e.getEntity().getPersistentDataContainer().has(m.kluczSmiec, PersistentDataType.BYTE)) e.setCancelled(true);
  }

  @EventHandler(ignoreCancelled = true)
  public void cel(EntityTargetEvent e) {
    if (e.getEntity().getPersistentDataContainer().has(m.kluczSmiec, PersistentDataType.BYTE)) e.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.LOW)
  public void ppm(PlayerInteractEntityEvent e) {
    Entity x = e.getRightClicked();
    if (!x.getPersistentDataContainer().has(m.kluczSmiec, PersistentDataType.BYTE)) return;
    if (x.getPersistentDataContainer().has(m.kluczNpc, PersistentDataType.STRING)) return; // NPC — obsługuje Npcs
    e.setCancelled(true); // kot/kura: bez oswajania, smyczy, karmienia
    if (x instanceof Cat) x.getWorld().playSound(x.getLocation(), "entity.cat.purreow", SoundCategory.NEUTRAL, 0.7f, 1f);
    if (x instanceof Mannequin mq) mq.swingMainHand();
  }
}
