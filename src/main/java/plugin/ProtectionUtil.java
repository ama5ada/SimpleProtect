package plugin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.EntitySnapshot;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.types.EVENT_TYPE;
import plugin.types.SimpleProtectWorldConfig;

import java.util.Set;
import java.util.UUID;

import static com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil.getLogger;

public class ProtectionUtil {
    /**
     * Event driven method
     * @param player - Player that did an action
     * @param event - Type of event that is occurring
     * @return - T/F Should protection be active (basically is the event cancelled)
     */
    public static boolean ShouldProtect(Player player, EVENT_TYPE event) {
        boolean globalProtection = ConfigState.get().isProtected();
        boolean verboseLogging = ConfigState.get().isVerbose();

        // Shortcut if globalProtection is off
        if (!globalProtection) {
            if (verboseLogging) {
                getLogger().info(
                        String.format("[Simple Protect] Global protection turned off, allowing %s", event));
            }
            return false;
        }

        // Guard to make sure the player is valid
        if (player == null) {
            getLogger().warning(
                    String.format(
                            "[Simple Protect] Event %s occurred but had no valid player" +
                                    "\nUsing global protection setting %s",
                            event, true));
            return true;
        }

        // Get playerName once guaranteed to be valid
        String playerName = player.getDisplayName();
        if (player.getWorld() == null) {
            getLogger().warning(
                    String.format(
                            "[Simple Protect] Event %s occurred triggered by player %s but had no valid world." +
                                    "\nUsing global protection setting %s",
                            event, playerName, true
                    )
            );
            return true;
        }

        // Get worldName once guaranteed to be valid
        String worldName = player.getWorld().getName();
        SimpleProtectWorldConfig currentConfig = ConfigState.get().getWorldProtectionConfig(worldName);
        boolean worldProtection = currentConfig.protectionEnabled;
        Set<EVENT_TYPE> enabledProtections = currentConfig.enabledProtections;

        // Shortcut if world protection is off
        if (!worldProtection) {
            if (verboseLogging) {
                getLogger().info(
                        String.format("[Simple Protect] World %s has protection turned off, allowing event %s for player %s",
                                worldName, event, playerName));
            }
            return false;
        }

        // Shortcut if the world does not protect against the current event
        if (!(enabledProtections.contains(EVENT_TYPE.ALL) || enabledProtections.contains(event))) {
            if (verboseLogging) {
                getLogger().info(
                        String.format("[Simple Protect] World %s does not protect against event %s, allowing it for player %s",
                                worldName, event, playerName));
            }
            return false;
        }

        // Guard against null ref
        Ref<EntityStore> ref = player.getReference();
        if (ref == null) {
            getLogger().warning(
                    String.format(
                            "[Simple Protect] Protected Event %s occurred triggered by player %s in world %s but had no valid Entity Store Ref to check permissions." +
                                    "\nUsing world protection setting %s",
                            event, playerName, worldName, true
                    )
            );
            return true;
        }

        // Get EntityStore once ref is guaranteed to be valid
        Store<EntityStore> store = ref.getStore();
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null || !playerRef.isValid()) {
            getLogger().warning(
                    String.format(
                            "[Simple Protect] Protected Event %s occurred triggered by player %s in world %s but had no valid Player Ref to check permissions." +
                                    "\nUsing world protection setting %s",
                            event, playerName, worldName, true
                    )
            );
            return true;
        }

        // Get playerUUID once guaranteed to be valid
        UUID playerUUID = playerRef.getUuid();

        // See if the player has the global bypass permission
        boolean globalBypass = player.hasPermission(ConfigState.get().getPermission());
        if (globalBypass) {
            if (verboseLogging) {
                getLogger().info(
                        String.format("[Simple Protect] Protected Event %s occurred triggered by player %s:%s in world %s." +
                                "\nPlayer has global bypass permission, allowing event.",
                                event, playerName, playerUUID, worldName));
            }
            return false;
        }

        // See if the player has a local bypass permission
        boolean localBypass = currentConfig.allowedPlayers.contains(playerUUID);
        if (localBypass) {
            if (verboseLogging) {
                getLogger().info(
                        String.format("[Simple Protect] Protected Event %s occurred triggered by player %s:%s in world %s." +
                                "\nPlayer has local bypass permission, allowing event.",
                                event, playerName, playerUUID, worldName));
            }
            return false;
        }

        // After all checks, the default is to protect
        if (verboseLogging) {
            getLogger().info(
                    String.format("[Simple Protect] Protected Event %s occurred triggered by player %s:%s in world %s" +
                            "\nPlayer has no permission, cancelling event.",
                            event, playerName, playerUUID, worldName));
        }

        if (currentConfig.notifyPlayer) {
            playerRef.sendMessage(Message.raw(
                    String.format("[Simple Protect] %s is disabled.", event)
            ));
        }
        return true;
    }

    /**
     * Packet handler method
     * @param playerRef - Ref to player that did the action
     * @param worldName - Name of the world an action is occurring in
     * @param event - Type of event that is occurring
     * @return - T/F Should protection be active (basically is the event cancelled)
     */
    public static boolean ShouldProtect(PlayerRef playerRef, String worldName, EVENT_TYPE event) {
        boolean globalProtection = ConfigState.get().isProtected();
        boolean verboseLogging = ConfigState.get().isVerbose();

        // Shortcut if globalProtection is off
        if (!globalProtection) {
            if (verboseLogging) {
                getLogger().info(
                        String.format("[Simple Protect] Global protection turned off, allowing %s", event));
            }
            return false;
        }

        UUID playerUUID = playerRef.getUuid();
        // Guard against cache failing
        if (worldName == null) {
            if (verboseLogging) getLogger().info(String.format(
                    "[Simple Protect] Player with UUID %s was prevented from %s because world name was null",
                    playerUUID, event
            ));
            return true;
        }

        // Load protection config from worldName after world grabbed from cache
        SimpleProtectWorldConfig currentConfig = ConfigState.get().getWorldProtectionConfig(worldName);
        boolean localProtection  = currentConfig.protectionEnabled;
        Set<EVENT_TYPE> enabledProtections = currentConfig.enabledProtections;

        // See if local protection is enabled on the world
        if (!localProtection) {
            if (verboseLogging) getLogger().info(String.format(
                    "[Simple Protect] World %s has protection set to %s allowing Event %s for player with UUID %s",
                    worldName, false, event, playerUUID
            ));
            return false;
        }

        // See if the local protections apply to the specific event
        if (!(enabledProtections.contains(EVENT_TYPE.ALL) || enabledProtections.contains(event))) {
            if (verboseLogging) {
                getLogger().info(
                        String.format("[Simple Protect] World %s does not protect against Event %s, allowing it for player with UUID %s",
                                worldName, event, playerUUID));
            }
            return false;
        }

        boolean localBypass = currentConfig.allowedPlayers.contains(playerUUID);

        // See if the player has permissions to bypass local rules
        if (localBypass) {
            if (verboseLogging) {
                getLogger().info(
                        String.format("[Simple Protect] Player %s has local bypass on World %s, allowing Event %s",
                                playerUUID, worldName, event));
            }
        }

        // After all checks, the default is to protect
        if (verboseLogging) {
            getLogger().info(
                    String.format("[Simple Protect] Protected Event %s occurred triggered by player %s in world %s" +
                                    "\nPlayer has no permission, cancelling event.",
                            event, playerUUID, worldName));
        }

        if (currentConfig.notifyPlayer) {
            playerRef.sendMessage(Message.raw(
                    String.format("[Simple Protect] %s is disabled.", event)
            ));
        }
        return true;
    }
}
