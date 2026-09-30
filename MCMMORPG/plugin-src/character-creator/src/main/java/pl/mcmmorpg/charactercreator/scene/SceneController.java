package pl.mcmmorpg.charactercreator.scene;

/**
 * Co robi scena, gdy gracz naciska klawisze. Scena (kamera, sterowanie, sciemnienie,
 * podpowiedzi, karta) jest wspolna; kontroler to jeden ekran: wybor postaci, rasa, klasa.
 *
 * <p>Wszystkie metody sa wolane na watku serwera i NIGDY w trakcie sciemnienia — wejscie
 * w tym czasie scena polyka, zeby dwa szybkie A nie przeskoczyly dwoch pozycji pod czernia.
 */
public interface SceneController {

  /** Scena gotowa, kamera juz patrzy. Tu ustawia sie pierwszy kadr, karte i podpowiedz. */
  void onOpen(SceneView view);

  /** A: poprzednia pozycja. */
  default void onPrevious(SceneView view) {}

  /** D: nastepna pozycja. */
  default void onNext(SceneView view) {}

  /** W: pozycja wyzej na liscie. */
  default void onUp(SceneView view) {}

  /** S: pozycja nizej na liscie. */
  default void onDown(SceneView view) {}

  /** Spacja (skok): w scenie wyboru graj, w kreatorze lista wszystkich klas. Domyslnie nic. */
  default void onJump(SceneView view) {}

  /** LPM. */
  default void onSelect(SceneView view) {}

  /** PPM. */
  default void onBack(SceneView view) {}

  /** Scena sie zamyka (z dowolnego powodu, takze przy wyjsciu gracza z serwera). */
  default void onClose(SceneView view) {}
}
