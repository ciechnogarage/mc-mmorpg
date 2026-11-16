package pl.mcmmorpg.chowance.gra;

import fr.phoenixdevt.profiles.ProfileDataModule;
import fr.phoenixdevt.profiles.event.ProfileCreateEvent;
import fr.phoenixdevt.profiles.event.ProfileRemoveEvent;
import fr.phoenixdevt.profiles.event.ProfileSelectEvent;
import fr.phoenixdevt.profiles.event.ProfileUnloadEvent;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;
import pl.mcmmorpg.chowance.Chowance;

/**
 * Moduł danych MMOProfiles (https://docs.phoenixdevt.fr/mmoprofiles/api/custom-module.html):
 * waliduje WSZYSTKIE cztery zdarzenia; Select = wczytanie w tle, potem validate na wątku głównym;
 * Unload = zapis w tle, potem validate. Profil-duch (id == UUID gracza, memory mmoprofiles-profil-duch)
 * nie dostaje danych — nie wolno mu nic przypisać, bo zniknie przy pierwszej poprawce profili.
 */
public final class ProfilModul implements ProfileDataModule {
  private final JavaPlugin plugin;
  private final Chowance c;
  private final NamespacedKey id;
  /** Sprzątanie sesji (walki, handel, przywołanie) przed zapisem — ustawiane przez plugin. */
  public java.util.function.Consumer<Player> przedZdjeciem = p -> {};
  public java.util.function.Consumer<Player> poWczytaniu = p -> {};
  private final Set<UUID> ostrzezeniDuchy = ConcurrentHashMap.newKeySet();

  public ProfilModul(JavaPlugin plugin, Chowance c) {
    this.plugin = plugin;
    this.c = c;
    this.id = new NamespacedKey(plugin, "chowance");
  }

  @Override public JavaPlugin getOwningPlugin() { return plugin; }
  @Override public NamespacedKey getId() { return id; }

  public static boolean duch(UUID gracz, UUID profil) { return gracz.equals(profil); }

  @EventHandler
  public void onCreate(ProfileCreateEvent e) {
    // Nowy profil = zero Chowańców; plik powstanie przy pierwszym zapisie.
    e.validate(this);
  }

  @EventHandler
  public void onSelect(ProfileSelectEvent e) {
    Player p = e.getPlayer();
    UUID profil = e.getProfile().getUniqueId();
    if (duch(p.getUniqueId(), profil)) {
      if (ostrzezeniDuchy.add(p.getUniqueId())) {
        c.log.warning("Profil-duch MMOProfiles dla " + p.getName() + " (id profilu == UUID gracza) — Chowańce wyłączone dla tego profilu.");
      }
      Bukkit.getScheduler().runTaskLater(plugin, () -> {
        if (p.isOnline()) c.blad(p, "Ten profil nie jest postacią (profil zastępczy) — Chowańce są na nim niedostępne. Wybierz swoją postać albo zgłoś to administracji.");
      }, 40L);
      e.validate(this);
      return;
    }
    c.dane.wczytaj(profil, p.getUniqueId(), p.getName()).whenComplete((d, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
      try {
        if (ex != null) {
          c.log.log(Level.SEVERE, "Nie wczytano Chowańców profilu " + profil + " gracza " + p.getName(), ex);
          if (p.isOnline()) c.blad(p, "Nie udało się wczytać twoich Chowańców — zgłoś administracji (dane są bezpieczne).");
        } else if (p.isOnline()) {
          c.dane.aktywuj(p.getUniqueId(), d);
        }
      } finally {
        e.validate(this);
      }
      if (ex == null && p.isOnline()) poWczytaniu.accept(p);
    }));
  }

  @EventHandler
  public void onUnload(ProfileUnloadEvent e) {
    Player p = e.getPlayer();
    try {
      przedZdjeciem.accept(p);
    } catch (RuntimeException ex) {
      c.log.log(Level.SEVERE, "Błąd sprzątania sesji Chowańców " + p.getName(), ex);
    }
    c.dane.dezaktywuj(p.getUniqueId()).whenComplete((v, ex) -> Bukkit.getScheduler().runTask(plugin, () -> e.validate(this)));
  }

  @EventHandler
  public void onRemove(ProfileRemoveEvent e) {
    UUID profil = e.getProfile().getUniqueId();
    c.dane.archiwizuj(profil).whenComplete((v, ex) -> Bukkit.getScheduler().runTask(plugin, () -> e.validate(this)));
  }
}
