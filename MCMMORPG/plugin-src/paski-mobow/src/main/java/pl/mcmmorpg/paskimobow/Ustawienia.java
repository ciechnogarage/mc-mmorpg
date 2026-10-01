package pl.mcmmorpg.paskimobow;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

record Ustawienia(double skala, double wysokosc, double zasieg, boolean liczbyHp, boolean ukryjNazwe,
                  boolean tylkoPoTrafieniu, Map<String, Wyglad.Rzadkosc> rzadkosci, Set<String> frakcje,
                  List<Pattern> wzorce, List<Pattern> pomin, boolean vanillaPotwory, int vanillaPoziom,
                  boolean poziomZMm, List<Wpis> moby) {

  record Wpis(Pattern wzorzec, Integer poziom, String rzadkosc) {}

  static final List<String> IKONY = List.of("klejnot", "korona", "gwiazda");

  static Ustawienia wczytaj(FileConfiguration c, Logger log) {
    Map<String, Wyglad.Rzadkosc> rz = new LinkedHashMap<>();
    ConfigurationSection sr = c.getConfigurationSection("rzadkosci");
    if (sr != null) for (String id : sr.getKeys(false)) {
      ConfigurationSection s = sr.getConfigurationSection(id);
      if (s == null) continue;
      TextColor kolor = TextColor.fromHexString(s.getString("kolor", "#ffffff"));
      if (kolor == null) { log.warning("rzadkosci." + id + ".kolor: zly hex, biore bialy"); kolor = TextColor.color(0xffffff); }
      rz.put(id, new Wyglad.Rzadkosc(id, s.getString("nazwa", id), kolor, IKONY.indexOf(s.getString("ikona", "brak")),
          s.getBoolean("pogrubiona", false)));
    }
    if (rz.isEmpty()) rz.put("pospolity", new Wyglad.Rzadkosc("pospolity", "Pospolity", TextColor.color(0xd8d8d8), -1, false));
    List<Wpis> moby = new ArrayList<>();
    ConfigurationSection sm = c.getConfigurationSection("moby");
    if (sm != null) for (String id : sm.getKeys(false)) {
      ConfigurationSection s = sm.getConfigurationSection(id);
      if (s == null) continue;
      String r = s.getString("rzadkosc");
      if (r != null && !rz.containsKey(r)) log.warning("moby." + id + ": nieznana rzadkosc '" + r + "'");
      moby.add(new Wpis(glob(id), s.contains("poziom") ? s.getInt("poziom") : null, r));
    }
    return new Ustawienia(c.getDouble("wyglad.skala", 0.7), c.getDouble("wyglad.wysokosc", 0.3),
        c.getDouble("wyglad.zasieg", 24), c.getBoolean("wyglad.liczby-hp", true),
        c.getBoolean("wyglad.ukryj-nazwe-vanilla", true), c.getBoolean("wyglad.tylko-po-trafieniu", false), rz,
        Set.copyOf(c.getStringList("frakcje")), c.getStringList("wzorce").stream().map(Ustawienia::glob).toList(),
        c.getStringList("pomin").stream().map(Ustawienia::glob).toList(), c.getBoolean("vanilla-potwory", true),
        c.getInt("vanilla-poziom", 1), c.getBoolean("poziom-z-mm", true), moby);
  }

  static Pattern glob(String g) {
    return Pattern.compile(java.util.Arrays.stream(g.split("\\*", -1)).map(Pattern::quote)
        .collect(java.util.stream.Collectors.joining(".*")), Pattern.CASE_INSENSITIVE);
  }

  static boolean pasuje(List<Pattern> wzorce, String id) {
    for (Pattern p : wzorce) if (p.matcher(id).matches()) return true;
    return false;
  }

  Wyglad.Rzadkosc rzadkosc(String id) {
    Wyglad.Rzadkosc r = id == null ? null : rzadkosci.get(id);
    return r != null ? r : rzadkosci.values().iterator().next();
  }
}
