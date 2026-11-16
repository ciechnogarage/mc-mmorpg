package pl.mcmmorpg.chowance.model;

/** Dziesięć typów z SPEC §3. Kolejność = kolejność wierszy tabeli skuteczności. */
public enum Typ {
  OGIEN("Ogień", "<#E8662A>"),
  WODA("Woda", "<#3C8BE0>"),
  ZIEMIA("Ziemia", "<#A07A45>"),
  POWIETRZE("Powietrze", "<#9FD6E8>"),
  NATURA("Natura", "<#5DB04A>"),
  MROK("Mrok", "<#8A63C2>"),
  SWIATLO("Światło", "<#F2D55C>"),
  METAL("Metal", "<#9AA3AD>"),
  BESTIA("Bestia", "<#C98B5B>"),
  NIEUMARLY("Nieumarły", "<#7FA38A>");

  private final String nazwa;
  private final String kolor;

  Typ(String nazwa, String kolor) {
    this.nazwa = nazwa;
    this.kolor = kolor;
  }

  public String nazwa() { return nazwa; }
  public String kolor() { return kolor; }

  public static Typ parse(String s) {
    if (s == null) return null;
    try {
      return valueOf(s.trim().toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
