package plugin;

import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.handlers.game.GamePacketHandler;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import plugin.cache.PacketLayerCache;
import plugin.types.EVENT_TYPE;

import java.util.List;
import java.util.UUID;

import static com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil.getLogger;

public class BuildToolPacketFilter implements PacketFilter {

    @Override
    public boolean test(PacketHandler packetHandler, Packet packet) {
        // All possible tool interaction packet types
        List<String> toolActions = List.of("BuilderToolLineAction", "BuilderToolOnUseInteraction",
                "BuilderToolPasteClipboard");
        if (!toolActions.contains(packet.getClass().getSimpleName())) {
            return false;
        }

        if (!(packetHandler instanceof GamePacketHandler gameHandler)) {
            return false;
        }

        // Get the player associated with this packet
        PlayerRef playerRef = gameHandler.getPlayerRef();
        UUID uuid = playerRef.getUuid();
        boolean hasBypass = PermissionsModule.get().hasPermission(uuid,
                ConfigState.get().getPermission());

        if (hasBypass) {
            if (ConfigState.get().isVerbose()) getLogger().info(String.format(
                    "Player with UUID %s was allowed to use Builder Tool, has global bypass permission %s",
                    uuid, ConfigState.get().getPermission()));
            return false;
        }

        String worldName = PacketLayerCache.get(uuid).world();
        return ProtectionUtil.ShouldProtect(playerRef, worldName, EVENT_TYPE.BUILDER_TOOL);
    }}
