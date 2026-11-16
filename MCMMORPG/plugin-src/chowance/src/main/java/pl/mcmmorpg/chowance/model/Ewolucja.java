package pl.mcmmorpg.chowance.model;

/** Gałąź ewolucji: poziom + opcjonalny przedmiot (ID MMOItems) + opcjonalna minimalna więź. */
public record Ewolucja(String doGatunku, int poziom, String przedmiot, int wiez) {
  public boolean wymagaPrzedmiotu() { return przedmiot != null && !przedmiot.isBlank(); }
}
