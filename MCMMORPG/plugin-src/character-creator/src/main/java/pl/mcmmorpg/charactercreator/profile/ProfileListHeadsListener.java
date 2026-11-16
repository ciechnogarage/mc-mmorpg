package pl.mcmmorpg.charactercreator.profile;

import fr.phoenixdevt.mmoprofiles.bukkit.MMOProfiles;
import fr.phoenixdevt.profiles.PlayerProfile;
import fr.phoenixdevt.profiles.ProfileList;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import pl.mcmmorpg.charactercreator.persist.CharacterRecord;
import pl.mcmmorpg.charactercreator.persist.CharacterStore;
import pl.mcmmorpg.charactercreator.skin.SkinService;

/**
 * Glowy na ekranie wyboru postaci MMOProfiles pokazuja SKORKE TEJ POSTACI.
 *
 * <p><b>Po co to w ogole jest.</b> MMOProfiles rysuje kazdy profil jako {@code PLAYER_HEAD}
 * bez tekstury (patrz jego {@code language/gui/profile-list.yml}), wiec kazdy wiersz na
 * liscie wygladal tak samo — domyslna glowa Steve'a. Gracz z czterema postaciami widzial
 * cztery identyczne glowy i musial czytac numer slotu. Skorki znamy (rekord postaci ->
 * skins.yml), wiec dopisujemy je do glow juz po tym, jak MMOProfiles zbuduje okno.
 *
 * <p><b>Dlaczego przez podmiane itemow, a nie przez config MMOProfiles.</b> Klucz
 * {@code texture:} w jego configu jest STALY dla calej pozycji — jedna tekstura dla
 * wszystkich profili. Placeholdera na teksture profilu nie ma.
 *
 * <p><b>Mapowanie slot -> postac</b> bierzemy z configu MMOProfiles
 * ({@code items.profile_item.slots}), a nie z wlasnej listy: N-ta glowa na liscie to N-ta
 * pozycja z {@code ProfileList#getProfiles()} (tak samo liczy je sam viewer). Gdy profili
 * jest wiecej niz slotow, wchodzi paginacja, ktorej z zewnatrz nie widac — wtedy NIE
 * ruszamy niczego, zeby nie podmienic glowy na cudza.
 */
public final class ProfileListHeadsListener implements Listener {

  /**
   * MythicLib dociaga do pozycji dane z innych pluginow ({@code PlaceholderCacheImpl}) i po
   * ich przyjsciu przerysowuje okno, wiec jednorazowe wejscie w pierwszym ticku bywa
   * nadpisane. Drugie podejscie jest po to, zeby glowy przetrwaly to przerysowanie.
   */
  private static final long[] APPLY_DELAYS_TICKS = {1L, 10L};

  private final JavaPlugin plugin;
  private final CharacterStore store;
  private final SkinService skins;
  private final Logger logger;
  private final String guiTitle;
  private final List<Integer> profileSlots;

  private ProfileListHeadsListener(JavaPlugin plugin, CharacterStore store, SkinService skins,
      String guiTitle, List<Integer> profileSlots) {
    this.plugin = plugin;
    this.store = store;
    this.skins = skins;
    this.logger = plugin.getLogger();
    this.guiTitle = guiTitle;
    this.profileSlots = profileSlots;
  }

  /**
   * @return {@code null}, gdy configu MMOProfiles nie da sie odczytac — wtedy po prostu nie
   *         dotykamy jego okna (glowy zostaja domyslne), zamiast zgadywac sloty.
   */
  public static ProfileListHeadsListener create(JavaPlugin plugin, CharacterStore store,
      SkinService skins) {
    File file = new File(plugin.getDataFolder().getParentFile(),
        "MMOProfiles/language/gui/profile-list.yml");
    if (!file.isFile()) {
      plugin.getLogger().warning("Nie ma " + file + " — glowy na ekranie wyboru postaci "
          + "zostana domyslne (skorki postaci i tak dzialaja).");
      return null;
    }
    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
    String title = stripColors(yaml.getString("name", ""));
    List<Integer> slots = yaml.getIntegerList("items.profile_item.slots");
    if (title.isBlank() || slots.isEmpty()) {
      plugin.getLogger().warning(file + " nie ma 'name' albo 'items.profile_item.slots' — "
          + "glowy na ekranie wyboru postaci zostana domyslne.");
      return null;
    }
    return new ProfileListHeadsListener(plugin, store, skins, title, slots);
  }

  @EventHandler
  public void onOpen(InventoryOpenEvent event) {
    if (!(event.getPlayer() instanceof Player player) || !isProfileList(event.getView().getTitle())) {
      return;
    }
    scheduleApply(player, event.getInventory());
  }

  /** Klikniecie przerysowuje pozycje (np. po zmianie strony) — glowy trzeba nalozyc ponownie. */
  @EventHandler
  public void onClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)
        || !isProfileList(event.getView().getTitle())) {
      return;
    }
    scheduleApply(player, event.getView().getTopInventory());
  }

  private boolean isProfileList(String title) {
    return stripColors(title).contains(guiTitle);
  }

  private void scheduleApply(Player player, Inventory inventory) {
    for (long delay : APPLY_DELAYS_TICKS) {
      Bukkit.getScheduler().runTaskLater(plugin, () -> apply(player, inventory), delay);
    }
  }

  private void apply(Player player, Inventory inventory) {
    if (!player.isOnline()) {
      return;
    }
    try {
      ProfileList profiles = MMOProfiles.plugin.getPlayerData(player.getUniqueId());
      if (profiles == null) {
        return;
      }
      List<PlayerProfile> list = profiles.getProfiles();
      if (list.size() > profileSlots.size()) {
        // Paginacja: nie wiadomo, ktora strona jest otwarta, wiec kazde przypisanie byloby
        // zgadywaniem. Lepiej domyslna glowa niz cudza.
        return;
      }
      boolean changed = false;
      for (int index = 0; index < list.size(); index++) {
        int slot = profileSlots.get(index);
        if (slot < 0 || slot >= inventory.getSize()) {
          continue;
        }
        ItemStack item = inventory.getItem(slot);
        if (item == null || item.getType() != Material.PLAYER_HEAD
            || !(item.getItemMeta() instanceof SkullMeta meta)) {
          continue;
        }
        CharacterRecord record = store.get(list.get(index).getUniqueId());
        if (record == null) {
          continue;
        }
        SkinService.Skin skin =
            skins.resolve(record.skinSource(), record.raceId(), record.classId());
        if (skin == null) {
          continue;
        }
        meta.setPlayerProfile(skins.headProfile(skin));
        item.setItemMeta(meta);
        changed = true;
      }
      if (changed) {
        player.updateInventory();
      }
    } catch (RuntimeException ex) {
      // Okno nalezy do MMOProfiles — jego zmiana nie moze wywrocic wyboru postaci.
      logger.warning("Nie udalo sie nalozyc skorek na glowy w ekranie wyboru postaci: " + ex);
    }
  }

  /** Tytul z configu ma kody kolorow, tytul okna moze je miec rozwiniete — porownujemy goly tekst. */
  private static String stripColors(String text) {
    return text == null ? "" : text.replaceAll("[\u00a7&][0-9a-fk-orA-FK-OR]", "").trim();
  }
}
