package pl.mcmmorpg.chowance.logika;

import java.util.ArrayList;
import java.util.List;

/**
 * Turniej pucharowy. Uczestnicy rozstawieni kolejnością listy (np. po ELO), wolne losy (null) dla
 * najwyżej rozstawionych, gdy liczba nie jest potęgą 2. Runda = lista par; zwycięzcy przechodzą dalej.
 */
public final class Drabinka {
  public record Para(String a, String b) {
    public boolean wolnyLos() { return a == null || b == null; }
    public String ktokolwiek() { return a != null ? a : b; }
  }

  private List<String> biezacy;
  private int runda = 1;

  public Drabinka(List<String> rozstawieni) {
    if (rozstawieni.size() < 2) throw new IllegalArgumentException("za mało uczestników");
    int n = 1;
    while (n < rozstawieni.size()) n <<= 1;
    List<String> s = new ArrayList<>(rozstawieni);
    while (s.size() < n) s.add(null);
    this.biezacy = kolejnoscRozstawienia(s);
  }

  /** Klasyczne rozstawienie 1 vs n, 2 vs n-1 ... tak, by 1 i 2 spotkali się najpóźniej w finale. */
  private static List<String> kolejnoscRozstawienia(List<String> s) {
    int n = s.size();
    List<Integer> order = new ArrayList<>(List.of(0));
    while (order.size() < n) {
      int m = order.size() * 2;
      List<Integer> next = new ArrayList<>();
      for (int x : order) { next.add(x); next.add(m - 1 - x); }
      order = next;
    }
    List<String> out = new ArrayList<>();
    for (int i : order) out.add(s.get(i));
    return out;
  }

  public int runda() { return runda; }

  public List<Para> pary() {
    List<Para> p = new ArrayList<>();
    for (int i = 0; i + 1 < biezacy.size(); i += 2) p.add(new Para(biezacy.get(i), biezacy.get(i + 1)));
    return p;
  }

  public boolean koniec() { return biezacy.size() == 1; }

  public String mistrz() { return koniec() ? biezacy.get(0) : null; }

  /** Zwycięzcy rundy w kolejności par (wolne losy uzupełnia sama). */
  public void zakonczRunde(java.util.Map<Para, String> zwyciezcy) {
    List<String> nast = new ArrayList<>();
    for (Para p : pary()) {
      if (p.wolnyLos()) { nast.add(p.ktokolwiek()); continue; }
      String w = zwyciezcy.get(p);
      if (w == null) throw new IllegalStateException("brak zwycięzcy pary " + p);
      nast.add(w);
    }
    biezacy = nast;
    runda++;
  }
}
