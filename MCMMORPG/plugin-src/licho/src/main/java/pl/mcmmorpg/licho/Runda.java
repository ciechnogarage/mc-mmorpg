package pl.mcmmorpg.licho;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Jedna runda: poczekalnia → rozproszenie (Licha się chowają, Zamawiacze w Komorze) → łowy → podsumowanie.
 * Wszystko liczone w ticku serwera; encje (wyświetlacze, zaczepy) istnieją tylko od rozproszenia do końca rundy.
 */
final class Runda {
  enum Faza { POCZEKALNIA, ROZPROSZENIE, LOWY, KONIEC }
  /** Cel trafienia/wcielenia po id encji: Licho (zaczepa gracza) albo prawdziwy sprzęt (zaczepa-wabik). */
  private record Cel(Uczestnik licho, Block sprzet) {}

  static final MiniMessage MM = MiniMessage.miniMessage();
  static final String PREFIKS = "<dark_green>[Licho]</dark_green> ";

  private final LichoPlugin pl;
  private final Ustawienia u;
  final World w;
  final Map<java.util.UUID, Uczestnik> gracze = new LinkedHashMap<>();
  Faza faza = Faza.POCZEKALNIA;
  private int poczatekFazy = Bukkit.getCurrentTick(), koniecFazy = -1;
  private final Map<Integer, Cel> cele = new HashMap<>();
  private final List<Entity> encje = new ArrayList<>();
  private final Map<Block, Uczestnik> zastygle = new HashMap<>();
  private boolean lichaWygraly;

  Runda(LichoPlugin pl, World w) {
    this.pl = pl;
    this.u = pl.u;
    this.w = w;
  }

  int teraz() { return Bukkit.getCurrentTick(); }
  boolean przyjmuje() { return faza != Faza.KONIEC && gracze.size() < u.max; }
  long licha() { return gracze.values().stream().filter(Uczestnik::licho).count(); }
  long zamawiacze() { return gracze.size() - licha(); }

  // ------------------------------------------------------------------ skład
  /** Nowy gracz: w poczekalni czeka, w trakcie rundy dołącza jako Zamawiacz (Licha już się schowały). */
  void dolacz(Uczestnik x, Player p) {
    gracze.put(x.id, x);
    x.tchu = u.tchu;
    x.iskry = u.iskry;
    for (Uczestnik l : gracze.values()) if (l != x && l.licho() && faza != Faza.POCZEKALNIA) ukryjPrzed(l, p);
    switch (faza) {
      case POCZEKALNIA -> {
        x.rola = Uczestnik.Rola.LICHO;
        pl.teleport(p, u.poczekalnia(w));
        if (gracze.size() >= u.min) {
          int nowy = koniecFazy < 0 ? teraz() + u.poczekalnia
              : Math.min(koniecFazy + u.poczekalniaZaGracza, poczatekFazy + u.poczekalniaMax);
          if (koniecFazy < 0) poczatekFazy = teraz();
          koniecFazy = nowy;
        }
        tytul(p, "<dark_green>Licho nie śpi</dark_green>", "<gray>" + u.nazwaMapy + " · czekamy na graczy</gray>", Sound.BLOCK_WOODEN_DOOR_OPEN, 0.8f);
        p.sendMessage(MM.deserialize(PREFIKS + "<gray>Licho chowa się w sprzętach zagrody, Zamawiacze je wyganiają. "
            + "<white>Licho:</white> PPM w sprzęt = wcielenie, stój bez ruchu = zastygasz, <white>F</white> psota. "
            + "<white>Zamawiacz:</white> LPM = cios (pudło kosztuje tchu), <white>F</white> okadzanie. <white>Q</white> = menu i wyjście.</gray>"));
      }
      case ROZPROSZENIE -> { zostanZamawiaczem(x, p, false); doKomory(x, p); }
      case LOWY -> {
        zostanZamawiaczem(x, p, false);
        pl.teleport(p, u.brama(w));
        tytul(p, "<red>Zamawiacz</red>", "<gray>Łowy trwają — znajdź Licha!</gray>", Sound.ITEM_FIRECHARGE_USE, 1f);
      }
      case KONIEC -> {}
    }
  }

  /** Gracz wychodzi (menu, relog, zmiana postaci). Licho, które ucieka, przepada jak złapane, ale bez punktów dla nikogo. */
  void odejdz(Uczestnik x) {
    Player p = x.gracz();
    if (x.licho()) odslon(x);
    przywroc(x, p);
    gracze.remove(x.id);
    if (faza == Faza.ROZPROSZENIE || faza == Faza.LOWY) {
      if (gracze.size() < u.min) koniec(licha() > 0, "Za mało graczy — runda przerwana.");
      else if (licha() == 0) koniec(false, null);
      else if (zamawiacze() == 0) koniec(true, "Zamawiacze uciekli.");
    } else if (faza == Faza.POCZEKALNIA && gracze.size() < u.min) koniecFazy = -1;
  }

  /** Efekty i widoczność zdejmowane zawsze, gdy gracz przestaje grać (też po rundzie). */
  void przywroc(Uczestnik x, Player p) {
    if (p == null) return;
    for (PotionEffectType t : List.of(PotionEffectType.BLINDNESS, PotionEffectType.SLOWNESS, PotionEffectType.SPEED, PotionEffectType.INVISIBILITY)) p.removePotionEffect(t);
    for (Player o : Bukkit.getOnlinePlayers()) o.showPlayer(pl, p);
  }

  /** Nowy widz w świecie gry (dołączył, obserwator): nie może zobaczyć ukrytych Lich ani ich ciał. */
  void nowyWidz(Player v) {
    if (faza == Faza.POCZEKALNIA) return;
    for (Uczestnik l : gracze.values()) if (l.licho() && !l.id.equals(v.getUniqueId())) ukryjPrzed(l, v);
  }

  private void ukryjPrzed(Uczestnik l, Player v) {
    Player p = l.gracz();
    if (p != null) v.hidePlayer(pl, p);
    if (l.zastygly != null) {
      v.sendBlockChange(l.zastygly.getLocation(), l.ksztalt);
      if (l.wyglad != null) v.hideEntity(pl, l.wyglad);
    }
  }

  // ------------------------------------------------------------------ pętla
  void tick() {
    int t = teraz();
    switch (faza) {
      case POCZEKALNIA -> {
        if (koniecFazy >= 0 && t >= koniecFazy) rozproszenie();
        else if (t % 10 == 0) for (Uczestnik x : gracze.values()) {
          Player p = x.gracz();
          if (p == null) continue;
          if (koniecFazy < 0) pl.pasekBoss(p, "<dark_green>Licho nie śpi</dark_green> <gray>· czekamy na drugiego gracza (" + gracze.size() + "/" + u.max + ")</gray>", 0f);
          else pl.pasekBoss(p, "<dark_green>Licho nie śpi</dark_green> <gray>· graczy " + gracze.size() + "/" + u.max + " · start za </gray><white>"
              + sek(koniecFazy - t) + " s</white>", 1f - (float) (t - poczatekFazy) / Math.max(1, koniecFazy - poczatekFazy));
        }
      }
      case ROZPROSZENIE, LOWY -> {
        for (Uczestnik x : new ArrayList<>(gracze.values())) {
          Player p = x.gracz();
          if (p == null) continue;
          if (p.isFlying()) p.setFlying(false);   // latanie dozwolone tylko po to, by stanie na pozornym bloku nie kickowało
          if (x.licho()) tickLicha(x, p);
          else tickZamawiacza(x, p, t);
        }
        if (faza == Faza.LOWY && (t - poczatekFazy) % 200 == 0 && t > poczatekFazy)
          for (Uczestnik x : gracze.values()) if (x.licho()) x.punkty += u.ptLicho;
        if (t % 20 == 0) { pilnujObszaru(); hud(t); odswiezPozorne(); }
        if (t >= koniecFazy) {
          if (faza == Faza.ROZPROSZENIE) lowy();
          else koniec(true, null);
        }
      }
      case KONIEC -> {
        if (t >= koniecFazy) pl.zakonczRunde(this);
      }
    }
  }

  private void tickLicha(Uczestnik x, Player p) {
    Location l = p.getLocation();
    if (x.zastygly != null) {
      double dx = l.getX() - (x.zastygly.getX() + 0.5), dz = l.getZ() - (x.zastygly.getZ() + 0.5);
      if (dx * dx + dz * dz < 0.09 && Math.abs(l.getY() - x.zastygly.getY()) < 0.6) return;
      odmroz(x);
    }
    boolean ruch = x.ostatnia == null || x.ostatnia.getWorld() != l.getWorld() || x.ostatnia.distanceSquared(l) > 0.0004;
    x.ostatnia = l;
    if (ruch) {
      x.bezRuchu = 0;
      x.zlyGrunt = false;
      if (x.wyglad != null) x.wyglad.teleport(new Location(w, l.getX() - 0.5, l.getY(), l.getZ() - 0.5));
      if (x.zaczepa != null) x.zaczepa.teleport(new Location(w, l.getX(), l.getY() - 0.01, l.getZ()));
    } else if (++x.bezRuchu >= u.zastygniecie) zastygnij(x, p);
    if (faza == Faza.LOWY && teraz() >= x.szeptOd) szept(x, p);
  }

  private void tickZamawiacza(Uczestnik x, Player p, int t) {
    if (faza != Faza.LOWY) return;
    if (x.zadyszkaDo > 0 && t >= x.zadyszkaDo) { x.zadyszkaDo = 0; x.tchu = Math.max(x.tchu, 2); x.tchuOd = t; pasek(p, 30, "§aZłapałeś oddech."); }
    if (x.zadyszkaDo == 0 && x.tchu < u.tchu && t - x.tchuOd >= u.tchuCo) { x.tchu++; x.tchuOd = t; }
    if (x.tchu >= u.tchu) x.tchuOd = t;
  }

  // ------------------------------------------------------------------ fazy
  private void rozproszenie() {
    if (gracze.size() < u.min) { koniecFazy = -1; return; }
    List<Uczestnik> kolej = new ArrayList<>(gracze.values());
    Collections.shuffle(kolej);
    int ileZam = Math.max(1, (kolej.size() + u.naZamawiacza - 1) / u.naZamawiacza);
    faza = Faza.ROZPROSZENIE;
    poczatekFazy = teraz();
    koniecFazy = poczatekFazy + u.rozproszenie;
    postawZaczepy();
    List<Block> sprzety = pl.mapa.sprzety(w, u.przebrania.keySet());
    for (int i = 0; i < kolej.size(); i++) {
      Uczestnik x = kolej.get(i);
      Player p = x.gracz();
      if (p == null) continue;
      if (i < ileZam) { zostanZamawiaczem(x, p, false); doKomory(x, p); continue; }
      x.rola = Uczestnik.Rola.LICHO;
      x.bylLicho = true;
      x.iskry = u.iskry;
      BlockData start = sprzety.isEmpty() ? Material.BARREL.createBlockData() : sprzety.get(ThreadLocalRandom.current().nextInt(sprzety.size())).getBlockData();
      wciel(x, p, start);
      // inni i tak go nie widzą (hidePlayer); to dla niego samego: w F5 ciało sterczało ze sprzętu (24.10, klient QA)
      p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false, false));
      tytul(p, "<green>Jesteś Lichem</green>", "<gray>Chowaj się! PPM w sprzęt = wcielenie, stój = zastygasz</gray>", Sound.ENTITY_VEX_AMBIENT, 1.2f);
    }
    for (Uczestnik l : gracze.values()) if (l.licho()) for (Player v : w.getPlayers()) if (!v.getUniqueId().equals(l.id)) ukryjPrzed(l, v);
    ogloszenieWGrze("<gray>Rozproszenie! Lichów: <white>" + licha() + "</white>, Zamawiaczy: <white>" + zamawiacze() + "</white>. Łowy za " + sek(u.rozproszenie) + " s.</gray>");
  }

  private void lowy() {
    faza = Faza.LOWY;
    poczatekFazy = teraz();
    koniecFazy = poczatekFazy + u.lowy;
    for (Uczestnik x : gracze.values()) {
      Player p = x.gracz();
      if (p == null) continue;
      if (x.licho()) {
        x.szeptOd = teraz() + u.szeptCo / 2 + ThreadLocalRandom.current().nextInt(u.szeptCo / 2 + 1);
        tytul(p, "<red>Łowy!</red>", "<gray>Zamawiacze wyszli. Nie ruszaj się, gdy są blisko.</gray>", Sound.BLOCK_BELL_USE, 0.6f);
      } else {
        p.removePotionEffect(PotionEffectType.BLINDNESS);
        x.tchu = u.tchu;
        x.tchuOd = teraz();
        pl.teleport(p, u.brama(w));
        tytul(p, "<red>Łowy!</red>", "<gray>LPM w podejrzany sprzęt · F = okadzanie</gray>", Sound.BLOCK_BELL_USE, 0.6f);
      }
    }
  }

  void koniec(boolean licha, String powod) {
    if (faza == Faza.KONIEC) return;
    boolean bylyLowy = faza == Faza.LOWY || faza == Faza.ROZPROSZENIE;
    faza = Faza.KONIEC;
    lichaWygraly = licha;
    poczatekFazy = teraz();
    koniecFazy = poczatekFazy + u.podsumowanie;
    for (Uczestnik x : gracze.values()) {
      if (x.licho() && bylyLowy && powod == null) { x.przetrwal = true; x.punkty += u.ptPrzetrwanie; }
      if (x.licho()) odslon(x);
      Player p = x.gracz();
      if (p != null) przywroc(x, p);
    }
    usunEncje();
    if (!bylyLowy) return;
    // tytuł krótki: „Świt! Licha przetrwały” przy GUI x3 w 1280 px wychodził poza ekran (24.10, klient QA)
    String gora = licha ? "<green>Świt!</green>" : "<red>Zamówione!</red>";
    String wynik = licha ? "<green>Świt! Licha przetrwały</green>" : "<red>Wszystkie Licha zamówione</red>";
    String dol = powod != null ? "<gray>" + powod + "</gray>" : licha ? "<gray>Licha przetrwały · wygrywają Licha</gray>" : "<gray>Wszystkie Licha zamówione · wygrywają Zamawiacze</gray>";
    for (Uczestnik x : gracze.values()) {
      Player p = x.gracz();
      if (p == null) continue;
      tytul(p, gora, dol, licha ? Sound.ENTITY_VEX_CHARGE : Sound.BLOCK_BELL_RESONATE, 1f);
      pl.pasekBoss(p, wynik + " <gray>· powrót za " + sek(u.podsumowanie) + " s</gray>", 1f);
    }
    podsumowanie();
  }

  private void podsumowanie() {
    List<Uczestnik> ranking = new ArrayList<>(gracze.values());
    ranking.sort(Comparator.comparingInt((Uczestnik x) -> -x.punkty));
    StringBuilder sb = new StringBuilder(PREFIKS + "<gray>Podsumowanie rundy (" + u.nazwaMapy + "):</gray>");
    for (int i = 0; i < ranking.size(); i++) {
      Uczestnik x = ranking.get(i);
      String rola = x.przetrwal ? "<green>przetrwało</green>" : x.bylLicho ? "<gray>Licho, zamówione</gray>" : "<red>Zamawiacz</red>";
      sb.append("\n<gray>  ").append(i + 1).append(".</gray> <white>").append(MM.escapeTags(x.nick)).append("</white> <yellow>")
          .append(x.punkty).append(" pkt</yellow> ").append(rola);
      if (x.trafienia > 0 || x.zlapani > 0) sb.append(" <gray>(trafień ").append(x.trafienia).append(", złapanych ").append(x.zlapani).append(")</gray>");
    }
    Component c = MM.deserialize(sb.toString());
    for (Uczestnik x : gracze.values()) {
      Player p = x.gracz();
      if (p == null) continue;
      p.sendMessage(c);
      if (pl.rekordy.zapisz(x.id, x.nick, x.punkty, x.przetrwal))
        p.sendMessage(MM.deserialize(PREFIKS + "<gold>Nowy rekord osobisty: " + x.punkty + " pkt!</gold>"));
    }
    pl.rekordy.zapiszPlik();
    pl.getLogger().info("Runda: " + (lichaWygraly ? "Licha" : "Zamawiacze") + " wygrywają, graczy " + gracze.size());
  }

  // ------------------------------------------------------------------ Licho
  private void wciel(Uczestnik x, Player p, BlockData ksztalt) {
    x.ksztalt = ksztalt.clone();
    Location l = p.getLocation();
    if (x.wyglad == null || !x.wyglad.isValid()) {
      x.wyglad = w.spawn(new Location(w, l.getX() - 0.5, l.getY(), l.getZ() - 0.5), BlockDisplay.class, d -> {
        d.setBlock(x.ksztalt);
        d.setTeleportDuration(2);
        d.setPersistent(false);
        oznacz(d);
      });
      encje.add(x.wyglad);
    } else x.wyglad.setBlock(x.ksztalt);
    if (x.zaczepa == null || !x.zaczepa.isValid()) {
      x.zaczepa = zaczepa(new Location(w, l.getX(), l.getY() - 0.01, l.getZ()));
      cele.put(x.zaczepa.getEntityId(), new Cel(x, null));
    }
    if (x.zastygly != null) for (Player v : w.getPlayers()) if (!v.getUniqueId().equals(x.id)) v.sendBlockChange(x.zastygly.getLocation(), x.ksztalt);
  }

  private void zastygnij(Uczestnik x, Player p) {
    Location l = p.getLocation();
    Block b = w.getBlockAt(l.getBlockX(), (int) Math.floor(l.getY() + 0.2), l.getBlockZ());
    boolean ok = b.isPassable() && !b.isLiquid() && b.getRelative(BlockFace.DOWN).getType().isSolid() && !zastygle.containsKey(b)
        && w.getNearbyEntities(BoundingBox.of(b), e -> e instanceof Player o && !o.getUniqueId().equals(x.id)).isEmpty();
    if (!ok) {
      if (!x.zlyGrunt) { x.zlyGrunt = true; pasek(p, 30, "§7Tu nie zastygniesz — stań na ziemi, w wolnym miejscu."); }
      return;
    }
    x.zastygly = b;
    zastygle.put(b, x);
    Location srodek = new Location(w, b.getX() + 0.5, b.getY(), b.getZ() + 0.5, l.getYaw(), l.getPitch());
    pl.teleport(p, srodek);
    x.ostatnia = p.getLocation();
    x.wyglad.teleport(new Location(w, b.getX(), b.getY(), b.getZ()));
    x.zaczepa.teleport(new Location(w, b.getX() + 0.5, b.getY() - 0.01, b.getZ() + 0.5));
    for (Player v : w.getPlayers()) {
      if (v.getUniqueId().equals(x.id)) continue;
      v.sendBlockChange(b.getLocation(), x.ksztalt);
      v.hideEntity(pl, x.wyglad);
    }
    p.playSound(p.getLocation(), Sound.BLOCK_WOOD_PLACE, 0.4f, 0.8f);
    pasek(p, 20, "§aZastygłeś — dla innych jesteś prawdziwym sprzętem.");
  }

  private void odmroz(Uczestnik x) {
    Block b = x.zastygly;
    if (b == null) return;
    x.zastygly = null;
    zastygle.remove(b);
    for (Player v : w.getPlayers()) {
      if (v.getUniqueId().equals(x.id)) continue;
      v.sendBlockChange(b.getLocation(), b.getBlockData());
      if (x.wyglad != null) v.showEntity(pl, x.wyglad);
    }
    x.bezRuchu = 0;
  }

  /** Licho przestaje być Lichem (złapane, wyszło, koniec rundy): blok wraca, encje znikają, gracz znów widoczny. */
  private void odslon(Uczestnik x) {
    odmroz(x);
    if (x.zaczepa != null) { cele.remove(x.zaczepa.getEntityId()); x.zaczepa.remove(); x.zaczepa = null; }
    if (x.wyglad != null) { x.wyglad.remove(); x.wyglad = null; }
    Player p = x.gracz();
    if (p != null) { p.removePotionEffect(PotionEffectType.INVISIBILITY); for (Player o : Bukkit.getOnlinePlayers()) o.showPlayer(pl, p); }
  }

  private void szept(Uczestnik x, Player p) {
    boolean koncowka = koniecFazy - teraz() <= 600;
    x.szeptOd = teraz() + (koncowka ? u.szeptCoKoniec : u.szeptCo);
    w.playSound(p.getLocation(), Sound.ENTITY_VEX_AMBIENT, (float) (u.szeptZasieg / 16.0), 0.6f);
    pasek(p, 20, "§8Szepczesz… (słychać do " + (int) u.szeptZasieg + " bl.)");
  }

  private void psota(Uczestnik x, Player p) {
    if (faza != Faza.LOWY) { pasek(p, 30, "§7Psoty dopiero w łowach."); return; }
    int t = teraz();
    if (t < x.psotaDo) { pasek(p, 30, "§7Psota za " + sek(x.psotaDo - t) + " s."); return; }
    x.psotaDo = t + u.psotaCo;
    Location l = x.zastygly != null ? x.zastygly.getLocation().add(0.5, 0.5, 0.5) : p.getLocation().add(0, 0.5, 0);
    w.playSound(l, Sound.ENTITY_WITCH_CELEBRATE, 1.5f, 1.4f);
    w.spawnParticle(Particle.WITCH, l, 18, 0.35, 0.4, 0.35, 0.02);
    boolean ktosSlyszal = false;
    for (Uczestnik z : gracze.values()) {
      Player zp = z.gracz();
      if (!z.licho() && zp != null && zp.getLocation().distanceSquared(l) <= u.psotaSluch * u.psotaSluch) { ktosSlyszal = true; break; }
    }
    if (ktosSlyszal) { x.punkty += u.ptPsota; pasek(p, 30, "§d+" + u.ptPsota + " pkt za psotę!"); }
    else pasek(p, 30, "§7Nikt nie słyszał — psota bez punktów (Zamawiacz dalej niż " + (int) u.psotaSluch + " bl.).");
  }

  void ppm(Uczestnik x, Player p, int encja) {
    if (!x.licho() || (faza != Faza.ROZPROSZENIE && faza != Faza.LOWY)) return;
    Cel c = cele.get(encja);
    if (c == null || c.sprzet() == null) return;
    Location srodek = c.sprzet().getLocation().add(0.5, 0.5, 0.5);
    if (p.getEyeLocation().distance(srodek) > u.zasiegWcielenia + 0.5) { pasek(p, 30, "§7Za daleko."); return; }
    int t = teraz();
    if (faza == Faza.LOWY && t < x.wcielenieDo) { pasek(p, 30, "§7Wcielenie za " + sek(x.wcielenieDo - t) + " s."); return; }
    if (faza == Faza.LOWY) x.wcielenieDo = t + u.wcielenieCo;
    wciel(x, p, c.sprzet().getBlockData());
    p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 0.6f, 1.3f);
    pasek(p, 30, "§aJesteś teraz: §f" + u.przebrania.getOrDefault(x.ksztalt.getMaterial(), "sprzęt"));
  }

  // ------------------------------------------------------------------ Zamawiacz
  void cios(Uczestnik x, Player p, int encja) {
    if (x.licho() || faza != Faza.LOWY) return;
    int t = teraz();
    if (x.zadyszkaDo > 0) { pasek(p, 30, "§cZadyszka! Złap oddech."); return; }
    if (t < x.ciosDo) return;
    Cel c = cele.get(encja);
    if (c == null) return;
    Location cel = c.licho() != null ? (c.licho().zaczepa == null ? null : c.licho().zaczepa.getLocation().add(0, 0.5, 0))
        : c.sprzet().getLocation().add(0.5, 0.5, 0.5);
    if (cel == null || p.getEyeLocation().distance(cel) > u.zasiegCiosu + 0.8) return;
    x.ciosDo = t + u.ciosCo;
    p.swingMainHand();
    if (c.licho() != null && c.licho().licho()) trafienie(x, p, c.licho(), cel);
    else if (c.sprzet() != null) pudlo(x, p, cel);
  }

  private void trafienie(Uczestnik z, Player zp, Uczestnik l, Location gdzie) {
    Player lp = l.gracz();
    l.iskry--;
    z.punkty += u.ptTrafienie;
    z.trafienia++;
    w.playSound(gdzie, Sound.ENTITY_VEX_HURT, 1.2f, 1.1f);
    w.spawnParticle(Particle.ENCHANTED_HIT, gdzie, 14, 0.3, 0.3, 0.3, 0.1);
    if (l.iskry <= 0) { zlap(z, zp, l, lp); return; }
    odmroz(l);
    if (l.wyglad != null) {
      l.wyglad.setGlowing(true);
      BlockDisplay d = l.wyglad;
      Bukkit.getScheduler().runTaskLater(pl, () -> { if (d.isValid()) d.setGlowing(false); }, 20L);
    }
    pasek(zp, 30, "§a+" + u.ptTrafienie + " pkt — trafione Licho! Zostało mu iskier: " + l.iskry);
    if (lp != null) {
      lp.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false, false));
      lp.showTitle(Title.title(Component.empty(), MM.deserialize("<red>Trafiony!</red> <gray>Iskry: </gray>" + iskry(l.iskry)),
          Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ofMillis(250))));
    }
  }

  private void zlap(Uczestnik z, Player zp, Uczestnik l, Player lp) {
    z.punkty += u.ptZlapanie;
    z.zlapani++;
    odslon(l);
    ogloszenieWGrze("<red>" + MM.escapeTags(z.nick) + "</red> <gray>zamówił Licho</gray> <white>" + MM.escapeTags(l.nick) + "</white><gray>! Zostało Lichów: " + (licha() - 1) + ".</gray>");
    if (lp != null) {
      zostanZamawiaczem(l, lp, true);
      tytul(lp, "<red>Zamówiony!</red>", "<gray>Teraz łowisz razem z Zamawiaczami</gray>", Sound.ENTITY_VEX_DEATH, 1f);
    } else l.rola = Uczestnik.Rola.ZAMAWIACZ;
    pasek(zp, 30, "§a+" + u.ptZlapanie + " pkt — Licho zamówione!");
    if (licha() == 0) koniec(false, null);
  }

  private void pudlo(Uczestnik x, Player p, Location gdzie) {
    x.tchu--;
    w.spawnParticle(Particle.SMOKE, gdzie, 6, 0.25, 0.25, 0.25, 0.01);
    p.playSound(gdzie, Sound.ENTITY_PLAYER_ATTACK_WEAK, 0.8f, 0.7f);
    if (x.tchu <= 0) {
      x.tchu = 0;
      x.zadyszkaDo = teraz() + u.zadyszka;
      p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, u.zadyszka, 1, false, false, false));
      p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BREATH, 1f, 0.8f);
      pasek(p, 40, "§cZadyszka! " + sek(u.zadyszka) + " s bez ciosów.");
    } else pasek(p, 30, "§7Pudło — to prawdziwy sprzęt. Tchu: " + tchu(x.tchu));
  }

  private void okadzanie(Uczestnik x, Player p) {
    if (faza != Faza.LOWY) return;
    int t = teraz();
    if (t < x.okadzanieDo) { pasek(p, 30, "§7Okadzanie za " + sek(x.okadzanieDo - t) + " s."); return; }
    x.okadzanieDo = t + u.okadzanieCo;
    Location s = p.getLocation();
    w.playSound(s, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0.6f);
    for (int i = 0; i < 28; i++) {
      double a = i * Math.PI * 2 / 28, r = u.okadzaniePromien * ThreadLocalRandom.current().nextDouble(0.3, 1.0);
      w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, s.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r), 0, 0, 0.04, 0, 1);
    }
    int ile = 0;
    for (Uczestnik l : gracze.values()) {
      Player lp = l.gracz();
      if (!l.licho() || lp == null || lp.getLocation().distanceSquared(s) > u.okadzaniePromien * u.okadzaniePromien) continue;
      ile++;
      Bukkit.getScheduler().runTaskLater(pl, () -> {
        if (faza != Faza.LOWY || !l.licho() || !lp.isOnline()) return;
        Location g = l.zastygly != null ? l.zastygly.getLocation().add(0.5, 0.8, 0.5) : lp.getLocation().add(0, 0.8, 0);
        w.playSound(g, Sound.ENTITY_PANDA_SNEEZE, 1.2f, 1.3f);
        w.spawnParticle(Particle.SNEEZE, g, 10, 0.2, 0.2, 0.2, 0.02);
        pasek(lp, 40, "§eKichnąłeś od dymu!");
      }, 20L + ThreadLocalRandom.current().nextInt(30));
    }
    pasek(p, 30, "§7Okadzasz… " + (ile > 0 ? "coś się kręci w dymie." : "cisza."));
  }

  void klawiszF(Uczestnik x, Player p) {
    if (x.licho()) psota(x, p);
    else okadzanie(x, p);
  }

  private void zostanZamawiaczem(Uczestnik x, Player p, boolean zlapany) {
    x.rola = Uczestnik.Rola.ZAMAWIACZ;
    x.tchu = u.tchu;
    x.tchuOd = teraz();
    x.zadyszkaDo = 0;
    if (!zlapany) return;
    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false, false));
  }

  private void doKomory(Uczestnik x, Player p) {
    pl.teleport(p, u.komora(w));
    int zostalo = Math.max(20, koniecFazy - teraz());
    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, zostalo + 20, 0, false, false, false));
    tytul(p, "<red>Jesteś Zamawiaczem</red>", "<gray>Czekaj w Komorze — Licha się chowają</gray>", Sound.BLOCK_IRON_DOOR_CLOSE, 0.7f);
  }

  // ------------------------------------------------------------------ encje
  private Interaction zaczepa(Location l) {
    Interaction i = w.spawn(l, Interaction.class, e -> {
      e.setInteractionWidth(1.02f);
      e.setInteractionHeight(1.02f);
      e.setResponsive(false);
      e.setPersistent(false);
      oznacz(e);
    });
    encje.add(i);
    return i;
  }

  /** Taka sama zaczepa na każdym prawdziwym sprzęcie: hitboxy (F3+B) nie zdradzają Licha, a pudło da się policzyć. */
  private void postawZaczepy() {
    for (Block b : pl.mapa.sprzety(w, u.przebrania.keySet())) {
      Interaction i = zaczepa(new Location(w, b.getX() + 0.5, b.getY() - 0.01, b.getZ() + 0.5));
      cele.put(i.getEntityId(), new Cel(null, b));
    }
  }

  private void oznacz(Entity e) { e.getPersistentDataContainer().set(pl.klucz, PersistentDataType.BYTE, (byte) 1); }

  void usunEncje() {
    for (Uczestnik x : gracze.values()) if (x.zastygly != null) odmroz(x);
    for (Entity e : encje) e.remove();
    encje.clear();
    cele.clear();
  }

  int ileEncji() { return encje.size(); }

  /** Klient gubi pozorny blok przy ponownym wczytaniu chunka — co sekundę wysyłamy go jeszcze raz (≤12 widzów × kilka Lichów). */
  private void odswiezPozorne() {
    for (Uczestnik l : zastygle.values())
      for (Player v : w.getPlayers()) if (!v.getUniqueId().equals(l.id)) v.sendBlockChange(l.zastygly.getLocation(), l.ksztalt);
  }

  /** Test na klonie: skraca odliczanie poczekalni (komenda konsoli `licho start`). */
  boolean szybkiStart(int ticki) {
    if (faza != Faza.POCZEKALNIA || gracze.size() < u.min) return false;
    poczatekFazy = teraz();
    koniecFazy = teraz() + ticki;
    return true;
  }

  // ------------------------------------------------------------------ porządek
  private void pilnujObszaru() {
    for (Uczestnik x : gracze.values()) {
      Player p = x.gracz();
      if (p == null) continue;
      if (faza == Faza.ROZPROSZENIE && !x.licho()) continue;      // Zamawiacze w Komorze, pod mapą
      if (u.wObszarze(p.getLocation())) continue;
      if (x.licho()) odmroz(x);
      pl.teleport(p, faza == Faza.ROZPROSZENIE ? u.poczekalnia(w) : u.brama(w));
      pasek(p, 40, "§7Poza zagrodą — wracasz.");
    }
  }

  private void hud(int t) {
    int zostalo = Math.max(0, koniecFazy - t);
    float postep = (float) zostalo / Math.max(1, koniecFazy - poczatekFazy);
    String czas = zegar(zostalo);
    long licha = licha();
    for (Uczestnik x : gracze.values()) {
      Player p = x.gracz();
      if (p == null) continue;
      String tekst;
      if (faza == Faza.ROZPROSZENIE) tekst = x.licho()
          ? "<green>Chowaj się!</green> <gray>Zamawiacze wyjdą za</gray> <white>" + czas + "</white> <gray>· " + przebranie(x) + "</gray>"
          : "<red>Komora</red> <gray>· Licha się chowają · wychodzisz za</gray> <white>" + czas + "</white>";
      else tekst = x.licho()
          ? "<green>Licho</green> <gray>· iskry</gray> " + iskry(x.iskry) + " <gray>· " + x.punkty + " pkt · Lichów " + licha + " ·</gray> <white>" + czas + "</white>"
          : "<red>Zamawiacz</red> <gray>· tchu</gray> " + tchu(x.tchu) + " <gray>· " + x.punkty + " pkt · Lichów " + licha + " ·</gray> <white>" + czas + "</white>";
      pl.pasekBoss(p, tekst, postep);
    }
  }

  private String przebranie(Uczestnik x) { return x.ksztalt == null ? "" : u.przebrania.getOrDefault(x.ksztalt.getMaterial(), "sprzęt"); }

  // ------------------------------------------------------------------ komunikaty
  private void ogloszenieWGrze(String tekst) {
    Component c = MM.deserialize(PREFIKS + tekst);
    for (Uczestnik x : gracze.values()) { Player p = x.gracz(); if (p != null) p.sendMessage(c); }
  }

  static String iskry(int n) { return "<gold>" + "✦".repeat(Math.max(0, n)) + "</gold><dark_gray>" + "✦".repeat(Math.max(0, 3 - n)) + "</dark_gray>"; }
  private String tchu(int n) { return "<aqua>" + "●".repeat(Math.max(0, n)) + "</aqua><dark_gray>" + "●".repeat(Math.max(0, u.tchu - n)) + "</dark_gray>"; }
  static int sek(int ticki) { return (ticki + 19) / 20; }
  static String zegar(int ticki) { int s = sek(ticki); return s / 60 + ":" + String.format("%02d", s % 60); }

  static void pasek(Player p, int priorytet, String tekst) {
    if (p == null) return;
    if (MMOPlayerData.has(p)) MMOPlayerData.get(p).getActionBar().show(priorytet, 40, tekst);
    else p.sendActionBar(Component.text(tekst));
  }

  static void tytul(Player p, String gora, String dol, Sound dzwiek, float ton) {
    if (p == null) return;
    p.showTitle(Title.title(MM.deserialize(gora), MM.deserialize(dol), Title.Times.times(Duration.ZERO, Duration.ofMillis(1400), Duration.ofMillis(300))));
    if (dzwiek != null) p.playSound(p.getLocation(), dzwiek, 1f, ton);
  }

  String opis() {
    StringBuilder sb = new StringBuilder("faza=" + faza + " zostalo=" + sek(Math.max(0, koniecFazy - teraz())) + "s encje=" + encje.size() + " cele=" + cele.size()
        + " zastygle=" + zastygle.size());
    for (Uczestnik x : gracze.values())
      sb.append(String.format(java.util.Locale.ROOT, " | %s %s iskry=%d tchu=%d pkt=%d ksztalt=%s zastygly=%s", x.nick, x.rola, x.iskry, x.tchu, x.punkty,
          x.ksztalt == null ? "-" : x.ksztalt.getMaterial().name().toLowerCase(java.util.Locale.ROOT),
          x.zastygly == null ? "-" : x.zastygly.getX() + "," + x.zastygly.getY() + "," + x.zastygly.getZ()));
    return sb.toString();
  }
}
