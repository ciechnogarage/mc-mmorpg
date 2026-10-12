package pl.mcmmorpg.zaklecia;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.comp.interaction.InteractionType;
import io.lumine.mythic.lib.api.event.fake.FakeEntityDamageByEntityEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

/** Cykl zycia rzeczy przywolanych. Kazdy byt ma znacznik do sprzatania po awarii/restartcie. */
final class EfektyCzasowe implements Listener {
  enum Odmowa { BRAK, CEL, SOJUSZNIK, MIEJSCE_PRZYWOLANIA, KONFIGURACJA }
  private record Bron(int slot, boolean prawa, ItemStack nosnik, long koniec) {}
  private record Sluga(Entity byt, String zaklecie, long koniec) {}
  private final Zaklecia plugin;
  private final Ksiega ksiega;
  private final Nosnik nosnik;
  private final Skille skille;
  private final NamespacedKey kBron, kZrodlo, kWlasciciel;
  private final Map<UUID, Map<Boolean, Bron>> bronie = new HashMap<>();
  private final Map<UUID, Map<Boolean, Bron>> poSmierci = new HashMap<>();
  private final Map<UUID, Deque<Sluga>> sludzy = new HashMap<>();
  private final Map<UUID, Long> spokoj = new HashMap<>();
  private final Map<UUID, LivingEntity> oczekiwaneCele = new HashMap<>();
  /** Ostatni cel wskazany przywolancowi przez plugin — AI MM potrafi go zgubic, strzelcy (atronach) strzelaja w niego. */
  private final Map<UUID, LivingEntity> celSlugi = new HashMap<>();
  private final Map<UUID, Long> ostatnieCelowanie = new HashMap<>();
  private final Set<String> bledneKonfiguracje = new HashSet<>();
  private boolean sprawdzanieCelu;
  private long tick;

  EfektyCzasowe(Zaklecia plugin, Ksiega ksiega, Nosnik nosnik, Skille skille) {
    this.plugin = plugin; this.ksiega = ksiega; this.nosnik = nosnik; this.skille = skille;
    kBron = new NamespacedKey(plugin, "bron_przywolana");
    kZrodlo = new NamespacedKey(plugin, "bron_zrodlo");
    kWlasciciel = new NamespacedKey(plugin, "sluga_wlasciciel");
    Bukkit.getPluginManager().registerEvents(this, plugin);
    Bukkit.getScheduler().runTaskTimer(plugin, this::tik, 1, 1);
    for (Player p : Bukkit.getOnlinePlayers()) sprzatajPoRestarcie(p);
  }

  static String nazwa(Ksiega.Zaklecie z, String faza, String reka) {
    return "Zak_" + Character.toUpperCase(z.id().charAt(0)) + z.id().substring(1) + "_" + faza + "_" + reka;
  }

  /** Serwerowy Target Actor; blok zaslania byt, zasieg interakcji klienta nie ma znaczenia. */
  static LivingEntity cel(Player p, double zasieg, java.util.function.Predicate<LivingEntity> warunek) {
    Location oko = p.getEyeLocation();
    double max = zasieg;
    RayTraceResult blok = p.getWorld().rayTraceBlocks(oko, oko.getDirection(), zasieg, FluidCollisionMode.NEVER, true);
    if (blok != null) max = Math.min(max, blok.getHitPosition().distance(oko.toVector()));
    RayTraceResult traf = p.getWorld().rayTraceEntities(oko, oko.getDirection(), max, 0.6,   // wspomaganie celowania (cel sie rusza)
        e -> e instanceof LivingEntity le && !le.isDead() && e != p && !(e instanceof ArmorStand));
    return traf != null && traf.getHitEntity() instanceof LivingEntity le && warunek.test(le) ? le : null;
  }

  boolean sojusznik(Player p, LivingEntity cel) {
    String nazwa = cel.getName().toLowerCase(java.util.Locale.ROOT);
    if (cel.isDead() || org.bukkit.Tag.ENTITY_TYPES_UNDEAD.isTagged(cel.getType())
        || nazwa.contains("atronach") || nazwa.contains("nieumar") || nazwa.contains("undead")
        || nazwa.contains("golem") || nazwa.contains("konstrukt") || nazwa.contains("construct")
        || cel.getType().name().contains("GOLEM")) return false;
    try {
      var am = MythicBukkit.inst().getAPIHelper().getMythicMobInstance(cel);
      if (am != null) {
        String id = am.getType().getInternalName().toLowerCase(java.util.Locale.ROOT);
        if (id.contains("atronach") || id.contains("undead") || id.contains("nieumar")
            || id.contains("zombie") || id.contains("skeleton") || id.contains("szkielet")
            || id.contains("wither") || id.contains("golem") || id.contains("construct")
            || id.contains("konstrukt")) return false;
      }
    } catch (Throwable ex) { return false; }
    if (cel instanceof Player) return MythicLib.inst().getEntities().canTarget(p, cel, InteractionType.SUPPORT_ACTION);
    String wlasciciel = cel.getPersistentDataContainer().get(kWlasciciel, PersistentDataType.STRING);
    if (wlasciciel != null) return wlasciciel.equals(p.getUniqueId().toString());
    // MythicLib SUPPORT odmawia nie-graczom; przyjazne NPC i zwierzeta wybieramy przez Bukkit.
    return !(cel instanceof org.bukkit.entity.Enemy);
  }

  private boolean chowance(Entity e) {
    try {
      var am = MythicBukkit.inst().getAPIHelper().getMythicMobInstance(e);
      return am != null && am.getType().getInternalName().toUpperCase(java.util.Locale.ROOT).startsWith("CHW_");
    } catch (Throwable ignored) { return false; }
  }

  boolean wrog(Player p, LivingEntity cel) {
    return cel instanceof org.bukkit.entity.Enemy && !chowance(cel)
        && !cel.getPersistentDataContainer().has(kWlasciciel, PersistentDataType.STRING)
        && MythicLib.inst().getEntities().canTarget(p, cel, InteractionType.OFFENSE_ACTION);
  }

  Odmowa przygotuj(Player p, Ksiega.Zaklecie z, boolean obie) {
    if (z.typ().equals("bron")) {
      if (MythicBukkit.inst().getItemManager().getItemStack(bronId(z)) == null) return konfiguracja(z);
      return Odmowa.BRAK;
    }
    if (z.typ().equals("przywolanie")) {
      String mob = plugin.getConfig().getString("zaklecia." + z.id() + ".mob", "");
      if (mob.isBlank() || MythicBukkit.inst().getAPIHelper().getMythicMob(mob) == null) return konfiguracja(z);
      Location oko = p.getEyeLocation();
      RayTraceResult r = p.getWorld().rayTraceBlocks(oko, oko.getDirection(), z.zasieg(), FluidCollisionMode.NEVER, true);
      org.bukkit.util.Vector przod = p.getLocation().getDirection().setY(0);
      if (przod.lengthSquared() < 0.01) przod.setX(1);
      Location at = r == null ? p.getLocation().add(przod.normalize().multiply(2))
          : r.getHitPosition().toLocation(p.getWorld()).add(0, 1, 0);
      return at.getBlock().isPassable() && at.clone().add(0, 1, 0).getBlock().isPassable()
          ? Odmowa.BRAK : Odmowa.MIEJSCE_PRZYWOLANIA;
    }
    if (z.typ().equals("na_cel")) return cel(p, z.zasieg(), e -> wrog(p, e) && poziom(e) <=
        maksPoziom(z, obie)) != null ? Odmowa.BRAK : Odmowa.CEL;
    if (z.typ().equals("ladowany") && z.cel().equals("inny")) {
      LivingEntity cel = cel(p, z.zasieg(), e -> sojusznik(p, e));
      if (plugin.getConfig().getBoolean("debug", false)) plugin.getLogger().info("ZAKDBG heal prepare "
          + z.id() + " target=" + (cel == null ? "null" : cel.getUniqueId()));
      return cel != null ? Odmowa.BRAK : Odmowa.SOJUSZNIK;
    }
    return Odmowa.BRAK;
  }

  private Odmowa konfiguracja(Ksiega.Zaklecie z) {
    if (bledneKonfiguracje.add(z.id())) plugin.getLogger().warning("Brak zasobu zaklecia " + z.id());
    return Odmowa.KONFIGURACJA;
  }

  private int maksPoziom(Ksiega.Zaklecie z, boolean obie) {
    int baza = plugin.getConfig().getInt("zaklecia." + z.id() + ".maks-poziom", 9);
    return obie ? (int) Math.floor(baza * z.efektOburacz()) : baza;
  }

  private String bronId(Ksiega.Zaklecie z) {
    return plugin.getConfig().getString("zaklecia." + z.id() + ".bron", "");
  }

  private int poziom(Entity e) {
    try {
      var am = MythicBukkit.inst().getAPIHelper().getMythicMobInstance(e);
      return am == null ? 0 : (int) Math.ceil(am.getLevel());
    } catch (Throwable ignored) { return Integer.MAX_VALUE; }
  }

  boolean wystrzel(Player p, Ksiega.Zaklecie z, boolean prawa, boolean obie, String reka) {
    if (z.typ().equals("bron")) {
      if (!dajBron(p, z, prawa || obie, obie)) return false;
      skille.cast(p, nazwa(z, "Wystrzal", reka));
      return true;
    }
    else if (z.typ().equals("przywolanie")) return przywolaj(p, z, obie, reka);
    else if (z.typ().equals("na_cel")) {
      LivingEntity cel = cel(p, z.zasieg(), e -> wrog(p, e) && poziom(e) <= maksPoziom(z, obie));
      return cel != null && skille.cele(p, nazwa(z, "Wystrzal", reka), List.of(cel));
    } else if (z.typ().equals("ladowany")) {
      String nazwa = nazwa(z, "Wystrzal", reka);
      if (z.cel().equals("inny")) {
        LivingEntity cel = cel(p, z.zasieg(), e -> sojusznik(p, e));
        if (plugin.getConfig().getBoolean("debug", false)) plugin.getLogger().info("ZAKDBG heal fire "
            + z.id() + " target=" + (cel == null ? "null" : cel.getUniqueId())
            + " hp=" + (cel == null ? "-" : cel.getHealth()));
        return cel != null && skille.cele(p, nazwa, List.of(cel));
      } else if (z.cel().equals("obszar")) {
        List<Entity> cele = new java.util.ArrayList<>();
        cele.add(p);
        for (Entity e : p.getNearbyEntities(z.promien(), z.promien(), z.promien()))
          if (e instanceof LivingEntity zywy && !zywy.isDead()
              && e.getLocation().distanceSquared(p.getLocation()) <= z.promien() * z.promien()
              && sojusznik(p, zywy)) cele.add(e);
        return skille.cele(p, nazwa, cele);
      } else if (z.cel().equals("siebie")) return skille.cele(p, nazwa, List.of(p));
      else return skille.cast(p, nazwa);
    }
    return false;
  }

  void uspokoj(LivingEntity cel, int ticki) {
    if (!(cel instanceof Mob m)) return;
    m.setTarget(null);
    m.setAware(false);
    spokoj.put(cel.getUniqueId(), tick + Math.max(1, ticki));
  }

  private boolean dajBron(Player p, Ksiega.Zaklecie z, boolean prawa, boolean obie) {
    int slot = prawa ? p.getInventory().getHeldItemSlot() : 40;
    ItemStack teraz = prawa ? p.getInventory().getItem(slot) : p.getInventory().getItemInOffHand();
    Map<Boolean, Bron> mapa = bronie.computeIfAbsent(p.getUniqueId(), u -> new HashMap<>());
    Bron stara = mapa.get(prawa);
    if (stara == null && (teraz == null || ksiega.w(teraz) == null)) return false;
    ItemStack oryginal = stara == null ? teraz.clone() : stara.nosnik();
    ItemStack bron = MythicBukkit.inst().getItemManager().getItemStack(bronId(z));
    if (bron == null) return false;
    bron.editMeta(m -> {
      m.getPersistentDataContainer().set(kBron, PersistentDataType.BYTE, (byte) 1);
      m.getPersistentDataContainer().set(kZrodlo, PersistentDataType.STRING, z.id());
    });
    if (prawa) p.getInventory().setItem(slot, bron);
    else p.getInventory().setItemInOffHand(bron);
    mapa.put(prawa, new Bron(slot, prawa, oryginal, tick + z.czasZycia() * (obie ? 2 : 1)));
    return true;
  }

  private boolean przywolaj(Player p, Ksiega.Zaklecie z, boolean obie, String reka) {
    Location oko = p.getEyeLocation();
    RayTraceResult r = p.getWorld().rayTraceBlocks(oko, oko.getDirection(), z.zasieg(), FluidCollisionMode.NEVER, true);
    org.bukkit.util.Vector przod = p.getLocation().getDirection().setY(0);
    if (przod.lengthSquared() < 0.01) przod.setX(1);
    Location at = r == null ? p.getLocation().add(przod.normalize().multiply(2))
        : r.getHitPosition().toLocation(p.getWorld()).add(0, 1, 0);
    if (!at.getBlock().isPassable() || !at.clone().add(0, 1, 0).getBlock().isPassable()) return false;
    String mob = plugin.getConfig().getString("zaklecia." + z.id() + ".mob", "");
    try {
      Entity e = MythicBukkit.inst().getAPIHelper().spawnMythicMob(mob, at);
      if (e == null) return false;
      var am = MythicBukkit.inst().getAPIHelper().getMythicMobInstance(e);
      if (am == null) { e.remove(); return false; }
      am.setOwnerUUID(p.getUniqueId());   // MM 5.11.2: ActiveMob.setOwnerUUID (setOwner nie istnieje)
      if (e instanceof Wolf w) w.setOwner(p);
      e.setPersistent(false);
      e.getPersistentDataContainer().set(kWlasciciel, PersistentDataType.STRING, p.getUniqueId().toString());
      Deque<Sluga> lista = sludzy.computeIfAbsent(p.getUniqueId(), u -> new ArrayDeque<>());
      int limit = Math.max(1, plugin.getConfig().getInt("przywolania.limit", 1));
      while (lista.size() >= limit) usun(lista.removeFirst().byt());
      lista.addLast(new Sluga(e, z.id(), tick + z.czasZycia() * (obie ? 2 : 1)));
      skille.punkt(p, nazwa(z, "Wystrzal", reka), at);
      return true;
    } catch (Throwable ex) { plugin.getLogger().warning("Przywolanie " + mob + ": " + ex); return false; }
  }

  private boolean oznaczona(ItemStack i) {
    return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(kBron, PersistentDataType.BYTE);
  }

  private void zdejmij(Player p, boolean prawa) {
    Map<Boolean, Bron> mapa = bronie.get(p.getUniqueId());
    if (mapa == null) return;
    Bron b = mapa.remove(prawa);
    if (b == null) return;
    ItemStack obecna = prawa ? p.getInventory().getItem(b.slot()) : p.getInventory().getItemInOffHand();
    if (oznaczona(obecna)) {
      if (prawa) p.getInventory().setItem(b.slot(), b.nosnik());
      else p.getInventory().setItemInOffHand(b.nosnik());
    }
    if (mapa.isEmpty()) bronie.remove(p.getUniqueId());
  }

  void zdejmijBronie(Player p) { zdejmij(p, true); zdejmij(p, false); }

  private void sprzatajPoRestarcie(Player p) {
    for (int i = 0; i < 41; i++) {
      ItemStack item = p.getInventory().getItem(i);
      if (!oznaczona(item)) continue;
      String id = item.getItemMeta().getPersistentDataContainer().get(kZrodlo, PersistentDataType.STRING);
      Ksiega.Zaklecie z = ksiega.poId.get(id);
      p.getInventory().setItem(i, z == null ? null : nosnik.zrob(z));
    }
  }

  private void usun(Entity e) {
    oczekiwaneCele.remove(e.getUniqueId());
    celSlugi.remove(e.getUniqueId());
    ostatnieCelowanie.remove(e.getUniqueId());
    if (e.isValid()) {
      e.getWorld().spawnParticle(Particle.SMOKE, e.getLocation().add(0, 0.8, 0), 12, 0.4, 0.5, 0.4, 0.01);
      e.remove();
    }
  }

  private void usunSlugi(UUID u) {
    Deque<Sluga> l = sludzy.remove(u);
    if (l != null) for (Sluga s : l) usun(s.byt());
  }

  private void tik() {
    tick++;
    for (Player p : Bukkit.getOnlinePlayers()) {
      Map<Boolean, Bron> mapa = bronie.get(p.getUniqueId());
      if (mapa != null) for (boolean prawa : List.of(true, false)) {
        Bron b = mapa.get(prawa);
        if (b != null && (tick >= b.koniec() || p.isDead())) zdejmij(p, prawa);
      }
    }
    for (var it = sludzy.entrySet().iterator(); it.hasNext();) {
      var wpis = it.next(); Player p = Bukkit.getPlayer(wpis.getKey());
      for (var j = wpis.getValue().iterator(); j.hasNext();) {
        Sluga s = j.next();
        if (p == null || !p.isOnline() || p.isDead() || tick >= s.koniec() || !s.byt().isValid()
            || s.byt().getWorld() != p.getWorld()) { usun(s.byt()); j.remove(); }
        else if (s.byt() instanceof Mob m) {
          aktualizujCel(m);
          strzelec(m, s.zaklecie());
        }
      }
      if (wpis.getValue().isEmpty()) it.remove();
    }
    for (var it = spokoj.entrySet().iterator(); it.hasNext();) {
      var wpis = it.next();
      if (tick < wpis.getValue()) continue;
      Entity e = Bukkit.getEntity(wpis.getKey());
      if (e instanceof Mob m && m.isValid()) m.setAware(true);
      it.remove();
    }
  }

  private boolean celWZasiegu(Mob sluga, LivingEntity cel) {
    return cel.isValid() && !cel.isDead() && sluga.getWorld() == cel.getWorld()
        && sluga.getLocation().distanceSquared(cel.getLocation()) <= 24 * 24
        && dozwolonyCel(sluga, cel);
  }

  private void aktualizujCel(Mob sluga) {
    UUID id = sluga.getUniqueId();
    LivingEntity obecny = sluga.getTarget();
    LivingEntity nowy = oczekiwaneCele.remove(id);
    if (nowy != null && !celWZasiegu(sluga, nowy)) nowy = null;
    if (obecny != null && celWZasiegu(sluga, obecny) && nowy != null && obecny != nowy
        && sluga.getLocation().distanceSquared(obecny.getLocation()) < 6 * 6) nowy = null;
    if (nowy == null && (obecny == null || celWZasiegu(sluga, obecny))) return;
    if (tick - ostatnieCelowanie.getOrDefault(id, -10L) < 10) {
      if (nowy != null) oczekiwaneCele.put(id, nowy);
      return;
    }
    if (nowy != obecny) {
      sluga.setTarget(nowy);
      ostatnieCelowanie.put(id, tick);
    }
    if (nowy != null) celSlugi.put(id, nowy);
  }

  /** Przywolancy-strzelcy (config `zaklecia.<id>.strzal`, np. Zak_Atronach_Strzal) strzelaja co 30 tickow w swoj cel. */
  private void strzelec(Mob m, String zaklecie) {
    String skill = plugin.getConfig().getString("zaklecia." + zaklecie + ".strzal", "");
    if (skill.isBlank() || tick % 30 != 0) return;
    LivingEntity cel = m.getTarget();
    if (cel == null || !celWZasiegu(m, cel)) cel = celSlugi.get(m.getUniqueId());
    if (cel == null || !celWZasiegu(m, cel)) { celSlugi.remove(m.getUniqueId()); return; }
    if (m.getTarget() != cel) m.setTarget(cel);
    try { MythicBukkit.inst().getAPIHelper().castSkill(m, skill, m, m.getLocation(), List.of(cel), List.of(), 1f); }
    catch (Throwable t) { plugin.getLogger().warning("strzal " + skill + ": " + t); }
  }

  private void wskazCel(Player wlasciciel, LivingEntity cel) {
    Deque<Sluga> lista = sludzy.get(wlasciciel.getUniqueId());
    if (lista == null) return;
    for (Sluga s : lista) {
      if (s.byt() instanceof Mob sluga && sluga.isValid() && !sluga.isDead()
          && celWZasiegu(sluga, cel)) oczekiwaneCele.put(sluga.getUniqueId(), cel);
    }
  }

  private boolean dozwolonyCel(Entity sluga, Entity cel) {
    if (sprawdzanieCelu) return false;
    String id = sluga.getPersistentDataContainer().get(kWlasciciel, PersistentDataType.STRING);
    Player wlasciciel;
    try { wlasciciel = id == null ? null : Bukkit.getPlayer(UUID.fromString(id)); }
    catch (IllegalArgumentException ex) { return false; }
    if (wlasciciel == null || !cel.isValid() || cel.isDead() || cel == wlasciciel
        || !(cel instanceof LivingEntity zywy)
        || chowance(cel) || cel.getPersistentDataContainer().has(kWlasciciel, PersistentDataType.STRING)) return false;
    if (!(cel instanceof org.bukkit.entity.Enemy) && !(cel instanceof Player)) return false;
    // canTarget wywoluje sztuczne zdarzenie obrazen; ponowne wejscie nie moze uruchomic kolejnej oceny.
    sprawdzanieCelu = true;
    try { return MythicLib.inst().getEntities().canTarget(wlasciciel, zywy, InteractionType.OFFENSE_ACTION); }
    finally { sprawdzanieCelu = false; }
  }

  void wylacz() {
    for (Player p : Bukkit.getOnlinePlayers()) { zdejmij(p, true); zdejmij(p, false); }
    for (UUID u : List.copyOf(sludzy.keySet())) usunSlugi(u);
    for (UUID u : List.copyOf(spokoj.keySet())) {
      Entity e = Bukkit.getEntity(u); if (e instanceof Mob m && m.isValid()) m.setAware(true);
    }
    spokoj.clear();
  }

  @EventHandler public void wejscie(PlayerJoinEvent e) { sprzatajPoRestarcie(e.getPlayer()); }
  @EventHandler public void wyjscie(PlayerQuitEvent e) {
    zdejmij(e.getPlayer(), true); zdejmij(e.getPlayer(), false); usunSlugi(e.getPlayer().getUniqueId());
  }
  @EventHandler public void smierc(PlayerDeathEvent e) {
    e.getDrops().removeIf(this::oznaczona);
    Map<Boolean, Bron> stare = bronie.get(e.getEntity().getUniqueId());
    if (stare != null && !e.getKeepInventory()) poSmierci.put(e.getEntity().getUniqueId(), new HashMap<>(stare));
    zdejmij(e.getEntity(), true); zdejmij(e.getEntity(), false); usunSlugi(e.getEntity().getUniqueId());
  }
  @EventHandler public void odrodzenie(PlayerRespawnEvent e) {
    Map<Boolean, Bron> stare = poSmierci.remove(e.getPlayer().getUniqueId());
    if (stare == null) return;
    Bukkit.getScheduler().runTask(plugin, () -> {
      Player p = e.getPlayer();
      for (Bron b : stare.values()) {
        ItemStack obecny = p.getInventory().getItem(b.slot());
        if (obecny == null || obecny.getType().isAir()) p.getInventory().setItem(b.slot(), b.nosnik());
        else p.getInventory().addItem(b.nosnik());
      }
    });
  }
  @EventHandler public void swiat(PlayerChangedWorldEvent e) { usunSlugi(e.getPlayer().getUniqueId()); }
  @EventHandler(priority = EventPriority.LOWEST) public void klik(InventoryClickEvent e) {
    if (oznaczona(e.getCurrentItem()) || oznaczona(e.getCursor()) ||
        (e.getHotbarButton() >= 0 && oznaczona(e.getWhoClicked().getInventory().getItem(e.getHotbarButton()))) ||
        (e.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND &&
            oznaczona(e.getWhoClicked().getInventory().getItemInOffHand())))
      e.setCancelled(true);
  }
  @EventHandler(priority = EventPriority.LOWEST) public void przeciagnij(InventoryDragEvent e) {
    if (oznaczona(e.getOldCursor())) e.setCancelled(true);
  }
  @EventHandler(priority = EventPriority.LOWEST) public void wyrzuc(PlayerDropItemEvent e) {
    if (oznaczona(e.getItemDrop().getItemStack())) { e.setCancelled(true); Bukkit.getScheduler().runTask(plugin, e.getPlayer()::updateInventory); }
  }
  @EventHandler(priority = EventPriority.LOWEST) public void zamien(PlayerSwapHandItemsEvent e) {
    if (oznaczona(e.getMainHandItem()) || oznaczona(e.getOffHandItem())) e.setCancelled(true);
  }
  /** Jak w Skyrimie: odlozenie przywolanej broni (zmiana slotu) ja odwoluje — wraca zaklecie; pasek nie jest blokowany. */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void schowaj(PlayerItemHeldEvent e) {
    if (oznaczona(e.getPlayer().getInventory().getItem(e.getPreviousSlot()))) zdejmij(e.getPlayer(), true);
  }
  @EventHandler(priority = EventPriority.LOWEST) public void istota(PlayerInteractEntityEvent e) {
    ItemStack i = e.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND
        ? e.getPlayer().getInventory().getItemInOffHand() : e.getPlayer().getInventory().getItemInMainHand();
    if (oznaczona(i)) e.setCancelled(true);
  }
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void obrazenia(EntityDamageByEntityEvent e) {
    if (e instanceof FakeEntityDamageByEntityEvent || sprawdzanieCelu) return;
    Entity sprawca = e.getDamager();
    if (sprawca instanceof org.bukkit.entity.Projectile pocisk && pocisk.getShooter() instanceof Entity byt) sprawca = byt;
    if (spokoj.getOrDefault(sprawca.getUniqueId(), 0L) > tick) { e.setCancelled(true); return; }
    if (!sprawca.getPersistentDataContainer().has(kWlasciciel, PersistentDataType.STRING)) return;
    if (!dozwolonyCel(sprawca, e.getEntity())) e.setCancelled(true);
  }
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void pomocSlug(EntityDamageByEntityEvent e) {
    if (e instanceof FakeEntityDamageByEntityEvent || sprawdzanieCelu) return;
    Entity sprawca = e.getDamager();
    if (sprawca instanceof org.bukkit.entity.Projectile pocisk && pocisk.getShooter() instanceof Entity byt) sprawca = byt;
    if (sprawca instanceof Player p && e.getEntity() instanceof LivingEntity cel) wskazCel(p, cel);
    if (e.getEntity() instanceof Player p && sprawca instanceof LivingEntity cel) wskazCel(p, cel);
  }
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void celowanie(EntityTargetLivingEntityEvent e) {
    if (spokoj.getOrDefault(e.getEntity().getUniqueId(), 0L) > tick) e.setCancelled(true);
  }
}
