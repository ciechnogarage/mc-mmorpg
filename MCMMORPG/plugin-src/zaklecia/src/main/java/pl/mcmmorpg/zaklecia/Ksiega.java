package pl.mcmmorpg.zaklecia;

import io.lumine.mythic.bukkit.MythicBukkit;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

/** Znane zaklecia z config.yml i rozpoznawanie ich w przedmiotach (po typie MythicMobs). */
final class Ksiega {
  record Zaklecie(String id, String przedmiot, double manaZaTik, String nazwa) {}
  record Stozek(double zasieg, double polKat) {}

  final Map<String, Zaklecie> poId = new LinkedHashMap<>();
  private final Map<String, Zaklecie> poPrzedmiocie = new LinkedHashMap<>();
  final double podwojneMnoznik;
  final String skStart, skStop, skObraz, skTik, skTrafienia, skBrakMany, skDlon;
  final Stozek stozek, stozekPodwojny;

  Ksiega(ConfigurationSection cfg) {
    ConfigurationSection z = cfg.getConfigurationSection("zaklecia");
    if (z != null) {
      for (String id : z.getKeys(false)) {
        ConfigurationSection s = z.getConfigurationSection(id);
        Zaklecie zak = new Zaklecie(id, s.getString("przedmiot"), s.getDouble("mana-za-tik"), s.getString("nazwa", id));
        poId.put(id, zak);
        poPrzedmiocie.put(zak.przedmiot().toUpperCase(), zak);
      }
    }
    podwojneMnoznik = cfg.getDouble("podwojne-mnoznik-many", 2.5);
    skStart = cfg.getString("skille.start", "Zak_Start");
    skStop = cfg.getString("skille.stop", "Zak_Stop");
    skObraz = cfg.getString("skille.obraz", "Zak_Obraz");
    skTik = cfg.getString("skille.tik", "Zak_Tik");
    skTrafienia = cfg.getString("skille.trafienia", "Zak_Trafienia");
    stozek = new Stozek(cfg.getDouble("stozek.zasieg", 6), cfg.getDouble("stozek.pol-kat", 20));
    stozekPodwojny = new Stozek(cfg.getDouble("stozek-podwojny.zasieg", 7), cfg.getDouble("stozek-podwojny.pol-kat", 22.5));
    skBrakMany = cfg.getString("skille.brak-many", "Zak_BrakMany");
    skDlon = cfg.getString("skille.dlon", "Zak_Dlon");
  }

  /** Zaklecie w tym przedmiocie albo null. */
  Zaklecie w(ItemStack item) {
    if (item == null || item.getType().isAir()) return null;
    String typ = MythicBukkit.inst().getItemManager().getMythicTypeFromItem(item);
    return typ == null ? null : poPrzedmiocie.get(typ.toUpperCase());
  }
}
