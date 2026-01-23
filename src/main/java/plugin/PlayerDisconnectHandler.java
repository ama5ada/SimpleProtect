package plugin;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import plugin.cache.PacketLayerCache;

import java.util.UUID;

public class PlayerDisconnectHandler {
    public static void HandlePlayerDisconnect(PlayerDisconnectEvent event) {
        UUID playerId = event.getPlayerRef().getUuid();
        PacketLayerCache.remove(playerId);
    }
}
