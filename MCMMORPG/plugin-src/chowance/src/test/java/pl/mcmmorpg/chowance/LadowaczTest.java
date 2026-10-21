package pl.mcmmorpg.chowance;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import pl.mcmmorpg.chowance.model.Typ;
import pl.mcmmorpg.chowance.tresc.LadowaczTresci;
import pl.mcmmorpg.chowance.tresc.Rejestr;

class LadowaczTest {
  @Test void fixtureBezBledow() throws Exception {
    Rejestr r = Fixture.rejestr();
    assertEquals(3, r.gatunki().size());
    assertTrue(r.bestiariusz().isEmpty(), "testowe gatunki poza Bestiariuszem");
    assertEquals(2.0, r.typy().mnoznik(Typ.WODA, Typ.OGIEN));
    assertEquals(0.5, r.typy().mnoznik(Typ.ZIEMIA, Typ.POWIETRZE), "tabela bez 0.0 (B-005)");
    assertEquals(1.25, r.typy().stab(), 1e-9);
    assertEquals(new String[] {"TESTOGNIK", "D"}[0], r.poMobie().get("CHW_TEST_BRAK_D")[0]);
    assertTrue(r.charaktery().size() >= 10);
  }

  @Test void wadliweWpisyZglaszaneZPlikiemIKluczem() throws Exception {
    Path d = Fixture.katalog();
    Files.writeString(d.resolve("gatunki/b_zle.yml"), """
        ZLY_TYP:
          typy: [PLAZMA]
          staty_bazowe: {zdrowie: 1, atak: 1, obrona: 1, atak_spec: 1, obrona_spec: 1, szybkosc: 1}
          cechy: [TEST_ZAR]
          ruchy: {1: [TEST_DRAPNIECIE]}
          mob: {zastepczy: FOX}
        MARTWY_RUCH:
          typy: [OGIEN]
          staty_bazowe: {zdrowie: 1, atak: 1, obrona: 1, atak_spec: 1, obrona_spec: 1, szybkosc: 1}
          cechy: [TEST_ZAR]
          ruchy: {1: [NIE_MA_TAKIEGO]}
          mob: {zastepczy: FOX}
          literowka_klucza: 5
        zle_id:
          typy: [OGIEN]
        """);
    Files.writeString(d.resolve("cechy/b.yml"), "ZLA: {rodzaj: TELEPORTACJA}\n");
    LadowaczTresci l = new LadowaczTresci(d.toFile());
    Rejestr r = l.wczytaj();
    String all = l.problemy().wpisy().toString();
    assertFalse(r.gatunki().containsKey("ZLY_TYP"));
    assertFalse(r.gatunki().containsKey("MARTWY_RUCH"));
    assertTrue(r.gatunki().containsKey("TESTOGNIK"), "dobre gatunki wchodzą mimo złych obok");
    assertTrue(all.contains("gatunki/b_zle.yml: ZLY_TYP.typy"), all);
    assertTrue(all.contains("NIE_MA_TAKIEGO"), all);
    assertTrue(all.contains("literowka_klucza"), all);
    assertTrue(all.contains("zle_id"), all);
    assertTrue(all.contains("TELEPORTACJA"), all);
  }

  @Test void ewolucjaDoNieistniejacegoWylaczaGatunekKaskadowo() throws Exception {
    Path d = Fixture.katalog();
    Files.writeString(d.resolve("gatunki/c.yml"), """
        A1:
          typy: [OGIEN]
          linia: A1
          staty_bazowe: {zdrowie: 1, atak: 1, obrona: 1, atak_spec: 1, obrona_spec: 1, szybkosc: 1}
          cechy: [TEST_ZAR]
          ruchy: {1: [TEST_DRAPNIECIE]}
          ewolucje: [{do: A2, poziom: 5}]
          mob: {zastepczy: FOX}
        """);
    LadowaczTresci l = new LadowaczTresci(d.toFile());
    Rejestr r = l.wczytaj();
    assertFalse(r.gatunki().containsKey("A1"));
    assertTrue(l.problemy().wpisy().toString().contains("A2"));
  }
}
