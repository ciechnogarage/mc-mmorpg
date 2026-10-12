package pl.mcmmorpg.charactercreator.creator;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Walidacja imienia postaci. Czysta logika, bez Bukkita — jedyny element kreatora, ktory
 * da sie w calosci przetestowac bez serwera (patrz src/test).
 *
 * <p><b>Dlaczego whitelist, a nie {@code \p{L}}.</b> Poprzednia wersja przepuszczala cale
 * Unicode, wiec cyrylickie {@code А} (U+0410) dawalo imie wizualnie NIEODROZNIALNE od
 * lacinskiego {@code A} — mozna bylo zrobic postac wygladajaca dokladnie jak "Admin".
 * Whitelist ogranicza zbior do alfabetu lacinskiego, polskich znakow, cyfr i podkreslnika.
 *
 * <p>Normalizacja NFKC idzie PRZED walidacja, zeby warianty zgodnosciowe (szerokie formy
 * ASCII, ligatury) sprowadzic do postaci kanonicznej zamiast odrzucac je z bledem, ktorego
 * gracz nie zrozumie.
 */
public final class NameRules {

  public static final int MIN_LENGTH = 3;
  public static final int MAX_LENGTH = 16;

  private static final Pattern ALLOWED =
      Pattern.compile("[A-Za-zĄĆĘŁŃÓŚŹŻąćęłńóśźż0-9_]+");
  private static final Pattern HAS_LETTER =
      Pattern.compile(".*[A-Za-zĄĆĘŁŃÓŚŹŻąćęłńóśźż].*");

  /**
   * Prefiksy zarezerwowane dla obslugi serwera. Sprawdzane po sprowadzeniu do malych liter,
   * wiec "AdminXyz", "adminxyz" i "ADMINxyz" sa odrzucane tak samo.
   */
  private static final List<String> RESERVED_PREFIXES = List.of(
      "admin", "administrator", "moderator", "mod_", "gm_", "owner", "właściciel",
      "wlasciciel", "staff", "server", "serwer", "console", "konsola", "support",
      "helper", "mcmmorpg");

  public enum Rejection {
    EMPTY("Imię nie może być puste."),
    TOO_SHORT("Imię musi mieć co najmniej " + MIN_LENGTH + " znaki."),
    TOO_LONG("Imię może mieć najwyżej " + MAX_LENGTH + " znaków."),
    ILLEGAL_CHARS("Dozwolone są tylko litery (w tym polskie), cyfry i podkreślnik."),
    NO_LETTER("Imię musi zawierać przynajmniej jedną literę."),
    RESERVED_PREFIX("To imię jest zarezerwowane dla obsługi serwera."),
    DUPLICATE("Postać o tym imieniu już istnieje — wybierz inne."),
    OFFENSIVE("To imię jest niedozwolone — wybierz inne.");

    private final String message;

    Rejection(String message) {
      this.message = message;
    }

    /** Gotowy komunikat dla gracza — konkretny powod, nie ogolne "zle imie". */
    public String message() {
      // Tekst z configu (teksty.imie.<powod>), domyslnie jak w kodzie. Bez Bukkita w testach
      // Teksty ma pusta mape, wiec testy widza tekst domyslny.
      return pl.mcmmorpg.charactercreator.Teksty.plain("imie." + name().toLowerCase(java.util.Locale.ROOT).replace('_', '-'), message);
    }
  }

  /** Wynik walidacji: albo poprawne imie, albo konkretny powod odmowy. Nigdy oba. */
  public record Result(String name, Rejection rejection) {

    public boolean ok() {
      return name != null;
    }

    public static Result of(String name) {
      return new Result(name, null);
    }

    public static Result reject(Rejection rejection) {
      return new Result(null, rejection);
    }
  }

  private NameRules() {
  }

  public static Result validate(String raw) {
    if (raw == null) {
      return Result.reject(Rejection.EMPTY);
    }
    String name = Normalizer.normalize(raw, Normalizer.Form.NFKC).trim();
    if (name.isEmpty()) {
      return Result.reject(Rejection.EMPTY);
    }
    if (name.length() < MIN_LENGTH) {
      return Result.reject(Rejection.TOO_SHORT);
    }
    if (name.length() > MAX_LENGTH) {
      return Result.reject(Rejection.TOO_LONG);
    }
    if (!ALLOWED.matcher(name).matches()) {
      return Result.reject(Rejection.ILLEGAL_CHARS);
    }
    if (!HAS_LETTER.matcher(name).matches()) {
      return Result.reject(Rejection.NO_LETTER);
    }
    String lower = name.toLowerCase(Locale.ROOT);
    for (String prefix : RESERVED_PREFIXES) {
      if (lower.startsWith(prefix)) {
        return Result.reject(Rejection.RESERVED_PREFIX);
      }
    }
    if (isOffensive(name)) {
      return Result.reject(Rejection.OFFENSIVE);
    }
    return Result.of(name);
  }

  // --- filtr wulgaryzmow (2026-09-30) ---------------------------------------------------

  /**
   * Wbudowana lista na wypadek configu bez {@code names.blocked}. Celowo slowa DLUZSZE:
   * krotkie angielskie ("ass", "cum") siedza w zwyklych imionach (Kassandra, Cumulus).
   * Porownanie po {@link #filterKey} — "Kuuurw4", "kurwa_" i "KÚRWA" to ten sam klucz.
   */
  static final List<String> DEFAULT_BLOCKED = List.of(
      "kurw", "chuj", "huj", "pierdol", "pierdziel", "jeban", "jebac", "jebie", "zajeb",
      "wyjeb", "pojeb", "spierdal", "skurwysyn", "skurwiel", "cipa", "cipsk", "pizda", "pizdzi",
      "dziwk", "szmata", "fiut", "kutas", "pedal", "cwel", "debil", "kretyn", "sukinsyn",
      "hitler", "fuck", "shit", "bitch", "whore", "slut", "cunt", "pussy",
      "nigger", "nigga", "faggot", "retard", "porn", "penis", "vagina");

  private static volatile List<String> blocked = keys(DEFAULT_BLOCKED);

  /** Lista z configu ({@code names.blocked}); pusta albo null = lista wbudowana. */
  public static void setBlockedWords(java.util.Collection<String> words) {
    blocked = keys(words == null || words.isEmpty() ? DEFAULT_BLOCKED : words);
  }

  private static List<String> keys(java.util.Collection<String> words) {
    List<String> out = new java.util.ArrayList<>();
    for (String w : words) {
      String k = filterKey(w);
      if (k.length() >= 3) {
        out.add(k);
      }
    }
    return List.copyOf(out);
  }

  /** Czy imie zawiera slowo z listy (po sprowadzeniu obu stron do {@link #filterKey}). */
  public static boolean isOffensive(String name) {
    String key = filterKey(name);
    // "chój" / "hój" — ó pisane zamiast u; sprawdzamy oba odczyty.
    String alt = key.replace('o', 'u');
    for (String word : blocked) {
      if (key.contains(word) || alt.contains(word)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Klucz porownania: male litery, bez polskich znakow i akcentow, podstawowy leetspeak
   * (0-o, 1-i, 3-e, 4-a, 5-s, 7-t, 8-b, 9-g, @-a, $-s), bez podkreslnikow, powtorzone
   * litery zlane ("kuuurwa" -> "kurwa").
   */
  static String filterKey(String raw) {
    if (raw == null) {
      return "";
    }
    String s = Normalizer.normalize(raw.toLowerCase(Locale.ROOT).replace('ł', 'l'), Normalizer.Form.NFKD)
        .replaceAll("\\p{M}+", "");
    StringBuilder out = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = switch (s.charAt(i)) {
        case '0' -> 'o';
        case '1', '!', '|' -> 'i';
        case '3' -> 'e';
        case '4', '@' -> 'a';
        case '5', '$' -> 's';
        case '7' -> 't';
        case '8' -> 'b';
        case '9' -> 'g';
        default -> s.charAt(i);
      };
      if (c < 'a' || c > 'z') {
        continue;
      }
      if (out.length() > 0 && out.charAt(out.length() - 1) == c) {
        continue;
      }
      out.append(c);
    }
    return out.toString();
  }

  /**
   * Klucz indeksu unikalnosci. Male litery + usuniete podkreslniki, zeby "Grim_test"
   * i "grimtest" nie mogly wspolistniec — to ta sama nazwa dla ludzkiego oka.
   */
  public static String uniqueKey(String name) {
    return name == null ? "" : name.toLowerCase(Locale.ROOT).replace("_", "");
  }
}
