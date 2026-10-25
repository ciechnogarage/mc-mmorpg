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

/**
 * Najlepszy wynik rundy i liczba przetrwanych rund na gracza (rekordy.yml) + tytuły za progi (kosmetyka, bez łupu).
 * Do tytułów liczą się tylko rundy dokończone przy co najmniej {@code tytuly.min-graczy} graczach
 * (pola tytul-przetrwane / tytul-zlapani); sam tytuł wynika z progów w configu, więc zmiana progów działa wstecz.
 */
final class Rekordy {
  record Pozycja(String nick, int najlepszy, int rundy, int przetrwane, String tytul) {}
  record Wynik(boolean rekord, List<String> noweTytuly) {}

  private final File plik;
  private final Logger log;
  private final YamlConfiguration dane;
  private Ustawienia u;

  Rekordy(File plik, Logger log, Ustawienia u) {
    this.plik = plik;
    this.log = log;
    this.u = u;
    this.dane = YamlConfiguration.loadConfiguration(plik);
  }

  void ustawienia(Ustawienia u) { this.u = u; }

  /** @param liczona runda liczy się do tytułów (dokończona, dość graczy) */
  Wynik zapisz(UUID id, String nick, int punkty, boolean przetrwal, int zlapani, boolean liczona) {
    String s = "gracze." + id;
    int poprzedni = dane.getInt(s + ".najlepszy", -1);
    dane.set(s + ".nick", nick);
    dane.set(s + ".rundy", dane.getInt(s + ".rundy") + 1);
    if (przetrwal) dane.set(s + ".przetrwane", dane.getInt(s + ".przetrwane") + 1);
    boolean rekord = punkty > poprzedni;
    if (rekord) { dane.set(s + ".najlepszy", punkty); dane.set(s + ".data", LocalDate.now().toString()); }
    List<String> nowe = new ArrayList<>();
    if (liczona) {
      int tp = dane.getInt(s + ".tytul-przetrwane"), tz = dane.getInt(s + ".tytul-zlapani");
      int np = tp + (przetrwal ? 1 : 0), nz = tz + zlapani;
      dane.set(s + ".tytul-przetrwane", np);
      dane.set(s + ".tytul-zlapani", nz);
      for (Ustawienia.Prog p : u.tytulyLicho) if (tp < p.ile() && np >= p.ile()) nowe.add(p.nazwa());
      for (Ustawienia.Prog p : u.tytulyZamawiacz) if (tz < p.ile() && nz >= p.ile()) nowe.add(p.nazwa());
    }
    return new Wynik(rekord && poprzedni >= 0, nowe);
  }

  void zapiszPlik() {
    try { dane.save(plik); } catch (IOException e) { log.warning("Nie zapisano rekordów: " + e.getMessage()); }
  }

  List<Pozycja> top(int ile, Comparator<Pozycja> wg) {
    ConfigurationSection s = dane.getConfigurationSection("gracze");
    List<Pozycja> lista = new ArrayList<>();
    if (s == null) return lista;
    for (String id : s.getKeys(false)) lista.add(pozycja("gracze." + id));
    lista.sort(wg);
    return lista.subList(0, Math.min(ile, lista.size()));
  }

  Pozycja moj(UUID id) {
    String s = "gracze." + id;
    return dane.contains(s) ? pozycja(s) : null;
  }

  private Pozycja pozycja(String s) {
    return new Pozycja(dane.getString(s + ".nick", "?"), dane.getInt(s + ".najlepszy"), dane.getInt(s + ".rundy"), dane.getInt(s + ".przetrwane"), tytul(s));
  }

  /** Najwyższy zdobyty tytuł (wyższy stopień wygrywa, przy remisie ścieżka Licha); null = brak. */
  String tytul(UUID id) { return tytul("gracze." + id); }

  private String tytul(String s) {
    int sl = stopien(u.tytulyLicho, dane.getInt(s + ".tytul-przetrwane"));
    int sz = stopien(u.tytulyZamawiacz, dane.getInt(s + ".tytul-zlapani"));
    if (sl == 0 && sz == 0) return null;
    return sl >= sz ? u.tytulyLicho.get(sl - 1).nazwa() : u.tytulyZamawiacz.get(sz - 1).nazwa();
  }

  /** „Domowik (3/5)” — następny próg ścieżki; pusty tekst, gdy wszystkie zdobyte. */
  String postep(UUID id, boolean licho) {
    List<Ustawienia.Prog> progi = licho ? u.tytulyLicho : u.tytulyZamawiacz;
    int ile = dane.getInt("gracze." + id + (licho ? ".tytul-przetrwane" : ".tytul-zlapani"));
    for (Ustawienia.Prog p : progi) if (ile < p.ile()) return p.nazwa() + " (" + ile + "/" + p.ile() + ")";
    return "";
  }

  private static int stopien(List<Ustawienia.Prog> progi, int ile) {
    int st = 0;
    for (Ustawienia.Prog p : progi) if (ile >= p.ile()) st++;
    return st;
  }
}
