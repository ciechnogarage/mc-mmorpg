package pl.mcmmorpg.trafienia;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class Trafienia extends JavaPlugin implements Listener {

    /**
     * Stan jednego moba: dwie pule na część. rana (cięcie, rąbanie) prowadzi do odcięcia, zgniot (obuch) do zmiażdżenia:
     * kończyna zostaje, ale wiotczeje. Pchnięcie nie zużywa żadnej (kłuje, nie odcina i nie łamie). Ginie z mobem.
     */
    static final class Stan {
        final Map<String, Double> rana = new HashMap<>();
        final Map<String, Double> zgniot = new HashMap<>();
        final Set<String> odciete = new HashSet<>();
        final Set<String> zgniecione = new HashSet<>();
        boolean przygotowany;
        String ostatniaCzesc; int ostatniaCzescTick;
        double zachw; int zachwN, zachwTick;                       // pasek zachwiania
        final Map<String, Integer> plonie = new HashMap<>();       // część -> tick zapłonu
        final Map<String, Double> spalenie = new HashMap<>();      // część -> sekundy palenia (zwęglenie, przepalenie)
        int mrozDo, mokryDo, gasiDo;
        int palenieDo, ogienTick;
        String smierc = "death_oklapniecie";                       // wariant śmierci wg ostatniego ciosu                                  // własne palenie zamiast vanillowego (bez słupa ognia)
    }

    /** Wynik jednego promienia łuku. */
    record Traf(Konfig.Czesc czesc, double t, double kat, Location punkt) {}

    /** Łuk: trafione części w kolejności zamachu i część, w której ostrze utknęło (null = przeszło). */
    record Luk(List<Traf> trafy, Traf utknal) {}

    private Konfig k;
    private final Map<UUID, Stan> stany = new HashMap<>();
    private final Map<UUID, Double> naladowanie = new HashMap<>();
    private final Map<UUID, String> wymuszony = new HashMap<>();
    private double wysokoscCelu = 2;   // wysokość bieżącego celu (wątek serwera, ustawiana przed luk())
    private final Map<ModelBone, Color> blyskOryginal = new WeakHashMap<>();
    private final Map<ModelBone, Integer> blyskDo = new WeakHashMap<>();
    private final List<Odpad> odpady = new ArrayList<>();
    private NamespacedKey kluczNogi;
    private boolean mythicLib, mmoItems;
    /** Kolor bazowy kości (zwęglenie, szron, zwiędłe pędy) — błysk trafienia wraca do niego, nie do białego. */
    private final Map<ModelBone, Color> tintBazowy = new WeakHashMap<>();
    private final Map<UUID, Integer> tagiDo = new HashMap<>();
    private final Map<Long, Integer> ostatniCios = new HashMap<>();
    /** Ostatni prawdziwy atak bronią (klik): gracz -> [cel, tick]. Obrażenia od gracza bez niego to umiejętność/efekt. */
    private final Map<UUID, AtakKlik> atakGracza = new HashMap<>();
    record AtakKlik(Entity cel, int tick) {}

    @Override public void onEnable() {
        saveDefaultConfig();
        kluczNogi = new NamespacedKey(this, "noga");
        przeladuj();
        mythicLib = getServer().getPluginManager().isPluginEnabled("MythicLib");
        mmoItems = mythicLib && getServer().getPluginManager().isPluginEnabled("MMOItems");
        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::tickStatusy, 10, 10);
        // przywracanie koloru po błysku
        Bukkit.getScheduler().runTaskTimer(this, this::tickBlysk, 1, 1);
    }

    @Override public void onDisable() {
        odpady.forEach(Odpad::usun);
        blyskOryginal.forEach(ModelBone::setDefaultTint);
        tintBazowy.keySet().forEach(b -> b.setDefaultTint(Color.WHITE));
    }

    private void przeladuj() {
        reloadConfig();
        k = Konfig.wczytaj(getConfig(), getLogger());
    }

    // ---------- wejście: naładowanie ciosu zanim vanilla je wyzeruje ----------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void przedAtakiem(PrePlayerAttackEntityEvent e) {
        naladowanie.put(e.getPlayer().getUniqueId(), (double) e.getPlayer().getAttackCooldown());
        atakGracza.put(e.getPlayer().getUniqueId(), new AtakKlik(e.getAttacked(), Bukkit.getCurrentTick()));
    }

    // ---------- główne zdarzenie ----------

    /** Opis ciosu po rozpoznaniu źródła. zrodlo: bron | pocisk | zaklecie. odLewej = cios lewą ręką (łuk lustrzany). */
    record Cios(Player gracz, String zrodlo, String rodzaj, boolean mocny, boolean odLewej, Set<String> zywioly,
                double[] o, double[] kier, double zasieg) {}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void trafienie(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity cel)) return;
        long t0 = System.nanoTime();
        ModeledEntity me = ModelEngineAPI.getModeledEntity(cel);
        if (me == null) return;
        ActiveModel model = null; Konfig.ModelDef def = null;
        for (Map.Entry<String, ActiveModel> m : me.getModels().entrySet()) {
            def = k.modele.get(m.getKey());
            if (def != null) { model = m.getValue(); break; }
        }
        if (model == null) return;
        Cios ci = rozpoznaj(e, cel);
        if (ci == null) return;
        // jeden cios gracza w moba na tick: MMOItems/MythicLib dorzucają w tym samym ticku dodatkowe zdarzenia obrażeń
        // (zmierzone 19.10: sztylet MMOItems = 3 zdarzenia) — liczone jak osobne ciosy zużywałyby kończyny 3x szybciej
        long klucz = ci.gracz().getUniqueId().getLeastSignificantBits() ^ cel.getUniqueId().getMostSignificantBits();
        if (ostatniCios.getOrDefault(klucz, -1) == Bukkit.getCurrentTick()) return;
        ostatniCios.put(klucz, Bukkit.getCurrentTick());
        if (ostatniCios.size() > 512) ostatniCios.clear();

        Stan st = stan(cel, model, def);
        long tl = System.nanoTime();
        wysokoscCelu = cel.getHeight();
        Luk lk = ci.zrodlo().equals("zaklecie")
                ? new Luk(lista(najblizsza(ci.o(), ci.kier(), ci.zasieg(), model, def, st)), null)
                : luk(ci.o(), ci.kier(), ci.rodzaj(), ci.mocny(), ci.odLewej(), ci.zasieg(), model, def, st);
        long usLuk = (System.nanoTime() - tl) / 1000;
        List<Traf> trafy = lk.trafy();
        if (trafy.isEmpty()) {
            Traf t = najblizsza(ci.o(), ci.kier(), ci.zasieg(), model, def, st);
            if (t == null) return;
            trafy = List.of(t);
        }
        // główna część CIAŁA: ta na środku łuku, a gdy tam jest przedmiot (kosa, kapelusz) — pierwsza część ciała w zamachu
        Traf glowny = trafy.stream().filter(t -> t.kat() == 0 && !t.czesc().przedmiot()).findFirst()
                .orElse(trafy.stream().filter(t -> !t.czesc().przedmiot()).min(Comparator.comparingDouble(Traf::kat)).orElse(null));
        Traf wPrzedmiot = trafy.stream().filter(t -> t.czesc().przedmiot()).min(Comparator.comparingDouble(Traf::kat)).orElse(null);
        if (lk.utknal() != null) utkniecie(ci.gracz(), lk.utknal());
        st.ostatniaCzesc = (glowny != null ? glowny : trafy.get(0)).czesc().id();
        st.ostatniaCzescTick = Bukkit.getCurrentTick();

        Set<String> naSrodku = new HashSet<>();      // części przecięte środkowym promieniem (tam celował gracz)
        for (Traf t : trafy) if (t.kat() == 0) naSrodku.add(t.czesc().id());
        Map<String, Traf> czesci = new LinkedHashMap<>();
        if (glowny != null) czesci.put(glowny.czesc().id(), glowny);
        for (Traf t : trafy) czesci.putIfAbsent(t.czesc().id(), t);

        // HP: mnożnik MIEJSCA × rodzaju ciosu; zaklęcie bez mnożnika miejsca (nie celuje w kolano), przedmiot = 0
        double przed = e.getDamage();
        boolean zderzenie = glowny == null;
        if (zderzenie) e.setCancelled(true);
        else e.setDamage(przed * (ci.zrodlo().equals("zaklecie") ? 1 : glowny.czesc().mnoznik(ci.rodzaj())));
        double obr = zderzenie ? 0 : e.getFinalDamage();
        boolean smierc = !zderzenie && obr >= cel.getHealth();
        boolean zamarzniety = cel.getScoreboardTags().contains("traf_zamarzniety");

        List<String> odcieteTeraz = new ArrayList<>(), zgniecioneTeraz = new ArrayList<>();
        for (Traf t : czesci.values()) {
            Konfig.Czesc c = t.czesc();
            Konfig.Wsp w = ci.zrodlo().equals("zaklecie") ? Konfig.NEUTRALNY : k.wsp(c.material(), ci.rodzaj());
            blysk(model, c);
            efekt(t, ci.rodzaj());
            if (!(c.bron() && zderzenie)) drgniecie(model, c);
            zywioly(cel, st, c, ci.zywioly());
            if (st.odciete.contains(c.id())) continue;
            // pełna siła: główna część ciała i to, w co celował środek łuku (kapelusz, kosa), reszta łuku — część
            double udzial = t == glowny || naSrodku.contains(c.id()) || (glowny == null && t == wPrzedmiot) ? 1 : k.udzialPoboczny;
            // drzewce wbite w ziemię po zamachu to dźwignia: obuch i topór łamią je dwa razy łatwiej
            double dzw = c.bron() && cel.getScoreboardTags().contains("traf_kosa_w_ziemi") ? 2 : 1;
            double mroz = zamarzniety ? 2 : 1;      // zamarznięta mokra słoma jest krucha
            double r = st.rana.containsKey(c.id()) ? st.rana.merge(c.id(), -przed * udzial * w.rana(), Double::sum) : 1;
            double z = st.zgniot.containsKey(c.id()) ? st.zgniot.merge(c.id(), -przed * udzial * w.zgniot() * dzw * mroz, Double::sum) : 1;
            Konfig.Skutek odc = c.odciecie();
            if (odc != null && zamarzniety && z <= 0 && w.zgniot() > 0) {
                odetnij(cel, model, st, c, odc, srodekVec(ci.kier()), !smierc, "lod");     // roztrzaskanie
                odcieteTeraz.add(c.id());
            } else if (odc != null && ((r <= 0 && odc.przez().contains("rana")) || (z <= 0 && odc.przez().contains("zgniot")))) {
                odetnij(cel, model, st, c, odc, srodekVec(ci.kier()), !smierc, null);
                odcieteTeraz.add(c.id());
            } else if (c.zgniecenie() != null && z <= 0 && !st.zgniecione.contains(c.id())) {
                zgniec(cel, model, st, c, srodekVec(ci.kier()), !smierc);
                zgniecioneTeraz.add(c.id());
            }
        }
        String wynikZderzenia = null;
        if (zderzenie && wPrzedmiot != null && wPrzedmiot.czesc().bron() && !st.odciete.contains(wPrzedmiot.czesc().id()))
            wynikZderzenia = zderz(ci, cel, model, st, wPrzedmiot, przed);
        if (!zderzenie) st.smierc = switch (ci.rodzaj()) {
            case "ciecie", "topor" -> "rozsypanie";
            default -> "death_oklapniecie";
        };
        if (smierc) ustawSmierc(model, st);
        if (!ci.zrodlo().equals("zaklecie") && !smierc)
            zachwianie(cel, model, st, przed, ci.rodzaj(), ci.mocny(), zderzenie ? wPrzedmiot.czesc() : glowny.czesc());
        if (k.log) getLogger().info(String.format(Locale.ROOT,
                "TRAF {\"lewa\":%s,\"zrodlo\":\"%s\",\"zderzenie\":%s,\"zywioly\":%s,\"zachw\":%.2f,\"zgniecione\":%s,\"utknal\":%s,\"us_luk\":%d,\"us\":%d,\"gracz\":\"%s\",\"cel\":\"%s\",\"rodzaj\":\"%s\",\"mocny\":%s,\"glowna\":\"%s\",\"czesci\":%s,\"obr\":%.2f,\"po\":%.2f,\"hp\":%s,\"odciete\":%s,\"smierc\":%s}",
                ci.odLewej(), ci.zrodlo(), wynikZderzenia == null ? "null" : "\"" + wynikZderzenia + "\"", json(ci.zywioly()), st.zachw,
                json(zgniecioneTeraz), lk.utknal() == null ? "null" : "\"" + lk.utknal().czesc().id() + "\"", usLuk,
                (System.nanoTime() - t0) / 1000, ci.gracz().getName(), cel.getUniqueId(), ci.rodzaj(), ci.mocny(),
                glowny == null ? (wPrzedmiot == null ? "" : wPrzedmiot.czesc().id()) : glowny.czesc().id(), json(czesci.keySet()), przed, obr,
                jsonHp(st, def), json(odcieteTeraz), smierc));
    }

    private static List<Traf> lista(Traf t) { return t == null ? List.of() : List.of(t); }

    /**
     * Skąd przyszedł cios. Umiejętności MythicLib też przychodzą jako ENTITY_ATTACK — odróżnia je AttackMetadata.
     * Broń: typ MMOItems (SWORD, GREATHAMMER...) przed nazwą materiału. Lewa ręka: znacznik DwieBronie z tego ticka.
     */
    private Cios rozpoznaj(EntityDamageByEntityEvent e, LivingEntity cel) {
        Integracje.Atak am = null;
        if (mythicLib) try { am = Integracje.atak(e); } catch (Throwable ex) { mythicLib = false; }
        Set<String> zyw = am != null ? am.zywioly() : Set.of();
        if (e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player p) {
            Vector v = pr.getVelocity();
            if (v.lengthSquared() < 1e-6) return null;
            v.normalize();
            Set<String> z = new HashSet<>(zyw);
            if (pr.getFireTicks() > 0) z.add("fire");      // płonąca strzała
            return new Cios(p, am != null && am.umiejetnosc() ? "zaklecie" : "pocisk", "pocisk", false, false, z,
                    wek(pr.getLocation().toVector().subtract(v.clone().multiply(2))), new double[]{v.getX(), v.getY(), v.getZ()}, 5);
        }
        if (!(e.getDamager() instanceof Player p)) return null;
        Location oko = p.getEyeLocation();
        if (am != null && am.umiejetnosc()) {
            // zaklęcie: bez łuku i mnożnika miejsca; część = ta, którą widać od rzucającego w stronę środka celu
            Location sr = cel.getLocation().add(0, cel.getHeight() / 2, 0);
            Vector d = sr.toVector().subtract(oko.toVector());
            double dl = d.length();
            if (dl < 1e-6) return null;
            d.multiply(1 / dl);
            return new Cios(p, "zaklecie", "zaklecie", false, false, zyw, wek(oko), new double[]{d.getX(), d.getY(), d.getZ()}, dl + 2);
        }
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK && e.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return null;
        AtakKlik at = atakGracza.get(p.getUniqueId());
        // klik w moba albo w jego pod-hitbox ME (osobna encja bez modelu tuż przy nim, np. ostrze kosy)
        boolean klik = e.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
                || (at != null && at.tick() == Bukkit.getCurrentTick() && (at.cel().equals(cel)
                    || (ModelEngineAPI.getModeledEntity(at.cel()) == null && at.cel().getWorld().equals(cel.getWorld())
                        && at.cel().getLocation().distanceSquared(cel.getLocation()) < 16)));
        if (!klik || oko.distance(cel.getLocation()) > k.zasieg + 2.5) {
            // obrażenia od gracza bez ataku bronią w tym ticku (umiejętność innego pluginu, wybuch, efekt): jak zaklęcie
            Location sr = cel.getLocation().add(0, cel.getHeight() / 2, 0);
            Vector d = sr.toVector().subtract(oko.toVector());
            double dl = d.length();
            if (dl < 1e-6) return null;
            d.multiply(1 / dl);
            return new Cios(p, "zaklecie", "zaklecie", false, false, zyw, wek(oko), new double[]{d.getX(), d.getY(), d.getZ()}, dl + 2);
        }
        boolean lewa = p.hasMetadata("dwiebronie_lewa") && p.getMetadata("dwiebronie_lewa").get(0).asInt() == Bukkit.getCurrentTick();
        ItemStack bron = lewa ? p.getInventory().getItemInOffHand() : p.getInventory().getItemInMainHand();
        String w = wymuszony.remove(p.getUniqueId());          // test: "lekki" | "mocny" | "<rodzaj>" | "<rodzaj>+mocny"
        String rodzaj = rodzajBroni(bron);
        double nal = naladowanie.getOrDefault(p.getUniqueId(), 1.0);
        boolean mocny = w != null ? w.endsWith("mocny") : nal >= k.mocnyOd;
        String wr = w == null ? null : w.split("\\+")[0];
        if (wr != null && k.luki.containsKey(wr)) rodzaj = wr;
        Set<String> z = new HashSet<>(zyw);
        if (bron.containsEnchantment(org.bukkit.enchantments.Enchantment.FIRE_ASPECT)) z.add("fire");
        return new Cios(p, "bron", rodzaj, mocny, lewa, z, wek(oko), Geometria.kierunek(oko.getYaw(), oko.getPitch()), k.zasieg + 1);
    }

    private String rodzajBroni(ItemStack it) {
        if (mmoItems) try {
            String t = Integracje.typMmo(it);
            if (t != null && k.bronieMmo.containsKey(t)) return k.bronieMmo.get(t);
        } catch (Throwable ex) { mmoItems = false; }
        return k.rodzajBroni(it.getType().name());
    }

    private static void ustawSmierc(ActiveModel model, Stan st) {
        if (st.smierc.equals("rozsypanie") || !model.getBlueprint().getAnimations().containsKey(st.smierc)) return;
        model.getAnimationHandler().setDefaultProperty(new com.ticxo.modelengine.api.animation.handler.AnimationHandler.DefaultProperty(
                com.ticxo.modelengine.api.animation.ModelState.DEATH, st.smierc, .1, .1, 1));
    }

    /**
     * Śmierć wynika z ciała i ostatniego ciosu: kosa zawsze wypada z rąk; cięcie/topór — rozpada się na kawałki
     * (każda kość osobno), obuch — łamie się w pół (animacja), ogień — zwęglony pada sztywno. Startuje z aktualnej pozy.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void smierc(EntityDeathEvent e) {
        Stan st = stany.remove(e.getEntity().getUniqueId());
        if (st == null) return;
        ModeledEntity me = ModelEngineAPI.getModeledEntity(e.getEntity());
        if (me == null) return;
        ActiveModel model = null;
        for (Map.Entry<String, ActiveModel> m : me.getModels().entrySet())
            if (k.modele.containsKey(m.getKey())) { model = m.getValue(); break; }
        if (model == null) return;
        LivingEntity cel = e.getEntity();
        if (k.log) getLogger().info("SMIERC " + cel.getUniqueId() + " " + st.smierc);
        if (!cel.getScoreboardTags().contains("traf_bez_kosy"))
            upusc(cel, model, st, List.of("kosisko", "ostrze", "kikut_kosiska"), new Vector(Math.random() - .5, 0, Math.random() - .5));
        if (st.smierc.equals("rozsypanie")) rozsyp(cel, model);
        else {
            ustawSmierc(model, st);
            var an = model.getBlueprint().getAnimations().get(st.smierc);
            if (an != null) {           // szczątki zostają tam, gdzie animacja śmierci je położyła (ME zaraz usunie model)
                ActiveModel m2 = model; Location kot = cel.getLocation();
                Bukkit.getScheduler().runTaskLater(this, () -> szczatki(m2, kot), Math.max(1, Math.round(an.getLength() * 20) - 2));
            }
        }
    }

    private void szczatki(ActiveModel model, Location kotwica) {
        List<Odpad.Kawalek> kawalki = new ArrayList<>();
        for (ModelBone b : model.getBones().values()) {
            if (!b.isRenderer() || !b.isVisible()) continue;
            ItemStack it = b.getModels().getFirst();
            if (it != null && !it.getType().isAir()) kawalki.add(new Odpad.Kawalek(it.clone(), new Matrix4f(b.getTransformMatrix())));
        }
        if (kawalki.isEmpty()) return;
        Odpad o = new Odpad(kotwica, kawalki, new Vector3f(), new Vector(), null, k.odpadTicki + 80, true);
        o.runTaskTimer(this, 1, 1);
        odpady.add(o);
    }

    /** Rozpada się: każda widoczna kość leci osobno od środka ciała, słoma bucha. */
    private void rozsyp(LivingEntity cel, ActiveModel model) {
        Location kotwica = cel.getLocation();
        Vector3f srodekCiala = new Vector3f(0, (float) (cel.getHeight() * .55), 0);
        for (ModelBone b : model.getBones().values()) {
            if (!b.isRenderer() || !b.isVisible()) continue;
            ItemStack it = b.getModels().getFirst();
            if (it == null || it.getType().isAir()) continue;
            Location l = b.getLocation();
            Vector3f sr = new Vector3f((float) (l.getX() - kotwica.getX()), (float) (l.getY() - kotwica.getY()), (float) (l.getZ() - kotwica.getZ()));
            Vector kier = new Vector(sr.x - srodekCiala.x, 0, sr.z - srodekCiala.z);
            Odpad o = new Odpad(kotwica, List.of(new Odpad.Kawalek(it.clone(), new Matrix4f(b.getTransformMatrix()))), sr, kier,
                    Material.HAY_BLOCK.createBlockData(), k.odpadTicki + 80);
            o.runTaskTimer(this, 1, 1);
            odpady.add(o);
        }
        for (ModelBone b : model.getBones().values()) schowaj(b);
        Location p = kotwica.clone().add(0, cel.getHeight() * .55, 0);
        p.getWorld().spawnParticle(Particle.BLOCK, p, 80, .4, .6, .4, Material.HAY_BLOCK.createBlockData());
        p.getWorld().playSound(p, Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 1.5f, .5f);
        p.getWorld().playSound(p, Sound.BLOCK_WOOL_BREAK, SoundCategory.HOSTILE, 1.2f, .6f);
    }

    // ---------- łuk ciosu ----------

    /**
     * Promienie w kolejności zamachu (cięcie: od prawej do lewej, obuch: z góry w dół, skos: z prawej-góry).
     * Każda NOWA przecięta część zabiera swój opór z energii ciosu; gdy energia się skończy, ostrze utyka w tej części
     * i dalsze promienie już nic nie trafiają (tułów-snop i drzewce zatrzymują, cienka kończyna przepuszcza).
     */
    private Luk luk(double[] o, double[] srodek, String rodzaj, boolean mocny, boolean odLewej, double zasieg,
                    ActiveModel model, Konfig.ModelDef def, Stan st) {
        Konfig.Luk luk = k.luki.getOrDefault(rodzaj, new Konfig.Luk("brak", 0, 0, 1));
        double pol = mocny ? luk.mocny() : luk.lekki();
        double yaw0 = Math.toDegrees(Math.atan2(-srodek[0], srodek[2]));
        double pitch0 = Math.toDegrees(-Math.asin(Geometria.clamp(srodek[1], -1, 1)));
        double energia = mocny ? k.energiaMocny : k.energiaLekki;
        // zamach z góry nie dosięgnie kolana, nie trafiając wcześniej bioder: celując nisko, gracz bije płasko, z boku
        String os = luk.os();
        if (os.equals("pion") || os.equals("skos")) {
            Location b = model.getModeledEntity().getBase().getLocation();
            double dx = b.getX() - o[0], dz = b.getZ() - o[2], poz = Math.hypot(srodek[0], srodek[2]);
            if (poz > 1e-6) {
                double y = o[1] + srodek[1] * Math.hypot(dx, dz) / poz;
                if (y < b.getY() + k.niskoDo * wysokoscCelu) os = "poziom";
            }
        }
        List<Traf> wynik = new ArrayList<>();
        Set<String> przeciete = new HashSet<>();
        // krok kątowy: nie rzadziej niż co 0,1 bl. w odległości celu (goleń ma ~0,2 bl. — rzadszy łuk przelatywał obok)
        double dist = Math.max(1, Geometria.len(Geometria.sub(srodekModelu(model), o)));
        double krok = Math.min(luk.krok(), Math.toDegrees(Math.atan(.1 / dist)));
        int n = os.equals("brak") ? 0 : (int) Math.floor(pol / krok);
        Map<Konfig.Kapsula, double[][]> konce = konceWszystkie(model, def, st);
        // broń zatacza łuk na odległości celowanego punktu: dalej (np. pierś za wysuniętym przedramieniem) nie sięga.
        // Bliżej tak — tam przechodzi głownia. Bez trafienia na środku łuk sięga do środka moba.
        Traf naCelu = promien(srodek, o, zasieg, model, def, st, 0, konce);
        double siega = Math.min(zasieg, (naCelu != null ? naCelu.t() : dist) + k.glownia);
        for (int j = 0; j <= 2 * n; j++) {
            int i = odLewej ? j - n : n - j;          // prawa ręka tnie od prawej, lewa od lewej
            double kat = i * krok;
            double yaw = yaw0, pitch = pitch0;
            switch (os) {
                case "poziom" -> yaw += kat;                                  // yaw rośnie w prawo: start z prawej
                case "pion" -> pitch = Geometria.clamp(pitch0 - kat, -89, 89);   // pitch ujemny = w górę: start z góry
                case "skos" -> { yaw += kat * .8; pitch = Geometria.clamp(pitch0 - kat * .6, -89, 89); }
                default -> { }
            }
            Traf t = kat == 0 ? naCelu : promien(Geometria.kierunek(yaw, pitch), o, siega, model, def, st, Math.abs(kat), konce);
            if (t == null) continue;
            wynik.add(t);
            if (przeciete.add(t.czesc().id())) {
                energia -= t.czesc().opor();
                if (energia <= 1e-9) return new Luk(wynik, t);
            }
        }
        return new Luk(wynik, null);
    }

    /** Ostrze utknęło: twarda część odbija (iskry), miękka (snop) wciąga — gracz na chwilę zwalnia, wyrywając broń. */
    private void utkniecie(Player gracz, Traf t) {
        Location p = t.punkt(); var w = p.getWorld();
        if (t.czesc().twarda()) {
            w.spawnParticle(Particle.CRIT, p, 8, .1, .1, .1, .3);
            w.playSound(p, Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, .9f, 1.3f);
        } else {
            w.spawnParticle(Particle.BLOCK, p, 14, .15, .15, .15, Material.HAY_BLOCK.createBlockData());
            w.playSound(p, Sound.BLOCK_GRASS_BREAK, SoundCategory.PLAYERS, 1f, .6f);
            gracz.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 10, 1, false, false, false));
        }
    }

    /** Najbliższa trafiona część na promieniu; przy remisie (±0.15 bl.) wygrywa wyższy priorytet (słaby punkt). */
    /** Końce kapsuł liczone RAZ na cios (pozycje kości z ME), tylko dla trafialnych części. */
    private Map<Konfig.Kapsula, double[][]> konceWszystkie(ActiveModel model, Konfig.ModelDef def, Stan st) {
        Map<Konfig.Kapsula, double[][]> m = new HashMap<>();
        for (Konfig.Czesc c : def.czesci().values()) {
            if (st.odciete.contains(c.id()) || !widoczna(model, c)) continue;
            for (Konfig.Kapsula kap : c.kapsuly()) { double[][] ab = konce(model, kap); if (ab != null) m.put(kap, ab); }
        }
        return m;
    }

    private static double[] srodekModelu(ActiveModel model) {
        Location l = model.getModeledEntity().getBase().getLocation();
        return new double[]{l.getX(), l.getY() + 1, l.getZ()};
    }

    private Traf promien(double[] d, double[] o, double zasieg, ActiveModel model, Konfig.ModelDef def, Stan st, double kat,
                         Map<Konfig.Kapsula, double[][]> konce) {
        Traf best = null;
        double skala = model.getScale().x();
        for (Konfig.Czesc c : def.czesci().values()) {
            for (Konfig.Kapsula kap : c.kapsuly()) {
                double[][] ab = konce.get(kap);
                if (ab == null) continue;
                double t = Geometria.trafienie(o, d, ab[0], ab[1], kap.promienPx() / 16 * skala, zasieg);
                if (t < 0) continue;
                if (best == null || t < best.t() - .15 || (t < best.t() + .15 && c.priorytet() > best.czesc().priorytet())) {
                    Location p = new Location(model.getModeledEntity().getBase().getLocation().getWorld(),
                            o[0] + d[0] * t, o[1] + d[1] * t, o[2] + d[2] * t);
                    best = new Traf(c, t, kat, p);
                }
            }
        }
        return best;
    }

    /** Klient trafił hitbox ME, ale żaden promień nie wszedł w kapsułę — bierzemy część najbliższą promieniowi środka. */
    private Traf najblizsza(double[] o, double[] d, double zasieg, ActiveModel model, Konfig.ModelDef def, Stan st) {
        Traf best = null; double bestD = .8;
        double skala = model.getScale().x();
        for (Konfig.Czesc c : def.czesci().values()) {
            if (st.odciete.contains(c.id()) || !widoczna(model, c)) continue;
            for (Konfig.Kapsula kap : c.kapsuly()) {
                double[][] ab = konce(model, kap);
                if (ab == null) continue;
                Geometria.Zblizenie z = Geometria.zblizenie(o, d, ab[0], ab[1], zasieg);
                double luz = z.odleglosc() - kap.promienPx() / 16 * skala;
                if (luz < bestD) {
                    bestD = luz;
                    best = new Traf(c, z.t(), 90, new Location(model.getModeledEntity().getBase().getLocation().getWorld(),
                            o[0] + d[0] * z.t(), o[1] + d[1] * z.t(), o[2] + d[2] * z.t()));
                }
            }
        }
        return best;
    }

    /** Część znika razem z odciętym rodzicem (np. kosa z prawą ręką) — schowanej kości nie da się trafić. */
    private static boolean widoczna(ActiveModel model, Konfig.Czesc c) {
        if (c.kosci().isEmpty()) return true;
        return model.getBone(c.kosci().get(0)).map(ModelBone::isVisible).orElse(true);
    }

    private double[][] konce(ActiveModel model, Konfig.Kapsula kap) {
        ModelBone a = model.getBone(kap.a()).orElse(null), b = model.getBone(kap.b()).orElse(null);
        if (a == null || b == null) return null;
        return new double[][]{wek(a.getLocation()), wek(b.getLocation())};
    }

    // ---------- stan, błysk, efekty ----------

    private Stan stan(LivingEntity cel, ActiveModel model, Konfig.ModelDef def) {
        Stan st = stany.computeIfAbsent(cel.getUniqueId(), u -> new Stan());
        if (!st.przygotowany) {
            st.przygotowany = true;
            model.setCanHurt(false); // bez czerwonego błysku CAŁEGO modelu — błyska tylko trafiona część
            // vanilla rysuje płomień na niewidzialnej bazie jako słup ognia zasłaniający model; pali się ciało (cząsteczki części)
            cel.setVisualFire(net.kyori.adventure.util.TriState.FALSE);
            AttributeInstance max = cel.getAttribute(Attribute.MAX_HEALTH);
            double mh = max != null ? max.getValue() : cel.getHealth();
            for (Konfig.Czesc c : def.czesci().values()) {
                for (Konfig.Skutek sk : new Konfig.Skutek[]{c.odciecie(), c.zgniecenie()}) {
                    if (sk == null) continue;
                    for (String pula : sk.przez())
                        (pula.equals("rana") ? st.rana : st.zgniot).merge(c.id(), sk.wytrzymalosc() * mh, Math::min);
                }
            }
        }
        return st;
    }

    private void blysk(ActiveModel model, Konfig.Czesc c) {
        int koniec = Bukkit.getCurrentTick() + k.blyskTicki;
        for (String id : c.kosci()) model.getBone(id).ifPresent(b -> {
            blyskOryginal.putIfAbsent(b, b.getDefaultTint());
            b.setDefaultTint(c.blysk());
            blyskDo.put(b, koniec);
        });
    }

    /** Krótka animacja hurt_<czesc> z modelu (jeśli jest): rusza się tylko trafiona część, nie całe ciało. */
    private static void drgniecie(ActiveModel model, Konfig.Czesc c) {
        String id = "hurt_" + c.id();
        if (model.getBlueprint().getAnimations().containsKey(id))
            model.getAnimationHandler().playAnimation(id, 0, .1, 1, true);
    }

    private void tickBlysk() {
        int teraz = Bukkit.getCurrentTick();
        blyskDo.entrySet().removeIf(en -> {
            if (en.getValue() > teraz) return false;
            Color c = blyskOryginal.remove(en.getKey());
            if (c != null) en.getKey().setDefaultTint(tintBazowy.getOrDefault(en.getKey(), c));
            return true;
        });
        odpady.removeIf(Odpad::isCancelled);
    }

    private void efekt(Traf t, String rodzaj) {
        Location p = t.punkt();
        var w = p.getWorld();
        // obuch w słomę: głuche plaśnięcie, słoma się ugina zamiast pękać — gracz słyszy, że ten cios tu nie działa
        if (t.czesc().efekt().equals("sloma") && (rodzaj.equals("obuch") || rodzaj.equals("piesc"))) {
            w.spawnParticle(Particle.WHITE_ASH, p, 18, .2, .2, .2, .02);
            w.playSound(p, Sound.BLOCK_WOOL_HIT, SoundCategory.HOSTILE, 1.2f, .5f);
            w.playSound(p, Sound.BLOCK_GRASS_STEP, SoundCategory.HOSTILE, 1f, .6f);
            return;
        }
        // pchnięcie w słomę: grot przechodzi przez wypchanie
        if (t.czesc().efekt().equals("sloma") && (rodzaj.equals("pchniecie") || rodzaj.equals("pocisk"))) {
            w.spawnParticle(Particle.BLOCK, p, 5, .05, .05, .05, Material.HAY_BLOCK.createBlockData());
            w.playSound(p, Sound.BLOCK_GRASS_HIT, SoundCategory.HOSTILE, .8f, 1.3f);
            return;
        }
        switch (t.czesc().efekt()) {
            case "iskry" -> {
                w.spawnParticle(Particle.CRIT, p, 10, .1, .1, .1, .25);
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 6, .08, .08, .08, .05);
                w.playSound(p, Sound.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, .35f, 1.7f);
            }
            case "rana" -> {
                w.spawnParticle(Particle.DUST, p, 14, .12, .12, .12, 0, new Particle.DustOptions(Color.fromRGB(0x3FD1B4), 1.2f));
                w.spawnParticle(Particle.BLOCK, p, 10, .1, .1, .1, Material.MUD.createBlockData());
                w.playSound(p, Sound.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 1f, .6f);
                w.playSound(p, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.HOSTILE, .9f, .8f);
            }
            case "sloma" -> {
                w.spawnParticle(Particle.BLOCK, p, 12, .1, .1, .1, Material.HAY_BLOCK.createBlockData());
                w.playSound(p, Sound.BLOCK_GRASS_HIT, SoundCategory.HOSTILE, 1f, .8f);
                w.playSound(p, Sound.BLOCK_WOOL_HIT, SoundCategory.HOSTILE, .8f, .7f);
            }
            case "drewno" -> {
                w.spawnParticle(Particle.BLOCK, p, 8, .08, .08, .08, Material.DARK_OAK_PLANKS.createBlockData());
                w.playSound(p, Sound.BLOCK_WOOD_HIT, SoundCategory.HOSTILE, 1f, .9f);
            }
            default -> {
                w.spawnParticle(Particle.BLOCK, p, 10, .08, .08, .08, Material.MUD.createBlockData());
                w.playSound(p, Sound.BLOCK_MUD_HIT, SoundCategory.HOSTILE, .8f, .9f);
            }
        }
    }

    // ---------- odcięcie ----------

    /** wariant: null = rozcięcie/rozbicie, "lod" = zamarznięta kończyna roztrzaskana, "ogien" = przepalona (zostaje popiół). */
    private void odetnij(LivingEntity cel, ActiveModel model, Stan st, Konfig.Czesc c, Konfig.Skutek odc, Vector kierunek, boolean zyje, String wariant) {
        st.odciete.add(c.id());
        st.plonie.remove(c.id());
        // kości do ukrycia: wskazane + wszystkie ich dzieci (ME ukrywa każdą kość osobno)
        List<ModelBone> kosci = new ArrayList<>();
        for (String id : odc.ukryj()) model.getBone(id).ifPresent(b -> zbierz(b, kosci));
        Location kotwica = cel.getLocation();
        List<Odpad.Kawalek> kawalki = new ArrayList<>();
        Vector3f srodek = new Vector3f(); int n = 0;
        for (ModelBone b : kosci) {
            if (b.isRenderer() && b.isVisible()) {
                ItemStack it = b.getModels().getFirst();
                if (it != null && !it.getType().isAir()) kawalki.add(new Odpad.Kawalek(it.clone(), new Matrix4f(b.getTransformMatrix())));
                Location l = b.getLocation();
                srodek.add((float) (l.getX() - kotwica.getX()), (float) (l.getY() - kotwica.getY()), (float) (l.getZ() - kotwica.getZ()));
                n++;
            }
            schowaj(b);
        }
        if (n > 0) srodek.div(n);
        if (odc.kikut() != null) model.getBone(odc.kikut()).ifPresent(b -> b.setVisible(true));
        BlockData krew = ("lod".equals(wariant) ? Material.PACKED_ICE : switch (c.efekt()) { case "sloma" -> Material.HAY_BLOCK; case "drewno" -> Material.DARK_OAK_PLANKS; default -> Material.MUD; }).createBlockData();
        if ("ogien".equals(wariant)) {
            // przepalona: nie leci w całości, osypuje się popiołem i żarem
            Location p = kotwica.clone().add(srodek.x, srodek.y, srodek.z);
            p.getWorld().spawnParticle(Particle.LARGE_SMOKE, p, 16, .2, .3, .2, .02);
            p.getWorld().spawnParticle(Particle.WHITE_ASH, p, 40, .3, .4, .3, .05);
            p.getWorld().spawnParticle(Particle.LAVA, p, 4, .1, .1, .1, 0);
            p.getWorld().playSound(p, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.HOSTILE, 1f, .7f);
            if (odc.kikut() != null) model.getBone(odc.kikut()).ifPresent(b -> b.setVisible(true));
            nastepstwa(cel, model, c, odc, zyje);
            return;
        }
        if ("lod".equals(wariant)) {
            Location p = kotwica.clone().add(srodek.x, srodek.y, srodek.z);
            p.getWorld().spawnParticle(Particle.SNOWFLAKE, p, 30, .3, .3, .3, .05);
            p.getWorld().playSound(p, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1.3f, .8f);
        }
        if (!kawalki.isEmpty()) {
            Odpad o = new Odpad(kotwica, kawalki, srodek, kierunek, krew, k.odpadTicki);
            o.runTaskTimer(this, 1, 1);
            odpady.add(o);
        }
        Location p = kotwica.clone().add(srodek.x, srodek.y, srodek.z);
        var w = p.getWorld();
        w.spawnParticle(Particle.BLOCK, p, 40, .25, .25, .25, krew);
        if (c.efekt().equals("drewno")) w.playSound(p, Sound.BLOCK_WOOD_BREAK, SoundCategory.HOSTILE, 1.4f, .7f);
        else w.spawnParticle(Particle.BLOCK, p, 20, .35, .2, .35, Material.HAY_BLOCK.createBlockData());
        w.playSound(p, Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 1.4f, .6f);
        w.playSound(p, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1f, .7f);
        w.playSound(p, Sound.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 1.2f, .5f);
        nastepstwa(cel, model, c, odc, zyje);
    }

    /** Tagi dla MM, animacja (kulenie, zwis), spowolnienie, przyklęknięcie — wspólne dla odcięcia i zmiażdżenia. */
    private void nastepstwa(LivingEntity cel, ActiveModel model, Konfig.Czesc c, Konfig.Skutek sk, boolean zyje) {
        for (String t : sk.tagi()) cel.addScoreboardTag(t);
        if (!zyje) return;
        if (sk.chod() != null && model.getBlueprint().getAnimations().containsKey(sk.chod()))   // chód z chorą nogą
            model.getAnimationHandler().setDefaultProperty(new com.ticxo.modelengine.api.animation.handler.AnimationHandler.DefaultProperty(
                    com.ticxo.modelengine.api.animation.ModelState.WALK, sk.chod(), .2, .2, 1));
        if (sk.animacja() != null)      // kilka animacji po przecinku, np. przyklęk (raz) + kulenie (pętla)
            for (String an : sk.animacja().split(",")) model.getAnimationHandler().playAnimation(an.trim(), .2, .3, 1, true);
        if (sk.predkosc() < 1) {
            AttributeInstance sp = cel.getAttribute(Attribute.MOVEMENT_SPEED);
            if (sp != null) {
                NamespacedKey key = new NamespacedKey(this, "noga_" + c.id());
                sp.removeModifier(key);       // odcięcie po zmiażdżeniu zastępuje słabszą karę mocniejszą
                sp.addModifier(new AttributeModifier(key, sk.predkosc() - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
            }
        }
        if (sk.ogluszenie() > 0) zachwiej(cel, model, sk.ogluszenie(), false);   // pęknięty kij w nodze: pada na kolano
    }

    /**
     * Zmiażdżenie (obuch): kończyna NIE odpada. Wiotczeje (animacja hold), a to, co trzymała (ukryj), wypada na ziemię;
     * zmiażdżone kolano: przyklęknięcie i trwałe kulenie, łagodniejsze niż bez nogi.
     */
    private void zgniec(LivingEntity cel, ActiveModel model, Stan st, Konfig.Czesc c, Vector kierunek, boolean zyje) {
        Konfig.Skutek sk = c.zgniecenie();
        st.zgniecione.add(c.id());
        Location kotwica = cel.getLocation();
        if (!sk.ukryj().isEmpty()) {
            List<ModelBone> kosci = new ArrayList<>();
            // kikut wskazany wprost też wypada (złamane kosisko w zmiażdżonej dłoni); zbierz() pomija kikuty dzieci
            for (String id : sk.ukryj()) model.getBone(id).ifPresent(b -> { if (id.startsWith("kikut_")) kosci.add(b); else zbierz(b, kosci); });
            List<Odpad.Kawalek> kawalki = new ArrayList<>();
            Vector3f srodek = new Vector3f(); int n = 0;
            for (ModelBone b : kosci) {
                if (b.isRenderer() && b.isVisible()) {
                    ItemStack it = b.getModels().getFirst();
                    if (it != null && !it.getType().isAir()) kawalki.add(new Odpad.Kawalek(it.clone(), new Matrix4f(b.getTransformMatrix())));
                    Location l = b.getLocation();
                    srodek.add((float) (l.getX() - kotwica.getX()), (float) (l.getY() - kotwica.getY()), (float) (l.getZ() - kotwica.getZ()));
                    n++;
                }
                schowaj(b);
            }
            if (n > 0) srodek.div(n);
            if (!kawalki.isEmpty()) {   // upuszczone, nie odrzucone: słaby pęd w bok
                Odpad o = new Odpad(kotwica, kawalki, srodek, kierunek.clone().multiply(.25), Material.AIR.createBlockData(), k.odpadTicki);
                o.runTaskTimer(this, 1, 1);
                odpady.add(o);
            }
        }
        if (sk.kikut() != null) model.getBone(sk.kikut()).ifPresent(b -> b.setVisible(true));
        Location p = kotwica.clone().add(0, 1, 0);
        var w = p.getWorld();
        w.spawnParticle(Particle.WHITE_ASH, p, 30, .3, .4, .3, .03);
        w.playSound(p, Sound.BLOCK_WOOD_BREAK, SoundCategory.HOSTILE, 1f, .6f);
        w.playSound(p, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.HOSTILE, .7f, .8f);
        nastepstwa(cel, model, c, sk, zyje);
    }

    /** Tag dla MythicMobs na czas: ostatni termin wygrywa (dłuższe zachwianie nie zostanie skrócone krótszym). */
    private void tagNa(LivingEntity cel, String tag, int ticki) {
        cel.addScoreboardTag(tag);
        UUID u = cel.getUniqueId();
        int koniec = Bukkit.getCurrentTick() + ticki;
        String klucz = u + tag;
        tagiDo.merge(UUID.nameUUIDFromBytes(klucz.getBytes()), koniec, Math::max);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Integer k2 = tagiDo.get(UUID.nameUUIDFromBytes(klucz.getBytes()));
            if (k2 != null && k2 <= Bukkit.getCurrentTick()) {
                tagiDo.remove(UUID.nameUUIDFromBytes(klucz.getBytes()));
                Entity e = Bukkit.getEntity(u);
                if (e != null) e.removeScoreboardTag(tag);
            }
        }, ticki + 1L);
    }

    /**
     * Utrata równowagi: mob nie atakuje i nie idzie przez `ticki` (tag traf_zachwiany przerywa zamach w MM).
     * zPaska = pełny pasek zachwiania (kołysanie); inaczej przyklęk po pęknięciu kija w nodze (animację gra skutek).
     */
    private void zachwiej(LivingEntity cel, ActiveModel model, int ticki, boolean zPaska) {
        if (ticki <= 0) return;
        tagNa(cel, "traf_zachwiany", ticki);
        cel.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, ticki, 9, false, false, false));
        if (zPaska) {
            model.getAnimationHandler().stopAnimation("attack");
            if (model.getBlueprint().getAnimations().containsKey("zachwianie"))
                model.getAnimationHandler().playAnimation("zachwianie", .1, .2, 1, true);
            Location p = cel.getLocation().add(0, cel.getHeight() * .8, 0);
            p.getWorld().spawnParticle(Particle.CRIT, p, 14, .3, .15, .3, .05);
            p.getWorld().playSound(p, Sound.BLOCK_WOOD_BREAK, SoundCategory.HOSTILE, .8f, .5f);
        }
    }

    /**
     * Pasek zachwiania: ładuje go siła ciosu × wkład rodzaju (obuch najwięcej) × waga części (głowa, nogi) × mocny.
     * Pełny = zachwianie, pasek do zera, próg rośnie (w grupie nie da się trzymać moba w miejscu). Bez ciosów opada.
     */
    private void zachwianie(LivingEntity cel, ActiveModel model, Stan st, double sila, String rodzaj, boolean mocny, Konfig.Czesc c) {
        Konfig.Zachwianie z = k.zachw;
        double wk = z.wklad().getOrDefault(rodzaj, 0.0);
        if (wk <= 0) return;
        int teraz = Bukkit.getCurrentTick();
        if (teraz - st.zachwTick > z.spadekPo()) st.zachw = Math.max(0, st.zachw - (teraz - st.zachwTick - z.spadekPo()) / 20.0 * z.spadekNaS() * prog(cel, st));
        st.zachwTick = teraz;
        st.zachw += sila * wk * c.zachwianie() * (mocny ? 1.5 : 1);
        if (st.zachw >= prog(cel, st)) {
            st.zachw = 0;
            st.zachwN++;
            zachwiej(cel, model, z.czas(), true);
        }
    }

    private double prog(LivingEntity cel, Stan st) {
        AttributeInstance max = cel.getAttribute(Attribute.MAX_HEALTH);
        return k.zachw.prog() * (max != null ? max.getValue() : 20) * Math.pow(k.zachw.wzrost(), st.zachwN);
    }

    /**
     * Cios trafił tylko w broń moba (kosa): żelazo o żelazo, mob nie traci HP.
     * W trakcie zapowiedzi zamachu (tag traf_zamach z MM): broń gracza wybija kosę z toru — zamach przepada (traf_wybity),
     * obaj dostają odrzut; mocny obuch lub topór wytrąca kosę z rąk. Poza zamachem: kosa odskakuje, chwilę ją ustawia.
     */
    private String zderz(Cios ci, LivingEntity cel, ActiveModel model, Stan st, Traf t, double sila) {
        Location p = t.punkt();
        var w = p.getWorld();
        boolean stal = t.czesc().efekt().equals("iskry");
        w.playSound(p, stal ? Sound.BLOCK_ANVIL_LAND : Sound.BLOCK_WOOD_HIT, SoundCategory.HOSTILE, stal ? .6f : 1.2f, stal ? 1.6f : .8f);
        w.playSound(p, Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1f, stal ? 1.2f : .8f);
        w.spawnParticle(stal ? Particle.ELECTRIC_SPARK : Particle.CRIT, p, 30, .15, .15, .15, .35);
        w.spawnParticle(Particle.CRIT, p, 16, .1, .1, .1, .5);
        Player g = ci.gracz();
        Vector odGracza = cel.getLocation().toVector().subtract(g.getLocation().toVector()).setY(0);
        if (odGracza.lengthSquared() > 1e-6) odGracza.normalize();
        boolean zamach = cel.getScoreboardTags().contains("traf_zamach");
        if (model.getBlueprint().getAnimations().containsKey("odbicie"))
            model.getAnimationHandler().playAnimation("odbicie", .05, .15, 1, true);
        if (zamach) {
            cel.addScoreboardTag("traf_wybity");                      // MM: linie ciosu sprawdzają ?!hastag
            model.getAnimationHandler().stopAnimation("attack");
            cel.setVelocity(cel.getVelocity().add(odGracza.clone().multiply(k.zderzenieOdrzutMoba).setY(.1)));
            g.setVelocity(g.getVelocity().add(odGracza.clone().multiply(-k.zderzenieOdrzutGracza).setY(.05)));
            boolean ciezka = ci.mocny() && (ci.rodzaj().equals("obuch") || ci.rodzaj().equals("topor"));
            Konfig.Czesc reka = model == null ? null : znajdz(st, cel, "reka_r");
            if (ciezka && reka != null && reka.zgniecenie() != null && !st.odciete.contains("reka_r") && !cel.getScoreboardTags().contains("traf_bez_kosy")) {
                upusc(cel, model, st, List.of("kosisko", "ostrze", "kikut_kosiska"), odGracza);
                cel.addScoreboardTag("traf_bez_kosy");
                cel.addScoreboardTag("traf_kosa_wytracona");
                return "wytracona";
            }
            return "wybity";
        }
        tagNa(cel, "traf_kosa_odtracona", k.odtracenieTicki);
        return "odtracona";
    }

    private Konfig.Czesc znajdz(Stan st, LivingEntity cel, String id) {
        ModeledEntity me = ModelEngineAPI.getModeledEntity(cel);
        if (me == null) return null;
        for (String m : me.getModels().keySet()) {
            Konfig.ModelDef d = k.modele.get(m);
            if (d != null) return d.czesci().get(id);
        }
        return null;
    }

    /** Wypuszczenie trzymanego: kości chowane, ich kopie spadają obok (bez rozprysku). */
    private void upusc(LivingEntity cel, ActiveModel model, Stan st, List<String> ids, Vector kierunek) {
        Location kotwica = cel.getLocation();
        List<ModelBone> kosci = new ArrayList<>();
        for (String id : ids) model.getBone(id).ifPresent(b -> { if (id.startsWith("kikut_")) kosci.add(b); else zbierz(b, kosci); });
        List<Odpad.Kawalek> kawalki = new ArrayList<>();
        Vector3f srodek = new Vector3f(); int n = 0;
        for (ModelBone b : kosci) {
            if (b.isRenderer() && b.isVisible()) {
                ItemStack it = b.getModels().getFirst();
                if (it != null && !it.getType().isAir()) kawalki.add(new Odpad.Kawalek(it.clone(), new Matrix4f(b.getTransformMatrix())));
                Location l = b.getLocation();
                srodek.add((float) (l.getX() - kotwica.getX()), (float) (l.getY() - kotwica.getY()), (float) (l.getZ() - kotwica.getZ()));
                n++;
            }
            schowaj(b);
        }
        if (n > 0) srodek.div(n);
        if (!kawalki.isEmpty()) {
            Odpad o = new Odpad(kotwica, kawalki, srodek, kierunek.clone().multiply(.4), Material.AIR.createBlockData(), k.odpadTicki);
            o.runTaskTimer(this, 1, 1);
            odpady.add(o);
        }
        st.odciete.addAll(List.of("kosisko", "ostrze"));
    }

    // ---------- żywioły i statusy na ciele ----------

    /** Żywioł z ciosu lub zaklęcia trafia w konkretną część: ogień ją zapala, lód mrozi całe ciało. */
    private void zywioly(LivingEntity cel, Stan st, Konfig.Czesc c, Set<String> z) {
        if (z.isEmpty()) return;
        int teraz = Bukkit.getCurrentTick();
        if (z.contains("fire") && k.palnosc(c.material()) > 0 && !st.odciete.contains(c.id()) && !mokry(cel, st))
            st.plonie.putIfAbsent(c.id(), teraz);
        if (z.contains("ice")) st.mrozDo = Math.max(st.mrozDo, teraz + 100);
        if (z.contains("water")) st.mokryDo = Math.max(st.mokryDo, teraz + 600);
    }

    private static boolean mokry(LivingEntity cel, Stan st) {
        return cel.isInWaterOrRainOrBubbleColumn() || st.mokryDo > Bukkit.getCurrentTick();
    }

    /** Każdy mob z modelem z configu jest śledzony od pojawienia się (mróz, ogień z otoczenia — zanim ktoś go uderzy). */
    @EventHandler
    public void pojawil(com.destroystokyo.paper.event.entity.EntityAddToWorldEvent e) {
        if (e.getEntity() instanceof LivingEntity le && !(le instanceof Player))
            Bukkit.getScheduler().runTaskLater(this, () -> { if (le.isValid()) zarejestruj(le); }, 10);   // model nakłada MM w onSpawn
    }

    @EventHandler
    public void zniknal(com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent e) {
        stany.remove(e.getEntity().getUniqueId());
    }

    /**
     * Podpalenie moba z modelem przejmuje plugin: vanilla rysowałaby płomień na niewidzialnej bazie (słup ognia przez model).
     * Czas i obrażenia jak vanilla (1 pkt/s), więc balans się nie zmienia; płoną konkretne części.
     */
    @EventHandler(ignoreCancelled = true)
    public void zapalony(org.bukkit.event.entity.EntityCombustEvent e) {
        if (!(e.getEntity() instanceof LivingEntity le)) return;
        zarejestruj(le);
        Stan st = stany.get(le.getUniqueId());
        if (st == null) return;
        e.setCancelled(true);
        if (!mokry(le, st)) st.palenieDo = Math.max(st.palenieDo, Bukkit.getCurrentTick() + Math.round(e.getDuration() * 20));
    }

    @EventHandler(ignoreCancelled = true)
    public void efekt(org.bukkit.event.entity.EntityPotionEffectEvent e) {
        if (e.getEntity() instanceof LivingEntity le && !(le instanceof Player)) zarejestruj(le);
    }

    private void zarejestruj(LivingEntity le) {
        ModeledEntity me = ModelEngineAPI.getModeledEntity(le);
        if (me == null) return;
        for (Map.Entry<String, ActiveModel> m : me.getModels().entrySet()) {
            Konfig.ModelDef d = k.modele.get(m.getKey());
            if (d != null) { stan(le, m.getValue(), d); return; }
        }
    }

    /**
     * Co 0,5 s: statusy mobów przełożone na ciało. Liczby (obrażenia od ognia, trucizny) robi vanilla/MythicLib bez zmian —
     * tu tylko forma: która część płonie i czy przepali się do końca, szron i krucha mokra słoma, więdnące pędy, ślepe oczy.
     */
    private void tickStatusy() {
        int teraz = Bukkit.getCurrentTick();
        for (Map.Entry<UUID, Stan> en : new ArrayList<>(stany.entrySet())) {
            Entity e = Bukkit.getEntity(en.getKey());
            if (!(e instanceof LivingEntity cel) || cel.isDead()) continue;
            ModeledEntity me = ModelEngineAPI.getModeledEntity(cel);
            if (me == null) continue;
            ActiveModel model = null; Konfig.ModelDef def = null;
            for (Map.Entry<String, ActiveModel> m : me.getModels().entrySet()) {
                def = k.modele.get(m.getKey());
                if (def != null) { model = m.getValue(); break; }
            }
            if (model == null) continue;
            Stan st = en.getValue();
            boolean wWodzie = cel.isInWaterOrRainOrBubbleColumn();
            if (wWodzie) st.mokryDo = teraz + 600;
            boolean mokry = st.mokryDo > teraz;
            if (cel.getFireTicks() > 0) {        // ogień spoza zdarzenia (komenda, NBT): też przejmujemy
                if (!mokry) st.palenieDo = Math.max(st.palenieDo, teraz + cel.getFireTicks());
                cel.setFireTicks(0);
            }
            if (wWodzie) st.palenieDo = 0;
            boolean pali = st.palenieDo > teraz;
            ustawTag(cel, "traf_plonie", pali);                       // ikona ognia w PaskiMobow (vanilla Fire = 0)
            if (pali) { st.smierc = "death_zweglenie"; ustawSmierc(model, st); }
            if (pali && teraz - st.ogienTick >= 20) {      // obrażenia jak vanilla: 1 pkt co sekundę
                st.ogienTick = teraz;
                cel.damage(1.0, org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.ON_FIRE).build());
            }

            // --- ogień: zaczyna się w trafionej części (albo w nogach, gdy wszedł w ogień), idzie do sąsiadów
            if (pali) {
                if (st.plonie.isEmpty()) {
                    String start = st.ostatniaCzesc != null && teraz - st.ostatniaCzescTick < 40 ? st.ostatniaCzesc : null;
                    for (String id : start != null ? List.of(start) : List.of("noga_l", "noga_r")) {
                        Konfig.Czesc c = def.czesci().get(id);
                        if (c != null && !st.odciete.contains(id) && k.palnosc(c.material()) > 0) st.plonie.put(id, teraz);
                    }
                }
                st.mrozDo = 0;                       // ogień topi szron
                for (Map.Entry<String, Integer> p : new ArrayList<>(st.plonie.entrySet())) {
                    Konfig.Czesc c = def.czesci().get(p.getKey());
                    if (c == null || st.odciete.contains(c.id())) { st.plonie.remove(p.getKey()); continue; }
                    double s2 = st.spalenie.merge(c.id(), .5 * k.palnosc(c.material()), Double::sum);
                    double[] mid = srodekCzesci(model, c);
                    if (mid != null) {
                        Location l = new Location(cel.getWorld(), mid[0], mid[1], mid[2]);
                        cel.getWorld().spawnParticle(Particle.FLAME, l, 5, .12, .15, .12, .01);
                        cel.getWorld().spawnParticle(Particle.SMOKE, l, 3, .1, .2, .1, .01);
                    }
                    if (teraz - p.getValue() >= 40) for (String sas : c.sasiedzi()) {      // po 2 s ogień przechodzi dalej
                        Konfig.Czesc cs = def.czesci().get(sas);
                        if (cs != null && !st.odciete.contains(sas) && k.palnosc(cs.material()) > 0) st.plonie.putIfAbsent(sas, teraz);
                    }
                    if (c.spalenie() > 0 && s2 >= c.spalenie() && c.odciecie() != null) {
                        odetnij(cel, model, st, c, c.odciecie(), new Vector(0, -1, 0), true, "ogien");
                    }
                }
                // gaszenie: płonie dłużej niż 2 s -> bije się po ogniu (raz na 8 s), przerywa przy tym atak
                boolean dlugo = st.plonie.values().stream().anyMatch(t -> teraz - t >= 40);
                if (dlugo && teraz >= st.gasiDo) {
                    st.gasiDo = teraz + 160;
                    tagNa(cel, "traf_gasi", 20);
                    if (model.getBlueprint().getAnimations().containsKey("gasi"))
                        model.getAnimationHandler().playAnimation("gasi", .1, .2, 1, true);
                }
            } else if (!st.plonie.isEmpty()) {
                st.plonie.clear();
                if (wWodzie) cel.getWorld().spawnParticle(Particle.CLOUD, cel.getLocation().add(0, 1, 0), 12, .3, .5, .3, .02);
            }

            // --- mróz: sucha słoma tylko sztywnieje (spowolnienie robi vanilla/skill); mokra zamarza i kruszeje
            boolean mroz = cel.getFreezeTicks() > 0 || st.mrozDo > teraz;
            boolean zamarzniety = mroz && mokry;
            ustawTag(cel, "traf_zamarzniety", zamarzniety);
            ustawTag(cel, "traf_zmrozony", mroz);                     // ikona zamrożenia w PaskiMobow (także od lodu z zaklęcia)
            if (mroz) cel.getWorld().spawnParticle(Particle.SNOWFLAKE, cel.getLocation().add(0, cel.getHeight() * .6, 0), zamarzniety ? 8 : 3, .3, .5, .3, 0);

            // --- trucizna i wither: słoma jest martwa, działają na żywe pędy Szczepu — więdną, mob robi się ospały
            boolean trucizna = cel.hasPotionEffect(org.bukkit.potion.PotionEffectType.POISON);
            boolean wither = cel.hasPotionEffect(org.bukkit.potion.PotionEffectType.WITHER);
            boolean slepy = cel.hasPotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
            ustawTag(cel, "traf_ospaly", trucizna || wither || mroz || cel.getScoreboardTags().contains("traf_bez_pedow"));
            ustawTag(cel, "traf_slepy", slepy);
            if (slepy && cel instanceof org.bukkit.entity.Mob mob && mob.getTarget() != null
                    && mob.getTarget().getLocation().distanceSquared(cel.getLocation()) > 9) mob.setTarget(null);   // oczy zgasły: słyszy tylko z bliska

            // --- kolory: zwęglenie części, szron, zwiędłe pędy, przygaszone oczy
            for (Konfig.Czesc c : def.czesci().values()) {
                double sp = st.spalenie.getOrDefault(c.id(), 0.0);
                Color tint = Color.WHITE;
                if (sp > 0) tint = miesz(tint, Color.fromRGB(0x2A2018), Math.min(.85, sp / Math.max(2, c.spalenie() > 0 ? c.spalenie() : 6)));
                if (zamarzniety) tint = miesz(tint, Color.fromRGB(0xB4D2FF), .55);
                else if (mroz) tint = miesz(tint, Color.fromRGB(0xD2E6FF), .3);
                if (c.id().equals("pedy") && (trucizna || wither)) tint = miesz(tint, Color.fromRGB(wither ? 0x2A2A20 : 0x7A5A2A), .7);
                for (String id : c.kosci()) { Color t = tint; model.getBone(id).ifPresent(b -> baza(b, t)); }
            }
            Color oczy = slepy ? Color.fromRGB(0x404040) : Color.WHITE;
            model.getBone("swiatlo_oczy").ifPresent(b -> baza(b, oczy));
        }
    }

    private void ustawTag(LivingEntity cel, String tag, boolean on) {
        if (on) cel.addScoreboardTag(tag); else cel.removeScoreboardTag(tag);
    }

    /** Kolor bazowy kości i jej dzieci; w trakcie błysku tylko zapamiętuje (błysk wróci do niego). */
    private void baza(ModelBone b, Color c) {
        Color stary = tintBazowy.put(b, c);
        if (!c.equals(stary) && !blyskDo.containsKey(b)) b.setDefaultTint(c);
    }

    private static Color miesz(Color a, Color b, double t) {
        return Color.fromRGB((int) (a.getRed() + (b.getRed() - a.getRed()) * t), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private double[] srodekCzesci(ActiveModel model, Konfig.Czesc c) {
        if (c.kapsuly().isEmpty()) return null;
        double[][] ab = konce(model, c.kapsuly().get(0));
        return ab == null ? null : Geometria.mul(Geometria.add(ab[0], ab[1]), .5);
    }

    /** Chowa kość i zdejmuje jej pod-hitbox ME (odcięta/upuszczona kosa nie może dalej przyjmować ciosów). */
    private static void schowaj(ModelBone b) {
        b.setVisible(false);
        if (b.hasBoneBehavior(com.ticxo.modelengine.api.model.bone.BoneBehaviorTypes.SUB_HITBOX))
            b.removeBoneBehavior(com.ticxo.modelengine.api.model.bone.BoneBehaviorTypes.SUB_HITBOX);
    }

    private static void zbierz(ModelBone b, List<ModelBone> out) {
        if (b.getBoneId().startsWith("kikut_")) return;
        out.add(b);
        for (ModelBone d : b.getChildren().values()) zbierz(d, out);
    }

    // ---------- komendy (testy / kalibracja) ----------

    @Override public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (a.length == 0) { s.sendMessage("trafienia pokaz [s] | celuj <gracz> <czesc> | cios <gracz> [lekki|mocny|rodzaj] | stan | przeladuj"); return true; }
        switch (a[0]) {
            case "przeladuj" -> { przeladuj(); s.sendMessage("Trafienia: przeladowano"); }
            case "obszar" -> {             // test: obrażenia obszarowe jak zaklęcie z promieniem (każda żywa encja w r)
                Player p = Bukkit.getPlayerExact(a[1]);
                double r = Double.parseDouble(a[2]), ile = Double.parseDouble(a[3]);
                boolean ml = a.length > 4 && a[4].equals("ml");
                int n = 0;
                for (Entity e : p.getNearbyEntities(r, r, r))
                    if (e instanceof LivingEntity le && !(e instanceof Player)) {
                        n++;
                        if (ml && mythicLib) Integracje.obszarMl(p, le, ile); else le.damage(ile, p);
                    }
                s.sendMessage("OBSZAR trafionych encji: " + n);
            }
            case "zaklecie" -> {           // test: trafienie zaklęcia (MythicLib SKILL+MAGIC z żywiołem) od gracza
                Player p = Bukkit.getPlayerExact(a[1]);
                Cel c = najblizszyModel(p.getLocation(), 12);
                if (c == null || !mythicLib) { s.sendMessage("brak modelu albo MythicLib"); return true; }
                boolean ok = Integracje.zaklecie(p, c.byt(), a[2], a.length > 3 ? Double.parseDouble(a[3]) : 4);
                if (ok && a[2].equalsIgnoreCase("fire")) c.byt().setFireTicks(40);      // jak ignite w skillu
                if (ok && a[2].equalsIgnoreCase("ice")) c.byt().setFreezeTicks(160);    // jak freeze w skillu
                s.sendMessage("ZAKLECIE " + a[2] + " -> " + ok);
            }
            case "punkt" -> {              // test: środek części w świecie (bot celuje w nią prawdziwym kliknięciem)
              // test: środek części w świecie (bot celuje w nią prawdziwym kliknięciem)
                Cel c = najblizszyModel(Bukkit.getWorlds().get(0).getSpawnLocation().clone().set(0.5, -59, 4.5), 30);
                if (c == null) { s.sendMessage("brak modelu"); return true; }
                Konfig.Czesc cz = c.def().czesci().get(a[1]);
                double[] m = srodekCzesci(c.model(), cz);
                s.sendMessage(m == null ? "brak" : String.format(Locale.ROOT, "PUNKT %.3f %.3f %.3f", m[0], m[1], m[2]));
            }
            case "rodzaj" -> {             // test: rodzaj ciosu przedmiotu w ręce (typ MMOItems przed materiałem)
                Player p = Bukkit.getPlayerExact(a[1]);
                ItemStack it = p.getInventory().getItemInMainHand();
                String typ = null;
                if (mmoItems) try { typ = Integracje.typMmo(it); } catch (Throwable ex) { typ = "?"; }
                s.sendMessage("RODZAJ " + it.getType() + " typ=" + typ + " -> " + rodzajBroni(it));
            }
            case "pokaz" -> pokaz(s, a.length > 1 ? Integer.parseInt(a[1]) : 10);
            case "stan" -> {
                for (Map.Entry<UUID, Stan> en : stany.entrySet()) {
                    StringBuilder vis = new StringBuilder();
                    Entity e = Bukkit.getEntity(en.getKey());
                    ModeledEntity me = e != null ? ModelEngineAPI.getModeledEntity(e) : null;
                    if (me != null) for (ActiveModel am : me.getModels().values())
                        for (String b : List.of("kikut_reka_l", "kikut_reka_r", "kikut_noga_l", "kikut_noga_r", "kikut_kosiska", "przedramie_seg_l", "przedramie_seg_r", "dlon_r", "ostrze", "kosisko", "noga_l", "golen_l", "noga_r", "kapelusz", "pedy"))
                            am.getBone(b).ifPresent(x -> vis.append(b).append('=').append(x.isVisible() ? 1 : 0).append(' '));
                    s.sendMessage("STAN " + en.getKey() + " rana=" + en.getValue().rana + " zgniot=" + en.getValue().zgniot + " odciete=" + en.getValue().odciete
                            + " zgniecione=" + en.getValue().zgniecione + " plonie=" + en.getValue().plonie.keySet()
                            + " spalenie=" + en.getValue().spalenie + " smierc=" + en.getValue().smierc
                            + " chod=" + chod(me) + String.format(Locale.ROOT, " zachw=%.1f/%d", en.getValue().zachw, en.getValue().zachwN) + " tagi=" + (e != null ? e.getScoreboardTags() : "[]") + " widoczne: " + vis);
                }
            }
            case "celuj" -> {
                Player p = Bukkit.getPlayerExact(a[1]);
                Cel c = najblizszyModel(p.getLocation(), 12);
                if (c == null) { s.sendMessage("brak modelu"); return true; }
                String[] ci = a[2].split(":");               // czesc[:nr_kapsuly[:ulamek]], np. noga_l:1 = goleń, reka_r:0:0.15 = przy łokciu
                Konfig.Czesc cz = c.def().czesci().get(ci[0]);
                double[][] ab = konce(c.model(), cz.kapsuly().get(ci.length > 1 ? Integer.parseInt(ci[1]) : 0));
                double u = ci.length > 2 ? Double.parseDouble(ci[2]) : .5;
                double[] m = Geometria.add(ab[0], Geometria.mul(Geometria.sub(ab[1], ab[0]), u));
                Location oko = p.getEyeLocation();
                Vector d = new Vector(m[0], m[1], m[2]).subtract(oko.toVector());
                Location cel = p.getLocation().setDirection(d);
                p.teleport(cel);
                s.sendMessage(String.format(Locale.ROOT, "CELUJ %s -> %s yaw=%.1f pitch=%.1f dist=%.2f", p.getName(), cz.id(), cel.getYaw(), cel.getPitch(), d.length()));
            }
            case "cios" -> {
                Player p = Bukkit.getPlayerExact(a[1]);
                Cel c = najblizszyModel(p.getLocation(), 8);
                if (c == null) { s.sendMessage("brak modelu"); return true; }
                if (a.length > 2) wymuszony.put(p.getUniqueId(), a[2]);
                p.attack(c.byt());
                s.sendMessage("CIOS " + p.getName() + " -> " + c.byt().getUniqueId());
            }
            default -> s.sendMessage("nieznane");
        }
        return true;
    }

    private String chod(ModeledEntity me) {
        if (me == null) return "-";
        for (Map.Entry<String, ActiveModel> m : me.getModels().entrySet())
            if (k.modele.containsKey(m.getKey())) {
                var dp = m.getValue().getAnimationHandler().getDefaultProperty(com.ticxo.modelengine.api.animation.ModelState.WALK);
                return dp == null ? "walk" : dp.getAnimation();
            }
        return "-";
    }

    record Cel(LivingEntity byt, ActiveModel model, Konfig.ModelDef def) {}

    private Cel najblizszyModel(Location l, double r) {
        Cel best = null; double bd = r * r;
        for (Entity e : l.getWorld().getNearbyEntities(l, r, r, r)) {
            if (!(e instanceof LivingEntity le) || e instanceof Player) continue;
            ModeledEntity me = ModelEngineAPI.getModeledEntity(e);
            if (me == null) continue;
            for (Map.Entry<String, ActiveModel> m : me.getModels().entrySet()) {
                Konfig.ModelDef def = k.modele.get(m.getKey());
                double dd = e.getLocation().distanceSquared(l);
                if (def != null && dd < bd) { bd = dd; best = new Cel(le, m.getValue(), def); }
            }
        }
        return best;
    }

    private static final int[] KOLORY = {0xFF4040, 0x40FF40, 0x4080FF, 0xFFFF40, 0xFF40FF, 0x40FFFF, 0xFF9020, 0xFFFFFF, 0x9040FF, 0x80FF80};

    /** Rysuje kapsuły części najbliższych modeli cząsteczkami (kalibracja, zrzuty QA). */
    private void pokaz(CommandSender s, int sekundy) {
        Location l = s instanceof Player p ? p.getLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();
        final int[] t = {0};
        Bukkit.getScheduler().runTaskTimer(this, task -> {
            if (t[0]++ > sekundy * 10) { task.cancel(); return; }
            for (Player gr : Bukkit.getOnlinePlayers()) {
                Cel c = najblizszyModel(gr.getLocation(), 24);
                if (c == null) continue;
                Stan st = stany.getOrDefault(c.byt().getUniqueId(), new Stan());
                double skala = c.model().getScale().x();
                int i = 0;
                for (Konfig.Czesc cz : c.def().czesci().values()) {
                    Particle.DustOptions kol = new Particle.DustOptions(Color.fromRGB(KOLORY[i++ % KOLORY.length]), .6f);
                    if (st.odciete.contains(cz.id())) continue;
                    for (Konfig.Kapsula kap : cz.kapsuly()) {
                        double[][] ab = konce(c.model(), kap);
                        if (ab == null) continue;
                        double r = kap.promienPx() / 16 * skala;
                        for (int j = 0; j <= 6; j++) {
                            double[] q = Geometria.add(ab[0], Geometria.mul(Geometria.sub(ab[1], ab[0]), j / 6.0));
                            for (int u = 0; u < 8; u++) {
                                double ang = u * Math.PI / 4;
                                c.byt().getWorld().spawnParticle(Particle.DUST, q[0] + Math.cos(ang) * r, q[1] + (u % 2 == 0 ? r : -r) * .3, q[2] + Math.sin(ang) * r, 1, 0, 0, 0, 0, kol);
                            }
                        }
                    }
                }
            }
        }, 0, 2);
        s.sendMessage("Trafienia: pokazuje kapsuly " + sekundy + " s");
    }

    // ---------- drobne ----------

    private static double[] wek(Location l) { return new double[]{l.getX(), l.getY(), l.getZ()}; }
    private static double[] wek(Vector v) { return new double[]{v.getX(), v.getY(), v.getZ()}; }
    private static Vector srodekVec(double[] d) { return new Vector(d[0], d[1], d[2]); }

    private static String json(Iterable<String> it) {
        StringBuilder b = new StringBuilder("[");
        for (String x : it) { if (b.length() > 1) b.append(','); b.append('"').append(x).append('"'); }
        return b.append(']').toString();
    }

    private static String jsonHp(Stan st, Konfig.ModelDef def) {
        StringBuilder b = new StringBuilder("{");
        for (Konfig.Czesc c : def.czesci().values()) {
            if (!st.rana.containsKey(c.id()) && !st.zgniot.containsKey(c.id())) continue;
            if (b.length() > 1) b.append(',');
            b.append('"').append(c.id()).append("\":[").append(String.format(Locale.ROOT, "%.2f,%.2f", st.rana.getOrDefault(c.id(), -1.0), st.zgniot.getOrDefault(c.id(), -1.0))).append(']');
        }
        return b.append('}').toString();
    }
}
