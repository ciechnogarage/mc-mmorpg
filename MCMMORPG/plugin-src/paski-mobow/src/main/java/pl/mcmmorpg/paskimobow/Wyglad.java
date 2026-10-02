package pl.mcmmorpg.paskimobow;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

/**
 * Sklada tekst paska ze znakow czcionki `paskimobow:pasek`. Kody i szerokosci = generuj.py (zrodlo prawdy).
 * Szerokosc znaku bitmapowego = szerokosc obrazka + 1, wiec kursor liczymy sami i dosuwamy spacjami U+F000+dx+128.
 * Znaki NIE moga na siebie zachodzic (klient rysuje je w nieustalonej kolejnosci - cyfra na tle wychodzila
 * przyciemniona), dlatego pasek i plakietka to kafle stykajace sie krawedziami, a liczby HP stoja obok paska.
 */
final class Wyglad {
  static final Key FONT = Key.key("paskimobow", "pasek");

  static final int BAR_W = 50;
  static final char CYFRA_PLAKIETKI = '\uE000', PLAKIETKA_L = '\uE010', PLAKIETKA_P = '\uE011';
  static final char RAMKA_L = '\uE020', RAMKA_P = '\uE021', RZADKOSC = '\uE030';
  static final char DUCH = '\uE100', HP = '\uE200', TLO = '\uE500', CYFRA_HP = '\uE300', UKOSNIK_HP = '\uE30A';
  static final char STATUS = '\uE400';
  static final int SPACJA = 0xF000;

  /** Indeksy jak STATUSY w generuj.py. */
  enum Status { OGIEN, TRUCIZNA, KRWAWIENIE, OBUMARCIE, SPOWOLNIENIE, OSLABIENIE, OSLEPIENIE, ZAMROZENIE,
    SILA, ODPORNOSC, REGENERACJA, SZYBKOSC, ABSORPCJA }

  static final TextColor CIEMNY = TextColor.color(28, 12, 24);
  private static final int ODSTEP = 2;   // plakietka -> pasek

  record Rzadkosc(String id, String nazwa, TextColor kolor, int ikona, boolean pogrubiona) {}

  static int stopienHp(double hp, double max, boolean kolorHp) {
    double procent = max <= 0 ? 0 : hp / max;
    return !kolorHp || procent <= 0.15 ? 3 : procent > 0.60 ? 0 : procent > 0.35 ? 1 : 2;
  }

  /** Linia z kursorem: kazdy fragment ma swoj kolor (kolor mnozy piksele znaku). */
  private static final class Linia {
    final TextComponent.Builder b = Component.text();
    final StringBuilder bufor = new StringBuilder();
    TextColor kolorBufora = NamedTextColor.WHITE;
    int x;

    void znak(char c, int szerokosc, TextColor kolor) {
      if (!kolor.equals(kolorBufora)) zrzuc(kolor);
      bufor.append(c);
      x += szerokosc;
    }

    /** Kafel o szerokosci w: znak + spacja -1, wiec nastepny kafel styka sie bez szczeliny. */
    void kafel(char c, int w, TextColor kolor) {
      znak(c, w + 1, kolor);
      doX(x - 1);
    }

    void doX(int cel) {
      int dx = cel - x;
      while (dx != 0) {
        int s = Math.max(-128, Math.min(128, dx));
        bufor.append((char) (SPACJA + s + 128));
        dx -= s;
        x += s;
      }
    }

    void zrzuc(TextColor nowy) {
      if (!bufor.isEmpty()) b.append(Component.text(bufor.toString(), kolorBufora).font(FONT));
      bufor.setLength(0);
      kolorBufora = nowy;
    }

    Component gotowa() { zrzuc(NamedTextColor.WHITE); return b.build(); }
  }

  /**
   * @param hp      biezace HP (liczba w tekscie zaokraglana w gore)
   * @param duch    HP "ducha" obrazen (>= hp), jasny odcinek za kolorem HP
   * @param pasek   false = sama nazwa i poziom
   */
  static Component tekst(Component nazwa, Rzadkosc r, int poziom, double hp, double duch, double max,
                         List<Status> statusy, boolean pasek, boolean liczby, boolean kolorHp) {
    Component wynik = Component.empty();
    if (!statusy.isEmpty()) {
      Linia s = new Linia();
      for (Status st : statusy) s.znak((char) (STATUS + st.ordinal()), 10, NamedTextColor.WHITE);
      wynik = wynik.append(s.gotowa()).append(Component.newline());
    }
    wynik = wynik.append(liniaNazwy(nazwa, r)).append(Component.newline());
    return wynik.append(liniaPaska(r, poziom, hp, duch, max, pasek, liczby, kolorHp));
  }

  private static Component liniaNazwy(Component nazwa, Rzadkosc r) {
    Component imie = nazwa.color(r.kolor());
    if (r.pogrubiona()) imie = imie.decorate(TextDecoration.BOLD);
    if (r.ikona() < 0) return imie;
    // ikona + 2 px, a za nazwa tyle samo pustego miejsca -> nazwa wypada na srodku nad mobem
    Component ikona = Component.text(String.valueOf((char) (RZADKOSC + r.ikona())) + (char) (SPACJA + 2 + 128), r.kolor())
        .font(FONT);
    Component wyrownanie = Component.text(String.valueOf((char) (SPACJA + 12 + 128))).font(FONT);
    return Component.text().append(ikona, imie, wyrownanie).build();
  }

  private static Component liniaPaska(Rzadkosc r, int poziom, double hp, double duch, double max,
                                      boolean pasek, boolean liczby, boolean kolorHp) {
    String cyfry = String.valueOf(Math.max(0, Math.min(999, poziom)));
    int lewa = 2 + 4 * cyfry.length() + 1 + ODSTEP;          // plakietka + odstep
    String t = (int) Math.ceil(hp) + "/" + Math.round(max);
    int prawa = pasek && liczby ? 3 + 4 * t.length() : 0;    // odstep + liczby
    Linia l = new Linia();
    if (pasek) l.doX(Math.max(0, prawa - lewa));            // pasek na srodku nad mobem
    int x0 = l.x;
    l.kafel(PLAKIETKA_L, 2, r.kolor());
    for (char c : cyfry.toCharArray()) l.kafel((char) (CYFRA_PLAKIETKI + (c - '0')), 4, r.kolor());
    l.kafel(PLAKIETKA_P, 1, r.kolor());
    if (!pasek) return l.gotowa();
    l.doX(x0 + lewa);
    int k = max <= 0 ? 0 : (int) Math.ceil(Math.min(1, hp / max) * BAR_W);
    if (hp > 0) k = Math.max(1, k);
    int j = max <= 0 ? 0 : (int) Math.ceil(Math.min(1, duch / max) * BAR_W);
    j = Math.max(k, Math.min(BAR_W, j));
    l.kafel(RAMKA_L, 1, NamedTextColor.WHITE);
    int stopien = stopienHp(hp, max, kolorHp);
    if (k > 0) l.kafel((char) (HP + stopien * 64 + k), k, NamedTextColor.WHITE);
    if (j > k) l.kafel((char) (DUCH + (j - k)), j - k, NamedTextColor.WHITE);
    if (j < BAR_W) l.kafel((char) (TLO + (BAR_W - j)), BAR_W - j, NamedTextColor.WHITE);
    l.kafel(RAMKA_P, 1, NamedTextColor.WHITE);
    if (prawa > 0) {
      l.doX(l.x + 3);
      for (char c : t.toCharArray())   // cyfra 3 px + cien 1 px = kafel 4
        l.kafel(c == '/' ? UKOSNIK_HP : (char) (CYFRA_HP + (c - '0')), 4, NamedTextColor.WHITE);
    }
    l.doX(l.x + Math.max(0, lewa - prawa));
    return l.gotowa();
  }
}
