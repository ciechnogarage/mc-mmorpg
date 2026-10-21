package pl.mcmmorpg.chowance.logika;

/** Globalny licznik sygnałów w oknie sekundy, używany na głównym wątku. */
public final class LimitSygnalow {
  private long sekunda = Long.MIN_VALUE;
  private int liczba;
  public boolean wez(long terazMs, int limit) {
    long s = terazMs / 1000;
    if (s != sekunda) { sekunda = s; liczba = 0; }
    if (liczba >= limit) return false;
    liczba++;
    return true;
  }
}
