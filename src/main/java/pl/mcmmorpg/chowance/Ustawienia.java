package pl.mcmmorpg.chowance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import pl.mcmmorpg.chowance.logika.WzorLapania;

/** config.yml sparsowany raz — reszta kodu nie czyta YAML-a w locie. */
public final class Ustawienia {
  public final WzorLapania.Parametry lapanie;
  public final int zbieglySek, szalSek;
  public final double predkoscRzutu;
  public final long odstepRzutuMs;
  public final boolean odniesienieAktywny;
  public final double szansaLsniacego;
  public final boolean ogloszenieLsniacego;
  public final boolean wybiegBezLupu;
  public final List<String> wybiegSpawneryPrefiksy;
  public final int oknoLegendarneS;
  public final double szansaLegendarnego;
  public final double zasiegOswajania;
  public final long odstepOswajaniaMs;
  public final TreeMap<Integer, Integer> limitDruzyny, limitMagazynu;
  public final int stronaMagazynu, stronaBestiariusza;
  public final double odlegloscPrzywolania, teleportZaGraczem, pveMnoznikNiechowance;
  public final boolean despawnZmianaSwiata, pveExp;
  public final List<String> swiatyZabronione;
  public final double regenNaMinute;
  public final boolean regenZemdlony;
  public final double premiaWiezi;
  public final int poziomMax, limitBazowy, zaOdznake;
  public final double expPve, expTrener;
  public final boolean expPojedynek;
  /** część exp dla reszty sprawnej drużyny (nie walczyła) — recenzja R-04 */
  public final double expDzielony;
  public final int karmienieOdstepS, wiezMax;
  public final int hodowlaCzasMin, genyDziedziczone, jajaMax, probkowanieS;
  public final double mnoznikLsniacegoObcy;
  public final double walkaZasieg, odlegloscCial;
  /** Kamera areny (tryb widza przypięty do statywu) i tempo odtwarzania tury (ticki na krok). */
  public final boolean kamera, pojedynekAuto;
  /** Teren areny (2026-09-30): wybór miejsca, odsłanianie bloków u klienta, pusta dłoń, HUD w MythicHUD. */
  public final boolean arenaSzukaj, widokOdslaniaj, widokBezReki, hudMythic;
  public final double arenaZasieg;
  /** Pojedynek: cele AI MythicMobs ciała wyczyszczone (skill CHW_AI_POJEDYNEK), ruchem steruje tylko plugin. */
  public final boolean pojedynekBezAiMm;
  public final int odtwarzanieTicki;
  /** Real-time: mnożnik obrażeń (dłuższa, czytelna walka), odstęp między ruchami stwora, wstęp. */
  public final double pojedynekMnoznik;
  public final int pojedynekOdstepMs, pojedynekWstepMs;
  public final int czasTuryS, maxTur;
  public final List<String> arena;
  public final int pojedynekCzasS, zaproszenieS;
  public final double pojedynekZasieg;
  /**
   * Walka z dzikimi w otwartym świecie (2026-09-29, właściciel: „dziki mnie nie bije”, „moim petem łatwo go niszczę”).
   * Obrażenia dzikiego w gracza liczone jak w wirtualnego stwora na poziomie postaci i przeliczane na HP gracza;
   * obrażenia gracza w dzikiego skalowane; szansa, że trafiony przez chowańca dziki przerzuci się na chowańca.
   */
  public final double dzikiWGracza, graczWDzikiego, dzikiZmianaCelu;
  /** Real-time: zapowiedź ruchu (okno uniku), okno uniku, premia rozkazu, premia kontry po uniku. */
  public final int zapowiedzKontaktTicki, zapowiedzDystansTicki, unikOknoMs;
  public final double rozkazPremia, kontraPremia;
  public final int eloStart, eloK, rankingTop;
  public final int turniejMin, turniejCzasMeczuS;
  public final double handelZasieg;
  public final int handelZaproszenieS;
  public final int skanS;
  public final double skanZasieg;
  public final boolean starterAutoOkno;
  public final int starterOpoznienie;
  public final String profesjaId;
  public final double expZlapanie, expLsniacyMn, expWyklucie, expEwolucja, expOswojenie;
  public final String menuDm;
  public final String ikonaPustegoSlotu;
  public final boolean debug;
  public final boolean towarzyszWlaczony, pierwszeKrokiWlaczone;
  public final int glaskanieWiez, glaskanieOdstepS, glaskanieEfektOdstepS, reakcjaOdstepS;
  public final int bezruchS, ambientMinS, ambientMaxS, limitSygnalowNaS;
  public final double ambientZasieg, niskiHpProg, niskiHpReset;
  public final Map<String, String> spoczynekModele;
  public final List<List<String>> krokiNagrody;
  public final List<String> krokiOpisy, krokiWskazowki;
  public final String krokiCoDalej;

  public Ustawienia(FileConfiguration c) {
    lapanie = new WzorLapania.Parametry(
        c.getDouble("lapanie.status.SEN", 2.0), c.getDouble("lapanie.status.SPETANIE", 1.5),
        c.getDouble("lapanie.status.OSZOLOMIENIE", 2.0), c.getDouble("lapanie.poziom.wsp", 0.02),
        c.getDouble("lapanie.poziom.min", 0.4), c.getDouble("lapanie.poziom.max", 1.5),
        c.getDouble("lapanie.profesja.wsp", 0.01), c.getDouble("lapanie.profesja.max", 1.5));
    zbieglySek = c.getInt("lapanie.zbiegly_sekundy", 60);
    szalSek = c.getInt("lapanie.szal_sekundy", 10);
    predkoscRzutu = c.getDouble("lapanie.zasieg_rzutu", 1.4);
    odstepRzutuMs = c.getLong("lapanie.odstep_rzutu_ms", 1500);
    odniesienieAktywny = "AKTYWNY".equalsIgnoreCase(c.getString("lapanie.poziom_odniesienia", "DRUZYNA_MAX"));
    szansaLsniacego = c.getDouble("lsniace.szansa", 0.001);
    ogloszenieLsniacego = c.getBoolean("lsniace.ogloszenie", true);
    wybiegBezLupu = c.getBoolean("wybieg.bez_lupu", true);
    wybiegSpawneryPrefiksy = c.isList("wybieg.spawnery_prefiksy")
        ? List.copyOf(c.getStringList("wybieg.spawnery_prefiksy")) : List.of("chw_wybieg_");
    oknoLegendarneS = c.getInt("legendarne.okno_s", 60);
    szansaLegendarnego = c.getDouble("legendarne.szansa_w_oknie", 0.35);
    zasiegOswajania = c.getDouble("oswajanie.zasieg", 4.0);
    odstepOswajaniaMs = c.getLong("oswajanie.odstep_ms", 1000);
    limitDruzyny = progi(c.getConfigurationSection("druzyna.limit_druzyny"), Map.of(0, 6));
    limitMagazynu = progi(c.getConfigurationSection("druzyna.limit_magazynu"), Map.of(0, 1000));
    stronaMagazynu = Math.max(1, c.getInt("druzyna.strona_magazynu", 28));
    stronaBestiariusza = Math.max(1, c.getInt("druzyna.strona_bestiariusza", 28));
    odlegloscPrzywolania = c.getDouble("przywolanie.odleglosc", 2.5);
    despawnZmianaSwiata = c.getBoolean("przywolanie.despawn_przy_zmianie_swiata", true);
    teleportZaGraczem = c.getDouble("przywolanie.teleport_za_graczem_powyzej", 24);
    swiatyZabronione = new ArrayList<>(c.getStringList("przywolanie.swiaty_zabronione"));
    pveMnoznikNiechowance = c.getDouble("przywolanie.pve_mnoznik_obrazen_niechowance", 0.15);
    pveExp = c.getBoolean("przywolanie.pve_exp_za_zabicie", true);
    regenNaMinute = c.getDouble("regeneracja.procent_na_minute", 5.0);
    regenZemdlony = c.getBoolean("regeneracja.zemdlony_tez", true);
    premiaWiezi = c.getDouble("statystyki.premia_wiezi", 0.05);
    poziomMax = Math.min(100, c.getInt("poziomy.max", 100));
    limitBazowy = c.getInt("poziomy.limit_bazowy", 20);
    zaOdznake = c.getInt("poziomy.za_odznake", 20);
    expPve = c.getDouble("poziomy.exp_pve_mnoznik", 1.0);
    expTrener = c.getDouble("poziomy.exp_trener_mnoznik", 1.5);
    expPojedynek = c.getBoolean("poziomy.exp_pojedynek", false);
    expDzielony = Math.max(0, Math.min(1, c.getDouble("poziomy.exp_dzielony", 0.5)));
    karmienieOdstepS = c.getInt("karmienie.odstep_s", 30);
    wiezMax = Math.min(255, c.getInt("karmienie.wiez_max", 255));
    hodowlaCzasMin = c.getInt("hodowla.czas_min", 30);
    genyDziedziczone = c.getInt("hodowla.geny_dziedziczone", 3);
    mnoznikLsniacegoObcy = c.getDouble("hodowla.mnoznik_lsniacego_obcy", 4.0);
    jajaMax = c.getInt("hodowla.jaja_max", 3);
    probkowanieS = Math.max(1, c.getInt("hodowla.probkowanie_s", 5));
    walkaZasieg = c.getDouble("walka.zasieg", 24);
    czasTuryS = c.getInt("walka.czas_tury_s", 60);
    odlegloscCial = c.getDouble("walka.odleglosc_cial", 5.0);
    maxTur = c.getInt("walka.max_tur", 150);
    kamera = c.getBoolean("walka.kamera", true);
    arenaSzukaj = c.getBoolean("walka.arena_szukaj", true);
    arenaZasieg = c.getDouble("walka.arena_zasieg", 9.0);
    widokOdslaniaj = c.getBoolean("walka.widok_odslaniaj", true);
    widokBezReki = c.getBoolean("walka.widok_bez_reki", true);
    hudMythic = c.getBoolean("walka.hud_mythichud", true);
    pojedynekBezAiMm = c.getBoolean("pojedynek.bez_ai_mm", true);
    odtwarzanieTicki = Math.max(6, Math.min(60, c.getInt("walka.odtwarzanie_ticki", 24)));
    pojedynekAuto = c.getBoolean("pojedynek.auto", true);
    pojedynekMnoznik = Math.max(0.05, c.getDouble("pojedynek.mnoznik_obrazen", 0.4));
    pojedynekOdstepMs = Math.max(500, c.getInt("pojedynek.odstep_ruchow_ms", 2400));
    pojedynekWstepMs = Math.max(0, c.getInt("pojedynek.wstep_ms", 2500));
    arena = c.getStringList("pojedynek.arena");
    pojedynekCzasS = c.getInt("pojedynek.czas_s", 180);
    pojedynekZasieg = c.getDouble("pojedynek.zasieg", 30);
    zaproszenieS = c.getInt("pojedynek.zaproszenie_s", 60);
    zapowiedzKontaktTicki = Math.max(0, c.getInt("pojedynek.zapowiedz_kontakt_ticki", 12));
    zapowiedzDystansTicki = Math.max(0, c.getInt("pojedynek.zapowiedz_dystans_ticki", 10));
    unikOknoMs = Math.max(200, c.getInt("pojedynek.unik_okno_ms", 500));
    rozkazPremia = Math.max(1.0, c.getDouble("pojedynek.premia_rozkazu", 1.2));
    kontraPremia = Math.max(1.0, c.getDouble("pojedynek.premia_kontry", 1.3));
    dzikiWGracza = Math.max(0, c.getDouble("dzikie_walka.obrazenia_w_gracza", 0.6));
    graczWDzikiego = Math.max(0.01, c.getDouble("dzikie_walka.obrazenia_od_gracza", 0.4));
    dzikiZmianaCelu = Math.max(0, Math.min(1, c.getDouble("dzikie_walka.zmiana_celu_na_chowanca", 0.5)));
    eloStart = c.getInt("ranking.elo_start", 1000);
    eloK = c.getInt("ranking.k", 32);
    rankingTop = c.getInt("ranking.top", 10);
    turniejMin = c.getInt("turniej.min_graczy", 4);
    turniejCzasMeczuS = c.getInt("turniej.czas_na_mecz_s", 600);
    handelZasieg = c.getDouble("handel.zasieg", 12);
    handelZaproszenieS = c.getInt("handel.zaproszenie_s", 60);
    skanS = Math.max(1, c.getInt("bestiariusz.skan_s", 3));
    skanZasieg = c.getDouble("bestiariusz.skan_zasieg", 12);
    starterAutoOkno = c.getBoolean("starter.auto-okno", true);
    starterOpoznienie = c.getInt("starter.opoznienie_ticki", 60);
    profesjaId = c.getString("profesja.id", "poskramiacz");
    expZlapanie = c.getDouble("profesja.exp.zlapanie_bazowe", 20);
    expLsniacyMn = c.getDouble("profesja.exp.lsniacy_mnoznik", 5);
    expWyklucie = c.getDouble("profesja.exp.wyklucie", 40);
    expEwolucja = c.getDouble("profesja.exp.ewolucja", 60);
    expOswojenie = c.getDouble("profesja.exp.oswojenie", 30);
    menuDm = c.getString("menu.deluxemenus", "chowance_menazeria");
    ikonaPustegoSlotu = c.getString("menu.ikona_pustego_slotu", "hub_chw_magazyn");
    debug = c.getBoolean("debug", false);
    towarzyszWlaczony = c.getBoolean("towarzysz.wlaczony", true);
    pierwszeKrokiWlaczone = c.getBoolean("pierwsze_kroki.wlaczone", true);
    glaskanieWiez = Math.max(0, c.getInt("towarzysz.glaskanie.wiez", 1));
    glaskanieOdstepS = Math.max(1, c.getInt("towarzysz.glaskanie.odstep_s", 600));
    glaskanieEfektOdstepS = Math.max(1, c.getInt("towarzysz.glaskanie.efekt_odstep_s", 5));
    reakcjaOdstepS = Math.max(1, c.getInt("towarzysz.reakcja_odstep_s", 20));
    bezruchS = Math.max(1, c.getInt("towarzysz.bezruch_s", 8));
    ambientMinS = Math.max(1, c.getInt("towarzysz.ambient_min_s", 40));
    ambientMaxS = Math.max(ambientMinS, c.getInt("towarzysz.ambient_max_s", 90));
    ambientZasieg = Math.max(0, c.getDouble("towarzysz.ambient_zasieg", 8));
    limitSygnalowNaS = Math.max(0, c.getInt("towarzysz.limit_sygnalow_na_s", 5));
    niskiHpProg = Math.max(0, Math.min(1, c.getDouble("towarzysz.niski_hp_prog", 0.25)));
    niskiHpReset = Math.max(niskiHpProg, Math.min(1, c.getDouble("towarzysz.niski_hp_reset", 0.30)));
    Map<String, String> stany = new java.util.LinkedHashMap<>(Map.of("chw_grudnik", "zwiniecie", "chw_muszlik", "schowanie",
        "chw_glebiniec", "schowanie", "chw_skorupien", "schowanie"));
    ConfigurationSection st = c.getConfigurationSection("towarzysz.spoczynek_modele");
    if (st != null) for (String k : st.getKeys(false)) stany.put(k, st.getString(k, ""));
    spoczynekModele = Map.copyOf(stany);
    krokiNagrody = List.of(
        nagroda(c, 1, "mi give MATERIAL PROF_KARMA_MIESNA %gracz% 3"),
        nagroda(c, 2, "mi give MISCELLANEOUS PIECZEC_KAMIENNA %gracz% 3"),
        nagroda(c, 3, "mi give MISCELLANEOUS ELIKSIR_MALY %gracz% 2", "mi give MISCELLANEOUS PIECZEC_KAMIENNA %gracz% 2"),
        nagroda(c, 4, "mi give MISCELLANEOUS PIECZEC_KRYSZTALOWA %gracz% 1"));
    krokiOpisy = List.of(c.getString("pierwsze_kroki.opisy.krok1", "Przywołaj chowańca"),
        c.getString("pierwsze_kroki.opisy.krok2", "Pogłaszcz lub nakarm chowańca"),
        c.getString("pierwsze_kroki.opisy.krok3", "Wygraj walkę z trenerem"),
        c.getString("pierwsze_kroki.opisy.krok4", "Złap dzikiego chowańca"));
    krokiCoDalej = c.getString("pierwsze_kroki.co_dalej", "<gold>Co dalej? <gray>Pieczęcie i karmę wytworzysz w Warsztacie (Menażeria → Warsztat). "
        + "Na Wybiegu w Ogrodzie zawsze kręcą się łatwe dzikie. Lochy Chowańców są dla drużyn od poz. 15 — najpierw podszkol stwory.");
    krokiWskazowki = List.of(c.getString("pierwsze_kroki.wskazowki.krok1", "Klawisz Q → Chowańce → kliknij swojego chowańca (LPM), by go przywołać."),
        c.getString("pierwsze_kroki.wskazowki.krok2", "PPM pustą ręką na przywołanym chowańcu (albo nakarm go karmą)."),
        c.getString("pierwsze_kroki.wskazowki.krok3", "Strażniczka Ogrodu czeka przy furtce Ogrodu Chowańców (Stolica, północny wschód) — PPM, by walczyć."),
        c.getString("pierwsze_kroki.wskazowki.krok4", "Na Wybiegu w Ogrodzie Chowańców żyją łatwe dzikie — osłab jednego i rzuć pieczęcią (PPM)."));
  }

  private static List<String> nagroda(FileConfiguration c, int n, String... domyslne) {
    String p = "pierwsze_kroki.nagrody.krok" + n;
    return c.isList(p) ? List.copyOf(c.getStringList(p)) : List.of(domyslne);
  }

  private static TreeMap<Integer, Integer> progi(ConfigurationSection s, Map<Integer, Integer> def) {
    TreeMap<Integer, Integer> m = new TreeMap<>();
    if (s != null) for (String k : s.getKeys(false)) {
      try { m.put(Integer.parseInt(k), s.getInt(k)); } catch (NumberFormatException ignored) {}
    }
    if (m.isEmpty()) m.putAll(def);
    return m;
  }

  public static int prog(TreeMap<Integer, Integer> m, int poziom) {
    Map.Entry<Integer, Integer> e = m.floorEntry(poziom);
    return e == null ? m.firstEntry().getValue() : e.getValue();
  }

  public int limitPoziomu(int odznaki) {
    return Math.max(1, Math.min(poziomMax, limitBazowy + zaOdznake * odznaki));
  }
}
