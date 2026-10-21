package pl.mcmmorpg.chowance.gra;

import java.time.Duration;
import java.util.IdentityHashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.integracja.Mythic;
import pl.mcmmorpg.chowance.logika.SilnikWalki.Zdarzenie;
import pl.mcmmorpg.chowance.logika.Wojownik;
import pl.mcmmorpg.chowance.model.Ruch;

/**
 * Odgrywanie walki na arenie (widać, co robią stwory): doskok albo pocisk, skill ruchu z MythicMobs,
 * liczby obrażeń nad celem, pasek HP spadający krok po kroku, napis na ekranie, omdlenie, drżenie pieczęci.
 * Wspólne dla walki turowej (krok po kroku) i pojedynku real-time (pojedyncze trafienia).
 */
public final class Scena {
  private final Chowance c;
  private final Ciala ciala;

  /** Stan pokazywany graczom w trakcie odtwarzania (silnik zna już wynik całej tury). */
  public static final class Stan {
    public final Map<Wojownik, Double> hp = new IdentityHashMap<>();
    public final Wojownik[] pokazany = new Wojownik[2];
  }

  /** Gracze z paskami walki (HUD) dostają opis kroków w pasku, nie na środku ekranu (zasłaniał stwory). */
  public java.util.function.Predicate<Player> bezNapisow = p -> false;
  /**
   * Walczący z panelami MythicHUD nie widzą etykiet HP nad stworami — przy kamerze tuż za stworem etykieta
   * zasłaniała kadr i dublowała panel (klient QA 2026-09-30). Widzowie z boku widzą je nadal.
   */
  public java.util.function.Predicate<Player> bezEtykiet = p -> false;
  /** Kandydaci do ukrycia etykiet: tylko gracze w kamerze walki (nie wszyscy gracze świata co 2 ticki). */
  public java.util.function.Supplier<java.util.Collection<Player>> walczacy = java.util.List::of;
  /** Komu która etykieta jest ukryta (żeby pokazać ją z powrotem, gdy gracz wyjdzie z walki). */
  private final Map<java.util.UUID, java.util.Set<java.util.UUID>> ukryte = new java.util.HashMap<>();

  public Scena(Chowance c, Ciala ciala) {
    this.c = c;
    this.ciala = ciala;
    // Etykiety podążają za stworami co 2 ticki. NIE jako pasażer: teleport ciała (doskok, odrzut) zrzuca
    // pasażerów — etykieta zostawała w miejscu (klient QA 2026-09-27: pasek Iskrzyka wisiał przy trenerze).
    Bukkit.getScheduler().runTaskTimer(c.plugin, this::prowadzEtykiety, 2L, 2L);
  }

  private void prowadzEtykiety() {
    if (etykiety.isEmpty()) return;
    java.util.Collection<Player> wal = walczacy.get(); // raz na przebieg, nie na etykietę
    for (var it = etykiety.entrySet().iterator(); it.hasNext(); ) {
      var e = it.next();
      LivingEntity cialo = cialaEtykiet.get(e.getKey());
      TextDisplay d = e.getValue();
      if (cialo == null || !cialo.isValid() || !d.isValid()) { d.remove(); cialaEtykiet.remove(e.getKey()); it.remove(); continue; }
      Location l = cialo.getLocation().add(0, cialo.getHeight() + 0.55, 0);
      if (!l.getWorld().equals(d.getWorld()) || l.distanceSquared(d.getLocation()) > 0.0025) d.teleport(l);
      java.util.Set<java.util.UUID> ukr = ukryte.get(d.getUniqueId());
      for (Player p : wal) {
        if (!bezEtykiet.test(p) || (ukr != null && ukr.contains(p.getUniqueId()))) continue;
        p.hideEntity(c.plugin, d);
        ukryte.computeIfAbsent(d.getUniqueId(), k -> new java.util.HashSet<>()).add(p.getUniqueId());
      }
      if (ukr != null && !ukr.isEmpty()) {
        for (var i = ukr.iterator(); i.hasNext(); ) {
          Player p = Bukkit.getPlayer(i.next());
          if (p == null) { i.remove(); continue; }
          if (!bezEtykiet.test(p)) { p.showEntity(c.plugin, d); i.remove(); }
        }
      }
    }
    if (!ukryte.isEmpty()) ukryte.keySet().retainAll(etykiety.values().stream().map(org.bukkit.entity.Entity::getUniqueId).collect(java.util.stream.Collectors.toSet()));
  }

  /** Odgrywa jedno zdarzenie; zwraca, ile ticków czekać do następnego. */
  public int krok(Player[] gracze, LivingEntity[] cialaWalki, Zdarzenie e, Stan st, Ruch ruch, int t) {
    int s = e.strona();
    LivingEntity moje = s >= 0 && s < 2 ? cialaWalki[s] : null;
    LivingEntity jego = s >= 0 && s < 2 ? cialaWalki[1 - s] : null;
    switch (e.rodzaj()) {
      case RUCH -> {
        napisWszystkim(gracze, "<white>" + e.tekst(), t + 10);
        // następny krok (obrażenia) wypada DOKŁADNIE w chwili trafienia
        return Math.max(4, atak(moje, jego, ruch));
      }
      case OBRAZENIA -> {
        Wojownik w = st.pokazany[s];
        if (w != null) {
          st.hp.put(w, Math.max(0, st.hp.getOrDefault(w, w.hp) - e.wartosc()));
          pasek(moje, w, st.hp.get(w));
        }
        boolean kryt = e.tekst() != null && e.tekst().contains("krytyczne");
        uderzenie(moje, jego, e.wartosc(), e.skutecznosc(), kryt, gracze, gracze[s], ruch);
        napisWszystkim(gracze, "<gray>" + e.tekst(), t);
        return t;
      }
      case PUDLO -> {
        liczba(jego, "<gray><i>Chybienie!");
        napisWszystkim(gracze, "<gray>" + e.tekst(), t);
        return Math.max(8, t * 2 / 3);
      }
      case LECZENIE -> {
        Wojownik w = st.pokazany[s];
        if (w != null) {
          st.hp.put(w, Math.min(w.hpMax, st.hp.getOrDefault(w, w.hp) + e.wartosc()));
          pasek(moje, w, st.hp.get(w));
        }
        if (moje != null && moje.isValid()) moje.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, moje.getLocation().add(0, 1, 0), 12, 0.4, 0.5, 0.4, 0);
        liczba(moje, "<green><b>+" + e.wartosc());
        napisWszystkim(gracze, "<green>" + e.tekst(), t);
        return Math.max(8, t * 2 / 3);
      }
      case STATUS, TEKST -> {
        if (moje != null) liczba(moje, "<light_purple>" + krotko(e.tekst()));
        napisWszystkim(gracze, "<light_purple>" + e.tekst(), t);
        return Math.max(8, t * 2 / 3);
      }
      case ZMIANA -> {
        napisWszystkim(gracze, "<aqua>" + e.tekst(), t + 6);
        if (moje != null && moje.isValid()) {
          moje.getWorld().spawnParticle(Particle.END_ROD, moje.getLocation().add(0, 0.8, 0), 30, 0.4, 0.6, 0.4, 0.03);
          moje.getWorld().playSound(moje.getLocation(), Sound.ENTITY_ALLAY_ITEM_GIVEN, 0.8f, 1.2f);
          Wojownik w = st.pokazany[s];
          if (w != null) pasek(moje, w, st.hp.getOrDefault(w, w.hp));
        }
        return t;
      }
      case ZEMDLAL -> {
        Wojownik w = st.pokazany[s];
        if (w != null) st.hp.put(w, 0.0);
        if (moje != null && moje.isValid()) {
          if (w != null) ciala.nazwij(moje, w.o, "<red>omdlał");
          Mythic.sygnal(moje, "OMDLENIE", null);
          moje.getWorld().spawnParticle(Particle.POOF, moje.getLocation().add(0, 0.6, 0), 25, 0.35, 0.35, 0.35, 0.02);
          moje.getWorld().playSound(moje.getLocation(), Sound.ENTITY_GENERIC_DEATH, 0.8f, 1.0f);
          opadnij(moje, t);
        }
        napisWszystkim(gracze, "<red><b>" + e.tekst(), t + 10);
        return t + 10;
      }
      case KONIEC -> {
        // krótko: po nim zakoncz pokazuje własne podsumowanie (wynik + exp)
        for (Player p : gracze) if (p != null && p.isOnline()) tytul(p, "<gold><b>" + e.tekst(), "", t);
        return t;
      }
      default -> { return 0; }
    }
  }

  // ------------------------------------------------------------------ atak

  /**
   * Zapowiedź ruchu w real-time (zamach): „⚠ Ruch” w kolorze typu nad stworem, zwężający się pierścień
   * cząsteczek u stóp i dźwięk. To okno uniku przeciwnika — bez niej unik był zgadywaniem.
   */
  public void zapowiedz(LivingEntity at, Ruch r, String prefiks, int ticki) {
    if (at == null || !at.isValid() || r == null) return;
    String kol = r.typ().kolor();
    Location l = at.getLocation().add(0, at.getHeight() + 1.45, 0);
    TextDisplay d = l.getWorld().spawn(l, TextDisplay.class, x -> {
      x.setPersistent(false);
      x.text(Chowance.mm(prefiks + kol + "<b>⚠ " + r.nazwa()));
      x.setBillboard(Display.Billboard.CENTER);
      x.setBackgroundColor(Color.fromARGB(110, 60, 0, 0));
      x.setShadowed(true);
      x.setSeeThrough(true);
      x.setTransformation(skalowanie(1.3f));
    });
    Color c0 = kolor(r);
    at.getWorld().playSound(at.getLocation(), r.kontakt() ? Sound.ENTITY_WOLF_GROWL : Sound.ENTITY_EVOKER_PREPARE_ATTACK, 0.9f, r.kontakt() ? 0.8f : 1.4f);
    new BukkitRunnable() {
      int i = 0;
      @Override public void run() {
        if (!at.isValid() || i > ticki) { d.remove(); cancel(); return; }
        d.teleport(at.getLocation().add(0, at.getHeight() + 1.45, 0));
        if (i % 2 == 0) {
          double rad = 1.3 * (1 - (double) i / Math.max(1, ticki)) + 0.25;
          Location sr = at.getLocation().add(0, 0.1, 0);
          for (int k = 0; k < 12; k++) {
            double a = Math.PI * 2 * k / 12;
            sr.getWorld().spawnParticle(Particle.DUST, sr.clone().add(Math.cos(a) * rad, 0, Math.sin(a) * rad), 1, 0, 0, 0, 0, new Particle.DustOptions(c0, 1.2f));
          }
        }
        i++;
      }
    }.runTaskTimer(c.plugin, 0L, 1L);
  }

  /**
   * Doskok (ruch kontaktowy) albo pocisk (dystansowy) + skill ruchu z MythicMobs jako wizualia.
   * Zwraca, po ilu tickach cios TRAFIA — wtedy trzeba zagrać {@link #uderzenie}.
   */
  public int atak(LivingEntity at, LivingEntity cel, Ruch r) {
    if (at == null || !at.isValid()) return 0;
    if (r != null && !r.skill().isBlank()) Mythic.skill(at, r.skill(), cel);
    if (cel == null || !cel.isValid()) return 0;
    boolean kontakt = r == null || r.kontakt();
    if (kontakt) return doskok(at, cel);
    at.getWorld().playSound(at.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.6f, 1.4f);
    return pocisk(at, cel, r, 8);
  }

  /**
   * Zamach (3 t cofnięcia), skok (4 t), stop-klatka na celu (4 t), powrót (7 t). Trafienie w 7. ticku.
   * Wcześniej jednolity, płytki podskok i obrażenia w osobnym kroku — właściciel: „za słaby feeling”.
   */
  private int doskok(LivingEntity at, LivingEntity cel) {
    Location start = at.getLocation().clone();
    Vector d = cel.getLocation().toVector().subtract(start.toVector()).setY(0);
    double dl = d.length();
    if (dl < 0.8) return 3;
    Vector kier = d.clone().normalize();
    Vector droga = kier.clone().multiply(Math.max(0.3, dl - 1.1));
    final int zamach = 3, skok = 4, stop = 4, powrot = 7;
    at.getWorld().playSound(at.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 0.8f);
    new BukkitRunnable() {
      int i = 0;
      @Override public void run() {
        if (!at.isValid() || i > zamach + skok + stop + powrot) { if (at.isValid()) Przesun.animuj(at, start, c.log); cancel(); return; }
        Vector v;
        double h = 0;
        if (i <= zamach) v = kier.clone().multiply(-0.35 * i / zamach);
        else if (i <= zamach + skok) { double f = (double) (i - zamach) / skok; v = kier.clone().multiply(-0.35).add(droga.clone().add(kier.clone().multiply(0.35)).multiply(f * f)); h = Math.sin(Math.PI * f) * 0.45; }
        else if (i <= zamach + skok + stop) v = droga.clone();
        else { double f = (double) (i - zamach - skok - stop) / powrot; f = f * f * (3 - 2 * f); v = droga.clone().multiply(1 - f); }
        Location l = start.clone().add(v).add(0, h, 0);
        l.setYaw(start.getYaw());
        l.setPitch(start.getPitch());
        Przesun.animuj(at, l, c.log);
        i++;
      }
    }.runTaskTimer(c.plugin, 0L, 1L);
    return zamach + skok;
  }

  private int pocisk(LivingEntity at, LivingEntity cel, Ruch r, int ticki) {
    Color kol = kolor(r);
    Location a = at.getEyeLocation().clone();
    Location b = cel.getLocation().add(0, cel.getHeight() * 0.6, 0);
    Vector krok = b.toVector().subtract(a.toVector()).multiply(1.0 / ticki);
    new BukkitRunnable() {
      int i = 0;
      final Location l = a.clone();
      @Override public void run() {
        if (i++ >= ticki || !cel.isValid()) { cancel(); return; }
        l.add(krok);
        l.getWorld().spawnParticle(Particle.DUST, l, 10, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(kol, 1.6f));
        l.getWorld().spawnParticle(Particle.DUST, l.clone().subtract(krok.clone().multiply(0.5)), 4, 0.05, 0.05, 0.05, 0, new Particle.DustOptions(kol, 0.9f));
      }
    }.runTaskTimer(c.plugin, 0L, 1L);
    return ticki;
  }

  /**
   * Chwila trafienia: odrzut celu, błysk i cząsteczki w kolorze typu, warstwowy dźwięk, duża liczba,
   * trzęsienie kamery (hurt-animacja gracza bez obrażeń) u właściciela trafionego — przy krytyku
   * i „bardzo skutecznym” u wszystkich.
   */
  public void uderzenie(LivingEntity cel, LivingEntity od, int obrazenia, double skut, boolean kryt, Player[] widzowie, Player wlascicielCelu, Ruch r) {
    if (cel == null || !cel.isValid()) return;
    Location srodek = cel.getLocation().add(0, cel.getHeight() * 0.55, 0);
    boolean mocne = kryt || skut >= 2;
    if (obrazenia <= 0 && skut == 0) {
      liczba(cel, "<gray><b>Bez efektu", 1.3f);
      cel.getWorld().playSound(srodek, Sound.BLOCK_ANVIL_LAND, 0.3f, 2.0f);
      return;
    }
    cel.playHurtAnimation(0);
    odrzut(cel, od, mocne ? 0.9 : 0.5);
    Color kol = kolor(r);
    cel.getWorld().spawnParticle(Particle.DUST, srodek, mocne ? 40 : 22, 0.35, 0.35, 0.35, 0, new Particle.DustOptions(kol, 1.8f));
    cel.getWorld().spawnParticle(Particle.CRIT, srodek, mocne ? 30 : 14, 0.3, 0.3, 0.3, 0.4);
    cel.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, srodek, Math.min(15, 2 + obrazenia / 3), 0.3, 0.3, 0.3, 0.15);
    if (mocne) cel.getWorld().spawnParticle(Particle.EXPLOSION, srodek, 1, 0, 0, 0, 0);
    cel.getWorld().playSound(srodek, kryt ? Sound.ENTITY_PLAYER_ATTACK_CRIT : Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.9f);
    cel.getWorld().playSound(srodek, Sound.ENTITY_PLAYER_HURT, 0.7f, 1.3f);
    if (mocne) cel.getWorld().playSound(srodek, Sound.ENTITY_GENERIC_EXPLODE, 0.35f, 1.7f);
    String dop = skut >= 2 ? "\n<green><b>Bardzo skuteczne!" : skut > 0 && skut < 1 ? "\n<gray>Mało skuteczne…" : "";
    liczba(cel, (kryt ? "<gold><b>KRYTYK! -" : "<red><b>-") + obrazenia + "</b>" + dop, mocne ? 2.4f : 1.6f);
    if (widzowie != null) for (Player p : widzowie) {
      if (p == null || !p.isOnline()) continue;
      if (mocne || p.equals(wlascicielCelu)) p.sendHurtAnimation(0);
    }
  }

  /** Odrzut: cel odlatuje od atakującego i wraca na swoje miejsce (ciała walki mają wyłączone AI). */
  private void odrzut(LivingEntity cel, LivingEntity od, double sila) {
    if (od == null || !od.isValid()) return;
    if (cel.hasAI()) { cel.setVelocity(cel.getLocation().toVector().subtract(od.getLocation().toVector()).setY(0).normalize().multiply(sila * 0.6).setY(0.25)); return; }
    Location start = cel.getLocation().clone();
    Vector kier = start.toVector().subtract(od.getLocation().toVector()).setY(0);
    if (kier.lengthSquared() < 1e-4) return;
    kier.normalize().multiply(sila);
    new BukkitRunnable() {
      int i = 0;
      @Override public void run() {
        if (!cel.isValid() || i > 10) { if (cel.isValid()) Przesun.animuj(cel, start, c.log); cancel(); return; }
        double f = i <= 3 ? i / 3.0 : 1 - (i - 3) / 7.0;
        Location l = start.clone().add(kier.clone().multiply(f)).add(0, i <= 3 ? 0.15 * f : 0, 0);
        l.setYaw(start.getYaw());
        l.setPitch(start.getPitch());
        Przesun.animuj(cel, l, c.log);
        i++;
      }
    }.runTaskTimer(c.plugin, 0L, 1L);
  }

  private static Color kolor(Ruch r) {
    if (r == null) return Color.WHITE;
    return switch (r.typ()) {
      case OGIEN -> Color.fromRGB(255, 120, 30);
      case WODA -> Color.fromRGB(60, 140, 255);
      case ZIEMIA -> Color.fromRGB(150, 110, 60);
      case POWIETRZE -> Color.fromRGB(200, 235, 255);
      case NATURA -> Color.fromRGB(90, 200, 70);
      case MROK -> Color.fromRGB(90, 40, 120);
      case SWIATLO -> Color.fromRGB(255, 240, 150);
      case METAL -> Color.fromRGB(180, 180, 190);
      case NIEUMARLY -> Color.fromRGB(120, 200, 170);
      default -> Color.WHITE;
    };
  }

  /** Omdlenie: ciało powoli osiada (bez zdejmowania — walka zdejmie je sama przy zmianie/końcu). */
  private void opadnij(LivingEntity e, int ticki) {
    Location start = e.getLocation().clone();
    new BukkitRunnable() {
      int i = 0;
      @Override public void run() {
        if (!e.isValid() || i++ >= ticki) { cancel(); return; }
        Location l = start.clone().add(0, -0.4 * i / ticki, 0);
        l.setPitch(Math.min(90, 90f * i / ticki));
        Przesun.animuj(e, l, c.log);
      }
    }.runTaskTimer(c.plugin, 0L, 1L);
  }

  // ------------------------------------------------------------------ pieczęć

  /** Rzut pieczęcią w walce turowej: łuk, drżenia z dźwiękiem, wynik. Zwraca czas trwania w tickach. */
  public int pieczec(Player[] gracze, LivingEntity od, LivingEntity cel, int drzenia, boolean sukces, int t) {
    if (cel == null || !cel.isValid()) return 1;
    Location a = od != null && od.isValid() ? od.getLocation().add(0, 1.2, 0) : cel.getLocation().add(0, 3, 0);
    Location b = cel.getLocation().add(0, cel.getHeight() * 0.5, 0);
    int lot = 12;
    new BukkitRunnable() {
      int i = 0;
      @Override public void run() {
        if (i > lot) { cancel(); return; }
        double f = (double) i / lot;
        Location l = a.clone().add(b.toVector().subtract(a.toVector()).multiply(f)).add(0, Math.sin(Math.PI * f) * 1.5, 0);
        l.getWorld().spawnParticle(Particle.DUST, l, 5, 0.05, 0.05, 0.05, 0, new Particle.DustOptions(Color.fromRGB(255, 210, 90), 1.3f));
        i++;
      }
    }.runTaskTimer(c.plugin, 0L, 1L);
    napisWszystkim(gracze, "<gold>Rzut pieczęcią!", lot + 4);
    // trafienie: model MEG kurczy się do pieczęci (setInvisible nie chowa modelu), przy „Wyrywa się!” wraca
    boolean meg = pl.mcmmorpg.chowance.integracja.SkalaModelu.maModel(cel);
    double skala0 = meg ? pl.mcmmorpg.chowance.integracja.SkalaModelu.skala(cel) : 1.0;
    if (meg) {
      for (int i = 1; i <= 6; i++) {
        double f = Math.max(0.1, 1.0 - i / 6.0);
        Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
          if (cel.isValid()) pl.mcmmorpg.chowance.integracja.SkalaModelu.ustaw(cel, skala0 * f);
        }, lot + i);
      }
    }
    int krok = Math.max(12, t);
    for (int n = 1; n <= drzenia; n++) {
      int nr = n;
      Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
        if (!cel.isValid()) return;
        cel.getWorld().playSound(cel.getLocation(), Sound.BLOCK_CHAIN_HIT, 1f, 0.8f + nr * 0.15f);
        cel.getWorld().spawnParticle(Particle.ENCHANT, cel.getLocation().add(0, 0.8, 0), 30, 0.3, 0.4, 0.3, 0.5);
        Location l = cel.getLocation();
        l.setYaw(l.getYaw() + (nr % 2 == 0 ? 20 : -20));
        Przesun.animuj(cel, l, c.log);
        napisWszystkim(gracze, "<gold>Pieczęć drży… " + "●".repeat(nr), krok);
      }, lot + (long) krok * (n - 1));
    }
    int koniec = lot + krok * Math.max(1, drzenia);
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      if (!cel.isValid()) return;
      if (sukces) {
        cel.getWorld().spawnParticle(Particle.FIREWORK, cel.getLocation().add(0, 1, 0), 40, 0.4, 0.6, 0.4, 0.05);
        cel.getWorld().playSound(cel.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        for (Player p : gracze) if (p != null && p.isOnline()) tytul(p, "<green><b>Złapany!", "", krok * 2);
      } else {
        cel.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, cel.getLocation().add(0, 1.2, 0), 8, 0.4, 0.3, 0.4, 0);
        cel.getWorld().playSound(cel.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1f);
        if (meg) pl.mcmmorpg.chowance.integracja.SkalaModelu.ustaw(cel, skala0);
        napisWszystkim(gracze, "<red><b>Wyrywa się!", krok);
      }
    }, koniec);
    return koniec + krok;
  }

  // ------------------------------------------------------------------ napisy, liczby, paski

  /** Etykiety nad stworami w walce (ciało -> TextDisplay jadący na ciele). */
  private final Map<java.util.UUID, TextDisplay> etykiety = new java.util.concurrent.ConcurrentHashMap<>();
  private final Map<java.util.UUID, LivingEntity> cialaEtykiet = new java.util.concurrent.ConcurrentHashMap<>();

  /**
   * Pasek HP według stanu pokazywanego (nie końcowego): w nazwie ciała i na własnej etykiecie nad nim —
   * ModelEngine ukrywa zwykłą nazwę ciała z modelem (klient QA 2026-09-27: nad stworami nie było nic widać).
   */
  /** Ostatni pokazany stan paska ciała — bez zmiany nie parsujemy MiniMessage i nie wysyłamy metadanych. */
  private final Map<java.util.UUID, String> ostatniPasek = new java.util.concurrent.ConcurrentHashMap<>();

  public void pasek(LivingEntity e, Wojownik w, double hp) {
    if (e == null || !e.isValid() || w == null) return;
    ciala.ustawZdrowie(e, w.hpMax, Math.max(0.5, hp)); // zawsze: tanie, pilnuje zdrowia ciała
    String klucz = w.o.id + "|" + w.o.poziom + "|" + (int) Math.ceil(hp) + "/" + w.hpMax;
    TextDisplay jest = etykiety.get(e.getUniqueId());
    if (klucz.equals(ostatniPasek.get(e.getUniqueId())) && jest != null && jest.isValid()) return;
    ostatniPasek.put(e.getUniqueId(), klucz);
    ciala.nazwij(e, w.o, pasekHp(hp, w.hpMax, 10));
    etykieta(e, c.nazwaKolor(w.o) + " <gray>Poz. " + w.o.poziom + "\n" + pasekHp(hp, w.hpMax, 12));
  }

  public void etykieta(LivingEntity e, String tekst) {
    TextDisplay d = etykiety.get(e.getUniqueId());
    if (d == null || !d.isValid()) {
      d = e.getWorld().spawn(e.getLocation().add(0, e.getHeight() + 0.55, 0), TextDisplay.class, x -> {
        x.setPersistent(false);
        x.setBillboard(Display.Billboard.CENTER);
        x.setBackgroundColor(Color.fromARGB(90, 0, 0, 0));
        x.setShadowed(true);
        x.setTeleportDuration(2);
      });
      etykiety.put(e.getUniqueId(), d);
      cialaEtykiet.put(e.getUniqueId(), e);
    }
    d.text(Chowance.mm(tekst));
  }

  /** Zdejmuje etykietę walki (koniec walki, zmiana stwora, dziki wraca do świata). */
  public void usunEtykiete(LivingEntity e) {
    if (e == null) return;
    ostatniPasek.remove(e.getUniqueId());
    TextDisplay d = etykiety.remove(e.getUniqueId());
    cialaEtykiet.remove(e.getUniqueId());
    if (d != null) d.remove();
  }

  public void usunWszystkieEtykiety() {
    for (TextDisplay d : etykiety.values()) d.remove();
    etykiety.clear();
    ostatniPasek.clear();
    cialaEtykiet.clear();
  }

  public static String pasekHp(double hp, int max, int dl) {
    double pr = max <= 0 ? 0 : Math.max(0, Math.min(100, hp * 100.0 / max));
    int pelne = (int) Math.ceil(pr / 100.0 * dl);
    String kol = pr > 50 ? "<green>" : pr > 20 ? "<yellow>" : "<red>";
    return kol + "▮".repeat(Math.max(0, pelne)) + "<dark_gray>" + "▮".repeat(Math.max(0, dl - pelne)) + "</dark_gray> <gray>" + (int) Math.ceil(hp) + "/" + max;
  }

  public void liczba(LivingEntity nad, String tekst) { liczba(nad, tekst, 1.2f); }

  /** Liczba/napis nad stworem: wyskakuje (skala 0,3 → docelowa), unosi się i znika. */
  public void liczba(LivingEntity nad, String tekst, float skala) {
    if (nad == null || !nad.isValid()) return;
    Location l = nad.getLocation().add((Math.random() - 0.5) * 0.4, nad.getHeight() + 0.5, (Math.random() - 0.5) * 0.4);
    TextDisplay d = l.getWorld().spawn(l, TextDisplay.class, x -> {
      x.setPersistent(false);
      x.text(Chowance.mm(tekst));
      x.setBillboard(Display.Billboard.CENTER);
      x.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
      x.setShadowed(true);
      x.setSeeThrough(true);
      x.setTeleportDuration(20);
      x.setTransformation(skalowanie(0.3f));
    });
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      if (!d.isValid()) return;
      d.setInterpolationDelay(0);
      d.setInterpolationDuration(4);
      d.setTransformation(skalowanie(skala));
      d.teleport(l.clone().add(0, 1.1, 0));
    }, 2L);
    Bukkit.getScheduler().runTaskLater(c.plugin, () -> {
      if (!d.isValid()) return;
      d.setInterpolationDelay(0);
      d.setInterpolationDuration(6);
      d.setTransformation(skalowanie(skala * 0.6f));
    }, 24L);
    Bukkit.getScheduler().runTaskLater(c.plugin, d::remove, 32L);
  }

  private static org.bukkit.util.Transformation skalowanie(float s) {
    return new org.bukkit.util.Transformation(new org.joml.Vector3f(), new org.joml.Quaternionf(), new org.joml.Vector3f(s, s, s), new org.joml.Quaternionf());
  }

  public void napis(Player p, String tekst, int ticki) {
    if (p == null || !p.isOnline()) return;
    p.showTitle(Title.title(Component.empty(), Chowance.mm(tekst),
        Title.Times.times(Duration.ZERO, Duration.ofMillis(ticki * 50L), Duration.ofMillis(150))));
  }

  public void napisWszystkim(Player[] gracze, String tekst, int ticki) {
    for (Player p : gracze) if (p != null && !bezNapisow.test(p)) napis(p, tekst, ticki);
  }

  public void tytul(Player p, String gora, String dol, int ticki) {
    p.showTitle(Title.title(Chowance.mm(gora), Chowance.mm(dol),
        Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(ticki * 50L), Duration.ofMillis(300))));
  }

  private static String krotko(String t) {
    return t.length() > 32 ? t.substring(0, 31) + "…" : t;
  }
}
