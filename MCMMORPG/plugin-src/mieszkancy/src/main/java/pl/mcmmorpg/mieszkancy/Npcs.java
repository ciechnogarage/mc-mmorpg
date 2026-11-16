package pl.mcmmorpg.mieszkancy;

import io.papermc.paper.entity.LookAnchor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * NPC Stolicy: manekin (skórka gracza, strój, przedmiot w ręce) + Interaction (prawdziwy klient nie celuje w manekina —
 * klient QA 27.09 przy trenerze Chowańców) + napis nad głową + podpowiedź „PPM” widoczna z bliska.
 *
 * <p>Życie NPC: głowa śledzi najbliższego gracza (8 m), bez gracza patrzy na swój warsztat; zaczepka w dymku przy
 * podejściu (raz na 90 s na NPC, nie częściej niż co 6 s na gracza); czynność co N ticków (machnięcie, dźwięk,
 * cząsteczki przy warsztacie) tylko, gdy gracz jest w zasięgu 24 m.
 */
public final class Npcs implements Listener {
  private static final double ZASIEG_PATRZ = 8, ZASIEG_ZACZEPKA = 4.5, ZASIEG_CZYNNOSC = 24;

  record Opcja(String etykieta, String podpowiedz, String komenda, List<String> odpowiedz, String prowadz, String dzwiek) {}

  record Czynnosc(int coTickow, boolean machniecie, String dzwiek, float glosnosc, float ton, Particle czastka, Object dane,
      int ilosc, double[] rozrzut, double predkosc) {}

  record Def(String id, String imie, String rola, String kolor, Location miejsce, String skorka, Map<EquipmentSlot, ItemStack> stroj,
      Pose poza, List<String> zaczepki, List<String> rozmowa, List<String> pozegnania, List<Opcja> opcje, Czynnosc czynnosc,
      Location patrz, String glos, String gdzie) {}

  static final class Zywy {
    final Def d;
    Mannequin m;
    Interaction hit;
    TextDisplay napis, podpowiedz;
    UUID patrzyNa;
    boolean wSpoczynku;
    long tick;
    Zywy(Def d) { this.d = d; }
    boolean ok() { return m != null && m.isValid(); }
  }

  private final Mieszkancy m;
  private final Map<String, Zywy> npc = new LinkedHashMap<>();
  /** gracz → (npc → tick ostatniej zaczepki) */
  private final Map<UUID, Map<String, Integer>> zaczepione = new HashMap<>();
  private final Map<UUID, Integer> ostatniaZaczepka = new HashMap<>();
  private final Map<UUID, Integer> klikniete = new HashMap<>();
  private final Map<UUID, Integer> prowadzeni = new HashMap<>();
  private final List<String> domyslnePozegnania = List.of("Bywaj.", "Niech Korzeń cię prowadzi.", "Do zobaczenia.");

  public Npcs(Mieszkancy m) { this.m = m; }

  public int ile() { return npc.size(); }
  public long zywych() { return npc.values().stream().filter(Zywy::ok).count(); }
  public Collection<String> ids() { return npc.keySet(); }

  void wczytaj(YamlConfiguration y) {
    npc.clear();
    ConfigurationSection root = y.getConfigurationSection("npc");
    if (root == null) { m.getLogger().warning("npc.yml: brak sekcji 'npc'"); return; }
    for (String id : root.getKeys(false)) {
      ConfigurationSection c = root.getConfigurationSection(id);
      if (c == null) continue;
      Location loc = m.miejsce(c.getList("miejsce"));
      if (loc == null) { m.getLogger().warning("npc." + id + ": brak/zle 'miejsce' albo brak świata"); continue; }
      if (c.getList("miejsce").size() < 4) {
        // Bez podanego yaw: twarzą do Nexusu (środek wyspy).
        double dx = m.srodek.getX() - loc.getX(), dz = m.srodek.getZ() - loc.getZ();
        loc.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
      }
      Map<EquipmentSlot, ItemStack> stroj = new HashMap<>();
      ConfigurationSection s = c.getConfigurationSection("stroj");
      if (s != null) for (String k : s.getKeys(false)) {
        EquipmentSlot slot = switch (k) {
          case "glowa" -> EquipmentSlot.HEAD; case "tors" -> EquipmentSlot.CHEST; case "nogi" -> EquipmentSlot.LEGS;
          case "buty" -> EquipmentSlot.FEET; case "reka" -> EquipmentSlot.HAND; case "lewa" -> EquipmentSlot.OFF_HAND;
          default -> null;
        };
        ItemStack it = m.przedmiot(s.getString(k));
        if (slot != null && it != null) stroj.put(slot, it);
      }
      List<Opcja> opcje = new ArrayList<>();
      for (Map<?, ?> o : c.getMapList("opcje")) {
        opcje.add(new Opcja(str(o.get("etykieta")), str(o.get("podpowiedz")), str(o.get("komenda")),
            lista(o.get("odpowiedz")), str(o.get("prowadz")), m.dzwiek(str(o.get("dzwiek")), "npc." + id + ".opcje")));
      }
      Czynnosc cz = null;
      ConfigurationSection cs = c.getConfigurationSection("czynnosc");
      if (cs != null) {
        Particle p = m.czastka(cs.getString("czastka"), "npc." + id);
        List<Double> r = cs.getDoubleList("rozrzut");
        cz = new Czynnosc(Math.max(10, cs.getInt("co-tickow", 60)), cs.getBoolean("machniecie", true),
            m.dzwiek(cs.getString("dzwiek"), "npc." + id + ".czynnosc"), (float) cs.getDouble("glosnosc", 0.6), (float) cs.getDouble("ton", 1.0),
            p, m.daneCzastki(p, cs), cs.getInt("ilosc", 6),
            new double[] {r.size() > 0 ? r.get(0) : 0.2, r.size() > 1 ? r.get(1) : 0.2, r.size() > 2 ? r.get(2) : 0.2}, cs.getDouble("predkosc", 0.02));
      }
      Pose poza = null;
      String pz = c.getString("poza");
      if (pz != null) try { poza = Pose.valueOf(pz.toUpperCase()); } catch (IllegalArgumentException e) { m.getLogger().warning("npc." + id + ": zła poza " + pz); }
      Location patrz = m.miejsce(c.getList("patrz"));
      npc.put(id, new Zywy(new Def(id, c.getString("imie", id), c.getString("rola", ""), c.getString("kolor", "<gold>"), loc,
          c.getString("skorka"), stroj, poza, c.getStringList("zaczepki"), c.getStringList("rozmowa"),
          c.getStringList("pozegnania").isEmpty() ? domyslnePozegnania : c.getStringList("pozegnania"),
          opcje, cz, patrz, m.dzwiek(c.getString("glos", "item.book.page_turn"), "npc." + id + ".glos"),
          c.getString("gdzie", ""))));
    }
    m.getLogger().info("NPC: " + npc.size() + " z npc.yml");
  }

  private static String str(Object o) { return o == null ? null : String.valueOf(o); }

  private static List<String> lista(Object o) {
    if (o == null) return null;
    if (o instanceof List<?> l) { List<String> r = new ArrayList<>(); for (Object x : l) r.add(String.valueOf(x)); return r; }
    return List.of(String.valueOf(o));
  }

  // ---------- stawianie ----------

  /** Co sekundę: NPC, których chunk jest wczytany, a encji brak (start, chunk wczytany od nowa) — stawiamy. */
  void dostaw(World w) {
    for (Zywy z : npc.values()) {
      Location l = z.d.miejsce();
      if (!w.isChunkLoaded(l.getBlockX() >> 4, l.getBlockZ() >> 4)) continue;
      if (z.ok() && z.hit != null && z.hit.isValid() && z.napis != null && z.napis.isValid()) continue;
      usun(z);
      postaw(z);
    }
  }

  private void postaw(Zywy z) {
    Def d = z.d;
    Location l = Mieszkancy.naZiemi(d.miejsce());
    z.m = l.getWorld().spawn(l, Mannequin.class, x -> {
      x.setPersistent(false);
      x.setInvulnerable(true);
      x.setImmovable(true);
      x.setSilent(true);
      x.setGravity(false);
      x.setCollidable(false);
      x.customName(null);
      x.setCustomNameVisible(false);
      x.setDescription(null);
      var prof = m.skorka(d.skorka());
      if (prof != null) x.setProfile(prof);
      for (var e : d.stroj().entrySet()) x.getEquipment().setItem(e.getKey(), e.getValue());
      if (d.poza() != null && Mannequin.validPoses().contains(d.poza())) x.setPose(d.poza(), true);
      x.setBodyYaw(l.getYaw());
      x.setRotation(l.getYaw(), 0);
      tag(x, d.id());
    });
    z.hit = l.getWorld().spawn(l, Interaction.class, i -> {
      i.setPersistent(false);
      i.setInteractionWidth(0.9f);
      i.setInteractionHeight(d.poza() == Pose.SNEAKING ? 1.6f : 2.0f);
      i.setResponsive(true);
      tag(i, d.id());
    });
    double h = d.poza() == Pose.SNEAKING ? 1.5 : 1.8;
    z.napis = l.getWorld().spawn(l.clone().add(0, h + 0.55, 0), TextDisplay.class, t -> {
      ustawNapis(t, 0.9f, 0.45f);
      t.text(Mieszkancy.mm(d.kolor() + "<b>" + d.imie() + "</b>" + (d.rola().isBlank() ? "" : "\n<#d8cba8>" + d.rola())));
      tag(t, d.id());
    });
    z.podpowiedz = l.getWorld().spawn(l.clone().add(0, h + 0.25, 0), TextDisplay.class, t -> {
      ustawNapis(t, 0.6f, 0.1f);
      t.setBackgroundColor(Color.fromARGB(0));
      t.text(Mieszkancy.mm("<#f0c060>◆ <white>PPM</white> — porozmawiaj"));
      tag(t, d.id());
    });
    z.patrzyNa = null;
    z.wSpoczynku = true;
  }

  static void ustawNapis(TextDisplay t, float skala, float zasieg) {
    t.setPersistent(false);
    t.setBillboard(Display.Billboard.CENTER);
    t.setShadowed(true);
    t.setAlignment(TextDisplay.TextAlignment.CENTER);
    t.setBackgroundColor(Color.fromARGB(0x90100C08));
    t.setViewRange(zasieg);
    t.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(skala, skala, skala), new AxisAngle4f()));
  }

  private void tag(Entity e, String id) {
    e.getPersistentDataContainer().set(m.kluczNpc, PersistentDataType.STRING, id);
    e.getPersistentDataContainer().set(m.kluczSmiec, PersistentDataType.BYTE, (byte) 1);
  }

  private void usun(Zywy z) {
    for (Entity e : new Entity[] {z.m, z.hit, z.napis, z.podpowiedz}) if (e != null && e.isValid()) e.remove();
    z.m = null; z.hit = null; z.napis = null; z.podpowiedz = null;
  }

  void usunWszystkie() { for (Zywy z : npc.values()) usun(z); }

  // ---------- życie ----------

  void tick() {
    int now = Bukkit.getCurrentTick();
    World w = m.swiat();
    if (w == null) return;
    List<Player> gracze = w.getPlayers();
    for (Zywy z : npc.values()) {
      if (!z.ok()) continue;
      Location l = z.m.getLocation();
      Player blisko = null;
      double best = Double.MAX_VALUE;
      boolean ktosWZasiegu = false;
      for (Player p : gracze) {
        double d2 = p.getLocation().distanceSquared(l);
        if (d2 < ZASIEG_CZYNNOSC * ZASIEG_CZYNNOSC) ktosWZasiegu = true;
        if (d2 < best) { best = d2; blisko = p; }
      }
      // Głowa co 2 ticki: śledzi gracza; ciało obraca się dopiero, gdy głowa wychodzi poza ~70°.
      if ((now & 1) == 0) {
        if (blisko != null && best < ZASIEG_PATRZ * ZASIEG_PATRZ && !blisko.isInvisible()) {
          z.m.lookAt(blisko.getEyeLocation(), LookAnchor.EYES);
          float yawDoGracza = z.m.getYaw();
          float roznica = wrap(yawDoGracza - z.m.getBodyYaw());
          if (Math.abs(roznica) > 70) z.m.setBodyYaw(z.m.getBodyYaw() + Math.signum(roznica) * (Math.abs(roznica) - 70));
          z.patrzyNa = blisko.getUniqueId();
          z.wSpoczynku = false;
        } else if (!z.wSpoczynku) {
          if (z.d.patrz() != null) z.m.lookAt(z.d.patrz().getX(), z.d.patrz().getY(), z.d.patrz().getZ(), LookAnchor.EYES);
          else z.m.setRotation(z.d.miejsce().getYaw(), 0);
          z.m.setBodyYaw(z.d.patrz() != null ? z.m.getYaw() : z.d.miejsce().getYaw());
          z.patrzyNa = null;
          z.wSpoczynku = true;
        }
      }
      // Zaczepka przy podejściu.
      if (blisko != null && best < ZASIEG_ZACZEPKA * ZASIEG_ZACZEPKA && !z.d.zaczepki().isEmpty()) zaczepka(z, blisko, now);
      // Czynność przy warsztacie.
      Czynnosc c = z.d.czynnosc();
      if (c != null && ktosWZasiegu && now - z.tick >= c.coTickow()) {
        z.tick = now + ThreadLocalRandom.current().nextInt(c.coTickow() / 3 + 1);
        boolean rozmawia = z.patrzyNa != null;
        if (c.machniecie() && !rozmawia) z.m.swingMainHand();
        Location gdzie = z.d.patrz() != null ? z.d.patrz().clone().add(0.5, 0.6, 0.5) : l.clone().add(0, 1.2, 0);
        if (c.dzwiek() != null) w.playSound(gdzie, c.dzwiek(), SoundCategory.NEUTRAL, c.glosnosc(), c.ton() * (0.9f + ThreadLocalRandom.current().nextFloat() * 0.2f));
        if (c.czastka() != null) w.spawnParticle(c.czastka(), gdzie, c.ilosc(), c.rozrzut()[0], c.rozrzut()[1], c.rozrzut()[2], c.predkosc(), c.dane());
      }
    }
    if ((now % 10) == 0) prowadzenieTick(now);
  }

  private static float wrap(float a) { a %= 360; if (a > 180) a -= 360; if (a < -180) a += 360; return a; }

  private void zaczepka(Zywy z, Player p, int now) {
    UUID u = p.getUniqueId();
    Integer last = ostatniaZaczepka.get(u);
    if (last != null && now - last < 120) return;
    Map<String, Integer> mp = zaczepione.computeIfAbsent(u, k -> new HashMap<>());
    Integer t = mp.get(z.d.id());
    if (t != null && now - t < 1800) return;
    mp.put(z.d.id(), now);
    ostatniaZaczepka.put(u, now);
    List<String> l = z.d.zaczepki();
    m.dymki.powiedz(p, z.m, z.d.kolor() + z.d.imie(), l.get(ThreadLocalRandom.current().nextInt(l.size())), z.napis);
    z.m.swingMainHand();
  }

  // ---------- rozmowa ----------

  @EventHandler(priority = EventPriority.LOWEST)
  public void uzyciePrzedmiotu(PlayerInteractEvent e) {
    // PPM w NPC z przedmiotem w ręce (Dziennik, kostur) nie może go przy okazji użyć (Chowance, klient QA 27.09).
    Integer t = klikniete.get(e.getPlayer().getUniqueId());
    if (t == null || Bukkit.getCurrentTick() - t > 1) return;
    e.setCancelled(true);
    e.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void ppm(PlayerInteractEntityEvent e) {
    String id = e.getRightClicked().getPersistentDataContainer().get(m.kluczNpc, PersistentDataType.STRING);
    if (id == null) return;
    e.setCancelled(true);
    if (e.getHand() != EquipmentSlot.HAND) return;
    Player p = e.getPlayer();
    Integer poprz = klikniete.put(p.getUniqueId(), Bukkit.getCurrentTick());
    if (poprz != null && Bukkit.getCurrentTick() - poprz < 10) return; // podwójny pakiet (hitbox + manekin)
    Bukkit.getScheduler().runTaskLater(m, () -> { if (p.isOnline()) rozmowa(p, id); }, 2L);
  }

  @EventHandler(ignoreCancelled = true)
  public void obrazenia(EntityDamageEvent e) {
    if (e.getEntity().getPersistentDataContainer().has(m.kluczNpc, PersistentDataType.STRING)) e.setCancelled(true);
  }

  @EventHandler
  public void wyjscie(PlayerQuitEvent e) {
    UUID u = e.getPlayer().getUniqueId();
    zaczepione.remove(u); ostatniaZaczepka.remove(u); klikniete.remove(u); prowadzeni.remove(u);
    m.dymki.zdejmij(u);
  }

  public boolean rozmowa(Player p, String id) {
    Zywy z = npc.get(id);
    if (z == null) return false;
    Def d = z.d;
    p.closeInventory();
    m.dymki.zdejmij(p.getUniqueId());
    if (z.ok()) {
      z.m.lookAt(p.getEyeLocation(), LookAnchor.EYES);
      z.m.swingMainHand();
    }
    if (d.glos() != null) p.playSound(p.getLocation(), d.glos(), SoundCategory.NEUTRAL, 0.7f, 1.0f);
    String kwestia = d.rozmowa().isEmpty() ? "…" : d.rozmowa().get(ThreadLocalRandom.current().nextInt(d.rozmowa().size()));
    List<Okna.Przycisk> btn = new ArrayList<>();
    for (Opcja o : d.opcje()) btn.add(new Okna.Przycisk(o.etykieta(), o.podpowiedz(), x -> wykonaj(x, z, o)));
    m.okna.pokaz(p, tytul(d), List.of("<#f3ead2>" + kwestia), btn, new Okna.Przycisk("<gray>Do widzenia", "", x -> pozegnaj(x, z)));
    return true;
  }

  private static String tytul(Def d) { return d.kolor() + d.imie() + (d.rola().isBlank() ? "" : " <gray>· " + d.rola()); }

  private void wykonaj(Player p, Zywy z, Opcja o) {
    if (o.dzwiek() != null) p.playSound(p.getLocation(), o.dzwiek(), SoundCategory.NEUTRAL, 0.8f, 1.0f);
    if (o.odpowiedz() != null && !o.odpowiedz().isEmpty()) {
      List<String> tresc = new ArrayList<>();
      for (String s : o.odpowiedz()) tresc.add("<#f3ead2>" + s);
      List<Okna.Przycisk> btn = new ArrayList<>();
      if (o.komenda() != null) btn.add(new Okna.Przycisk("<gold>" + "Zgoda", "", x -> komenda(x, o.komenda())));
      btn.add(new Okna.Przycisk("<#d8cba8>‹ Wróć", "", x -> rozmowa(x, z.d.id())));
      m.okna.pokaz(p, tytul(z.d), tresc, btn, new Okna.Przycisk("<gray>Do widzenia", "", x -> pozegnaj(x, z)));
      return;
    }
    if (o.prowadz() != null) { prowadz(p, o.prowadz()); return; }
    if (o.komenda() != null) komenda(p, o.komenda());
  }

  private void komenda(Player p, String k) {
    // Tick później: okno dialogu zamyka się po kliknięciu, nowe GUI (DeluxeMenus/stacja) musi wejść po nim.
    Bukkit.getScheduler().runTaskLater(m, () -> { if (p.isOnline()) m.konsola(k, p); }, 2L);
  }

  private void pozegnaj(Player p, Zywy z) {
    if (!z.ok() || z.d.pozegnania().isEmpty()) return;
    m.dymki.powiedz(p, z.m, z.d.kolor() + z.d.imie(), z.d.pozegnania().get(ThreadLocalRandom.current().nextInt(z.d.pozegnania().size())), z.napis);
  }

  // ---------- prowadzenie (Q wskazuje drogę do NPC, usługa jest tylko u niego) ----------

  public boolean prowadz(Player p, String id) {
    Zywy z = npc.get(id);
    if (z == null) return false;
    p.closeInventory();
    Location cel = z.d.miejsce();
    if (!p.getWorld().equals(cel.getWorld())) {
      p.sendMessage(Mieszkancy.mm("<#f0c060>◆ <white>" + z.d.imie() + "</white> <gray>(" + z.d.rola() + ") czeka w Stolicy"
          + (z.d.gdzie().isBlank() ? "" : " — " + z.d.gdzie()) + ". Wróć do Stolicy, a pokażę ci drogę."));
      return true;
    }
    prowadzeni.put(p.getUniqueId(), Bukkit.getCurrentTick());
    zapiszCel(p, id);
    p.playSound(p.getLocation(), "block.note_block.chime", SoundCategory.PLAYERS, 0.6f, 1.4f);
    return true;
  }

  private final Map<UUID, String> cele = new HashMap<>();
  private void zapiszCel(Player p, String id) { cele.put(p.getUniqueId(), id); }

  private void prowadzenieTick(int now) {
    for (var it = prowadzeni.entrySet().iterator(); it.hasNext(); ) {
      var e = it.next();
      Player p = Bukkit.getPlayer(e.getKey());
      String id = cele.get(e.getKey());
      Zywy z = id == null ? null : npc.get(id);
      if (p == null || z == null || !p.getWorld().equals(z.d.miejsce().getWorld()) || now - e.getValue() > 20 * 90) { it.remove(); continue; }
      Location cel = z.d.miejsce().clone().add(0, 1, 0);
      Location od = p.getLocation().add(0, 1.1, 0);
      double dyst = od.distance(cel);
      if (dyst < 3.5) {
        p.sendActionBar(Mieszkancy.mm("<#f0c060>◆ <white>" + z.d.imie() + "</white> <gray>— jesteś na miejscu"));
        it.remove();
        continue;
      }
      p.sendActionBar(Mieszkancy.mm("<#f0c060>➜ <white>" + z.d.imie() + "</white> <gray>· " + z.d.rola() + " · <white>" + (int) dyst + " m"));
      // Ścieżka świetlików od gracza w stronę NPC (tylko dla niego): 8 punktów co 0,9 m.
      var kier = cel.toVector().subtract(od.toVector()).normalize();
      for (int i = 1; i <= 8 && i * 0.9 < dyst; i++) {
        Location pt = od.clone().add(kier.clone().multiply(i * 0.9 + 0.6));
        p.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(0xF0C060), 0.9f));
      }
      // Nad samym NPC słup światła widoczny z daleka.
      p.spawnParticle(Particle.END_ROD, cel.clone().add(0, 1.6, 0), 2, 0.05, 0.6, 0.05, 0.0);
    }
  }

  /** Dla Zycie: lista żywych manekinów (np. przechodnie omijają NPC). */
  List<Mannequin> manekiny() {
    List<Mannequin> l = new ArrayList<>();
    for (Zywy z : npc.values()) if (z.ok()) l.add(z.m);
    return l;
  }

  static Component brak() { return Component.empty(); }
}
