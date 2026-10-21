package pl.mcmmorpg.mapa;

import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;

interface PanelMost {
  void show(Player player, Component title, List<ItemStack> items);
  void refresh(Player player, Component title, List<ItemStack> items);
  void hide(Player player);
  void close();
}
