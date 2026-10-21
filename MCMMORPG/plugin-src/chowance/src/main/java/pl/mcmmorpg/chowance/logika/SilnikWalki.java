package pl.mcmmorpg.chowance.logika;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import pl.mcmmorpg.chowance.model.EfektRuchu;
import pl.mcmmorpg.chowance.model.Kategoria;
import pl.mcmmorpg.chowance.model.RodzajCechy;
import pl.mcmmorpg.chowance.model.Ruch;
import pl.mcmmorpg.chowance.model.Stat;
import pl.mcmmorpg.chowance.model.StatEtapu;
import pl.mcmmorpg.chowance.model.Status;
import pl.mcmmorpg.chowance.model.Typ;
import pl.mcmmorpg.chowance.tresc.Rejestr;

/**
 * Silnik walki turowej (SPEC §6.3) — deterministyczny przy danym RNG, bez Bukkita.
 * Warstwa UI (dialog Papera) wybiera akcje, woła {@link #tura}, a zdarzenia z wyniku odgrywa
 * (tekst + skill MythicMobs ruchu).
 */
public final class SilnikWalki {

  public static final class Strona {
    public final String nazwa;
    public final List<Wojownik> druzyna;
    public int aktywny;
    public final boolean ai;
    /** dziki stwór: wybiera ruchy losowo (jak dzikie w Pokémonach), nie „najlepszy” */
    public boolean dziki;

    public Strona(String nazwa, List<Wojownik> druzyna, boolean ai) {
      this.nazwa = nazwa;
      this.druzyna = druzyna;
      this.ai = ai;
      this.aktywny = 0;
      while (aktywny < druzyna.size() && druzyna.get(aktywny).zemdlony()) aktywny++;
      if (aktywny >= druzyna.size()) aktywny = 0;
    }

    public Wojownik akt() { return druzyna.get(aktywny); }

    public boolean przegrala() {
      for (Wojownik w : druzyna) if (!w.zemdlony()) return false;
      return true;
    }

    public boolean mozeZmienicNa(int i) {
      return i >= 0 && i < druzyna.size() && i != aktywny && !druzyna.get(i).zemdlony();
    }

    public int pierwszyZdolny() {
      for (int i = 0; i < druzyna.size(); i++) if (!druzyna.get(i).zemdlony()) return i;
      return -1;
    }
  }

  /** Akcja strony w turze. */
  public sealed interface Akcja permits Akcja.UzyjRuchu, Akcja.Zmiana, Akcja.Pomin {
    record UzyjRuchu(int indeks) implements Akcja {}
    record Zmiana(int naIndeks) implements Akcja {}
    /** Strona zużyła turę na coś, co zrobiła warstwa UI (przedmiot, pieczęć, nieudana ucieczka). */
    record Pomin(String powod) implements Akcja {}
  }

  /** Zdarzenie do odegrania. {@code strona} 0/1 = kto działał, -1 = narrator. */
  public record Zdarzenie(int strona, Rodzaj rodzaj, String tekst, String ruch, int wartosc, double skutecznosc) {
    public enum Rodzaj { TEKST, RUCH, OBRAZENIA, PUDLO, STATUS, ZEMDLAL, ZMIANA, LECZENIE, KONIEC }
  }

  public record WynikObrazen(int obrazenia, double skutecznosc, boolean krytyk) {}

  private final Rejestr r;
  private final RandomGenerator rng;
  public final Strona[] strony;
  private int nrTury = 0;
  private int zwyciezca = -1;

  public SilnikWalki(Rejestr r, Strona a, Strona b, RandomGenerator rng) {
    this.r = r;
    this.rng = rng;
    this.strony = new Strona[] {a, b};
    a.akt().walczyl = true;
    b.akt().walczyl = true;
  }

  public int nrTury() { return nrTury; }
  public boolean koniec() { return zwyciezca >= 0; }
  public int zwyciezca() { return zwyciezca; }

  /** Kapitulacja/ucieczka: strona przegrywa. */
  public void poddaj(int strona) { zwyciezca = 1 - strona; }

  /** Czy strona musi wskazać następcę (aktywny zemdlał, a są jeszcze zdolni). */
  public boolean wymagaZmiany(int s) {
    return !koniec() && strony[s].akt().zemdlony() && !strony[s].przegrala();
  }

  /** Wymuszona zmiana po omdleniu (nie zużywa tury). */
  public List<Zdarzenie> wymuszonaZmiana(int s, int na) {
    List<Zdarzenie> z = new ArrayList<>();
    if (!wymagaZmiany(s) || !strony[s].mozeZmienicNa(na)) return z;
    zmien(s, na, z);
    return z;
  }

  // ------------------------------------------------------------------ tura

  public List<Zdarzenie> tura(Akcja a0, Akcja a1) {
    List<Zdarzenie> z = new ArrayList<>();
    if (koniec()) return z;
    nrTury++;
    Akcja[] ak = {popraw(0, a0), popraw(1, a1)};
    // 1) zmiany i pominięcia przed ruchami
    for (int s = 0; s < 2; s++) {
      if (ak[s] instanceof Akcja.Zmiana zm && strony[s].mozeZmienicNa(zm.naIndeks())) zmien(s, zm.naIndeks(), z);
    }
    // 2) kolejność ruchów
    int pierwszy = kolejnosc(ak);
    for (int k = 0; k < 2; k++) {
      int s = k == 0 ? pierwszy : 1 - pierwszy;
      if (!(ak[s] instanceof Akcja.UzyjRuchu ur)) continue;
      Wojownik atak = strony[s].akt();
      if (atak.zemdlony() || koniec()) continue;
      wykonajRuch(s, ur.indeks(), z);
      if (sprawdzKoniec(z)) return z;
    }
    // 3) koniec tury: statusy, regeneracja
    for (int s = 0; s < 2; s++) koniecTury(s, z);
    sprawdzKoniec(z);
    return z;
  }

  /**
   * Odnowienie w turach: ruch z odnowieniem real-time < 5 s — co turę; dłuższe — przez round(s/6) tur
   * (GROM 18 s = 3 tury). Bez tego w turowej zawsze opłacał się najmocniejszy ruch (recenzja R-07).
   */
  public static int turyOdnowienia(Ruch r) {
    return r == null || r.odnowienie() < 5 ? 0 : (int) Math.max(1, Math.round(r.odnowienie() / 6.0));
  }

  public int odnawiaSie(int s, int idx) {
    Wojownik w = strony[s].akt();
    if (idx < 0 || idx >= w.o.ruchy.size()) return 0;
    return w.odnowienieTur.getOrDefault(w.o.ruchy.get(idx), 0);
  }

  public boolean gotowy(int s, int idx) {
    Wojownik w = strony[s].akt();
    return idx >= 0 && idx < w.o.ruchy.size() && r.ruch(w.o.ruchy.get(idx)) != null && odnawiaSie(s, idx) <= 0;
  }

  /** Ruch na odnowieniu (przeterminowany wybór, stara SI) zamieniany na gotowy; bez gotowych — najbliższy końca. */
  private Akcja popraw(int s, Akcja a) {
    if (!(a instanceof Akcja.UzyjRuchu ur) || gotowy(s, ur.indeks())) return a;
    Wojownik w = strony[s].akt();
    int best = -1, min = Integer.MAX_VALUE;
    for (int i = 0; i < w.o.ruchy.size(); i++) {
      if (gotowy(s, i)) return new Akcja.UzyjRuchu(wyborSI(s));
      int t = odnawiaSie(s, i);
      if (t < min) { min = t; best = i; }
    }
    return best < 0 ? a : new Akcja.UzyjRuchu(best);
  }

  private int kolejnosc(Akcja[] ak) {
    int p0 = priorytet(0, ak[0]);
    int p1 = priorytet(1, ak[1]);
    if (p0 != p1) return p0 > p1 ? 0 : 1;
    double s0 = szybkosc(strony[0].akt());
    double s1 = szybkosc(strony[1].akt());
    if (s0 != s1) return s0 > s1 ? 0 : 1;
    return rng.nextBoolean() ? 0 : 1;
  }

  private int priorytet(int s, Akcja a) {
    if (a instanceof Akcja.UzyjRuchu ur) {
      Ruch ruch = ruchStrony(s, ur.indeks());
      return ruch == null ? 0 : ruch.priorytet();
    }
    return 10;
  }

  private Ruch ruchStrony(int s, int idx) {
    Wojownik w = strony[s].akt();
    if (idx < 0 || idx >= w.o.ruchy.size()) return null;
    return r.ruch(w.o.ruchy.get(idx));
  }

  public double szybkosc(Wojownik w) {
    double v = w.staty[Stat.SZYBKOSC.ordinal()] * StatEtapu.mnoznik(w.etapy[StatEtapu.SZYBKOSC.ordinal()]) * mnoznikStatuCechy(w, Stat.SZYBKOSC);
    if (w.status == Status.SPETANIE) v *= 0.5;
    return v;
  }

  /** Tylko warunkowy (`gdy`) MNOZNIK_STATU — bezwarunkowy jest już w statach (Statystyki). */
  private static double mnoznikStatuCechy(Wojownik w, Stat s) {
    if (w.ma(RodzajCechy.MNOZNIK_STATU) && w.cecha.stat() == s && !w.cecha.gdy().isEmpty() && w.cecha.aktywnaW(w.srodowisko)) {
      return w.cecha.d("mnoznik", 1.0);
    }
    return 1.0;
  }

  private void zmien(int s, int na, List<Zdarzenie> z) {
    Strona st = strony[s];
    Wojownik stary = st.akt();
    stary.resetEtapow();
    st.aktywny = na;
    st.akt().walczyl = true;
    z.add(new Zdarzenie(s, Zdarzenie.Rodzaj.ZMIANA, st.nazwa + ": " + stary.nazwa() + " wraca, do walki wchodzi "
        + st.akt().nazwa() + "!", null, na, 1));
  }

  private void wykonajRuch(int s, int idx, List<Zdarzenie> z) {
    Wojownik atak = strony[s].akt();
    Wojownik obr = strony[1 - s].akt();
    Ruch ruch = ruchStrony(s, idx);
    if (ruch == null) {
      z.add(new Zdarzenie(s, Zdarzenie.Rodzaj.TEKST, atak.nazwa() + " nie wie, co zrobić.", null, 0, 1));
      return;
    }
    // statusy blokujące
    if (atak.status == Status.SEN) {
      z.add(new Zdarzenie(s, Zdarzenie.Rodzaj.STATUS, atak.nazwa() + " śpi.", null, 0, 1));
      return;
    }
    if (atak.status == Status.OSZOLOMIENIE && rng.nextDouble() < 0.33) {
      z.add(new Zdarzenie(s, Zdarzenie.Rodzaj.STATUS, atak.nazwa() + " jest oszołomiony i nie atakuje.", null, 0, 1));
      return;
    }
    if (atak.status == Status.SPETANIE && rng.nextDouble() < 0.25) {
      z.add(new Zdarzenie(s, Zdarzenie.Rodzaj.STATUS, atak.nazwa() + " jest spętany i nie może się ruszyć.", null, 0, 1));
      return;
    }
    z.add(new Zdarzenie(s, Zdarzenie.Rodzaj.RUCH, atak.nazwa() + " używa: " + ruch.nazwa() + "!", ruch.id(), 0, 1));
    int tury = turyOdnowienia(ruch);
    if (tury > 0) atak.odnowienieTur.put(ruch.id(), tury + 1); // +1: koniec tej tury od razu zdejmuje jedną
    if (!trafia(atak, obr, ruch)) {
      z.add(new Zdarzenie(s, Zdarzenie.Rodzaj.PUDLO, "Chybienie!", ruch.id(), 0, 1));
      return;
    }
    int zadane = 0;
    if (ruch.kategoria() != Kategoria.STATUS) {
      WynikObrazen w = obrazenia(atak, obr, ruch, rng);
      if (w.skutecznosc() <= 0) {
        z.add(new Zdarzenie(s, Zdarzenie.Rodzaj.OBRAZENIA, "To nie działa na " + obr.nazwa() + "…", ruch.id(), 0, 0));
        return;
      }
      zadane = (int) Math.min(obr.hp, w.obrazenia());
      obr.hp -= zadane;
      String dop = w.skutecznosc() >= 2 ? " Bardzo skuteczne!" : w.skutecznosc() < 1 ? " Mało skuteczne…" : "";
      if (w.krytyk()) dop += " Trafienie krytyczne!";
      z.add(new Zdarzenie(1 - s, Zdarzenie.Rodzaj.OBRAZENIA, obr.nazwa() + " traci " + zadane + " HP." + dop, ruch.id(), zadane, w.skutecznosc()));
      // kontra
      if (ruch.kontakt() && obr.ma(RodzajCechy.KONTRA_STATUS) && atak.status == null
          && rng.nextDouble() * 100 < obr.cecha.d("szansa", 0)) {
        nalozStatus(atak, obr.cecha.status(), 3, s, z);
      }
    }
    for (EfektRuchu e : ruch.efekty()) {
      if (rng.nextDouble() * 100 >= e.szansa()) continue;
      Wojownik cel = e.naSiebie() ? atak : obr;
      int celStrona = e.naSiebie() ? s : 1 - s;
      switch (e.rodzaj()) {
        case STATUS -> { if (!cel.zemdlony()) nalozStatus(cel, e.status(), e.czas(), celStrona, z); }
        case STAT -> {
          if (cel.zemdlony()) break;
          int i = e.stat().ordinal();
          int przed = cel.etapy[i];
          cel.etapy[i] = Math.max(-6, Math.min(6, przed + e.zmiana()));
          if (cel.etapy[i] != przed) z.add(new Zdarzenie(celStrona, Zdarzenie.Rodzaj.TEKST, cel.nazwa() + ": "
              + e.stat().name().toLowerCase().replace('_', ' ') + (e.zmiana() > 0 ? " rośnie" : " spada") + ".", null, e.zmiana(), 1));
        }
        case LECZENIE -> lecz(cel, cel.hpMax * e.procent() / 100.0, celStrona, z);
        case WYSYSANIE -> { if (zadane > 0) lecz(atak, zadane * e.procent() / 100.0, s, z); }
        case ODRZUT -> {
          if (zadane > 0) {
            int odrzut = Math.max(1, zadane * e.procent() / 100);
            atak.hp = Math.max(0, atak.hp - odrzut);
            z.add(new Zdarzenie(s, Zdarzenie.Rodzaj.OBRAZENIA, atak.nazwa() + " obrywa odrzutem (" + odrzut + " HP).", null, odrzut, 1));
          }
        }
      }
    }
    omdlenia(z);
  }

  private void lecz(Wojownik w, double ile, int s, List<Zdarzenie> z) {
    double przed = w.hp;
    w.hp = Math.min(w.hpMax, w.hp + ile);
    int d = (int) Math.round(w.hp - przed);
    if (d > 0) z.add(new Zdarzenie(s, Zdarzenie.Rodzaj.LECZENIE, w.nazwa() + " odzyskuje " + d + " HP.", null, d, 1));
  }

  private void nalozStatus(Wojownik cel, Status st, int czas, int s, List<Zdarzenie> z) {
    if (st == null || cel.status != null) return;
    if (cel.ma(RodzajCechy.ODPORNOSC_NA_STATUS) && cel.cecha.status() == st) {
      z.add(new Zdarzenie(s, Zdarzenie.Rodzaj.TEKST, cel.nazwa() + " jest odporny (" + cel.cecha.nazwa() + ").", null, 0, 1));
      return;
    }
    cel.status = st;
    cel.statusCzas = Math.max(1, czas);
    z.add(new Zdarzenie(s, Zdarzenie.Rodzaj.STATUS, cel.nazwa() + ": " + st.nazwa() + "!", null, 0, 1));
  }

  private boolean trafia(Wojownik atak, Wojownik obr, Ruch ruch) {
    if (ruch.zawszeTrafia()) return true;
    double p = ruch.celnosc() / 100.0
        * StatEtapu.mnoznikCelnosci(atak.etapy[StatEtapu.CELNOSC.ordinal()])
        / StatEtapu.mnoznikCelnosci(obr.etapy[StatEtapu.UNIK.ordinal()]);
    if (obr.ma(RodzajCechy.UNIK)) p *= 1 - obr.cecha.d("procent", 0) / 100.0;
    return rng.nextDouble() < p;
  }

  /** Obrażenia ruchu — wspólne dla tur i real-time. */
  public static WynikObrazen obrazenia(Rejestr r, Wojownik atak, Wojownik obr, Ruch ruch, RandomGenerator rng) {
    boolean fiz = ruch.kategoria() == Kategoria.FIZYCZNY;
    Stat sa = fiz ? Stat.ATAK : Stat.ATAK_SPEC;
    Stat sd = fiz ? Stat.OBRONA : Stat.OBRONA_SPEC;
    double a = atak.staty[sa.ordinal()] * StatEtapu.mnoznik(atak.etapy[StatEtapu.valueOf(sa.name()).ordinal()]) * mnoznikStatuCechy(atak, sa);
    double d = obr.staty[sd.ordinal()] * StatEtapu.mnoznik(obr.etapy[StatEtapu.valueOf(sd.name()).ordinal()]) * mnoznikStatuCechy(obr, sd);
    if (fiz && atak.status == Status.OPARZENIE) a *= 0.5;
    int baza = Obrazenia.bazowe(atak.o.poziom, ruch.moc(), a, d);
    double typ = r.typy().mnoznik(ruch.typ(), obr.g.typy());
    boolean stab = atak.g.typy().contains(ruch.typ());
    boolean kryt = rng.nextInt(16) == 0;
    double los = 0.85 + rng.nextDouble() * 0.15;
    double cechy = mnoznikCechAtaku(atak, ruch.typ()) * mnoznikCechObrony(obr, ruch.typ());
    if (typ * cechy <= 0) return new WynikObrazen(0, 0, false);
    int dmg = Obrazenia.koncowe(baza, stab ? r.typy().stab() : 1.0, typ, kryt, los, cechy);
    return new WynikObrazen(dmg, typ, kryt);
  }

  private WynikObrazen obrazenia(Wojownik atak, Wojownik obr, Ruch ruch, RandomGenerator rng) {
    return obrazenia(r, atak, obr, ruch, rng);
  }

  static double mnoznikCechAtaku(Wojownik w, Typ typRuchu) {
    if (w.cecha == null) return 1.0;
    if (w.ma(RodzajCechy.WZMOCNIENIE_TYPU) && w.cecha.typ() == typRuchu && w.cecha.aktywnaW(w.srodowisko)) return w.cecha.d("mnoznik", 1.0);
    if (w.ma(RodzajCechy.WZMOCNIENIE_TYPU_PRZY_NISKIM_HP) && w.cecha.typ() == typRuchu
        && w.hpProcent() <= w.cecha.d("prog_hp", 33)) return w.cecha.d("mnoznik", 1.0);
    return 1.0;
  }

  static double mnoznikCechObrony(Wojownik w, Typ typRuchu) {
    if (w.ma(RodzajCechy.ODPORNOSC_NA_TYP) && w.cecha.typ() == typRuchu) return w.cecha.d("mnoznik", 1.0);
    return 1.0;
  }

  private void omdlenia(List<Zdarzenie> z) {
    for (int s = 0; s < 2; s++) {
      Wojownik w = strony[s].akt();
      if (w.hp <= 0 && !zgloszoneOmdlenie(w)) {
        w.hp = 0;
        w.status = null;
        oznaczOmdlenie(w);
        z.add(new Zdarzenie(s, Zdarzenie.Rodzaj.ZEMDLAL, w.nazwa() + " mdleje!", null, 0, 1));
      }
    }
  }

  private final java.util.Set<Wojownik> zemdleni = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
  private boolean zgloszoneOmdlenie(Wojownik w) { return zemdleni.contains(w); }
  private void oznaczOmdlenie(Wojownik w) { zemdleni.add(w); }

  private void koniecTury(int s, List<Zdarzenie> z) {
    Wojownik w = strony[s].akt();
    w.odnowienieTur.replaceAll((k, v) -> v - 1);
    w.odnowienieTur.values().removeIf(v -> v <= 0);
    if (w.zemdlony()) return;
    if (w.status == Status.OPARZENIE || w.status == Status.ZATRUCIE) {
      int d = Math.max(1, w.hpMax / (w.status == Status.OPARZENIE ? 16 : 8));
      w.hp = Math.max(0, w.hp - d);
      z.add(new Zdarzenie(s, Zdarzenie.Rodzaj.OBRAZENIA, w.nazwa() + " cierpi (" + w.status.nazwa() + "): -" + d + " HP.", null, d, 1));
    }
    if (w.ma(RodzajCechy.REGENERACJA) && !w.zemdlony()) lecz(w, w.hpMax * w.cecha.d("procent", 0) / 100.0, s, z);
    if (w.status != null && !w.zemdlony()) {
      w.statusCzas--;
      if (w.statusCzas <= 0) {
        z.add(new Zdarzenie(s, Zdarzenie.Rodzaj.STATUS, w.nazwa() + ": koniec stanu " + w.status.nazwa() + ".", null, 0, 1));
        w.status = null;
      }
    }
    omdlenia(z);
  }

  private boolean sprawdzKoniec(List<Zdarzenie> z) {
    boolean p0 = strony[0].przegrala();
    boolean p1 = strony[1].przegrala();
    if (!p0 && !p1) return false;
    zwyciezca = p0 && p1 ? 1 : (p0 ? 1 : 0);
    z.add(new Zdarzenie(-1, Zdarzenie.Rodzaj.KONIEC, "Walkę wygrywa: " + strony[zwyciezca].nazwa + "!", null, zwyciezca, 1));
    return true;
  }

  /**
   * SI: dziki — losowy gotowy ruch (jak dzikie w Pokémonach; zawsze „najlepszy” ruch dzikiego kładł startera
   * w pierwszej walce, recenzja R-01); trener — najwyższa oczekiwana wartość (moc × celność × typ × STAB z typy.yml),
   * bez rzucania stanu na stwora, który już go ma, i bez obniżania statu, który jest już nisko (R-08, R-22).
   */
  public int wyborSI(int s) {
    Wojownik w = strony[s].akt();
    Wojownik cel = strony[1 - s].akt();
    List<Integer> gotowe = new ArrayList<>();
    for (int i = 0; i < w.o.ruchy.size(); i++) if (gotowy(s, i) && !bezSensu(r.ruch(w.o.ruchy.get(i)), w, cel)) gotowe.add(i);
    if (gotowe.isEmpty()) for (int i = 0; i < w.o.ruchy.size(); i++) if (gotowy(s, i)) gotowe.add(i);
    if (gotowe.isEmpty()) return 0;
    if (strony[s].dziki) return gotowe.get(rng.nextInt(gotowe.size()));
    int best = gotowe.get(0);
    double bestV = -1;
    for (int i : gotowe) {
      Ruch ruch = r.ruch(w.o.ruchy.get(i));
      double v;
      if (ruch.kategoria() == Kategoria.STATUS) v = 25;
      else {
        v = ruch.moc() * (ruch.zawszeTrafia() ? 1 : ruch.celnosc() / 100.0) * r.typy().mnoznik(ruch.typ(), cel.g.typy())
            * (w.g.typy().contains(ruch.typ()) ? r.typy().stab() : 1.0);
      }
      v *= 0.85 + rng.nextDouble() * 0.3;
      if (v > bestV) { bestV = v; best = i; }
    }
    return best;
  }

  /** Ruch statusu, który nic nie da: stan na stworze, który już ma stan; obniżenie statu, który jest na -2 lub niżej. */
  private static boolean bezSensu(Ruch ruch, Wojownik ja, Wojownik cel) {
    if (ruch == null || ruch.kategoria() != Kategoria.STATUS) return false;
    boolean cos = false;
    for (EfektRuchu e : ruch.efekty()) {
      Wojownik x = e.naSiebie() ? ja : cel;
      switch (e.rodzaj()) {
        case STATUS -> { if (x.status == null) cos = true; }
        case STAT -> { int st = x.etapy[e.stat().ordinal()]; if (e.zmiana() < 0 ? st > -2 : st < 2) cos = true; }
        case LECZENIE -> { if (x.hpProcent() < 70) cos = true; }
        default -> cos = true;
      }
    }
    return !cos;
  }
}
