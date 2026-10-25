package pl.mcmmorpg.wiary;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;

/** Dane wiary jednej POSTACI (profil MMOProfiles): plik profile/<uuid-profilu>.yml. Pliki są malutkie. */
public final class Magazyn {
  /** Stan wiary postaci. patron == null: jeszcze nie wybrano. laskaDo w ms czasu rzeczywistego. */
  public static final class Dane {
    public String patron;
    public long zmiana;
    public long laskaDo;
    public int laskaWiara;
    public String laskaBostwo;
  }

  private final File dir;

  public Magazyn(File dataFolder) {
    this.dir = new File(dataFolder, "profile");
  }

  private File plik(UUID profil) { return new File(dir, profil + ".yml"); }

  public Dane wczytaj(UUID profil) {
    Dane d = new Dane();
    File f = plik(profil);
    if (!f.isFile()) return d;
    YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
    d.patron = y.getString("patron");
    d.zmiana = y.getLong("zmiana");
    d.laskaDo = y.getLong("laska.do");
    d.laskaWiara = y.getInt("laska.wiara");
    d.laskaBostwo = y.getString("laska.bostwo");
    return d;
  }

  public void zapisz(UUID profil, String gracz, Dane d) throws IOException {
    if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Nie mogę utworzyć " + dir);
    YamlConfiguration y = new YamlConfiguration();
    y.set("gracz", gracz);
    y.set("patron", d.patron);
    y.set("zmiana", d.zmiana);
    y.set("laska.do", d.laskaDo);
    y.set("laska.wiara", d.laskaWiara);
    y.set("laska.bostwo", d.laskaBostwo);
    File tmp = new File(dir, profil + ".yml.tmp");
    y.save(tmp);
    File cel = plik(profil);
    if (!tmp.renameTo(cel)) {
      // Na części systemów plików rename nie nadpisuje — wtedy kasujemy stary i próbujemy raz jeszcze.
      if (!cel.delete() || !tmp.renameTo(cel)) throw new IOException("Nie mogę zapisać " + cel);
    }
  }

  /**
   * Profile (pliki w profile/), które zapisano z danym nickiem (pole "gracz", bez względu na wielkość liter).
   * Dla graczy offline: plugin nie zna wtedy aktywnej postaci, więc zwraca wszystkie. Wartość = nick zapisany w pliku.
   */
  public Map<UUID, String> profileGracza(String nick) {
    Map<UUID, String> out = new LinkedHashMap<>();
    File[] pliki = dir.listFiles((d, n) -> n.endsWith(".yml"));
    if (pliki == null) return out;
    for (File f : pliki) {
      try {
        UUID id = UUID.fromString(f.getName().substring(0, f.getName().length() - 4));
        String g = YamlConfiguration.loadConfiguration(f).getString("gracz");
        if (g != null && g.toLowerCase(Locale.ROOT).equals(nick.toLowerCase(Locale.ROOT))) out.put(id, g);
      } catch (IllegalArgumentException ignored) { }
    }
    return out;
  }

  /** Usunięta postać: plik do archiwum, nie kasujemy danych na ślepo. */
  public void archiwizuj(UUID profil) {
    File f = plik(profil);
    if (!f.isFile()) return;
    File arch = new File(dir, "usuniete");
    if (arch.isDirectory() || arch.mkdirs()) {
      f.renameTo(new File(arch, profil + "-" + System.currentTimeMillis() + ".yml"));
    }
  }
}
