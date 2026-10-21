package pl.mcmmorpg.chowance;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import pl.mcmmorpg.chowance.tresc.LadowaczTresci;
import pl.mcmmorpg.chowance.tresc.Rejestr;

/** Katalog treści zbudowany z zasobów pluginu (typy, charaktery, gatunki testowe) + przedmioty testowe. */
public final class Fixture {
  public static Path katalog() throws IOException {
    Path d = Files.createTempDirectory("chw");
    kopiuj("typy.yml", d.resolve("typy.yml"));
    kopiuj("charaktery.yml", d.resolve("charaktery.yml"));
    Files.createDirectories(d.resolve("gatunki"));
    Files.createDirectories(d.resolve("ruchy"));
    Files.createDirectories(d.resolve("cechy"));
    kopiuj("testowe/gatunki.yml", d.resolve("gatunki/zz_silnik_test.yml"));
    kopiuj("testowe/ruchy.yml", d.resolve("ruchy/zz_silnik_test.yml"));
    kopiuj("testowe/cechy.yml", d.resolve("cechy/zz_silnik_test.yml"));
    Files.writeString(d.resolve("przedmioty.yml"), """
        pieczecie:
          PIECZEC_KAMIENNA: {rodzaj: ZWYKLA, mnoznik: 1.0}
          PIECZEC_NOCNA: {rodzaj: NOCNA, mnoznik: 1.0, mnoznik_warunku: 3.0}
        kamienie: [KAMIEN_TEST]
        karma:
          PROF_TEST_PRZYSMAK: {exp: 20, wiez: 3}
        """);
    return d;
  }

  public static Rejestr rejestr() throws IOException {
    LadowaczTresci l = new LadowaczTresci(katalog().toFile());
    Rejestr r = l.wczytaj();
    if (l.problemy().bledy() > 0) throw new AssertionError("fixture ma błędy: " + l.problemy().wpisy());
    return r;
  }

  private static void kopiuj(String zasob, Path cel) throws IOException {
    try (InputStream in = Fixture.class.getClassLoader().getResourceAsStream(zasob)) {
      if (in == null) throw new IOException("brak zasobu " + zasob);
      Files.copy(in, cel, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
