package pl.mcmmorpg.napady;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.util.DefenseFormula;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

/**
 * Wyposazenie jednostki z przedmiotow MMOItems - tych samych, ktore nosi gracz - i liczby walki z ich statystyk.
 * MMOItems nie daje statystyk mobom ("mmo stats on the armor DO NOT work on MythicMobs", MM wiki mobs-equipment.md),
 * wiec plugin czyta je z przedmiotu (NBT MMOITEMS_*) i daje cialu tak jak graczowi:
 * pancerz i wytrzymalosc = atrybuty vanilla (MMOItems features-stats-stats.md "Armor, Armor Toughness"),
 * zycie = baza + MAX_HEALTH przedmiotow, atak = ATTACK_DAMAGE broni w rece, obrona = wzor MythicLib
 * (config.yml defense-application.natural; MythicLib liczy go tylko graczom - DamageReduction wymaga MMOPlayerData).
 * Symulacja liczy cios tym samym wzorem ({@link #poPancerzu}, {@link #poObronie}), wiec cialo i model daja to samo.
 */
final class Wyposazenie {
  /** Gotowy zestaw roli: przedmioty do zalozenia i liczby (pancerz po calym zestawie, atak broni z reki). */
  record Zestaw(Map<EquipmentSlot, ItemStack> przedmioty, double pancerz, double wytrzymalosc, double obrona,
                double zycie, double atak, String opis) {}

  private Wyposazenie() {}

  static boolean dostepne() {
    var pm = Bukkit.getPluginManager();
    return pm.isPluginEnabled("MMOItems") && pm.isPluginEnabled("MythicLib");
  }

  /**
   * Zestaw z sekcji configu: {@code SLOT: TYP:ID} (sloty Bukkit HAND, OFF_HAND, HEAD, CHEST, LEGS, FEET).
   * Przedmioty losuje MMOItems na zadanym poziomie (skala base+scale*poziom jak u gracza). Skorzane barwi na kolor strony.
   * Bez MMOItems albo bez sekcji: same liczby bazowe (zycie z configu, atak 1 = gola reka).
   */
  static Zestaw zestaw(ConfigurationSection s, int poziom, double zycieBazowe, Color barwa, Logger log) {
    Map<EquipmentSlot, ItemStack> eq = new EnumMap<>(EquipmentSlot.class);
    double pancerz = 0, wytrz = 0, obrona = 0, zycie = zycieBazowe, atak = 1;
    StringBuilder opis = new StringBuilder();
    if (s != null && dostepne()) {
      for (String klucz : s.getKeys(false)) {
        EquipmentSlot slot;
        try { slot = EquipmentSlot.valueOf(klucz.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { log.warning("wyposazenie: nieznany slot " + klucz + " (" + s.getCurrentPath() + ")"); continue; }
        String wpis = s.getString(klucz, "");
        int dw = wpis.indexOf(':');
        Type typ = dw > 0 ? Type.get(wpis.substring(0, dw)) : null;
        ItemStack it = typ == null ? null : MMOItems.plugin.getItem(typ, wpis.substring(dw + 1), poziom, null);
        if (it == null) { log.warning("wyposazenie: MMOItems nie zna " + wpis + " (" + s.getCurrentPath() + "." + klucz + ")"); continue; }
        NBTItem nbt = NBTItem.get(it);
        // bron liczy sie w rece, reszta (zbroja, tarcza w drugiej rece) wszedzie - jak u gracza
        if (slot == EquipmentSlot.HAND) atak = Math.max(1, nbt.getStat("ATTACK_DAMAGE"));
        else {
          pancerz += nbt.getStat("ARMOR");
          wytrz += nbt.getStat("ARMOR_TOUGHNESS");
        }
        obrona += nbt.getStat("DEFENSE");
        zycie += nbt.getStat("MAX_HEALTH");
        bezAtrybutow(it);
        if (barwa != null && it.getItemMeta() instanceof LeatherArmorMeta lm) { lm.setColor(barwa); it.setItemMeta(lm); }
        eq.put(slot, it);
        opis.append(opis.isEmpty() ? "" : ", ").append(wpis.substring(dw + 1));
      }
    }
    return new Zestaw(eq, pancerz, wytrz, obrona, zycie, atak, opis.isEmpty() ? "brak" : opis.toString());
  }

  /**
   * Pusty komponent attribute_modifiers: przedmiot na mobie nie dodaje wlasnych (vanillowych) atrybutow,
   * liczby daje plugin atrybutami ciala (Napad.przygotujMM) ze statystyk MMOItems.
   */
  private static void bezAtrybutow(ItemStack it) {
    it.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.itemAttributes().build());
  }

  /** Obrazenia po obronie (wzor MythicLib z jego configu); bez obrony bez zmian. */
  static double poObronie(double dmg, double obrona) {
    if (obrona <= 1e-6 || dmg <= 0 || !dostepne()) return dmg;
    return DefenseFormula.calculateDamage(false, obrona, dmg);
  }

  /** Obrazenia po pancerzu vanilla 1.21 (CombatRules.getDamageAfterAbsorb) - symulacja, cialo liczy to samo w grze. */
  static double poPancerzu(double dmg, double pancerz, double wytrz) {
    double f = 2 + wytrz / 4;
    double g = Math.max(Math.min(pancerz - dmg / f, 20), pancerz * 0.2);
    return dmg * (1 - g / 25);
  }
}
