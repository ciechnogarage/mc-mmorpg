package pl.mcmmorpg.petcore;

/** Result of registering PetCore's PlaceholderAPI expansion. */
enum PetCorePlaceholderRegistrationResult {
  REGISTERED(true, "PetCore PlaceholderAPI expansion registered."),
  REJECTED(false, "PetCore PlaceholderAPI expansion rejected by PlaceholderAPI.");

  private final boolean registered;
  private final String logMessage;

  PetCorePlaceholderRegistrationResult(boolean registered, String logMessage) {
    this.registered = registered;
    this.logMessage = logMessage;
  }

  static PetCorePlaceholderRegistrationResult from(boolean registered) {
    return registered ? REGISTERED : REJECTED;
  }

  boolean registered() {
    return registered;
  }

  String logMessage() {
    return logMessage;
  }
}
