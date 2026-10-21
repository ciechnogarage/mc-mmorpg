package pl.mcmmorpg.chowance.tresc;

import java.util.ArrayList;
import java.util.List;

/** Zbiór błędów/ostrzeżeń walidacji treści — z plikiem i kluczem, żeby B wiedział, co poprawić. */
public final class Problemy {
  public record Wpis(boolean blad, String plik, String klucz, String opis) {
    @Override public String toString() {
      return (blad ? "BŁĄD " : "UWAGA ") + plik + ": " + klucz + ": " + opis;
    }
  }

  private final List<Wpis> wpisy = new ArrayList<>();

  public void blad(String plik, String klucz, String opis) { wpisy.add(new Wpis(true, plik, klucz, opis)); }
  public void uwaga(String plik, String klucz, String opis) { wpisy.add(new Wpis(false, plik, klucz, opis)); }
  public List<Wpis> wpisy() { return wpisy; }
  public long bledy() { return wpisy.stream().filter(Wpis::blad).count(); }
  public long uwagi() { return wpisy.stream().filter(w -> !w.blad()).count(); }
}
