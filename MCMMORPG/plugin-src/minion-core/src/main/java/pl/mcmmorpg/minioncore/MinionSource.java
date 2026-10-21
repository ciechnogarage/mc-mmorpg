package pl.mcmmorpg.minioncore;

import java.util.UUID;
import org.bukkit.entity.Entity;

record MinionSource(UUID ownerId, UUID groupId, Entity rootEntity, boolean provisional) { }
