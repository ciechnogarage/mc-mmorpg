package pl.mcmmorpg.charactercreator.scene;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

/**
 * Scena jednego gracza. Wszystkie encje sa widoczne TYLKO dla niego
 * ({@code setVisibleByDefault(false)} + {@code showEntity}).
 *
 * <p>Kontroler rozmawia z graczem wylacznie przez te klase: {@link #point}, {@link #card},
 * {@link #hint}, {@link #fade}. Nie dotyka encji kamery ani gracza bezposrednio — scena
 * pilnuje trybu gry, celu kamery i blokad.
 */
public final class SceneView {

  private final SceneManager manager;
  private final UUID playerId;
  private final Location origin;
  private final GameMode originMode;
  private final Map<String, Object> state = new HashMap<>();

  SceneController controller;
  ItemDisplay camera;
  Interaction shield;
  TextDisplay card;
  SceneSettings.Point currentPoint;
  String currentPointId;
  Component hint = Component.empty();
  /** Tick serwera, do ktorego wejscie jest polykane (sciemnienie w toku). */
  long busyUntilTick;
  /** Po ilu tickach od otwarcia kamera ma byc juz celem spectatora. */
  long targetReadyTick;
  /**
   * Scena sama zmienia cel kamery — listener przepuszcza wtedy Start/StopSpectating,
   * ktore w kazdej innej sytuacji anuluje.
   */
  boolean retargeting;
  /** PPM przychodzi dwa razy (obie rece) i czasem w dwoch kolejnych tickach. */
  long lastBackTick;
  boolean lastLeft;
  boolean lastRight;
  boolean closed;

  SceneView(SceneManager manager, Player player) {
    this.manager = manager;
    this.playerId = player.getUniqueId();
    this.origin = player.getLocation().clone();
    this.originMode = player.getGameMode();
  }

  public UUID playerId() {
    return playerId;
  }

  public Player player() {
    return org.bukkit.Bukkit.getPlayer(playerId);
  }

  Location origin() {
    return origin;
  }

  GameMode originMode() {
    return originMode;
  }

  public SceneController controller() {
    return controller;
  }

  /** Podmiana ekranu (np. rasa -> klasa). Nowy kontroler dostaje {@code onOpen}. */
  public void switchTo(SceneController next) {
    manager.switchController(this, next);
  }

  /** Kadr: kamera i karta na punkt o tej nazwie (albo domyslny). Bez sciemnienia. */
  public void point(String pointId) {
    manager.moveToPoint(this, pointId);
  }

  public String pointId() {
    return currentPointId;
  }

  /** Miejsce postaci w biezacym kadrze — tu kontroler stawia podglad. */
  public Location subject() {
    return manager.subjectFacingCamera(this);
  }

  /** Tresc karty obok postaci. {@code null} chowa karte. */
  public void card(Component text) {
    manager.setCard(this, text);
  }

  /** Podpowiedz sterowania na action barze, odswiezana przez scene. */
  public void hint(Component text) {
    this.hint = text == null ? Component.empty() : text;
    manager.pushHint(this);
  }

  /** Sciemnienie: {@code atBlack} wykonuje sie, gdy ekran jest czarny. */
  public void fade(Runnable atBlack) {
    manager.fade(this, atBlack);
  }

  /** Zamyka scene i oddaje gracza tam, skad przyszedl. */
  public void close() {
    manager.close(playerId);
  }

  /** Encje sceny, ktore maja byc widoczne tylko dla tego gracza (np. podglad postaci). */
  public void own(Entity entity) {
    manager.ownEntity(this, entity);
  }

  @SuppressWarnings("unchecked")
  public <T> T get(String key) {
    return (T) state.get(key);
  }

  public void put(String key, Object value) {
    state.put(key, value);
  }

  public boolean isClosed() {
    return closed;
  }
}
