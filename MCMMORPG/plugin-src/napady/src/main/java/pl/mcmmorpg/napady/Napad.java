package pl.mcmmorpg.napady;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.util.BoundingBox;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * Jeden napad. Dowodca (cele, rozkazy, fazy, brama, lup, morale) pracuje na modelu jednostek, a ruch
 * i walke wykonuje jeden z dwoch wykonawcow - zaleznie od poziomu szczegolow (LOD):
 * <ul>
 *   <li>SYM - nikt nie patrzy: {@link Symulacja}, bez encji, chunkow i A*;</li>
 *   <li>CIALA - gracz dolaczyl albo jest blisko: ciala MythicMobs z AI vanilla, plugin dowodzi.</li>
 * </ul>
 * Przejscie (materializacja/dematerializacja) idzie przez kolejke pracy pluginu z budzetem czasu na tik,
 * napad stoi na ten czas (zamrozony tik), wiec stan sie nie rozjezdza.
 */
final class Napad {
  enum Faza { MARSZ, BRAMA, SZTURM, GRABIEZ, ODWROT, KONIEC }
  enum Lod { SYM, MATERIALIZUJE, CIALA, DEMATERIALIZUJE }

  final Napady pl;
  final int id;
  final Arena arena;
  final long ziarno;
  final Random los;
  final Symulacja sym;
  final List<Jednostka> jednostki = new ArrayList<>();
  Faza faza = Faza.MARSZ;
  Lod lod = Lod.SYM;
  int tik, hpBramy, lup, napastnikowStart;
  /** ostatni tik z graczem w promieniu wyjscia (histereza dematerializacji) */
  int ktosBlisko;
  /** do zrobienia po materializacji (teleport dolaczajacych) */
  final List<Runnable> poMaterializacji = new ArrayList<>();
  /** ciosy graczy, ktore doszly do cial / zablokowane (do zapisu i testu) */
  int ciosyGraczy, ciosyGraczyBlok;
  int przebrane, materializacji;
  TextDisplay napisBramy;
  String wynik;
  private boolean brakTypuZgloszony;
  // pomiar
  long sumaNs, maxNs, tikiPomiaru;
  // zapis
  final StringBuilder zapis = new StringBuilder(1 << 16);

  Napad(Napady pl, int id, Arena arena, long ziarno) {
    this.pl = pl; this.id = id; this.arena = arena; this.ziarno = ziarno;
    this.los = new Random(ziarno);
    this.sym = new Symulacja(pl.getConfig().getConfigurationSection("sym"), pl.getConfig().getDouble("obrazenia", 3));
  }

  /** Start = sam model (bez swiata). Ciala powstana dopiero, gdy ktos bedzie patrzyl. */
  void start() {
    var c = pl.getConfig();
    hpBramy = c.getInt("hp-bramy", 300);
    arena.uklad = Uklad.BRAMA_STOI;   // arena slotu jest wspolna dla kolejnych napadow
    double hp = c.getDouble("hp-jednostki", 20);
    int nid = 0;
    for (int i = 0; i < c.getInt("atak.wojownicy", 8); i++)
      dodaj(++nid, Strona.ATAK, Jednostka.Rola.WOJOWNIK, 2 + i % 3, 16 + (i * 7) % 9, false, i, hp);
    for (int i = 0; i < c.getInt("atak.lucznicy", 2); i++)
      dodaj(++nid, Strona.ATAK, Jednostka.Rola.LUCZNIK, 0, 18 + i * 4, false, i, hp);
    int ob = c.getInt("obrona.wojownicy", 4);
    for (int i = 0; i < ob; i++)
      dodaj(++nid, Strona.OBRONA, Jednostka.Rola.WOJOWNIK, i < ob / 2 ? 23 : 31, 18 + (i % 2) * 4, false, i, hp);
    for (int i = 0; i < c.getInt("obrona.lucznicy", 2); i++)
      dodaj(++nid, Strona.OBRONA, Jednostka.Rola.LUCZNIK, Arena.BRAMA_X, i == 0 ? 13 : 27, true, i, hp);
    napastnikowStart = (int) jednostki.stream().filter(j -> j.strona == Strona.ATAK).count();
    ktosBlisko = -1_000_000;
    zapiszNaglowek();
    pl.getLogger().info("Napad " + id + " start, arena x=" + arena.ox + ", jednostek " + jednostki.size() + ", ziarno " + ziarno);
  }

  private void dodaj(int nid, Strona s, Jednostka.Rola r, double x, double z, boolean naMurze, int slot, double hp) {
    int skora = pl.skory.isEmpty() ? -1 : los.nextInt(pl.skory.size());
    jednostki.add(new Jednostka(this, nid, s, r, x, z, naMurze, slot, hp, skora));
  }

  // ---------------------------------------------------------------- tik
  void tik() {
    if (faza == Faza.KONIEC) return;
    if (lod == Lod.MATERIALIZUJE || lod == Lod.DEMATERIALIZUJE) return;   // przejscie w kolejce pracy: napad stoi
    var c = pl.getConfig();
    if (tik % 10 == 0) { lodKontrola(); if (lod != Lod.SYM && lod != Lod.CIALA) return; }
    tik++;
    long t = System.nanoTime();
    boolean ciala = lod == Lod.CIALA;
    if (ciala) for (Jednostka j : jednostki) synchronizuj(j);
    if (tik % 10 == 0) { wybierzCele(); t = odcinek("cele", t); }
    for (Jednostka j : jednostki) {
      if (!j.zywa) continue;
      boolean decyzja = (tik + j.id) % 5 == 0;
      if (decyzja) rozkaz(j);
      if (ciala && j.cialo != null) { if (decyzja) wykonajCialem(j); }
      else sym.krok(this, j);
      t = odcinek("j" + j.id + " " + j.akcja, t);
    }
    if (tik % 20 == 0) {
      sprawdzFazy(); t = odcinek("fazy", t);
      if (ciala) { pilnujWidzow(); t = odcinek("widzowie", t); }
      if (tik > c.getInt("limit-czasu-s", 300) * 20) zakoncz("limit czasu");
    }
    if (faza != Faza.KONIEC && tik % c.getInt("zapis-co-tikow", 5) == 0) { zapiszKlatke(); odcinek("zapis", t); }
  }

  /** Diagnoza skokow: odcinek tiku dluzszy niz 5 ms idzie do logu z nazwa (koszt: nanoTime na odcinek). */
  private long odcinek(String co, long od) {
    long teraz = System.nanoTime();
    if (teraz - od > 5_000_000L) pl.getLogger().warning("Napad " + id + " tik " + tik + " odcinek " + co + ": " + (teraz - od) / 1000 + " us");
    return teraz;
  }

  // ---------------------------------------------------------------- LOD
  /**
   * Co 0,5 s: ciala, gdy ktos dolaczyl (takze widz) albo gracz jest w swiecie napadow blizej niz
   * {@code lod.r-wejscia} od srodka areny; z powrotem symulacja, gdy nikt nie dolaczyl i nikogo nie
   * bylo blizej niz {@code lod.r-wyjscia} przez {@code lod.zwloka-s} (histereza: bez migotania na granicy).
   */
  private void lodKontrola() {
    if (!pl.cialaMM()) return;
    var c = pl.getConfig();
    String tryb = c.getString("lod.tryb", "auto");
    boolean dolaczeni = pl.dolaczeni.values().stream().anyMatch(d -> d.napad == id);
    boolean chce;
    if ("ciala".equals(tryb)) chce = true;
    else if ("symulacja".equals(tryb) && !dolaczeni) chce = false;
    else {
      // zwloka liczy sie od ostatniej chwili, gdy ktos byl w napadzie albo w poblizu (nie od materializacji)
      double rWe = c.getDouble("lod.r-wejscia", 64), rWy = c.getDouble("lod.r-wyjscia", 96);
      double d = najblizszyGracz();
      if (dolaczeni || d <= rWy) ktosBlisko = tik;
      chce = dolaczeni || (lod == Lod.CIALA ? tik - ktosBlisko < c.getInt("lod.zwloka-s", 10) * 20 : d <= rWe);
    }
    if (chce && lod == Lod.SYM) materializuj();
    else if (!chce && lod == Lod.CIALA) dematerializuj();
  }

  private double najblizszyGracz() {
    double sx = arena.ox + 24.5, sz = arena.oz + 20.5, best = Double.MAX_VALUE;
    for (Player p : arena.swiat.getPlayers()) {
      Location l = p.getLocation();
      best = Math.min(best, Math.hypot(l.getX() - sx, l.getZ() - sz));
    }
    return best;
  }

  /** Wymuszenie cial (dolaczenie): od razu, bez czekania na kontrole co 0,5 s. */
  void zadajCial() {
    if (lod == Lod.SYM && faza != Faza.KONIEC && pl.cialaMM()) materializuj();
  }

  /**
   * Chunki asynchronicznie, potem w kolejce pracy: brakujaca budowa (kolumnami), brama do stanu napadu,
   * ciala w pozycjach i z HP modelu, napis. Ostatnia pozycja kolejki odmraza napad.
   */
  private void materializuj() {
    lod = Lod.MATERIALIZUJE;
    materializacji++;
    zdarzenie("lod", "ciala");
    long t0 = System.nanoTime();
    arena.zaladuj().whenComplete((v, err) -> Bukkit.getScheduler().runTask(pl, () -> {
      if (err != null) pl.getLogger().warning("Napad " + id + ": ladowanie chunkow areny: " + err);
      if (faza == Faza.KONIEC) return;
      arena.trzymaj(pl);
      if (!arena.zbudowana())
        for (int x = Uklad.MIN_X; x < Uklad.MIN_X + Uklad.W; x++) { int kx = x; pl.praca(() -> arena.zbudujKolumne(kx)); }
      pl.praca(arena::ustawBrame);
      for (Jednostka j : jednostki) pl.praca(() -> { if (faza != Faza.KONIEC) spawnCiala(j); });
      pl.praca(() -> {
        if (faza == Faza.KONIEC) return;
        spawnNapis();
        lod = Lod.CIALA;
        ktosBlisko = tik;
        pl.getLogger().info("Napad " + id + ": ciala (" + policzCiala() + ") po " + (System.nanoTime() - t0) / 1_000_000 + " ms; " + kosztCial());
        for (Runnable r : poMaterializacji) r.run();
        poMaterializacji.clear();
      });
    }));
  }

  /** Model juz ma pozycje i HP (synchronizacja co tik); ciala i strzaly znikaja kolejka, bilety na koniec. */
  private void dematerializuj() {
    lod = Lod.DEMATERIALIZUJE;
    zdarzenie("lod", "symulacja");
    usunCiala();
    pl.praca(() -> {
      sprzatnijArene();
      if (faza != Faza.KONIEC) lod = Lod.SYM;
      pl.getLogger().info("Napad " + id + ": symulacja (nikt nie patrzy)");
    });
  }

  private void usunCiala() {
    for (Jednostka j : jednostki) {
      Mob m = j.cialo;
      if (m == null) continue;
      j.cialo = null;
      pl.cialaJednostek.remove(m.getUniqueId());
      pl.praca(() -> { if (m.isValid()) m.remove(); });
    }
    TextDisplay t = napisBramy;
    napisBramy = null;
    if (t != null) pl.praca(() -> { if (t.isValid()) t.remove(); });
  }

  /** Strzaly cial w obszarze areny (nie trwale, ale zostalyby do rozladowania chunku) i bilety. */
  private void sprzatnijArene() {
    BoundingBox b = new BoundingBox(arena.ox + Uklad.MIN_X, Arena.Y - 2, arena.oz + Uklad.MIN_Z,
        arena.ox + Uklad.MIN_X + Uklad.W, Arena.Y + 24, arena.oz + Uklad.MIN_Z + Uklad.H);
    if (arena.chunkiZaladowane()) for (Entity e : arena.swiat.getNearbyEntities(b, e -> e instanceof AbstractArrow)) e.remove();
    arena.zwolnij(pl);
  }

  private int policzCiala() {
    int n = 0;
    for (Jednostka j : jednostki) if (j.cialo != null) n++;
    return n;
  }

  private void spawnNapis() {
    if (napisBramy != null && napisBramy.isValid()) return;
    Location ln = arena.loc(Arena.BRAMA_X - 1, Arena.BRAMA_Z0 + 1).add(0, 3.6, 0);
    napisBramy = arena.swiat.spawn(ln, TextDisplay.class, t -> {
      t.setBillboard(Display.Billboard.CENTER);
      t.setPersistent(false);
    });
    odswiezNapis();
  }

  /** Cialo MM w miejscu modelu; null (brak typu w MM) = jednostka dalej liczona symulacja. */
  private void spawnCiala(Jednostka j) {
    if (!j.zywa || j.cialo != null || lod == Lod.DEMATERIALIZUJE) return;
    String typ = "NAP_" + j.strona.name() + "_" + j.rola.name();
    Location l = new Location(arena.swiat, arena.ox + j.x, Arena.Y + (j.naMurze ? Arena.MUR_H : 0), arena.oz + j.z, (float) j.yaw, 0);
    long t0 = System.nanoTime();
    LivingEntity e = CialaMM.spawn(typ, l);
    long t1 = System.nanoTime();
    nsSpawn += t1 - t0; maxSpawn = Math.max(maxSpawn, t1 - t0);
    if (!(e instanceof Mob m)) {
      if (e != null) e.remove();
      if (!brakTypuZgloszony) pl.getLogger().warning("MythicMobs nie zna " + typ + " (mythicmobs/mobs/Napady.yml) - jednostka w symulacji");
      brakTypuZgloszony = true;
      return;
    }
    przygotujMM(m, j);
    j.cialo = m;
    j.ruchX = j.ruchDoX = Double.NaN;
    pl.cialaJednostek.put(m.getUniqueId(), j);
  }

  private static void atrybut(LivingEntity e, Attribute a, double v) {
    var i = e.getAttribute(a);
    if (i != null) i.setBaseValue(v);
  }

  /** Cialo MM: liczby z configu (te same co model), skora LD z modelu (ta sama po kazdej materializacji). */
  private void przygotujMM(Mob e, Jednostka j) {
    var c = pl.getConfig();
    e.setPersistent(false);
    e.setCanPickupItems(false);
    if (e instanceof Ageable a) a.setAdult();
    e.customName(Component.text(j.strona == Strona.ATAK ? "Najeźdźca" : "Obrońca",
        j.strona == Strona.ATAK ? NamedTextColor.RED : NamedTextColor.AQUA));
    e.setCustomNameVisible(false);
    atrybut(e, Attribute.MAX_HEALTH, j.maxHp);
    e.setHealth(Math.max(0.5, Math.min(j.maxHp, j.hp)));
    atrybut(e, Attribute.ATTACK_DAMAGE, c.getDouble("obrazenia", 3));
    atrybut(e, Attribute.SPAWN_REINFORCEMENTS, 0);
    // lucznik na murze stoi: bowattack inaczej schodzi z muru, zeby okrazac cel
    if (j.naMurze) { atrybut(e, Attribute.MOVEMENT_SPEED, 0); atrybut(e, Attribute.KNOCKBACK_RESISTANCE, 1); }
    // sprzet z Equipment w mobs/Napady.yml (sprzet z pluginu MM nadpisuje przy spawnie)
    var eq = e.getEquipment();
    if (eq != null) for (var slot : org.bukkit.inventory.EquipmentSlot.values())
      try { eq.setDropChance(slot, 0); } catch (IllegalArgumentException ignored) { /* slot bez szansy upuszczenia (np. BODY) */ }
    long t0 = System.nanoTime();
    if (j.skora >= 0 && pl.przebrania()
        && CialaMM.przebierz(e, j.strona == Strona.ATAK ? "Najezdzca" : "Obronca", pl.skory.get(j.skora), pl.getLogger())) przebrane++;
    long dt = System.nanoTime() - t0;
    nsPrzebr += dt; maxPrzebr = Math.max(maxPrzebr, dt);
  }

  /** Koszt ostatniej materializacji (ms): suma i najdluzszy pojedynczy spawn MM / przebranie LD. */
  private long nsSpawn, maxSpawn, nsPrzebr, maxPrzebr;

  private String kosztCial() {
    String r = String.format(Locale.ROOT, "spawn MM %.1f (max %.1f), przebranie LD %.1f (max %.1f)",
        nsSpawn / 1e6, maxSpawn / 1e6, nsPrzebr / 1e6, maxPrzebr / 1e6);
    nsSpawn = maxSpawn = nsPrzebr = maxPrzebr = 0;
    return r;
  }

  /** Model <- cialo (pozycja, HP). Cialo znikniete bez smierci (np. rozladowane) wraca kolejka. */
  private void synchronizuj(Jednostka j) {
    Mob m = j.cialo;
    if (m == null || !j.zywa) return;
    if (!m.isValid()) {
      j.cialo = null;
      pl.cialaJednostek.remove(m.getUniqueId());
      if (m.isDead()) { padl(j); return; }
      zdarzenie("cialo-znika", String.valueOf(j.id));
      pl.praca(() -> { if (lod == Lod.CIALA && faza != Faza.KONIEC) spawnCiala(j); });
      return;
    }
    Location l = m.getLocation();
    j.x = l.getX() - arena.ox;
    j.z = l.getZ() - arena.oz;
    j.yaw = l.getYaw();
    j.hp = m.getHealth();
  }

  // ---------------------------------------------------------------- dowodca
  /** Kandydat na cel: jednostka (model) albo gracz (tylko przy cialach). */
  private record Kand(Jednostka j, Player p, double x, double z, Strona s, boolean naMurze, double hp) {
    Object klucz() { return j != null ? j : p; }
  }

  /**
   * Dowodca co 0,5 s: cele rozlozone (najwyzej kilku na jednego, kara za tlok), dobijanie slabych,
   * trzymanie obecnego celu (bez skakania), wojownik nie celuje w lucznika na murze (nie dojdzie)
   * ani w cel, do ktorego nie ma drogi, obronca bije tylko w zasiegu od posterunku (smycz). Ranni
   * (HP < prog) wycofuja sie i powoli dochodza do siebie z dala od wroga; wracaja do walki od 70% HP.
   */
  private void wybierzCele() {
    double progRanny = pl.getConfig().getDouble("ranny-prog", 0.3);
    List<Kand> wszyscy = new ArrayList<>();
    for (Jednostka j : jednostki) if (j.zywa) wszyscy.add(new Kand(j, null, j.x, j.z, j.strona, j.naMurze, j.hpUlamek()));
    if (lod == Lod.CIALA)
      for (var e : pl.dolaczeni.entrySet()) {
        var d = e.getValue();
        if (d.napad != id || d.polegl || d.widz) continue;
        Player p = Bukkit.getPlayer(e.getKey());
        if (p == null || p.isDead() || p.getGameMode() == GameMode.SPECTATOR || p.getWorld() != arena.swiat) continue;
        Location l = p.getLocation();
        var max = p.getAttribute(Attribute.MAX_HEALTH);
        wszyscy.add(new Kand(null, p, l.getX() - arena.ox, l.getZ() - arena.oz, d.strona, false,
            p.getHealth() / (max == null ? 20 : max.getValue())));
      }
    Map<Object, Integer> wzieci = new HashMap<>();
    for (Jednostka j : jednostki) {
      if (!j.zywa) continue;
      double hp = j.hpUlamek();
      if (!j.ranny && hp < progRanny) { j.ranny = true; zdarzenie("ranny", String.valueOf(j.id)); }
      else if (j.ranny && hp >= 0.7) { j.ranny = false; zdarzenie("wraca-do-walki", String.valueOf(j.id)); }
    }
    Uklad u = arena.uklad;
    for (Jednostka j : jednostki) {
      if (!j.zywa) continue;
      boolean lucznik = j.lucznik();
      double zasieg = lucznik ? 22 : j.strona == Strona.OBRONA ? 10 : 12;
      boolean smycz = j.strona == Strona.OBRONA && !j.naMurze;
      double rx = smycz ? j.postX + 0.5 : j.x, rz = smycz ? j.postZ + 0.5 : j.z;
      Kand best = null;
      double bs = Double.MAX_VALUE, najblizszy = Double.MAX_VALUE;
      for (Kand k : wszyscy) {
        if (k.s != j.strona.wrog()) continue;
        double dj = j.odl(k.x, k.z);
        najblizszy = Math.min(najblizszy, dj);
        if (!lucznik && k.naMurze) continue;
        if (Math.hypot(k.x - rx, k.z - rz) > zasieg) continue;
        // wrecz tylko cel, do ktorego da sie dojsc (inaczej stoi pod zamknieta brama z celem za nia)
        if (!lucznik && !u.osiagalne(j.x, j.z, k.x, k.z)) continue;
        double s = dj;
        if (!lucznik) s += 3.0 * wzieci.getOrDefault(k.klucz(), 0);
        if (k.hp < 0.35) s -= 2.5;
        if (k.j != null ? k.j == j.cel : k.p == j.celGracz) s -= 2.0;
        if (s < bs) { bs = s; best = k; }
      }
      if (j.ranny && najblizszy > 6 && j.hp < j.maxHp) {
        j.hp = Math.min(j.maxHp, j.hp + 0.5);
        if (j.cialo != null) j.cialo.setHealth(j.hp);
      }
      j.cel = best == null ? null : best.j;
      j.celGracz = best == null ? null : best.p;
      if (best != null && !lucznik) wzieci.merge(best.klucz(), 1, Integer::sum);
    }
  }

  /**
   * Rozkaz dla modelu (wspolny dla cial i symulacji): walka z celem (punkt NaN) albo marsz do punktu
   * z promieniem dojscia; ustawia akcje i bije brame. Bez celu: szyk przed brama (3 kolumny, pierwsze
   * 2 rzedy bija), po zburzeniu szyk przy drzwiach magazynu, lucznicy oslaniaja z dystansu, ranni z tylu,
   * obroncy na posterunkach, w odwrocie napastnicy uciekaja.
   */
  private void rozkaz(Jednostka j) {
    double cx = Double.NaN, cz = Double.NaN;
    if (j.cel != null && j.cel.zywa) { cx = j.cel.x; cz = j.cel.z; }
    else if (j.celGracz != null && j.celGracz.isValid() && !j.celGracz.isDead() && j.celGracz.getWorld() == arena.swiat) {
      cx = j.celGracz.getLocation().getX() - arena.ox; cz = j.celGracz.getLocation().getZ() - arena.oz;
    }
    boolean maCel = !Double.isNaN(cx);
    boolean odwrot = faza == Faza.ODWROT && j.strona == Strona.ATAK;
    if (odwrot || (j.ranny && j.strona == Strona.ATAK)) maCel = false;
    if (j.ranny && maCel && j.odl(cx, cz) > 3) maCel = false;   // ranny broni sie tylko z bliska
    if (maCel) {
      j.px = Double.NaN;
      j.akcja = j.lucznik() ? "strzela" : "walczy";
      return;
    }
    j.cel = null;
    j.celGracz = null;
    if (j.naMurze) { j.px = Double.NaN; j.akcja = "stoi"; return; }
    // ucieczka = wejscie w pas wyjscia (cala szerokosc), nie w punkt: pierwsi uciekinierzy staja na skraju
    // okregu i zamykaja punkt spoznionym (25.10, ziarno 101: jeden stal 2,3 bl. od punktu do limitu czasu)
    if (odwrot) { j.akcja = punkt(j, 2, 18 + j.slot % 5, 2.0) || j.x < WYJSCIE_X ? "uciekl" : "ucieka"; return; }
    if (j.ranny) {
      if (j.strona == Strona.ATAK) punkt(j, 3 - j.slot % 2, 18 + j.slot % 5, 1.5);
      else punkt(j, Arena.MAG_X0 + 3, Arena.DRZWI_Z - 1 + j.slot % 3, 1.0);
      j.akcja = "ranny";
      return;
    }
    if (j.strona == Strona.OBRONA) { j.akcja = punkt(j, j.postX, j.postZ, 1.2) ? "stoi" : "wraca"; return; }
    boolean poBramie = faza.ordinal() >= Faza.SZTURM.ordinal();
    if (j.lucznik()) {
      j.akcja = punkt(j, poBramie ? 16 : 8, 16 + j.slot * 8 % 9, 1.2) ? "oslania" : "maszeruje";
      return;
    }
    if (!poBramie) {
      int rzad = j.slot / 3, kol = j.slot % 3;
      punkt(j, Arena.BRAMA_X - 1.4 - rzad * 1.3, Arena.BRAMA_Z0 + kol, 0.8);
      if (Math.abs(j.x - Arena.BRAMA_X) < 3.4 && j.z > Arena.BRAMA_Z0 - 1.5 && j.z < Arena.BRAMA_Z1 + 1.5) {
        j.yaw = -90;
        if (j.cialo != null) j.cialo.lookAt(arena.loc(Arena.BRAMA_X, Arena.BRAMA_Z0 + 1).add(0, 1, 0));
        if (tik >= j.gotowyOd) uderzBrame(j); else j.akcja = "bije brame";
      } else j.akcja = "maszeruje";
      return;
    }
    punkt(j, Arena.MAG_X0 - 1.0 - (j.slot / 3), Arena.DRZWI_Z - 1 + j.slot % 3, 0.8);
    j.akcja = Math.hypot(Arena.MAG_X0 - 0.5 - j.x, Arena.DRZWI_Z + 0.5 - j.z) < 2.8 ? "grabi" : "szturmuje";
  }

  /** Rozkaz marszu do kratki (tx, tz) areny; true = juz na miejscu. */
  /** Pas wyjscia napastnikow: x areny ponizej tego = zszedl z pola. */
  static final double WYJSCIE_X = 6;

  private static boolean punkt(Jednostka j, double tx, double tz, double r) {
    j.px = tx + 0.5; j.pz = tz + 0.5; j.pr = r;
    return j.odl(j.px, j.pz) <= r;
  }

  /**
   * Wykonawca cial (co 5 tikow na jednostke): walka = setTarget i AI vanilla; marsz = krotki skok
   * pathfindera ({@code lod.krok-sciezki} kratek po polu przeplywu), bo A* na krotki odcinek ma staly,
   * maly koszt. Nowa sciezka tylko przy zmianie celu, przy punkcie posrednim, po utracie albo co 2 s,
   * najwyzej {@code lod.sciezki-na-tik} na caly serwer; moveTo=false wstrzymuje jednostke na 1 s.
   */
  private void wykonajCialem(Jednostka j) {
    Mob m = j.cialo;
    LivingEntity cel = j.cel != null ? j.cel.cialo : j.celGracz;
    if (Double.isNaN(j.px) && cel != null) {
      if (m.getTarget() != cel) m.setTarget(cel);
      j.ruchX = j.ruchDoX = Double.NaN;
      return;
    }
    if (m.getTarget() != null) m.setTarget(null);
    var pf = m.getPathfinder();
    if (Double.isNaN(j.px) || j.odl(j.px, j.pz) <= j.pr) {
      if (pf.hasPath()) pf.stopPathfinding();
      j.ruchX = j.ruchDoX = Double.NaN;
      return;
    }
    if (tik < j.ruchWstrzymanyDo) return;
    boolean nowyCel = j.ruchDoX != j.px || j.ruchDoZ != j.pz;
    boolean przyPunkcie = !Double.isNaN(j.ruchX) && j.odl(j.ruchX, j.ruchZ) < 2;
    if (!nowyCel && !przyPunkcie && pf.hasPath() && tik - j.ruchOd <= 40) return;
    if (!pl.sciezka()) return;   // budzet sciezek tego tiku wyczerpany: sprobuje przy nastepnej decyzji
    double[] w = arena.uklad.posredni(j.x, j.z, j.px, j.pz, pl.getConfig().getInt("lod.krok-sciezki", 6));
    boolean ok = pf.moveTo(new Location(arena.swiat, arena.ox + w[0], m.getLocation().getY(), arena.oz + w[1]), 1.0);
    j.ruchX = w[0]; j.ruchZ = w[1]; j.ruchDoX = j.px; j.ruchDoZ = j.pz; j.ruchOd = tik;
    if (!ok) j.ruchWstrzymanyDo = tik + 20;
  }

  private int odnowienie() { return 18 + los.nextInt(8); }

  private void uderzBrame(Jednostka j) {
    if (j.cialo != null) j.cialo.swingMainHand();
    hpBramy -= (int) pl.getConfig().getDouble("obrazenia", 3);
    j.gotowyOd = tik + odnowienie();
    j.akcja = "bije brame";
    if (faza == Faza.MARSZ) { faza = Faza.BRAMA; zdarzenie("faza", faza.name()); }
    boolean ciala = lod == Lod.CIALA;
    if (ciala && tik % 4 == 0) arena.swiat.playSound(arena.loc(Arena.BRAMA_X, Arena.BRAMA_Z0 + 1), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.7f, 1f);
    if (hpBramy <= 0) {
      hpBramy = 0;
      arena.uklad = Uklad.BRAMA_ZBURZONA;
      if (ciala) {
        arena.ustawBrame();
        arena.swiat.playSound(arena.loc(Arena.BRAMA_X, Arena.BRAMA_Z0 + 1), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.5f, 0.8f);
      }
      faza = Faza.SZTURM;
      zdarzenie("brama", "zburzona");
      zdarzenie("faza", faza.name());
    }
    odswiezNapis();
  }

  /** Trafienie w symulacji (cialo celu, jesli jest, dostaje obrazenia przez zdarzenie). */
  void trafienie(Jednostka a, Jednostka c, double dmg, boolean strzala) {
    zdarzenie("traf", a.id + ">" + c.id + ":" + r1(dmg) + ":" + r1(a.odl(c.x, c.z)) + ":" + (strzala ? "strzala" : "wrecz"));
    if (c.cialo != null) { c.cialo.damage(dmg); return; }
    c.hp -= dmg;
    if (c.hp <= 0) padl(c);
  }

  /** Smierc jednostki (cialo: z EntityDeathEvent, symulacja: z trafienia). */
  void padl(Jednostka j) {
    if (!j.zywa) return;
    j.zywa = false;
    j.hp = 0;
    j.akcja = "padl";
    if (j.cialo != null) { pl.cialaJednostek.remove(j.cialo.getUniqueId()); j.cialo = null; }
    zdarzenie("padl", String.valueOf(j.id));
  }

  /** Strona encji w tym napadzie: cialo jednostki albo dolaczony gracz (nie polegly, nie widz). */
  Strona stronaEncji(Entity e) {
    if (e instanceof Player p) {
      var d = pl.dolaczeni.get(p.getUniqueId());
      return d != null && d.napad == id && !d.polegl && !d.widz ? d.strona : null;
    }
    Jednostka j = e == null ? null : pl.cialaJednostek.get(e.getUniqueId());
    return j != null && j.napad == this ? j.strona : null;
  }

  String idJednostki(Entity e) {
    Jednostka j = e == null ? null : pl.cialaJednostek.get(e.getUniqueId());
    if (j != null) return String.valueOf(j.id);
    return e instanceof Player p ? p.getName() : "?";
  }

  private void sprawdzFazy() {
    long atakZywi = 0, atakWalczy = 0, obronaZywi = 0, grabia = 0;
    boolean obronaPrzy = false, wszyscyUciekli = true;
    for (Jednostka j : jednostki) {
      if (!j.zywa) continue;
      if (j.strona == Strona.ATAK) {
        atakZywi++;
        if (!j.ranny) atakWalczy++;
        if (!"uciekl".equals(j.akcja)) wszyscyUciekli = false;
      } else {
        obronaZywi++;
        if (j.odl(Arena.MAG_X0 + 0.5, Arena.DRZWI_Z + 0.5) < 4) obronaPrzy = true;
      }
      if ("grabi".equals(j.akcja)) grabia++;
    }
    var c = pl.getConfig();
    if ((faza == Faza.SZTURM || faza == Faza.GRABIEZ) && grabia > 0 && !obronaPrzy) {
      if (faza == Faza.SZTURM) { faza = Faza.GRABIEZ; zdarzenie("faza", faza.name()); }
      lup = (int) Math.min(c.getInt("lup-max", 100), lup + grabia * 2);
      if (lup >= c.getInt("lup-max", 100)) { wynik = "napastnicy zlupili magazyn"; odwrot(); }
    }
    if (faza != Faza.ODWROT && atakWalczy <= napastnikowStart * (1 - c.getDouble("morale-odwrot", 0.7))) {
      wynik = "napastnicy zalamani (" + atakWalczy + " zdolnych do walki, " + atakZywi + " zywych z " + napastnikowStart + ")";
      odwrot();
    }
    if (atakZywi == 0) zakoncz(wynik != null ? wynik : "napastnicy wybici");
    else if (faza == Faza.ODWROT && wszyscyUciekli) zakoncz(wynik + ", odwrot zakonczony");
    else if (obronaZywi == 0 && faza != Faza.ODWROT && faza.ordinal() >= Faza.SZTURM.ordinal() && wynik == null)
      zdarzenie("obrona", "wybita");
  }

  private void odwrot() {
    if (faza == Faza.ODWROT) return;
    faza = Faza.ODWROT;
    zdarzenie("faza", faza.name() + ":" + wynik);
  }

  private void odswiezNapis() {
    if (napisBramy == null || !napisBramy.isValid()) return;
    int max = pl.getConfig().getInt("hp-bramy", 300);
    napisBramy.text(hpBramy > 0
        ? Component.text("Brama " + (100 * hpBramy / max) + "%", NamedTextColor.GOLD)
        : Component.text("Brama zburzona", NamedTextColor.RED));
  }

  /** Punkt widza: nad murem, widac brame i magazyn. */
  Location punktWidza() {
    Location l = arena.loc(14, 20).add(0, 10, 0);
    l.setYaw(-90);
    l.setPitch(35);
    return l;
  }

  /** Polegli i widzowie ogladaja do konca, ale nie odlatuja dalej niz 80 bl. od areny ani do innego swiata. */
  private void pilnujWidzow() {
    Location srodek = arena.loc(24, 20);
    for (var e : pl.dolaczeni.entrySet()) {
      var d = e.getValue();
      if (d.napad != id || !(d.polegl || d.widz)) continue;
      Player p = Bukkit.getPlayer(e.getKey());
      if (p == null || p.getGameMode() != GameMode.SPECTATOR) continue;
      if (p.getWorld() != arena.swiat || p.getLocation().distanceSquared(srodek) > 80 * 80) p.teleportAsync(punktWidza());
    }
  }

  Location punktDolaczenia(Strona s) {
    Location l = s == Strona.ATAK ? arena.loc(4, 20) : arena.loc(30, 26);
    l.setYaw(s == Strona.ATAK ? -90 : 90);
    return l;
  }

  // ---------------------------------------------------------------- koniec
  boolean skonczony() { return faza == Faza.KONIEC; }

  /** Koniec: ciala, napis, strzaly i bilety kolejka (bez skoku tiku), brama w blokach wraca do stania. */
  void zakoncz(String powod) {
    if (faza == Faza.KONIEC) return;
    Lod byl = lod;
    faza = Faza.KONIEC;
    zdarzenie("koniec", powod);
    usunCiala();
    poMaterializacji.clear();
    arena.uklad = Uklad.BRAMA_STOI;
    if (byl == Lod.CIALA || byl == Lod.DEMATERIALIZUJE || byl == Lod.MATERIALIZUJE)
      pl.praca(() -> { if (arena.chunkiZaladowane()) arena.ustawBrame(); sprzatnijArene(); });
    pl.zwolnijGraczy(id, powod);
    String dane = zapis.toString();
    String opis = opis();
    File f = new File(pl.getDataFolder(), "zapisy/napad-" + id + "-" + System.currentTimeMillis() / 1000 + ".jsonl.gz");
    Runnable zapisz = () -> {
      f.getParentFile().mkdirs();
      try (var out = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(f)), StandardCharsets.UTF_8)) {
        out.write(dane);
      } catch (IOException e) { pl.getLogger().warning("Zapis " + f + ": " + e); }
      pl.getLogger().info("Napad " + id + " koniec: " + powod + " | " + opis + " | zapis " + f.getName() + " "
          + f.length() + " B (surowo " + dane.length() + " zn.)");
    };
    // przy wylaczaniu pluginu planista nie przyjmuje zadan: zapis od razu
    if (pl.isEnabled()) Bukkit.getScheduler().runTaskAsynchronously(pl, zapisz); else zapisz.run();
  }

  // ---------------------------------------------------------------- pomiar i zapis
  void pomiar(long ns) {
    sumaNs += ns;
    tikiPomiaru++;
    if (ns > maxNs) maxNs = ns;
  }

  String opis() {
    long a = 0, o = 0;
    for (Jednostka j : jednostki) if (j.zywa) { if (j.strona == Strona.ATAK) a++; else o++; }
    return String.format(Locale.ROOT, "#%d %s t=%ds atak %d obrona %d brama %d lup %d gracze ciosy %d/%d | sr %.0f us max %.0f us",
        id, faza, tik / 20, a, o, hpBramy, lup, ciosyGraczy, ciosyGraczy + ciosyGraczyBlok, tikiPomiaru == 0 ? 0.0 : sumaNs / 1000.0 / tikiPomiaru, maxNs / 1000.0)
        + " | lod " + lod + " ciala " + policzCiala() + "/lib " + przebrane + " materializacji " + materializacji + " | ziarno " + ziarno;
  }

  private void zapiszNaglowek() {
    zapis.append("{\"napad\":").append(id).append(",\"arena\":[").append(arena.ox).append(',').append(arena.oz)
        .append("],\"ziarno\":").append(ziarno).append(",\"lod\":\"").append(pl.getConfig().getString("lod.tryb", "auto"))
        .append("\",\"jednostki\":[");
    for (int i = 0; i < jednostki.size(); i++) {
      Jednostka j = jednostki.get(i);
      if (i > 0) zapis.append(',');
      zapis.append("{\"id\":").append(j.id).append(",\"s\":\"").append(j.strona.name()).append("\",\"r\":\"")
          .append(j.rola.name()).append("\"}");
    }
    zapis.append("]}\n");
  }

  /** Klatka z modelu (to samo przy cialach i w symulacji); m = c (ciala) | s (symulacja). */
  private void zapiszKlatke() {
    zapis.append("{\"t\":").append(tik).append(",\"f\":\"").append(faza.name()).append("\",\"m\":\"")
        .append(lod == Lod.CIALA ? 'c' : 's').append("\",\"b\":").append(hpBramy).append(",\"l\":").append(lup).append(",\"u\":[");
    boolean pierwszy = true;
    for (Jednostka j : jednostki) {
      if (!j.zywa) continue;
      if (!pierwszy) zapis.append(',');
      pierwszy = false;
      zapis.append('[').append(j.id).append(',').append(r1(j.x)).append(',').append(Arena.Y + (j.naMurze ? Arena.MUR_H : 0))
          .append(',').append(r1(j.z)).append(',').append(Math.round(j.yaw)).append(',')
          .append(Math.round(j.hp)).append(",\"").append(j.akcja).append("\"]");
    }
    for (var e : pl.dolaczeni.entrySet())
      if (e.getValue().napad == id && !e.getValue().widz) {
        Player p = Bukkit.getPlayer(e.getKey());
        if (p == null) continue;
        Location l = p.getLocation();
        zapis.append(pierwszy ? "" : ",").append("[\"").append(p.getName()).append("\",").append(r1(l.getX() - arena.ox))
            .append(',').append(r1(l.getY())).append(',').append(r1(l.getZ() - arena.oz)).append(',')
            .append(Math.round(l.getYaw())).append(',').append(Math.round(p.getHealth())).append(",\"gracz\"]");
        pierwszy = false;
      }
    zapis.append("]}\n");
  }

  void zdarzenie(String typ, String dane) {
    zapis.append("{\"t\":").append(tik).append(",\"e\":\"").append(typ).append("\",\"d\":\"")
        .append(dane.replace("\"", "'")).append("\"}\n");
  }

  static String r1(double v) { return String.valueOf(Math.round(v * 10) / 10.0); }
}
