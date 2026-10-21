package pl.mcmmorpg.characterstage;

public interface ProfileMappingStore {
  LookupResult lookupByProfileId(String profileId);

  record LookupResult(LookupStatus status, ProfileMappingRecord record, String detail) {
    public static LookupResult unique(ProfileMappingRecord record) {
      return new LookupResult(LookupStatus.UNIQUE, record, null);
    }

    public static LookupResult notFound(String detail) {
      return new LookupResult(LookupStatus.NOT_FOUND, null, detail);
    }

    public static LookupResult ambiguous(String detail) {
      return new LookupResult(LookupStatus.AMBIGUOUS, null, detail);
    }

    public static LookupResult unavailable(String detail) {
      return new LookupResult(LookupStatus.UNAVAILABLE, null, detail);
    }
  }

  enum LookupStatus {
    UNIQUE,
    NOT_FOUND,
    AMBIGUOUS,
    UNAVAILABLE
  }

  record ProfileMappingRecord(
      String profileId,
      String characterId,
      String accountId,
      String bindingStatus,
      String lifecycleStatusSnapshot,
      int mappingSchemaVersion
  ) {}
}
