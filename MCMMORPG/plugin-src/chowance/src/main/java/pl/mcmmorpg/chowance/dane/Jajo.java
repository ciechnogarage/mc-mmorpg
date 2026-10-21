package pl.mcmmorpg.chowance.dane;

import pl.mcmmorpg.chowance.model.Osobnik;

/** Jajo w Wylęgarni profilu: młode już wylosowane (geny ustalone przy złożeniu), wykluwa się po dystansie. */
public final class Jajo {
  public String id = java.util.UUID.randomUUID().toString();
  public Osobnik mlode;
  public double przebyte;
  public int wymagane;
  public long zlozone;
}
