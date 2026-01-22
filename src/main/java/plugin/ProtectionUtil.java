package plugin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import plugin.types.EVENT_TYPE;
import plugin.types.SimpleProtectWorldConfig;
import java.util.UUID;

import static com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil.getLogger;

public class ProtectionUtil {
    public static boolean ShouldProtect(Player player, String worldName, EVENT_TYPE event) {
        boolean globalProtection = ConfigState.get().isProtected();
        boolean verboseLogging = ConfigState.get().isVerbose();
        // First make sure the player is valid
        if (player != null) {
            // Get relevant player info once guaranteed to be valid
            String playerName = player.getDisplayName();
            UUID playerUUID = player.getUuid();
            // Make sure protection is on
            if (globalProtection) {
                // Then get the individual world config
                SimpleProtectWorldConfig currentConfig = ConfigState.get().getWorldProtectionConfig(worldName);
                // Make sure the world should be protected
                if (currentConfig.protectionEnabled) {
                    // Make sure the event is protected from on the world
                    if (currentConfig.enabledProtections.contains(EVENT_TYPE.ALL) || currentConfig.enabledProtections.contains(event)) {
                        // Make a decision based on player perms
                        // If the player has the bypass perm or is allowed explicitly on this world
                        boolean globalBypass = player.hasPermission(ConfigState.get().getPermission());
                        boolean localBypass = currentConfig.allowedPlayers.contains(playerUUID);
                        if (globalBypass || localBypass) {
                            if (verboseLogging) getLogger().info(String.format(
                                    "%s:%s was allowed to %s on world %s\nGlobal Bypass : %s\n Local Bypass : %s",
                                    playerName, playerUUID, event, worldName, globalBypass, localBypass
                            ));
                            return false;
                        } else {
                            // Player failed both permission checks
                            if (verboseLogging) getLogger().info(String.format(
                                    "%s:%s was prevented from %s on world %s",
                                    playerName, playerUUID, event, worldName));
                            if (ConfigState.get().notifyPlayer() && currentConfig.notifyPlayer) {
                                player.sendMessage(Message.raw(
                                        String.format("[Simple Protect] %s is disabled.", event)
                                ));
                                return true;
                            }
                        }
                    } else {
                        if (verboseLogging) getLogger().info(String.format(
                                "%s:%s was allowed to %s, event is allowed through Simple Protect for world %s",
                                playerName, playerUUID, event, worldName
                        ));
                    }
                } else {
                    if (verboseLogging) getLogger().info(String.format(
                            "%s:%s was allowed to %s, Simple Protect is turned off for world %s",
                            playerName, playerUUID, event, worldName
                    ));
                }
            } else {
                if (verboseLogging) getLogger().info(String.format(
                        "%s:%s was allowed to %s, Simple Protect is globally off",
                        playerName, playerUUID, event));
            }
        } else {
            getLogger().warning(
                    String.format(
                            "[Simple Protect] Event %s occurred on world %s but had no valid player",
                            event, worldName));
        }
        return false;
    }
}
