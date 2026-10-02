package pl.mcmmorpg.paskimobow;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Stan paska jednego moba. Zmieniany tylko w watku glownym; id czytane tez z netty. */
final class Pasek {
  final UUID mob;
  final int idMoba;
  final int idWyswietlacza;
  final Set<UUID> widzowie = new HashSet<>();
  String nazwa;
  Wyglad.Rzadkosc rzadkosc;
  int poziom;
  boolean zMythicMobs;
  double duch = -1;
  int ostatnieTrafienie = -1000;
  double wysokoscModelu;
  volatile double dodatkowaWysokosc;
  volatile boolean podazanie;
  double ostatnieX, ostatnieY, ostatnieZ;
  String ostatniSwiat;
  String ostatniKlucz = "";

  Pasek(UUID mob, int idMoba, int idWyswietlacza) {
    this.mob = mob;
    this.idMoba = idMoba;
    this.idWyswietlacza = idWyswietlacza;
  }
}
