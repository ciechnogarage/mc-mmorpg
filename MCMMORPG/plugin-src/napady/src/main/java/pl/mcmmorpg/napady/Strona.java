package pl.mcmmorpg.napady;

import org.bukkit.Color;

enum Strona {
  ATAK("napastnicy", Color.fromRGB(0xA83232)),
  OBRONA("obroncy", Color.fromRGB(0x3255A8));

  final String nazwa;
  final Color kolor;

  Strona(String nazwa, Color kolor) { this.nazwa = nazwa; this.kolor = kolor; }

  Strona wrog() { return this == ATAK ? OBRONA : ATAK; }
}
