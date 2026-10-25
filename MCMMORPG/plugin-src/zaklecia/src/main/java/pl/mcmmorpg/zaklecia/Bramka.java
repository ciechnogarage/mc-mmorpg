package pl.mcmmorpg.zaklecia;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * Bramka klasy (decyzja wlasciciela 21.10: zaklecia tylko mag i akolita; wojownik i ninja maja skille F).
 * Lista dozwolonych klas MMOCore w plugins/Zaklecia/klasy.yml (`dozwolone-klasy`), pisze ja
 * tools/klasy-rasowe/generuj_klasy.py (wszystkie klasy archetypow mag + akolita). Pusta lista albo brak pliku = bramka wylaczona.
 */
final class Bramka {
  static final String ODMOWA = "§cTwoja klasa nie włada zaklęciami.";
  private final Set<String> dozwolone = new HashSet<>();

  Bramka(Zaklecia plugin) {
    File f = new File(plugin.getDataFolder(), "klasy.yml");
    if (!f.isFile()) { plugin.getLogger().info("Bramka klasy wylaczona (brak klasy.yml)."); return; }
    for (String k : YamlConfiguration.loadConfiguration(f).getStringList("dozwolone-klasy")) dozwolone.add(k.toUpperCase(Locale.ROOT));
    plugin.getLogger().info("Bramka klasy: " + dozwolone.size() + " klas moze rzucac zaklecia.");
  }

  boolean wolno(Player p) {
    if (dozwolone.isEmpty()) return true;
    try {
      PlayerData d = PlayerData.get(p);
      return d != null && d.getProfess() != null && dozwolone.contains(d.getProfess().getId().toUpperCase(Locale.ROOT));
    } catch (RuntimeException e) {
      return false;  // dane MMOCore niewczytane (zmiana postaci) — nie rzucamy
    }
  }
}
