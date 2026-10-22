package pl.mcmmorpg.czujnosc;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Display;

/** Czujnosc jednego moba. Wartosc 0..100; poziom wynika z wartosci z histereza (Poziom.z). */
final class Stan {
  enum Poziom {
    SPOKOJNY, ZANIEPOKOJONY, SZUKA, ALARM;
    /** Progi wejscia 25/60/100; wyjscie o 10 nizej (bez migania ikony na granicy). */
    static Poziom z(double v, Poziom teraz) {
      if (v >= 100) return ALARM;
      if (teraz == ALARM) return SZUKA;          // z alarmu zawsze przez szukanie (zgubil cel)
      if (v >= 60 || (teraz == SZUKA && v >= 50)) return SZUKA;
      if (v >= 25 || (teraz.ordinal() >= ZANIEPOKOJONY.ordinal() && v >= 15)) return ZANIEPOKOJONY;
      return SPOKOJNY;
    }
  }

  final Profil profil;
  double wartosc;
  double zysk;            // suma przyrostow z biezacego cyklu (od wszystkich graczy)
  double zyskWzrok;       // czesc z wzroku (tylko wzrok/dotyk/cios moga dac ALARM)
  Poziom poziom = Poziom.SPOKOJNY;
  UUID zrodlo;            // gracz, ktory dal najwiekszy przyrost
  double zrodloZysk;
  Location ostatnieMiejsce;
  long ostatnioWidzianyTick;
  long ostatniBodziecTick;   // ostatni cykl z przyrostem: zanik dopiero po profil.zanikZwloka (mob zerknal w bok != zapomnial)
  Display ikona;
  boolean patrzy;         // co tik lookAt(ostatnieMiejsce) - ustawia Czujnosc.zachowanie
  String slad;            // tylko przy /czujnosc debug: ostatni pomiar pary (kat, dystans, przyrosty, linia wzroku)

  Stan(Profil profil) { this.profil = profil; }

  void dodaj(UUID gracz, double ile, boolean wzrok, Location gdzie) {
    if (ile <= 0) return;
    zysk += ile;
    if (wzrok) zyskWzrok += ile;
    if (ile >= zrodloZysk) { zrodloZysk = ile; zrodlo = gracz; ostatnieMiejsce = gdzie; }
  }
}
