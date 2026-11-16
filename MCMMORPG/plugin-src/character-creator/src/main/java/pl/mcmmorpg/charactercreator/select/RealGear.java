package pl.mcmmorpg.charactercreator.select;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.FlagWatcher;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

/**
 * Podglad w scenie wyboru pokazuje TO, CO POSTAC NAPRAWDE NOSI: zbroje i bron z zapisanego
 * ekwipunku profilu MMOProfiles ({@code PlayerProfileImpl#getInventory}), a nie szablon klasy.
 *
 * <p>Uklad tablicy jak {@code PlayerInventory#getContents}: 0-35 plecak (0-8 pasek), 36 buty,
 * 37 nogi, 38 tors, 39 helm, 40 druga reka. Bron = slot paska z {@code getHeldItemSlot}.
 *
 * <p>Wolane PO {@code PreviewDummyManager#applyAppearance} — nadpisuje szablon klasy na tym samym
 * przebraniu (albo na samym nosicielu, gdy przebrania nie ma). Kolejne applyAppearance przy
 * zmianie postaci czysci ekwipunek do szablonu, wiec nic nie przechodzi na nastepna postac.
 * Pusty slot zostaje pusty — postac bez helmu pokazuje glowe ze skorki, bez broni ma pusta reke.
 */
public final class RealGear {

  static final int BOOTS = 36;
  static final int LEGS = 37;
  static final int CHEST = 38;
  static final int HELMET = 39;
  static final int OFF_HAND = 40;

  private RealGear() {
  }

  /** Co nalozyc: [helm, tors, nogi, buty, reka, druga reka]; null = pusto. Czysta funkcja. */
  static ItemStack[] pick(ItemStack[] inventory, int heldSlot) {
    ItemStack[] out = new ItemStack[6];
    if (inventory == null) {
      return out;
    }
    out[0] = at(inventory, HELMET);
    out[1] = at(inventory, CHEST);
    out[2] = at(inventory, LEGS);
    out[3] = at(inventory, BOOTS);
    out[4] = heldSlot >= 0 && heldSlot <= 8 ? at(inventory, heldSlot) : null;
    out[5] = at(inventory, OFF_HAND);
    return out;
  }

  /** @return czy cokolwiek nalozono (false = postac bez ekwipunku — zostaje szablon klasy) */
  public static boolean apply(Player viewer, Entity subject, ItemStack[] inventory, int heldSlot) {
    if (!(subject instanceof LivingEntity living)) {
      return false;
    }
    ItemStack[] gear = pick(inventory, heldSlot);
    boolean any = false;
    for (ItemStack it : gear) {
      any |= it != null;
    }
    Disguise disguise = DisguiseAPI.getDisguise(viewer, living);
    if (disguise == null) {
      disguise = DisguiseAPI.getDisguise(living);
    }
    if (!any) {
      // applyAppearance nie rusza drugiej reki — bez tego tarcza poprzedniej postaci zostalaby.
      if (disguise != null) {
        disguise.getWatcher().setItemInOffHand(null);
      } else if (living.getEquipment() != null) {
        living.getEquipment().setItemInOffHand(null);
      }
      return false;
    }
    if (disguise != null) {
      FlagWatcher w = disguise.getWatcher();
      w.setHelmet(gear[0]);
      w.setChestplate(gear[1]);
      w.setLeggings(gear[2]);
      w.setBoots(gear[3]);
      w.setItemInMainHand(gear[4]);
      w.setItemInOffHand(gear[5]);
      return true;
    }
    EntityEquipment eq = living.getEquipment();
    if (eq == null) {
      return false;
    }
    eq.setHelmet(gear[0]);
    eq.setChestplate(gear[1]);
    eq.setLeggings(gear[2]);
    eq.setBoots(gear[3]);
    eq.setItemInMainHand(gear[4]);
    eq.setItemInOffHand(gear[5]);
    eq.setHelmetDropChance(0f);
    eq.setChestplateDropChance(0f);
    eq.setLeggingsDropChance(0f);
    eq.setBootsDropChance(0f);
    eq.setItemInMainHandDropChance(0f);
    eq.setItemInOffHandDropChance(0f);
    return true;
  }

  private static ItemStack at(ItemStack[] inv, int slot) {
    if (slot < 0 || slot >= inv.length) {
      return null;
    }
    ItemStack it = inv[slot];
    return it == null || it.getType() == Material.AIR || it.getAmount() <= 0 ? null : it.clone();
  }
}
