package plugin.gui.pages;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.ConfigState;
import plugin.types.EVENT_TYPE;
import plugin.types.PLAYER_ROLE;
import plugin.gui.pages.SimpleProtectUIState.PanelView;

import javax.annotation.Nonnull;
import java.util.*;

public class SimpleProtectUI extends InteractiveCustomUIPage<SimpleProtectUI.Data> {
    private final UUID playerUUID = playerRef.getUuid();

    private final SimpleProtectUIState uiState = new SimpleProtectUIState(playerUUID);

    private int allowedCurrentPage = 0;
    private int disallowedCurrentPage = 0;

    private static final int PAGE_ENTRIES = 10;

    private final PlayerListService playerListService = new PlayerListService();

    long revisionNumber = 0;

    private static final String PANEL_CLICK = "PanelBtnClick";
    private static final String CONFIG_ACTION = "ConfigBtnClick";
    private static final String GLOBAL_CONFIG_UPDATE = "GlobalConfigUpdate";
    private static final String WORLD_CONFIG_UPDATE = "WorldConfigUpdate";
    private static final String PROTECTION_UPDATE = "ProtectionUpdate";
    private static final String PLAYER_ACTION = "PlayerClick";
    private static final String UUID_ACTION = "UUIDClick";
    private static final String GROUP_ACTION = "GroupClick";

    public SimpleProtectUI(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime) {
        super(playerRef, lifetime, Data.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder uiCommandBuilder,
                      @Nonnull UIEventBuilder uiEventBuilder,
                      @Nonnull Store<EntityStore> store) {

        // Append static UI (containers, static labels, inputs)
        uiCommandBuilder.append("SimpleProtectUI.ui");

        // Bind search input
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#WorldSearchInput",
                EventData.of("@WorldSearchInput", "#WorldSearchInput.Value"), false);

        // Render dynamic children for the first time
        rebuildMainConfigPanel(uiCommandBuilder, uiEventBuilder);

        if (canAdministrate()) {
            buildAdminPanel(uiCommandBuilder, uiEventBuilder);
            rebuildWorldListPanel(uiCommandBuilder, uiEventBuilder);
        }
    }

    /**
     *  Method that rebuilds only the world list in the panel so updates can be scoped as precisely as possible
     */
    private void rebuildWorldListPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
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
                if (uiState.panelView() == PanelView.CREATE_NEW_CONFIG) backgroundColor = "#FF0000CC";
                uiCommandBuilder.set("#WorldList[" + i + "].Background", backgroundColor);
            }

            // Bind click events for new buttons
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WorldList[" + i + "]",
                    EventData.of(PANEL_CLICK, world), false);
        }
    }

    /**
     *  Method that rebuilds the main config panel where settings are displayed
     */
    private void rebuildMainConfigPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.clear("#ConfigInfoBody");
        uiCommandBuilder.clear("#ExitPlaceholder");

        uiState.setPlayerSearch("");

        switch (uiState.panelView()) {
            case CLEAR -> {
                uiCommandBuilder.set("#InfoTitle.Text", " ");
                uiCommandBuilder.append("#ConfigInfoBody", "ModInfo.ui");
            }
            case EDIT_WORLD -> {
                uiCommandBuilder.set("#InfoTitle.Text", String.format("Editing config for world : %s", uiState.currentWorld()));
                buildWorldEditPanel(uiCommandBuilder, uiEventBuilder);
                rebuildProtectionsPanel(uiCommandBuilder, uiEventBuilder);
            }
            case EDIT_DEFAULT_WORLD -> {
                uiCommandBuilder.set("#InfoTitle.Text", "Editing the Default World config");
                uiCommandBuilder.set("#DefaultWorldBtn.Background", "#263047CC");
                buildDefaultEditPanel(uiCommandBuilder, uiEventBuilder);
                rebuildProtectionsPanel(uiCommandBuilder, uiEventBuilder);
            }
            case CREATE_NEW_CONFIG -> {
                uiCommandBuilder.set("#InfoTitle.Text", "Create New World Config");
                uiCommandBuilder.set("#CreateConfigBtn.Background", "#263047CC");
                buildConfigCreatePanel(uiCommandBuilder, uiEventBuilder);
                rebuildProtectionsPanel(uiCommandBuilder, uiEventBuilder);
            }
            case EDIT_GLOBAL_CONFIG -> {
                uiCommandBuilder.set("#InfoTitle.Text", "Edit Global Config");
                uiCommandBuilder.set("#EditGlobalConfigBtn.Background", "#263047CC");
                buildEditGlobalConfigPanel(uiCommandBuilder, uiEventBuilder);
            }
        }

        uiCommandBuilder.set("#WorldSearchInput.Value", uiState.worldFilter());

        if (uiState.panelView() != PanelView.CLEAR) {
            uiCommandBuilder.append("#ExitPlaceholder", "ExitPanelButton.ui");
            // Bind config panel close input if the button exists
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ExitPanelButton",
                    EventData.of(PANEL_CLICK, "ExitPanelBtn"), false);
        }
    }

    private void rebuildProtectionsPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
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

    private void refreshPlayerPanels() {
        long revision = ++revisionNumber;
        refreshAllowedPlayersAsync(revision);
        refreshDisallowedPlayersAsync(revision);
    }

    private void refreshAllowedPlayersAsync(long revision) {
        playerListService.buildAllowedPlayersPageAsync(
                uiState.config(),
                uiState.editPlayerRole(),
                uiState.playerSearch(),
                allowedCurrentPage,
                PAGE_ENTRIES,
                dbResult -> {
                    if (revision != revisionNumber) return;
                    runOnWorldThread(() -> rebuildAllowedPlayersPanel(dbResult));
                }
        ).thenAccept(cacheResult -> {
            if (revision != revisionNumber) return;
            runOnWorldThread(() -> rebuildAllowedPlayersPanel(cacheResult));
        });
    }

    private void runOnWorldThread(Runnable task) {
        if (playerRef.getWorldUuid() == null) return;
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world != null && world.isAlive()) {
            world.execute(task);
        }
    }

    private void rebuildAllowedPlayersPanel(PageResult<PlayerListService.PlayerEntry> page) {
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        UIEventBuilder uiEventBuilder = new UIEventBuilder();

        uiCommandBuilder.clear("#AllowedPlayers");

        List<PlayerListService.PlayerEntry> entries = page.items();

        if (entries.isEmpty()) {
            // No players to show
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

        sendUpdate(uiCommandBuilder, uiEventBuilder, false);
    }

    private void refreshDisallowedPlayersAsync(long revision) {
        playerListService.buildDisallowedPlayersPageAsync(
                uiState.config(),
                uiState.playerSearch(),
                allowedCurrentPage,
                PAGE_ENTRIES,
                dbResult -> {
                    if (revision != revisionNumber) return;

                    runOnWorldThread(() -> rebuildDisallowedPlayersPanel(dbResult));
                }
        ).thenAccept(cacheResult -> {
            if (revision != revisionNumber) return;
            runOnWorldThread(() -> rebuildDisallowedPlayersPanel(cacheResult));
        });
    }

    private void rebuildDisallowedPlayersPanel(PageResult<PlayerListService.PlayerEntry> page) {
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        UIEventBuilder uiEventBuilder = new UIEventBuilder();

        uiCommandBuilder.clear("#DisallowedPlayers");

        List<PlayerListService.PlayerEntry> entries = page.items();

        if (entries.isEmpty()) {
            // No players to show
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

        sendUpdate(uiCommandBuilder, uiEventBuilder, false);
    }

    private void buildWorldEditPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.append("#ConfigInfoBody", "EditWorldConfig.ui");
        uiCommandBuilder.append("#WorldConfigButtons", "DeleteButton.ui");
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteBtn",
                EventData.of(CONFIG_ACTION, "DeleteBtn"), false);
        uiState.syncWorldConfig();

        uiState.resolvePlayerRole();

        bindSharedWorldConfigEvents(uiEventBuilder);
        buildSharedConfigButtons(uiCommandBuilder);
        bindSharedConfigButtons(uiEventBuilder);
        buildEditWorldConfigPanel(uiCommandBuilder, uiEventBuilder);
        refreshPlayerPanels();
    }

    private void buildDefaultEditPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.append("#ConfigInfoBody", "EditWorldConfig.ui");
        uiState.syncWorldConfig();
        bindSharedWorldConfigEvents(uiEventBuilder);
        buildSharedConfigButtons(uiCommandBuilder);
        bindSharedConfigButtons(uiEventBuilder);
        buildEditWorldConfigPanel(uiCommandBuilder, uiEventBuilder);
        refreshPlayerPanels();
    }

    private void buildConfigCreatePanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.append("#ConfigInfoBody", "EditWorldConfig.ui");
        uiCommandBuilder.append("#WorldConfigButtons","NameWorldInput.ui");
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#WorldNameInput",
                EventData.of("@WorldNameInput", "#WorldNameInput.Value"), false);
        uiState.syncWorldConfig();
        bindSharedWorldConfigEvents(uiEventBuilder);
        buildSharedConfigButtons(uiCommandBuilder);
        bindSharedConfigButtons(uiEventBuilder);
        buildEditWorldConfigPanel(uiCommandBuilder, uiEventBuilder);
        refreshPlayerPanels();
    }

    private void buildAdminPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.append("#LeftColumnPanel", "AdminConfigOptions.ui");
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#DefaultWorldBtn",
                EventData.of(PANEL_CLICK, "DefaultWorldBtn"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CreateConfigBtn",
                EventData.of(PANEL_CLICK, "CreateConfigBtn"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#EditGlobalConfigBtn",
                EventData.of(PANEL_CLICK, "EditGlobalConfigBtn"), false);
    }

    private void rebuildAdminPanel(UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.set("#DefaultWorldBtn.Background", "#00000000");
        uiCommandBuilder.set("#CreateConfigBtn.Background", "#00000000");
        uiCommandBuilder.set("#EditGlobalConfigBtn.Background", "#00000000");

        switch(uiState.panelView()) {
            case EDIT_DEFAULT_WORLD -> uiCommandBuilder.set("#DefaultWorldBtn.Background", "#263047CC");
            case CREATE_NEW_CONFIG -> uiCommandBuilder.set("#CreateConfigBtn.Background", "#263047CC");
            case EDIT_GLOBAL_CONFIG -> uiCommandBuilder.set("#EditGlobalConfigBtn.Background", "#263047CC");
        }
    }

    private void buildEditGlobalConfigPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.append("#ConfigInfoBody", "EditGlobalConfig.ui");
        uiState.syncGlobalSettings();
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

    private void rebuildEditGlobalConfigPanel(UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.set("#GlobalProtection #CheckBox.Value", uiState.globalProtection());
        uiCommandBuilder.set("#PlayerNotify #CheckBox.Value", uiState.notifyPlayer());
        uiCommandBuilder.set("#VerboseLogging #CheckBox.Value", uiState.verboseLogging());
    }

    private void buildEditWorldConfigPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.set("#WorldProtection #CheckBox.Value", uiState.config().protectionEnabled);
        uiCommandBuilder.set("#WorldPlayerNotify #CheckBox.Value", uiState.config().notifyPlayer);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#WorldProtection #CheckBox",
                EventData.of(WORLD_CONFIG_UPDATE, "ToggleWorldProtection"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#WorldPlayerNotify #CheckBox",
                EventData.of(WORLD_CONFIG_UPDATE, "ToggleWorldNotify"), false);
    }

    private void rebuildEditWorldConfigPanel(UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.set("#WorldProtection #CheckBox.Value", uiState.config().protectionEnabled);
        uiCommandBuilder.set("#WorldPlayerNotify #CheckBox.Value", uiState.config().notifyPlayer);
    }

    private void rebuildGroupSelectionPanel(UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.set("#MemberBtn.Background", "#00000000");
        uiCommandBuilder.set("#ModeratorBtn.Background", "#00000000");
        uiCommandBuilder.set("#AdministratorBtn.Background", "#00000000");
        switch(uiState.editPlayerRole()) {
            case PLAYER_ROLE.MEMBER -> uiCommandBuilder.set("#MemberBtn.Background", "#263047CC");
            case PLAYER_ROLE.MODERATOR -> uiCommandBuilder.set("#ModeratorBtn.Background", "#263047CC");
            case PLAYER_ROLE.ADMINISTRATOR -> uiCommandBuilder.set("#AdministratorBtn.Background", "#263047CC");
        }
    }

    private void buildSharedConfigButtons(UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("#WorldConfigButtons", "SaveButton.ui");
        uiCommandBuilder.append("#WorldConfigButtons", "LoadButton.ui");
        uiCommandBuilder.append("#WorldConfigButtons", "CancelButton.ui");
        rebuildGroupSelectionPanel(uiCommandBuilder);
    }

    // Bind Save, Load, and Cancel buttons since all configs share these buttons
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

    private void saveWorldConfig() {
        if (canAdministrate()) {
            if (uiState.panelView() == PanelView.EDIT_DEFAULT_WORLD) {
                ConfigState.get().updateDefaultWorldConfig(uiState.config());
            } else {
                ConfigState.get().setWorldProtectionConfig(uiState.currentWorld(), uiState.config());
            }
        }
    }

    private void deleteWorldConfig() {
        if (canAdministrate()) {
            ConfigState.get().deleteWorldProtectionConfig(uiState.currentWorld());
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                Data data) {
        super.handleDataEvent(ref, store, data);
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        UIEventBuilder uiEventBuilder = new UIEventBuilder();

        // Handle a change to the world search field
        if (data.worldFilter != null) {
            uiState.setWorldFilter(data.worldFilter);
            rebuildWorldListPanel(uiCommandBuilder, uiEventBuilder);
            sendUpdate(uiCommandBuilder, uiEventBuilder, false);
            return;
        }

        // Handle a change to the world name input while creating a new config
        if (data.nameWorldUpdate != null) {
            uiState.setWorldFilter(data.nameWorldUpdate);
            uiState.setNameForWorld(data.nameWorldUpdate);
            rebuildWorldListPanel(uiCommandBuilder, uiEventBuilder);
            sendUpdate(uiCommandBuilder, uiEventBuilder, false);
            return;
        }

        // Change to the player name that is being searched for
        if (data.playerNameUpdate != null) {
            uiState.setPlayerSearch(data.playerNameUpdate);
            refreshPlayerPanels();
            return;
        }

        // Change to the UUID that is being searched for
        if (data.uuidUpdate != null) {
            uiState.setUuidInput(data.uuidUpdate);
            uiCommandBuilder.set("#AddUUIDBtn.Disabled", !validateUUID(uiState.uuidInput()));
            sendUpdate(uiCommandBuilder, uiEventBuilder, false);
            return;
        }

        // Change the group that is being edited
        if (data.groupClicked != null) {
            uiState.setEditPlayerRole(PLAYER_ROLE.valueOf(data.groupClicked.toUpperCase()));
            rebuildGroupSelectionPanel(uiCommandBuilder);
            refreshPlayerPanels();
            sendUpdate(uiCommandBuilder, uiEventBuilder, false);
            return;
        }

        // Handle a change to the global config
        if (data.globalConfigUpdate != null) {
            switch(data.globalConfigUpdate) {
                case "ToggleGlobalProtection" -> uiState.toggleGlobalProtection();
                case "ToggleGlobalPlayerNotify" -> uiState.toggleNotifyPlayer();
                case "ToggleVerboseLogging" -> uiState.toggleVerboseLogging();
            }
            rebuildEditGlobalConfigPanel(uiCommandBuilder);
            sendUpdate(uiCommandBuilder, uiEventBuilder, false);
            return;
        }

        if (data.playerUpdate != null) {
            UUID playerUUID = UUID.fromString(data.playerUpdate);
            Set<UUID> allowedGroup;
            switch(uiState.editPlayerRole()) {
                case PLAYER_ROLE.MODERATOR -> allowedGroup = uiState.config().moderators;
                case PLAYER_ROLE.ADMINISTRATOR -> allowedGroup = uiState.config().administrators;
                default -> allowedGroup = uiState.config().members;
            }
            if (allowedGroup.contains(playerUUID)) {
                allowedGroup.remove(playerUUID);
            } else {
                allowedGroup.add(playerUUID);
            }
            refreshPlayerPanels();
            return;
        }

        if (data.uuidAction != null) {
            UUID playerUUID = UUID.fromString(uiState.uuidInput());
            uiState.config().members.add(playerUUID);
            sendUpdate(uiCommandBuilder, uiEventBuilder, false);
            return;
        }

        if (data.protectionUpdate != null) {
            EVENT_TYPE changed = EVENT_TYPE.valueOf(data.protectionUpdate);
            if (uiState.config().enabledProtections.contains(changed)) {
                uiState.config().enabledProtections.remove(changed);
            } else {
                uiState.config().enabledProtections.add(changed);
            }
            rebuildProtectionsPanel(uiCommandBuilder, uiEventBuilder);
            sendUpdate(uiCommandBuilder, uiEventBuilder, false);
            return;
        }

        if (data.worldConfigUpdate != null) {
            switch(data.worldConfigUpdate) {
                case "ToggleWorldProtection" -> uiState.config().protectionEnabled = !uiState.config().protectionEnabled;
                case "ToggleWorldNotify" -> uiState.config().notifyPlayer = !uiState.config().notifyPlayer;
            }
            rebuildEditWorldConfigPanel(uiCommandBuilder);
            sendUpdate(uiCommandBuilder, uiEventBuilder, false);
            return;
        }

        if (data.configAction != null) {
            switch(uiState.panelView()) {
                case PanelView.EDIT_GLOBAL_CONFIG -> {
                    switch(data.configAction) {
                        case "SaveBtn" -> uiState.saveGlobalConfig();
                        case "LoadBtn" -> {
                            uiState.syncGlobalSettings();
                            rebuildEditGlobalConfigPanel(uiCommandBuilder);
                        }
                    }
                }
                case PanelView.EDIT_WORLD -> {
                    switch (data.configAction) {
                        case "SaveBtn" -> saveWorldConfig();
                        case "LoadBtn" -> {
                            uiState.syncWorldConfig();
                            rebuildProtectionsPanel(uiCommandBuilder, uiEventBuilder);
                            rebuildEditWorldConfigPanel(uiCommandBuilder);
                        }
                        case "DeleteBtn" -> {
                            deleteWorldConfig();
                            uiState.setPanelView(PanelView.CLEAR);
                            uiState.setWorldFilter("");
                            uiState.setCurrentWorld("");
                            rebuildMainConfigPanel(uiCommandBuilder, uiEventBuilder);
                        }
                    }
                }
                case PanelView.CREATE_NEW_CONFIG -> {
                    switch (data.configAction) {
                        case "SaveBtn" -> {
                            uiState.setCurrentWorld(uiState.nameForWorld());
                            uiState.setNameForWorld("");
                            uiState.setPanelView(PanelView.EDIT_WORLD);
                            saveWorldConfig();
                            rebuildMainConfigPanel(uiCommandBuilder, uiEventBuilder);
                            rebuildAdminPanel(uiCommandBuilder);
                        }
                        case "LoadBtn" -> {
                            uiState.syncWorldConfig();
                            rebuildProtectionsPanel(uiCommandBuilder, uiEventBuilder);
                            rebuildEditWorldConfigPanel(uiCommandBuilder);
                        }
                    }
                }
                case PanelView.EDIT_DEFAULT_WORLD -> {
                    switch (data.configAction) {
                        case "SaveBtn" -> {
                            saveWorldConfig();
                        }
                        case "LoadBtn" -> {
                            uiState.syncWorldConfig();
                            rebuildProtectionsPanel(uiCommandBuilder, uiEventBuilder);
                            rebuildEditWorldConfigPanel(uiCommandBuilder);
                        }
                    }
                }
            }
            rebuildWorldListPanel(uiCommandBuilder, uiEventBuilder);
            sendUpdate(uiCommandBuilder, uiEventBuilder, false);
            return;
        }

        if (data.mainPanel != null) {
            // All panel changing actions cause the world list to rebuild to update the highlighting
            uiState.setCurrentWorld("");
            switch (data.mainPanel) {
                case "DefaultWorldBtn" -> uiState.setPanelView(PanelView.EDIT_DEFAULT_WORLD);
                case "CreateConfigBtn" -> uiState.setPanelView(PanelView.CREATE_NEW_CONFIG);
                case "EditGlobalConfigBtn" -> uiState.setPanelView(PanelView.EDIT_GLOBAL_CONFIG);
                case "ExitPanelBtn" -> uiState.setPanelView(PanelView.CLEAR);
                default -> {
                    uiState.setCurrentWorld(data.mainPanel);
                    uiState.setPanelView(PanelView.EDIT_WORLD);
                }
            }
            uiState.syncWorldConfig();
            // All panel changing actions cause the world list to rebuild to update the highlighting
            rebuildWorldListPanel(uiCommandBuilder, uiEventBuilder);
            rebuildMainConfigPanel(uiCommandBuilder, uiEventBuilder);
            if (canAdministrate()) rebuildAdminPanel(uiCommandBuilder);
        }

        // Apply updates to the client
        sendUpdate(uiCommandBuilder, uiEventBuilder, false);
    }

    public static class Data {
        public static final BuilderCodec<Data> CODEC = BuilderCodec.builder(Data.class, Data::new)
                .append(new KeyedCodec<>("@WorldSearchInput", Codec.STRING),
                        (data, value) -> data.worldFilter = value,
                        data -> data.worldFilter).add()
                .append(new KeyedCodec<>(PANEL_CLICK, Codec.STRING),
                        (data, value) -> data.mainPanel = value,
                        data -> data.mainPanel).add()
                .append(new KeyedCodec<>(CONFIG_ACTION, Codec.STRING),
                        (data, value) -> data.configAction = value,
                        data -> data.configAction).add()
                .append(new KeyedCodec<>(GLOBAL_CONFIG_UPDATE, Codec.STRING),
                        (data, value) -> data.globalConfigUpdate = value,
                        data -> data.globalConfigUpdate).add()
                .append(new KeyedCodec<>(WORLD_CONFIG_UPDATE, Codec.STRING),
                        (data, value) -> data.worldConfigUpdate = value,
                        data -> data.worldConfigUpdate).add()
                .append(new KeyedCodec<>(PROTECTION_UPDATE, Codec.STRING),
                        (data, value) -> data.protectionUpdate = value,
                        data -> data.protectionUpdate).add()
                .append(new KeyedCodec<>(PLAYER_ACTION, Codec.STRING),
                        (data, value) -> data.playerUpdate = value,
                        data -> data.playerUpdate).add()
                .append(new KeyedCodec<>("@WorldNameInput", Codec.STRING),
                        (data, value) -> data.nameWorldUpdate = value,
                        data -> data.nameWorldUpdate).add()
                .append(new KeyedCodec<>("@PlayerSearchInput", Codec.STRING),
                        (data, value) -> data.playerNameUpdate = value,
                        data -> data.playerNameUpdate).add()
                .append(new KeyedCodec<>("@UUIDInput", Codec.STRING),
                        (data, value) -> data.uuidUpdate = value,
                        data -> data.uuidUpdate).add()
                .append(new KeyedCodec<>(UUID_ACTION, Codec.STRING),
                        (data, value) -> data.uuidAction = value,
                        data -> data.uuidAction).add()
                .append(new KeyedCodec<>(GROUP_ACTION, Codec.STRING),
                        (data, value) -> data.groupClicked = value,
                        data -> data.groupClicked).add()
                .build();

        private String worldFilter;
        private String mainPanel;
        private String configAction;
        private String globalConfigUpdate;
        private String worldConfigUpdate;
        private String protectionUpdate;
        private String playerUpdate;
        private String nameWorldUpdate;
        private String playerNameUpdate;
        private String uuidUpdate;
        private String uuidAction;
        private String groupClicked;
    }

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

    private boolean canAdministrate() {
        return PermissionsModule.get().hasPermission(playerUUID, ConfigState.get().getAdministratePermission());
    }
}
