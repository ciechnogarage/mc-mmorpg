package pl.mcmmorpg.rzemioslo;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Rybak: wlasna scena lowienia wedka profesji (wedka vanilla — stare zachowanie w Lowienie).
 *  Zarzut = vanilla. Spławik w wodzie lowiska: vanilla nigdy nie bierze (czas oczekiwania ~godzina), plugin prowadzi
 *  scene: drobnica skubie, gatunek zdradza sie ruchem spławika (zapowiedz >= 1 s), zaciecie PPM w oknie gatunku
 *  (>= 700 ms), hol: Shift = zwijanie, ryba rwie w bok (zapowiedz >= 1 s) — kamera w przeciwna strone = kontra,
 *  trzeszczenie = popusc (pusc Shift), zerwanie albo zgubienie = 0. Wyskok ryby = ItemDisplay widoczny dla wszystkich.
 *  Bez paska postepu: odleglosc widac po spławiku, naprezenie slychac (trzeszczenie). */
final class Rybak implements Listener {
    static final String PROF = "fishing";
    static boolean pomiar;
    private static final int OKNO_MIN = 14;   // 700 ms

    /** Ruch spławika, po ktorym gracz rozpoznaje, co bierze. */
    enum Wzorzec { ZANURZENIE, PODNOSI, ODJAZD, SZARPNIECIE, TONIE, OKAZ, DROBNICA }
    enum Gatunek { DROBNICA, RYBA, DAR_WODY, OKAZ }
    enum Faza { CZEKA, ZAPOWIEDZ, OKNO, HOL, KONIEC }

    /** Zachowanie ryby glownej tieru: wzorzec brania, okno zaciecia (ticki), sila w holu, odstep zrywow, szansa wyskoku. */
    record Ryba(Wzorzec wzorzec, int okno, double sila, int odstep, double skok) {}
    private static final Ryba[] RYBY = {
        null,
        new Ryba(Wzorzec.ZANURZENIE, 20, 1.0, 80, 0.25),   // I   Świeży połów
        new Ryba(Wzorzec.ZANURZENIE, 16, 1.0, 70, 0.35),   // II  śledź
        new Ryba(Wzorzec.PODNOSI, 24, 1.1, 80, 0.20),      // III karp
        new Ryba(Wzorzec.ODJAZD, 18, 1.1, 70, 0.10),       // IV  ślepa ryba
        new Ryba(Wzorzec.ZANURZENIE, 18, 1.25, 70, 0.15),  // V   dorsz
        new Ryba(Wzorzec.SZARPNIECIE, 14, 1.2, 60, 0.45),  // VI  pstrąg
        new Ryba(Wzorzec.SZARPNIECIE, 14, 1.35, 55, 0.40), // VII szczupak
        new Ryba(Wzorzec.PODNOSI, 20, 1.35, 60, 0.30),     // VIII
        new Ryba(Wzorzec.ODJAZD, 16, 1.4, 55, 0.60),       // IX latająca ryba
        new Ryba(Wzorzec.SZARPNIECIE, 14, 1.5, 50, 0.40),  // X
    };

    static final class Sesja {
        final Player p; final FishHook hak; final int tier; final Dane.Lowisko low; final Location woda;
        Faza faza = Faza.CZEKA; Gatunek gat; Wzorzec wz; Ryba ryba;
        int t, doZdarzenia, oknoOd, oknoDo, zapowiedzOd;
        int drobnic; Gatunek wymuszony;
        // hol
        double dyst = 1.0, nap = 0.25; int kier, zrywDo, zrywZapow, nastZryw, bezZwijania, trzeszczy, maxTrzeszczy;
        int zrywow, zrywTickow, kontraTickow, skokow; boolean juzTrzeszczalo;
        double trafienieOkna = -1;   // 0..1 — gdzie w oknie zacial
        Location start; Vector bok, doGracza;
        final List<ItemDisplay> disp = new ArrayList<>();
        final List<String> uwagi = new ArrayList<>();
        BukkitTask zad; boolean konczy;
        Sesja(Player p, FishHook hak, int tier, Dane.Lowisko low, Location woda) { this.p = p; this.hak = hak; this.tier = tier; this.low = low; this.woda = woda; }
    }

    private final Rzemioslo plugin;
    private final Lowienie lowienie;
    private final Map<UUID, Sesja> sesje = new HashMap<>();
    /** Haki zarzucone wedka profesji, jeszcze w locie (sesja startuje, gdy spławik usiadzie na wodzie). */
    private final Map<UUID, BukkitTask> loty = new HashMap<>();

    Rybak(Rzemioslo plugin, Lowienie lowienie) { this.plugin = plugin; this.lowienie = lowienie; }

    // ================================================================ zarzut
    /** Wywolane z Lowienie przy zarzucie wedka profesji (zepsuta juz odrzucona). */
    void zarzut(Player p, FishHook h) {
        zakonczGracza(p, null);
        h.setApplyLure(false);
        h.setWaitTime(72000, 72000);   // vanilla nie bierze nigdy — brania prowadzi scena
        BukkitTask stary = loty.remove(p.getUniqueId());
        if (stary != null) stary.cancel();
        final int[] t = {0};
        loty.put(p.getUniqueId(), Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            t[0]++;
            if (!h.isValid() || !p.isOnline() || t[0] > 200) { lotKoniec(p); return; }
            if (h.getState() != FishHook.HookState.BOBBING) return;
            lotKoniec(p);
            usiadl(p, h);
        }, 2L, 1L));
    }

    private void lotKoniec(Player p) { BukkitTask z = loty.remove(p.getUniqueId()); if (z != null) z.cancel(); }

    private void usiadl(Player p, FishHook h) {
        Dane d = plugin.dane();
        Lowienie.Wedka w = lowienie.wedka(p);
        if (w == null) return;
        int tier = lowienie.tierWody(h.getLocation());
        if (w.narz().tier() < tier) { plugin.pasek(p, plugin.kom("slaba-wedka", "{tier}", d.rzymska(tier), "{mam}", d.rzymska(w.narz().tier()))); return; }
        if (Profesje.poziom(p, PROF) < d.start(tier)) {
            plugin.pasek(p, plugin.kom("za-niski-poziom", "{profesja}", d.nazwyProfesji.get(PROF),
                    "{poziom}", String.valueOf(d.start(tier)), "{nazwa}", "Łowisko " + d.rzymska(tier)));
            return;
        }
        Dane.Lowisko low = d.lowiska.get(tier);
        if (low == null) return;
        Sesja s = new Sesja(p, h, tier, low, h.getLocation());
        double k = Math.max(0.5, 1 - 0.05 * (w.narz().tier() - tier));   // lepsza wedka niz woda — szybsze branie
        s.doZdarzenia = (int) (ThreadLocalRandom.current().nextInt(60, 160) * k);
        sesje.put(p.getUniqueId(), s);
        s.zad = Bukkit.getScheduler().runTaskTimer(plugin, () -> tik(s), 1L, 1L);
        if (pomiar) plugin.getLogger().info("[rybak] start " + p.getName() + " tier=" + tier + " za=" + s.doZdarzenia);
    }

    // ================================================================ tik
    private void tik(Sesja s) {
        if (s.konczy) return;
        s.t++;
        Player p = s.p;
        if (!p.isOnline() || p.isDead()) { zakoncz(s, null, false); return; }
        boolean schowana = lowienie.wedka(p) == null;   // vanilla zwija hak sama, gdy w rece nie ma zadnej wedki — tu tez wedka profesji
        if (!s.hak.isValid() || schowana && s.t % 5 == 0) {
            if (s.faza == Faza.HOL) s.uwagi.add(schowana ? "schowana wędka" : "zwinięta żyłka");
            zakoncz(s, s.faza != Faza.HOL ? null : schowana ? "§cSchowałeś wędkę — ryba poszła." : "§cZwinąłeś żyłkę — ryba poszła.", false);
            if (s.hak.isValid()) s.hak.remove();
            return;
        }
        if (s.hak.getWaitTime() < 1200) s.hak.setWaitTime(72000);
        switch (s.faza) {
            case CZEKA -> czeka(s);
            case ZAPOWIEDZ, OKNO -> branie(s);
            case HOL -> hol(s);
            default -> {}
        }
    }

    private void czeka(Sesja s) {
        if (--s.doZdarzenia > 0) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double x = r.nextDouble();
        Gatunek g;
        if (s.drobnic < 2 && x < 0.35) g = Gatunek.DROBNICA;
        else {
            double y = r.nextDouble();
            g = y < s.low.szansaPoboczny() ? Gatunek.DAR_WODY : y < s.low.szansaPoboczny() + 0.05 ? Gatunek.OKAZ : Gatunek.RYBA;
        }
        if (s.wymuszony != null) { g = s.wymuszony; s.wymuszony = null; }
        s.gat = g;
        s.ryba = RYBY[Math.max(1, Math.min(10, s.tier))];
        s.wz = switch (g) { case DROBNICA -> Wzorzec.DROBNICA; case DAR_WODY -> Wzorzec.TONIE; case OKAZ -> Wzorzec.OKAZ; default -> s.ryba.wzorzec(); };
        int zapow = switch (s.wz) { case OKAZ -> 44; case TONIE -> 22; case DROBNICA -> 50; default -> 24 + r.nextInt(10); };
        int okno = switch (s.wz) { case TONIE -> 30; case OKAZ -> 18; case DROBNICA -> 0; default -> s.ryba.okno(); };
        s.zapowiedzOd = s.t;
        s.oknoOd = s.t + zapow;
        s.oknoDo = s.oknoOd + Math.max(OKNO_MIN, okno);
        s.faza = Faza.ZAPOWIEDZ;
        if (pomiar) plugin.getLogger().info("[rybak] branie " + s.p.getName() + " gat=" + g + " wz=" + s.wz + " okno=" + (s.oknoOd - s.t) + "+" + (s.oknoDo - s.oknoOd));
    }

    /** Ruch spławika wg wzorca; okno otwiera sie po zapowiedzi. */
    private void branie(Sesja s) {
        int od = s.t - s.zapowiedzOd, doOkna = s.oknoOd - s.t;
        FishHook h = s.hak;
        Location l = h.getLocation();
        if (s.faza == Faza.ZAPOWIEDZ && s.t >= s.oknoOd) {
            if (s.wz == Wzorzec.DROBNICA) {   // drobnica odplywa — dalej czekasz
                s.drobnic++; s.faza = Faza.CZEKA; s.doZdarzenia = ThreadLocalRandom.current().nextInt(40, 100); return;
            }
            s.faza = Faza.OKNO;
            ruchDecydujacy(s);
            if (wprawa(s) < 5) plugin.pasek(s.p, "§b" + switch (s.wz) {
                case PODNOSI -> "Spławik się podniósł i położył — zatnij! §7(PPM)";
                case ODJAZD, OKAZ -> "Spławik odjeżdża w bok — zatnij! §7(PPM)";
                case TONIE -> "Spławik powoli tonie i stoi — coś ciężkiego. Zatnij §7(PPM)";
                default -> "Spławik poszedł pod wodę — zatnij! §7(PPM)";
            });
            return;
        }
        if (s.faza == Faza.OKNO) {
            ruchTrzymany(s);
            if (s.t > s.oknoDo) {
                zakoncz(s, "§7Ryba zdjęła przynętę. Zwiń i zarzuć jeszcze raz.", false);
                s.hak.setWaitTime(72000);
            }
            return;
        }
        // zapowiedz: ruch zalezny od wzorca
        switch (s.wz) {
            case DROBNICA -> { if (od % 6 == 0) dip(h, 0.06 + ThreadLocalRandom.current().nextDouble(0.04)); if (od % 12 == 3) kregi(l, 1); }
            case SZARPNIECIE -> { if (od % 5 == 0) kregi(l, 2); if (od == 2) dzwiek(l, Sound.ENTITY_FISH_SWIM, 0.6f, 0.7f); }
            case TONIE -> { h.setVelocity(h.getVelocity().setY(-0.03)); }
            case OKAZ -> { if (od % 4 == 0) { h.setVelocity(h.getVelocity().add(bokOd(s).multiply(0.02))); kregi(l, 1); } }
            default -> { if (od == 2 || od == 14) { dip(h, 0.12); dzwiek(l, Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.25f, 1.6f); } }
        }
        if (pomiar && doOkna == 20) plugin.getLogger().info("[rybak] zapowiedz " + s.p.getName() + " 1s do okna");
    }

    private void ruchDecydujacy(Sesja s) {
        FishHook h = s.hak; Location l = h.getLocation();
        switch (s.wz) {
            case PODNOSI -> { h.setVelocity(new Vector(0, 0.22, 0)); dzwiek(l, Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.3f, 1.8f); }
            case ODJAZD -> { h.setVelocity(bokOd(s).multiply(0.25)); dzwiek(l, Sound.ENTITY_FISH_SWIM, 0.8f, 1.0f); }
            case SZARPNIECIE -> { h.setVelocity(new Vector(0, -0.5, 0)); plusk(l, 12); dzwiek(l, Sound.ENTITY_GENERIC_SPLASH, 0.8f, 1.3f); }
            case OKAZ -> { h.setVelocity(new Vector(0, -0.45, 0)); plusk(l, 16); dzwiek(l, Sound.ENTITY_GENERIC_SPLASH, 1.0f, 0.7f); }
            case TONIE -> h.setVelocity(new Vector(0, -0.08, 0));
            default -> { h.setVelocity(new Vector(0, -0.35, 0)); plusk(l, 6); dzwiek(l, Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.8f, 1.0f); }
        }
    }

    /** W oknie spławik trzyma ruch (pod woda / sunie w bok), zeby bylo widac, ze to nie drgniecie. */
    private void ruchTrzymany(Sesja s) {
        FishHook h = s.hak;
        switch (s.wz) {
            case ZANURZENIE, SZARPNIECIE, OKAZ, TONIE -> h.setVelocity(h.getVelocity().setY(-0.09));
            case ODJAZD -> h.setVelocity(h.getVelocity().add(bokOd(s).multiply(0.03)));
            case PODNOSI -> { if (s.t - s.oknoOd < 6) h.setVelocity(h.getVelocity().setY(0.06)); }
            default -> {}
        }
    }

    private Vector bokOd(Sesja s) {
        Vector f = s.hak.getLocation().toVector().subtract(s.p.getLocation().toVector()).setY(0);
        if (f.lengthSquared() < 1e-4) f = new Vector(1, 0, 0);
        f.normalize();
        return new Vector(-f.getZ(), 0, f.getX());   // prawo gracza patrzacego na spławik
    }

    // ================================================================ PPM (zaciecie / zwiniecie)
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void naPpm(PlayerFishEvent e) {
        if (e.getState() == PlayerFishEvent.State.FISHING) return;
        Sesja s = sesje.get(e.getPlayer().getUniqueId());
        if (s == null || !s.hak.equals(e.getHook())) return;
        switch (s.faza) {
            case CZEKA -> zakoncz(s, null, false);   // zwyczajne zwiniecie
            case ZAPOWIEDZ -> zakoncz(s, s.wz == Wzorzec.DROBNICA ? "§7Zaciąłeś drobnicę — spłoszona. Nic." : "§cZa wcześnie — ryba puściła przynętę.", false);
            case OKNO -> { e.setCancelled(true); zacial(s); }
            case HOL -> e.setCancelled(true);   // w holu PPM nic nie robi (zwijanie = Shift)
            default -> {}
        }
    }

    private void zacial(Sesja s) {
        s.trafienieOkna = (double) (s.t - s.oknoOd) / Math.max(1, s.oknoDo - s.oknoOd);
        s.faza = Faza.HOL;
        s.start = s.hak.getLocation().clone();
        s.bok = bokOd(s);
        s.nastZryw = s.t + 25;
        Location l = s.hak.getLocation();
        dzwiek(l, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 0.9f, 0.8f);
        plusk(l, 10);
        if (s.gat == Gatunek.OKAZ || ThreadLocalRandom.current().nextDouble() < s.ryba.skok() && s.gat == Gatunek.RYBA) wyskok(s);
        if (wprawa(s) < 5) plugin.pasek(s.p, "§aSiedzi! §7Trzymaj §fShift§7 — zwijaj.");
        if (pomiar) plugin.getLogger().info("[rybak] zaciecie " + s.p.getName() + " gat=" + s.gat + " wOknie=" + String.format("%.2f", s.trafienieOkna));
    }

    // ================================================================ hol
    private void hol(Sesja s) {
        Player p = s.p;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        boolean zwija = p.isSneaking();
        double sila = s.gat == Gatunek.OKAZ ? s.ryba.sila() * 1.5 : s.gat == Gatunek.DAR_WODY ? 0.8 : s.ryba.sila();
        boolean zrywy = s.gat != Gatunek.DAR_WODY;
        // zapowiedz zrywu (>= 1 s), potem zryw w bok
        if (zrywy && s.zrywDo <= s.t && s.zrywZapow == 0 && s.t >= s.nastZryw) {
            s.kier = r.nextBoolean() ? 1 : -1;
            s.zrywZapow = s.t + 22;
            dzwiek(s.hak.getLocation(), Sound.ENTITY_FISH_SWIM, 0.9f, 0.8f);
            if (wprawa(s) < 5) plugin.pasek(p, (s.kier > 0 ? "§eRyba rwie w prawo §f» §7— kamera w lewo" : "§f« §eRyba rwie w lewo §7— kamera w prawo"));
        }
        if (s.zrywZapow > 0) {
            Location l = s.hak.getLocation().add(s.bok.clone().multiply(0.8 * s.kier));
            l.getWorld().spawnParticle(Particle.BUBBLE, l, 3, 0.2, 0.05, 0.2, 0.02);
            if (s.t % 4 == 0) l.getWorld().spawnParticle(Particle.SPLASH, l.add(0, 0.2, 0), 6, 0.2, 0.05, 0.2, 0.1);
            if (s.t >= s.zrywZapow) {
                s.zrywZapow = 0;
                s.zrywDo = s.t + 30 + r.nextInt(25);
                s.zrywow++;
                plusk(s.hak.getLocation(), 8);
                dzwiek(s.hak.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.7f, 1.2f);
                if (r.nextDouble() < s.ryba.skok() * (s.gat == Gatunek.OKAZ ? 1.5 : 1)) wyskok(s);
            }
        }
        boolean zryw = s.zrywDo > s.t;
        boolean kontra = false;
        if (zryw) {
            s.zrywTickow++;
            double kat = katDoSplawika(s);   // > 0: gracz patrzy na prawo od osi holu
            kontra = kat * s.kier < -15;
            if (kontra) s.kontraTickow++;
            if (s.zrywDo == s.t + 1) s.nastZryw = s.t + s.ryba.odstep() + r.nextInt(30);
        }
        // naprezenie: zwijanie w zrywie bez kontry rwie najszybciej; bez zwijania zryw zabiera zylke
        double dn;
        if (zryw) dn = (zwija ? 0.022 : 0.006) * sila * (kontra ? 0.35 : 1.0) - (!zwija && kontra ? 0.012 : 0);
        else dn = zwija ? (s.gat == Gatunek.DAR_WODY ? 0.012 : 0.0035) * sila : -0.02;
        s.nap = Math.max(0, s.nap + dn);
        if (zwija) {
            double v = zryw ? (kontra ? 0.002 : -0.001) : 0.010 / sila;
            s.dyst -= v * (s.gat == Gatunek.DAR_WODY ? 0.8 : 1.0);
            s.bezZwijania = 0;
        } else {
            s.bezZwijania++;
            if (zryw) s.dyst += 0.006 * (kontra ? 0.4 : 1.0);
        }
        if (zryw) s.dyst += 0.002 * sila;
        // trzeszczenie = sygnal: popusc
        if (s.nap > 0.75) {
            s.trzeszczy++; s.juzTrzeszczalo = true;
            s.maxTrzeszczy = Math.max(s.maxTrzeszczy, s.trzeszczy);
            if (s.t % 4 == 0) dzwiek(p.getLocation(), Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 0.8f, 0.6f + (float) s.nap * 0.4f);
            if (s.t % 3 == 0) p.getWorld().spawnParticle(Particle.CRIT, p.getEyeLocation().add(p.getLocation().getDirection().multiply(1.2)), 2, 0.1, 0.1, 0.1, 0.01);
            if (s.trzeszczy == 1 && wprawa(s) < 5) plugin.pasek(p, "§c✶ Żyłka trzeszczy — puść Shift! ✶");
        } else s.trzeszczy = 0;
        if (s.nap >= 1.0) {
            dzwiek(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1.4f);
            plusk(s.hak.getLocation(), 10);
            s.uwagi.add("zerwana");
            zakoncz(s, "§cŻyłka pękła! Ryba poszła z przynętą.", false);
            s.hak.remove();
            return;
        }
        if (s.dyst >= 1.45) { s.uwagi.add("odpłynęła"); zakoncz(s, "§cRyba wybrała całą żyłkę i się wypięła.", false); s.hak.remove(); return; }
        if (s.bezZwijania > 140 && !zryw) { s.uwagi.add("luz"); zakoncz(s, "§cZa długo luz — ryba się wypięła.", false); s.hak.remove(); return; }
        // spławik idzie do brzegu i w bok przy zrywie
        if (s.t % 2 == 0) {
            Location cel = punktHolu(s);
            if (cel != null) s.hak.teleport(cel);
        }
        if (s.dyst <= 0.0) { wyciagnieta(s); }
    }

    /** Punkt spławika w holu: miedzy miejscem zaciecia a brzegiem przed graczem, w bok przy zrywie. */
    private Location punktHolu(Sesja s) {
        Location g = s.p.getLocation();
        Vector doG = g.toVector().subtract(s.start.toVector()).setY(0);
        double len = doG.length();
        if (len < 0.5) return null;
        double frac = Math.max(0, Math.min(1, s.dyst));
        double dist = 1.6 + (len - 1.6) * frac;
        Vector kier = doG.normalize();
        Location l = g.clone().subtract(kier.multiply(dist));
        l.setY(s.start.getY());
        l.add(s.bok.clone().multiply(przesuniecieBok(s)));
        if (!l.getBlock().isLiquid() && !l.clone().add(0, -0.5, 0).getBlock().isLiquid()) return null;   // nie wyciagamy spławika na lad
        return l;
    }

    private double przesuniecieBok(Sesja s) {
        return s.zrywDo > s.t ? s.kier * Math.min(2.5, (s.zrywDo - s.t) * 0.05 + 0.8) : 0;
    }

    /** Kat kamery wzgledem OSI holu (spławik bez bocznego odjazdu ryby), > 0: gracz patrzy na prawo od osi.
     *  Wzgledem samego spławika nie: gracz, ktory nie rusza kamera, mialby "kontre" za darmo, bo ryba sama odjezdza w bok
     *  (klient QA 20.11: kontra 46/47 bez ruchu myszy). */
    private double katDoSplawika(Sesja s) {
        Location e = s.p.getLocation();
        Vector f = s.hak.getLocation().toVector().subtract(s.bok.clone().multiply(przesuniecieBok(s))).subtract(e.toVector());
        double yawSpl = Math.toDegrees(Math.atan2(-f.getX(), f.getZ()));
        double d = e.getYaw() - yawSpl;
        while (d > 180) d -= 360;
        while (d < -180) d += 360;
        return d;
    }

    // ================================================================ wyskok (widoczny dla wszystkich)
    private void wyskok(Sesja s) {
        ItemStack it = plugin.przedmioty().zrob(s.gat == Gatunek.DAR_WODY ? s.low.poboczny() : s.low.glowny(), 1);
        if (it == null) return;
        s.skokow++;
        Location o = s.hak.getLocation().clone().add(0, -0.2, 0);
        World w = o.getWorld();
        Vector b = s.bok.clone().multiply(s.kier == 0 ? 1 : s.kier);
        ItemDisplay d = w.spawn(o, ItemDisplay.class, x -> {
            x.setItemStack(it);
            x.setPersistent(false);
            x.setTeleportDuration(2);
            x.setBrightness(new Display.Brightness(15, 15));
            x.setViewRange(0.6f);   // ~38 blokow
        });
        s.disp.add(d);
        plusk(o, 10);
        dzwiek(o, Sound.ENTITY_GENERIC_SPLASH, 0.9f, 1.1f);
        final int[] k = {0};
        Bukkit.getScheduler().runTaskTimer(plugin, z -> {
            k[0] += 2;
            if (!d.isValid() || k[0] > 18) {
                if (d.isValid()) { plusk(d.getLocation(), 8); dzwiek(d.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.8f, 1.0f); d.remove(); }
                s.disp.remove(d); z.cancel(); return;
            }
            double f = k[0] / 18.0;
            Location l = o.clone().add(b.clone().multiply(f * 1.6)).add(0, 1.6 * 4 * f * (1 - f), 0);
            l.setYaw((float) Math.toDegrees(Math.atan2(-b.getX(), b.getZ())));
            l.setPitch((float) (-70 + 140 * f));
            d.teleport(l);
        }, 2L, 2L);
    }

    // ================================================================ wynik
    private void wyciagnieta(Sesja s) {
        s.faza = Faza.KONIEC;
        Location l = s.hak.getLocation();
        plusk(l, 12);
        dzwiek(l, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1f, 1.0f);
        s.hak.remove();
        zakoncz(s, null, true);
    }

    void zakoncz(Sesja s, String komunikat, boolean zlowiona) {
        if (s.konczy) return;
        s.konczy = true;
        if (s.zad != null) s.zad.cancel();
        sesje.remove(s.p.getUniqueId());
        for (ItemDisplay d : new ArrayList<>(s.disp)) if (d.isValid()) d.remove();
        String linia = null;
        if (s.faza == Faza.HOL || zlowiona) {
            Lowienie.Wedka w = lowienie.wedka(s.p);
            if (w != null) Przedmioty.zuzyj(s.p, w.slot());
        }
        if (zlowiona) {
            linia = nagroda(s);
            plugin.zlecenia().ustawFlage(s.p, "rybak_wprawa", String.valueOf(Math.min(99, wprawa(s) + 1)));
        } else if (s.faza == Faza.HOL) {
            linia = "§2[Rybak] §7" + nazwaGatunku(s) + " — nic tym razem" + (s.uwagi.isEmpty() ? "" : " §8(" + String.join(", ", s.uwagi) + ")");
        }
        if (pomiar) plugin.getLogger().info("[rybak] koniec " + s.p.getName() + " faza=" + s.faza + " gat=" + s.gat + " zlowiona=" + zlowiona
                + " uwagi=" + s.uwagi + " zrywow=" + s.zrywow + " kontra=" + s.kontraTickow + "/" + s.zrywTickow + " maxTrzeszczy=" + s.maxTrzeszczy + " skokow=" + s.skokow);
        if (komunikat != null) plugin.pasek(s.p, komunikat);
        if (linia != null) s.p.sendMessage(linia);
    }

    private String nazwaGatunku(Sesja s) {
        if (s.gat == null) return "Nic";
        return switch (s.gat) {
            case DAR_WODY -> plugin.dane().nazwa(s.low.poboczny());
            case OKAZ -> "Okaz: " + plugin.dane().nazwa(s.low.glowny());
            default -> plugin.dane().nazwa(s.low.glowny());
        };
    }

    /** Jakosc z zaciecia, kontry i trzeszczenia; zwraca linie czatu. */
    private String nagroda(Sesja s) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double q = 1.0;
        List<String> pow = new ArrayList<>();
        if (s.trafienieOkna > 0.8) { q -= 0.15; pow.add("późne zacięcie"); }
        if (s.zrywTickow > 0) {
            double kr = (double) s.kontraTickow / s.zrywTickow;
            q -= 0.4 * Math.max(0, 0.8 - kr) / 0.8;
            if (kr < 0.5) pow.add("ryba rwała bez kontry");
        }
        if (s.maxTrzeszczy > 0) { q -= Math.min(0.35, 0.012 * s.maxTrzeszczy); if (s.maxTrzeszczy > 10) pow.add("żyłka długo trzeszczała"); }
        q = Math.max(0, Math.min(1, q));
        int klasa = q >= 0.85 ? 2 : q >= 0.5 ? 1 : 0;
        String id; int ile;
        switch (s.gat) {
            case DAR_WODY -> {
                id = s.low.poboczny(); ile = 1;
                for (Map.Entry<String, Double> rz : s.low.rzadkie()) {   // perla z malza: szansa jak dawniej na kazdy polow
                    if (r.nextDouble() < rz.getValue() / Math.max(0.01, s.low.szansaPoboczny())) { id = rz.getKey(); break; }
                }
            }
            case OKAZ -> { id = s.low.glowny(); ile = 2; }
            default -> { id = s.low.glowny(); ile = 1; }
        }
        if (plugin.profesje().podwojny(s.p, PROF)) ile *= 2;
        String wid = klasa == 2 ? id + "_WYBORNE" : id;
        ItemStack it = plugin.przedmioty().zrob(wid, ile);
        if (it == null && klasa == 2) { plugin.getLogger().warning("Rybak: brak przedmiotu " + wid + " — wydaje " + id); it = plugin.przedmioty().zrob(id, ile); }
        if (it == null) { plugin.getLogger().warning("Rybak: brak przedmiotu " + id); ile = 0; }
        else {
            for (ItemStack nad : s.p.getInventory().addItem(it).values()) s.p.getWorld().dropItemNaturally(s.p.getLocation(), nad);
            plugin.zlecenia().zebrano(s.p, PROF, id, ile);
        }
        double mn = s.gat == Gatunek.OKAZ ? 2.0 : 1.0;
        double xp = plugin.dane().g(s.tier) * (0.4 + 0.6 * q) * mn;
        double dx = plugin.profesje().daj(s.p, PROF, s.tier, xp, s.p.getLocation());
        String kl = klasa == 2 ? "§6wyborne" : klasa == 1 ? "§adobre" : "§7słabe";
        StringBuilder sb = new StringBuilder("§2[Rybak] §f");
        if (s.gat == Gatunek.OKAZ) sb.append("Okaz! ");
        sb.append(ile).append("× ").append(plugin.dane().nazwa(id)).append(" §7(").append(kl).append("§7)");
        if (!pow.isEmpty()) sb.append(" §8— ").append(String.join(", ", pow));
        sb.append(" §7+").append(Math.round(dx)).append(" XP");
        if (pomiar) plugin.getLogger().info("[rybak] wynik " + s.p.getName() + " gat=" + s.gat + " id=" + id + " q=" + String.format("%.2f", q)
                + " klasa=" + klasa + " ile=" + ile + " xp=" + Math.round(dx) + " kontra=" + s.kontraTickow + "/" + s.zrywTickow + " trzeszczy=" + s.maxTrzeszczy);
        return sb.toString();
    }

    private int wprawa(Sesja s) {
        try { return Integer.parseInt(plugin.zlecenia().flaga(s.p, "rybak_wprawa")); } catch (NumberFormatException e) { return 0; }
    }

    // ================================================================ efekty
    private static void dip(FishHook h, double v) { h.setVelocity(h.getVelocity().setY(-v)); }

    private static void kregi(Location l, int n) {
        World w = l.getWorld();
        for (int i = 0; i < 8 * n; i++) {
            double a = Math.PI * 2 * i / (8 * n), rr = 0.35 * n;
            w.spawnParticle(Particle.FISHING, l.getX() + Math.cos(a) * rr, l.getY() + 0.05, l.getZ() + Math.sin(a) * rr, 1, 0, 0, 0, 0);
        }
    }

    private static void plusk(Location l, int n) {
        l.getWorld().spawnParticle(Particle.SPLASH, l.clone().add(0, 0.2, 0), n * 2, 0.25, 0.1, 0.25, 0.2);
        l.getWorld().spawnParticle(Particle.BUBBLE_POP, l, n, 0.2, 0.05, 0.2, 0.05);
    }

    private static void dzwiek(Location l, Sound d, float vol, float pitch) {
        l.getWorld().playSound(l, d, SoundCategory.PLAYERS, vol, pitch);   // slychac w poblizu
    }

    // ================================================================ koniec sesji z zewnatrz
    private void zakonczGracza(Player p, String kom) {
        Sesja s = sesje.get(p.getUniqueId());
        if (s != null) zakoncz(s, kom, false);
        lotKoniec(p);
    }

    @EventHandler public void naWyjscie(PlayerQuitEvent e) { zakonczGracza(e.getPlayer(), null); }
    @EventHandler public void naSmierc(PlayerDeathEvent e) { zakonczGracza(e.getEntity(), null); }
    @EventHandler public void naSwiat(PlayerChangedWorldEvent e) { zakonczGracza(e.getPlayer(), null); }

    void zatrzymajWszystko() {
        for (Sesja s : new ArrayList<>(sesje.values())) zakoncz(s, null, false);
        for (BukkitTask t : loty.values()) t.cancel();
        loty.clear();
    }

    /** Stan dla testow (rzemioslo rybak <gracz>). */
    String stan(Player p) {
        Sesja s = sesje.get(p.getUniqueId());
        if (s == null) return "brak sesji; wprawa=" + plugin.zlecenia().flaga(p, "rybak_wprawa") + (loty.containsKey(p.getUniqueId()) ? " lot" : "");
        Location h = s.hak.getLocation();
        return String.format(java.util.Locale.ROOT, "faza=%s gat=%s wz=%s tier=%d t=%d doOkna=%d oknoDo=%d nap=%.2f dyst=%.2f kier=%d zryw=%b zapow=%b kat=%.0f kontra=%d/%d trzeszczy=%d hak=%.2f,%.2f,%.2f",
                s.faza, s.gat, s.wz, s.tier, s.t, s.oknoOd - s.t, s.oknoDo - s.t, s.nap, s.dyst, s.kier, s.zrywDo > s.t, s.zrywZapow > 0,
                s.faza == Faza.HOL ? katDoSplawika(s) : 0.0, s.kontraTickow, s.zrywTickow, s.trzeszczy, h.getX(), h.getY(), h.getZ());
    }

    /** Test: wymus nastepne branie danego gatunku za `za` tickow (tylko konsola). */
    String wymus(Player p, String gat, int za) {
        Sesja s = sesje.get(p.getUniqueId());
        if (s == null || s.faza != Faza.CZEKA) return "brak sesji w CZEKA";
        try { s.wymuszony = Gatunek.valueOf(gat.toUpperCase()); } catch (IllegalArgumentException e) { return "gatunek: drobnica|ryba|dar_wody|okaz"; }
        s.doZdarzenia = Math.max(1, za);
        return "wymus " + s.wymuszony + " za " + s.doZdarzenia;
    }
}
