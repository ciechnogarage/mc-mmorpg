package pl.mcmmorpg.charactercreator.select;

import java.time.Duration;
import org.bukkit.configuration.ConfigurationSection;

/**
 * {@code wybor-postaci} w config.yml.
 *
 * @param listFrom od ilu postaci lista (siatka) otwiera sie sama przy wejsciu; 0 = nigdy
 * @param trashTtl jak dlugo postac lezy w koszu, zanim zniknie naprawde
 * @param confirmNameFromLevel od ktorego poziomu usuniecie wymaga wpisania imienia
 * @param places {@code miejsca}: lista {@code {swiat: <regex>, nazwa: <napis>}}
 * @param leftovers {@code po-usunieciu}: pliki postaci przenoszone do archiwum po trwalym usunieciu
 */
public record SelectSettings(int listFrom, Duration trashTtl, int confirmNameFromLevel,
    java.util.List<java.util.Map<?, ?>> places, java.util.List<String> leftovers) {

  public static SelectSettings read(ConfigurationSection root) {
    ConfigurationSection s = root == null ? null : root.getConfigurationSection("wybor-postaci");
    if (s == null) {
      return new SelectSettings(6, Duration.ofDays(7), 10, java.util.List.of(), ProfileLeftovers.DEFAULT);
    }
    return new SelectSettings(
        Math.max(0, s.getInt("lista-od", 6)),
        Duration.ofDays(Math.max(1, s.getInt("kosz-dni", 7))),
        Math.max(1, s.getInt("imie-od-poziomu", 10)),
        s.getMapList("miejsca"),
        s.isList("po-usunieciu") ? java.util.List.copyOf(s.getStringList("po-usunieciu")) : ProfileLeftovers.DEFAULT);
  }
}
