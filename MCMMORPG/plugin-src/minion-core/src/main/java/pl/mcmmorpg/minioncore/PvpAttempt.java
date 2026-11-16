package pl.mcmmorpg.minioncore;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public record PvpAttempt(UUID ownerId, Location ownerLocation, Entity minion, Player target) { }
