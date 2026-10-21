package pl.mcmmorpg.charactercreator.skin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pl.mcmmorpg.charactercreator.skin.SkinService.Skin;

class SkinServiceTest {

  private static SkinService withSkins(String... ids) {
    Map<String, Skin> skins = new LinkedHashMap<>();
    for (String id : ids) {
      skins.put(id, new Skin(id, "value-" + id, "sig-" + id));
    }
    return new SkinService(skins);
  }

  @Test
  void idIsRaceUnderscoreClass() {
    assertEquals("elf_ninja", SkinService.idFor("elf", "ninja"));
  }

  @Test
  void pairSkinIsUsedForFreshCharacter() {
    assertEquals("elf_ninja", withSkins("elf_ninja").resolve(null, "elf", "ninja").id());
  }

  /**
   * To jest ta regula, dla ktorej ten test w ogole istnieje: postac ma zachowac swoj wyglad
   * nawet wtedy, gdy ktos przemapuje klasy w config.yml. Zapisane w rekordzie wygrywa.
   */
  @Test
  void recordedSkinWinsOverCurrentRaceAndClass() {
    SkinService service = withSkins("elf_ninja", "ork_wojownik");
    assertEquals("elf_ninja", service.resolve("elf_ninja", "ork", "wojownik").id());
  }

  /** Rekord wskazujacy na skorke, ktorej juz nie ma, nie moze zostawic postaci bez niczego. */
  @Test
  void missingRecordedSkinFallsBackToPair() {
    SkinService service = withSkins("ork_wojownik");
    assertEquals("ork_wojownik", service.resolve("elf_mnich", "ork", "wojownik").id());
  }

  @Test
  void raceOnlySkinIsTheLastResort() {
    assertEquals("ork", withSkins("ork").resolve(null, "ork", "wojownik").id());
  }

  /** Brak skorki = gracz zostaje w swojej. Zadnego wyjatku i zadnej skorki "domyslnej". */
  @Test
  void nothingMatchesYieldsNull() {
    assertNull(withSkins("elf_ninja").resolve(null, "ork", "wojownik"));
    assertNull(withSkins().resolve("cokolwiek", "ork", "wojownik"));
  }
  /**
   * Raport /cc skin ma nazwac teksture: nasza po id, cudza po hashu. To jedyny wiersz, ktory
   * rozstrzyga zgloszenie "mam nie te skorke", wiec nie moze klamac ani rzucac wyjatkiem.
   */
  @Test
  void ownSkinIsNamedById() {
    SkinService service = withSkins("elf_ninja");
    assertEquals("elf_ninja", service.describeTexture("value-elf_ninja"));
  }

  @Test
  void foreignSkinIsNamedByTextureHash() {
    assertEquals("obca (bab20339a98d46fd...)", withSkins("elf_ninja").describeTexture(REAL_TEXTURE));
  }

  @Test
  void noTextureAndGarbageDoNotBlowUp() {
    SkinService service = withSkins("elf_ninja");
    assertEquals("brak", service.describeTexture(null));
    assertEquals("brak", service.describeTexture("  "));
    assertEquals("obca (nieczytelna)", service.describeTexture("!!! to nie jest base64 !!!"));
  }

  /** Prawdziwa wartosc profilu z skins.yml — zeby test lamal sie na zmianie ksztaltu JSON-a. */
  private static final String REAL_TEXTURE =
      "ewogICJ0aW1lc3RhbXAiOiAxNzg5OTE2NzE2ODMwLAogICJwcm9maWxlSWQiOiAiODhkNTQ1MWIxZWY4NDI3ZWFmZjJlZTQxMGYzMzQyZjAiLAogICJwcm9maWxlTmFtZSI6ICJEb25hbGRUcnVtcDQyIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiOiB0cnVlLAogICJ0ZXh0dXJlcyI6IHsKICAgICJTS0lOIjogewogICAgICAidXJsIjogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmFiMjAzMzlhOThkNDZmZDJmMGJjMWRlMTkzM2NiMTE0ZWQzZWI1ZjE3NGY5Zjg4ZjYyNzg0NWYzMzY0NGQ2MiIKICAgIH0KICB9Cn0=";
}
