package plugin.gui.pages;

import plugin.types.EVENT_TYPE;
import plugin.types.PLAYER_ROLE;
import plugin.gui.pages.SimpleProtectUIState.PanelView;
import plugin.gui.pages.SimpleProtectUI.Data;
import plugin.gui.pages.SimpleProtectUIRenderer.RenderScope;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Handles all event processing and state updates for the SimpleProtect UI.
 * This is the "Controller" in the MVC pattern.
 */
public class SimpleProtectUIHandler {

    /**
     * DataEventProcessor
     * Must indicate if it can process an event based on the relevant field of data being non-null
     * Must return the scope of UI to be re-rendered
     */
    public interface DataEventProcessor {
        boolean canProcess(Data data);
        EnumSet<RenderScope> process(Data data, SimpleProtectUIState state);
    }

    /**
     * List of registered processors that can handle any event from the UI
     * List and processors are static because behavior does not change from instance to instance
     * Processors are just logic that mutate state
     */
    public static final List<DataEventProcessor> PROCESSORS = List.of(
        new WorldFilterProcessor(),
        new WorldNameUpdateProcessor(),
        new PlayerSearchProcessor(),
        new UUIDInputProcessor(),
        new GroupChangeProcessor(),
        new GlobalConfigProcessor(),
        new WorldConfigProcessor(),
        new ProtectionToggleProcessor(),
        new PlayerGroupProcessor(),
        new AddUUIDProcessor(),
        new PanelChangeProcessor(),
        new ConfigSyncProcessor()
    );

    /**
     * Instance of the handler contains the SimpleProtectUIState of the player, this binds state to the handler
     */
    private final SimpleProtectUIState uiState;
    private final PlayerListService playerListService;

    public SimpleProtectUIHandler(SimpleProtectUIState uiState) {
        this.uiState = uiState;
        this.playerListService = new PlayerListService();
    }

    /**
     * Main method the handler uses to match the correct Processor to the Data related to the UI Event
     * @param data - Data object that represents UI interactions (events)
     * @return The RenderScope to indicate to the renderer what parts of the UI to update based on state
     */
    public EnumSet<RenderScope> handleEvent(Data data) {
        for (DataEventProcessor p : PROCESSORS) {
            if (p.canProcess(data)) {
                return p.process(data, this.uiState);
            }
        }
        return EnumSet.noneOf(RenderScope.class);
    }

    /**
     * Handle an update to the text box where players search for worlds by name
     */
    private static class WorldFilterProcessor implements DataEventProcessor {
        @Override
        public boolean canProcess(Data data) { return data.worldFilter != null; }

        @Override
        public EnumSet<RenderScope> process(Data data, SimpleProtectUIState state) {
            state.setWorldFilter(data.worldFilter);
            return EnumSet.of(RenderScope.WORLD_LIST);
        }
    }

    /**
     * Handle an update to the text box where players input the name of the world they want a config to manage
     */
    private static class WorldNameUpdateProcessor implements DataEventProcessor {
        @Override
        public boolean canProcess(Data data) { return data.nameWorldUpdate != null; }

        @Override
        public EnumSet<RenderScope> process(Data data, SimpleProtectUIState state) {
            state.setWorldFilter(data.nameWorldUpdate);
            state.setNameForWorld(data.nameWorldUpdate);
            return EnumSet.of(RenderScope.WORLD_LIST);
        }
    }

    /**
     * Handle an update to the text box where players input the name of a player they want to change permissions for
     */
    private static class PlayerSearchProcessor implements DataEventProcessor {
        @Override
        public boolean canProcess(Data data) { return data.playerSearchUpdate != null; }

        @Override
        public EnumSet<RenderScope> process(Data data, SimpleProtectUIState state) {
            state.setPlayerSearch(data.playerSearchUpdate);
            return EnumSet.of(RenderScope.WORLD_PLAYER_SETTINGS);
        }
    }

    /**
     * Handle an update to the text box that accepts raw UUIDs for players that cannot be found by name
     */
    private static class UUIDInputProcessor implements DataEventProcessor {
        @Override
        public boolean canProcess(Data data) { return data.uuidInputUpdate != null; }

        @Override
        public EnumSet<RenderScope> process(Data data, SimpleProtectUIState state) {
            state.setUuidInput(data.uuidInputUpdate);
            return EnumSet.of(RenderScope.WORLD_PLAYER_SETTINGS);
        }
    }

    /**
     * Handle an update to the permission group that is being shown in the config panel
     */
    private static class GroupChangeProcessor implements DataEventProcessor {
        @Override
        public boolean canProcess(Data data) { return data.groupClicked != null; }

        @Override
        public EnumSet<RenderScope> process(Data data, SimpleProtectUIState state) {
            state.setEditPlayerRole(PLAYER_ROLE.valueOf(data.groupClicked.toUpperCase()));
            return EnumSet.of(RenderScope.WORLD_PLAYER_SETTINGS, RenderScope.GROUP_SELECTION_PANEL);
        }
    }

    /**
     * Handle an update to a global config setting
     */
    private static class GlobalConfigProcessor implements DataEventProcessor {
        @Override
        public boolean canProcess(Data data) { return data.globalConfigUpdate != null; }

        @Override
        public EnumSet<RenderScope> process(Data data, SimpleProtectUIState state) {
            switch(data.globalConfigUpdate) {
                case "ToggleGlobalProtection" -> state.toggleGlobalProtection();
                case "ToggleGlobalPlayerNotify" -> state.toggleNotifyPlayer();
                case "ToggleVerboseLogging" -> state.toggleVerboseLogging();
            }
            return EnumSet.noneOf(RenderScope.class);
        }
    }

    /**
     * Handle an update to a world config setting
     */
    private static class WorldConfigProcessor implements DataEventProcessor {
        @Override
        public boolean canProcess(Data data) { return data.worldConfigUpdate != null; }

        @Override
        public EnumSet<RenderScope> process(Data data, SimpleProtectUIState state) {
            switch(data.worldConfigUpdate) {
                case "ToggleWorldProtection" -> state.config().protectionEnabled = !state.config().protectionEnabled;
                case "ToggleWorldNotify" -> state.config().notifyPlayer = !state.config().notifyPlayer;
            }
            return EnumSet.noneOf(RenderScope.class);
        }
    }

    /**
     * Handle an update to the active protections for a world
     */
    private static class ProtectionToggleProcessor implements DataEventProcessor {
        @Override
        public boolean canProcess(Data data) { return data.worldProtectionUpdate != null; }

        @Override
        public EnumSet<RenderScope> process(Data data, SimpleProtectUIState state) {
            EVENT_TYPE toggled = EVENT_TYPE.valueOf(data.worldProtectionUpdate);

            if (state.config().enabledProtections.contains(toggled)) {
                state.config().enabledProtections.remove(toggled);
            } else {
                state.config().enabledProtections.add(toggled);
            }

            return EnumSet.of(RenderScope.WORLD_PROTECTION_SETTINGS);
        }
    }

    /**
     * Handle a player being added to or removed from the active group
     */
    private static class PlayerGroupProcessor implements DataEventProcessor {
        @Override
        public boolean canProcess(Data data) { return data.playerGroupUpdate != null; }

        @Override
        public EnumSet<RenderScope> process(Data data, SimpleProtectUIState state) {
            UUID playerUUID = UUID.fromString(data.playerGroupUpdate);
            Set<UUID> allowedGroup = resolveConfigGroup(state);

            if (allowedGroup.contains(playerUUID)) {
                allowedGroup.remove(playerUUID);
            } else {
                allowedGroup.add(playerUUID);
            }

            return EnumSet.of(RenderScope.WORLD_PLAYER_SETTINGS);
        }
    }

    /**
     * Handle a UUID being added to the active group
     */
    private static class AddUUIDProcessor implements DataEventProcessor {
        @Override
        public boolean canProcess(Data data) { return data.addUuidAction != null; }

        @Override
        public EnumSet<RenderScope> process(Data data, SimpleProtectUIState state) {
            if (validateUUID(state.uuidInput())) {
                UUID playerUUID = UUID.fromString(state.uuidInput());
                Set<UUID> allowedGroup = resolveConfigGroup(state);
                allowedGroup.add(playerUUID);
                return EnumSet.of(RenderScope.WORLD_PLAYER_SETTINGS);
            }
            return EnumSet.noneOf(RenderScope.class);
        }
    }

    /**
     * Handle a change to the active panel
     */
    private static class PanelChangeProcessor implements DataEventProcessor {
        @Override
        public boolean canProcess(Data data) { return data.mainPanelUpdate != null; }

        @Override
        public EnumSet<RenderScope> process(Data data, SimpleProtectUIState state) {
            state.setCurrentWorld("");
            String actionString = data.mainPanelUpdate;

            EnumSet<RenderScope> result = EnumSet.of(RenderScope.MAIN_PANEL, RenderScope.ADMIN_PANEL, RenderScope.WORLD_LIST);

            if (!state.canAdministrate()) {
                state.setPanelView(PanelView.CLEAR);
                return result;
            }

            switch(actionString) {
                case "DefaultWorldBtn" -> {
                    state.setPanelView(PanelView.EDIT_DEFAULT_WORLD);
                    state.syncWorldConfig();
                    result.add(RenderScope.WORLD_PLAYER_SETTINGS);
                }
                case "CreateConfigBtn" -> {
                    state.setPanelView(PanelView.CREATE_NEW_CONFIG);
                    state.syncWorldConfig();
                    result.add(RenderScope.WORLD_PLAYER_SETTINGS);
                }
                case "EditGlobalConfigBtn" -> {
                    state.setPanelView(PanelView.EDIT_GLOBAL_CONFIG);
                    state.syncGlobalSettings();
                }
                case "ExitPanelBtn" -> {
                    state.setPanelView(PanelView.CLEAR);
                }
                default -> {
                    // Clicking a world name from the panel
                    state.setCurrentWorld(actionString);
                    state.setPanelView(PanelView.EDIT_WORLD);
                    state.syncWorldConfig();
                    state.resolvePlayerRole();
                    result.add(RenderScope.WORLD_PLAYER_SETTINGS);
                }
            }
            return result;
        }
    }

    /**
     * Handle an event that writes from the current to saved config or saved to current config (save, load, delete)
     */
    private static class ConfigSyncProcessor implements DataEventProcessor {
        @Override
        public boolean canProcess(Data data) { return data.configSyncAction != null; }

        @Override
        public EnumSet<RenderScope> process(Data data, SimpleProtectUIState state) {
            String syncAction = data.configSyncAction;

            switch(state.panelView()) {
                case EDIT_GLOBAL_CONFIG -> {
                    switch(syncAction) {
                        case "SaveBtn" -> {
                            state.saveGlobalConfig();
                            return EnumSet.of(RenderScope.GLOBAL_SETTINGS);
                        }
                        case "LoadBtn" -> {
                            state.syncGlobalSettings();
                            return EnumSet.of(RenderScope.GLOBAL_SETTINGS);
                        }
                    }
                }
                case EDIT_WORLD -> {
                    switch (syncAction) {
                        case "SaveBtn" -> {
                            state.saveWorldConfig();
                            return EnumSet.of(RenderScope.MAIN_PANEL);
                        }
                        case "LoadBtn" -> {
                            state.syncWorldConfig();
                            return EnumSet.of(RenderScope.MAIN_PANEL);
                        }
                        case "DeleteBtn" -> {
                            state.deleteWorldConfig();
                            state.setPanelView(PanelView.CLEAR);
                            state.setWorldFilter("");
                            state.setCurrentWorld("");
                            return EnumSet.of(RenderScope.MAIN_PANEL, RenderScope.WORLD_LIST);
                        }
                    }
                }
                case CREATE_NEW_CONFIG -> {
                    if (!state.canAdministrate()) {
                        return handlePermissionFailure(state);
                    }
                    switch (syncAction) {
                        case "SaveBtn" -> {
                            state.setCurrentWorld(state.nameForWorld());
                            state.setNameForWorld("");
                            state.setPanelView(PanelView.EDIT_WORLD);
                            state.saveWorldConfig();
                            return EnumSet.of(RenderScope.MAIN_PANEL, RenderScope.WORLD_LIST);
                        }
                        case "LoadBtn" -> {
                            state.syncWorldConfig();
                            return EnumSet.of(RenderScope.MAIN_PANEL);
                        }
                    }
                }
                case EDIT_DEFAULT_WORLD -> {
                    if (!state.canAdministrate()) {
                        return handlePermissionFailure(state);
                    }
                    switch (syncAction) {
                        case "SaveBtn" -> {
                            state.saveWorldConfig();
                            return EnumSet.of(RenderScope.MAIN_PANEL);
                        }
                        case "LoadBtn" -> {
                            state.syncWorldConfig();
                            return EnumSet.of(RenderScope.MAIN_PANEL);
                        }
                    }
                }
            }
            // Default to no permission, clear panel etc
            return handlePermissionFailure(state);
        }
    }

    /**
     * Helper method that resets state and forces a re-render of the panel if the player no longer has admin
     * @param state - state to be modified (reset)
     * @return - The RenderScope for all UI elements that should be reset
     */
    private static EnumSet<RenderScope> handlePermissionFailure(SimpleProtectUIState state) {
        state.setPanelView(PanelView.CLEAR);
        state.setWorldFilter("");
        state.setCurrentWorld("");
        return EnumSet.of(RenderScope.MAIN_PANEL, RenderScope.WORLD_LIST, RenderScope.ADMIN_PANEL);
    }

    /**
     * Refresh player lists asynchronously
     */
    public void refreshPlayerLists(java.util.function.Consumer<Runnable> worldThreadExecutor, BiConsumer<PageResult<PlayerListService.PlayerEntry>, Boolean> onComplete) {
        long revision = uiState.getAndIncrementRevisionNumber();

        // Refresh allowed players
        playerListService.buildAllowedPlayersPageAsync(
                uiState.config(),
                uiState.editPlayerRole(),
                uiState.playerSearch(),
                uiState.currentAllowedPage(),
                10, // PAGE_ENTRIES
                dbResult -> {
                    if (revision != uiState.revisionNumber()) return;
                    worldThreadExecutor.accept(() -> onComplete.accept(dbResult, true));
                }
        ).thenAccept(cacheResult -> {
            if (revision != uiState.revisionNumber()) return;
            worldThreadExecutor.accept(() -> onComplete.accept(cacheResult, true));
        });

        // Refresh disallowed players
        playerListService.buildDisallowedPlayersPageAsync(
                uiState.config(),
                uiState.playerSearch(),
                uiState.currentDisallowedPage(),
                10, // PAGE_ENTRIES
                dbResult -> {
                    if (revision != uiState.revisionNumber()) return;
                    worldThreadExecutor.accept(() -> onComplete.accept(dbResult, false));
                }
        ).thenAccept(cacheResult -> {
            if (revision != uiState.revisionNumber()) return;
            worldThreadExecutor.accept(() -> onComplete.accept(cacheResult, false));
        });
    }

    /**
     * Validate UUID format
     */
    private static boolean validateUUID(String uuid) {
        if (uuid == null) {
            return false;
        }
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Map the selected player role to the actual set of player UUIDs from the config
     * @param state - provides the currently edited player role (selected role in the panel)
     * @return - set of UUID of players with the current role
     */
    private static Set<UUID> resolveConfigGroup(SimpleProtectUIState state) {
        return switch(state.editPlayerRole()) {
            case MODERATOR -> state.config().moderators;
            case ADMINISTRATOR -> state.config().administrators;
            default -> state.config().members;
        };
    }
}