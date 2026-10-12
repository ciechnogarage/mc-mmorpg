package pl.mcmmorpg.charactercreator.creator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.mcmmorpg.charactercreator.CreatorConfig;

/**
 * Jednoekranowy kreator: rasy w lewej kolumnie, klasy w prawej, model 3D widoczny przez
 * CALKOWICIE przezroczysty ekran. Interfejs nie rysuje zadnego tla — widac same ikony
 * w slotach i imie postaci w pasku tytulu.
 *
 * <p><b>Dlaczego SHULKER_BOX.</b> Zeby bylo widac model, tlo panelu musi byc przezroczyste,
 * co robi override tekstury w resource packu. W 1.21.11 istnieje tylko JEDNA tekstura
 * {@code container/generic_54.png} wspolna dla wszystkich rozmiarow skrzyn — jej podmiana
 * rozwalilaby kazde GUI na serwerze. {@code container/shulker_box.png} ma wlasna teksture
 * i 27 slotow, a zaden plugin w tym stacku go nie uzywa.
 *
 * <p><b>Skad bierze sie "nic".</b> Trzy overrides w packu Nexo sa w pelni przezroczyste
 * (alpha 0 na calej powierzchni, sprawdzone): {@code gui/container/shulker_box.png} oraz
 * {@code gui/sprites/container/slot_highlight_front.png} i {@code ..._back.png}. To one
 * kasuja tlo panelu, kwadraty slotow i podswietlenie pod kursorem. Gdyby ktorykolwiek
 * wrocil do wersji z packa vanilla, na scenie pojawi sie siatka slotow.
 *
 * <p><b>Ramy nie ma i to jest wybor.</b> Poprzednia wersja rysowala caly panel — boczne
 * szyny, sztandar z tabliczka imienia i plinte pod przyciskiem — jednym glifem 256x256
 * wstawianym w TYTUL okna i pozycjonowanym fontem {@code nexo:shift}. Kosztowalo to 16
 * tekstur (rasa x klasa), dwie wartosci przesuniecia zmierzone sonda na zywym kliencie
 * i tryb pomiarowy w configu, a zabieralo ok. 1/3 ekranu wokol postaci. Generator tej
 * grafiki ({@code tools/gen_creator_art.py}) zostaje w repo i dalej dziala, ale nie jest
 * juz podpiety.
 *
 * <p><b>UWAGA przy ewentualnym powrocie do ramy:</b> zrodla tego pluginu NIE SA w gicie
 * (cale {@code plugin-src/character-creator/} jest nieśledzone), wiec starego
 * {@code title()} i tablic fontu {@code nexo:shift} nie ma skad wyciagnac poleceniem
 * {@code git}. Ostatni jar, ktory je zawieral, to {@code character-creator-0.2.0-*} —
 * deploy.sh odklada go do {@code deploy-backups/}. Same znaki przesuwajace generuje Nexo
 * do pack.zip, w zrodlach packa ich nie ma.
 *
 * <p><b>Geometria</b> (potwierdzona pomiarem na zywym kliencie, blad 1 px): panel 9x3 ma
 * 176 x 168 px GUI, slot (col,row) zaczyna sie w (8 + col*18, 18 + row*18). Ikony stoja
 * w skrajnych kolumnach, wiec srodek ekranu — caly pas miedzy nimi — zostaje pusty.
 */
public final class CreatorMenu {

  // Uklad 9x3 + sloty ekwipunku gracza (raw 27..53 = plecak, 54..62 = hotbar).
  //
  // RZEDY IKON SA ROZSTAWIONE: y = 18, 54, 85, 121 (odstepy 36/31/36 px GUI). Wczesniej
  // szly po kolei 18/36/54, a czwarta ladowala dopiero na 85 — trzy sklejone i jedna
  // odstajaca wygladaly jak blad. Rowny raster nie jest tu osiagalny (miedzy kontenerem
  // a ekwipunkiem gracza jest techniczna szczelina 13 px), ale 36/31/36 czyta sie jako
  // celowy rozstaw.
  //
  // Na dole sa DWA przyciski: WYJDZ (z lewej) i STWORZ POSTAC (na srodku). Ani zoomu,
  // ani obrotu — postac stoi zawsze tak samo, przodem do gracza (yaw 0, patrz
  // CreatorSession#MODEL_YAW). Kazde sterowanie kadrem, ktore probowalismy (zoom
  // przesuwajacy encje, strzalki obracajace model), dawalo wiecej zamieszania niz
  // pozytku: kadr przestawal byc przewidywalny,
  // a gracz i tak wracal do ustawienia domyslnego.
  public static final int[] SLOTS_RACE = {0, 18, 27, 45};     // skrajna kolumna z lewej
  public static final int[] SLOTS_CLASS = {8, 26, 35, 53};    // skrajna kolumna z prawej
  public static final int SLOT_NAME = 4;                      // GORA, srodek
  public static final int SLOT_CONFIRM = 58;                  // DOL, srodek

  /**
   * DOL, skrajnie z lewej — wyjscie z kreatora bez tworzenia postaci.
   *
   * <p>Do tej pory z sali NIE BYLO WYJSCIA innego niz stworzenie postaci: teleport,
   * komendy i czat sa zablokowane ({@link CreatorRoomListener}), zamkniete okno straznik
   * otwiera z powrotem, a tryb SPECTATOR jest wymuszany co 10 tickow. Kto wszedl, ten
   * konczyl albo sie rozlaczal.
   *
   * <p>Slot jest CELOWO daleko od {@link #SLOT_CONFIRM} (przeciwne konce dolnego rzedu,
   * cztery sloty przerwy): pomylka kosztuje tu porzucenie wyboru, a nie jedno klikniecie
   * wiecej.
   */
  public static final int SLOT_EXIT = 54;                     // DOL, z lewej

  /** Indeksy ekwipunku gracza, ktore trzymaja nasze ikony — sweeper ma ich NIE czyscic. */
  public static final Set<Integer> PLAYER_SLOTS_IN_USE =
      Set.of(playerIndex(27), playerIndex(45), playerIndex(35), playerIndex(53),
             playerIndex(SLOT_CONFIRM), playerIndex(SLOT_EXIT));

  /** Oznacza nasze inventory, zeby handlery klikniec odroznily je od innych GUI. */
  public static final class Holder implements InventoryHolder {
    private Inventory inventory;

    @Override
    public Inventory getInventory() {
      return inventory;
    }

    private void setInventory(Inventory inventory) {
      this.inventory = inventory;
    }
  }

  private final CreatorConfig config;

  public CreatorMenu(CreatorConfig config) {
    this.config = config;
  }

  /** Raw slot widoku -> indeks w ekwipunku gracza (tylko dla raw >= 27). */
  static int playerIndex(int rawSlot) {
    return rawSlot >= 54 ? rawSlot - 54 : rawSlot - 18;
  }

  /**
   * Tytul to samo imie postaci — jedyny tekst na ekranie.
   *
   * <p>Biel jest wymuszona: vanilla rysuje tytul kontenera ciemnoszarym {@code #404040},
   * co na ciemnej scenie jest nieczytelne. (Ten sam mechanizm mnozenia przez kolor tekstu
   * decydowal wczesniej o tym, czy glif ramy w ogole widac.)
   *
   * <p>Zwraca Stringa (legacy), a nie Component, celowo: {@link InventoryView#setTitle}
   * przyjmuje tylko Stringa, a to ono pozwala odswiezyc imie BEZ zamykania okna.
   */
  private String title(CreatorSession session) {
    return ChatColor.WHITE + "Imię: " + session.displayName();
  }

  /** Otwiera swieze okno kreatora dla gracza. */
  @SuppressWarnings("deprecation")
  public Inventory open(Player player, CreatorSession session) {
    Holder holder = new Holder();
    Inventory inventory = Bukkit.createInventory(holder, InventoryType.SHULKER_BOX, title(session));
    holder.setInventory(inventory);
    player.openInventory(inventory);
    render(inventory, session, player);
    return inventory;
  }

  /** Przemalowuje ramę w miejscu — bez zamykania okna, wiec ekran nie mruga. */
  @SuppressWarnings("deprecation")
  public void repaint(Player player, CreatorSession session) {
    InventoryView view = player.getOpenInventory();
    if (view.getTopInventory().getHolder() instanceof Holder) {
      view.setTitle(title(session));
    }
    render(session.menuInventory(), session, player);
  }

  /** Przerysowuje wszystkie sloty — wolac po kazdej zmianie rasy/klasy/imienia. */
  public void render(Inventory inventory, CreatorSession session, Player player) {
    if (inventory == null) {
      return;
    }
    for (int i = 0; i < inventory.getSize(); i++) {
      inventory.setItem(i, null);
    }

    List<String> raceIds = session.raceIds();
    for (int i = 0; i < SLOTS_RACE.length && i < raceIds.size(); i++) {
      String id = raceIds.get(i);
      CreatorConfig.RaceOption race = config.races().get(id);
      boolean selected = id.equals(session.currentRaceId());
      put(inventory, player, SLOTS_RACE[i], icon(Material.PAPER,
          race != null ? race.icon() : "",
          (selected ? "&f&l" : "&7") + (race != null ? race.display() : id), selected,
          lore(race == null ? "" : race.description(), race == null ? "" : race.bonus(),
              selected ? "&8Wybrana rasa" : "&8Kliknij, by wybrać")));
    }

    // Kafelek klasy mowi, KIM gracz zostanie przy wybranej rasie (Ork + wojownik =
    // Wodz Wojny), a nie sam archetyp — klase rasowa nadaje potem skrypt CoreTools.
    List<String> classIds = session.classIds();
    String raceId = session.currentRaceId();
    for (int i = 0; i < SLOTS_CLASS.length && i < classIds.size(); i++) {
      String id = classIds.get(i);
      CreatorConfig.ClassOption clazz = config.classes().get(id);
      boolean selected = id.equals(session.currentClassId());
      put(inventory, player, SLOTS_CLASS[i], icon(Material.PAPER,
          clazz != null ? clazz.icon() : "",
          (selected ? "&f&l" : "&7") + (clazz != null ? clazz.displayFor(raceId) : id), selected,
          lore(clazz == null ? "" : clazz.descriptionFor(raceId),
              clazz == null ? "" : clazz.bonusFor(raceId),
              selected ? "&8Wybrana klasa" : "&8Kliknij, by wybrać")));
    }

    String name = session.hasName() ? session.pendingName() : player.getName();
    put(inventory, player, SLOT_NAME, icon(Material.PAPER, "cc:ui_name",
        "&e&lImię: &f" + name, false, "&7LPM — wpisz imię", "&7PPM — losuj"));

    boolean ready = session.hasName();
    put(inventory, player, SLOT_CONFIRM, ready
        ? icon(Material.PAPER, "cc:ui_confirm", "&a&lSTWÓRZ POSTAĆ", true, "&7Wszystko gotowe!")
        : icon(Material.PAPER, "cc:ui_confirm", "&8&lSTWÓRZ POSTAĆ", false,
            "&7Najpierw ustaw imię"));

    put(inventory, player, SLOT_EXIT, icon(Material.PAPER, "cc:ui_exit",
        "&c&lWYJDŹ", false, "&7Wróć do wyboru postaci", "&8Postać nie zostanie utworzona"));
  }

  /**
   * Opis i etykieta leca do podpowiedzi przedmiotu, a nie na wypalony pergamin.
   * Vanillowy font jest w pelni czytelny, obsluguje polskie znaki i nie ma limitu
   * szerokosci — wypalony tekst 9 px progowany do 1 bitu byl nieczytelny i wychodzil
   * poza ramke panelu.
   */
  private String[] lore(String description, String bonus, String hint) {
    List<String> out = new ArrayList<>();
    if (description != null && !description.isBlank()) {
      for (String line : description.split("\n")) {
        out.add("&7" + line);
      }
    }
    if (bonus != null && !bonus.isBlank()) {
      out.add("&6" + bonus);
    }
    out.add(hint);
    return out.toArray(new String[0]);
  }

  /** Wklada ikone do gornego inventory albo do ekwipunku gracza, zaleznie od raw slotu. */
  private void put(Inventory top, Player player, int rawSlot, ItemStack item) {
    if (rawSlot < top.getSize()) {
      top.setItem(rawSlot, item);
    } else {
      player.getInventory().setItem(playerIndex(rawSlot), item);
    }
  }

  /** Null, jesli slot (wg InventoryClickEvent#getRawSlot) nie jest nasz. */
  public static String actionOf(int rawSlot) {
    for (int i = 0; i < SLOTS_RACE.length; i++) {
      if (SLOTS_RACE[i] == rawSlot) {
        return "race_" + i;
      }
    }
    for (int i = 0; i < SLOTS_CLASS.length; i++) {
      if (SLOTS_CLASS[i] == rawSlot) {
        return "class_" + i;
      }
    }
    return switch (rawSlot) {
      case SLOT_NAME -> "set_name";
      case SLOT_CONFIRM -> "confirm";
      case SLOT_EXIT -> "exit";
      default -> null;
    };
  }

  private ItemStack icon(Material material, String model, String name, boolean glint,
      String... lore) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    if (model != null && !model.isBlank()) {
      NamespacedKey key = NamespacedKey.fromString(model);
      if (key != null) {
        meta.setItemModel(key);
      }
    }
    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
    if (lore.length > 0) {
      List<String> colored = new ArrayList<>();
      for (String line : lore) {
        colored.add(ChatColor.translateAlternateColorCodes('&', line));
      }
      meta.setLore(colored);
    }
    if (glint) {
      meta.setEnchantmentGlintOverride(true);
    }
    item.setItemMeta(meta);
    return item;
  }
}
