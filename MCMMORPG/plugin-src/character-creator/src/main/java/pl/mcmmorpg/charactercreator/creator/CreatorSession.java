package pl.mcmmorpg.charactercreator.creator;

import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;

/**
 * In-progress creation state for one player, held only while they're in the creator room.
 */
public final class CreatorSession {

  private final UUID player;
  private final UUID profileId;
  private final PendingCreation pendingCreation;
  private final List<String> raceIds;
  private final List<String> classIds;
  private int raceIndex = 0;
  private int classIndex = 0;
  private String pendingName;
  /**
   * Postac stoi na stale PRZODEM do gracza — obrotu nie ma, patrz CreatorMenu.
   *
   * <p>Bylo tu 180f, co jest dokladnie odwrotnoscia tego, co obiecywal komentarz obok:
   * gracz widzial potylice i plecy. Wyprowadzenie: yaw 0 = zwrot na +Z, yaw 180 = na -Z.
   * Gracz stoi na z = 0.5 z yaw 180 (patrzy w -Z), dummy stoi na z = -2.2, czyli w -Z od
   * gracza. Zeby dummy patrzyl NA gracza, musi byc zwrocony w +Z — czyli yaw 0.
   * Potwierdzone zrzutem z prawdziwego klienta, nie wyliczeniem na sucho.
   */
  private static final float MODEL_YAW = 0f;
  private long dialogOpenedAt;
  private Entity previewDummy;
  private Inventory menuInventory;
  private EntryState entryState;
  private volatile boolean confirming;
  private long lastMenuReopenAt;

  public CreatorSession(UUID player, UUID profileId, PendingCreation pendingCreation,
      List<String> raceIds, List<String> classIds) {
    this.player = player;
    this.profileId = profileId;
    this.pendingCreation = pendingCreation;
    this.raceIds = raceIds;
    this.classIds = classIds;
  }

  public UUID player() {
    return player;
  }

  /** The MMOProfiles profile id this creator session will finalize into. */
  public UUID profileId() {
    return profileId;
  }

  public PendingCreation pendingCreation() {
    return pendingCreation;
  }

  public String currentRaceId() {
    return raceIds.get(raceIndex);
  }

  public String currentClassId() {
    return classIds.get(classIndex);
  }

  /** Lista ID ras w kolejnosci z config.yml — menu mapuje ja na sloty. */
  public List<String> raceIds() {
    return raceIds;
  }

  /** Lista ID klas w kolejnosci z config.yml. */
  public List<String> classIds() {
    return classIds;
  }

  /** Bezposredni wybor rasy z listy (klikniecie w konkretna ikone, nie prev/next). */
  public void selectRace(int index) {
    if (index >= 0 && index < raceIds.size()) {
      raceIndex = index;
    }
  }

  /** Bezposredni wybor klasy z listy. */
  public void selectClass(int index) {
    if (index >= 0 && index < classIds.size()) {
      classIndex = index;
    }
  }

  public void cycleRace(int direction) {
    raceIndex = Math.floorMod(raceIndex + direction, raceIds.size());
  }

  public void cycleClass(int direction) {
    classIndex = Math.floorMod(classIndex + direction, classIds.size());
  }

  /**
   * Znacznik "czeka na dialog imienia". Watchdog w CreatorSessionManager nie moze wtedy
   * otwierac menu, bo zasloniloby dialog. Limit czasu jest zabezpieczeniem na wypadek
   * sciezki, ktora nie wywola zadnego callbacku — lepiej odtworzyc menu niz zostawic
   * gracza uwiezionego bez GUI.
   */
  public void markDialogOpen() {
    dialogOpenedAt = System.currentTimeMillis();
  }

  public void clearDialog() {
    dialogOpenedAt = 0L;
  }

  public boolean awaitingDialog() {
    return dialogOpenedAt > 0L && System.currentTimeMillis() - dialogOpenedAt < 120_000L;
  }

  /** Katy obrotu modelu — sterowane RECZNIE strzalkami, model nie kreci sie sam. */
  public float modelYaw() {
    return MODEL_YAW;
  }

  public String pendingName() {
    return pendingName;
  }

  public void setPendingName(String pendingName) {
    this.pendingName = pendingName;
  }

  /** Nazwa do pokazania w pasku u gory — nigdy pusta. */
  public String displayName() {
    return hasName() ? pendingName : "—";
  }

  public boolean hasName() {
    return pendingName != null && !pendingName.isBlank();
  }

  public Entity previewDummy() {
    return previewDummy;
  }

  public void setPreviewDummy(Entity previewDummy) {
    this.previewDummy = previewDummy;
  }

  /** The currently-open CreatorMenu inventory for this session, if any. */
  public Inventory menuInventory() {
    return menuInventory;
  }

  public void setMenuInventory(Inventory menuInventory) {
    this.menuInventory = menuInventory;
  }

  /** Stan gracza sprzed wejscia do kreatora — to, co trzeba mu oddac przy wyjsciu. */
  public EntryState entryState() {
    return entryState;
  }

  public void setEntryState(EntryState entryState) {
    this.entryState = entryState;
  }

  /**
   * Blokada podwojnego potwierdzenia.
   *
   * <p>Ceremonia po kliknieciu "STWORZ POSTAC" trwa 45 tickow, a przez ten czas straznik
   * odtwarzal menu co 10 tickow — razem z klikalnym przyciskiem. Drugie klikniecie dawalo
   * drugi {@code validate()} na TYM SAMYM evencie MMOProfiles, drugi teleport i drugi
   * komplet komend konsolowych. Flaga jest ustawiana na wejsciu do ceremonii i czytana
   * zarowno przez obsluge klikniecia, jak i przez straznika menu.
   */
  public boolean confirming() {
    return confirming;
  }

  public void markConfirming() {
    this.confirming = true;
  }

  /** Throttling straznika menu — patrz CreatorSessionManager#reopenLostMenus. */
  public boolean menuReopenAllowed(long now, long cooldownMillis) {
    if (now - lastMenuReopenAt < cooldownMillis) {
      return false;
    }
    lastMenuReopenAt = now;
    return true;
  }
}
