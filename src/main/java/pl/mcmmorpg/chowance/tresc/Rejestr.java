package pl.mcmmorpg.chowance.tresc;

import java.util.List;
import java.util.Map;
import pl.mcmmorpg.chowance.model.Cecha;
import pl.mcmmorpg.chowance.model.Charakter;
import pl.mcmmorpg.chowance.model.Gatunek;
import pl.mcmmorpg.chowance.model.NagrodaBestiariusza;
import pl.mcmmorpg.chowance.model.Przedmioty;
import pl.mcmmorpg.chowance.model.Ruch;
import pl.mcmmorpg.chowance.model.Startery;
import pl.mcmmorpg.chowance.model.TabelaTypow;
import pl.mcmmorpg.chowance.model.Trener;
import pl.mcmmorpg.chowance.model.Wieza;

/**
 * Cała treść wczytana z plików — niezmienna migawka. Przeładowanie tworzy NOWY rejestr i podmienia
 * referencję atomowo, więc w trakcie walki nic się nie zmienia pod nogami.
 */
public record Rejestr(
    TabelaTypow typy,
    Map<String, Charakter> charaktery,
    Map<String, Gatunek> gatunki,
    Map<String, Ruch> ruchy,
    Map<String, Cecha> cechy,
    Przedmioty przedmioty,
    Startery startery,
    Map<String, Trener> trenerzy,
    Map<String, Wieza> wieze,
    Map<String, NagrodaBestiariusza> nagrody,
    Map<String, List<String>> zdarzenia,
    /** gatunki poza testowymi, posortowane stabilnie (Bestiariusz) */
    List<Gatunek> bestiariusz,
    /** typ moba MythicMobs -> [gatunek, "D"|"DL"|"O"|"OL"] */
    Map<String, String[]> poMobie) {

  public Gatunek gatunek(String id) { return id == null ? null : gatunki.get(id); }
  public Ruch ruch(String id) { return id == null ? null : ruchy.get(id); }
  public Cecha cecha(String id) { return id == null ? null : cechy.get(id); }
  public Charakter charakter(String id) { return id == null ? null : charaktery.get(id); }
}
