package pl.mcmmorpg.charactercreator.select;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/**
 * Czysta logika ekranu wyboru — bez Bukkita, pod testy.
 *
 * <p><b>Kolejnosc jest STALA</b>: tak, jak postacie leza w MMOProfiles (czyli w kolejnosci
 * zakladania). Wczesniej lista byla sortowana po "ostatnio grana", wiec postac zmieniala miejsce
 * przy kazdym wejsciu i nie dalo sie zapamietac, ze "mag jest trzeci". Ostatnio grana jest teraz
 * tylko WYROZNIONA i domyslnie zaznaczona.
 */
public final class SelectOrder {

  private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d.MM.yyyy");

  private SelectOrder() {
  }

  /**
   * Indeks ostatnio granej postaci (najpozniejszy {@code lastUsed}); przy remisie pierwsza z nich.
   * Pusta lista = -1, brak dat (null) = 0.
   */
  public static <T> int lastUsedIndex(List<T> ordered, Function<T, Instant> lastUsed) {
    int best = -1;
    Instant bestAt = null;
    for (int i = 0; i < ordered.size(); i++) {
      Instant at = lastUsed.apply(ordered.get(i));
      if (best < 0 || (at != null && (bestAt == null || at.isAfter(bestAt)))) {
        best = i;
        bestAt = at;
      }
    }
    return best;
  }

  /**
   * Indeks, na ktorym zostac po usunieciu czesci listy: ta sama postac, jesli przetrwala,
   * inaczej najblizsza nastepna (albo ostatnia). Lista pusta = 0.
   */
  public static <T, K> int keepPosition(List<T> before, int index, List<T> after, Function<T, K> key) {
    if (after.isEmpty()) {
      return 0;
    }
    for (int i = index; i < before.size(); i++) {
      K k = key.apply(before.get(i));
      for (int j = 0; j < after.size(); j++) {
        if (key.apply(after.get(j)).equals(k)) {
          return j;
        }
      }
    }
    return after.size() - 1;
  }

  /** "dziś 12:49", "wczoraj 18:03", "3 dni temu", "12.09.2026"; {@code null} = "nigdy". */
  public static String when(Instant then, Instant now, ZoneId zone) {
    if (then == null || then.toEpochMilli() <= 0) {
      return "nigdy";
    }
    ZonedDateTime t = then.atZone(zone);
    LocalDate today = now.atZone(zone).toLocalDate();
    LocalDate day = t.toLocalDate();
    if (day.equals(today)) {
      return "dziś " + HOUR.format(t);
    }
    if (day.equals(today.minusDays(1))) {
      return "wczoraj " + HOUR.format(t);
    }
    long days = java.time.temporal.ChronoUnit.DAYS.between(day, today);
    if (days > 1 && days < 7) {
      return days + " dni temu";
    }
    return DAY.format(t);
  }

  /** "45 min", "3 h 05 min". */
  public static String playTime(long millis) {
    long minutes = Math.max(0, millis) / 60_000L;
    if (minutes < 60) {
      return minutes + " min";
    }
    return (minutes / 60) + " h " + String.format("%02d", minutes % 60) + " min";
  }

  /** Czy imie wpisane w potwierdzeniu zgadza sie z imieniem postaci (bez wielkosci liter i spacji). */
  public static boolean nameMatches(String typed, String name) {
    return typed != null && name != null && typed.trim().equalsIgnoreCase(name.trim());
  }

  /** "1 postać", "2 postacie", "5 postaci". */
  public static String characters(int n) {
    int mod10 = n % 10;
    int mod100 = n % 100;
    if (n == 1) {
      return "1 postać";
    }
    if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
      return n + " postacie";
    }
    return n + " postaci";
  }

  /** "1 dzień", "3 dni". */
  public static String days(long n) {
    return n == 1 ? "1 dzień" : n + " dni";
  }
}
