package pl.mcmmorpg.charactercreator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Kafelek klasy w kreatorze pokazuje klase RASOWA (Ork + wojownik = Wodz Wojny), a bez
 * wpisu {@code races.<rasa>} — sam archetyp. Pomylka jest cicha: gracz widzi zla nazwe.
 */
class ClassOptionRaceTest {

  private final CreatorConfig.ClassOption wojownik = new CreatorConfig.ClassOption(
      "wojownik", "&cWojownik", "warrior", "", Map.of(), "Opis archetypu", "Obrońca",
      Map.of("ork", new CreatorConfig.RaceVariant("&cWódz Wojny", "Linia 1\nLinia 2", ""),
          "elf", new CreatorConfig.RaceVariant("", "", "Tylko bonus")));

  @Test
  void wariantRasyZastepujeArchetyp() {
    assertEquals("&cWódz Wojny", wojownik.displayFor("ork"));
    assertEquals("Linia 1\nLinia 2", wojownik.descriptionFor("ork"));
  }

  @Test
  void pustePoleWariantuSpadaDoArchetypu() {
    assertEquals("Obrońca", wojownik.bonusFor("ork"));
    assertEquals("&cWojownik", wojownik.displayFor("elf"));
    assertEquals("Tylko bonus", wojownik.bonusFor("elf"));
  }

  @Test
  void brakRasyAlboWpisuToArchetyp() {
    assertEquals("&cWojownik", wojownik.displayFor(null));
    assertEquals("&cWojownik", wojownik.displayFor("nieumarly"));
    assertEquals("Opis archetypu", wojownik.descriptionFor("nieumarly"));
  }
}
