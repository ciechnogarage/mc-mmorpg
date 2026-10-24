package pl.mcmmorpg.licho;

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

/** Najlepszy wynik rundy i liczba przetrwanych rund na gracza (rekordy.yml). Tylko ranking, bez nagród. */
final class Rekordy {
  record Pozycja(String nick, int najlepszy, int rundy, int przetrwane) {}

  private final File plik;
  private final Logger log;
  private final YamlConfiguration dane;

  Rekordy(File plik, Logger log) {
    this.plik = plik;
    this.log = log;
    this.dane = YamlConfiguration.loadConfiguration(plik);
  }

  /** @return true, gdy to nowy rekord osobisty. */
  boolean zapisz(UUID id, String nick, int punkty, boolean przetrwal) {
    String s = "gracze." + id;
    int poprzedni = dane.getInt(s + ".najlepszy", -1);
    dane.set(s + ".nick", nick);
    dane.set(s + ".rundy", dane.getInt(s + ".rundy") + 1);
    if (przetrwal) dane.set(s + ".przetrwane", dane.getInt(s + ".przetrwane") + 1);
    boolean rekord = punkty > poprzedni;
    if (rekord) { dane.set(s + ".najlepszy", punkty); dane.set(s + ".data", LocalDate.now().toString()); }
    return rekord && poprzedni >= 0;
  }

  void zapiszPlik() {
    try { dane.save(plik); } catch (IOException e) { log.warning("Nie zapisano rekordów: " + e.getMessage()); }
  }

  List<Pozycja> top(int ile, Comparator<Pozycja> wg) {
    ConfigurationSection s = dane.getConfigurationSection("gracze");
    List<Pozycja> lista = new ArrayList<>();
    if (s == null) return lista;
    for (String id : s.getKeys(false))
      lista.add(new Pozycja(s.getString(id + ".nick", "?"), s.getInt(id + ".najlepszy"), s.getInt(id + ".rundy"), s.getInt(id + ".przetrwane")));
    lista.sort(wg);
    return lista.subList(0, Math.min(ile, lista.size()));
  }

  Pozycja moj(UUID id) {
    String s = "gracze." + id;
    if (!dane.contains(s)) return null;
    return new Pozycja(dane.getString(s + ".nick", "?"), dane.getInt(s + ".najlepszy"), dane.getInt(s + ".rundy"), dane.getInt(s + ".przetrwane"));
  }
}
