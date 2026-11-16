package pl.mcmmorpg.lobby;

/** Trzy miejsca na portal wokol placu. dir = kierunek od srodka wyspy do portalu. */
enum PortalSlot {
  POLNOC("polnoc", 0, -1, 13),
  WSCHOD("wschod", 1, 0, 15),
  ZACHOD("zachod", -1, 0, 11);

  final String key;
  final int dirX;
  final int dirZ;
  /** Miejsce w menu wyboru swiata. */
  final int menuSlot;

  PortalSlot(String key, int dirX, int dirZ, int menuSlot) {
    this.key = key;
    this.dirX = dirX;
    this.dirZ = dirZ;
    this.menuSlot = menuSlot;
  }

  /** a = w poprzek luku, n = w strone placu (n > 0 to strona, z ktorej sie podchodzi). */
  int x(int a, int n) {
    return dirX * IslandBuilder.PORTAL_DIST - dirZ * a - dirX * n;
  }

  int z(int a, int n) {
    return dirZ * IslandBuilder.PORTAL_DIST + dirX * a - dirZ * n;
  }
}
