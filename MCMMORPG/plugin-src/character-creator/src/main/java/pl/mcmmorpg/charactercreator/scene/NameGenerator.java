package pl.mcmmorpg.charactercreator.scene;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import org.bukkit.configuration.ConfigurationSection;
import pl.mcmmorpg.charactercreator.creator.NameRules;

/**
 * Losowe imie w klimacie rasy: poczatek + koncowka z {@code names.<rasa>} w config.yml.
 * Wynik zawsze przechodzi {@link NameRules} i nie jest zajety — inaczej losujemy dalej.
 */
public final class NameGenerator {

  private static final Random RANDOM = new Random();
  private static final int ATTEMPTS = 60;

  private NameGenerator() {
  }

  /** @return wolne imie albo {@code null}, gdy rasa nie ma sylab w configu */
  public static String generate(ConfigurationSection names, String raceId, Predicate<String> taken) {
    ConfigurationSection race = names == null ? null : names.getConfigurationSection(raceId);
    if (race == null) {
      return null;
    }
    List<String> start = race.getStringList("start");
    List<String> end = race.getStringList("end");
    if (start.isEmpty() || end.isEmpty()) {
      return null;
    }
    for (int i = 0; i < ATTEMPTS; i++) {
      String candidate = start.get(RANDOM.nextInt(start.size())) + end.get(RANDOM.nextInt(end.size()));
      NameRules.Result result = NameRules.validate(candidate);
      if (result.ok() && !taken.test(result.name())) {
        return result.name();
      }
    }
    return null;
  }
}
