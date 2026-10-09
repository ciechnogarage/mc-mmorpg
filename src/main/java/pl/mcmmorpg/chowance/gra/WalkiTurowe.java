package pl.mcmmorpg.chowance.gra;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.api.event.WalkaZakonczonaEvent;
import pl.mcmmorpg.chowance.dane.ProfilDane;
import pl.mcmmorpg.chowance.integracja.Mythic;
import pl.mcmmorpg.chowance.integracja.PrzedmiotMmo;
import pl.mcmmorpg.chowance.logika.Generator;
import pl.mcmmorpg.chowance.logika.Rozwoj;
import pl.mcmmorpg.chowance.logika.SilnikWalki;
import pl.mcmmorpg.chowance.logika.SilnikWalki.Akcja;
import pl.mcmmorpg.chowance.logika.SilnikWalki.Zdarzenie;
import pl.mcmmorpg.chowance.logika.Wojownik;
import pl.mcmmorpg.chowance.logika.WzorLapania;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.Osobnik;
import pl.mcmmorpg.chowance.model.Przedmioty;
import pl.mcmmorpg.chowance.model.Ruch;
import pl.mcmmorpg.chowance.model.Trener;
import pl.mcmmorpg.chowance.model.Wieza;

/**
 * Walka turowa (SPEC §6.3): obie strony stoją, menu tur w natywnym oknie Papera.
 * Rodzaje: PVP (zaproszenie, rankingowa), DZIKI (z przyciskiem Pieczęć), TRENER/MISTRZ (odznaki),
 * WIEZA (fale), TURNIEJ. Silnik tur: logika/SilnikWalki — tu tylko sesje, ciała, okna i nagrody.
 */
public final class WalkiTurowe implements Listener {
  public enum Rodzaj { PVP, DZIKI, TRENER, WIEZA, TURNIEJ }

  private final Chowance c;
  private final Okna okna;
  private final Druzyny druzyny;
  private final Przywolania przywolania;
  private final Dzikie dzikie;
  private final Ciala ciala;
  private final Ranking ranking;
  private Lapanie lapanie;
  private Interakcje interakcje;
  /** callback turnieju: (zwycięzca, przegrany) */
  public BiConsumer<UUID, UUID> poMeczuTurnieju = (a, b) -> {};
  /** Widok areny (ustawiany przez plugin); null = bez kamery. */
  public Kamera kamera;
  public Scena scena;
  public HudWalki hud;

  private final Map<UUID, Walka> wgGracza = new ConcurrentHashMap<>();

  public WalkiTurowe(Chowance c, Okna okna, Druzyny druzyny, Przywolania przywolania, Dzikie dzikie, Ciala ciala, Ranking ranking) {
    this.c = c; this.okna = okna; this.druzyny = druzyny; this.przywolania = przywolania;
    this.dzikie = dzikie; this.ciala = ciala; this.ranking = ranking;
  }

  public void polacz(Lapanie l, Interakcje i) { this.lapanie = l; this.interakcje = i; }

  public boolean wWalce(Player p) { return wgGracza.containsKey(p.getUniqueId()); }

  public Walka walka(Player p) { return wgGracza.get(p.getUniqueId()); }

  public int aktywnych() { return (int) wgGracza.values().stream().distinct().count(); }

  /** Sesja jednej walki. Strona 0 = inicjator (zawsze gracz), strona 1 = gracz albo NPC/dziki. */
  public final class Walka {
    final Rodzaj rodzaj;
    final Player[] gracze = new Player[2];
    final LivingEntity[] ciala = new LivingEntity[2];
    final boolean[] cialoNasze = new boolean[2];
    final Akcja[] wybor = new Akcja[2];
    final long[] terminWyboru = new long[2];
    final int[] przeterminowania = new int[2];
    final List<String> log = new ArrayList<>();
    SilnikWalki silnik;
    LivingEntity dziki;
    Osobnik dzikiOsobnik;
    Trener trener;
    Wieza wieza;
    int fala;
    boolean rankingowa;
    Location srodek;
    boolean skonczona;
    /** Stały punkt odniesienia areny (gracz w trybie kamery nie stoi już przy stworach). */
    Location kotwica;
    Vector kier;
    /** Miejsca stworów wybrane przez {@link Arena} (null = stare ustawienie przed graczem). */
    Location[] miejscaAreny;
    /** Trwa odgrywanie tury — wybory i limity czasu wstrzymane. */
    boolean odtwarzanie, poPauzie;
    /** exp zdobyty przez stronę w tej walce (podsumowanie na końcu) */
    final long[] expZdobyty = new long[2];
    /** SI trenera: eliksiry do użycia i tura ostatniej zmiany stwora */
    int eliksiryNpc = -1, turaZmianyNpc = -10;
    /** podpowiedź „słabnie — pieczęć!” już pokazana */
    boolean podpowiedzSlaby;
    /** podwójne [8] = ucieczka/poddanie (ochrona przed przypadkowym klawiszem) */
    final long[] klikWyjscia = new long[2];
    /** slot w ręce przed walką (na czas walki slot 9, żeby klawisze 1–8 zawsze działały) */
    final int[] slotPrzed = {-1, -1};
    /** opis bieżącego kroku odtwarzania (pasek u góry) */
    String krok = "";
    /** osobniki gracza, które walczyły (do exp) */
    final java.util.Set<String>[] uczestnicy = new java.util.Set[] {new java.util.HashSet<String>(), new java.util.HashSet<String>()};

    Walka(Rodzaj r) { this.rodzaj = r; }

    public String opisPrzeciwnika(int s) {
      if (gracze[1 - s] != null) return gracze[1 - s].getName();
      if (trener != null) return trener.nazwa();
      return "dziki " + (silnik == null ? "" : silnik.strony[1].akt().nazwa());
    }
  }

  // ------------------------------------------------------------------ start

  private boolean mozeWalczyc(Player p, boolean komunikat) {
    ProfilDane d = c.dane(p);
    if (d == null) { if (komunikat) c.blad(p, "Wybierz postać, zanim zaczniesz walkę."); return false; }
    if (wWalce(p)) { if (komunikat) c.blad(p, "Już walczysz."); return false; }
    for (Osobnik o : d.druzyna) if (!druzyny.zemdlony(p, o)) return true;
    if (komunikat) c.blad(p, "Nie masz żadnego sprawnego chowańca w drużynie.");
    return false;
  }

  private SilnikWalki.Strona strona(Player p, ProfilDane d) {
    List<Wojownik> l = new ArrayList<>();
    int akt = -1;
    Osobnik aktywny = przywolania.osobnikAktywny(p);
    for (Osobnik o : d.druzyna) {
      Wojownik w = druzyny.wojownik(p, o);
      l.add(w);
      if (aktywny != null && aktywny.id.equals(o.id) && !w.zemdlony()) akt = l.size() - 1;
    }
    SilnikWalki.Strona s = new SilnikWalki.Strona(p.getName(), l, false);
    if (akt >= 0) s.aktywny = akt;
    return s;
  }

  public boolean startPvp(Player a, Player b, Rodzaj rodzaj) {
    if (!mozeWalczyc(a, true)) return false;
    if (!mozeWalczyc(b, false)) { c.blad(a, b.getName() + " nie może teraz walczyć."); return false; }
    if (!a.getWorld().equals(b.getWorld()) || a.getLocation().distance(b.getLocation()) > c.ust.walkaZasieg) {
      c.blad(a, "Przeciwnik jest za daleko."); return false;
    }
    Walka w = new Walka(rodzaj);
    w.gracze[0] = a; w.gracze[1] = b;
    w.rankingowa = true;
    SilnikWalki.Strona sa = strona(a, c.dane(a));
    SilnikWalki.Strona sb = strona(b, c.dane(b));
    w.silnik = new SilnikWalki(c.rej, sa, sb, ThreadLocalRandom.current());
    w.srodek = a.getLocation().clone().add(b.getLocation()).multiply(0.5);
    return uruchom(w);
  }

  public boolean startDziki(Player p, LivingEntity le) {
    if (!mozeWalczyc(p, true)) return false;
    Osobnik o = dzikie.osobnik(le);
    if (o == null || dzikie.zajete.contains(le.getUniqueId())) { c.blad(p, "Z tym stworem nie da się teraz walczyć."); return false; }
    Gatunek g = c.gatunek(o);
    if (g == null) return false;
    Walka w = new Walka(Rodzaj.DZIKI);
    w.gracze[0] = p;
    w.dziki = le;
    w.dzikiOsobnik = o;
    Wojownik ww = new Wojownik(c.rej, o, 0, le.getHealth());
    SilnikWalki.Strona sb = new SilnikWalki.Strona(g.nazwa(), new ArrayList<>(List.of(ww)), true);
    sb.dziki = true;
    w.silnik = new SilnikWalki(c.rej, strona(p, c.dane(p)), sb, ThreadLocalRandom.current());
    w.srodek = p.getLocation().clone().add(le.getLocation()).multiply(0.5);
    dzikie.zajete.add(le.getUniqueId());
    c.dane(p).oznaczWidziany(g.id());
    return uruchom(w);
  }

  public boolean startTrener(Player p, String trenerId, Wieza wieza, int fala) {
    Trener t = c.rej.trenerzy().get(trenerId);
    if (t == null) { c.blad(p, "Nieznany trener " + trenerId); return false; }
    if (!mozeWalczyc(p, true)) return false;
    ProfilDane d = c.dane(p);
    if (d.odznaki.size() < t.wymaganeOdznaki()) { c.blad(p, t.nazwa() + " przyjmie wyzwanie dopiero od " + t.wymaganeOdznaki() + " odznak."); return false; }
    List<Wojownik> l = druzynaTrenera(t);
    Walka w = new Walka(wieza != null ? Rodzaj.WIEZA : Rodzaj.TRENER);
    w.gracze[0] = p;
    w.trener = t;
    w.wieza = wieza;
    w.fala = fala;
    w.silnik = new SilnikWalki(c.rej, strona(p, d), new SilnikWalki.Strona(t.nazwa(), l, true), ThreadLocalRandom.current());
    w.srodek = p.getLocation().clone().add(p.getLocation().getDirection().setY(0).normalize().multiply(3));
    return uruchom(w);
  }

  /** Drużyna trenera NPC z trenerzy/*.yml (wspólne dla walki turowej i pojedynku real-time z NPC). */
  public List<Wojownik> druzynaTrenera(Trener t) {
    List<Wojownik> l = new ArrayList<>();
    for (Trener.Czlonek cz : t.druzyna()) {
      Gatunek g = c.rej.gatunek(cz.gatunek());
      if (g == null) continue;
      Osobnik o = Generator.nowy(c.rej, g, cz.poziom(), false, ThreadLocalRandom.current());
      java.util.Arrays.fill(o.geny, cz.geny());
      if (!cz.ruchy().isEmpty()) o.ruchy = new ArrayList<>(cz.ruchy());
      if (cz.cecha() != null) o.cecha = cz.cecha();
      if (cz.charakter() != null) o.charakter = cz.charakter();
      l.add(new Wojownik(c.rej, o, 0, Double.MAX_VALUE));
    }
    return l;
  }

  private boolean uruchom(Walka w) {
    // Klient QA: menu (np. Menażeria, z której gracz kliknął „Walcz z dzikim”) zostawało otwarte na wstępie walki.
    for (Player p : w.gracze) if (p != null) p.closeInventory();
    // Slot 9 w ręce: Paper nie zgłasza PlayerItemHeldEvent przy klawiszu slotu, który już jest wybrany —
    // gracz trzymający slot 1 nie mógłby wybrać ruchu [1] (eval 2026-09-27).
    for (int s = 0; s < 2; s++) if (w.gracze[s] != null) { w.slotPrzed[s] = w.gracze[s].getInventory().getHeldItemSlot(); w.gracze[s].getInventory().setHeldItemSlot(8); }
    for (Player p : w.gracze) if (p != null) {
      przywolania.odwolaj(p, true);
      wgGracza.put(p.getUniqueId(), w);
    }
    for (int s = 0; s < 2; s++) w.uczestnicy[s].add(w.silnik.strony[s].akt().o.id);
    ustalKotwice(w);
    postawCiala(w);
    dodajLog(w, "<gold>Walka: " + w.silnik.strony[0].nazwa + " vs " + w.silnik.strony[1].nazwa);
    for (int s = 0; s < 2; s++) if (w.gracze[s] != null) w.gracze[s].playSound(w.gracze[s].getLocation(), Sound.EVENT_RAID_HORN, 0.6f, 1.4f);
    // Wstęp: kamera na arenę, oba stwory w kadrze, dopiero potem pierwsze okno wyboru.
    boolean zKamera = false;
    for (int s = 0; s < 2; s++) if (w.gracze[s] != null && kamera != null && kamera.wlacz(w.gracze[s], okoKamery(w, s), celKamery(w, s))) zKamera = true;
    odslon(w);
    for (int s = 0; s < 2; s++) if (w.gracze[s] != null) scena.napis(w.gracze[s], "<gold>" + w.silnik.strony[0].nazwa + " <gray>vs <gold>" + w.silnik.strony[1].nazwa, 40);
    if (zKamera) {
      w.odtwarzanie = true;
      Bukkit.getScheduler().runTaskLater(c.plugin, () -> { w.odtwarzanie = false; nowaTura(w); }, 26L);
    } else {
      nowaTura(w);
    }
    return true;
  }

  // ------------------------------------------------------------------ ciała

  private void ustalKotwice(Walka w) {
    Location a = w.gracze[0].getLocation().clone();
    Vector kier;
    if (w.gracze[1] != null) kier = w.gracze[1].getLocation().toVector().subtract(a.toVector());
    else if (w.dziki != null) kier = w.dziki.getLocation().toVector().subtract(a.toVector());
    else kier = a.getDirection();
    kier.setY(0);
    if (kier.lengthSquared() < 0.01) kier = new Vector(1, 0, 0);
    w.kotwica = a;
    w.kier = kier.normalize();
    if (!c.ust.arenaSzukaj) return;
    boolean pvp = w.gracze[1] != null;
    Arena.Plan plan = Arena.znajdz(a, w.kier, odlegloscCial(), c.ust.arenaZasieg, 0,
        (m0, m1) -> pvp ? new Location[] {okoTury(m0, m1), okoTury(m1, m0)} : new Location[] {okoTury(m0, m1)});
    if (plan == null) { c.log.fine("Arena: brak dobrego miejsca przy " + a.toVector() + " — ustawienie przed graczem"); return; }
    w.kier = plan.kier();
    w.miejscaAreny = plan.miejsca();
    if (c.ust.debug) c.log.info("Arena: " + w.gracze[0].getName() + " kara=" + String.format(java.util.Locale.ROOT, "%.1f", plan.kara()) + " srodek=" + plan.srodek().toVector());
  }

  private double odlegloscCial() { return c.ust.kamera ? Math.min(c.ust.odlegloscCial, 3.5) : c.ust.odlegloscCial; }

  /** Oko kamery za własnym stworem, z ukosa (wspólne dla wyboru areny i dla samej kamery). */
  static Location okoTury(Location moj, Location jego) {
    Vector wstecz = moj.toVector().subtract(jego.toVector()).setY(0);
    if (wstecz.lengthSquared() < 1e-4) wstecz = new Vector(1, 0, 0);
    wstecz.normalize();
    Vector bok = new Vector(-wstecz.getZ(), 0, wstecz.getX());
    // z ukosa (klient QA: prosto zza stwora przeciwnik był kropką w oddali, własny stwór zasłaniał kadr)
    return moj.clone().add(wstecz.multiply(2.4)).add(bok.multiply(3.2)).add(0, 2.3, 0);
  }

  /** Kamera strony s: za własnym stworem, nad nim, lekko z boku — patrzy na przeciwnika (widok „trenera”). */
  private Location okoKamery(Walka w, int s) {
    return okoTury(miejsce(w, s), miejsce(w, 1 - s));
  }

  private Location celKamery(Walka w, int s) {
    Location moj = miejsce(w, s), jego = miejsce(w, 1 - s);
    return moj.clone().add(jego.toVector().subtract(moj.toVector()).multiply(0.5)).add(0, 0.6, 0);
  }

  /**
   * Stwór stoi na gruncie, nie w skale ani w powietrzu: od 2 bl. wyżej do 3 niżej szukamy miejsca z wolnymi
   * 2 blokami i podłożem pod spodem (world1: kieszeń skalna przy spawnie).
   */
  public static Location naGruncie(Location l) {
    for (int dy = 2; dy >= -3; dy--) {
      Location x = l.clone().add(0, dy, 0);
      x.setY(Math.floor(x.getY()));
      if (x.getBlock().isPassable() && x.clone().add(0, 1, 0).getBlock().isPassable() && !x.clone().add(0, -1, 0).getBlock().isPassable()) return x;
    }
    return l;
  }

  private Location miejsce(Walka w, int s) {
    if (w.kotwica == null) ustalKotwice(w);
    if (w.miejscaAreny != null) return w.miejscaAreny[s].clone();
    Location a = w.kotwica;
    Vector kier = w.kier.clone();
    Location baza = a.clone().add(kier.clone().multiply(1.8));
    // W widoku areny stwory bliżej siebie (3,5 bl.), żeby oba mieściły się w kadrze.
    double odl = odlegloscCial();
    Location l = s == 0 ? baza : baza.clone().add(kier.clone().multiply(odl));
    l.setDirection(s == 0 ? kier : kier.clone().multiply(-1));
    return naGruncie(l);
  }

  private void postawCiala(Walka w) {
    for (int s = 0; s < 2; s++) postawCialo(w, s);
  }

  /** Przeszkody między kamerą a stworami znikają u walczących (Widok). */
  private void odslon(Walka w) {
    if (kamera == null) return;
    List<LivingEntity> l = new ArrayList<>();
    for (LivingEntity e : w.ciala) if (e != null) l.add(e);
    for (Player p : w.gracze) if (p != null) kamera.odslon(p, l);
  }

  private void postawCialo(Walka w, int s) {
    if (w.ciala[s] != null) scena.usunEtykiete(w.ciala[s]);
    if (w.ciala[s] != null && w.cialoNasze[s]) Mythic.usun(w.ciala[s]);
    Wojownik akt = w.silnik.strony[s].akt();
    if (s == 1 && w.dziki != null) {
      w.ciala[1] = w.dziki;
      w.cialoNasze[1] = false;
      w.dziki.setAI(false);
      w.dziki.teleport(miejsce(w, 1));
      odswiezCialo(w, 1);
      odslon(w);
      return;
    }
    Gatunek g = akt.g;
    Ciala.Cialo cialo = ciala.postaw(g, g.mobOswojony(akt.o.lsniacy), miejsce(w, s), akt.o.poziom);
    if (cialo == null) { w.ciala[s] = null; return; }
    LivingEntity e = cialo.encja();
    e.setAI(false);
    e.setInvulnerable(true);
    e.getPersistentDataContainer().set(c.kCialoWalki, PersistentDataType.BYTE, (byte) 1);
    w.ciala[s] = e;
    w.cialoNasze[s] = true;
    odswiezCialo(w, s);
    if (w.gracze[s] != null) Mythic.sygnal(e, "PRZYWOLANIE", w.gracze[s]);
    odslon(w);
  }

  private void odswiezCialo(Walka w, int s) {
    LivingEntity e = w.ciala[s];
    if (e == null || !e.isValid()) return;
    Wojownik akt = w.silnik.strony[s].akt();
    akt.srodowisko = Srodowisko.z(e);
    ciala.ustawZdrowie(e, akt.hpMax, Math.max(0.5, akt.hp));
    ciala.nazwij(e, akt.o, pasekHp(akt, 10));
    if (scena != null) scena.pasek(e, akt, akt.hp);
  }

  static String pasekHp(Wojownik w, int dl) {
    double pr = w.hpProcent();
    int pelne = (int) Math.ceil(pr / 100.0 * dl);
    String kol = pr > 50 ? "<green>" : pr > 20 ? "<yellow>" : "<red>";
    return kol + "▮".repeat(Math.max(0, pelne)) + "<dark_gray>" + "▮".repeat(Math.max(0, dl - pelne)) + "</dark_gray> <gray>" + (int) Math.ceil(w.hp) + "/" + w.hpMax;
  }

  public boolean jestCialemWalki(org.bukkit.entity.Entity e) {
    return e.getPersistentDataContainer().has(c.kCialoWalki, PersistentDataType.BYTE);
  }

  // ------------------------------------------------------------------ tura

  private void nowaTura(Walka w) {
    if (w.skonczona) return;
    for (int s = 0; s < 2; s++) {
      w.wybor[s] = null;
      w.terminWyboru[s] = System.currentTimeMillis() + c.ust.czasTuryS * 1000L;
    }
    for (int s = 0; s < 2; s++) {
      if (w.silnik.wymagaZmiany(s)) {
        if (w.gracze[s] == null) {
          w.silnik.wymuszonaZmiana(s, w.silnik.strony[s].pierwszyZdolny());
          postawCialo(w, s);
          dodajLog(w, "<gray>" + w.silnik.strony[s].nazwa + " wysyła: " + w.silnik.strony[s].akt().nazwa());
        } else {
          oknoZmiany(w, s, true);
          return;
        }
      }
    }
    for (int s = 0; s < 2; s++) {
      if (w.gracze[s] == null) w.wybor[s] = akcjaSI(w, s);
      else pokazTure(w, s);
    }
    sprobujRozstrzygnac(w);
  }

  /**
   * SI trenera ponad wybór ruchu (recenzja R-08): eliksir przy < 25% HP (Mistrz 2, trener 1 na walkę) i zmiana
   * stwora, gdy obecny jest bardzo podatny na ruchy przeciwnika, a ktoś w drużynie nie (najwyżej co 3 tury).
   */
  private Akcja akcjaSI(Walka w, int s) {
    SilnikWalki.Strona st = w.silnik.strony[s];
    Wojownik ja = st.akt(), on = w.silnik.strony[1 - s].akt();
    if (w.trener != null) {
      if (w.eliksiryNpc < 0) w.eliksiryNpc = w.trener.rodzaj() == pl.mcmmorpg.chowance.model.Trener.Rodzaj.MISTRZ ? 2 : 1;
      if (w.eliksiryNpc > 0 && ja.hpProcent() < 25 && ThreadLocalRandom.current().nextDouble() < 0.75) {
        w.eliksiryNpc--;
        int przed = (int) Math.ceil(ja.hp);
        ja.hp = Math.min(ja.hpMax, ja.hp + ja.hpMax * 0.5);
        ja.status = null;
        int ile = (int) Math.ceil(ja.hp) - przed;
        dodajLog(w, st.nazwa + " podaje eliksir: " + ja.nazwa() + " +" + ile + " HP.");
        if (w.ciala[s] != null) {
          scena.liczba(w.ciala[s], "<green><b>+" + ile + "</b>\n<gray>eliksir", 1.5f);
          w.ciala[s].getWorld().spawnParticle(Particle.HEART, w.ciala[s].getLocation().add(0, 1, 0), 8, 0.4, 0.3, 0.4, 0);
          w.ciala[s].getWorld().playSound(w.ciala[s].getLocation(), Sound.ENTITY_WITCH_DRINK, 0.8f, 1.2f);
        }
        odswiezCialo(w, s);
        return new Akcja.Pomin("eliksir trenera");
      }
      if (w.silnik.nrTury() - w.turaZmianyNpc >= 3 && ja.hpProcent() < 70) {
        double grozba = najgrozniej(on, ja);
        if (grozba >= 2) {
          for (int i = 0; i < st.druzyna.size(); i++) {
            if (st.mozeZmienicNa(i) && najgrozniej(on, st.druzyna.get(i)) <= 1) {
              w.turaZmianyNpc = w.silnik.nrTury();
              return new Akcja.Zmiana(i);
            }
          }
        }
      }
    }
    return new Akcja.UzyjRuchu(w.silnik.wyborSI(s));
  }

  /** Największy mnożnik typu, jaki atakujący ma na obrońcę swoimi ruchami (bez statusowych). */
  private double najgrozniej(Wojownik atak, Wojownik obr) {
    double m = 1;
    for (String id : atak.o.ruchy) {
      Ruch r = c.rej.ruch(id);
      if (r != null && r.kategoria() != pl.mcmmorpg.chowance.model.Kategoria.STATUS) m = Math.max(m, c.rej.typy().mnoznik(r.typ(), obr.g.typy()));
    }
    return m;
  }

  private List<String> naglowek(Walka w, int s) {
    Wojownik ja = w.silnik.strony[s].akt();
    Wojownik on = w.silnik.strony[1 - s].akt();
    List<String> t = new ArrayList<>();
    t.add("<red>" + w.silnik.strony[1 - s].nazwa + ":</red> " + c.nazwaKolor(on.o) + " <gray>Poz. " + on.o.poziom + typy(on.g) + (on.status != null ? " <light_purple>[" + on.status.nazwa() + "]" : ""));
    t.add(pasekHp(on, 20));
    t.add("<green>Ty:</green> " + c.nazwaKolor(ja.o) + " <gray>Poz. " + ja.o.poziom + typy(ja.g) + (ja.status != null ? " <light_purple>[" + ja.status.nazwa() + "]" : ""));
    t.add(pasekHp(ja, 20));
    int od = Math.max(0, w.log.size() - 6);
    StringBuilder sb = new StringBuilder("<gray>");
    for (int i = od; i < w.log.size(); i++) sb.append(w.log.get(i)).append(i < w.log.size() - 1 ? "\n" : "");
    t.add(sb.toString());
    return t;
  }

  private static String typy(Gatunek g) {
    StringBuilder sb = new StringBuilder();
    for (var t : g.typy()) sb.append(" ").append(t.kolor()).append(t.nazwa());
    return sb.append("<gray>").toString();
  }

  private boolean klawisze(Walka w, int s) {
    Player p = w.gracze[s];
    return p != null && hud != null && kamera != null && kamera.aktywna(p);
  }

  /**
   * Tura gracza: z miejsca trenera — paski u góry i klawisze 1–8, arena cały czas widoczna;
   * bez kamery — dawne okno z przyciskami.
   */
  private void pokazTure(Walka w, int s) {
    if (!klawisze(w, s)) { oknoTury(w, s); return; }
    Player p = w.gracze[s];
    hudStwory(w, s, null);
    hudMenu(w, s);
    // na pasku akcji, nie na środku ekranu — podtytuł zasłaniał stwory (recenzja R-17)
    c.pasek(p, "<yellow>Twój ruch! <gray>klawisze <white>1–4</white> — ruch, <white>5–8</white> — reszta");
    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.6f);
    kamera.spojrz(p, celKamery(w, s));
  }

  private void hudStwory(Walka w, int s, Scena.Stan st) {
    Player p = w.gracze[s];
    if (p == null || hud == null) return;
    Wojownik ja = st != null && st.pokazany[s] != null ? st.pokazany[s] : w.silnik.strony[s].akt();
    Wojownik on = st != null && st.pokazany[1 - s] != null ? st.pokazany[1 - s] : w.silnik.strony[1 - s].akt();
    double hpJa = st != null ? st.hp.getOrDefault(ja, ja.hp) : ja.hp;
    double hpOn = st != null ? st.hp.getOrDefault(on, on.hp) : on.hp;
    hud.stwory(p, ja, hpJa, on, hpOn, w.silnik.strony[1 - s].nazwa);
  }

  private void hudMenu(Walka w, int s) {
    Player p = w.gracze[s];
    if (p == null || hud == null) return;
    long zostalo = Math.max(0, w.terminWyboru[s] - System.currentTimeMillis());
    float postep = (float) zostalo / Math.max(1, c.ust.czasTuryS * 1000L);
    if (w.odtwarzanie) {
      hud.klawisze(p, List.of());
      hud.menu(p, "<gray>Tura " + (w.silnik.nrTury()) + ": <white>" + (w.krok == null ? "" : w.krok), 1f, net.kyori.adventure.bossbar.BossBar.Color.WHITE);
      return;
    }
    if (w.wybor[s] != null) {
      hud.klawisze(p, List.of("<red>[8]×2</red> <gray>" + (w.rodzaj == Rodzaj.DZIKI ? "Ucieczka" : "Poddaj")));
      hud.menu(p, "<gray>Czekasz na ruch przeciwnika…", postep, net.kyori.adventure.bossbar.BossBar.Color.WHITE);
      return;
    }
    List<String> k = new ArrayList<>(hud.ruchyTury(w.silnik.strony[s].akt(), w.silnik.strony[1 - s].akt(), r -> w.silnik.strony[s].akt().odnowienieTur.getOrDefault(r.id(), 0)));
    k.add("<aqua>[5]</aqua> <gray>Zmień stwora");
    k.add("<yellow>[6]</yellow> <gray>Przedmiot");
    if (w.rodzaj == Rodzaj.DZIKI) k.add("<gold>[7]</gold> <gray>Pieczęć" + etykietaSzansy(szansaPieczeci(w, s)));
    k.add("<red>[8]</red> <gray>" + (w.rodzaj == Rodzaj.DZIKI ? "Ucieczka" : "Poddaj"));
    hud.klawisze(p, k);
    long sek = (zostalo + 999) / 1000;
    hud.menu(p, "<gold>Tura " + (w.silnik.nrTury() + 1) + "</gold> <gray>—</gray> <yellow><b>Twój ruch</b></yellow> <gray>(" + sek + " s)",
        postep, postep < 0.25f ? net.kyori.adventure.bossbar.BossBar.Color.RED : net.kyori.adventure.bossbar.BossBar.Color.YELLOW);
  }

  private static String etykietaSzansy(double sz) {
    if (sz < 0) return " <dark_gray>(brak)";
    int pr = (int) Math.round(sz * 100);
    return " " + (pr >= 50 ? "<green>" : pr >= 25 ? "<yellow>" : "<red>") + "~" + pr + "%";
  }

  /** Klawisze 1–8 w walce turowej (slot hotbara się nie zmienia). */
  @EventHandler(priority = EventPriority.HIGH)
  public void klawisz(org.bukkit.event.player.PlayerItemHeldEvent e) {
    Player p = e.getPlayer();
    Walka w = walka(p);
    if (w == null) return;
    int s = w.gracze[0] != null && w.gracze[0].getUniqueId().equals(p.getUniqueId()) ? 0 : 1;
    if (!klawisze(w, s)) return;
    e.setCancelled(true);
    int k = e.getNewSlot();
    if (w.skonczona || w.odtwarzanie) return;
    if (k == 7) {
      long teraz = System.currentTimeMillis();
      if (teraz - w.klikWyjscia[s] < 1500) { ucieczka(w, s); return; }
      w.klikWyjscia[s] = teraz;
      scena.napis(p, "<red>Naciśnij [8] jeszcze raz: " + (w.rodzaj == Rodzaj.DZIKI ? "ucieczka" : "poddanie"), 30);
      return;
    }
    if (w.wybor[s] != null) return;
    if (w.silnik.wymagaZmiany(s)) { oknoZmiany(w, s, true); return; }
    if (k <= 3) {
      if (k >= w.silnik.strony[s].akt().o.ruchy.size()) return;
      if (!w.silnik.gotowy(s, k)) {
        c.pasek(p, "<gray>" + nazwaRuchu(w, s, k) + " odnawia się jeszcze " + w.silnik.odnawiaSie(s, k) + " t.");
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
        return;
      }
      p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
      wybierz(w, s, new Akcja.UzyjRuchu(k));
    } else if (k == 4) oknoZmiany(w, s, false);
    else if (k == 5) oknoPrzedmiotu(w, s);
    else if (k == 6 && w.rodzaj == Rodzaj.DZIKI) oknoPieczeci(w, s);
  }

  private String nazwaRuchu(Walka w, int s, int k) {
    Ruch r = c.rej.ruch(w.silnik.strony[s].akt().o.ruchy.get(k));
    return r == null ? "?" : r.nazwa();
  }

  /**
   * Okna walki zamykają się po kliknięciu (afterAction CLOSE). Przy WAIT_FOR_RESPONSE klient pokazywał ekran
   * „Waiting for Server”, który zasłaniał odgrywaną turę (klient QA, 2026-09-27).
   */
  private void oknoTury(Walka w, int s) {
    Player p = w.gracze[s];
    Wojownik ja = w.silnik.strony[s].akt();
    List<Okna.Przycisk> pr = new ArrayList<>();
    for (int i = 0; i < ja.o.ruchy.size(); i++) {
      Ruch r = c.rej.ruch(ja.o.ruchy.get(i));
      if (r == null) continue;
      int idx = i;
      double sk = r.kategoria() == pl.mcmmorpg.chowance.model.Kategoria.STATUS ? 1 : c.rej.typy().mnoznik(r.typ(), w.silnik.strony[1 - s].akt().g.typy());
      String tip = r.typ().kolor() + r.typ().nazwa() + "<gray> · " + r.kategoria().name().toLowerCase()
          + (r.moc() > 0 ? " · moc " + r.moc() : "") + " · celność " + (r.celnosc() == 0 ? "—" : r.celnosc()) + (sk >= 2 ? "\n<green>Bardzo skuteczny" : sk < 1 ? "\n<red>Mało skuteczny" : "")
          + (r.opis().isBlank() ? "" : "\n<gray>" + r.opis());
      int od = w.silnik.odnawiaSie(s, i);
      if (SilnikWalki.turyOdnowienia(r) > 0) tip += "\n<gray>Po użyciu odpoczywa " + SilnikWalki.turyOdnowienia(r) + " t.";
      pr.add(new Okna.Przycisk(od > 0 ? "<dark_gray>" + r.nazwa() + " (" + od + " t.)" : r.typ().kolor() + r.nazwa(), tip, 150,
          x -> { if (w.silnik.gotowy(s, idx)) wybierz(w, s, new Akcja.UzyjRuchu(idx)); else pokazTure(w, s); }));
    }
    pr.add(new Okna.Przycisk("<aqua>Zmień", "Wyślij innego chowańca (zajmuje turę)", 100, x -> oknoZmiany(w, s, false)));
    pr.add(new Okna.Przycisk("<yellow>Przedmiot", "Eliksir z ekwipunku (zajmuje turę)", 100, x -> oknoPrzedmiotu(w, s)));
    if (w.rodzaj == Rodzaj.DZIKI) pr.add(new Okna.Przycisk("<gold>Pieczęć", "Spróbuj związać dzikiego (zajmuje turę)", 100, x -> oknoPieczeci(w, s)));
    String wyj = w.rodzaj == Rodzaj.DZIKI ? "Uciekaj" : "Poddaj się";
    Okna.Przycisk wyjscie = new Okna.Przycisk("<red>" + wyj, w.rodzaj == Rodzaj.DZIKI ? "Szansa zależy od szybkości" : "Oddajesz walkę", 100, x -> ucieczka(w, s));
    okna.pokaz(p, "<gold>Walka — tura " + (w.silnik.nrTury() + 1), naglowek(w, s), pr, wyjscie, 2, false, false);
  }

  private void oknoZmiany(Walka w, int s, boolean wymuszona) {
    Player p = w.gracze[s];
    SilnikWalki.Strona st = w.silnik.strony[s];
    List<Okna.Przycisk> pr = new ArrayList<>();
    for (int i = 0; i < st.druzyna.size(); i++) {
      Wojownik x = st.druzyna.get(i);
      if (!st.mozeZmienicNa(i)) continue;
      int idx = i;
      pr.add(new Okna.Przycisk(c.nazwaKolor(x.o) + " <gray>" + (int) Math.ceil(x.hp) + "/" + x.hpMax, "Poz. " + x.o.poziom + typy(x.g), 150,
          y -> {
            if (wymuszona) {
              List<Zdarzenie> z = w.silnik.wymuszonaZmiana(s, idx);
              w.uczestnicy[s].add(w.silnik.strony[s].akt().o.id);
              postawCialo(w, s);
              for (Zdarzenie e : z) dodajLog(w, e.tekst());
              nowaTura(w);
            } else {
              wybierz(w, s, new Akcja.Zmiana(idx));
            }
          }));
    }
    List<String> tresc = naglowek(w, s);
    tresc.add(0, wymuszona ? "<red>Twój chowaniec zemdlał — wybierz następnego." : "<aqua>Kogo wysłać do walki?");
    Okna.Przycisk wroc = wymuszona ? new Okna.Przycisk("<red>Poddaj się", "", 100, x -> ucieczka(w, s))
        : new Okna.Przycisk("<gray>Wróć", "", 100, x -> pokazTure(w, s));
    okna.pokaz(p, "<aqua>Zmiana", tresc, pr, wroc, 2, false, false);
  }

  private void oknoPrzedmiotu(Walka w, int s) {
    Player p = w.gracze[s];
    Map<String, Integer> ile = new LinkedHashMap<>();
    for (ItemStack it : p.getInventory().getContents()) {
      String id = PrzedmiotMmo.id(it);
      if (id != null && c.rej.przedmioty().eliksiry().containsKey(id)) ile.merge(id, it.getAmount(), Integer::sum);
    }
    List<Okna.Przycisk> pr = new ArrayList<>();
    for (Map.Entry<String, Integer> e : ile.entrySet()) {
      Przedmioty.Eliksir el = c.rej.przedmioty().eliksiry().get(e.getKey());
      pr.add(new Okna.Przycisk("<yellow>" + c.nazwyMmo.nazwa(e.getKey()) + " <gray>x" + e.getValue(),
          (el.wskrzesza() ? "Wskrzesza. " : "") + "Leczy " + (int) el.leczenieProcent() + "%" + (el.leczyStatus() ? ", zdejmuje stan" : ""), 150,
          x -> uzyjEliksiruWWalce(w, s, el)));
    }
    List<String> tresc = naglowek(w, s);
    if (pr.isEmpty()) tresc.add(0, "<red>Nie masz żadnych eliksirów.");
    okna.pokaz(p, "<yellow>Przedmiot", tresc, pr, new Okna.Przycisk("<gray>Wróć", "", 100, x -> pokazTure(w, s)), 2, false, false);
  }

  private void uzyjEliksiruWWalce(Walka w, int s, Przedmioty.Eliksir el) {
    Player p = w.gracze[s];
    Wojownik cel = w.silnik.strony[s].akt();
    ItemStack it = znajdzPrzedmiot(p, el.id());
    if (it == null) { pokazTure(w, s); return; }
    if (cel.zemdlony() && !el.wskrzesza()) { pokazTure(w, s); return; }
    it.setAmount(it.getAmount() - 1);
    double przed = cel.hp;
    cel.hp = Math.min(cel.hpMax, cel.hp + cel.hpMax * el.leczenieProcent() / 100.0);
    if (el.leczyStatus()) cel.status = null;
    dodajLog(w, cel.nazwa() + " dostaje eliksir (+" + (int) Math.round(cel.hp - przed) + " HP).");
    odswiezCialo(w, s);
    wybierz(w, s, new Akcja.Pomin("przedmiot"));
  }

  private static ItemStack znajdzPrzedmiot(Player p, String id) {
    for (ItemStack it : p.getInventory().getContents()) if (id.equals(PrzedmiotMmo.id(it))) return it;
    return null;
  }

  private void oknoPieczeci(Walka w, int s) {
    Player p = w.gracze[s];
    Map<String, Integer> ile = new LinkedHashMap<>();
    for (ItemStack it : p.getInventory().getContents()) {
      Przedmioty.Pieczec pc = lapanie.pieczec(it);
      if (pc != null) ile.merge(pc.id(), it.getAmount(), Integer::sum);
    }
    List<Okna.Przycisk> pr = new ArrayList<>();
    for (Map.Entry<String, Integer> e : ile.entrySet()) {
      pr.add(new Okna.Przycisk("<gold>" + c.nazwyMmo.nazwa(e.getKey()) + " <gray>x" + e.getValue(), "", 150, x -> pieczecWWalce(w, s, e.getKey())));
    }
    List<String> tresc = naglowek(w, s);
    if (pr.isEmpty()) tresc.add(0, "<red>Nie masz pieczęci.");
    okna.pokaz(p, "<gold>Pieczęć", tresc, pr, new Okna.Przycisk("<gray>Wróć", "", 100, x -> pokazTure(w, s)), 2, false, false);
  }

  private void pieczecWWalce(Walka w, int s, String pieczecId) {
    Player p = w.gracze[s];
    Przedmioty.Pieczec pc = c.rej.przedmioty().pieczecie().get(pieczecId);
    ItemStack it = znajdzPrzedmiot(p, pieczecId);
    Wojownik dz = w.silnik.strony[1].akt();
    Gatunek g = dz.g;
    ProfilDane d = c.dane(p);
    if (pc == null || it == null || d == null) { pokazTure(w, s); return; }
    String odmowa = odmowaPieczeci(w, p, d, pc);
    if (odmowa != null) { dodajLog(w, "<red>" + odmowa); c.blad(p, odmowa); pokazTure(w, s); return; }
    it.setAmount(it.getAmount() - 1);
    WzorLapania wz = new WzorLapania(c.ust.lapanie);
    WzorLapania.Wynik wy = wz.proba(wejsciePieczeci(w, p, d, pc), ThreadLocalRandom.current());
    dodajLog(w, wy.drzenia() > 0 ? "<gold>Pieczęć drży " + wy.drzenia() + "×…" : "<gold>Pieczęć pęka od razu!");
    w.odtwarzanie = true;
    for (Player x : w.gracze) if (x != null && x.isOnline()) okna.zamknij(x);
    // pieczęć rzuca trener, nie jego stwór (wcześniej łuk startował od chowańca i wyglądał jak atak)
    int t = scena.pieczec(w.gracze, p, w.ciala[1], wy.drzenia(), wy.sukces(), c.ust.odtwarzanieTicki);
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      if (w.skonczona) return;
      w.odtwarzanie = false;
      poPieczeci(w, s, p, pc, wy.sukces(), dz, g);
    }, t);
  }

  private String odmowaPieczeci(Walka w, Player p, ProfilDane d, Przedmioty.Pieczec pc) {
    Gatunek g = w.silnik.strony[1].akt().g;
    if (!g.lapaniePieczecia()) return g.nazwa() + " nie daje się związać pieczęcią.";
    if (!g.wymaganaPieczec().isBlank() && !g.wymaganaPieczec().equals(pc.id())) return "Działa tylko " + c.nazwyMmo.nazwa(g.wymaganaPieczec()) + ".";
    if (g.legendarny()) return "Legendarnego trzeba najpierw pokonać w otwartym świecie.";
    if (pc.rodzaj() == Przedmioty.RodzajPieczeci.LOCHOWA && !pl.mcmmorpg.chowance.integracja.Lochy.wLochu(p)) return "Pieczęć lochowa działa tylko w lochach.";
    if (d.druzyna.size() >= c.limitDruzyny(p) && d.magazyn.size() >= c.limitMagazynu(p)) return "Brak miejsca w drużynie i magazynie.";
    return null;
  }

  private WzorLapania.Wejscie wejsciePieczeci(Walka w, Player p, ProfilDane d, Przedmioty.Pieczec pc) {
    Wojownik dz = w.silnik.strony[1].akt();
    boolean warunek = switch (pc.rodzaj()) {
      case NOCNA -> { long t = p.getWorld().getTime(); yield t >= 13000 && t <= 23000; }
      case WODNA -> w.dziki.isInWater() || p.getWorld().hasStorm();
      case LOCHOWA -> true;
      // stwór, który zbiegł INNEMU graczowi (jak w łapaniu w świecie) — wcześniej w turowej zawsze false (R-21)
      case KRADZIEZY -> { var z = w.dziki == null ? null : dzikie.zbieglyOd(w.dziki); yield z != null && !z.equals(p.getUniqueId()); }
      default -> false;
    };
    return new WzorLapania.Wejscie(dz.hp, dz.hpMax, dz.g.wspolczynnikZlapania(),
        pc.mnoznik() * (warunek ? pc.mnoznikWarunku() : 1.0), dz.status == null ? null : dz.status.name(), dz.o.poziom,
        druzyny.maxPoziom(d), c.mmocore.poziom(p));
  }

  /**
   * Najlepsza szansa złapania pieczęcią z ekwipunku (0..1), -1 = brak pieczęci albo żadna nie działa. Pokazywana
   * przy [7] — wcześniej gracz rzucał w ciemno i nie wiedział, kiedy przestać bić, a kiedy łapać.
   */
  private double szansaPieczeci(Walka w, int s) {
    Player p = w.gracze[s];
    ProfilDane d = p == null ? null : c.dane(p);
    if (d == null || w.rodzaj != Rodzaj.DZIKI || lapanie == null) return -1;
    WzorLapania wz = new WzorLapania(c.ust.lapanie);
    double best = -1;
    java.util.Set<String> byly = new java.util.HashSet<>();
    for (ItemStack it : p.getInventory().getContents()) {
      Przedmioty.Pieczec pc = lapanie.pieczec(it);
      if (pc == null || !byly.add(pc.id()) || odmowaPieczeci(w, p, d, pc) != null) continue;
      best = Math.max(best, wz.szansa(wejsciePieczeci(w, p, d, pc)));
    }
    return best;
  }

  private void poPieczeci(Walka w, int s, Player p, Przedmioty.Pieczec pc, boolean sukces, Wojownik dz, Gatunek g) {
    if (sukces) {
      dz.o.hp = dz.hp;
      Osobnik lapany = dz.o;
      lapany.hpCzas = System.currentTimeMillis();
      LivingEntity cialo = w.dziki;
      zakoncz(w, 0, "ZLAPANY");
      if (cialo != null && cialo.isValid()) {
        cialo.setInvisible(false);
        lapanie.schwytaj(p, cialo, lapany, "PIECZEC", pc.id());
      }
      return;
    }
    dodajLog(w, "<red>" + g.nazwa() + " wyrywa się!");
    wybierz(w, s, new Akcja.Pomin("pieczec"));
  }

  private void ucieczka(Walka w, int s) {
    if (w.skonczona) return;
    if (w.rodzaj == Rodzaj.DZIKI) {
      double moja = w.silnik.szybkosc(w.silnik.strony[s].akt());
      double jego = w.silnik.szybkosc(w.silnik.strony[1 - s].akt());
      double szansa = Math.min(1.0, 0.5 + 0.5 * moja / Math.max(1, jego));
      if (ThreadLocalRandom.current().nextDouble() < szansa) {
        dodajLog(w, "Udało się uciec!");
        zakoncz(w, -1, "UCIECZKA");
      } else {
        dodajLog(w, "<red>Nie udało się uciec!");
        wybierz(w, s, new Akcja.Pomin("ucieczka"));
      }
      return;
    }
    dodajLog(w, w.silnik.strony[s].nazwa + " poddaje się.");
    w.silnik.poddaj(s);
    zakoncz(w, 1 - s, "PODDANIE");
  }

  private void wybierz(Walka w, int s, Akcja a) {
    c.debug("walka wybierz strona=" + s + " akcja=" + a + " skonczona=" + w.skonczona + " juz=" + w.wybor[s] + " drugi=" + w.wybor[1 - s]);
    if (w.skonczona || w.odtwarzanie || w.wybor[s] != null) return;
    w.wybor[s] = a;
    w.przeterminowania[s] = 0;
    if (w.gracze[s] != null && w.wybor[1 - s] == null && w.gracze[1 - s] != null && klawisze(w, s)) {
      hudMenu(w, s);
    } else if (w.gracze[s] != null && w.wybor[1 - s] == null && w.gracze[1 - s] != null) {
      okna.pokaz(w.gracze[s], "<gold>Walka — tura " + (w.silnik.nrTury() + 1), naglowekZ(w, s, "<gray>Czekasz na ruch przeciwnika…"), List.of(),
          new Okna.Przycisk("<red>Poddaj się", "Oddajesz walkę", 120, x -> ucieczka(w, s)), 1, false, true);
    }
    sprobujRozstrzygnac(w);
  }

  private List<String> naglowekZ(Walka w, int s, String dopisek) {
    List<String> t = naglowek(w, s);
    t.add(0, dopisek);
    return t;
  }

  private void sprobujRozstrzygnac(Walka w) {
    if (w.skonczona || w.odtwarzanie || w.wybor[0] == null || w.wybor[1] == null) return;
    int[] fazaPrzed = {w.silnik.strony[0].aktywny, w.silnik.strony[1].aktywny};
    Scena.Stan st = new Scena.Stan();
    for (int s = 0; s < 2; s++) {
      for (Wojownik x : w.silnik.strony[s].druzyna) st.hp.put(x, x.hp);
      st.pokazany[s] = w.silnik.strony[s].akt();
    }
    List<Zdarzenie> z = w.silnik.tura(w.wybor[0], w.wybor[1]);
    for (int s = 0; s < 2; s++) if (w.silnik.strony[s].aktywny != fazaPrzed[s]) w.uczestnicy[s].add(w.silnik.strony[s].akt().o.id);
    // Tura jest już policzona; teraz ją odgrywamy krok po kroku przy zamkniętych oknach (okno zasłania świat).
    w.odtwarzanie = true;
    for (Player p : w.gracze) if (p != null && p.isOnline()) okna.zamknij(p);
    krok(w, z, 0, st, fazaPrzed);
  }

  /** Jeden krok odtwarzania; następny po czasie zwróconym przez scenę. */
  private void krok(Walka w, List<Zdarzenie> z, int i, Scena.Stan st, int[] fazaPrzed) {
    if (w.skonczona) return;
    if (i >= z.size()) { poOdtworzeniu(w, fazaPrzed); return; }
    Zdarzenie e = z.get(i);
    dodajLog(w, e.tekst());
    w.krok = e.tekst();
    if (e.rodzaj() == Zdarzenie.Rodzaj.ZMIANA && e.strona() >= 0) {
      postawCialo(w, e.strona());
      st.pokazany[e.strona()] = w.silnik.strony[e.strona()].akt();
      fazaPrzed[e.strona()] = w.silnik.strony[e.strona()].aktywny;
    }
    int t = scena.krok(w.gracze, w.ciala, e, st, c.rej.ruch(e.ruch()), c.ust.odtwarzanieTicki);
    for (int s = 0; s < 2; s++) if (klawisze(w, s)) { hudStwory(w, s, st); hudMenu(w, s); }
    if (e.rodzaj() == Zdarzenie.Rodzaj.ZEMDLAL) poOmdleniu(w, e.strona());
    if (t <= 0) krok(w, z, i + 1, st, fazaPrzed);
    else Bukkit.getScheduler().runTaskLater(c.plugin, () -> krok(w, z, i + 1, st, fazaPrzed), t);
  }

  private void poOdtworzeniu(Walka w, int[] fazaPrzed) {
    if (!w.poPauzie) {
      // chwila na obejrzenie wyniku tury, zanim okno wyboru znów zasłoni arenę
      w.poPauzie = true;
      Bukkit.getScheduler().runTaskLater(c.plugin, () -> { if (!w.skonczona) poOdtworzeniu(w, fazaPrzed); }, 10L);
      return;
    }
    w.poPauzie = false;
    w.odtwarzanie = false;
    for (int s = 0; s < 2; s++) if (w.silnik.strony[s].aktywny != fazaPrzed[s]) postawCialo(w, s);
    for (int s = 0; s < 2; s++) odswiezCialo(w, s);
    if (w.silnik.koniec()) { zakoncz(w, w.silnik.zwyciezca(), "KO"); return; }
    if (w.rodzaj == Rodzaj.DZIKI && !w.podpowiedzSlaby && w.silnik.strony[1].akt().hpProcent() <= 35) {
      // Dziki słabnie: to moment na pieczęć — kolejny cios może go wykończyć, a pokonanego nie da się złapać.
      w.podpowiedzSlaby = true;
      double sz = szansaPieczeci(w, 0);
      Player p = w.gracze[0];
      if (p != null) {
        scena.tytul(p, "<gold>" + w.silnik.strony[1].akt().nazwa() + " słabnie!", sz < 0 ? "<gray>Nie masz pieczęci — pokonany ucieknie z łupem" : "<yellow>[7] Pieczęć — szansa ~" + Math.round(sz * 100) + "%", 40);
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
      }
    }
    if (w.silnik.nrTury() >= c.ust.maxTur) {
      int lepszy = sumaHp(w, 0) >= sumaHp(w, 1) ? 0 : 1;
      dodajLog(w, "Limit tur — wygrywa strona z większym zdrowiem.");
      zakoncz(w, lepszy, "LIMIT");
      return;
    }
    nowaTura(w);
  }

  private static double sumaHp(Walka w, int s) {
    double r = 0;
    for (Wojownik x : w.silnik.strony[s].druzyna) r += x.hpProcent();
    return r;
  }

  /** Exp dla uczestników strony gracza, gdy padł stwór przeciwnika (nie w PvP, chyba że config). */
  private void poOmdleniu(Walka w, int stronaZemdlona) {
    int zw = 1 - stronaZemdlona;
    Player p = w.gracze[zw];
    if (p == null) return;
    boolean pvp = w.gracze[stronaZemdlona] != null;
    if (pvp && !c.ust.expPojedynek) return;
    Wojownik pokonany = w.silnik.strony[stronaZemdlona].akt();
    ProfilDane d = c.dane(p);
    if (d == null) return;
    boolean trener = w.trener != null;
    long exp = Rozwoj.expZaPokonanie(pokonany.g, pokonany.o.poziom, trener, trener ? c.ust.expTrener / 1.5 : c.ust.expPve);
    List<Osobnik> dostaja = new ArrayList<>();
    for (Wojownik x : w.silnik.strony[zw].druzyna) if (!x.zemdlony() && w.uczestnicy[zw].contains(x.o.id)) dostaja.add(x.o);
    if (dostaja.isEmpty()) return;
    long kazdy = Math.max(1, exp / dostaja.size());
    // Dzielony exp (recenzja R-04): reszta sprawnej drużyny, która nie walczyła, dostaje część (config poziomy.exp_dzielony).
    if (c.ust.expDzielony > 0) {
      long czesc = Math.max(1, Math.round(exp * c.ust.expDzielony));
      for (Wojownik x : w.silnik.strony[zw].druzyna) {
        if (x.zemdlony() || w.uczestnicy[zw].contains(x.o.id)) continue;
        druzyny.ustawHp(x.o, x.hp);
        druzyny.dajExp(p, d, x.o, czesc, true);
      }
    }
    w.expZdobyty[zw] += kazdy;
    for (Osobnik o : dostaja) {
      Wojownik x = w.silnik.strony[zw].druzyna.stream().filter(v -> v.o == o).findFirst().orElse(null);
      int maxPrzed = x == null ? 0 : x.hpMax;
      druzyny.ustawHp(o, x == null ? 0 : x.hp);
      var a = druzyny.dajExp(p, d, o, kazdy);
      if (a != null && a.awansowal() && x != null) {
        // statystyki rosną w trakcie walki — przeliczamy wojownika w miejscu
        Wojownik nowy = new Wojownik(c.rej, o, c.ust.premiaWiezi, x.hp + (druzyny.hpMax(o) - maxPrzed));
        int idx = w.silnik.strony[zw].druzyna.indexOf(x);
        nowy.status = x.status; nowy.statusCzas = x.statusCzas;
        System.arraycopy(x.etapy, 0, nowy.etapy, 0, x.etapy.length);
        nowy.walczyl = x.walczyl;
        w.silnik.strony[zw].druzyna.set(idx, nowy);
      }
    }
    dodajLog(w, "<aqua>+" + kazdy + " exp" + (dostaja.size() > 1 ? " dla każdego z " + dostaja.size() : ""));
  }

  private void dodajLog(Walka w, String t) {
    w.log.add(t);
    if (w.log.size() > 40) w.log.remove(0);
  }

  // ------------------------------------------------------------------ koniec

  /** zwyciezca: 0/1, -1 = bez rozstrzygnięcia (ucieczka). */
  private void zakoncz(Walka w, int zwyciezca, String powod) {
    if (w.skonczona) return;
    w.skonczona = true;
    w.odtwarzanie = false;
    if (kamera != null) for (Player p : w.gracze) if (p != null) kamera.wylacz(p);
    if (hud != null) for (Player p : w.gracze) if (p != null) hud.ukryj(p);
    for (int s = 0; s < 2; s++) if (w.gracze[s] != null && w.gracze[s].isOnline() && w.slotPrzed[s] >= 0) w.gracze[s].getInventory().setHeldItemSlot(w.slotPrzed[s]);
    // HP wraca do osobników graczy
    for (int s = 0; s < 2; s++) {
      Player p = w.gracze[s];
      if (p == null) continue;
      ProfilDane d = c.dane(p);
      for (Wojownik x : w.silnik.strony[s].druzyna) {
        if (d != null && d.znajdz(x.o.id) != null) druzyny.ustawHp(x.o, x.hp);
      }
      if (d != null) d.brudne = true;
      wgGracza.remove(p.getUniqueId());
    }
    for (int s = 0; s < 2; s++) if (w.ciala[s] != null) scena.usunEtykiete(w.ciala[s]);
    for (int s = 0; s < 2; s++) if (w.ciala[s] != null && w.cialoNasze[s]) Mythic.usun(w.ciala[s]);
    if (w.dziki != null && w.dziki.isValid()) {
      dzikie.zajete.remove(w.dziki.getUniqueId());
      if (!"ZLAPANY".equals(powod)) {
        Wojownik dz = w.silnik.strony[1].akt();
        if (dz.zemdlony()) {
          // Pokonany dziki ginie „naprawdę” z graczem jako zabójcą: łupy MythicMobs (Esencja Więzi) jak po walce
          // w świecie. Wcześniej ciało znikało bez łupu — walka turowa była gorsza nagrodowo niż real-time.
          w.dziki.getWorld().spawnParticle(Particle.POOF, w.dziki.getLocation().add(0, 0.5, 0), 20, 0.3, 0.3, 0.3, 0.02);
          Player zab = w.gracze[0];
          w.dziki.setAI(true);
          w.dziki.setInvulnerable(false);
          w.dziki.setNoDamageTicks(0);
          // setKiller + setHealth zamiast damage(…, gracz): cios gracza z pieczęcią w ręce przechwyciłby Lapanie
          ciala.nazwij(w.dziki, w.dzikiOsobnik, null); // bez paska HP walki w komunikacie śmierci
          if (zab != null && zab.isOnline()) { w.dziki.setKiller(zab); w.dziki.setHealth(0); }
          if (w.dziki.isValid() && !w.dziki.isDead()) Mythic.usun(w.dziki);
          if (zab != null && zab.isOnline()) c.pasek(zab, "<gray>" + dz.nazwa() + " pada. <yellow>Pokonanego nie da się złapać — łap, gdy słabnie ([7]).");
        } else {
          w.dziki.setAI(true);
          w.dziki.setHealth(Math.max(0.5, Math.min(w.dziki.getHealth(), dz.hp)));
          ciala.nazwij(w.dziki, w.dzikiOsobnik, null);
        }
      }
    }
    Player zw = zwyciezca >= 0 ? w.gracze[zwyciezca] : null;
    Player pr = zwyciezca >= 0 ? w.gracze[1 - zwyciezca] : null;
    for (int s = 0; s < 2; s++) {
      Player p = w.gracze[s];
      if (p == null || !p.isOnline()) continue;
      okna.zamknij(p);
      String wynik = zwyciezca < 0 ? "<gray>Walka przerwana." : zwyciezca == s ? "<green><b>Zwycięstwo!</b>" : "<red><b>Porażka.</b>";
      // podsumowanie na ekranie: bez tego wygrana kończyła się jednym wierszem czatu (recenzja R-03)
      if (zwyciezca == s || "ZLAPANY".equals(powod)) scena.tytul(p, zwyciezca == s ? "<green><b>Zwycięstwo!" : "<green><b>Złapany!",
          w.expZdobyty[s] > 0 ? "<aqua>+" + w.expZdobyty[s] + " exp" : "", 30);
      else if (zwyciezca >= 0) scena.tytul(p, "<red><b>Porażka", "<gray>Ulecz chowańce: eliksir albo odpoczynek", 30);
      if ("ZLAPANY".equals(powod)) wynik = "<green>Walka zakończona schwytaniem.";
      c.msg(p, wynik + " <gray>(" + w.opisPrzeciwnika(s) + ")");
      p.playSound(p.getLocation(), zwyciezca == s ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1f);
    }
    if (zwyciezca >= 0) {
      if (zw != null) {
        Osobnik akt = w.silnik.strony[zwyciezca].akt().o;
        Bukkit.getScheduler().runTaskLater(c.plugin, () -> { if (zw.isOnline()) { przywolania.przywolaj(zw, akt); if (!c.ust.towarzyszWlaczony) przywolania.sygnalAktywnemu(zw, akt, "OKRZYK"); } }, 5L);
      }
      nagrody(w, zwyciezca);
    }
    if (w.rodzaj == Rodzaj.TURNIEJ && w.gracze[1] != null) {
      UUID a = w.gracze[0].getUniqueId(), b = w.gracze[1].getUniqueId();
      if (zwyciezca == 0) poMeczuTurnieju.accept(a, b);
      else if (zwyciezca == 1) poMeczuTurnieju.accept(b, a);
    }
    Bukkit.getPluginManager().callEvent(new WalkaZakonczonaEvent(w.rodzaj.name(), zw, pr, w.opisPrzeciwnika(0), w.trener == null ? null : w.trener.id()));
    for (Player p : w.gracze) if (p != null && c.dane(p) != null) c.dane.zapisz(c.dane(p));
  }

  private void nagrody(Walka w, int zwyciezca) {
    Player zw = w.gracze[zwyciezca];
    Player pr = w.gracze[1 - zwyciezca];
    if (zw == null) return; // wygrał NPC/dziki
    ProfilDane d = c.dane(zw);
    if (d == null) return;
    if (pr != null && w.rankingowa) {
      ProfilDane dp = c.dane(pr);
      if (dp != null) {
        int[] n = ranking.wynik(zw, d, pr, dp);
        c.msg(zw, "<gold>ELO: " + n[0]);
        c.msg(pr, "<gold>ELO: " + n[1]);
        c.zdarzenie("WYGRANA_POJEDYNKU", Map.of("gracz", zw.getName(), "przegrany", pr.getName(), "elo", String.valueOf(n[0])));
      }
    }
    if (w.trener != null) {
      Trener t = w.trener;
      if (t.rodzaj() == Trener.Rodzaj.MISTRZ && d.odznaki.add(t.odznaka())) {
        c.msg(zw, "<gold>Zdobywasz odznakę <yellow>" + t.odznaka() + "</yellow>! Limit poziomu chowańców: <yellow>" + c.limitPoziomu(d));
        c.zdarzenie("ODZNAKA", Map.of("gracz", zw.getName(), "odznaka", t.odznaka()));
      }
      c.komendyKonsoli(t.nagrodaKomendy(), Map.of("gracz", zw.getName(), "profil", d.profil));
      d.brudne = true;
    }
    if (w.wieza != null) {
      int fala = w.fala;
      Wieza wz = w.wieza;
      int rekord = d.wieza.getOrDefault(wz.id(), 0);
      if (fala > rekord) d.wieza.put(wz.id(), fala);
      c.zdarzenie("FALA_WIEZY", Map.of("gracz", zw.getName(), "wieza", wz.id(), "fala", String.valueOf(fala)));
      List<String> nag = wz.nagrody().get(fala);
      if (nag != null && d.nagrodyWiezy.add(wz.id() + ":" + fala)) c.komendyKonsoli(nag, Map.of("gracz", zw.getName(), "profil", d.profil));
      d.brudne = true;
      if (fala < wz.fale().size()) {
        druzyny.leczWszystkich(zw, d, wz.leczenieMiedzyFalami());
        c.msg(zw, "<gold>Fala " + fala + "/" + wz.fale().size() + " zaliczona. <gray>Następna za 3 s…");
        Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
          if (zw.isOnline() && c.dane(zw) != null) startTrener(zw, wz.fale().get(fala), wz, fala + 1);
        }, 60L);
      } else {
        c.msg(zw, "<gold><b>Wieża " + wz.nazwa() + " ukończona!</b>");
      }
    }
  }

  // ------------------------------------------------------------------ pilnowanie: czas tury, zasięg, wyjście

  public void tik() {
    long teraz = System.currentTimeMillis();
    for (Walka w : List.copyOf(new java.util.HashSet<>(wgGracza.values()))) {
      if (w.skonczona) continue;
      if (w.odtwarzanie) {
        for (int s = 0; s < 2; s++) w.terminWyboru[s] = teraz + c.ust.czasTuryS * 1000L;
        continue;
      }
      for (int s = 0; s < 2; s++) {
        Player p = w.gracze[s];
        if (p == null) continue;
        if (!p.isOnline() || p.isDead()) { przerwij(w, s); break; }
        LivingEntity moje = w.ciala[s];
        boolean wKamerze = kamera != null && kamera.aktywna(p);
        if (!wKamerze && moje != null && (!p.getWorld().equals(moje.getWorld()) || p.getLocation().distanceSquared(moje.getLocation()) > c.ust.walkaZasieg * c.ust.walkaZasieg)) {
          c.blad(p, "Oddaliłeś się od walki.");
          przerwij(w, s);
          break;
        }
        if (klawisze(w, s)) hudMenu(w, s);
        if (w.wybor[s] == null && teraz > w.terminWyboru[s]) {
          w.przeterminowania[s]++;
          if (w.przeterminowania[s] >= 3) { c.blad(p, "Trzykrotny brak ruchu — walka oddana."); przerwij(w, s); break; }
          if (w.silnik.wymagaZmiany(s)) {
            w.silnik.wymuszonaZmiana(s, w.silnik.strony[s].pierwszyZdolny());
            postawCialo(w, s);
            nowaTura(w);
          } else {
            c.pasek(p, "<red>Czas minął — ruch losowy.");
            int n = Math.max(1, w.silnik.strony[s].akt().o.ruchy.size());
            int k = ThreadLocalRandom.current().nextInt(n);
            for (int x = 0; x < n && !w.silnik.gotowy(s, k); x++) k = (k + 1) % n;
            wybierz(w, s, new Akcja.UzyjRuchu(k));
          }
        }
      }
    }
  }

  /** Strona s oddaje walkę (wyjście, śmierć, oddalenie, zmiana profilu). */
  public void przerwij(Walka w, int s) {
    if (w.skonczona) return;
    if (w.rodzaj == Rodzaj.DZIKI || w.gracze[1 - s] == null) { zakoncz(w, w.gracze[1 - s] == null && w.rodzaj != Rodzaj.DZIKI ? 1 - s : -1, "PRZERWANA"); return; }
    w.silnik.poddaj(s);
    zakoncz(w, 1 - s, "PRZERWANA");
  }

  public void przerwij(Player p) {
    Walka w = walka(p);
    if (w == null) return;
    przerwij(w, w.gracze[0] == p || (w.gracze[0] != null && w.gracze[0].getUniqueId().equals(p.getUniqueId())) ? 0 : 1);
  }

  public void przerwijWszystkie() {
    for (Walka w : List.copyOf(new java.util.HashSet<>(wgGracza.values()))) zakoncz(w, -1, "WYLACZENIE");
  }

  /** Ponowne otwarcie okna (np. gracz zamknął je klawiszem, a klient pozwolił). */
  public void pokazPonownie(Player p) {
    Walka w = walka(p);
    if (w == null) return;
    if (w.odtwarzanie) return;
    int s = w.gracze[0] != null && w.gracze[0].getUniqueId().equals(p.getUniqueId()) ? 0 : 1;
    if (w.silnik.wymagaZmiany(s)) oknoZmiany(w, s, true);
    else if (w.wybor[s] == null) pokazTure(w, s);
  }

  @EventHandler(priority = EventPriority.LOW)
  public void wyjscie(PlayerQuitEvent e) { przerwij(e.getPlayer()); }

  @EventHandler(priority = EventPriority.LOW)
  public void smierc(PlayerDeathEvent e) { przerwij(e.getPlayer()); }

  /** Ciała walki nie przyjmują obrażeń ze świata (walka liczy HP sama). */
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void cialaNietykalne(EntityDamageEvent e) {
    if (jestCialemWalki(e.getEntity())) e.setCancelled(true);
  }

  /** Dla targetera @ChwPrzeciwnik: ciało przeciwnika ciała {@code e}. */
  public LivingEntity przeciwnikCiala(org.bukkit.entity.Entity e) {
    for (Walka w : wgGracza.values()) {
      for (int s = 0; s < 2; s++) if (w.ciala[s] != null && w.ciala[s].getUniqueId().equals(e.getUniqueId())) return w.ciala[1 - s];
    }
    return null;
  }

  public boolean cialoWWalce(org.bukkit.entity.Entity e) {
    if (jestCialemWalki(e)) return true;
    for (Walka w : wgGracza.values()) if (w.dziki != null && w.dziki.getUniqueId().equals(e.getUniqueId())) return true;
    return false;
  }
}
