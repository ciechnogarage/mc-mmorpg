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
 */
public record CharacterRecord(
    String characterId,
    String name,
    String raceId,
    String classId,
    String skinSource,
    UUID ownerUuid,
    String ownerName,
    Instant createdAt
) {

  /** Rekord ze schematu 1 (bez wlasciciela) — migracja ustawia oba pola na null/pusty. */
  public boolean hasOwner() {
    return ownerUuid != null;
  }
}
