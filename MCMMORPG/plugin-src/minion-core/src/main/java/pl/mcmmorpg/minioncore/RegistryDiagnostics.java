package pl.mcmmorpg.minioncore;

/** Counts retained by the in-memory core, primarily for lifecycle assertions and later observability. */
public record RegistryDiagnostics(
    int trackedGroupCount,
    int activeGroupCount,
    int indexedEntityCount,
    int activeEntityCount,
    int reservedEntityCount,
    int ownerIndexCount) {
  public boolean isEmpty() {
    return trackedGroupCount == 0
        && activeGroupCount == 0
        && indexedEntityCount == 0
        && activeEntityCount == 0
        && reservedEntityCount == 0
        && ownerIndexCount == 0;
  }
}
