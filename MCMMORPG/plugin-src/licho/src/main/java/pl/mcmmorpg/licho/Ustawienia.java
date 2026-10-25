package pl.mcmmorpg.licho;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** config.yml w polach; czasy przeliczone na ticki. */
final class Ustawienia {
  final String swiat, nazwaMapy;
  final List<String> swiatyAwaryjne, swiatyWejscia;
  final int min, max, naZamawiacza;
  final int poczekalnia, poczekalniaZaGracza, poczekalniaMax, rozproszenie, lowy, podsumowanie;
  final int iskry, zastygniecie, wcielenieCo, psotaCo, szeptCo, szeptCoKoniec;
  final double zasiegWcielenia, psotaSluch, szeptZasieg;
  final int tchu, tchuCo, zadyszka, ciosCo, okadzanieCo;
  final double zasiegCiosu, okadzaniePromien;
  final int ptLicho, ptPsota, ptPrzetrwanie, ptTrafienie, ptZlapanie;
  final int[] obszar;
  private final double[] poczekalniaPkt, bramaPkt, komoraPkt;
  final Map<Material, String> przebrania = new LinkedHashMap<>();
  record Prog(int ile, String nazwa) {}
  final int tytulyMinGraczy;
  final List<Prog> tytulyLicho, tytulyZamawiacz;

  Ustawienia(FileConfiguration c) {
    swiat = c.getString("swiat", "licho");
    swiatyAwaryjne = c.getStringList("swiaty-awaryjne");
    swiatyWejscia = c.getStringList("swiaty-wejscia");
    min = c.getInt("gracze.min", 2);
    max = c.getInt("gracze.max", 12);
    naZamawiacza = Math.max(2, c.getInt("gracze.na-zamawiacza", 4));
    poczekalnia = s(c, "czasy.poczekalnia", 25);
    poczekalniaZaGracza = s(c, "czasy.poczekalnia-za-gracza", 5);
    poczekalniaMax = s(c, "czasy.poczekalnia-max", 45);
    rozproszenie = s(c, "czasy.rozproszenie", 30);
    lowy = s(c, "czasy.lowy", 180);
    podsumowanie = s(c, "czasy.podsumowanie", 8);
    iskry = c.getInt("licho.iskry", 3);
    zastygniecie = s(c, "licho.zastygniecie", 1.5);
    zasiegWcielenia = c.getDouble("licho.zasieg-wcielenia", 5);
    wcielenieCo = s(c, "licho.wcielenie-co", 20);
    psotaCo = s(c, "licho.psota-co", 15);
    psotaSluch = c.getDouble("licho.psota-sluch", 12);
    szeptCo = s(c, "licho.szept-co", 40);
    szeptCoKoniec = s(c, "licho.szept-co-koniec", 15);
    szeptZasieg = c.getDouble("licho.szept-zasieg", 12);
    tchu = c.getInt("zamawiacz.tchu", 6);
    tchuCo = s(c, "zamawiacz.tchu-co", 10);
    zadyszka = s(c, "zamawiacz.zadyszka", 4);
    ciosCo = s(c, "zamawiacz.cios-co", 0.5);
    zasiegCiosu = c.getDouble("zamawiacz.zasieg-ciosu", 4.5);
    okadzanieCo = s(c, "zamawiacz.okadzanie-co", 25);
    okadzaniePromien = c.getDouble("zamawiacz.okadzanie-promien", 5);
    ptLicho = c.getInt("punkty.licho-co-10s", 1);
    ptPsota = c.getInt("punkty.psota", 5);
    ptPrzetrwanie = c.getInt("punkty.przetrwanie", 15);
    ptTrafienie = c.getInt("punkty.trafienie", 3);
    ptZlapanie = c.getInt("punkty.zlapanie", 10);
    nazwaMapy = c.getString("mapa.nazwa", "Zagroda Zielarki");
    List<Integer> o = c.getIntegerList("mapa.obszar");
    obszar = o.size() == 6 ? new int[]{o.get(0), o.get(1), o.get(2), o.get(3), o.get(4), o.get(5)} : new int[]{-23, 60, -21, 23, 70, 21};
    poczekalniaPkt = pkt(c, "mapa.poczekalnia");
    bramaPkt = pkt(c, "mapa.brama");
    komoraPkt = pkt(c, "mapa.komora");
    ConfigurationSection p = c.getConfigurationSection("przebrania");
    if (p != null) for (String k : p.getKeys(false)) {
      Material m = Material.matchMaterial(k.toUpperCase(Locale.ROOT));
      if (m != null && m.isBlock()) przebrania.put(m, p.getString(k));
    }
    tytulyMinGraczy = c.getInt("tytuly.min-graczy", 4);
    tytulyLicho = progi(c, "tytuly.licho");
    tytulyZamawiacz = progi(c, "tytuly.zamawiacz");
  }

  /** {ile: nazwa} -> rosnąco po progu. */
  private static List<Prog> progi(FileConfiguration c, String klucz) {
    List<Prog> l = new ArrayList<>();
    ConfigurationSection s = c.getConfigurationSection(klucz);
    if (s != null) for (String k : s.getKeys(false)) {
      try { l.add(new Prog(Integer.parseInt(k), s.getString(k))); } catch (NumberFormatException ignored) { }
    }
    l.sort(Comparator.comparingInt(Prog::ile));
    return l;
  }

  private static int s(FileConfiguration c, String klucz, double domyslne) { return (int) Math.round(c.getDouble(klucz, domyslne) * 20); }
  private static double[] pkt(FileConfiguration c, String klucz) {
    List<Double> l = c.getDoubleList(klucz);
    return l.size() >= 3 ? new double[]{l.get(0), l.get(1), l.get(2), l.size() > 3 ? l.get(3) : 0} : new double[]{0.5, 64, 0.5, 0};
  }
  private static Location lok(World w, double[] p) { return new Location(w, p[0], p[1], p[2], (float) p[3], 0f); }
  Location poczekalnia(World w) { return lok(w, poczekalniaPkt); }
  Location brama(World w) { return lok(w, bramaPkt); }
  Location komora(World w) { return lok(w, komoraPkt); }

  boolean wObszarze(Location l) {
    return l.getX() >= obszar[0] && l.getX() <= obszar[3] + 1 && l.getY() >= obszar[1] && l.getY() <= obszar[4] + 1
        && l.getZ() >= obszar[2] && l.getZ() <= obszar[5] + 1;
  }
}
