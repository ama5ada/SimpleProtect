package plugin;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import plugin.cache.PacketLayerCache;

import java.util.UUID;

public class PlayerReadyHandler {
    public static void HandlePlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        PlayerRef playerRef = player.getReference().getStore().getComponent(player.getReference(), PlayerRef.getComponentType());
        UUID playerId = playerRef.getUuid();
        String worldName = player.getWorld().getName();
        PacketLayerCache.put(playerId, worldName);
    }
}
