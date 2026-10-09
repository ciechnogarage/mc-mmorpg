package pl.mcmmorpg.chowance;

import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Regresja: ładunek kliknięcia przychodzi z serwera jako SNBT z kluczami w innej kolejności ({b:0,o:1}). */
class OknaTest {
  static int[] parsuj(String snbt) throws Exception {
    var fo = pl.mcmmorpg.chowance.gra.Okna.class.getDeclaredField("KLUCZ_O");
    var fb = pl.mcmmorpg.chowance.gra.Okna.class.getDeclaredField("KLUCZ_B");
    fo.setAccessible(true); fb.setAccessible(true);
    Matcher mo = ((Pattern) fo.get(null)).matcher(snbt), mb = ((Pattern) fb.get(null)).matcher(snbt);
    assertTrue(mo.find() && mb.find(), snbt);
    return new int[] {Integer.parseInt(mo.group(1)), Integer.parseInt(mb.group(1))};
  }

  @Test void obieKolejnosci() throws Exception {
    assertArrayEquals(new int[] {12, 3}, parsuj("{o:12,b:3}"));
    assertArrayEquals(new int[] {12, 3}, parsuj("{b:3,o:12}"));
    assertArrayEquals(new int[] {1, 0}, parsuj("{b:0,o:1}"));
  }
}
