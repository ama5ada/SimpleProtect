package plugin.gui.pages;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import plugin.ConfigState;
import plugin.types.EVENT_TYPE;
import plugin.gui.pages.SimpleProtectUIState.PanelView;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Handles all UI rendering for the SimpleProtect UI.
 * This is the "View" in the MVC pattern.
 */
public class SimpleProtectUIRenderer {
    public enum RenderScope {
        WORLD_LIST,
        ADMIN_PANEL,
        MAIN_PANEL,
        GLOBAL_SETTINGS,
        WORLD_SETTINGS,
        UUID_BUTTON,
        WORLD_PROTECTION_SETTINGS,
        WORLD_PLAYER_SETTINGS,
        GROUP_SELECTION_PANEL
    }

    private final SimpleProtectUIState uiState;

    // Event type constants
    private static final String PANEL_CLICK = "PanelBtnClick";
    private static final String CONFIG_ACTION = "ConfigBtnClick";
    private static final String GLOBAL_CONFIG_UPDATE = "GlobalConfigUpdate";
    private static final String WORLD_CONFIG_UPDATE = "WorldConfigUpdate";
    private static final String PROTECTION_UPDATE = "ProtectionUpdate";
    private static final String PLAYER_ACTION = "PlayerClick";
    private static final String UUID_ACTION = "UUIDClick";
    private static final String GROUP_ACTION = "GroupClick";

    public SimpleProtectUIRenderer(SimpleProtectUIState uiState) {
        this.uiState = uiState;
    }

    /**
     * Main render dispatcher - routes render requests to appropriate methods.
     * This is the central rendering method called by the SimpleProtectUI, connects the Handler RenderScopes to rendering.
     */
    public void render(RenderScope scope, UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        switch (scope) {
            case MAIN_PANEL -> renderMainConfigPanel(uiCommandBuilder, uiEventBuilder);
            case WORLD_LIST -> renderWorldListPanel(uiCommandBuilder, uiEventBuilder);
            case ADMIN_PANEL -> updateAdminPanelHighlights(uiCommandBuilder);
            case GLOBAL_SETTINGS -> updateGlobalConfigPanel(uiCommandBuilder);
            case WORLD_SETTINGS -> updateWorldConfigPanel(uiCommandBuilder);
            case WORLD_PROTECTION_SETTINGS -> renderProtectionsPanel(uiCommandBuilder, uiEventBuilder);
            case WORLD_PLAYER_SETTINGS -> {
                updateGroupSelectionPanel(uiCommandBuilder);
                // Update input fields
                uiCommandBuilder.set("#PlayerSearchInput.Value", uiState.playerSearch());
                uiCommandBuilder.set("#UUIDInput.Value", uiState.uuidInput());
                // Validate and update UUID button state
                boolean isValid = validateUUIDFormat(uiState.uuidInput());
                updateUuidInputButton(uiCommandBuilder, isValid);
            }
            case GROUP_SELECTION_PANEL -> updateGroupSelectionPanel(uiCommandBuilder);
            case UUID_BUTTON -> {
                boolean isValid = validateUUIDFormat(uiState.uuidInput());
                updateUuidInputButton(uiCommandBuilder, isValid);
            }
        }
    }

    /**
     * Build the initial static UI structure
     */
    public void buildInitialUI(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        // Append static UI (containers, static labels, inputs)
        uiCommandBuilder.append("SimpleProtectUI.ui");

        // Bind search input
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#WorldSearchInput",
                EventData.of("@WorldSearchInput", "#WorldSearchInput.Value"), false);
    }

    /**
     * Render the world list panel
     */
    public void renderWorldListPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.clear("#WorldList");

        String[] worldNames = ConfigState.get().getWorldNames();
        if (!uiState.worldFilter().isBlank()) {
            String search = uiState.worldFilter().toLowerCase();
            worldNames = Arrays.stream(worldNames)
                    .filter(w -> w.toLowerCase().contains(search))
                    .toArray(String[]::new);
        }

        for (int i = 0; i < worldNames.length; i++) {
            String world = worldNames[i];
            uiCommandBuilder.append("#WorldList", "WorldButton.ui");
            uiCommandBuilder.set("#WorldList[" + i + "].Text", world);

            if (worldNames[i].equals(uiState.currentWorld())) {
                String backgroundColor = "#263047CC";
                if (uiState.panelView() == PanelView.CREATE_NEW_CONFIG) {
                    backgroundColor = "#FF0000CC";
                }
                uiCommandBuilder.set("#WorldList[" + i + "].Background", backgroundColor);
            }

            // Bind click events for new buttons
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WorldList[" + i + "]",
                    EventData.of(PANEL_CLICK, world), false);
        }
    }

    /**
     * Render the main config panel based on current view
     */
    public void renderMainConfigPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.clear("#ConfigInfoBody");
        uiCommandBuilder.clear("#ExitPlaceholder");

        switch (uiState.panelView()) {
            case CLEAR -> {
                uiCommandBuilder.set("#InfoTitle.Text", " ");
                uiCommandBuilder.append("#ConfigInfoBody", "ModInfo.ui");
            }
            case EDIT_WORLD -> {
                uiCommandBuilder.set("#InfoTitle.Text",
                        String.format("Editing config for world : %s", uiState.currentWorld()));
                renderWorldEditPanel(uiCommandBuilder, uiEventBuilder);
            }
            case EDIT_DEFAULT_WORLD -> {
                uiCommandBuilder.set("#InfoTitle.Text", "Editing the Default World config");
                renderDefaultEditPanel(uiCommandBuilder, uiEventBuilder);
            }
            case CREATE_NEW_CONFIG -> {
                uiCommandBuilder.set("#InfoTitle.Text", "Create New World Config");
                renderConfigCreatePanel(uiCommandBuilder, uiEventBuilder);
            }
            case EDIT_GLOBAL_CONFIG -> {
                uiCommandBuilder.set("#InfoTitle.Text", "Edit Global Config");
                renderEditGlobalConfigPanel(uiCommandBuilder, uiEventBuilder);
            }
        }

        uiCommandBuilder.set("#WorldSearchInput.Value", uiState.worldFilter());

        if (uiState.panelView() != PanelView.CLEAR) {
            uiCommandBuilder.append("#ExitPlaceholder", "ExitPanelButton.ui");
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ExitPanelButton",
                    EventData.of(PANEL_CLICK, "ExitPanelBtn"), false);
        }
    }

    /**
     * Render the protections panel (enabled/disabled protections)
     */
    public void renderProtectionsPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.clear("#EnabledProtections");
        uiCommandBuilder.clear("#DisabledProtections");
        EnumSet<EVENT_TYPE> disabled = EnumSet.complementOf(uiState.config().enabledProtections);

        int i = 0;
        for (EVENT_TYPE included : uiState.config().enabledProtections) {
            uiCommandBuilder.append("#EnabledProtections", "EventButton.ui");
            uiCommandBuilder.set("#EnabledProtections[" + i + "].Text", included.toString());
            uiCommandBuilder.set("#EnabledProtections[" + i + "].Background", "#26304719");

            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#EnabledProtections[" + i + "]",
                    EventData.of(PROTECTION_UPDATE, included.toString()), false);
            i++;
        }

        i = 0;
        for (EVENT_TYPE excluded : disabled) {
            uiCommandBuilder.append("#DisabledProtections", "EventButton.ui");
            uiCommandBuilder.set("#DisabledProtections[" + i + "].Text", excluded.toString());
            uiCommandBuilder.set("#DisabledProtections[" + i + "].Background", "#FF000019");

            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#DisabledProtections[" + i + "]",
                    EventData.of(PROTECTION_UPDATE, excluded.toString()), false);
            i++;
        }
    }

    /**
     * Render allowed players panel
     */
    public void renderAllowedPlayersPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder,
                                          PageResult<PlayerListService.PlayerEntry> page) {
        uiCommandBuilder.clear("#AllowedPlayers");
        List<PlayerListService.PlayerEntry> entries = page.items();

        if (entries.isEmpty()) {
            uiCommandBuilder.append("#AllowedPlayers", "PlayerButton.ui");
            uiCommandBuilder.set("#AllowedPlayers[0].Text", "No players");
            uiCommandBuilder.set("#AllowedPlayers[0].Disabled", true);
        } else {
            for (int i = 0; i < entries.size(); i++) {
                PlayerListService.PlayerEntry entry = entries.get(i);

                uiCommandBuilder.append("#AllowedPlayers", "PlayerButton.ui");
                uiCommandBuilder.set("#AllowedPlayers[" + i + "].Text", entry.displayName());
                uiCommandBuilder.set("#AllowedPlayers[" + i + "].Background", "#33FF0019");

                uiEventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#AllowedPlayers[" + i + "]",
                        EventData.of(PLAYER_ACTION, entry.uuid().toString()),
                        false
                );
            }
        }
    }

    /**
     * Render disallowed players panel
     */
    public void renderDisallowedPlayersPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder,
                                             PageResult<PlayerListService.PlayerEntry> page) {
        uiCommandBuilder.clear("#DisallowedPlayers");
        List<PlayerListService.PlayerEntry> entries = page.items();

        if (entries.isEmpty()) {
            uiCommandBuilder.append("#DisallowedPlayers", "PlayerButton.ui");
            uiCommandBuilder.set("#DisallowedPlayers[0].Text", "No players");
            uiCommandBuilder.set("#DisallowedPlayers[0].Disabled", true);
        } else {
            for (int i = 0; i < entries.size(); i++) {
                PlayerListService.PlayerEntry entry = entries.get(i);

                uiCommandBuilder.append("#DisallowedPlayers", "PlayerButton.ui");
                uiCommandBuilder.set("#DisallowedPlayers[" + i + "].Text", entry.displayName());
                uiCommandBuilder.set("#DisallowedPlayers[" + i + "].Background", "#FF000019");

                uiEventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#DisallowedPlayers[" + i + "]",
                        EventData.of(PLAYER_ACTION, entry.uuid().toString()),
                        false
                );
            }
        }
    }

    /**
     * Render admin panel (called once during initial build)
     */
    public void renderAdminPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.append("#LeftColumnPanel", "AdminConfigOptions.ui");
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#DefaultWorldBtn",
                EventData.of(PANEL_CLICK, "DefaultWorldBtn"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CreateConfigBtn",
                EventData.of(PANEL_CLICK, "CreateConfigBtn"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#EditGlobalConfigBtn",
                EventData.of(PANEL_CLICK, "EditGlobalConfigBtn"), false);
    }

    /**
     * Update admin panel button highlights
     */
    public void updateAdminPanelHighlights(UICommandBuilder uiCommandBuilder) {
        // Only update if admin panel exists
        if (!uiState.canAdministrate()) {
            return;
        }

        uiCommandBuilder.set("#DefaultWorldBtn.Background", "#00000000");
        uiCommandBuilder.set("#CreateConfigBtn.Background", "#00000000");
        uiCommandBuilder.set("#EditGlobalConfigBtn.Background", "#00000000");

        switch(uiState.panelView()) {
            case EDIT_DEFAULT_WORLD -> uiCommandBuilder.set("#DefaultWorldBtn.Background", "#263047CC");
            case CREATE_NEW_CONFIG -> uiCommandBuilder.set("#CreateConfigBtn.Background", "#263047CC");
            case EDIT_GLOBAL_CONFIG -> uiCommandBuilder.set("#EditGlobalConfigBtn.Background", "#263047CC");
        }
    }

    /**
     * Render global config edit panel
     */
    public void renderEditGlobalConfigPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.append("#ConfigInfoBody", "EditGlobalConfig.ui");

        uiCommandBuilder.set("#GlobalProtection #CheckBox.Value", uiState.globalProtection());
        uiCommandBuilder.set("#PlayerNotify #CheckBox.Value", uiState.notifyPlayer());
        uiCommandBuilder.set("#VerboseLogging #CheckBox.Value", uiState.verboseLogging());

        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#GlobalProtection #CheckBox",
                EventData.of(GLOBAL_CONFIG_UPDATE, "ToggleGlobalProtection"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PlayerNotify #CheckBox",
                EventData.of(GLOBAL_CONFIG_UPDATE, "ToggleGlobalPlayerNotify"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#VerboseLogging #CheckBox",
                EventData.of(GLOBAL_CONFIG_UPDATE, "ToggleVerboseLogging"), false);

        bindSharedConfigButtons(uiEventBuilder);
    }

    /**
     * Update global config panel values
     */
    public void updateGlobalConfigPanel(UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.set("#GlobalProtection #CheckBox.Value", uiState.globalProtection());
        uiCommandBuilder.set("#PlayerNotify #CheckBox.Value", uiState.notifyPlayer());
        uiCommandBuilder.set("#VerboseLogging #CheckBox.Value", uiState.verboseLogging());
    }

    /**
     * Render world config edit panel
     */
    public void renderWorldConfigEditPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.set("#WorldProtection #CheckBox.Value", uiState.config().protectionEnabled);
        uiCommandBuilder.set("#WorldPlayerNotify #CheckBox.Value", uiState.config().notifyPlayer);

        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#WorldProtection #CheckBox",
                EventData.of(WORLD_CONFIG_UPDATE, "ToggleWorldProtection"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#WorldPlayerNotify #CheckBox",
                EventData.of(WORLD_CONFIG_UPDATE, "ToggleWorldNotify"), false);
    }

    /**
     * Update world config panel values
     */
    public void updateWorldConfigPanel(UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.set("#WorldProtection #CheckBox.Value", uiState.config().protectionEnabled);
        uiCommandBuilder.set("#WorldPlayerNotify #CheckBox.Value", uiState.config().notifyPlayer);
    }

    /**
     * Update group selection button highlights
     */
    public void updateGroupSelectionPanel(UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.set("#MemberBtn.Background", "#00000000");
        uiCommandBuilder.set("#ModeratorBtn.Background", "#00000000");
        uiCommandBuilder.set("#AdministratorBtn.Background", "#00000000");

        switch(uiState.editPlayerRole()) {
            case MEMBER -> uiCommandBuilder.set("#MemberBtn.Background", "#263047CC");
            case MODERATOR -> uiCommandBuilder.set("#ModeratorBtn.Background", "#263047CC");
            case ADMINISTRATOR -> uiCommandBuilder.set("#AdministratorBtn.Background", "#263047CC");
        }
    }

    /**
     * Update UUID input button state
     */
    public void updateUuidInputButton(UICommandBuilder uiCommandBuilder, boolean isValid) {
        uiCommandBuilder.set("#AddUUIDBtn.Disabled", !isValid);
    }

    // PRIVATE HELPER METHODS

    /**
     * Validate UUID format - TODO move helper method to state
     */
    private boolean validateUUIDFormat(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return false;
        }
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void renderWorldEditPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.append("#ConfigInfoBody", "EditWorldConfig.ui");
        uiCommandBuilder.append("#WorldConfigButtons", "DeleteButton.ui");

        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteBtn",
                EventData.of(CONFIG_ACTION, "DeleteBtn"), false);

        bindSharedWorldConfigEvents(uiEventBuilder);
        buildSharedConfigButtons(uiCommandBuilder);
        bindSharedConfigButtons(uiEventBuilder);
        renderWorldConfigEditPanel(uiCommandBuilder, uiEventBuilder);
        renderProtectionsPanel(uiCommandBuilder, uiEventBuilder);
    }

    private void renderDefaultEditPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.append("#ConfigInfoBody", "EditWorldConfig.ui");

        bindSharedWorldConfigEvents(uiEventBuilder);
        buildSharedConfigButtons(uiCommandBuilder);
        bindSharedConfigButtons(uiEventBuilder);
        renderWorldConfigEditPanel(uiCommandBuilder, uiEventBuilder);
        renderProtectionsPanel(uiCommandBuilder, uiEventBuilder);
    }

    private void renderConfigCreatePanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.append("#ConfigInfoBody", "EditWorldConfig.ui");
        uiCommandBuilder.append("#WorldConfigButtons","NameWorldInput.ui");

        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#WorldNameInput",
                EventData.of("@WorldNameInput", "#WorldNameInput.Value"), false);

        bindSharedWorldConfigEvents(uiEventBuilder);
        buildSharedConfigButtons(uiCommandBuilder);
        bindSharedConfigButtons(uiEventBuilder);
        renderWorldConfigEditPanel(uiCommandBuilder, uiEventBuilder);
        renderProtectionsPanel(uiCommandBuilder, uiEventBuilder);
    }

    private void buildSharedConfigButtons(UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("#WorldConfigButtons", "SaveButton.ui");
        uiCommandBuilder.append("#WorldConfigButtons", "LoadButton.ui");
        uiCommandBuilder.append("#WorldConfigButtons", "CancelButton.ui");
        updateGroupSelectionPanel(uiCommandBuilder);
    }

    private void bindSharedConfigButtons(UIEventBuilder uiEventBuilder) {
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SaveBtn",
                EventData.of(CONFIG_ACTION, "SaveBtn"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#LoadBtn",
                EventData.of(CONFIG_ACTION, "LoadBtn"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelBtn",
                EventData.of(PANEL_CLICK, "ExitPanelBtn"), false);
    }

    private void bindSharedWorldConfigEvents(UIEventBuilder uiEventBuilder) {
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PlayerSearchInput",
                EventData.of("@PlayerSearchInput", "#PlayerSearchInput.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#UUIDInput",
                EventData.of("@UUIDInput", "#UUIDInput.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AddUUIDBtn",
                EventData.of(UUID_ACTION, "Add"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#MemberBtn",
                EventData.of(GROUP_ACTION, "Member"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ModeratorBtn",
                EventData.of(GROUP_ACTION, "Moderator"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AdministratorBtn",
                EventData.of(GROUP_ACTION, "Administrator"), false);
    }
}