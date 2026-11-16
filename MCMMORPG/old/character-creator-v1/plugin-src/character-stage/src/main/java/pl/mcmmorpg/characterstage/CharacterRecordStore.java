package pl.mcmmorpg.characterstage;

import java.util.Optional;

public interface CharacterRecordStore {
  Optional<CharacterRecord> findByCharacterId(String characterId);

  record CharacterRecord(
      String characterId,
      String lifecycleStatus,
      String creatorStage,
      int schemaVersion
  ) {}
}
