package pl.mcmmorpg.wyscigi;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Odczyt config.yml w jednym miejscu; fizyka czyta pola co tick, więc bez map i stringów w pętli. */
final class Ustawienia {
  record Gatunek(String id, String nazwa, String model, String ikona, double predkosc, double przyspieszenie, double skret, String opis) {}

  final String swiat;
  final List<String> swiatyAwaryjne;
  final Map<String, Gatunek> gatunki = new LinkedHashMap<>();
  final double hamowanie, wybieg, wsteczny, pozaTorem, skretPrzyPredkosci, przesuniecieBoczne;
  final double poslizgMinPredkosc, poslizgMinKat, poslizgSkret, katUslizgu, turboMnoznik;
  final int turbo1, turbo2, turbo1Czas, turbo2Czas;
  final int poleCzas; final double poleMnoznik;
  final int startOkno, startCzas, startZaWczesnie, startKara; final double startMnoznik;
  final double zderzenieSciana, skalaModelu, skretKlawisz, przechylPoslizgu;
  final int poczekalnia, poczekalniaDokladka, poczekalniaMax, maxGraczy, poZwyciezcy, limit, poMecie, wyjscieShift;
  final String torId;
  final Tor tor;

  Ustawienia(FileConfiguration c, java.util.logging.Logger log) {
    swiat = c.getString("swiat", "wyscigi");
    swiatyAwaryjne = c.getStringList("swiaty-awaryjne");
    ConfigurationSection g = c.getConfigurationSection("gatunki");
    if (g == null) {
      // brak sekcji nie wywraca startu: pusta lista gatunków, start() odmówi z komunikatem "Nieznany wierzchowiec"
      log.warning("config.yml: brak sekcji 'gatunki' - lista gatunków pusta, wyścigi nie wystartują.");
    }
    for (String id : g == null ? List.<String>of() : new java.util.ArrayList<>(g.getKeys(false))) {
      ConfigurationSection s = g.getConfigurationSection(id);
      if (s == null) continue;
      gatunki.put(id, new Gatunek(id, s.getString("nazwa", id), s.getString("model"), s.getString("ikona", s.getString("model")),
          s.getDouble("predkosc"), s.getDouble("przyspieszenie"), s.getDouble("skret"), s.getString("opis", "")));
    }
    hamowanie = c.getDouble("jazda.hamowanie", 0.05);
    wybieg = c.getDouble("jazda.wybieg", 0.012);
    wsteczny = c.getDouble("jazda.wsteczny", 0.14);
    pozaTorem = c.getDouble("jazda.poza-torem", 0.55);
    skretPrzyPredkosci = c.getDouble("jazda.skret-przy-predkosci", 0.4);
    przesuniecieBoczne = c.getDouble("jazda.przesuniecie-boczne", 0.07);
    poslizgMinPredkosc = c.getDouble("jazda.poslizg.min-predkosc", 0.55);
    poslizgMinKat = c.getDouble("jazda.poslizg.min-kat", 6);
    poslizgSkret = c.getDouble("jazda.poslizg.skret", 1.45);
    katUslizgu = c.getDouble("jazda.poslizg.kat-uslizgu", 16);
    turbo1 = c.getInt("jazda.poslizg.turbo-1", 30);
    turbo2 = c.getInt("jazda.poslizg.turbo-2", 65);
    turbo1Czas = c.getInt("jazda.poslizg.turbo-1-czas", 16);
    turbo2Czas = c.getInt("jazda.poslizg.turbo-2-czas", 30);
    turboMnoznik = c.getDouble("jazda.poslizg.turbo-mnoznik", 1.22);
    poleCzas = c.getInt("jazda.pole-przyspieszenia.czas", 26);
    poleMnoznik = c.getDouble("jazda.pole-przyspieszenia.mnoznik", 1.38);
    startOkno = c.getInt("jazda.start-turbo.okno", 10);
    startCzas = c.getInt("jazda.start-turbo.czas", 30);
    startMnoznik = c.getDouble("jazda.start-turbo.mnoznik", 1.3);
    startZaWczesnie = c.getInt("jazda.start-turbo.za-wczesnie", 32);
    startKara = c.getInt("jazda.start-turbo.kara", 24);
    zderzenieSciana = c.getDouble("jazda.zderzenie-sciana", 0.45);
    skalaModelu = c.getDouble("wyscig.skala-modelu", 1.4);
    skretKlawisz = c.getDouble("jazda.skret-klawisz", 0.6);
    przechylPoslizgu = c.getDouble("jazda.poslizg.przechyl", 18);
    poczekalnia = c.getInt("wyscig.poczekalnia-sekundy", 20);
    poczekalniaDokladka = c.getInt("wyscig.poczekalnia-dokladka", 5);
    poczekalniaMax = c.getInt("wyscig.poczekalnia-max", 40);
    maxGraczy = c.getInt("wyscig.max-graczy", 8);
    poZwyciezcy = c.getInt("wyscig.po-zwyciezcy-sekundy", 30);
    limit = c.getInt("wyscig.limit-sekundy", 420);
    poMecie = c.getInt("wyscig.po-mecie-sekundy", 6);
    wyjscieShift = c.getInt("wyscig.wyjscie-shift-ticki", 40);
    torId = c.getString("tor", "sad");
    ConfigurationSection sekcjaToru = c.getConfigurationSection("tory." + torId);
    if (sekcjaToru == null) throw new IllegalArgumentException("config.yml: brak sekcji tory." + torId + " (klucz 'tor' wskazuje nieistniejący tor)");
    tor = new Tor(torId, sekcjaToru);
  }
}
