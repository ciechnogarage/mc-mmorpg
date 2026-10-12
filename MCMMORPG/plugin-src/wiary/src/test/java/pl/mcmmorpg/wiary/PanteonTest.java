package pl.mcmmorpg.wiary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** Sprawdza config dostarczany z pluginem (src/main/resources/config.yml) i reguły wyboru. */
class PanteonTest {
  private static final Set<String> STATY = Set.of("ATTACK_SPEED", "ATTACK_DAMAGE", "MAX_HEALTH", "HEALTH_REGENERATION",
      "MOVEMENT_SPEED", "KNOCKBACK_RESISTANCE", "ARMOR", "ARMOR_TOUGHNESS", "MANA_REGENERATION", "LIFESTEAL", "SPELL_VAMPIRISM",
      "CRITICAL_STRIKE_CHANCE", "CRITICAL_STRIKE_POWER", "ADDITIONAL_EXPERIENCE", "COOLDOWN_REDUCTION", "PVE_DAMAGE",
      "UNDEAD_DAMAGE", "PVE_DEFENSE", "MAGICAL_DAMAGE", "PHYSICAL_DAMAGE", "SKILL_DAMAGE", "DAMAGE_REDUCTION");

  private static Panteon wczytaj(List<String> problemy) {
    YamlConfiguration y = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));
    return Panteon.wczytaj(y, problemy);
  }

  @Test
  void configMa15BostwBezProblemow() {
    List<String> problemy = new ArrayList<>();
    Panteon p = wczytaj(problemy);
    assertEquals(List.of(), problemy);
    assertEquals(15, p.bostwa().size());
    for (Panteon.Bostwo b : p.bostwa().values()) {
      for (Panteon.Efekt e : b.efekty()) {
        assertTrue(STATY.contains(e.stat()), b.id() + ": stat spoza listy SharedStat MythicLib: " + e.stat());
        if (e.typ().equals("RELATIVE")) assertTrue(e.opis().startsWith("%"), b.id() + ": RELATIVE musi mieć opis w %");
      }
    }
  }

  @Test
  void kazdaRasaMa6DoWyboru3Starszych3Wlasne() {
    Panteon p = wczytaj(new ArrayList<>());
    for (String rasa : p.rasy().keySet()) {
      List<Panteon.Bostwo> l = p.doWyboru(rasa);
      assertEquals(6, l.size(), rasa);
      assertEquals(3, l.stream().filter(b -> b.grupa().equals(Panteon.STARSI)).count(), rasa);
      assertEquals(3, l.stream().filter(b -> b.grupa().equals(rasa)).count(), rasa);
    }
    assertEquals(0, p.doWyboru(null).size(), "postać bez rasy nie wybiera nikogo");
  }

  @Test
  void obcaRasaNiedostepna() {
    Panteon p = wczytaj(new ArrayList<>());
    assertFalse(p.dostepne(p.bostwo("solenna"), "ork"));
    assertTrue(p.dostepne(p.bostwo("solenna"), "czlowiek"));
    assertTrue(p.dostepne(p.bostwo("vharun"), "nieumarly"));
  }

  @Test
  void imionaUnikalne() {
    Panteon p = wczytaj(new ArrayList<>());
    Set<String> imiona = new TreeSet<>();
    for (Panteon.Bostwo b : p.bostwa().values()) assertTrue(imiona.add(b.imie()), "powtórzone imię " + b.imie());
  }

  @Test
  void silaRosnieZWiaraDoLimitu() {
    Panteon.Efekt e = new Panteon.Efekt("UNDEAD_DAMAGE", "FLAT", 5, 0.25, 1, "% obrażeń");
    assertEquals(5.0, e.wartosc(0, 40), 1e-9);
    assertEquals(10.0, e.wartosc(20, 40), 1e-9);
    assertEquals(15.0, e.wartosc(40, 40), 1e-9);
    assertEquals(15.0, e.wartosc(99, 40), 1e-9);
    assertEquals(5.0, e.wartosc(-3, 40), 1e-9);
    assertEquals("+10% obrażeń", e.tekst(20, 40));
    Panteon.Efekt odrzut = new Panteon.Efekt("KNOCKBACK_RESISTANCE", "FLAT", 0.10, 0.005, 100, "% odporności na odrzut");
    assertEquals("+15% odporności na odrzut", odrzut.tekst(10, 40));
  }

  @Test
  void odnowienieZmiany() {
    long h = 3_600_000L;
    assertEquals(0, Panteon.doZmiany(null, 0, 1000, 24 * h), "pierwszy wybór od razu");
    assertEquals(23 * h, Panteon.doZmiany("solenna", 0, h, 24 * h));
    assertEquals(0, Panteon.doZmiany("solenna", 0, 25 * h, 24 * h));
  }

  @Test
  void formaty() {
    assertEquals("6,5", Panteon.liczba(6.5));
    assertEquals("3", Panteon.liczba(3.0));
    assertEquals("30 min", Panteon.czas(30 * 60_000L));
    assertEquals("1 min", Panteon.czas(1));
    assertEquals("24 h", Panteon.czas(24 * 3_600_000L));
    assertEquals("1 h 5 min", Panteon.czas(65 * 60_000L));
  }
}
