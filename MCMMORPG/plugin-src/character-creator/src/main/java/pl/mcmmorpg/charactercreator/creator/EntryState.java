package pl.mcmmorpg.charactercreator.creator;

import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/**
 * Stan gracza Z MOMENTU WEJSCIA do kreatora: ekwipunek, tryb gry i miejsce, z ktorego
 * przyszedl. Wszystko, co kreator nadpisuje i co trzeba mu oddac.
 *
 * <p>Wczesniej to zylo WYLACZNIE w pamieci procesu, w polu sesji. Skutek byl dokladnie taki,
 * jak to brzmi: {@code /stop} albo crash z graczem w kreatorze kasowal jego plecak na stale,
 * bo stash nie mial gdzie przetrwac. Ten rekord jest serializowany na dysk przy wejsciu i
 * kasowany dopiero po udanym wyjsciu.
 */
public record EntryState(
    UUID player,
    ItemStack[] inventory,
    ItemStack[] armor,
    ItemStack offHand,
    GameMode gameMode,
    Location origin,
    long savedAt
) {
}
