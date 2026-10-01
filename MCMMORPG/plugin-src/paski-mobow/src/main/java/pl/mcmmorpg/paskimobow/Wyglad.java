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
 */
final class Wyglad {
  static final Key FONT = Key.key("paskimobow", "pasek");

  static final int BAR_W = 50;
  static final char CYFRA_PLAKIETKI = '', PLAKIETKA = '', RAMKA = '', RZADKOSC = '';
  static final char DUCH = '', HP = '', CYFRA_HP = '', UKOSNIK_HP = '', STATUS = '';
  static final int SPACJA = 0xF000;

  /** Indeksy jak STATUSY w generuj.py. */
  enum Status { OGIEN, TRUCIZNA, KRWAWIENIE, OBUMARCIE, SPOWOLNIENIE, OSLABIENIE, OSLEPIENIE, ZAMROZENIE,
    SILA, ODPORNOSC, REGENERACJA, SZYBKOSC, ABSORPCJA }

  static final TextColor CIEMNY = TextColor.color(28, 12, 24);
  private static final int ODSTEP = 2;   // plakietka -> pasek

  record Rzadkosc(String id, String nazwa, TextColor kolor, int ikona, boolean pogrubiona) {}

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
   * @param hp      biezace HP (zaokraglone w gore, zeby zywy mob nie mial 0)
   * @param duch    HP "ducha" obrazen (>= hp), jasny odcinek za czerwonym
   * @param pasek   false = sama nazwa i poziom (tryb tylko-po-trafieniu)
   */
  static Component tekst(Component nazwa, Rzadkosc r, int poziom, double hp, double duch, double max,
                         List<Status> statusy, boolean pasek, boolean liczby) {
    Component wynik = Component.empty();
    if (!statusy.isEmpty()) {
      Linia s = new Linia();
      for (Status st : statusy) s.znak((char) (STATUS + st.ordinal()), 10, NamedTextColor.WHITE);
      wynik = wynik.append(s.gotowa()).append(Component.newline());
    }
    wynik = wynik.append(liniaNazwy(nazwa, r)).append(Component.newline());
    return wynik.append(liniaPaska(r, poziom, hp, duch, max, pasek, liczby));
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
                                      boolean pasek, boolean liczby) {
    Linia l = new Linia();
    String cyfry = String.valueOf(Math.max(0, Math.min(999, poziom)));
    int n = cyfry.length();
    int szerPlakietki = 4 * n + 4;
    l.znak((char) (PLAKIETKA + n), szerPlakietki, r.kolor());
    l.doX(2);
    for (char c : cyfry.toCharArray()) l.znak((char) (CYFRA_PLAKIETKI + (c - '0')), 4, CIEMNY);
    if (!pasek) return l.gotowa();
    int bx = szerPlakietki + ODSTEP;
    l.doX(bx);
    l.znak(RAMKA, BAR_W + 3, NamedTextColor.WHITE);
    int k = max <= 0 ? 0 : (int) Math.ceil(Math.min(1, hp / max) * BAR_W);
    if (hp > 0) k = Math.max(1, k);
    int j = max <= 0 ? 0 : (int) Math.ceil(Math.min(1, duch / max) * BAR_W);
    if (j > k) { l.doX(bx); l.znak((char) (DUCH + j), j + 2, NamedTextColor.WHITE); }
    if (k > 0) { l.doX(bx); l.znak((char) (HP + k), k + 2, NamedTextColor.WHITE); }
    if (liczby) {
      String t = (int) Math.ceil(hp) + "/" + Math.round(max);
      if (4 * t.length() > BAR_W) t = String.valueOf((int) Math.ceil(hp));
      int tx = bx + 1 + (BAR_W - 4 * t.length()) / 2;
      l.doX(tx);
      for (char c : t.toCharArray()) {
        l.znak(c == '/' ? UKOSNIK_HP : (char) (CYFRA_HP + (c - '0')), 5, NamedTextColor.WHITE);
        l.doX(l.x - 1);   // cyfra 3 px + cien 1 px: odstep 4 zamiast 5
      }
    }
    l.doX(bx + BAR_W + 3);
    l.doX(l.x + szerPlakietki + ODSTEP);   // symetria: pasek na srodku nad mobem
    return l.gotowa();
  }
}
