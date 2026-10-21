package pl.mcmmorpg.charactercreator.creator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NameRulesTest {

  @ParameterizedTest
  @ValueSource(strings = {"Gorak", "Zażółć", "Elf_1", "abc", "SzesnascieZnakow"})
  void acceptsOrdinaryNames(String name) {
    NameRules.Result result = NameRules.validate(name);
    assertTrue(result.ok(), name + " powinno przejsc, a odrzucono: " + result.rejection());
    assertEquals(name, result.name());
  }

  @Test
  void trimsSurroundingWhitespace() {
    assertEquals("Gorak", NameRules.validate("  Gorak  ").name());
  }

  @Test
  void rejectsTooShortAndTooLong() {
    assertEquals(NameRules.Rejection.TOO_SHORT, NameRules.validate("ab").rejection());
    assertEquals(NameRules.Rejection.TOO_LONG,
        NameRules.validate("a".repeat(NameRules.MAX_LENGTH + 1)).rejection());
  }

  @Test
  void rejectsEmptyAndNull() {
    assertEquals(NameRules.Rejection.EMPTY, NameRules.validate(null).rejection());
    assertEquals(NameRules.Rejection.EMPTY, NameRules.validate("   ").rejection());
  }

  /**
   * Regresja na konkretna dziure: stary {@code \p{L}} wpuszczal cale Unicode, wiec
   * cyrylickie A (U+0410) dawalo imie nieodroznialne od lacinskiego "Admin".
   */
  @Test
  void rejectsLookalikeCyrillic() {
    String cyrillicAdmin = "Аdmin";
    assertEquals(NameRules.Rejection.ILLEGAL_CHARS, NameRules.validate(cyrillicAdmin).rejection());
  }

  @ParameterizedTest
  @ValueSource(strings = {"Gor ak", "Gor-ak", "Gorak!", "§cGorak", "Gor.ak"})
  void rejectsPunctuationAndFormatting(String name) {
    assertEquals(NameRules.Rejection.ILLEGAL_CHARS, NameRules.validate(name).rejection());
  }

  @ParameterizedTest
  @ValueSource(strings = {"Admin", "administrator", "MODERATOR", "ServerBot", "konsola1"})
  void rejectsReservedPrefixes(String name) {
    assertEquals(NameRules.Rejection.RESERVED_PREFIX, NameRules.validate(name).rejection());
  }

  @Test
  void rejectsNamesWithoutAnyLetter() {
    assertEquals(NameRules.Rejection.NO_LETTER, NameRules.validate("123_4").rejection());
  }

  /** NFKC sprowadza warianty zgodnosciowe do postaci kanonicznej, zamiast je odrzucac. */
  @Test
  void normalizesFullWidthLatin() {
    NameRules.Result result = NameRules.validate("Ｇｏｒ"); // Ｇｏｒ
    assertTrue(result.ok(), "NFKC powinno sprowadzic pelnoszerokie ASCII do zwyklego");
    assertEquals("Gor", result.name());
  }

  @Test
  void uniqueKeyIgnoresCaseAndUnderscores() {
    assertEquals(NameRules.uniqueKey("Grim_Test"), NameRules.uniqueKey("grimtest"));
    assertFalse(NameRules.uniqueKey("Grimtest").equals(NameRules.uniqueKey("Grimtesta")));
  }

  @Test
  void everyRejectionHasAMessage() {
    for (NameRules.Rejection rejection : NameRules.Rejection.values()) {
      assertFalse(rejection.message().isBlank(), rejection + " nie ma komunikatu dla gracza");
    }
  }

  // --- filtr wulgaryzmow ---

  @ParameterizedTest
  @ValueSource(strings = {"Kurwa", "kuuurwa", "KURW4", "Chuj_", "chój", "Jeb4c", "Pierd0l", "Fuck3r",
      "Sk_urwysyn", "HITLER1", "xXpizdaXx", "Łajza_kurwiszon"})
  void rejectsOffensiveNames(String name) {
    assertEquals(NameRules.Rejection.OFFENSIVE, NameRules.validate(name).rejection(), name);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Kassandra", "Cumulus", "Nazir", "Pasukan", "Dickens", "Grapevine",
      "Bormir", "Galwen", "Thalriel", "Grokthar", "Mirosław", "Huberto", "Szymek"})
  void acceptsInnocentNamesContainingShortWords(String name) {
    assertTrue(NameRules.validate(name).ok(), name + " odrzucono: " + NameRules.validate(name).rejection());
  }

  @Test
  void configListReplacesBuiltInAndEmptyRestoresIt() {
    try {
      NameRules.setBlockedWords(java.util.List.of("gorak"));
      assertEquals(NameRules.Rejection.OFFENSIVE, NameRules.validate("Gorak").rejection());
      assertTrue(NameRules.validate("Kurwa").ok());
    } finally {
      NameRules.setBlockedWords(java.util.List.of());
    }
    assertTrue(NameRules.validate("Gorak").ok());
    assertFalse(NameRules.validate("Kurwa").ok());
  }

  @Test
  void filterKeyStripsDiacriticsLeetAndRepeats() {
    assertEquals("zazolc", NameRules.filterKey("Zażółć"));
    assertEquals("kurwa", NameRules.filterKey("Kuuu_rw4"));
  }
}
