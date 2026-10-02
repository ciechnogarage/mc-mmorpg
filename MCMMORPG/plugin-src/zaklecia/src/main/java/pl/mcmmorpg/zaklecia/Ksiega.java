package pl.mcmmorpg.zaklecia;

import io.lumine.mythic.bukkit.MythicBukkit;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

/** Znane zaklecia z config.yml i rozpoznawanie ich w przedmiotach (po typie MythicMobs). */
final class Ksiega {
  record Skille(String start, String stop, String tik, String traf, String uderzenie, String obraz, String dlon) {
    String reka(String baza, boolean prawa) { return baza.isBlank() ? "" : baza + (prawa ? "_P" : "_L"); }
    String obie(String baza) { return baza.isBlank() ? "" : baza + "_Obie"; }
  }
  record Zaklecie(String id, String przedmiot, double manaZaTik, String nazwa, String modelStrumienia, Skille skille,
      String typ, String cel, int czasLadowania, double mana, double efektOburacz, double manaOburacz,
      double zasieg, double promien, int limitAktywnych, int czasZycia, double sila, double redukcja, int narastanie) {
    boolean koncentracja() { return typ.equals("koncentracja") || typ.equals("tarcza"); }
    boolean odrzutowe() { return typ.equals("ladowany") || typ.equals("runa") || typ.equals("bron")
        || typ.equals("przywolanie") || typ.equals("na_cel"); }
  }
  record Stozek(double zasieg, double polKat) {}

  final Map<String, Zaklecie> poId = new LinkedHashMap<>();
  private final Map<String, Zaklecie> poPrzedmiocie = new LinkedHashMap<>();
  final double podwojneMnoznik;
  final String skBrakMany;
  final Stozek stozek, stozekPodwojny;

  Ksiega(ConfigurationSection cfg) {
    ConfigurationSection z = cfg.getConfigurationSection("zaklecia");
    if (z != null) {
      for (String id : z.getKeys(false)) {
        ConfigurationSection s = z.getConfigurationSection(id);
        String prefiks = "Zak_" + Character.toUpperCase(id.charAt(0)) + id.substring(1);
        Skille skille = new Skille(
            s.getString("skille.start", prefiks + "_Start"), s.getString("skille.stop", prefiks + "_Stop"),
            s.getString("skille.tik", prefiks + "_Tik"), s.getString("skille.traf", prefiks + "_Traf"),
            s.getString("skille.uderzenie", prefiks + "_Uderzenie"), s.getString("skille.obraz", ""),
            s.getString("skille.dlon", ""));
        Zaklecie zak = new Zaklecie(id, s.getString("przedmiot"), s.getDouble("mana-za-tik"),
            s.getString("nazwa", id), s.getString("model-strumienia", "strumien_" + id), skille,
            s.getString("typ", "koncentracja"), s.getString("cel", "stozek"),
            Math.max(1, s.getInt("czas-ladowania-t", 8)), s.getDouble("mana", 0),
            s.getDouble("oburacz.efekt", 2.2), s.getDouble("oburacz.mana", 2.8),
            s.getDouble("zasieg", 8), s.getDouble("promien", 2),
            Math.max(1, s.getInt("limit-aktywnych", 1)),
            Math.max(1, s.getInt("czas-trwania-t", s.getInt("czas-zycia-t", 1200))),
            s.getDouble("sila", 0), s.getDouble("redukcja", 0), Math.max(1, s.getInt("narastanie-t", 10)));
        poId.put(id, zak);
        poPrzedmiocie.put(zak.przedmiot().toUpperCase(), zak);
      }
    }
    podwojneMnoznik = cfg.getDouble("podwojne-mnoznik-many", 2.5);
    stozek = new Stozek(cfg.getDouble("stozek.zasieg", 6), cfg.getDouble("stozek.pol-kat", 20));
    stozekPodwojny = new Stozek(cfg.getDouble("stozek-podwojny.zasieg", 7), cfg.getDouble("stozek-podwojny.pol-kat", 22.5));
    skBrakMany = cfg.getString("skille.brak-many", "Zak_BrakMany");
  }

  /** Zaklecie w tym przedmiocie albo null. */
  Zaklecie w(ItemStack item) {
    if (item == null || item.getType().isAir()) return null;
    String typ = MythicBukkit.inst().getItemManager().getMythicTypeFromItem(item);
    return typ == null ? null : poPrzedmiocie.get(typ.toUpperCase());
  }
}
