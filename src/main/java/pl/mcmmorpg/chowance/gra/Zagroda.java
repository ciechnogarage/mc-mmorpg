package pl.mcmmorpg.chowance.gra;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.Ustawienia;
import pl.mcmmorpg.chowance.dane.ProfilDane;
import pl.mcmmorpg.chowance.integracja.Mythic;
import pl.mcmmorpg.chowance.integracja.SkalaModelu;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Stat;
import pl.mcmmorpg.chowance.model.Typ;

/**
 * Zagroda młodych w Ogrodzie Chowańców: pierwszy chowaniec nie wyskakuje oknem po wejściu, tylko gracz odbiera
 * od Strażniczki Pieczęć Opiekuna, ogląda młode na stanowiskach i wiąże wybrane (zgłoszenie właściciela 18.10:
 * „z bomby po wejściu masz”). Młode są nieprzetrwałe i stawiane od nowa, gdy chunk stanowiska jest wczytany.
 */
public final class Zagroda implements Listener {
  private final Chowance c;
  private final Ciala ciala;
  private final Startery startery;
  private final NamespacedKey kMlode, kPieczecOpiekuna;
  /** indeks stanowiska → ciało młodego i jego napis */
  private final Map<Integer, LivingEntity> mlode = new ConcurrentHashMap<>();
  private final Map<Integer, TextDisplay> napisy = new ConcurrentHashMap<>();
  /** młode w trakcie wiązania (schowane do pieczęci) */
  private final java.util.Set<UUID> zajete = ConcurrentHashMap.newKeySet();
  private final Map<UUID, Long> ostatniKlik = new ConcurrentHashMap<>();
  private final java.util.Set<UUID> powiadomieni = ConcurrentHashMap.newKeySet();

  public Zagroda(Chowance c, Ciala ciala, Startery startery) {
    this.c = c;
    this.ciala = ciala;
    this.startery = startery;
    this.kMlode = new NamespacedKey(c.plugin, "mlode");
    this.kPieczecOpiekuna = new NamespacedKey(c.plugin, "pieczec_opiekuna");
  }

  private boolean wlaczona() { return c.ust.zagrodaWlaczona && !c.ust.zagrodaMiejsca.isEmpty(); }

  // ------------------------------------------------------------------ stanowiska

  /** Co 5 s: brakujące młode na wczytanych stanowiskach; młode z innej konfiguracji znikają. */
  public void pilnuj() {
    if (!wlaczona()) { usunWszystkie(); return; }
    World w = Bukkit.getWorld(c.ust.zagrodaSwiat);
    if (w == null) return;
    List<Ustawienia.Stanowisko> st = c.ust.zagrodaMiejsca;
    for (int i = 0; i < st.size(); i++) {
      Ustawienia.Stanowisko s = st.get(i);
      Location l = new Location(w, s.x(), s.y(), s.z(), s.yaw(), 0);
      if (!w.isChunkLoaded(l.getBlockX() >> 4, l.getBlockZ() >> 4)) continue;
      LivingEntity le = mlode.get(i);
      if (le != null && le.isValid()) continue;
      postaw(i, s, l);
    }
    for (Integer i : List.copyOf(mlode.keySet())) if (i >= st.size()) usun(i);
    opisStrazniczki(w, st.get(st.size() / 2));
  }

  /** Napis pod Strażniczką: „walka próbna” myliło gracza bez chowańca, który ma z nią porozmawiać. */
  private void opisStrazniczki(World w, Ustawienia.Stanowisko s) {
    Location l = new Location(w, s.x(), s.y(), s.z());
    if (!w.isChunkLoaded(l.getBlockX() >> 4, l.getBlockZ() >> 4)) return;
    NamespacedKey k = new NamespacedKey(c.plugin, "npc_trener");
    for (Entity e : w.getNearbyEntities(l, 16, 6, 16)) {
      if (e instanceof org.bukkit.entity.Mannequin m && c.ust.zagrodaTrener.equals(m.getPersistentDataContainer().get(k, PersistentDataType.STRING))
          && !"PPM — porozmawiaj".equals(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(
              m.getDescription() == null ? net.kyori.adventure.text.Component.empty() : m.getDescription()))) {
        m.setDescription(Chowance.mm("<gray>PPM — porozmawiaj"));
      }
    }
  }

  private void postaw(int i, Ustawienia.Stanowisko s, Location l) {
    usun(i);
    Gatunek g = c.rej.gatunek(s.gatunek());
    if (g == null) return;
    Ciala.Cialo cialo = ciala.postaw(g, g.mobOswojony(false), l, c.rej.startery().poziom());
    if (cialo == null) return;
    LivingEntity le = cialo.encja();
    le.setAI(false);
    le.setInvulnerable(true);
    le.setSilent(true);
    le.setGravity(false);
    le.setCustomNameVisible(false);
    le.setRotation(s.yaw(), 0);
    le.getPersistentDataContainer().set(kMlode, PersistentDataType.STRING, g.id());
    mlode.put(i, le);
    TextDisplay t = l.getWorld().spawn(l.clone().add(0, s.napis(), 0), TextDisplay.class, x -> {
      x.setPersistent(false);
      x.setBillboard(Display.Billboard.CENTER);
      x.setShadowed(true);
      x.setBackgroundColor(org.bukkit.Color.fromARGB(0x70000000));
      x.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(0.7f, 0.7f, 0.7f), new AxisAngle4f()));
      x.text(Chowance.mm(etykieta(g)));
    });
    napisy.put(i, t);
  }

  private void usun(int i) {
    LivingEntity le = mlode.remove(i);
    if (le != null) Mythic.usun(le);
    TextDisplay t = napisy.remove(i);
    if (t != null) t.remove();
  }

  public void usunWszystkie() {
    for (Integer i : List.copyOf(mlode.keySet())) usun(i);
    for (Integer i : List.copyOf(napisy.keySet())) usun(i);
  }

  /** Dwie linie nad młodym: imię, żywioł · rola; młode „ludowe” z dopiskiem rasy. */
  private String etykieta(Gatunek g) {
    StringBuilder typy = new StringBuilder();
    for (Typ t : g.typy()) typy.append(typy.isEmpty() ? "" : "<gray>/").append(t.kolor()).append(t.nazwa());
    String lud = lud(g);
    return "<white><b>" + g.nazwa() + "</b>\n" + typy + " <#8C8C8C>· <#E6E6E6>" + rola(g)
        + (lud == null ? "" : "\n<#C9A86A>młode " + lud);
  }

  /** Młode spoza wspólnej oferty: zwraca „orków”/„nieumarłych” itd., null dla wspólnych. */
  private String lud(Gatunek g) { return lud(g, false); }

  /** celownik=true: „orkom” (ufa tylko …), inaczej dopełniacz: „orków” (młode …). */
  private String lud(Gatunek g, boolean celownik) {
    var st = c.rej.startery();
    if (st.domyslne().contains(g.id())) return null;
    List<String> rasy = new ArrayList<>();
    for (var e : st.rasy().entrySet()) if (e.getValue().contains(g.id())) rasy.add(nazwaLudu(e.getKey(), celownik));
    return rasy.isEmpty() ? null : String.join(" i ", rasy);
  }

  private static String nazwaLudu(String rasa, boolean celownik) {
    return switch (rasa) {
      case "czlowiek" -> celownik ? "ludziom" : "ludzi";
      case "elf" -> celownik ? "elfom" : "elfów";
      case "ork" -> celownik ? "orkom" : "orków";
      case "nieumarly" -> celownik ? "nieumarłym" : "nieumarłych";
      default -> rasa;
    };
  }

  static String rola(Gatunek g) {
    int obrona = g.bazowy(Stat.OBRONA) + g.bazowy(Stat.OBRONA_SPEC);
    int atak = g.bazowy(Stat.ATAK) + g.bazowy(Stat.ATAK_SPEC);
    if (obrona >= atak + 12) return "obrońca";
    if (g.bazowy(Stat.ATAK_SPEC) >= g.bazowy(Stat.ATAK) + 12) return "mag";
    if (g.bazowy(Stat.SZYBKOSC) >= 60 && g.bazowy(Stat.ATAK) > g.bazowy(Stat.ATAK_SPEC)) return "szybki";
    if (atak >= obrona + 12) return "napastnik";
    return "wszechstronny";
  }

  /** Co 5 ticków: młode odwracają głowę do najbliższego gracza (bez AI MythicMobs). */
  public void patrz() {
    for (LivingEntity le : mlode.values()) {
      if (!le.isValid() || zajete.contains(le.getUniqueId())) continue;
      Player best = null;
      double bd = 64;
      for (Player p : le.getWorld().getPlayers()) {
        double d = p.getLocation().distanceSquared(le.getLocation());
        if (d < bd) { bd = d; best = p; }
      }
      if (best == null) continue;
      Location od = le.getLocation();
      double dx = best.getX() - od.getX(), dz = best.getZ() - od.getZ();
      float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
      le.setRotation(yaw, 0);
      le.setBodyYaw(yaw);
    }
  }

  public boolean mlode(Entity e) {
    return e != null && e.getPersistentDataContainer().has(kMlode, PersistentDataType.STRING);
  }

  // ------------------------------------------------------------------ pieczęć

  public ItemStack pieczec() {
    ItemStack it = new ItemStack(org.bukkit.Material.PAPER);
    it.editMeta(m -> {
      m.itemName(Chowance.mm("<gold>Pieczęć Opiekuna"));
      m.lore(List.of(
          Chowance.mm("<!i><gray>Dar Strażniczki Ogrodu."),
          Chowance.mm("<!i><gray>Kliknij nią młode w zagrodzie,"),
          Chowance.mm("<!i><gray>by zostało twoim chowańcem."),
          Chowance.mm("<!i><dark_gray>Chowańce")));
      NamespacedKey model = NamespacedKey.fromString(c.ust.zagrodaModelPieczeci);
      if (model != null) m.setItemModel(model);
      m.getPersistentDataContainer().set(kPieczecOpiekuna, PersistentDataType.BYTE, (byte) 1);
    });
    return it;
  }

  public boolean pieczec(ItemStack it) {
    return it != null && it.hasItemMeta() && it.getItemMeta().getPersistentDataContainer().has(kPieczecOpiekuna, PersistentDataType.BYTE);
  }

  private boolean maPieczec(Player p) {
    for (ItemStack it : p.getInventory().getContents()) if (pieczec(it)) return true;
    return pieczec(p.getItemOnCursor());
  }

  private void zabierzPieczecie(Player p) {
    ItemStack[] zaw = p.getInventory().getContents();
    for (int i = 0; i < zaw.length; i++) if (pieczec(zaw[i])) p.getInventory().setItem(i, null);
  }

  // ------------------------------------------------------------------ Strażniczka

  /** PPM w Strażniczkę bez pierwszego chowańca. true = obsłużone (zamiast okna walki). */
  public boolean rozmowa(Player p, String trenerId) {
    if (!wlaczona()) return false;
    ProfilDane d = c.dane(p);
    if (d == null || d.starter) return false;
    if (!trenerId.equals(c.ust.zagrodaTrener)) {
      c.msg(p, "<gray>Najpierw zdobądź chowańca — <white>Strażniczka Ogrodu</white> w Stolicy powierza młode nowym opiekunom.");
      return true;
    }
    String kto = "<#9BD770>Strażniczka Ogrodu<gray>: <white>";
    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 0.7f, 1.3f);
    if (maPieczec(p)) {
      p.sendMessage(Chowance.mm(kto + "Pieczęć już masz. Podejdź do zagrody i kliknij malucha, którego wybierzesz."));
      c.pasek(p, "<gold>Kliknij wybrane młode <gray>(Pieczęć Opiekuna masz przy sobie)");
      return true;
    }
    p.sendMessage(Chowance.mm(kto + "Nowa twarz! Te młode w zagrodzie szukają opiekunów."));
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      if (!p.isOnline()) return;
      p.sendMessage(Chowance.mm(kto + "Każde walczy inaczej — przyjrzyj się im, kliknij, a opowiem ci o nim."));
    }, 30L);
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      if (!p.isOnline()) return;
      ProfilDane teraz = c.dane(p);
      if (teraz == null || teraz.starter || maPieczec(p)) return;
      p.sendMessage(Chowance.mm(kto + "Masz, to Pieczęć Opiekuna. Kliknij to młode, które wybierzesz — pieczęć zwiąże was. Tylko jedno, więź jest na zawsze."));
      var reszta = p.getInventory().addItem(pieczec());
      for (ItemStack r : reszta.values()) p.getWorld().dropItemNaturally(p.getLocation(), r);
      p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.0f);
      c.pasek(p, "<gold>Otrzymano: Pieczęć Opiekuna <gray>· kliknij wybrane młode");
    }, 60L);
    return true;
  }

  // ------------------------------------------------------------------ kliknięcie młodego

  /** PPM/LPM w młode (Bukkit albo most ModelEngine). true = kliknięcie było w młode. */
  public boolean klik(Player p, LivingEntity le) {
    if (!mlode(le)) return false;
    long teraz = System.currentTimeMillis();
    Long ost = ostatniKlik.put(p.getUniqueId(), teraz);
    if (ost != null && teraz - ost < 400) return true; // LPM w model MEG przychodzi dwa razy
    String gid = le.getPersistentDataContainer().get(kMlode, PersistentDataType.STRING);
    Gatunek g = gid == null ? null : c.rej.gatunek(gid);
    ProfilDane d = c.daneLubBlad(p);
    if (g == null || d == null) return true;
    if (zajete.contains(le.getUniqueId())) return true;
    if (d.starter) {
      le.getWorld().spawnParticle(Particle.HEART, le.getLocation().add(0, 0.6, 0), 2, 0.2, 0.2, 0.2, 0);
      c.pasek(p, "<gray>" + g.nazwa() + " czeka na nowego opiekuna — twój chowaniec już jest z tobą.");
      return true;
    }
    if (startery.oferta(p).stream().noneMatch(x -> x.id().equals(g.id()))) {
      String lud = lud(g, true);
      opis(p, g);
      c.pasek(p, "<red>" + g.nazwa() + " ufa tylko " + (lud == null ? "innym" : lud) + ".");
      return true;
    }
    // Pieczęć wystarczy mieć przy sobie: reguły rąk Księgi zdejmują zwykły przedmiot z dłoni (klient QA 18.10).
    if (!maPieczec(p)) {
      opis(p, g);
      c.pasek(p, "<gold>Pieczęć Opiekuna da ci Strażniczka Ogrodu <gray>(PPM w nią)");
      return true;
    }
    wiaz(p, le, g);
    return true;
  }

  /** Krótko o młodym: imię i rola w tytule, opis i ruchy na czacie. */
  private void opis(Player p, Gatunek g) {
    StringBuilder typy = new StringBuilder();
    for (Typ t : g.typy()) typy.append(typy.isEmpty() ? "" : "<gray>/").append(t.kolor()).append(t.nazwa());
    List<String> ruchy = new ArrayList<>();
    for (String id : g.ruchyDo(c.rej.startery().poziom())) {
      var r = c.rej.ruch(id);
      if (r != null) ruchy.add(r.nazwa());
    }
    p.sendMessage(Chowance.mm("<gold>" + g.nazwa() + " <dark_gray>· " + typy + " <dark_gray>· <gray>" + rola(g)));
    if (!g.opis().isEmpty()) p.sendMessage(Chowance.mm("<gray><i>" + String.join(" ", g.opis())));
    if (!ruchy.isEmpty()) p.sendMessage(Chowance.mm("<gray>Na start umie: <white>" + String.join(", ", ruchy)));
    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.6f);
  }

  /** Wiązanie młodego: zawsze udane (to nauka pieczęci, nie próba), model kurczy się w pieczęć. */
  private void wiaz(Player p, LivingEntity le, Gatunek g) {
    zabierzPieczecie(p);
    UUID id = le.getUniqueId();
    zajete.add(id);
    p.swingMainHand();
    Location l = le.getLocation();
    Location srodek = l.clone().add(0, Math.min(1.0, le.getHeight() * 0.5), 0);
    boolean meg = SkalaModelu.maModel(le);
    double skala0 = meg ? SkalaModelu.skala(le) : 1.0;
    TextDisplay napis = napisDla(le);
    if (napis != null) napis.setViewRange(0f);
    l.getWorld().playSound(l, Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.8f);
    l.getWorld().playSound(l, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.2f);
    int kroki = 10;
    for (int i = 1; i <= kroki; i++) {
      int nr = i;
      Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
        if (!le.isValid()) return;
        double f = 1.0 - (double) nr / kroki;
        if (meg) SkalaModelu.ustaw(le, Math.max(0.02, skala0 * f));
        double r = 0.3 + 0.9 * f;
        for (int k = 0; k < 6; k++) {
          double a = nr * 0.9 + k * Math.PI / 3;
          srodek.getWorld().spawnParticle(Particle.END_ROD, srodek.clone().add(Math.cos(a) * r, (k % 2 == 0 ? 0.3 : -0.2) * f, Math.sin(a) * r), 0,
              -Math.cos(a) * 0.08, 0, -Math.sin(a) * 0.08, 1);
        }
        if (nr == kroki) le.setInvisible(true);
      }, i);
    }
    p.showTitle(net.kyori.adventure.title.Title.title(Chowance.mm(""), Chowance.mm("<gold>Pieczęć wiąże: <white>" + g.nazwa()),
        net.kyori.adventure.title.Title.Times.times(java.time.Duration.ZERO, java.time.Duration.ofMillis(1500), java.time.Duration.ofMillis(250))));
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      l.getWorld().playSound(l, Sound.BLOCK_CHAIN_HIT, 0.9f, 1.0f);
      l.getWorld().spawnParticle(Particle.CRIT, l.clone().add(0, 0.5, 0), 6, 0.15, 0.1, 0.15, 0.05);
    }, 20L);
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      if (p.isOnline()) {
        ProfilDane d = c.dane(p);
        if (d != null && !d.starter) {
          startery.wybierz(p, g.id());
          if (d.starter) {
            l.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, l.clone().add(0, 0.8, 0), 40, 0.4, 0.5, 0.4, 0.2);
            l.getWorld().playSound(l, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.3f);
            p.showTitle(net.kyori.adventure.title.Title.title(Chowance.mm("<green><b>Związano!"), Chowance.mm("<white>" + g.nazwa() + " <gray>jest teraz twoim chowańcem"),
                net.kyori.adventure.title.Title.Times.times(java.time.Duration.ZERO, java.time.Duration.ofMillis(3000), java.time.Duration.ofMillis(500))));
          }
        }
        // nieudane wybierz (np. brak miejsca): pieczęć wraca
        if (d != null && !d.starter) p.getInventory().addItem(pieczec());
      }
    }, 40L);
    // Na stanowisko wychodzi „kolejny maluch” — to samo ciało, przywrócone.
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      zajete.remove(id);
      if (!le.isValid()) return;
      if (meg) SkalaModelu.ustaw(le, skala0);
      le.setInvisible(false);
      if (napis != null && napis.isValid()) napis.setViewRange(1f);
      le.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, le.getLocation().add(0, 0.5, 0), 12, 0.4, 0.3, 0.4, 0);
      le.getWorld().playSound(le.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.6f, 1.2f);
    }, c.ust.zagrodaOdrodzenieTicki);
  }

  private TextDisplay napisDla(LivingEntity le) {
    for (var e : mlode.entrySet()) if (e.getValue().equals(le)) return napisy.get(e.getKey());
    return null;
  }

  // ------------------------------------------------------------------ droga

  /** Gracz bez chowańca: przypomnienie po wejściu (raz na sesję) — bez okna. */
  public void poWczytaniu(Player p, ProfilDane d) {
    if (!wlaczona() || d.starter || !powiadomieni.add(p.getUniqueId())) return;
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      ProfilDane teraz = c.dane(p);
      if (!p.isOnline() || teraz != d || d.starter) return;
      c.msg(p, "<white>" + c.ust.zagrodaWskazowka);
    }, c.ust.starterOpoznienie);
  }

  public void zapomnij(UUID gracz) { powiadomieni.remove(gracz); ostatniKlik.remove(gracz); }

  /** /chw starter i przycisk w Menażerii: gdzie jest zagroda. */
  public void wskaz(Player p) {
    ProfilDane d = c.daneLubBlad(p);
    if (d == null) return;
    if (d.starter) { c.blad(p, "Ta postać ma już swojego pierwszego chowańca."); return; }
    if (!wlaczona()) { c.blad(p, "Zagroda młodych jest zamknięta — zgłoś administracji."); return; }
    World w = Bukkit.getWorld(c.ust.zagrodaSwiat);
    Ustawienia.Stanowisko s = c.ust.zagrodaMiejsca.get(c.ust.zagrodaMiejsca.size() / 2);
    if (w == null || !p.getWorld().equals(w)) {
      c.msg(p, "<white>" + c.ust.zagrodaWskazowka);
      return;
    }
    double dx = s.x() - p.getX(), dz = s.z() - p.getZ();
    int dyst = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
    c.msg(p, "<white>Strażniczka Ogrodu powierza młode chowańce nowym opiekunom — zagroda jest <gold>" + dyst + " bl. " + kierunek(dx, dz) + "</gold>.");
    c.pasek(p, "<gold>Zagroda młodych: <white>" + dyst + " bl. " + kierunek(dx, dz));
  }

  private static String kierunek(double dx, double dz) {
    // Minecraft: −Z = północ, +X = wschód
    double kat = (Math.toDegrees(Math.atan2(dx, -dz)) + 360) % 360;
    String[] n = {"na północ", "na północny wschód", "na wschód", "na południowy wschód", "na południe", "na południowy zachód", "na zachód", "na północny zachód"};
    return n[(int) Math.round(kat / 45) % 8];
  }

  // ------------------------------------------------------------------ zdarzenia

  @EventHandler(priority = EventPriority.LOWEST)
  public void ppm(PlayerInteractEntityEvent e) {
    if (e.getHand() != EquipmentSlot.HAND || !(e.getRightClicked() instanceof LivingEntity le) || !mlode(le)) return;
    e.setCancelled(true);
    klik(e.getPlayer(), le);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void uderzenie(EntityDamageByEntityEvent e) {
    if (!mlode(e.getEntity())) return;
    e.setCancelled(true);
    if (e.getDamager() instanceof Player p && e.getEntity() instanceof LivingEntity le) klik(p, le);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void obrazenia(EntityDamageEvent e) {
    if (mlode(e.getEntity())) e.setCancelled(true);
  }

  /** PPM pieczęcią w powietrze: podpowiedź zamiast ciszy. */
  @EventHandler(priority = EventPriority.HIGH)
  public void pieczecWPowietrze(PlayerInteractEvent e) {
    if (e.getHand() != EquipmentSlot.HAND || (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
    if (!pieczec(e.getItem())) return;
    e.setCancelled(true);
    c.pasek(e.getPlayer(), "<gold>Podejdź do zagrody i kliknij wybrane młode");
  }
}
