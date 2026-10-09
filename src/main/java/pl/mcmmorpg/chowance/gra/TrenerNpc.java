package pl.mcmmorpg.chowance.gra;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import pl.mcmmorpg.chowance.Chowance;
import pl.mcmmorpg.chowance.dane.ProfilDane;
import pl.mcmmorpg.chowance.logika.Wojownik;
import pl.mcmmorpg.chowance.model.Trener;

/**
 * Trener NPC do prób (manekin z opisem): PPM → okno z wyborem walki turowej albo real-time z jego drużyną
 * i uleczeniem własnej drużyny. Stawiany komendą admina, trwały (przeżywa restart).
 */
public final class TrenerNpc implements Listener {
  private final Chowance c;
  private final Okna okna;
  private final WalkiTurowe walki;
  private final Pojedynki pojedynki;
  private final Druzyny druzyny;
  private final NamespacedKey klucz;

  public TrenerNpc(Chowance c, Okna okna, WalkiTurowe walki, Pojedynki pojedynki, Druzyny druzyny) {
    this.c = c; this.okna = okna; this.walki = walki; this.pojedynki = pojedynki; this.druzyny = druzyny;
    this.klucz = new NamespacedKey(c.plugin, "npc_trener");
  }

  /** Stawia NPC trenera; null, gdy trener nie istnieje w trenerzy/*.yml. */
  public Mannequin postaw(Location l, String trenerId) {
    Trener t = c.rej.trenerzy().get(trenerId);
    if (t == null) return null;
    // Hitbox do PPM: prawdziwy klient nie celuje w manekina (klient QA 2026-09-27: klik trafiał w powietrze,
    // bot „klikał” go wprost pakietem). Interaction przyjmuje PPM jak każdy własny NPC.
    l.getWorld().spawn(l, org.bukkit.entity.Interaction.class, i -> {
      i.setPersistent(true);
      i.setInteractionWidth(0.9f);
      i.setInteractionHeight(2.0f);
      i.setResponsive(true);
      i.getPersistentDataContainer().set(klucz, PersistentDataType.STRING, t.id());
    });
    return l.getWorld().spawn(l, Mannequin.class, m -> {
      m.setPersistent(true);
      m.setRemoveWhenFarAway(false);
      m.setInvulnerable(true);
      m.setImmovable(true);
      m.setSilent(true);
      m.customName(Chowance.mm(t.nazwa()));
      m.setCustomNameVisible(true);
      m.setDescription(Chowance.mm("<gray>PPM — walka próbna"));
      m.getPersistentDataContainer().set(klucz, PersistentDataType.STRING, t.id());
    });
  }

  public int usun(Location l, double promien) {
    int n = 0;
    for (Entity e : l.getWorld().getNearbyEntities(l, promien, promien, promien)) {
      if (e.getPersistentDataContainer().has(klucz, PersistentDataType.STRING)) { e.remove(); n++; }
    }
    return n;
  }

  /** NPC ukryte przed graczem na czas jego walki (stał między graczem a areną i zasłaniał stwory). */
  private final java.util.Map<java.util.UUID, java.util.List<Entity>> ukryte = new java.util.concurrent.ConcurrentHashMap<>();

  private void ukryj(Player p, Entity npc) {
    List<Entity> l = new java.util.ArrayList<>();
    for (Entity e : npc.getNearbyEntities(0.5, 0.5, 0.5)) if (e.getPersistentDataContainer().has(klucz, PersistentDataType.STRING)) l.add(e);
    l.add(npc);
    for (Entity e : l) p.hideEntity(c.plugin, e);
    ukryte.put(p.getUniqueId(), l);
  }

  /** Co 2 s: po walce NPC wraca do widoku. */
  public void pokazPoWalce() {
    for (var it = ukryte.entrySet().iterator(); it.hasNext(); ) {
      var e = it.next();
      Player p = org.bukkit.Bukkit.getPlayer(e.getKey());
      if (p != null && (walki.wWalce(p) || pojedynki.wPojedynku(p))) continue;
      if (p != null) for (Entity x : e.getValue()) if (x.isValid()) p.showEntity(c.plugin, x);
      it.remove();
    }
  }

  /** gracz -> tick kliknięcia w NPC: użycie przedmiotu w tym samym/następnym ticku blokujemy (Dziennik). */
  private final java.util.Map<java.util.UUID, Integer> klikniete = new java.util.concurrent.ConcurrentHashMap<>();

  @EventHandler(priority = EventPriority.LOWEST)
  public void uzyciePrzedmiotu(org.bukkit.event.player.PlayerInteractEvent e) {
    Integer t = klikniete.get(e.getPlayer().getUniqueId());
    if (t == null || org.bukkit.Bukkit.getCurrentTick() - t > 1) return;
    e.setCancelled(true);
    e.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void ppm(PlayerInteractEntityEvent e) {
    if (e.getHand() != EquipmentSlot.HAND) return;
    String id = e.getRightClicked().getPersistentDataContainer().get(klucz, PersistentDataType.STRING);
    if (id == null) return;
    e.setCancelled(true);
    Player p = e.getPlayer();
    klikniete.put(p.getUniqueId(), org.bukkit.Bukkit.getCurrentTick());
    Trener t = c.rej.trenerzy().get(id);
    if (t == null) { c.blad(p, "Ten trener nie ma już drużyny (trenerzy/*.yml)."); return; }
    ProfilDane d = c.daneLubBlad(p);
    if (d == null) return;
    if (walki.wWalce(p) || pojedynki.wPojedynku(p)) { c.blad(p, "Już walczysz."); return; }
    if (d.druzyna.isEmpty()) { c.blad(p, "Nie masz jeszcze chowańca — wybierz startera w menu Chowańców (klawisz Q)."); return; }
    // Tick później i z zamknięciem ekwipunku: PPM z Dziennikiem w ręce otwierał menu Dziennika zamiast okna
    // trenera (klient QA 2026-09-27).
    Entity npc = e.getRightClicked();
    org.bukkit.Bukkit.getScheduler().runTaskLater(c.plugin, () -> { if (p.isOnline()) { p.closeInventory(); okno(p, t, npc); } }, 5L);
  }

  private void okno(Player p, Trener t, Entity npc) {
    List<Wojownik> jego = walki.druzynaTrenera(t);
    StringBuilder sklad = new StringBuilder();
    for (Wojownik w : jego) sklad.append(sklad.isEmpty() ? "" : ", ").append(w.g.nazwa()).append(" poz. ").append(w.o.poziom);
    okna.pokaz(p, t.nazwa(), List.of(
        "<gray>Drużyna: <white>" + sklad,
        "",
        "<gold>Walka turowa</gold> <gray>— wybierasz ruch klawiszami <white>1–4</white>, tura rozgrywa się na arenie.",
        "<gray>  <white>5</white> zmiana, <white>6</white> przedmiot, <white>8</white>×2 poddanie.",
        "<red>Real-time</red> <gray>— stwory walczą same, po KO wchodzi następny. Rozkazy:",
        "<gray>  <white>1–4</white> ruch, <white>5</white> unik (gdy „TERAZ!”), <white>6</white> eliksir, <white>7</white> zmiana, <white>8</white>×2 poddanie."),
        List.of(
            Okna.Przycisk.of("<gold>Walka turowa", "Cała drużyna trenera", x -> { if (walki.startTrener(x, t.id(), null, 0)) ukryj(x, npc); }),
            Okna.Przycisk.of("<red>Real-time", "Stwory walczą same, ty wydajesz rozkazy", x -> {
              List<Wojownik> l = walki.druzynaTrenera(t);
              if (!l.isEmpty() && pojedynki.startNpc(x, t.nazwa(), l)) ukryj(x, npc);
            }),
            Okna.Przycisk.of("<green>Ulecz drużynę", "Do prób — pełne zdrowie bez eliksirów", x -> {
              ProfilDane dx = c.dane(x);
              if (dx == null) return;
              druzyny.leczWszystkich(x, dx, 100);
              c.msg(x, "<green>Drużyna uleczona.");
            })),
        Okna.Przycisk.of("<gray>Zamknij", "", x -> {}), 1, true, false);
  }
}
