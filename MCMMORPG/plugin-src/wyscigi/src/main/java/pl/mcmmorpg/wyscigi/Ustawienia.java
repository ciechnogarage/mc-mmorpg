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
  final double zderzenieSciana, skalaModelu;
  final int poczekalnia, poczekalniaDokladka, poczekalniaMax, maxGraczy, poZwyciezcy, limit, poMecie, wyjscieShift;
  final String torId;
  final Tor tor;

  Ustawienia(FileConfiguration c) {
    swiat = c.getString("swiat", "wyscigi");
    swiatyAwaryjne = c.getStringList("swiaty-awaryjne");
    ConfigurationSection g = c.getConfigurationSection("gatunki");
    for (String id : g.getKeys(false)) {
      ConfigurationSection s = g.getConfigurationSection(id);
      gatunki.put(id, new Gatunek(id, s.getString("nazwa", id), s.getString("model"), s.getString("ikona", s.getString("model")),
          s.getDouble("predkosc"), s.getDouble("przyspieszenie"), s.getDouble("skret"), s.getString("opis", "")));
    }
    hamowanie = c.getDouble("jazda.hamowanie");
    wybieg = c.getDouble("jazda.wybieg");
    wsteczny = c.getDouble("jazda.wsteczny");
    pozaTorem = c.getDouble("jazda.poza-torem");
    skretPrzyPredkosci = c.getDouble("jazda.skret-przy-predkosci");
    przesuniecieBoczne = c.getDouble("jazda.przesuniecie-boczne");
    poslizgMinPredkosc = c.getDouble("jazda.poslizg.min-predkosc");
    poslizgMinKat = c.getDouble("jazda.poslizg.min-kat");
    poslizgSkret = c.getDouble("jazda.poslizg.skret");
    katUslizgu = c.getDouble("jazda.poslizg.kat-uslizgu");
    turbo1 = c.getInt("jazda.poslizg.turbo-1");
    turbo2 = c.getInt("jazda.poslizg.turbo-2");
    turbo1Czas = c.getInt("jazda.poslizg.turbo-1-czas");
    turbo2Czas = c.getInt("jazda.poslizg.turbo-2-czas");
    turboMnoznik = c.getDouble("jazda.poslizg.turbo-mnoznik");
    poleCzas = c.getInt("jazda.pole-przyspieszenia.czas");
    poleMnoznik = c.getDouble("jazda.pole-przyspieszenia.mnoznik");
    startOkno = c.getInt("jazda.start-turbo.okno");
    startCzas = c.getInt("jazda.start-turbo.czas");
    startMnoznik = c.getDouble("jazda.start-turbo.mnoznik");
    startZaWczesnie = c.getInt("jazda.start-turbo.za-wczesnie");
    startKara = c.getInt("jazda.start-turbo.kara");
    zderzenieSciana = c.getDouble("jazda.zderzenie-sciana");
    skalaModelu = c.getDouble("wyscig.skala-modelu", 1.4);
    poczekalnia = c.getInt("wyscig.poczekalnia-sekundy");
    poczekalniaDokladka = c.getInt("wyscig.poczekalnia-dokladka");
    poczekalniaMax = c.getInt("wyscig.poczekalnia-max");
    maxGraczy = c.getInt("wyscig.max-graczy");
    poZwyciezcy = c.getInt("wyscig.po-zwyciezcy-sekundy");
    limit = c.getInt("wyscig.limit-sekundy");
    poMecie = c.getInt("wyscig.po-mecie-sekundy");
    wyjscieShift = c.getInt("wyscig.wyjscie-shift-ticki");
    torId = c.getString("tor", "sad");
    tor = new Tor(torId, c.getConfigurationSection("tory." + torId));
  }
}
