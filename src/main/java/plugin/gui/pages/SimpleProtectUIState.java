package plugin.gui.pages;

import plugin.ConfigState;
import plugin.types.PLAYER_ROLE;
import plugin.types.EVENT_TYPE;
import plugin.config.SimpleProtectWorldConfig;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles all UI rendering for the SimpleProtect UI.
 * This is the "Model" in the MVC pattern.
 */
public class SimpleProtectUIState {
    public enum PanelView {
        CLEAR,
        EDIT_WORLD,
        EDIT_DEFAULT_WORLD,
        CREATE_NEW_CONFIG,
        EDIT_GLOBAL_CONFIG
    }

    private final UUID playerUUID;

    private PanelView currentPanelView = PanelView.CLEAR;
    private PLAYER_ROLE editPlayerRole = PLAYER_ROLE.MEMBER;
    private PLAYER_ROLE currentPlayerRole = PLAYER_ROLE.MEMBER;
    private String currentWorld = "";
    private String worldFilter = "";
    private String nameForWorld = "";

    private String playerSearch = "";
    private String uuidInput = "";

    private long revisionNumber = 0;

    private int currentAllowedPage = 0;
    private int currentDisallowedPage = 0;

    private boolean globalProtection;
    private boolean verboseLogging;
    private boolean notifyPlayer;

    private SimpleProtectWorldConfig currentConfig;

    public SimpleProtectUIState(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public void syncGlobalSettings() {
        globalProtection = ConfigState.get().isProtected();
        verboseLogging = ConfigState.get().isVerbose();
        notifyPlayer = ConfigState.get().notifyPlayer();
    }

    public void saveGlobalConfig() {
        if (canAdministrate()) {
            ConfigState.get().updateGlobalConfig(
                    globalProtection,
                    notifyPlayer,
                    verboseLogging
            );
        }
    }

    public void syncWorldConfig() {
        if (currentPanelView == PanelView.EDIT_DEFAULT_WORLD
                || currentPanelView == PanelView.CREATE_NEW_CONFIG) {
            currentConfig = new SimpleProtectWorldConfig(
                    ConfigState.get().getDefaultWorldConfig()
            );
        } else {
            currentConfig = new SimpleProtectWorldConfig(
                    ConfigState.get().getWorldProtectionConfig(currentWorld)
            );
        }
    }

    public void saveWorldConfig() {
        if (!canAdministrate()) return;

        if (currentPanelView == PanelView.EDIT_DEFAULT_WORLD) {
            ConfigState.get().updateDefaultWorldConfig(currentConfig);
        } else {
            ConfigState.get().setWorldProtectionConfig(currentWorld, currentConfig);
        }
    }

    public void deleteWorldConfig() {
        if (canAdministrate()) {
            ConfigState.get().deleteWorldProtectionConfig(currentWorld);
        }
    }

    public void resolvePlayerRole() {
        if (currentConfig.owner != null && currentConfig.owner.equals(playerUUID)) {
            currentPlayerRole = PLAYER_ROLE.OWNER;
        }
        else if (PermissionsModule.get().hasPermission(
                playerUUID,
                ConfigState.get().getAdministratePermission()
        )) {
            currentPlayerRole = PLAYER_ROLE.OWNER;
        } else if (currentConfig.administrators.contains(playerUUID)) {
            currentPlayerRole = PLAYER_ROLE.ADMINISTRATOR;
        } else if (currentConfig.moderators.contains(playerUUID)) {
            currentPlayerRole = PLAYER_ROLE.MODERATOR;
        } else {
            currentPlayerRole = PLAYER_ROLE.MEMBER;
        }
    }

    public Set<UUID> getActiveRoleGroup() {
        return switch (currentPlayerRole) {
            case ADMINISTRATOR -> currentConfig.administrators;
            case MODERATOR -> currentConfig.moderators;
            default -> currentConfig.members;
        };
    }

    public void toggleProtection(EVENT_TYPE type) {
        if (currentConfig.enabledProtections.contains(type)) {
            currentConfig.enabledProtections.remove(type);
        } else {
            currentConfig.enabledProtections.add(type);
        }
    }

    public EnumSet<EVENT_TYPE> disabledProtections() {
        return EnumSet.complementOf(currentConfig.enabledProtections);
    }

    public boolean canAdministrate() {
        return PermissionsModule.get()
                .hasPermission(playerUUID, ConfigState.get().getAdministratePermission());
    }

    public PanelView panelView() { return currentPanelView; }
    public void setPanelView(PanelView view) { this.currentPanelView = view; }

    public PLAYER_ROLE currentPlayerRole() { return currentPlayerRole; }
    public void setCurrentPlayerRole(PLAYER_ROLE role) { this.currentPlayerRole = role; }

    public PLAYER_ROLE editPlayerRole() { return editPlayerRole; }
    public void setEditPlayerRole(PLAYER_ROLE role) { this.editPlayerRole = role; }

    public String currentWorld() { return currentWorld; }
    public void setCurrentWorld(String world) { this.currentWorld = world; }

    public String worldFilter() { return worldFilter; }
    public void setWorldFilter(String filter) { this.worldFilter = filter; }

    public String nameForWorld() { return nameForWorld; }
    public void setNameForWorld(String nameForWorld) { this.nameForWorld = nameForWorld; }

    public String playerSearch() { return playerSearch; }
    public void setPlayerSearch(String search) { this.playerSearch = search; }

    public String uuidInput() { return uuidInput; }
    public void setUuidInput(String uuid) { this.uuidInput = uuid; }

    public long revisionNumber() { return this.revisionNumber; }
    public long getAndIncrementRevisionNumber() {
        this.revisionNumber++;
        return this.revisionNumber;
    }

    public int currentAllowedPage() { return currentAllowedPage; }
    public void setCurrentAllowedPage(int pageNumber) { this.currentAllowedPage = pageNumber; }
    public void incrementCurrentAllowedPage() { this.currentAllowedPage++; }
    public void decrementCurrentAllowedPage() { this.currentAllowedPage--; }

    public int currentDisallowedPage() { return currentDisallowedPage; }
    public void setCurrentDisallowedPage(int pageNumber) { this.currentDisallowedPage = pageNumber; }
    public void incrementCurrentDisallowedPage() { this.currentDisallowedPage++; }
    public void decrementCurrentDisallowedPage() { this.currentDisallowedPage--; }

    public SimpleProtectWorldConfig config() { return currentConfig; }

    public boolean globalProtection() { return globalProtection; }
    public boolean notifyPlayer() { return notifyPlayer; }
    public boolean verboseLogging() { return verboseLogging; }

    public void toggleGlobalProtection() { globalProtection = !globalProtection; }
    public void toggleNotifyPlayer() { notifyPlayer = !notifyPlayer; }
    public void toggleVerboseLogging() { verboseLogging = !verboseLogging; }
}
