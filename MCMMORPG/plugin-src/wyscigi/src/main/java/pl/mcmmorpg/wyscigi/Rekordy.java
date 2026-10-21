package pl.mcmmorpg.wyscigi;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/** Najlepszy przejazd i najlepsze okrążenie gracza na każdym torze (rekordy.yml). */
final class Rekordy {
  record Wynik(boolean rekordOsobisty, boolean rekordToru, long poprzedniOsobisty) {}
  record Pozycja(String nick, String gatunek, long ms) {}

  private final File plik;
  private final Logger log;
  private final YamlConfiguration dane;

  Rekordy(File plik, Logger log) {
    this.plik = plik;
    this.log = log;
    this.dane = YamlConfiguration.loadConfiguration(plik);
  }

  Wynik zapisz(String tor, UUID id, String nick, String gatunek, long przejazdMs, long okrazenieMs) {
    String sciezka = "tory." + tor + ".przejazd." + id;
    long poprzedni = dane.getLong(sciezka + ".ms", -1);
    long rekordToru = najlepszy(tor, "przejazd");
    boolean osobisty = poprzedni < 0 || przejazdMs < poprzedni;
    boolean toru = rekordToru < 0 || przejazdMs < rekordToru;
    if (osobisty) wpisz(sciezka, nick, gatunek, przejazdMs);
    String sciezkaOkr = "tory." + tor + ".okrazenie." + id;
    long poprzednieOkr = dane.getLong(sciezkaOkr + ".ms", -1);
    if (okrazenieMs != Long.MAX_VALUE && (poprzednieOkr < 0 || okrazenieMs < poprzednieOkr)) wpisz(sciezkaOkr, nick, gatunek, okrazenieMs);
    zapiszPlik();
    return new Wynik(osobisty, toru, poprzedni);
  }

  private void wpisz(String sciezka, String nick, String gatunek, long ms) {
    dane.set(sciezka + ".nick", nick);
    dane.set(sciezka + ".gatunek", gatunek);
    dane.set(sciezka + ".ms", ms);
    dane.set(sciezka + ".data", LocalDate.now().toString());
  }

  private long najlepszy(String tor, String rodzaj) {
    List<Pozycja> top = top(tor, rodzaj, 1);
    return top.isEmpty() ? -1 : top.get(0).ms();
  }

  List<Pozycja> top(String tor, String rodzaj, int ile) {
    ConfigurationSection s = dane.getConfigurationSection("tory." + tor + "." + rodzaj);
    List<Pozycja> lista = new ArrayList<>();
    if (s == null) return lista;
    for (String id : s.getKeys(false))
      lista.add(new Pozycja(s.getString(id + ".nick", "?"), s.getString(id + ".gatunek", ""), s.getLong(id + ".ms")));
    lista.sort(Comparator.comparingLong(Pozycja::ms));
    return lista.subList(0, Math.min(ile, lista.size()));
  }

  long osobisty(String tor, String rodzaj, UUID id) { return dane.getLong("tory." + tor + "." + rodzaj + "." + id + ".ms", -1); }

  private void zapiszPlik() {
    try { dane.save(plik); } catch (IOException e) { log.warning("Nie zapisano rekordów: " + e.getMessage()); }
  }
}
