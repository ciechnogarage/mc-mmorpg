package pl.mcmmorpg.charactercreator.persist;

import java.time.Instant;
import java.util.UUID;

/**
 * One record per MMOProfiles profile. {@code characterId} equals the owning profile's own
 * unique id (see CharacterProfileModule) — MMOProfiles already tracks which player owns
 * which profile, so we don't duplicate that as the KEY.
 *
 * <p>{@code ownerUuid}/{@code ownerName} sa jednak zapisywane od schematu 2: bez nich
 * {@code /cc info <gracz>} i raport sierot nie mialy jak powiazac rekordu z czlowiekiem,
 * a to wlasnie ta niemoznosc pozwolila 18 rekordom testowym przelezec na produkcje
 * niezauwazonym. {@code ownerName} jest wylacznie informacyjne (nick moze sie zmienic) —
 * wiazace jest {@code ownerUuid}.
 *
 * <p>{@code skinSource} to klucz skorki w skins.yml ({@code <rasa>_<klasa>}), zapisywany RAZ,
 * przy tworzeniu postaci — od tej pory to ON decyduje o wygladzie, wiec przemapowanie klas
 * w config.yml nie przebiera istniejacych postaci. Puste = dla tej pary nie bylo wtedy
 * skorki; wygrywa wtedy dzisiejsza para rasa+klasa (SkinService#resolve).
 *
 * <p>{@code lastUsedAt} (schemat 3) to znacznik OSTATNIEGO wyboru tej postaci. Istnieje
 * dla jednej rzeczy: zeby przy wejsciu na serwer, jeszcze PRZED ekranem wyboru, dalo sie
 * nalozyc graczowi skorke postaci, ktora gral ostatnio — inaczej przez te kilkanascie
 * sekund chodzi w skorce swojego konta (patrz CharacterProfileModule#scheduleJoinSkin).
 * Puste u rekordow sprzed schematu 3; wtedy liczy sie {@code createdAt}.
 */
public record CharacterRecord(
    String characterId,
    String name,
    String raceId,
    String classId,
    String skinSource,
    UUID ownerUuid,
    String ownerName,
    Instant createdAt,
    Instant lastUsedAt,
    String iconColor
) {

  /**
   * Rekord bez koloru ikony — wszystko sprzed sceny kreatora. Kolor wybiera sie w ksiedze
   * "Nowa postac" (scena); pusty = domyslny kolor przy wyswietlaniu.
   */
  public CharacterRecord(String characterId, String name, String raceId, String classId,
      String skinSource, UUID ownerUuid, String ownerName, Instant createdAt,
      Instant lastUsedAt) {
    this(characterId, name, raceId, classId, skinSource, ownerUuid, ownerName, createdAt,
        lastUsedAt, "");
  }

  /** Rekord ze schematu 1 (bez wlasciciela) — migracja ustawia oba pola na null/pusty. */
  public boolean hasOwner() {
    return ownerUuid != null;
  }

  /**
   * Kiedy postac byla ostatnio w grze — a dla rekordow sprzed schematu 3 kiedy powstala.
   *
   * <p>Fallback na {@code createdAt} jest celowy i nie jest przyblizeniem "na oko": postac
   * zaraz po utworzeniu JEST ta ostatnio graną, wiec dla kogos, kto ma jedna postac sprzed
   * migracji, odpowiedz i tak wychodzi poprawna.
   */
  public Instant effectiveLastUsed() {
    return lastUsedAt != null ? lastUsedAt : createdAt;
  }

  public CharacterRecord withLastUsed(Instant when) {
    return new CharacterRecord(characterId, name, raceId, classId, skinSource, ownerUuid,
        ownerName, createdAt, when, iconColor);
  }

  /**
   * Nowa skorka postaci — na STALE, czyli w rekordzie.
   *
   * <p>Do {@code /cc skin}: dopoki skorka siedziala tylko na profilu gracza w pamieci,
   * kazdy respawn i kazde wejscie przywracalo te z rekordu. Zapis tutaj jest jedynym
   * miejscem, w ktorym zmiana wygladu postaci przezywa restart serwera.
   */
  public CharacterRecord withSkinSource(String skinSource) {
    return new CharacterRecord(characterId, name, raceId, classId, skinSource, ownerUuid,
        ownerName, createdAt, lastUsedAt, iconColor);
  }

  public CharacterRecord withName(String newName) {
    return new CharacterRecord(characterId, newName, raceId, classId, skinSource, ownerUuid,
        ownerName, createdAt, lastUsedAt, iconColor);
  }

  public CharacterRecord withOwner(UUID uuid, String name) {
    return new CharacterRecord(characterId, this.name, raceId, classId, skinSource, uuid,
        name, createdAt, lastUsedAt, iconColor);
  }
}
