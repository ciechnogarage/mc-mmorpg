package pl.mcmmorpg.chowance.gra;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.util.Vector;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.api.event.WalkaZakonczonaEvent;
import pl.mcmmorpg.chowance.dane.ProfilDane;
import pl.mcmmorpg.chowance.integracja.Mythic;
import pl.mcmmorpg.chowance.logika.SilnikWalki;
import pl.mcmmorpg.chowance.model.Osobnik;
import pl.mcmmorpg.chowance.logika.Wojownik;
import pl.mcmmorpg.chowance.model.EfektRuchu;
import pl.mcmmorpg.chowance.model.Kategoria;
import pl.mcmmorpg.chowance.model.RodzajCechy;
import pl.mcmmorpg.chowance.model.Ruch;
import pl.mcmmorpg.chowance.model.Status;

/**
 * Pojedynek areny w czasie rzeczywistym, sterowany (SPEC §6.2): stwory biegają, ale obrażenia zadają
 * WYŁĄCZNIE ruchy wybrane przez graczy — klawisze hotbara 1–4 (PlayerItemHeldEvent, anulowany, więc
 * ekwipunek nietknięty) albo /chw ruch &lt;1-4&gt; z menu. Odnowienie ruchu = pole `odnowienie`.
 * Rankingowy (ELO). Jedno zadanie co 20 ticków na wszystkie pojedynki (statusy, czas, zasięg).
 */
public final class Pojedynki implements Listener {
  private final Chowance c;
  private final Druzyny druzyny;
  private final Przywolania przywolania;
  private final Ranking ranking;
  private final Map<UUID, Pojedynek> wgGracza = new ConcurrentHashMap<>();
  /** Widok areny i odgrywanie trafień (ustawiane przez plugin). */
  public Kamera kamera;
  public Scena scena;
  public HudWalki hud;
  /** do ciała przeciwnika-NPC (ustawiane przez plugin) */
  public Ciala ciala;

  public Pojedynki(Chowance c, Druzyny druzyny, Przywolania przywolania, Ranking ranking) {
    this.c = c; this.druzyny = druzyny; this.przywolania = przywolania; this.ranking = ranking;
  }

  final class Pojedynek {
    final Player[] gracze = new Player[2];
    final Wojownik[] w = new Wojownik[2];
    final LivingEntity[] ciala = new LivingEntity[2];
    final Location[] powrot = new Location[2];
    final Map<String, Long>[] odnowienia = new Map[] {new HashMap<String, Long>(), new HashMap<String, Long>()};
    final long[] statusDo = new long[2];
    /** ostatni ruch strony (auto: odstęp między ruchami) */
    final long[] ostatniRuch = new long[2];
    /** wygładzona pozycja kamery strony */
    final Location[] kam = new Location[2];
    /** oś areny ustalona na starcie (kamera nie kręci się razem z biegającymi stworami) */
    Vector os;
    /** Kamera: ostatni obrót ustawiony przez nas i chwila ostatniego obrotu gracza (auto-celowanie tylko w spoczynku). */
    final float[][] obrot = new float[2][];
    final long[] recznyObrot = new long[2];
    /** Ostatni cel pathfindera (ścieżka liczona od nowa tylko, gdy cel się przesunął). */
    final Location[] celDrogi = new Location[2];
    /** rozkaz trenera: indeks ruchu (-1 = brak) i termin ważności */
    final int[] rozkaz = {-1, -1};
    final long[] rozkazDo = new long[2];
    /** unik: aktywny do / odnowienie do; eliksir: odnowienie do; podwójne [8] */
    final long[] unikDo = new long[2], unikOd = new long[2], eliksirOd = new long[2], klikWyjscia = new long[2];
    long koniec;
    boolean skonczony;
    /** KO: chwila na napis zwycięzcy, zanim kamera wróci do gracza */
    boolean konczy;
    long startCiosow;
    int tik;
    /** strona 1 to trener NPC (bez gracza): jego ciało stawiamy i usuwamy sami */
    String npcNazwa;
    /** slot w ręce przed pojedynkiem (przywracany na końcu) */
    final int[] slotPrzed = {-1, -1};
    /** ruch w zapowiedzi (zamach widoczny dla przeciwnika = okno uniku); null = brak */
    final Zapowiedz[] zap = new Zapowiedz[2];
    /** po udanym uniku: następny ruch z premią kontry do tej chwili */
    final long[] kontraDo = new long[2];
    /** doskok w toku: ciało prowadzi animacja, nie sterowanie ruchem */
    final long[] blokadaDo = new long[2];
    /** kierunek krążenia (bok) i chwila ostatniej zmiany */
    final int[] bok = {1, -1};
    final long[] bokOd = new long[2];
    /** drużyna trenera NPC (real-time: po KO wchodzi następny) */
    List<Wojownik> npcDruzyna = List.of();
    /** zmiana stwora klawiszem [7]: odnowienie do */
    final long[] zmianaOd = new long[2];
    /** zamach w toku — obrońca widzi „TERAZ!” (ostatnia chwila na unik) */
    final boolean[] teraz = new boolean[2];
    /** środek areny z chwili startu — ciała nie odchodzą od niego dalej niż PROMIEN_ARENY */
    Location srodekAreny;
  }

  static final double PROMIEN_ARENY = 5.0, ODSTEP_OD_TRENERA = 5.0;

  /**
   * Punkt ruchu ciała ograniczony do areny: nie dalej niż PROMIEN_ARENY od środka i nie bliżej trenerów niż
   * ODSTEP_OD_TRENERA. Klient QA 2026-09-29: cofający się stwór wchodził pod kamerę trenera i zasłaniał cały ekran.
   */
  private Location wArenie(Pojedynek pj, Location cel) {
    Location l = cel.clone();
    if (pj.srodekAreny != null && pj.srodekAreny.getWorld().equals(l.getWorld())) {
      Vector od = l.toVector().subtract(pj.srodekAreny.toVector()).setY(0);
      if (od.length() > PROMIEN_ARENY) l = pj.srodekAreny.clone().add(od.normalize().multiply(PROMIEN_ARENY)).add(0, l.getY() - pj.srodekAreny.getY(), 0);
    }
    for (Player p : pj.gracze) {
      if (p == null || !p.getWorld().equals(l.getWorld())) continue;
      Vector od = l.toVector().subtract(p.getLocation().toVector()).setY(0);
      if (od.length() < ODSTEP_OD_TRENERA) {
        if (od.lengthSquared() < 1e-4 && pj.srodekAreny != null) od = pj.srodekAreny.toVector().subtract(p.getLocation().toVector()).setY(0);
        if (od.lengthSquared() < 1e-4) od = new Vector(1, 0, 0);
        l = p.getLocation().clone().add(od.normalize().multiply(ODSTEP_OD_TRENERA)).add(0, l.getY() - p.getLocation().getY(), 0);
      }
    }
    // Trzymamy poziom areny: na stoku pathfinder sprowadzał stwory w dół, poza kadr (klient QA 2026-09-30).
    if (pj.srodekAreny != null && pj.srodekAreny.getWorld().equals(l.getWorld())) {
      Location wys = l.clone();
      wys.setY(pj.srodekAreny.getY());
      Location g = Arena.grunt(wys, 2, 2);
      if (g == null) g = Arena.grunt(pj.srodekAreny, 2, 2);
      if (g != null) l = g;
    }
    return l;
  }

  /** Stwór spadł/zbiegł poniżej areny (odrzut, stok) — wraca na jej poziom obok środka. */
  private void naArene(Pojedynek pj, LivingEntity e) {
    if (pj.srodekAreny == null || e == null || !e.isValid() || !e.getWorld().equals(pj.srodekAreny.getWorld())) return;
    Location l = e.getLocation();
    double dy = Math.abs(l.getY() - pj.srodekAreny.getY());
    double dxz = l.clone().subtract(pj.srodekAreny).toVector().setY(0).length();
    if (dy <= 3 && dxz <= PROMIEN_ARENY + 3) return;
    Location g = wArenie(pj, l);
    if (e instanceof Mob m) m.getPathfinder().stopPathfinding();
    g.setDirection(l.getDirection());
    e.teleport(g);
  }

  /** Zapowiedziany ruch strony: trafi po {@code ticki} (zamach), wtedy dopiero liczy się unik obrońcy. */
  static final class Zapowiedz {
    final int idx; final Ruch r; final boolean rozkaz;
    Zapowiedz(int idx, Ruch r, boolean rozkaz) { this.idx = idx; this.r = r; this.rozkaz = rozkaz; }
  }

  public boolean wPojedynku(Player p) { return wgGracza.containsKey(p.getUniqueId()); }

  /** Diagnostyka (admin): gdzie są ciała względem siebie, trenera i środka areny. */
  public String stan(Player p) {
    Pojedynek pj = wgGracza.get(p.getUniqueId());
    if (pj == null) return "brak pojedynku";
    StringBuilder sb = new StringBuilder();
    LivingEntity a = pj.ciala[0], b = pj.ciala[1];
    if (a != null && b != null && a.isValid() && b.isValid() && a.getWorld().equals(b.getWorld()))
      sb.append(String.format(java.util.Locale.ROOT, "odl=%.1f", a.getLocation().distance(b.getLocation())));
    for (int s = 0; s < 2; s++) {
      LivingEntity e = pj.ciala[s];
      if (e == null || !e.isValid()) { sb.append(" | s").append(s).append(" brak"); continue; }
      Location l = e.getLocation();
      sb.append(String.format(java.util.Locale.ROOT, " | s%d %s hp=%.0f/%d poz=%.1f,%.1f,%.1f", s, pj.w[s].nazwa(), pj.w[s].hp, pj.w[s].hpMax, l.getX(), l.getY(), l.getZ()));
      Player t = pj.gracze[s];
      if (t != null && t.getWorld().equals(e.getWorld())) sb.append(String.format(java.util.Locale.ROOT, " doTrenera=%.1f", t.getLocation().distance(l)));
      if (pj.srodekAreny != null && pj.srodekAreny.getWorld().equals(e.getWorld())) sb.append(String.format(java.util.Locale.ROOT, " odSrodka=%.1f", pj.srodekAreny.distance(l)));
    }
    return sb.toString();
  }

  public int aktywnych() { return (int) wgGracza.values().stream().distinct().count(); }

  public boolean cialoWPojedynku(Entity e) {
    for (Pojedynek p : wgGracza.values()) for (LivingEntity x : p.ciala) if (x != null && x.getUniqueId().equals(e.getUniqueId())) return true;
    return false;
  }

  public LivingEntity przeciwnikCiala(Entity e) {
    for (Pojedynek p : wgGracza.values()) {
      if (p.ciala[0] != null && p.ciala[0].getUniqueId().equals(e.getUniqueId())) return p.ciala[1];
      if (p.ciala[1] != null && p.ciala[1].getUniqueId().equals(e.getUniqueId())) return p.ciala[0];
    }
    return null;
  }

  public boolean start(Player a, Player b) {
    for (Player p : List.of(a, b)) {
      ProfilDane d = c.dane(p);
      if (d == null || wPojedynku(p)) { c.blad(a, p.getName() + " nie może teraz stanąć do pojedynku."); return false; }
      boolean sprawny = false;
      for (var o : d.druzyna) if (!druzyny.zemdlony(p, o)) sprawny = true;
      if (!sprawny) { c.blad(a, p.getName() + " nie ma sprawnego chowańca."); if (p != a) c.blad(p, "Nie masz sprawnego chowańca."); return false; }
    }
    List<Location> arena = arena();
    if (arena == null && (!a.getWorld().equals(b.getWorld()) || a.getLocation().distance(b.getLocation()) > c.ust.pojedynekZasieg)) {
      c.blad(a, "Przeciwnik jest za daleko."); return false;
    }
    Pojedynek pj = new Pojedynek();
    pj.gracze[0] = a; pj.gracze[1] = b;
    a.closeInventory(); b.closeInventory();
    for (int s = 0; s < 2; s++) {
      Player p = pj.gracze[s];
      pj.powrot[s] = p.getLocation().clone();
      if (arena != null) p.teleport(arena.get(s));
    }
    for (int s = 0; s < 2; s++) {
      Player p = pj.gracze[s];
      ProfilDane d = c.dane(p);
      var akt = przywolania.osobnikAktywny(p);
      var o = akt != null && !druzyny.zemdlony(p, akt) ? akt : d.druzyna.stream().filter(x -> !druzyny.zemdlony(p, x)).findFirst().orElse(null);
      if (akt == null || !akt.id.equals(o.id)) przywolania.przywolaj(p, o);
      Przywolania.Aktywny ak = przywolania.aktywny(p);
      if (ak == null) { c.blad(a, "Nie udało się przywołać stwora " + p.getName() + "."); cofnijTeleport(pj); return false; }
      pj.ciala[s] = ak.cialo;
      pj.w[s] = druzyny.wojownik(p, o);
    }
    pj.koniec = System.currentTimeMillis() + c.ust.pojedynekCzasS * 1000L;
    pj.startCiosow = System.currentTimeMillis() + c.ust.pojedynekWstepMs;
    przejmijCiala(pj);
    for (int s = 0; s < 2; s++) {
      Player p = pj.gracze[s];
      wgGracza.put(p.getUniqueId(), pj);
      pj.slotPrzed[s] = p.getInventory().getHeldItemSlot();
      p.getInventory().setHeldItemSlot(8);
      c.msg(p, "<gold>Pojedynek z " + pj.gracze[1 - s].getName() + "! " + (c.ust.pojedynekAuto
          ? "<yellow>Stwory walczą same.</yellow> <gray>Rozkazy: <white>1–4</white> ruch, <white>5</white> unik, <white>6</white> eliksir.</gray>"
          : "<yellow>Klawisze 1–4 = ruchy</yellow>") + " <gray>Czas: " + c.ust.pojedynekCzasS + " s.");
      pokazRuchy(pj, s);
    }
    for (int s = 0; s < 2; s++) {
      if (kamera == null) break;
      Location[] k = kameraCel(pj, s);
      pj.kam[s] = k[0];
      kamera.wlacz(pj.gracze[s], k[0], k[1]);
      odslon(pj);
      if (scena != null) scena.napis(pj.gracze[s], "<gold>Pojedynek! <gray>" + pj.gracze[0].getName() + " vs " + pj.gracze[1].getName(), 40);
    }
    return true;
  }

  private String nazwa(Pojedynek pj, int s) {
    return pj.gracze[s] != null ? pj.gracze[s].getName() : pj.npcNazwa == null ? "?" : pj.npcNazwa;
  }

  /**
   * Pojedynek real-time z trenerem NPC (pierwszy stwór jego drużyny). Strona NPC walczy sama (auto);
   * bez rankingu. Do prób i treningu — wcześniej real-time był tylko gracz–gracz.
   */
  public boolean startNpc(Player a, String nazwaNpc, Wojownik przeciwnik) { return startNpc(a, nazwaNpc, List.of(przeciwnik)); }

  public boolean startNpc(Player a, String nazwaNpc, List<Wojownik> druzynaNpc) {
    if (druzynaNpc.isEmpty()) return false;
    Wojownik przeciwnik = druzynaNpc.get(0);
    ProfilDane d = c.dane(a);
    if (d == null || wPojedynku(a)) { c.blad(a, "Nie możesz teraz stanąć do pojedynku."); return false; }
    var akt = przywolania.osobnikAktywny(a);
    var o = akt != null && !druzyny.zemdlony(a, akt) ? akt : d.druzyna.stream().filter(x -> !druzyny.zemdlony(a, x)).findFirst().orElse(null);
    if (o == null) { c.blad(a, "Nie masz sprawnego chowańca."); return false; }
    Pojedynek pj = new Pojedynek();
    pj.gracze[0] = a;
    pj.npcNazwa = nazwaNpc;
    a.closeInventory();
    pj.powrot[0] = a.getLocation().clone();
    if (akt == null || !akt.id.equals(o.id)) przywolania.przywolaj(a, o);
    Przywolania.Aktywny ak = przywolania.aktywny(a);
    if (ak == null) { c.blad(a, "Nie udało się przywołać stwora."); return false; }
    pj.ciala[0] = ak.cialo;
    pj.w[0] = druzyny.wojownik(a, o);
    Vector kier = a.getLocation().getDirection().setY(0);
    if (kier.lengthSquared() < 0.01) kier = new Vector(1, 0, 0);
    // najdalej 6 bl., ale na wolnym gruncie — w skale stwór NPC byłby niewidoczny (world1: kieszeń przy spawnie)
    kier.normalize();
    Location miejsce = null;
    for (double odl = 6; odl >= 2 && miejsce == null; odl -= 1) {
      Location x = WalkiTurowe.naGruncie(ak.cialo.getLocation().clone().add(kier.clone().multiply(odl)));
      if (x.getBlock().isPassable() && x.clone().add(0, 1, 0).getBlock().isPassable()) miejsce = x;
    }
    if (miejsce == null) miejsce = a.getLocation().clone();
    miejsce.setDirection(kier.clone().multiply(-1));
    var g = przeciwnik.g;
    Ciala.Cialo cialo = ciala == null ? null : ciala.postaw(g, g.mobOswojony(false), miejsce, przeciwnik.o.poziom);
    if (cialo == null) { c.blad(a, "Nie udało się postawić stwora przeciwnika."); return false; }
    LivingEntity e = cialo.encja();
    e.getPersistentDataContainer().set(c.kCialoWalki, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
    ciala.ustawZdrowie(e, przeciwnik.hpMax, przeciwnik.hp);
    ciala.nazwij(e, przeciwnik.o, null);
    pj.ciala[1] = e;
    pj.w[1] = przeciwnik;
    pj.npcDruzyna = druzynaNpc;
    pj.koniec = System.currentTimeMillis() + c.ust.pojedynekCzasS * 1000L;
    pj.startCiosow = System.currentTimeMillis() + c.ust.pojedynekWstepMs;
    przejmijCiala(pj);
    wgGracza.put(a.getUniqueId(), pj);
    pj.slotPrzed[0] = a.getInventory().getHeldItemSlot();
    a.getInventory().setHeldItemSlot(8);
    c.msg(a, "<gold>Pojedynek z " + nazwaNpc + "! <yellow>Stwory walczą same.</yellow> <gray>Rozkazy: <white>1–4</white> ruch, <white>5</white> unik, <white>6</white> eliksir, <white>8×2</white> poddanie.");
    if (kamera != null) {
      Location[] k = kameraCel(pj, 0);
      pj.kam[0] = k[0];
      kamera.wlacz(a, k[0], k[1]);
      odslon(pj);
    }
    if (scena != null) scena.napis(a, "<gold>Pojedynek! <gray>" + a.getName() + " vs " + nazwaNpc, 40);
    pokazRuchy(pj, 0);
    return true;
  }

  /**
   * Na czas pojedynku ciała prowadzi pojedynek, nie AI MythicMobs/vanilli. Wcześniej meleeattack trzymał oba
   * stwory sklejone pyskami (doskok z 0,5 bl. się nie odgrywał, pocisk leciał pół bloku), a gotoowner ciągnął
   * do trenera — trafienia nie zgadzały się z tym, co widać („sloppy”). Po walce ciało jest przywoływane
   * ponownie (Przywolania.przestaw), co przywraca mu zwykłe zachowanie.
   */
  private void przejmijCiala(Pojedynek pj) {
    ustawArene(pj);
    if (pj.ciala[0] != null && pj.ciala[1] != null) pj.srodekAreny = pj.ciala[0].getLocation().clone().add(pj.ciala[1].getLocation()).multiply(0.5);
    for (LivingEntity e : pj.ciala) {
      if (!(e instanceof Mob m)) continue;
      // MobGoals#removeAllGoals rzuca na celach MythicMobs (CustomAIAdapt) — zamiast tego ciało nie dostaje celu
      // (celPojedynku), więc meleeattack stoi, a ruchem steruje pathfinder z steruj().
      m.setTarget(null);
      bezAi(e);
    }
  }

  private boolean brakSkillaAi;

  /**
   * Cele AI ciała MM na czas pojedynku: tylko pływanie (skill CHW_AI_POJEDYNEK, skills/Chowance_Walka.yml).
   * Właściciel 2026-09-30: „pety w real time biegną ciągle do ownera zamiast się bić” — `gotoowner` bez parametrów
   * startuje, gdy właściciel jest dalej niż 4 bl. (bajtkod GoToOwnerGoal), a trener siedzi 7+ bl. od areny;
   * co tick nadpisywał nasz pathfinder. Po walce ciało jest przywoływane od nowa (zwykłe AI wraca).
   */
  private void bezAi(LivingEntity e) {
    if (!c.ust.pojedynekBezAiMm || e == null || Mythic.aktywny(e) == null) return;
    if (!Mythic.skill(e, "CHW_AI_POJEDYNEK", null) && !brakSkillaAi) {
      brakSkillaAi = true;
      c.log.warning("Brak skilla MythicMobs CHW_AI_POJEDYNEK (skills/Chowance_Walka.yml) — w pojedynku gotoowner ciągnie stwory do trenera");
    }
  }

  /**
   * Miejsce pojedynku z {@link Arena}: płaskie koło, wolna linia wzroku z kamery bocznej. Ciała stają na
   * wybranych miejscach, oś kamery = oś areny. Bez dobrego miejsca zostaje jak było (tam, gdzie stały).
   */
  private void ustawArene(Pojedynek pj) {
    if (!c.ust.arenaSzukaj || pj.ciala[0] == null || pj.ciala[1] == null || pj.gracze[0] == null) return;
    Location start = pj.gracze[0].getLocation();
    Vector pref = pj.ciala[1].getLocation().toVector().subtract(pj.ciala[0].getLocation().toVector());
    boolean dwaj = pj.gracze[1] != null;
    Arena.Plan plan0 = Arena.znajdz(start, pref, 5.0, c.ust.arenaZasieg, PROMIEN_ARENY - 1, (a, b) -> {
      Vector os = b.toVector().subtract(a.toVector()).setY(0).normalize();
      Location m = a.clone().add(b).multiply(0.5);
      return dwaj ? new Location[] {okoBoczne(m, os, 5, 0), okoBoczne(m, os, 5, 1)} : new Location[] {okoBoczne(m, os, 5, 0)};
    });
    Arena.Plan plan = plan0;
    // Na stromym stoku w pobliżu nie ma płaskiego koła — stwory zbiegały w dół, kamera patrzyła w śnieg
    // (klient QA 2026-09-30). Wtedy szukamy dalej; lepiej przejść kawałek niż walczyć na zboczu.
    if (plan == null || plan.kara() > 4.5) {
      Arena.Plan dalej = Arena.znajdz(start, pref, 5.0, Math.max(18, c.ust.arenaZasieg * 2), PROMIEN_ARENY - 1, (a, b) -> {
        Vector os = b.toVector().subtract(a.toVector()).setY(0).normalize();
        return new Location[] {okoBoczne(a.clone().add(b).multiply(0.5), os, 5, 0)};
      });
      if (dalej != null && (plan == null || dalej.kara() < plan.kara())) plan = dalej;
    }
    if (plan == null) return;
    for (int s = 0; s < 2; s++) {
      LivingEntity e = pj.ciala[s];
      if (e instanceof Mob m) m.getPathfinder().stopPathfinding();
      e.teleport(plan.miejsca()[s]);
    }
    pj.os = plan.kier().clone();
    if (c.ust.debug) c.log.info("Arena (pojedynek): kara=" + String.format(java.util.Locale.ROOT, "%.1f", plan.kara()) + " srodek=" + plan.srodek().toVector());
  }

  /** Oko kamery z boku osi areny (strona s bliżej swojego stwora). */
  static Location okoBoczne(Location srodek, Vector os, double dl, int s) {
    Vector bok = new Vector(-os.getZ(), 0, os.getX()).multiply(s == 0 ? 1 : -1);
    Vector doSwojego = os.clone().multiply(s == 0 ? -1 : 1);
    // min. 7 bl.: arena r=5 cała w kadrze, stwór nie wbiega pod kamerę (klient QA na zboczu 2026-09-30)
    return srodek.clone().add(bok.multiply(Math.max(7, dl * 0.6 + 5))).add(doSwojego.multiply(1.0)).add(0, 2.8, 0);
  }

  /** Stwory biegają — co sekundę dokładamy do odsłoniętych bloki na ich nowej drodze. */
  private void odslon(Pojedynek pj) {
    if (kamera == null) return;
    List<LivingEntity> l = new java.util.ArrayList<>();
    for (LivingEntity e : pj.ciala) if (e != null) l.add(e);
    for (Player p : pj.gracze) if (p != null) kamera.odslon(p, l, false);
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void celPojedynku(org.bukkit.event.entity.EntityTargetEvent e) {
    if (e.getTarget() != null && cialoWPojedynku(e.getEntity())) e.setCancelled(true);
  }

  private List<Location> arena() {
    List<String> a = c.ust.arena;
    if (a.size() < 8) return null;
    World w = Bukkit.getWorld(a.get(0));
    World w2 = Bukkit.getWorld(a.get(4));
    if (w == null || w2 == null) return null;
    try {
      return List.of(new Location(w, Double.parseDouble(a.get(1)), Double.parseDouble(a.get(2)), Double.parseDouble(a.get(3))),
          new Location(w2, Double.parseDouble(a.get(5)), Double.parseDouble(a.get(6)), Double.parseDouble(a.get(7))));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private void cofnijTeleport(Pojedynek pj) {
    if (arena() == null) return;
    for (int s = 0; s < 2; s++) if (pj.gracze[s] != null && pj.gracze[s].isOnline() && pj.powrot[s] != null) pj.gracze[s].teleport(pj.powrot[s]);
  }

  static final long UNIK_ODNOWIENIE = 6000, ELIKSIR_ODNOWIENIE = 10000, KONTRA_MS = 3000, ZMIANA_ODNOWIENIE = 10000;

  /** Paski u góry: oba stwory + klawisze z odnowieniami (albo, bez HUD, pasek akcji jak dawniej). */
  private void pokazRuchy(Pojedynek pj, int s) {
    Player p = pj.gracze[s];
    if (p == null || !p.isOnline()) return;
    long teraz = System.currentTimeMillis();
    Wojownik ja = pj.w[s], on = pj.w[1 - s];
    Zapowiedz grozi = pj.zap[1 - s];
    if (hud == null) {
      StringBuilder sb = new StringBuilder();
      if (grozi != null) sb.append("<red><b>⚠ ").append(grozi.r.nazwa()).append(" — [5] UNIK!</b> ");
      for (int i = 0; i < ja.o.ruchy.size(); i++) {
        Ruch r = c.rej.ruch(ja.o.ruchy.get(i));
        if (r == null) continue;
        Long od = pj.odnowienia[s].get(r.id());
        boolean gotowy = od == null || od <= teraz;
        sb.append(gotowy ? "<white>" : "<dark_gray>").append(i + 1).append(":").append(r.nazwa());
        if (!gotowy) sb.append(" ").append((od - teraz) / 1000 + 1).append("s");
        sb.append("  ");
      }
      c.pasek(p, sb.toString() + "<gray>| " + WalkiTurowe.pasekHp(on, 8));
      return;
    }
    hud.stwory(p, ja, ja.hp, on, on.hp, nazwa(pj, 1 - s));
    long unik = Math.max(0, pj.unikOd[s] - teraz), el = Math.max(0, pj.eliksirOd[s] - teraz);
    int eliksirow = eliksiry(p, false);
    String rozkaz = pj.rozkaz[s] >= 0 && pj.rozkazDo[s] > teraz ? " <white>▶ rozkaz: " + nazwaRuchu(ja, pj.rozkaz[s]) : "";
    List<String> k = new java.util.ArrayList<>(hud.ruchy(ja, on, r -> { Long od = pj.odnowienia[s].get(r.id()); return od == null ? 0 : Math.max(0, od - teraz); }));
    if (pj.rozkaz[s] >= 0 && pj.rozkazDo[s] > teraz && pj.rozkaz[s] < k.size()) k.set(pj.rozkaz[s], k.get(pj.rozkaz[s]) + " <white>◀");
    k.add("<aqua>[5]</aqua> " + (unik > 0 ? "<dark_gray>Unik · " + (unik / 1000 + 1) + " s" : "<aqua>Unik"));
    k.add("<yellow>[6]</yellow> " + (eliksirow == 0 ? "<dark_gray>Eliksir ×0" : el > 0 ? "<dark_gray>Eliksir · " + (el / 1000 + 1) + " s" : "<yellow>Eliksir ×" + eliksirow));
    k.add("<green>[7]</green> " + (pj.zmianaOd[s] > teraz ? "<dark_gray>Zmiana · " + ((pj.zmianaOd[s] - teraz) / 1000 + 1) + " s" : "<green>Zmiana"));
    k.add("<red>[8]×2</red> <gray>Poddaj");
    hud.klawisze(p, k);
    float czas = (float) Math.max(0, pj.koniec - teraz) / Math.max(1, c.ust.pojedynekCzasS * 1000L);
    long sek = Math.max(0, pj.koniec - teraz) / 1000;
    if (pj.teraz[s]) {
      hud.menu(p, "<gold><b>⚡ TERAZ! [5] UNIK</b>", czas, net.kyori.adventure.bossbar.BossBar.Color.PINK);
      return;
    }
    if (grozi != null) {
      // zamach przeciwnika: pasek na czerwono — to jest okno uniku
      hud.menu(p, "<red><b>⚠ " + pj.w[1 - s].nazwa() + " szykuje: " + grozi.r.nazwa() + "!</b></red>  " + (unik > 0 ? "<dark_gray>[5] unik za " + (unik / 1000 + 1) + " s" : "<gray>czekaj na <gold>TERAZ</gold>, wtedy [5]"),
          czas, net.kyori.adventure.bossbar.BossBar.Color.RED);
      return;
    }
    String stan = pj.kontraDo[s] > teraz ? "<gold><b>KONTRA gotowa!</b>" : rozkaz.isEmpty() ? "<gray>Stwory walczą same — wydaj rozkaz" : rozkaz.trim();
    hud.menu(p, "<gold>Pojedynek</gold> <gray>" + (sek / 60) + ":" + String.format("%02d", sek % 60) + "</gray>  " + stan, czas, net.kyori.adventure.bossbar.BossBar.Color.YELLOW);
  }

  private String nazwaRuchu(Wojownik w, int idx) {
    Ruch r = idx < w.o.ruchy.size() ? c.rej.ruch(w.o.ruchy.get(idx)) : null;
    return r == null ? "?" : r.nazwa();
  }

  // ------------------------------------------------------------------ sterowanie

  @EventHandler(priority = EventPriority.HIGH)
  public void klawisz(PlayerItemHeldEvent e) {
    Player p = e.getPlayer();
    Pojedynek pj = wgGracza.get(p.getUniqueId());
    if (pj == null) return;
    e.setCancelled(true);
    int k = e.getNewSlot();
    int s = pj.gracze[0].getUniqueId().equals(p.getUniqueId()) ? 0 : 1;
    if (k <= 3) ruch(p, k);
    else if (k == 4) unik(pj, s);
    else if (k == 5) eliksir(pj, s);
    else if (k == 6) zmienKlawiszem(pj, s);
    else if (k == 7) {
      long teraz = System.currentTimeMillis();
      if (teraz - pj.klikWyjscia[s] < 1500) { przerwij(p); return; }
      pj.klikWyjscia[s] = teraz;
      if (scena != null) scena.napis(p, "<red>Naciśnij [8] jeszcze raz, aby się poddać", 30);
    }
  }

  /** Ruch z klawisza albo z komendy/menu (rozkaz trenera). */
  public void ruch(Player p, int idx) {
    Pojedynek pj = wgGracza.get(p.getUniqueId());
    if (pj == null || pj.skonczony) { c.blad(p, "Nie jesteś w pojedynku."); return; }
    if (pj.konczy || System.currentTimeMillis() < pj.startCiosow) return;
    int s = pj.gracze[0].getUniqueId().equals(p.getUniqueId()) ? 0 : 1;
    Wojownik ja = pj.w[s];
    if (idx < 0 || idx >= ja.o.ruchy.size()) return;
    Ruch r = c.rej.ruch(ja.o.ruchy.get(idx));
    if (r == null) return;
    long teraz = System.currentTimeMillis();
    Long od = pj.odnowienia[s].get(r.id());
    if (od != null && od > teraz) {
      if (scena != null) scena.napis(p, "<gray>" + r.nazwa() + " odnawia się (" + ((od - teraz) / 1000 + 1) + " s)", 20);
      return;
    }
    // Rozkaz trenera: pierwszeństwo przed wyborem stwora i premia obrażeń; stwór podbiega, jeśli za daleko (do 4 s).
    pj.rozkaz[s] = idx;
    pj.rozkazDo[s] = teraz + 4000;
    p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.6f, 1.3f);
    if (scena != null) scena.napis(p, "<yellow>Rozkaz: <white>" + r.nazwa() + "!", 20);
    rozkazy(pj, s, teraz);
    pokazRuchy(pj, s);
  }

  /** Rozkaz w toku: gdy cel w zasięgu ruchu i stwór wolny — wykonaj; po terminie przepada. */
  private void rozkazy(Pojedynek pj, int s, long teraz) {
    int idx = pj.rozkaz[s];
    if (idx < 0) return;
    if (teraz > pj.rozkazDo[s]) { pj.rozkaz[s] = -1; return; }
    if (pj.skonczony || pj.konczy || teraz < pj.startCiosow || pj.zap[s] != null || teraz < pj.blokadaDo[s] || teraz - pj.ostatniRuch[s] < 600) return;
    LivingEntity at = pj.ciala[s], cel = pj.ciala[1 - s];
    if (at == null || cel == null || !at.isValid() || !cel.isValid()) return;
    Ruch r = c.rej.ruch(pj.w[s].o.ruchy.get(idx));
    if (r == null) { pj.rozkaz[s] = -1; return; }
    if (at.getLocation().distance(cel.getLocation()) > zasieg(r)) return; // podbiegnie sterowanie (steruj)
    pj.rozkaz[s] = -1;
    wykonaj(pj, s, idx, true);
  }

  private static double zasieg(Ruch r) { return r.kontakt() ? 3.2 : 14; }

  private void unik(Pojedynek pj, int s) {
    Player p = pj.gracze[s];
    long teraz = System.currentTimeMillis();
    if (pj.konczy || teraz < pj.startCiosow) return;
    if (teraz < pj.unikOd[s]) { if (scena != null) scena.napis(p, "<gray>Unik za " + ((pj.unikOd[s] - teraz) / 1000 + 1) + " s", 20); return; }
    c.debug("pojedynek unik s=" + s + " teraz=" + pj.teraz[s] + " zap=" + (pj.zap[1 - s] != null));
    pj.unikDo[s] = teraz + c.ust.unikOknoMs;
    pj.unikOd[s] = teraz + UNIK_ODNOWIENIE;
    LivingEntity e = pj.ciala[s], on = pj.ciala[1 - s];
    if (e != null && e.isValid()) {
      Vector bok = on == null ? new Vector(1, 0, 0) : on.getLocation().toVector().subtract(e.getLocation().toVector()).setY(0);
      if (bok.lengthSquared() < 1e-4) bok = new Vector(1, 0, 0);
      bok = new Vector(-bok.getZ(), 0, bok.getX()).normalize().multiply(pj.bok[s] * 0.9).setY(0.3);
      pj.bok[s] = -pj.bok[s];
      if (e instanceof Mob m) m.getPathfinder().stopPathfinding();
      e.setVelocity(bok);
      e.getWorld().spawnParticle(Particle.CLOUD, e.getLocation().add(0, 0.4, 0), 15, 0.3, 0.2, 0.3, 0.05);
      e.getWorld().playSound(e.getLocation(), org.bukkit.Sound.ENTITY_BREEZE_JUMP, 0.8f, 1.3f);
    }
    if (scena != null && p != null) scena.napis(p, "<aqua>Unik!", 12);
    pokazRuchy(pj, s);
  }

  /** Liczba eliksirów leczących w ekwipunku; {@code zuzyj} = zużyj pierwszy i zwróć jego leczenie % (albo 0). */
  private int eliksiry(Player p, boolean zuzyj) {
    int ile = 0;
    for (org.bukkit.inventory.ItemStack it : p.getInventory().getContents()) {
      String id = pl.mcmmorpg.chowance.integracja.PrzedmiotMmo.id(it);
      var el = id == null ? null : c.rej.przedmioty().eliksiry().get(id);
      if (el == null || el.wskrzesza() || el.leczenieProcent() <= 0) continue;
      if (zuzyj) { it.setAmount(it.getAmount() - 1); return (int) Math.round(el.leczenieProcent()); }
      ile += it.getAmount();
    }
    return ile;
  }

  private void eliksir(Pojedynek pj, int s) {
    Player p = pj.gracze[s];
    long teraz = System.currentTimeMillis();
    if (pj.konczy || teraz < pj.startCiosow) return;
    if (teraz < pj.eliksirOd[s]) { if (scena != null) scena.napis(p, "<gray>Eliksir za " + ((pj.eliksirOd[s] - teraz) / 1000 + 1) + " s", 20); return; }
    Wojownik ja = pj.w[s];
    if (ja.hp >= ja.hpMax) { if (scena != null) scena.napis(p, "<gray>" + ja.nazwa() + " jest w pełni sił", 20); return; }
    int proc = eliksiry(p, true);
    if (proc <= 0) { if (scena != null) scena.napis(p, "<red>Nie masz eliksiru leczącego", 20); return; }
    pj.eliksirOd[s] = teraz + ELIKSIR_ODNOWIENIE;
    int przed = (int) Math.ceil(ja.hp);
    ja.hp = Math.min(ja.hpMax, ja.hp + ja.hpMax * proc / 100.0);
    LivingEntity e = pj.ciala[s];
    if (e != null && e.isValid()) {
      e.getWorld().spawnParticle(Particle.HEART, e.getLocation().add(0, 1, 0), 8, 0.4, 0.3, 0.4, 0);
      e.getWorld().playSound(e.getLocation(), org.bukkit.Sound.ENTITY_WITCH_DRINK, 0.8f, 1.3f);
    }
    if (scena != null) scena.liczba(e, "<green><b>+" + ((int) Math.ceil(ja.hp) - przed), 1.6f);
    odswiez(pj);
    pokazRuchy(pj, s);
  }

  /**
   * Start ruchu: odnowienie + ZAPOWIEDŹ (zamach widoczny nad stworem, pasek obrońcy na czerwono). Obrażenia
   * dopiero w chwili trafienia — wtedy liczy się unik. Wcześniej cios był liczony od razu i bez sygnału, więc
   * unik [5] był zgadywaniem, a rozkaz nie różnił się od wyboru stwora.
   */
  private void wykonaj(Pojedynek pj, int s, int idx, boolean rozkaz) {
    Player p = pj.gracze[s];
    Wojownik ja = pj.w[s];
    if (idx < 0 || idx >= ja.o.ruchy.size() || pj.zap[s] != null) return;
    Ruch r = c.rej.ruch(ja.o.ruchy.get(idx));
    if (r == null) return;
    long teraz = System.currentTimeMillis();
    Long od = pj.odnowienia[s].get(r.id());
    if (od != null && od > teraz) { pokazRuchy(pj, s); return; }
    if (pj.konczy) return;
    if (ja.status == Status.SEN) { if (p != null) c.pasek(p, "<light_purple>" + ja.nazwa() + " śpi…"); return; }
    if (ja.status == Status.OSZOLOMIENIE && ThreadLocalRandom.current().nextDouble() < 0.33) {
      pj.odnowienia[s].put(r.id(), teraz + 1000);
      pj.ostatniRuch[s] = teraz;
      if (scena != null) scena.liczba(pj.ciala[s], "<light_purple>oszołomiony!", 1.2f);
      return;
    }
    pj.odnowienia[s].put(r.id(), teraz + Math.round(r.odnowienie() * 1000 * (ja.status == Status.SPETANIE ? 1.5 : 1.0)));
    pj.ostatniRuch[s] = teraz;
    Zapowiedz z = new Zapowiedz(idx, r, rozkaz);
    int ticki = r.kontakt() ? c.ust.zapowiedzKontaktTicki : c.ust.zapowiedzDystansTicki;
    LivingEntity at = pj.ciala[s], cel = pj.ciala[1 - s];
    if (ticki <= 0 || at == null || !at.isValid()) { uderz(pj, s, z); return; }
    pj.zap[s] = z;
    if (at instanceof Mob m) { m.getPathfinder().stopPathfinding(); if (cel != null) m.lookAt(cel); }
    if (scena != null) scena.zapowiedz(at, r, (rozkaz ? "<yellow>Rozkaz! " : "") , ticki);
    Player obronca = pj.gracze[1 - s];
    if (obronca != null && obronca.isOnline()) obronca.playSound(obronca.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BELL, 0.9f, 0.7f);
    for (int x = 0; x < 2; x++) pokazRuchy(pj, x);
    // NPC też czasem robi unik (przy gotowym uniku ~30%) — inaczej unik byłby przywilejem tylko jednej strony
    if (obronca == null && teraz >= pj.unikOd[1 - s] && ThreadLocalRandom.current().nextDouble() < 0.3) {
      Bukkit.getScheduler().runTaskLater(c.plugin, () -> { if (!pj.skonczony) unik(pj, 1 - s); }, ticki + 2L);
    }
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> { if (!pj.skonczony && pj.zap[s] == z) uderz(pj, s, z); }, ticki);
  }

  /** Koniec zamachu: animacja ataku (doskok/pocisk); obrażenia w chwili, gdy cios dolatuje. */
  private void uderz(Pojedynek pj, int s, Zapowiedz z) {
    pj.zap[s] = null;
    if (pj.skonczony || pj.konczy) return;
    LivingEntity at = pj.ciala[s], cel = pj.ciala[1 - s];
    if (at != null && cel != null && at.getLocation().distanceSquared(cel.getLocation()) > 400) {
      Player p = pj.gracze[s];
      if (p != null) c.pasek(p, "<gray>Za daleko — cel poza zasięgiem ruchu.");
      return;
    }
    // „TERAZ!”: cios leci — to jest okno uniku (unik wciśnięty na samym „szykuje” jest za wcześnie; recenzja R-10)
    pj.teraz[1 - s] = true;
    Player obr = pj.gracze[1 - s];
    if (obr != null && obr.isOnline()) {
      obr.playSound(obr.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
      pokazRuchy(pj, 1 - s);
    }
    int doTrafienia = 0;
    if (scena != null) doTrafienia = scena.atak(at, cel, z.r);
    else if (!z.r.skill().isBlank()) Mythic.skill(at, z.r.skill(), cel);
    if (z.r.kontakt()) pj.blokadaDo[s] = System.currentTimeMillis() + 950;
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> rozstrzygnij(pj, s, z), Math.max(1, doTrafienia));
  }

  private void rozstrzygnij(Pojedynek pj, int s, Zapowiedz z) {
    pj.teraz[1 - s] = false;
    if (pj.skonczony) return;
    long teraz = System.currentTimeMillis();
    Wojownik ja = pj.w[s], on = pj.w[1 - s];
    Ruch r = z.r;
    LivingEntity at = pj.ciala[s], cel = pj.ciala[1 - s];
    boolean unikniety = pj.unikDo[1 - s] >= teraz;
    c.debug("pojedynek cios s=" + s + " unikDo-teraz=" + (pj.unikDo[1 - s] - teraz));
    c.debug("pojedynek trafienie s=" + s + " ruch=" + r.id() + " rozkaz=" + z.rozkaz + " unik=" + unikniety
        + " dist=" + (at != null && cel != null && at.getWorld().equals(cel.getWorld()) ? String.format(java.util.Locale.ROOT, "%.2f", at.getLocation().distance(cel.getLocation())) : "?"));
    if (unikniety) {
      // udany unik = kontra: następny ruch obrońcy mocniejszy, a unik odnawia się szybciej
      pj.unikDo[1 - s] = 0;
      pj.kontraDo[1 - s] = teraz + KONTRA_MS;
      pj.unikOd[1 - s] = Math.min(pj.unikOd[1 - s], teraz + 3500);
      if (scena != null) scena.liczba(cel, "<aqua><b>UNIK!</b>\n<gold>kontra gotowa", 1.6f);
      if (cel != null && cel.isValid()) cel.getWorld().playSound(cel.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 1f, 1.4f);
      pasekObu(pj, "<aqua>" + on.nazwa() + " unika: " + r.nazwa() + "! <gold>Kontra gotowa.");
      for (int x = 0; x < 2; x++) pokazRuchy(pj, x);
      return;
    }
    boolean trafil = r.zawszeTrafia() || ThreadLocalRandom.current().nextDouble() * 100 < r.celnosc()
        * (on.ma(RodzajCechy.UNIK) ? 1 - on.cecha.d("procent", 0) / 100.0 : 1.0);
    if (!trafil) {
      if (scena != null) scena.liczba(cel, "<gray><i>Chybienie!", 1.4f);
      pasekObu(pj, "<gray>" + ja.nazwa() + ": " + r.nazwa() + " — chybienie!");
      for (int x = 0; x < 2; x++) pokazRuchy(pj, x);
      return;
    }
    int zadane = 0;
    if (r.kategoria() != Kategoria.STATUS) {
      boolean kontra = pj.kontraDo[s] >= teraz;
      if (kontra) pj.kontraDo[s] = 0;
      // ruchy z długim odnowieniem („ostateczne”) mocniej w real-time: do +50% (R-09)
      double ostateczny = 1.0 + Math.min(0.5, Math.max(0, r.odnowienie() - 6) / 24.0);
      double premia = (z.rozkaz ? c.ust.rozkazPremia : 1.0) * (kontra ? c.ust.kontraPremia : 1.0) * ostateczny;
      var wyn = SilnikWalki.obrazenia(c.rej, ja, on, r, ThreadLocalRandom.current());
      zadane = (int) Math.min(on.hp, Math.max(1, Math.round(wyn.obrazenia() * c.ust.pojedynekMnoznik * premia)));
      on.hp -= zadane;
      if (cel != null && scena != null) {
        scena.uderzenie(cel, at, zadane, wyn.skutecznosc(), wyn.krytyk(), pj.gracze, pj.gracze[1 - s], r);
        if (kontra) scena.liczba(at, "<gold><b>KONTRA!", 1.5f);
      } else if (cel != null) {
        cel.playHurtAnimation(0);
      }
      pasekObu(pj, "<white>" + ja.nazwa() + ": " + r.nazwa() + (kontra ? " <gold>(kontra)" : z.rozkaz ? " <yellow>(rozkaz)" : "") + " <red>-" + zadane
          + (wyn.skutecznosc() >= 2 ? " <green>bardzo skuteczne!" : wyn.skutecznosc() < 1 ? " <gray>mało skuteczne" : ""));
    }
    for (EfektRuchu ef : r.efekty()) {
      if (ThreadLocalRandom.current().nextDouble() * 100 >= ef.szansa()) continue;
      Wojownik x = ef.naSiebie() ? ja : on;
      int sx = ef.naSiebie() ? s : 1 - s;
      switch (ef.rodzaj()) {
        case STATUS -> {
          if (x.status == null && !(x.ma(RodzajCechy.ODPORNOSC_NA_STATUS) && x.cecha.status() == ef.status())) {
            x.status = ef.status();
            pj.statusDo[sx] = teraz + Math.max(1, ef.czas()) * 1000L;
            if (scena != null) scena.liczba(pj.ciala[sx], "<light_purple>" + ef.status().nazwa() + "!", 1.3f);
          }
        }
        case STAT -> x.etapy[ef.stat().ordinal()] = Math.max(-6, Math.min(6, x.etapy[ef.stat().ordinal()] + ef.zmiana()));
        case LECZENIE -> x.hp = Math.min(x.hpMax, x.hp + x.hpMax * ef.procent() / 100.0);
        case WYSYSANIE -> ja.hp = Math.min(ja.hpMax, ja.hp + zadane * ef.procent() / 100.0);
        case ODRZUT -> ja.hp = Math.max(0, ja.hp - Math.max(1, zadane * ef.procent() / 100));
      }
    }
    if (r.kontakt() && on.ma(RodzajCechy.KONTRA_STATUS) && ja.status == null && ThreadLocalRandom.current().nextDouble() * 100 < on.cecha.d("szansa", 0)) {
      ja.status = on.cecha.status();
      pj.statusDo[s] = teraz + 3000;
    }
    odswiez(pj);
    sprawdzKoniec(pj);
    for (int x = 0; x < 2; x++) pokazRuchy(pj, x);
  }

  /** Najlepszy gotowy ruch strony (ten sam wzór co towarzysz PvE); {@code tylkoWZasiegu} = sięga celu teraz. */
  private int najlepszy(Pojedynek pj, int s, long teraz, double dist, boolean tylkoWZasiegu) {
    Wojownik ja = pj.w[s], on = pj.w[1 - s];
    int najlepszy = -1;
    double best = -1;
    for (int i = 0; i < ja.o.ruchy.size(); i++) {
      Ruch r = c.rej.ruch(ja.o.ruchy.get(i));
      if (r == null) continue;
      Long od = pj.odnowienia[s].get(r.id());
      if (od != null && od > teraz) continue;
      if (tylkoWZasiegu && dist > zasieg(r)) continue;
      double v = r.kategoria() == Kategoria.STATUS ? (on.status == null ? 15 : 1)
          : r.moc() * (r.zawszeTrafia() ? 1 : r.celnosc() / 100.0) * c.rej.typy().mnoznik(r.typ(), on.g.typy()) * (ja.g.typy().contains(r.typ()) ? c.rej.typy().stab() : 1.0);
      v *= 0.85 + ThreadLocalRandom.current().nextDouble() * 0.3;
      if (v > best) { best = v; najlepszy = i; }
    }
    return najlepszy;
  }

  /** Stwór sam wybiera ruch (real-time), gdy trener nie wydał rozkazu. Odstęp między ruchami z configu. */
  private void auto(Pojedynek pj, int s, long teraz) {
    if (pj.rozkaz[s] >= 0 && pj.rozkazDo[s] > teraz) return; // czeka na rozkaz trenera
    if (pj.skonczony || pj.konczy || teraz < pj.startCiosow || pj.zap[s] != null || teraz < pj.blokadaDo[s]
        || teraz - pj.ostatniRuch[s] < c.ust.pojedynekOdstepMs) return;
    LivingEntity at = pj.ciala[s], cel = pj.ciala[1 - s];
    if (at == null || cel == null || !at.isValid() || !cel.isValid()) return;
    if (pj.w[s].status == Status.SEN) return;
    int i = najlepszy(pj, s, teraz, at.getLocation().distance(cel.getLocation()), true);
    if (i >= 0) wykonaj(pj, s, i, false);
  }

  /**
   * Ruch ciała po arenie (co 4 ticki): do ruchu kontaktowego podchodzi na ~2 bl. (doskok ma skąd skoczyć),
   * dystansowy trzyma 4–8 bl. i krąży bokiem. W zamachu i doskoku stoi.
   */
  private void steruj(Pojedynek pj, int s, long teraz) {
    if (!(pj.ciala[s] instanceof Mob m) || !m.isValid() || pj.ciala[1 - s] == null || !pj.ciala[1 - s].isValid()) return;
    naArene(pj, m);
    LivingEntity cel = pj.ciala[1 - s];
    if (pj.zap[s] != null || teraz < pj.blokadaDo[s] || teraz < pj.startCiosow || pj.konczy || pj.w[s].status == Status.SEN) {
      stop(pj, s, m);
      if (!pj.konczy) m.lookAt(cel);
      return;
    }
    Location ja = m.getLocation(), on = cel.getLocation();
    double d = ja.distance(on);
    int idx = pj.rozkaz[s] >= 0 && pj.rozkazDo[s] > teraz ? pj.rozkaz[s] : najlepszy(pj, s, teraz, d, false);
    Ruch r = idx >= 0 && idx < pj.w[s].o.ruchy.size() ? c.rej.ruch(pj.w[s].o.ruchy.get(idx)) : null;
    Vector odCelu = ja.toVector().subtract(on.toVector()).setY(0);
    if (odCelu.lengthSquared() < 1e-4) odCelu = new Vector(1, 0, 0);
    odCelu.normalize();
    if (r != null && r.kontakt()) {
      if (d > 2.4) idz(pj, s, m, wArenie(pj, on.clone().add(odCelu.clone().multiply(1.6))), 1.35);
      else { stop(pj, s, m); m.lookAt(cel); }
      return;
    }
    if (d < 4) { idz(pj, s, m, wArenie(pj, ja.clone().add(odCelu.multiply(3))), 1.2); return; }
    if (d > 8) { idz(pj, s, m, wArenie(pj, on.clone().add(odCelu.multiply(5))), 1.1); return; }
    if (teraz - pj.bokOd[s] > 2200) { pj.bokOd[s] = teraz; if (ThreadLocalRandom.current().nextBoolean()) pj.bok[s] = -pj.bok[s]; }
    Vector bok = new Vector(-odCelu.getZ(), 0, odCelu.getX()).multiply(2.0 * pj.bok[s]);
    idz(pj, s, m, wArenie(pj, ja.clone().add(bok)), 0.9);
  }

  /**
   * moveTo tylko, gdy cel przesunął się o > 1 bl. albo ścieżka się skończyła. Wcześniej ścieżka była liczona od nowa
   * co 4 ticki na każde ciało (5×/s) — koszt rośnie z liczbą walk, a ruch szarpał przy każdym przeliczeniu.
   */
  private void idz(Pojedynek pj, int s, Mob m, Location cel, double predkosc) {
    Location byl = pj.celDrogi[s];
    if (byl != null && byl.getWorld() == cel.getWorld() && byl.distanceSquared(cel) < 1.0 && m.getPathfinder().hasPath()) return;
    pj.celDrogi[s] = cel.clone();
    m.getPathfinder().moveTo(cel, predkosc);
  }

  private void stop(Pojedynek pj, int s, Mob m) {
    if (pj.celDrogi[s] == null && !m.getPathfinder().hasPath()) return;
    pj.celDrogi[s] = null;
    m.getPathfinder().stopPathfinding();
  }

  // ------------------------------------------------------------------ kamera boczna

  /** Kamera z boku areny (strona gracza bliżej), patrzy na środek między stworami. [oko, cel] */
  private Location[] kameraCel(Pojedynek pj, int s) {
    LivingEntity a = pj.ciala[0], b = pj.ciala[1];
    if (a == null || b == null) return new Location[] {pj.gracze[s].getEyeLocation(), pj.gracze[s].getEyeLocation().add(pj.gracze[s].getLocation().getDirection())};
    Location m = a.getLocation().clone().add(b.getLocation()).multiply(0.5);
    Vector ab = b.getLocation().toVector().subtract(a.getLocation().toVector()).setY(0);
    double dl = ab.length();
    if (pj.os == null) pj.os = ab.lengthSquared() < 0.01 ? new Vector(1, 0, 0) : ab.clone().normalize();
    // Klient QA 2026-09-27: kamera liczona z bieżącej osi kręciła się i podjeżdżała pod biegające stwory.
    // Stała oś + dystans od rozstawu (min. 5 bl., ujęcie z bliska: małe modele) = spokojny widok.
    Location oko = okoBoczne(m, pj.os, dl, s);
    return new Location[] {oko, m.clone().add(0, 0.8, 0)};
  }

  /**
   * Co 4 ticki: rozkazy trenera (szybka reakcja na klawisz). Siedzisko trenera stoi w miejscu —
   * gracz sam się rozgląda; przesuwamy je tylko, gdy stwory odbiegną dalej niż 14 bl.
   */
  public void kamery() {
    long teraz = System.currentTimeMillis();
    for (Pojedynek pj : new java.util.HashSet<>(wgGracza.values())) {
      if (pj.skonczony) continue;
      for (int s = 0; s < 2; s++) {
        rozkazy(pj, s, teraz);
        boolean npc = pj.gracze[s] == null && pj.npcNazwa != null && s == 1;
        // Wybór ruchu i ruch po arenie co 4 ticki (wcześniej co sekundę — do 1 s spóźnienia każdej decyzji).
        if (!pj.skonczony && (c.ust.pojedynekAuto || npc)) auto(pj, s, teraz);
        if (!pj.skonczony) steruj(pj, s, teraz);
        if (pj.skonczony) break;
        Player p = pj.gracze[s];
        if (kamera == null || p == null || !kamera.aktywna(p) || pj.ciala[0] == null || pj.ciala[1] == null) continue;
        Location m = pj.ciala[0].getLocation().clone().add(pj.ciala[1].getLocation()).multiply(0.5);
        if (m.getWorld().equals(p.getWorld()) && m.distanceSquared(p.getLocation()) > 14 * 14) {
          Location[] k = kameraCel(pj, s);
          kamera.przesun(p, k[0], k[1]);
          odslon(pj);
        } else if (m.getWorld().equals(p.getWorld())) {
          dociagnijWzrok(pj, s, p, m.add(0, 0.6, 0), teraz);
        }
      }
    }
  }

  /**
   * Stwory wybiegły z kadru, a gracz od 2,5 s sam się nie rozgląda — wzrok wraca na walkę (klient QA 2026-09-30:
   * na zboczu stwory zniknęły z ekranu, w kadrze została koza). Obrót gracza wykrywamy po różnicy z tym, co
   * ustawiliśmy ostatnio; gdy się rozgląda, nie przeszkadzamy.
   */
  private void dociagnijWzrok(Pojedynek pj, int s, Player p, Location cel, long teraz) {
    Location oko = p.getEyeLocation();
    float yaw = oko.getYaw(), pitch = oko.getPitch();
    float[] nasz = pj.obrot[s];
    if (nasz != null && (Math.abs(wrap(yaw - nasz[0])) > 2 || Math.abs(pitch - nasz[1]) > 2)) { pj.recznyObrot[s] = teraz; pj.obrot[s] = new float[] {yaw, pitch}; }
    if (teraz - pj.recznyObrot[s] < 2500) return;
    Vector ku = cel.toVector().subtract(oko.toVector());
    if (ku.lengthSquared() < 1e-3) return;
    double kat = Math.toDegrees(oko.getDirection().angle(ku));
    if (kat < 32) return;
    kamera.spojrz(p, cel);
    Location po = oko.clone().setDirection(ku);
    pj.obrot[s] = new float[] {po.getYaw(), po.getPitch()};
  }

  private static float wrap(float d) { d %= 360; if (d > 180) d -= 360; if (d < -180) d += 360; return d; }

  private void pasekObu(Pojedynek pj, String t) {
    // przez MMOCore (displayActionBar) — zwykły pasek akcji domyślny HUD MMOCore nadpisywał po 0,25 s
    for (Player p : pj.gracze) if (p != null && p.isOnline()) c.pasek(p, t);
  }

  private void odswiez(Pojedynek pj) {
    for (int s = 0; s < 2; s++) {
      LivingEntity e = pj.ciala[s];
      if (e == null || !e.isValid()) continue;
      var attr = e.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
      if (attr != null) attr.setBaseValue(pj.w[s].hpMax);
      e.setHealth(Math.max(0.5, Math.min(pj.w[s].hpMax, pj.w[s].hp)));
      if (scena != null) scena.pasek(e, pj.w[s], pj.w[s].hp);
    }
  }

  private void sprawdzKoniec(Pojedynek pj) {
    if (pj.skonczony) return;
    // Drużyna ma znaczenie (recenzja R-05): po KO wchodzi następny sprawny stwór strony, dopiero brak = przegrana.
    for (int s = 0; s < 2 && !pj.konczy; s++) if (pj.w[s].zemdlony()) nastepny(pj, s, true);
    boolean z0 = pj.w[0].zemdlony(), z1 = pj.w[1].zemdlony();
    if ((!z0 && !z1) || pj.konczy) return;
    int zw = z1 ? 0 : 1;
    pj.konczy = true;
    LivingEntity pokonany = pj.ciala[1 - zw];
    if (pokonany != null && pokonany.isValid()) {
      pokonany.getWorld().spawnParticle(Particle.POOF, pokonany.getLocation().add(0, 0.6, 0), 25, 0.35, 0.35, 0.35, 0.02);
      if (pokonany instanceof Mob m) { m.setTarget(null); m.setAI(false); }
    }
    if (pj.ciala[zw] instanceof Mob m) m.setTarget(null);
    if (scena != null) scena.napisWszystkim(pj.gracze, "<gold><b>Wygrywa " + nazwa(pj, zw) + "!", 50);
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      if (pokonany instanceof Mob m && m.isValid()) m.setAI(true);
      zakoncz(pj, zw, "KO");
    }, 50L);
  }

  /** [7] zmiana stwora w trakcie pojedynku (odnowienie 10 s; nie w zamachu). */
  private void zmienKlawiszem(Pojedynek pj, int s) {
    Player p = pj.gracze[s];
    long teraz = System.currentTimeMillis();
    if (p == null || pj.konczy || teraz < pj.startCiosow || pj.zap[s] != null) return;
    if (teraz < pj.zmianaOd[s]) { if (scena != null) scena.napis(p, "<gray>Zmiana za " + ((pj.zmianaOd[s] - teraz) / 1000 + 1) + " s", 20); return; }
    if (!nastepny(pj, s, false)) { c.pasek(p, "<gray>Nie masz innego sprawnego chowańca."); return; }
    pj.zmianaOd[s] = teraz + ZMIANA_ODNOWIENIE;
  }

  /**
   * Następny sprawny stwór strony wchodzi na arenę w miejsce obecnego. {@code poKo} — obecny zemdlał.
   * Zwraca false, gdy nikogo nie ma (wtedy przy KO kończy sprawdzKoniec).
   */
  private boolean nastepny(Pojedynek pj, int s, boolean poKo) {
    Wojownik stary = pj.w[s];
    LivingEntity cialoStare = pj.ciala[s];
    Location miejsce = cialoStare != null && cialoStare.isValid() ? cialoStare.getLocation() : null;
    Player p = pj.gracze[s];
    Wojownik nowy = null;
    LivingEntity cialo;
    if (p == null) {
      if (pj.npcNazwa == null) return false;
      for (Wojownik x : pj.npcDruzyna) if (x != stary && !x.zemdlony()) { nowy = x; break; }
      if (nowy == null || ciala == null || miejsce == null) return false;
      Ciala.Cialo cl = ciala.postaw(nowy.g, nowy.g.mobOswojony(false), miejsce, nowy.o.poziom);
      if (cl == null) return false;
      cialo = cl.encja();
      cialo.getPersistentDataContainer().set(c.kCialoWalki, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
      ciala.ustawZdrowie(cialo, nowy.hpMax, nowy.hp);
      ciala.nazwij(cialo, nowy.o, null);
      if (scena != null) scena.usunEtykiete(cialoStare);
      if (cialoStare != null) Mythic.usun(cialoStare);
    } else {
      ProfilDane d = c.dane(p);
      if (d == null) return false;
      Osobnik o = null;
      for (Osobnik x : d.druzyna) if (!x.id.equals(stary.o.id) && !druzyny.zemdlony(p, x)) { o = x; break; }
      if (o == null) return false;
      if (scena != null) scena.usunEtykiete(cialoStare);
      double hpStarego = stary.hp;
      if (!przywolania.przywolaj(p, o, true)) return false;
      druzyny.ustawHp(stary.o, hpStarego); // odwołanie zapisało HP z ciała (min. 0,5) — prawda jest w silniku pojedynku
      Przywolania.Aktywny ak = przywolania.aktywny(p);
      if (ak == null) return false;
      cialo = ak.cialo;
      if (miejsce != null) cialo.teleport(miejsce);
      nowy = druzyny.wojownik(p, o);
    }
    pj.w[s] = nowy;
    pj.ciala[s] = cialo;
    bezAi(cialo);
    pj.zap[s] = null;
    pj.rozkaz[s] = -1;
    pj.kontraDo[s] = 0;
    pj.ostatniRuch[s] = System.currentTimeMillis();
    if (cialo instanceof Mob m) m.setTarget(null);
    if (pj.ciala[1 - s] instanceof Mob m2) m2.setTarget(null);
    cialo.getWorld().spawnParticle(Particle.END_ROD, cialo.getLocation().add(0, 0.8, 0), 25, 0.3, 0.5, 0.3, 0.03);
    String kto = nazwa(pj, s);
    Wojownik n = nowy;
    for (Player x : pj.gracze) if (x != null && x.isOnline() && scena != null)
      scena.napis(x, (poKo ? "<red>" + stary.nazwa() + " mdleje! " : "") + "<aqua>" + kto + " wysyła: <white>" + n.nazwa(), 35);
    odswiez(pj);
    for (int x = 0; x < 2; x++) pokazRuchy(pj, x);
    return true;
  }

  // ------------------------------------------------------------------ tik (co 20 ticków)

  public void tik() {
    long teraz = System.currentTimeMillis();
    for (Pojedynek pj : List.copyOf(new java.util.HashSet<>(wgGracza.values()))) {
      if (pj.skonczony || pj.konczy) continue;
      pj.tik++;
      odslon(pj);
      for (int s = 0; s < 2; s++) {
        Player p = pj.gracze[s];
        boolean npc = p == null && pj.npcNazwa != null && s == 1;
        if (!npc && (p == null || !p.isOnline() || p.isDead())) { zakoncz(pj, 1 - s, "PRZERWANY"); break; }
        LivingEntity e = pj.ciala[s];
        if (e == null || !e.isValid()) { zakoncz(pj, 1 - s, "PRZERWANY"); break; }
        if (!npc && (!p.getWorld().equals(e.getWorld()) || p.getLocation().distanceSquared(e.getLocation()) > c.ust.pojedynekZasieg * c.ust.pojedynekZasieg)) {
          c.blad(p, "Oddaliłeś się z areny."); zakoncz(pj, 1 - s, "PRZERWANY"); break;
        }
        Wojownik w = pj.w[s];
        w.srodowisko = Srodowisko.z(e);
        if (w.status != null && teraz > pj.statusDo[s]) w.status = null;
        if ((w.status == Status.OPARZENIE || w.status == Status.ZATRUCIE) && pj.tik % 2 == 0) {
          w.hp = Math.max(0, w.hp - Math.max(1, w.hpMax / (w.status == Status.OPARZENIE ? 16 : 8)));
        }
        if (w.ma(RodzajCechy.REGENERACJA) && pj.tik % 10 == 0) w.hp = Math.min(w.hpMax, w.hp + w.hpMax * w.cecha.d("procent", 0) / 100.0);
        pokazRuchy(pj, s);
      }
      if (pj.skonczony) continue;
      odswiez(pj);
      sprawdzKoniec(pj);
      if (!pj.skonczony && teraz > pj.koniec) {
        int lepszy = pj.w[0].hpProcent() >= pj.w[1].hpProcent() ? 0 : 1;
        zakoncz(pj, lepszy, "CZAS");
      }
    }
  }

  private void zakoncz(Pojedynek pj, int zwyciezca, String powod) {
    if (pj.skonczony) return;
    pj.skonczony = true;
    if (kamera != null) for (Player p : pj.gracze) if (p != null) kamera.wylacz(p);
    if (hud != null) for (Player p : pj.gracze) if (p != null) hud.ukryj(p);
    for (int s = 0; s < 2; s++) if (pj.gracze[s] != null && pj.gracze[s].isOnline() && pj.slotPrzed[s] >= 0) pj.gracze[s].getInventory().setHeldItemSlot(pj.slotPrzed[s]);
    if (pj.npcNazwa != null && pj.ciala[1] != null) { if (scena != null) scena.usunEtykiete(pj.ciala[1]); Mythic.usun(pj.ciala[1]); }
    for (int s = 0; s < 2; s++) {
      Player p = pj.gracze[s];
      if (p == null) continue;
      wgGracza.remove(p.getUniqueId());
      ProfilDane d = c.dane(p);
      if (pj.ciala[s] != null && pj.ciala[s] instanceof Mob m) m.setTarget(null);
      if (scena != null) scena.usunEtykiete(pj.ciala[s]);
      // KOLEJNOŚĆ: odwołanie zapisuje HP z CIAŁA (min. 0,5 — ciało nie może mieć 0), więc HP z silnika
      // pojedynku zapisujemy PO nim. Odwrotnie pokonany wychodził z 0,5 HP zamiast wyczerpany (PROBLEMY A-008).
      if (p.isOnline() && pj.w[s].zemdlony()) przywolania.odwolaj(p, false);
      if (d != null) {
        druzyny.ustawHp(pj.w[s].o, pj.w[s].hp);
        d.brudne = true;
      }
      if (p.isOnline()) {
        if (!pj.w[s].zemdlony()) {
          // ciało straciło AI na czas pojedynku (przejmijCiala) — przywołujemy je ponownie ze zwykłym AI i HP po walce
          var o = pj.w[s].o;
          Bukkit.getScheduler().runTaskLater(c.plugin, () -> { if (p.isOnline() && !wPojedynku(p)) przywolania.przestaw(p, o); }, 2L);
        }
        c.msg(p, (zwyciezca == s ? "<green><b>Wygrywasz pojedynek!</b>" : "<red><b>Przegrywasz pojedynek.</b>") + " <gray>(" + powod.toLowerCase() + ")");
        p.playSound(p.getLocation(), zwyciezca == s ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1f);
      }
    }
    Player zw = pj.gracze[zwyciezca], pr = pj.gracze[1 - zwyciezca];
    ProfilDane dz = zw == null ? null : c.dane(zw), dp = pr == null ? null : c.dane(pr);
    if (dz != null && dp != null) {
      int[] n = ranking.wynik(zw, dz, pr, dp);
      c.msg(zw, "<gold>ELO: " + n[0]);
      if (pr.isOnline()) c.msg(pr, "<gold>ELO: " + n[1]);
      c.zdarzenie("WYGRANA_POJEDYNKU", Map.of("gracz", zw.getName(), "przegrany", pr.getName(), "elo", String.valueOf(n[0])));
      if (!c.ust.towarzyszWlaczony) przywolania.sygnalAktywnemu(zw, pj.w[zwyciezca].o, "OKRZYK");
    }
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> cofnijTeleport(pj), 40L);
    // Pojedynek z NPC trenera niesie id (pierwsze kroki: „wygraj z trenerem” w obu trybach walki).
    Bukkit.getPluginManager().callEvent(new WalkaZakonczonaEvent("POJEDYNEK", zw, pr, pr == null ? (pj.npcNazwa == null ? "" : pj.npcNazwa) : pr.getName(),
        pj.npcNazwa == null || "PRZERWANY".equals(powod) ? null : "NPC_POJEDYNEK"));
    for (Player p : pj.gracze) if (p != null && c.dane(p) != null) c.dane.zapisz(c.dane(p));
  }

  public void przerwij(Player p) {
    Pojedynek pj = wgGracza.get(p.getUniqueId());
    if (pj != null) zakoncz(pj, pj.gracze[0].getUniqueId().equals(p.getUniqueId()) ? 1 : 0, "PRZERWANY");
  }

  public void przerwijWszystkie() {
    for (Pojedynek pj : List.copyOf(new java.util.HashSet<>(wgGracza.values()))) {
      pj.skonczony = true;
      if (kamera != null) for (Player p : pj.gracze) if (p != null) kamera.wylacz(p);
      if (hud != null) for (Player p : pj.gracze) if (p != null) hud.ukryj(p);
      if (pj.npcNazwa != null && pj.ciala[1] != null) Mythic.usun(pj.ciala[1]);
      for (Player p : pj.gracze) if (p != null) wgGracza.remove(p.getUniqueId());
      cofnijTeleport(pj);
    }
  }

  // ------------------------------------------------------------------ tylko ruchy zadają obrażenia

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void naturalneObrazenia(EntityDamageEvent e) {
    if (cialoWPojedynku(e.getEntity())) e.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void graczBijeStwora(EntityDamageByEntityEvent e) {
    Entity d = e.getDamager();
    if (d instanceof Projectile pr && pr.getShooter() instanceof Entity s) d = s;
    if (cialoWPojedynku(d) && e.getEntity() instanceof Player) e.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.LOW)
  public void wyjscie(PlayerQuitEvent e) { przerwij(e.getPlayer()); }

  @EventHandler(priority = EventPriority.LOW)
  public void smierc(PlayerDeathEvent e) { przerwij(e.getPlayer()); }
}
