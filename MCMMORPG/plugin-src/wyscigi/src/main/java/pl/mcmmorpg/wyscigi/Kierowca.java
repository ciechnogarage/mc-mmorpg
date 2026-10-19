package pl.mcmmorpg.wyscigi;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.mount.controller.MountController;
import com.ticxo.modelengine.api.nms.entity.wrapper.MoveController;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Gracz w wyścigu i jego wierzchowiec. Fizyka jest nasza (ModelEngine podaje tylko wejścia WASD/skok/Shift):
 * kierunek goni wzrok gracza z ograniczoną szybkością skrętu, W/S zmieniają prędkość z bezwładnością,
 * Shift w zakręcie to poślizg ładujący turbo.
 */
final class Kierowca {
  final UUID id;
  final String nick;
  final Ustawienia.Gatunek gatunek;
  final Wyscig wyscig;
  final int nrStartu;
  Mob mob;
  ActiveModel model;

  // fizyka
  double v;
  float kierunek;
  boolean poslizg;
  int stronaPoslizgu, ladowanie;
  int turbo; double turboMnoznik = 1;
  int zamrozonyDo;              // tick serwera, do którego wierzchowiec stoi (start, powrót na tor)
  int karaDo;                   // "przegrzany" start: brak gazu do ticka
  int gazOd = -1;               // tick, od którego W jest trzymane (turbo startowe)
  boolean skokTrzymany, shiftTrzymany;
  int shiftPostoj;              // ticki Shift na postoju (wyjście)
  int[] poleOstatnio;
  double ostX = Double.NaN, ostZ, oczekiwane;
  int zderzenia;
  boolean pozaTorem;
  // postęp
  int indeks = -1;
  double postep, maxPostep;
  int okrazenieZgloszone;
  int startTick, metaTick = -1;
  double metaUlamek;
  long najlepszeOkrazenieMs = Long.MAX_VALUE;
  int okrazenieOdTick; double okrazenieOdUlamek;
  int pozycja;
  int bezKierowcy;
  MountController.MountInput wejscie;

  Kierowca(Player p, Ustawienia.Gatunek gatunek, Wyscig wyscig, int nrStartu) {
    this.id = p.getUniqueId();
    this.nick = p.getName();
    this.gatunek = gatunek;
    this.wyscig = wyscig;
    this.nrStartu = nrStartu;
    this.poleOstatnio = new int[wyscig.u.tor.pola.size()];
  }

  Player gracz() { return Bukkit.getPlayer(id); }
  boolean naMecie() { return metaTick >= 0; }

  /** Wywoływane przez ModelEngine co tick ruchu wierzchowca (wątek główny). */
  void krok(MoveController mc, MountController.MountInput in) {
    wejscie = in;
    Ustawienia u = wyscig.u;
    int teraz = Bukkit.getCurrentTick();
    Player p = gracz();
    if (p == null || mob == null || !mob.isValid()) return;
    float front = in == null ? 0 : in.getFront(), side = in == null ? 0 : in.getSide();
    boolean skok = in != null && in.isJump(), shift = in != null && in.isSneak();
    boolean naZiemi = mc.isOnGround();

    // Turbo startowe: liczy się, KIEDY gracz wcisnął gaz względem sygnału START.
    if (front > 0.1) { if (gazOd < 0) gazOd = teraz; } else gazOd = -1;

    if (teraz < zamrozonyDo || naMecie()) {
      double vy = mc.getVelocity().getY();
      mc.setVelocity(0, vy, 0);
      v = 0; poslizg = false; ladowanie = 0;
      shiftTrzymany = shift;
      if (shift && !naMecie()) shiftPostoj++; else shiftPostoj = 0;   // wyjście działa też w poczekalni i odliczaniu
      return;
    }

    double vmaxBaza = gatunek.predkosc();
    double vmax = vmaxBaza * (pozaTorem ? u.pozaTorem : 1.0);
    if (turbo > 0) { vmax = Math.max(vmax, vmaxBaza * turboMnoznik); turbo--; }

    // --- skręt: kierunek goni wzrok gracza
    float cel = p.getLocation().getYaw();
    float roznica = zawin(cel - kierunek);
    double udzial = Math.min(1, Math.abs(v) / vmaxBaza);
    double szybkoscSkretu = gatunek.skret() * (1 - u.skretPrzyPredkosci * udzial);
    if (Math.abs(v) < 0.04) szybkoscSkretu = gatunek.skret() * 0.7;   // obrót w miejscu
    if (poslizg) szybkoscSkretu *= u.poslizgSkret;
    float zmiana = (float) Math.max(-szybkoscSkretu, Math.min(szybkoscSkretu, roznica));
    if (v < -0.01) zmiana = -zmiana * 0.6f;                           // wsteczny skręca odwrotnie, jak auto
    kierunek = zawin(kierunek + zmiana);

    // --- poślizg i mini-turbo
    boolean nowyShift = shift && !shiftTrzymany;
    if (!poslizg && shift && naZiemi && v > u.poslizgMinPredkosc * vmaxBaza && Math.abs(roznica) > u.poslizgMinKat && (nowyShift || ladowanie == 0)) {
      poslizg = true;
      stronaPoslizgu = roznica > 0 ? 1 : -1;
      ladowanie = 0;
      mc.jump();                                                         // podskok na wejściu w poślizg
      mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_HORSE_LAND, 0.5f, 1.6f);
    }
    if (poslizg) {
      if (!shift || v < 0.35 * vmaxBaza) {
        int prog2 = (int) (u.turbo2 * ("zelaznorog".equals(gatunek.id()) ? 0.85 : 1)), prog1 = (int) (u.turbo1 * ("zelaznorog".equals(gatunek.id()) ? 0.85 : 1));
        if (ladowanie >= prog2) daj(u.turbo2Czas, u.turboMnoznik + 0.06, "turbo2");
        else if (ladowanie >= prog1) daj(u.turbo1Czas, u.turboMnoznik, "turbo1");
        poslizg = false;
        ladowanie = 0;
      } else {
        boolean wZakret = Math.signum(roznica) == stronaPoslizgu && Math.abs(roznica) > 12;
        ladowanie += wZakret ? 2 : 1;
        iskry();
      }
    }
    shiftTrzymany = shift;

    // --- gaz / hamulec
    if (teraz < karaDo) front = 0;
    // Gaz rozpędza tylko do vmax; nadwyżkę po turbo zdejmuje łagodnie linia niżej (inaczej gaz > zwalnianie = bez limitu).
    if (front > 0.1) { if (v < vmax) v = Math.min(vmax, v + gatunek.przyspieszenie() * (v < 0 ? 2.5 : 1) * (pozaTorem ? 0.7 : 1)); }
    else if (front < -0.1) v -= v > 0.01 ? u.hamowanie : gatunek.przyspieszenie() * 0.6;
    else v -= Math.signum(v) * Math.min(Math.abs(v), u.wybieg);
    if (v < -u.wsteczny) v = -u.wsteczny;
    if (v > vmax) v = Math.max(vmax, v - 0.02);                         // po turbo / wjeździe na pobocze zwalnia płynnie
    if (turbo > 0 && v < vmax) v = Math.min(vmax, v + 0.06);

    // --- wektor ruchu (w poślizgu ruch odchylony na zewnątrz zakrętu)
    double kat = Math.toRadians(kierunek - (poslizg ? stronaPoslizgu * u.katUslizgu : 0));
    double hx = -Math.sin(kat), hz = Math.cos(kat);
    double vx = hx * v, vz = hz * v;
    if (!poslizg && Math.abs(side) > 0.1 && Math.abs(v) > 0.05) {          // A/D: lekki unik w bok (side > 0 = A = w lewo)
      double prawoX = -Math.cos(Math.toRadians(kierunek)), prawoZ = -Math.sin(Math.toRadians(kierunek));
      vx -= prawoX * Math.signum(side) * u.przesuniecieBoczne; vz -= prawoZ * Math.signum(side) * u.przesuniecieBoczne;
    }

    // --- uderzenie w płot: przesunięcie dużo mniejsze, niż kazaliśmy
    Location l = mob.getLocation();
    if (!Double.isNaN(ostX) && oczekiwane > 0.2) {
      double faktycznie = Math.hypot(l.getX() - ostX, l.getZ() - ostZ);
      if (faktycznie < oczekiwane * 0.35) {
        if (++zderzenia >= 2) {
          v *= u.zderzenieSciana;
          poslizg = false; ladowanie = 0;
          mob.getWorld().playSound(l, Sound.BLOCK_WOOD_HIT, 0.8f, 0.8f);
          zderzenia = 0;
        }
      } else zderzenia = 0;
    }
    ostX = l.getX(); ostZ = l.getZ(); oczekiwane = Math.hypot(vx, vz);

    mc.setVelocity(vx, mc.getVelocity().getY(), vz);
    if (skok && !skokTrzymany && naZiemi && !poslizg) mc.jump();
    skokTrzymany = skok;

    // Wyjście: Shift na postoju przez kilka sekund.
    if (shift && Math.abs(v) < 0.02 && !poslizg) shiftPostoj++; else shiftPostoj = 0;
  }

  void daj(int ticki, double mnoznik, String rodzaj) {
    if (ticki <= 0) return;
    wyscig.plugin.debug(nick + " " + rodzaj + " ticki=" + ticki + " x" + mnoznik);
    if (turbo <= 0 || mnoznik >= turboMnoznik) turboMnoznik = mnoznik;
    turbo = Math.max(turbo, ticki);
    if (mob == null) return;
    Location l = mob.getLocation();
    switch (rodzaj) {
      case "turbo2" -> { mob.getWorld().playSound(l, Sound.ENTITY_BLAZE_SHOOT, 0.7f, 1.4f); mob.getWorld().spawnParticle(Particle.FLAME, l.add(0, 0.4, 0), 14, 0.3, 0.2, 0.3, 0.05); }
      case "turbo1" -> { mob.getWorld().playSound(l, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.9f); mob.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, l.add(0, 0.4, 0), 10, 0.3, 0.2, 0.3, 0.04); }
      case "pole" -> { mob.getWorld().playSound(l, Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 2f); mob.getWorld().spawnParticle(Particle.END_ROD, l.add(0, 0.3, 0), 12, 0.4, 0.1, 0.4, 0.08); }
      case "start" -> { mob.getWorld().playSound(l, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.2f); mob.getWorld().spawnParticle(Particle.CLOUD, l, 10, 0.4, 0.1, 0.4, 0.05); }
      default -> {}
    }
  }

  private void iskry() {
    if (mob == null || Bukkit.getCurrentTick() % 2 != 0) return;
    Ustawienia u = wyscig.u;
    double mn = "zelaznorog".equals(gatunek.id()) ? 0.85 : 1;
    Color kolor = ladowanie >= u.turbo2 * mn ? Color.ORANGE : ladowanie >= u.turbo1 * mn ? Color.fromRGB(80, 160, 255) : Color.fromRGB(230, 230, 230);
    Location l = mob.getLocation();
    double kat = Math.toRadians(kierunek);
    double tylX = Math.sin(kat) * 0.8, tylZ = -Math.cos(kat) * 0.8;
    mob.getWorld().spawnParticle(Particle.DUST, l.getX() + tylX, l.getY() + 0.1, l.getZ() + tylZ, 4, 0.25, 0.05, 0.25, 0, new Particle.DustOptions(kolor, 1.1f));
  }

  static float zawin(float k) {
    k %= 360f;
    if (k >= 180f) k -= 360f;
    if (k < -180f) k += 360f;
    return k;
  }
}
