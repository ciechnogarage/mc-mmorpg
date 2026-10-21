package pl.mcmmorpg.chowance.model;

import java.util.List;

public record Ruch(String id, String nazwa, String opis, Typ typ, Kategoria kategoria, int moc,
    int celnosc, int priorytet, double odnowienie, boolean kontakt, List<EfektRuchu> efekty, String skill) {

  public boolean zawszeTrafia() { return celnosc <= 0; }
}
