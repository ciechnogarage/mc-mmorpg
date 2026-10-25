package pl.mcmmorpg.napady;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.util.Vector;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/** Jeden napad: fazy, sterowanie manekinami, brama, lup, morale, zapis co N tikow. */
final class Napad {
  enum Faza { MARSZ, BRAMA, SZTURM, GRABIEZ, ODWROT, KONIEC }

  final Napady pl;
  final int id;
  final Arena arena;
  final List<Jednostka> jednostki = new ArrayList<>();
  final Random los = new Random();
  Faza faza = Faza.MARSZ;
  int tik, hpBramy, lup, napastnikowStart;
  /** ciosy graczy, ktore doszly do manekinow / zablokowane (do zapisu i testu) */
  int ciosyGraczy, ciosyGraczyBlok;
  TextDisplay napisBramy;
  String wynik;
  // pomiar
  long sumaNs, maxNs, tikiPomiaru;
  // zapis
  final StringBuilder zapis = new StringBuilder(1 << 16);

  Napad(Napady pl, int id, Arena arena) { this.pl = pl; this.id = id; this.arena = arena; }

  void start() {
    arena.bilety(pl, true);
    arena.zbuduj();
    arena.przelicz();
    var c = pl.getConfig();
    hpBramy = c.getInt("hp-bramy", 300);
    int nid = 0;
    for (int i = 0; i < c.getInt("atak.wojownicy", 8); i++)
      dodaj(++nid, Strona.ATAK, Jednostka.Rola.WOJOWNIK, 2 + i % 3, 16 + (i * 7) % 9, false);
    for (int i = 0; i < c.getInt("atak.lucznicy", 2); i++)
      dodaj(++nid, Strona.ATAK, Jednostka.Rola.LUCZNIK, 0, 18 + i * 4, false);
    int ob = c.getInt("obrona.wojownicy", 4);
    for (int i = 0; i < ob; i++)
      dodaj(++nid, Strona.OBRONA, Jednostka.Rola.WOJOWNIK, i < ob / 2 ? 23 : 31, 18 + (i % 2) * 4, false);
    for (int i = 0; i < c.getInt("obrona.lucznicy", 2); i++)
      dodaj(++nid, Strona.OBRONA, Jednostka.Rola.LUCZNIK, Arena.BRAMA_X, i == 0 ? 13 : 27, true);
    napastnikowStart = (int) jednostki.stream().filter(j -> j.strona == Strona.ATAK).count();
    Location ln = arena.loc(Arena.BRAMA_X - 1, Arena.BRAMA_Z0 + 1).add(0, 3.6, 0);
    napisBramy = arena.swiat.spawn(ln, TextDisplay.class, t -> {
      t.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
      t.setPersistent(false);
    });
    odswiezNapis();
    zapiszNaglowek();
    pl.getLogger().info("Napad " + id + " start, arena x=" + arena.ox + ", jednostek " + jednostki.size());
  }

  private void dodaj(int nid, Strona s, Jednostka.Rola r, double x, double z, boolean naMurze) {
    Location l = arena.loc(x, z);
    if (naMurze) l.setY(Arena.Y + Arena.MUR_H);
    l.setYaw(s == Strona.ATAK ? -90 : 90);
    var skora = pl.skory.get(los.nextInt(pl.skory.size()));
    Mannequin m = arena.swiat.spawn(l, Mannequin.class, e -> {
      e.setPersistent(false);
      e.setProfile(ResolvableProfile.resolvableProfile().addProperty(skora).build());
      e.customName(Component.text(s == Strona.ATAK ? "Najeźdźca" : "Obrońca",
          s == Strona.ATAK ? NamedTextColor.RED : NamedTextColor.AQUA));
      // nazwa tylko przy celowaniu: przy 10+ jednostkach stale napisy zlewaly sie w jedna plame (klient QA 25.10)
      e.setCustomNameVisible(false);
      e.setDescription(Component.text(r == Jednostka.Rola.LUCZNIK ? "łucznik" : "wojownik", NamedTextColor.GRAY));
      e.setImmovable(false);
      var hp = e.getAttribute(Attribute.MAX_HEALTH);
      if (hp != null) hp.setBaseValue(pl.getConfig().getDouble("hp-jednostki", 20));
      e.setHealth(pl.getConfig().getDouble("hp-jednostki", 20));
      var eq = e.getEquipment();
      eq.setItemInMainHand(new ItemStack(r == Jednostka.Rola.LUCZNIK ? Material.BOW
          : s == Strona.ATAK ? Material.IRON_AXE : Material.IRON_SWORD));
      if (r == Jednostka.Rola.WOJOWNIK && s == Strona.OBRONA) eq.setItemInOffHand(new ItemStack(Material.SHIELD));
      ItemStack zbroja = new ItemStack(Material.LEATHER_CHESTPLATE);
      if (zbroja.getItemMeta() instanceof LeatherArmorMeta lm) { lm.setColor(s.kolor); zbroja.setItemMeta(lm); }
      eq.setChestplate(zbroja);
      eq.setHelmet(new ItemStack(s == Strona.ATAK ? Material.CHAINMAIL_HELMET : Material.IRON_HELMET));
    });
    jednostki.add(new Jednostka(nid, s, r, m, x, z, naMurze));
  }

  // ---------------------------------------------------------------- tik
  void tik() {
    if (faza == Faza.KONIEC) return;
    tik++;
    var c = pl.getConfig();
    if (tik % 10 == 0) wybierzCele();
    for (Jednostka j : jednostki) if (j.zywa()) steruj(j);
    if (tik % 20 == 0) {
      sprawdzFazy();
      if (tik > c.getInt("limit-czasu-s", 300) * 20) zakoncz("limit czasu");
    }
    if (tik % c.getInt("zapis-co-tikow", 5) == 0) zapiszKlatke();
  }

  private void wybierzCele() {
    List<LivingEntity> wszyscy = new ArrayList<>();
    for (Jednostka j : jednostki) if (j.zywa()) wszyscy.add(j.cialo);
    for (var e : pl.dolaczeni.entrySet())
      if (e.getValue().napad() == id) {
        Player p = Bukkit.getPlayer(e.getKey());
        if (p != null && !p.isDead() && p.getWorld() == arena.swiat) wszyscy.add(p);
      }
    for (Jednostka j : jednostki) {
      if (!j.zywa()) continue;
      double zasieg = j.rola == Jednostka.Rola.LUCZNIK ? 22 : 9;
      LivingEntity best = null;
      double bd = zasieg * zasieg;
      for (LivingEntity e : wszyscy) {
        if (stronaEncji(e) != j.strona.wrog()) continue;
        double d = e.getLocation().distanceSquared(j.cialo.getLocation());
        if (d < bd) { bd = d; best = e; }
      }
      j.cel = best;
    }
  }

  Strona stronaEncji(Entity e) {
    if (e instanceof Player p) {
      var d = pl.dolaczeni.get(p.getUniqueId());
      return d != null && d.napad() == id ? d.strona() : null;
    }
    for (Jednostka j : jednostki) if (j.cialo == e) return j.strona;
    return null;
  }

  private void steruj(Jednostka j) {
    Location l = j.cialo.getLocation();
    double x = l.getX() - arena.ox, z = l.getZ() - arena.oz;
    LivingEntity cel = j.cel != null && j.cel.isValid() && !j.cel.isDead() ? j.cel : null;
    if (faza == Faza.ODWROT && j.strona == Strona.ATAK) cel = null;   // w odwrocie nie walcza, uciekaja

    if (j.rola == Jednostka.Rola.LUCZNIK) {
      if (cel != null) {
        patrz(j, cel.getLocation());
        stoj(j);
        if (tik >= j.gotowyOd) strzel(j, cel);
        j.akcja = "strzela";
        return;
      }
      if (j.strona == Strona.ATAK && !j.naMurze) { idzPolem(j, x, z, celPolaNapastnika(), true); return; }
      stoj(j);
      j.akcja = "stoi";
      return;
    }

    if (cel != null) {
      double d = cel.getLocation().distance(l);
      if (d < 2.3) {
        patrz(j, cel.getLocation());
        stoj(j);
        if (tik >= j.gotowyOd) uderz(j, cel);
        return;
      }
      double tx = cel.getLocation().getX() - arena.ox, tz = cel.getLocation().getZ() - arena.oz;
      if (arena.wolnaLinia(x, z, tx, tz)) { idz(j, tx, tz); j.akcja = "szarzuje"; return; }
    }

    if (j.strona == Strona.OBRONA) {
      if (Math.hypot(j.postX + 0.5 - x, j.postZ + 0.5 - z) > 1.2 && arena.wolnaLinia(x, z, j.postX, j.postZ)) {
        idz(j, j.postX + 0.5, j.postZ + 0.5);
        j.akcja = "wraca";
      } else { stoj(j); j.akcja = "stoi"; }
      return;
    }

    // napastnik bez celu
    if (faza == Faza.MARSZ || faza == Faza.BRAMA) {
      double gx = Arena.BRAMA_X - 0.5, gz = Arena.BRAMA_Z0 + 1.5;
      if (Math.hypot(gx - x, gz - z) < 2.6) {
        patrz(j, arena.loc(Arena.BRAMA_X, Arena.BRAMA_Z0 + 1).add(0, 1, 0));
        stoj(j);
        if (tik >= j.gotowyOd) uderzBrame(j);
        return;
      }
    }
    if (faza == Faza.SZTURM || faza == Faza.GRABIEZ) {
      if (Math.hypot(Arena.MAG_X0 - 0.5 - x, Arena.DRZWI_Z + 0.5 - z) < 1.8) {
        stoj(j);
        j.akcja = "grabi";
        return;
      }
    }
    idzPolem(j, x, z, celPolaNapastnika(), false);
  }

  private int[] celPolaNapastnika() {
    return switch (faza) {
      case MARSZ, BRAMA -> arena.doBramy;
      case SZTURM, GRABIEZ -> arena.doMagazynu;
      default -> arena.doOdwrotu;
    };
  }

  private void idzPolem(Jednostka j, double x, double z, int[] pole, boolean lucznik) {
    double[] k = arena.krok(pole, x, z);
    if (k == null) { stoj(j); j.akcja = faza == Faza.ODWROT ? "uciekl" : "czeka"; return; }
    idz(j, k[0], k[1]);
    j.akcja = faza == Faza.ODWROT ? "ucieka" : "maszeruje";
  }

  private void idz(Jednostka j, double tx, double tz) {
    Location l = j.cialo.getLocation();
    double dx = tx - (l.getX() - arena.ox), dz = tz - (l.getZ() - arena.oz);
    double len = Math.hypot(dx, dz);
    if (len < 0.05) { stoj(j); return; }
    double v = pl.getConfig().getDouble("predkosc", 0.2);
    // rozpychanie od swoich, zeby nie stali w jednym punkcie
    double sx = 0, sz = 0;
    for (Jednostka o : jednostki) {
      if (o == j || !o.zywa() || o.strona != j.strona) continue;
      Location ol = o.cialo.getLocation();
      double ex = l.getX() - ol.getX(), ez = l.getZ() - ol.getZ(), d2 = ex * ex + ez * ez;
      if (d2 < 1.21 && d2 > 1e-4) { double d = Math.sqrt(d2); sx += ex / d * (1.1 - d); sz += ez / d * (1.1 - d); }
    }
    double mx = dx / len * Math.min(v, len) + sx * 0.5, mz = dz / len * Math.min(v, len) + sz * 0.5;
    float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
    if ("teleport".equals(pl.getConfig().getString("ruch"))) {
      Location n = l.clone().add(mx, 0, mz);
      n.setYaw(yaw);
      n.setPitch(0);
      j.cialo.teleport(n);
    } else {
      j.cialo.setRotation(yaw, 0);
      Vector cur = j.cialo.getVelocity();
      j.cialo.setVelocity(new Vector(mx, cur.getY(), mz));
    }
  }

  private void stoj(Jednostka j) {
    Vector cur = j.cialo.getVelocity();
    if (Math.abs(cur.getX()) + Math.abs(cur.getZ()) > 0.01 && !"teleport".equals(pl.getConfig().getString("ruch")))
      j.cialo.setVelocity(new Vector(0, cur.getY(), 0));
  }

  private void patrz(Jednostka j, Location na) {
    Location l = j.cialo.getEyeLocation();
    Vector d = na.toVector().add(new Vector(0, 1.2, 0)).subtract(l.toVector());
    float yaw = (float) Math.toDegrees(Math.atan2(-d.getX(), d.getZ()));
    float pitch = (float) -Math.toDegrees(Math.atan2(d.getY(), Math.hypot(d.getX(), d.getZ())));
    j.cialo.setRotation(yaw, pitch);
  }

  private int odnowienie() { return 18 + los.nextInt(8); }

  private void uderz(Jednostka j, LivingEntity cel) {
    j.cialo.swingMainHand();
    cel.damage(pl.getConfig().getDouble("obrazenia", 3), j.cialo);
    j.gotowyOd = tik + odnowienie();
    j.akcja = "uderza";
    if (cel.isDead() || cel.getHealth() <= 0) zdarzenie("smierc", idEncji(cel) + "<" + j.id);
  }

  private void uderzBrame(Jednostka j) {
    j.cialo.swingMainHand();
    hpBramy -= (int) pl.getConfig().getDouble("obrazenia", 3);
    j.gotowyOd = tik + odnowienie();
    j.akcja = "bije brame";
    if (faza == Faza.MARSZ) { faza = Faza.BRAMA; zdarzenie("faza", faza.name()); }
    if (tik % 4 == 0) arena.swiat.playSound(arena.loc(Arena.BRAMA_X, Arena.BRAMA_Z0 + 1), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.7f, 1f);
    if (hpBramy <= 0) {
      hpBramy = 0;
      arena.zburzBrame();
      arena.przelicz();
      arena.swiat.playSound(arena.loc(Arena.BRAMA_X, Arena.BRAMA_Z0 + 1), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.5f, 0.8f);
      faza = Faza.SZTURM;
      zdarzenie("brama", "zburzona");
      zdarzenie("faza", faza.name());
    }
    odswiezNapis();
  }

  private void strzel(Jednostka j, LivingEntity cel) {
    Location oko = j.cialo.getEyeLocation();
    Vector d = cel.getLocation().add(0, 1.0, 0).toVector().subtract(oko.toVector());
    double dist = d.length();
    d.normalize().multiply(1.6).add(new Vector(0, dist * 0.012, 0));
    Arrow a = j.cialo.launchProjectile(Arrow.class, d);
    a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
    a.setDamage(1.2);
    a.setPersistent(false);
    j.gotowyOd = tik + 36 + los.nextInt(10);
    zdarzenie("strzal", j.id + ">" + idEncji(cel));
  }

  String idJednostki(Entity e) { return idEncji(e); }

  private String idEncji(Entity e) {
    for (Jednostka j : jednostki) if (j.cialo == e) return String.valueOf(j.id);
    return e instanceof Player p ? p.getName() : "?";
  }

  private void sprawdzFazy() {
    long atakZywi = jednostki.stream().filter(j -> j.strona == Strona.ATAK && j.zywa()).count();
    long obronaZywi = jednostki.stream().filter(j -> j.strona == Strona.OBRONA && j.zywa()).count();
    if (faza == Faza.SZTURM || faza == Faza.GRABIEZ) {
      long grabia = jednostki.stream().filter(j -> j.zywa() && "grabi".equals(j.akcja)).count();
      boolean obronaPrzy = jednostki.stream().anyMatch(j -> j.strona == Strona.OBRONA && j.zywa()
          && j.cialo.getLocation().distance(arena.loc(Arena.MAG_X0, Arena.DRZWI_Z)) < 4);
      if (grabia > 0 && !obronaPrzy) {
        if (faza == Faza.SZTURM) { faza = Faza.GRABIEZ; zdarzenie("faza", faza.name()); }
        lup = (int) Math.min(pl.getConfig().getInt("lup-max", 100), lup + grabia * 2);
        if (lup >= pl.getConfig().getInt("lup-max", 100)) { wynik = "napastnicy zlupili magazyn"; odwrot(); }
      }
    }
    if (faza != Faza.ODWROT && atakZywi <= napastnikowStart * (1 - pl.getConfig().getDouble("morale-odwrot", 0.7))) {
      wynik = "napastnicy zalamani (" + atakZywi + "/" + napastnikowStart + ")";
      odwrot();
    }
    if (atakZywi == 0) zakoncz(wynik != null ? wynik : "napastnicy wybici");
    else if (faza == Faza.ODWROT && jednostki.stream().filter(j -> j.strona == Strona.ATAK && j.zywa())
        .allMatch(j -> "uciekl".equals(j.akcja))) zakoncz(wynik + ", odwrot zakonczony");
    else if (obronaZywi == 0 && faza != Faza.ODWROT && faza.ordinal() >= Faza.SZTURM.ordinal() && wynik == null)
      zdarzenie("obrona", "wybita");
  }

  private void odwrot() {
    if (faza == Faza.ODWROT) return;
    faza = Faza.ODWROT;
    zdarzenie("faza", faza.name() + ":" + wynik);
  }

  private void odswiezNapis() {
    if (napisBramy == null || !napisBramy.isValid()) return;
    int max = pl.getConfig().getInt("hp-bramy", 300);
    napisBramy.text(hpBramy > 0
        ? Component.text("Brama " + (100 * hpBramy / max) + "%", NamedTextColor.GOLD)
        : Component.text("Brama zburzona", NamedTextColor.RED));
  }

  Location punktDolaczenia(Strona s) {
    Location l = s == Strona.ATAK ? arena.loc(4, 20) : arena.loc(30, 26);
    l.setYaw(s == Strona.ATAK ? -90 : 90);
    return l;
  }

  // ---------------------------------------------------------------- koniec
  boolean skonczony() { return faza == Faza.KONIEC; }

  void zakoncz(String powod) {
    if (faza == Faza.KONIEC) return;
    faza = Faza.KONIEC;
    zdarzenie("koniec", powod);
    for (Jednostka j : jednostki) if (j.cialo.isValid()) j.cialo.remove();
    if (napisBramy != null) napisBramy.remove();
    arena.postawBrame();
    arena.bilety(pl, false);
    pl.dolaczeni.values().removeIf(d -> d.napad() == id);
    String dane = zapis.toString();
    String opis = opis();
    File f = new File(pl.getDataFolder(), "zapisy/napad-" + id + "-" + System.currentTimeMillis() / 1000 + ".jsonl.gz");
    Bukkit.getScheduler().runTaskAsynchronously(pl, () -> {
      f.getParentFile().mkdirs();
      try (var out = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(f)), StandardCharsets.UTF_8)) {
        out.write(dane);
      } catch (IOException e) { pl.getLogger().warning("Zapis " + f + ": " + e); }
      pl.getLogger().info("Napad " + id + " koniec: " + powod + " | " + opis + " | zapis " + f.getName() + " "
          + f.length() + " B (surowo " + dane.length() + " zn.)");
    });
  }

  // ---------------------------------------------------------------- pomiar i zapis
  void pomiar(long ns) {
    sumaNs += ns;
    tikiPomiaru++;
    if (ns > maxNs) maxNs = ns;
  }

  String opis() {
    long a = jednostki.stream().filter(j -> j.strona == Strona.ATAK && j.zywa()).count();
    long o = jednostki.stream().filter(j -> j.strona == Strona.OBRONA && j.zywa()).count();
    return String.format(Locale.ROOT, "#%d %s t=%ds atak %d obrona %d brama %d lup %d gracze ciosy %d/%d | sr %.0f us max %.0f us",
        id, faza, tik / 20, a, o, hpBramy, lup, ciosyGraczy, ciosyGraczy + ciosyGraczyBlok, tikiPomiaru == 0 ? 0.0 : sumaNs / 1000.0 / tikiPomiaru, maxNs / 1000.0);
  }

  private void zapiszNaglowek() {
    zapis.append("{\"napad\":").append(id).append(",\"arena\":[").append(arena.ox).append(',').append(arena.oz)
        .append("],\"jednostki\":[");
    for (int i = 0; i < jednostki.size(); i++) {
      Jednostka j = jednostki.get(i);
      if (i > 0) zapis.append(',');
      zapis.append("{\"id\":").append(j.id).append(",\"s\":\"").append(j.strona.name()).append("\",\"r\":\"")
          .append(j.rola.name()).append("\"}");
    }
    zapis.append("]}\n");
  }

  private void zapiszKlatke() {
    zapis.append("{\"t\":").append(tik).append(",\"f\":\"").append(faza.name()).append("\",\"b\":").append(hpBramy)
        .append(",\"l\":").append(lup).append(",\"u\":[");
    boolean pierwszy = true;
    for (Jednostka j : jednostki) {
      if (!j.zywa()) continue;
      Location l = j.cialo.getLocation();
      if (!pierwszy) zapis.append(',');
      pierwszy = false;
      zapis.append('[').append(j.id).append(',').append(r1(l.getX() - arena.ox)).append(',').append(r1(l.getY()))
          .append(',').append(r1(l.getZ() - arena.oz)).append(',').append(Math.round(l.getYaw())).append(',')
          .append(Math.round(j.cialo.getHealth())).append(",\"").append(j.akcja).append("\"]");
    }
    for (var e : pl.dolaczeni.entrySet())
      if (e.getValue().napad() == id) {
        Player p = Bukkit.getPlayer(e.getKey());
        if (p == null) continue;
        Location l = p.getLocation();
        zapis.append(pierwszy ? "" : ",").append("[\"").append(p.getName()).append("\",").append(r1(l.getX() - arena.ox))
            .append(',').append(r1(l.getY())).append(',').append(r1(l.getZ() - arena.oz)).append(',')
            .append(Math.round(l.getYaw())).append(',').append(Math.round(p.getHealth())).append(",\"gracz\"]");
        pierwszy = false;
      }
    zapis.append("]}\n");
  }

  void zdarzenie(String typ, String dane) {
    zapis.append("{\"t\":").append(tik).append(",\"e\":\"").append(typ).append("\",\"d\":\"")
        .append(dane.replace("\"", "'")).append("\"}\n");
  }

  private static String r1(double v) { return String.valueOf(Math.round(v * 10) / 10.0); }
}
