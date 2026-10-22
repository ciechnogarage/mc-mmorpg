package pl.mcmmorpg.czujnosc;

import org.bukkit.configuration.ConfigurationSection;

/** Zmysly jednego rodzaju moba (config `profile.<nazwa>`, brakujace klucze z `profile.domyslny`). */
public record Profil(
    String nazwa,
    double wzrokZasieg, double wzrokKat, double wzrokOstry, double peryferia,
    double czasBlisko, double czasDaleko,
    double mnKuca, double mnStoi, double mnBieg, double swiatloMin,
    double sluchZasieg, double sluchSzybkosc, double sluchLimit,
    double dotyk, double zanik, double zanikZwloka, double pamiecS, double wolanie, double predkoscSzukania) {

  static Profil z(String nazwa, ConfigurationSection s, Profil d) {
    if (s == null) s = new org.bukkit.configuration.MemoryConfiguration();
    return new Profil(nazwa,
        s.getDouble("wzrok.zasieg", d == null ? 20 : d.wzrokZasieg),
        s.getDouble("wzrok.kat", d == null ? 120 : d.wzrokKat),
        s.getDouble("wzrok.kat_ostry", d == null ? 60 : d.wzrokOstry),
        s.getDouble("wzrok.peryferia", d == null ? 0.5 : d.peryferia),
        s.getDouble("wzrok.czas_blisko_s", d == null ? 0.6 : d.czasBlisko),
        s.getDouble("wzrok.czas_daleko_s", d == null ? 6 : d.czasDaleko),
        s.getDouble("postawa.kuca", d == null ? 0.35 : d.mnKuca),
        s.getDouble("postawa.stoi", d == null ? 0.6 : d.mnStoi),
        s.getDouble("postawa.biegnie", d == null ? 1.6 : d.mnBieg),
        s.getDouble("swiatlo_min", d == null ? 0.3 : d.swiatloMin),
        s.getDouble("sluch.zasieg", d == null ? 12 : d.sluchZasieg),
        s.getDouble("sluch.szybkosc", d == null ? 30 : d.sluchSzybkosc),
        s.getDouble("sluch.limit", d == null ? 70 : d.sluchLimit),
        s.getDouble("dotyk", d == null ? 1.3 : d.dotyk),
        s.getDouble("zanik_na_s", d == null ? 12 : d.zanik),
        s.getDouble("zanik_zwloka_s", d == null ? 2 : d.zanikZwloka),
        s.getDouble("pamiec_s", d == null ? 8 : d.pamiecS),
        s.getDouble("wolanie_sojusznikow", d == null ? 10 : d.wolanie),
        s.getDouble("predkosc_szukania", d == null ? 1.0 : d.predkoscSzukania));
  }

  public double zasiegMax() { return Math.max(wzrokZasieg, sluchZasieg * 1.5); }
}
