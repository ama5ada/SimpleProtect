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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.ConfigState;
import plugin.UUIDCache;
import plugin.db.PlayerInfoDB;
import plugin.types.EVENT_TYPE;
import plugin.types.SimpleProtectWorldConfig;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.regex.Pattern;

public class SimpleProtectUI extends InteractiveCustomUIPage<SimpleProtectUI.Data> {
    private enum PANEL_VIEW {
        CLEAR,
        EDIT_WORLD,
        EDIT_DEFAULT_WORLD,
        CREATE_NEW_CONFIG,
        EDIT_GLOBAL_CONFIG
    }

    private String worldSearch = "";
    private String currentWorld = "";
    private String nameForWorld = "";
    private String playerSearch = "";
    private String uuidInput = "";
    private PANEL_VIEW currentPanelView = PANEL_VIEW.CLEAR;
    private static final String PANEL_ACTION = "PanelBtnClick";
    private static final String CONFIG_ACTION = "ConfigBtnClick";
    private static final String GLOBAL_CONFIG_UPDATE = "GlobalConfigUpdate";
    private static final String WORLD_CONFIG_UPDATE = "WorldConfigUpdate";
    private static final String PROTECTION_UPDATE = "ProtectionUpdate";
    private static final String PLAYER_ACTION = "PlayerClick";
    private static final String UUID_ACTION = "UUIDClick";
    private final boolean canAdministrate;

    private boolean globalProtection = false;
    private boolean verboseLogging = false;
    private boolean notifyPlayer = false;

    private SimpleProtectWorldConfig currentConfig;

    public SimpleProtectUI(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime) {
        super(playerRef, lifetime, Data.CODEC);
        this.canAdministrate = PermissionsModule.get().hasPermission(playerRef.getUuid(),
                ConfigState.get().getAdministratePermission());
        if (canAdministrate) {
            syncGlobalSettings();
        }
    }

    private void syncGlobalSettings() {
        globalProtection = ConfigState.get().isProtected();
        verboseLogging = ConfigState.get().isVerbose();
        notifyPlayer = ConfigState.get().notifyPlayer();
    }

    private void syncConfigSettings() {
        if (currentPanelView == PANEL_VIEW.EDIT_DEFAULT_WORLD || currentPanelView == PANEL_VIEW.CREATE_NEW_CONFIG) {
            currentConfig = new SimpleProtectWorldConfig(ConfigState.get().getDefaultWorldConfig());
        } else {
            currentConfig = new SimpleProtectWorldConfig(ConfigState.get().getWorldProtectionConfig(currentWorld));
        }
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

        if (canAdministrate) {
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
        if (!worldSearch.isBlank()) {
            String search = worldSearch.toLowerCase();
            worldNames = Arrays.stream(worldNames)
                    .filter(w -> w.toLowerCase().contains(search))
                    .toArray(String[]::new);
        }

        for (int i = 0; i < worldNames.length; i++) {
            String world = worldNames[i];
            uiCommandBuilder.append("#WorldList", "WorldButton.ui");
            uiCommandBuilder.set("#WorldList[" + i + "].Text", world);
            if (worldNames[i].equals(currentWorld)) {
                String backgroundColor = "#263047CC";
                if (currentPanelView == PANEL_VIEW.CREATE_NEW_CONFIG) backgroundColor = "#FF0000CC";
                uiCommandBuilder.set("#WorldList[" + i + "].Background", backgroundColor);
            }

            // Bind click events for new buttons
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WorldList[" + i + "]",
                    EventData.of(PANEL_ACTION, world), false);
        }
    }

    /**
     *  Method that rebuilds the main config panel where settings are displayed
     */
    private void rebuildMainConfigPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.clear("#ConfigInfoBody");
        uiCommandBuilder.clear("#ExitPlaceholder");

        playerSearch = "";

        switch (currentPanelView) {
            case CLEAR -> {
                uiCommandBuilder.set("#InfoTitle.Text", " ");
                uiCommandBuilder.append("#ConfigInfoBody", "ModInfo.ui");
            }
            case EDIT_WORLD -> {
                uiCommandBuilder.set("#InfoTitle.Text", String.format("Editing config for world : %s", currentWorld));
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

        uiCommandBuilder.set("#WorldSearchInput.Value", worldSearch);

        if (currentPanelView != PANEL_VIEW.CLEAR) {
            uiCommandBuilder.append("#ExitPlaceholder", "ExitPanelButton.ui");
            // Bind config panel close input if the button exists
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ExitPanelButton",
                    EventData.of(PANEL_ACTION, "ExitPanelBtn"), false);
        }
    }

    private void rebuildProtectionsPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.clear("#EnabledProtections");
        uiCommandBuilder.clear("#DisabledProtections");
        EnumSet<EVENT_TYPE> disabled = EnumSet.complementOf(currentConfig.enabledProtections);

        int i = 0;
        for (EVENT_TYPE included : currentConfig.enabledProtections) {
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

    private void rebuildAllowedPlayersPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.clear("#AllowedPlayers");
        List<Integer> misses = new ArrayList<>();
        List<UUID> missedIds = new ArrayList<>();
        String compare = playerSearch.toLowerCase();

        int i = 0;
        for (UUID playerID : currentConfig.allowedPlayers) {
            String displayName = UUIDCache.get().getNameFromUUID(playerID);
            String displayString;
            if (displayName != null) {
                displayString = String.format("%s:%s", displayName, playerID.toString());
            } else {
                displayString = playerID.toString();
                misses.add(i);
                missedIds.add(playerID);
            }

            if (displayName != null && !playerSearch.isEmpty() && !displayName.toLowerCase().startsWith(compare)) {
                continue;
            }

            uiCommandBuilder.append("#AllowedPlayers", "PlayerButton.ui");
            uiCommandBuilder.set("#AllowedPlayers[" + i + "].Text", displayString);
            uiCommandBuilder.set("#AllowedPlayers[" + i + "].Background", "#33FF0019");

            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AllowedPlayers[" + i + "]",
                    EventData.of(PLAYER_ACTION, playerID.toString()), false);
            i++;
        }

        sendUpdate(uiCommandBuilder, uiEventBuilder, false);

        if (!missedIds.isEmpty()) {
            PlayerInfoDB.queryPlayersByUUIDsAsync(missedIds, dbResults -> {
                UICommandBuilder dynamicBuilder = new UICommandBuilder();
                UIEventBuilder dynamicEvents = new UIEventBuilder();

                for (int j = 0; j < missedIds.size(); j++) {
                    UUID uuid = missedIds.get(j);
                    int uiIndex = misses.get(j);

                    String displayName = dbResults.get(uuid);
                    if (displayName != null) {
                        UUIDCache.get().putPlayerInfo(uuid, displayName);
                        dynamicBuilder.set("#AllowedPlayers[" + uiIndex + "].Text",
                                String.format("%s:%s", displayName, uuid));
                    }
                }

                sendUpdate(dynamicBuilder, dynamicEvents, false);
            });
        }
    }

    private void rebuildDisallowedPlayersPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.clear("#DisallowedPlayers");

        PlayerInfoDB.queryPlayersAsync(playerSearch, dbResults -> {
            UICommandBuilder dynamicBuilder = new UICommandBuilder();
            UIEventBuilder dynamicEvents = new UIEventBuilder();

            for (Map.Entry<UUID, String> entry : dbResults.entrySet()) {
                UUID uuid = entry.getKey();
                String displayName = entry.getValue();

                // Cache miss add an entry
                if (UUIDCache.get().getNameFromUUID(uuid) == null) {
                    UUIDCache.get().putPlayerInfo(uuid, displayName);
                }
            }

            // Iterate over the cache keys
            int i = 0;
            for (Map.Entry<UUID, String> entry : UUIDCache.get().getEntries()) {
                UUID uuid = entry.getKey();
                String displayName = entry.getValue();
                if (!playerSearch.isEmpty() && !displayName.startsWith(playerSearch)) {
                    continue;
                }

                uiCommandBuilder.append("#DisallowedPlayers", "PlayerButton.ui");
                uiCommandBuilder.set("#DisallowedPlayers[" + i + "].Text", uuid.toString());
                uiCommandBuilder.set("#DisallowedPlayers[" + i + "].Background", "#33FF0019");

                uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AllowedPlayers[" + i + "]",
                        EventData.of(PLAYER_ACTION, uuid.toString()), false);
                i++;
            }

            sendUpdate(dynamicBuilder, dynamicEvents, false);
        });
    }

    private void buildWorldEditPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.append("#ConfigInfoBody", "EditWorldConfig.ui");
        uiCommandBuilder.append("#WorldConfigButtons", "DeleteButton.ui");
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteBtn",
                EventData.of(CONFIG_ACTION, "DeleteBtn"), false);
        syncConfigSettings();
        bindSharedWorldConfigEvents(uiEventBuilder);
        rebuildAllowedPlayersPanel(uiCommandBuilder, uiEventBuilder);
        buildSharedConfigButtons(uiCommandBuilder);
        bindSharedConfigButtons(uiEventBuilder);
        buildEditWorldConfigPanel(uiCommandBuilder, uiEventBuilder);
    }

    private void buildDefaultEditPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.append("#ConfigInfoBody", "EditWorldConfig.ui");
        syncConfigSettings();
        bindSharedWorldConfigEvents(uiEventBuilder);
        rebuildAllowedPlayersPanel(uiCommandBuilder, uiEventBuilder);
        buildSharedConfigButtons(uiCommandBuilder);
        bindSharedConfigButtons(uiEventBuilder);
        buildEditWorldConfigPanel(uiCommandBuilder, uiEventBuilder);
    }

    private void buildConfigCreatePanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.append("#ConfigInfoBody", "EditWorldConfig.ui");
        uiCommandBuilder.append("#WorldConfigButtons","NameWorldInput.ui");
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#WorldNameInput",
                EventData.of("@WorldNameInput", "#WorldNameInput.Value"), false);
        syncConfigSettings();
        bindSharedWorldConfigEvents(uiEventBuilder);
        rebuildAllowedPlayersPanel(uiCommandBuilder, uiEventBuilder);
        buildSharedConfigButtons(uiCommandBuilder);
        bindSharedConfigButtons(uiEventBuilder);
        buildEditWorldConfigPanel(uiCommandBuilder, uiEventBuilder);
    }

    private void buildAdminPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.append("#LeftColumnPanel", "AdminConfigOptions.ui");
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#DefaultWorldBtn",
                EventData.of(PANEL_ACTION, "DefaultWorldBtn"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CreateConfigBtn",
                EventData.of(PANEL_ACTION, "CreateConfigBtn"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#EditGlobalConfigBtn",
                EventData.of(PANEL_ACTION, "EditGlobalConfigBtn"), false);
    }

    private void rebuildAdminPanel(UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.set("#DefaultWorldBtn.Background", "#00000000");
        uiCommandBuilder.set("#CreateConfigBtn.Background", "#00000000");
        uiCommandBuilder.set("#EditGlobalConfigBtn.Background", "#00000000");

        switch(currentPanelView) {
            case EDIT_DEFAULT_WORLD -> uiCommandBuilder.set("#DefaultWorldBtn.Background", "#263047CC");
            case CREATE_NEW_CONFIG -> uiCommandBuilder.set("#CreateConfigBtn.Background", "#263047CC");
            case EDIT_GLOBAL_CONFIG -> uiCommandBuilder.set("#EditGlobalConfigBtn.Background", "#263047CC");
        }
    }

    private void buildEditGlobalConfigPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.append("#ConfigInfoBody", "EditGlobalConfig.ui");
        syncGlobalSettings();
        uiCommandBuilder.set("#GlobalProtection #CheckBox.Value", globalProtection);
        uiCommandBuilder.set("#PlayerNotify #CheckBox.Value", notifyPlayer);
        uiCommandBuilder.set("#VerboseLogging #CheckBox.Value", verboseLogging);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#GlobalProtection #CheckBox",
                EventData.of(GLOBAL_CONFIG_UPDATE, "ToggleGlobalProtection"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PlayerNotify #CheckBox",
                EventData.of(GLOBAL_CONFIG_UPDATE, "ToggleGlobalPlayerNotify"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#VerboseLogging #CheckBox",
                EventData.of(GLOBAL_CONFIG_UPDATE, "ToggleVerboseLogging"), false);
        bindSharedConfigButtons(uiEventBuilder);
    }

    private void rebuildEditGlobalConfigPanel(UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.set("#GlobalProtection #CheckBox.Value", globalProtection);
        uiCommandBuilder.set("#PlayerNotify #CheckBox.Value", notifyPlayer);
        uiCommandBuilder.set("#VerboseLogging #CheckBox.Value", verboseLogging);
    }

    private void buildEditWorldConfigPanel(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.set("#WorldProtection #CheckBox.Value", currentConfig.protectionEnabled);
        uiCommandBuilder.set("#WorldPlayerNotify #CheckBox.Value", currentConfig.notifyPlayer);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#WorldProtection #CheckBox",
                EventData.of(WORLD_CONFIG_UPDATE, "ToggleWorldProtection"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#WorldPlayerNotify #CheckBox",
                EventData.of(WORLD_CONFIG_UPDATE, "ToggleWorldNotify"), false);
    }

    private void rebuildEditWorldConfigPanel(UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.set("#WorldProtection #CheckBox.Value", currentConfig.protectionEnabled);
        uiCommandBuilder.set("#WorldPlayerNotify #CheckBox.Value", currentConfig.notifyPlayer);
    }

    private void buildSharedConfigButtons(UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("#WorldConfigButtons", "SaveButton.ui");
        uiCommandBuilder.append("#WorldConfigButtons", "LoadButton.ui");
        uiCommandBuilder.append("#WorldConfigButtons", "CancelButton.ui");
    }

    // Bind Save, Load, and Cancel buttons since all configs share these buttons
    private void bindSharedConfigButtons(UIEventBuilder uiEventBuilder) {
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SaveBtn",
                EventData.of(CONFIG_ACTION, "SaveBtn"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#LoadBtn",
                EventData.of(CONFIG_ACTION, "LoadBtn"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelBtn",
                EventData.of(PANEL_ACTION, "ExitPanelBtn"), false);
    }

    private void bindSharedWorldConfigEvents(UIEventBuilder uiEventBuilder) {
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PlayerSearchInput",
                EventData.of("@PlayerSearchInput", "#PlayerSearchInput.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#UUIDInput",
                EventData.of("@UUIDInput", "#UUIDInput.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AddUUIDBtn",
                EventData.of(UUID_ACTION, "Add"), false);
    }

    private void saveGlobalConfig() {
        if (canAdministrate) {
            ConfigState.get().updateGlobalConfig(globalProtection, notifyPlayer, verboseLogging);
        }
    }

    private void saveWorldConfig() {
        if (canAdministrate) {
            if (currentPanelView == PANEL_VIEW.EDIT_DEFAULT_WORLD) {
                ConfigState.get().updateDefaultWorldConfig(currentConfig);
            } else {
                ConfigState.get().setWorldProtectionConfig(currentWorld, currentConfig);
            }
        }
    }

    private void deleteWorldConfig() {
        if (canAdministrate) {
            ConfigState.get().deleteWorldProtectionConfig(currentWorld);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                Data data) {
        super.handleDataEvent(ref, store, data);
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        UIEventBuilder uiEventBuilder = new UIEventBuilder();

        // Handle a change to the search field
        if (data.search != null) {
            worldSearch = data.search;
            rebuildWorldListPanel(uiCommandBuilder, uiEventBuilder);
            sendUpdate(uiCommandBuilder, uiEventBuilder, false);
            return;
        }

        if (data.nameWorldUpdate != null) {
            worldSearch = data.nameWorldUpdate;
            nameForWorld = data.nameWorldUpdate;
            rebuildWorldListPanel(uiCommandBuilder, uiEventBuilder);
            sendUpdate(uiCommandBuilder, uiEventBuilder, false);
            return;
        }

        if (data.playerNameUpdate != null) {
            playerSearch = data.playerNameUpdate;
            rebuildAllowedPlayersPanel(uiCommandBuilder, uiEventBuilder);
            return;
        }

        if (data.uuidUpdate != null) {
            uuidInput = data.uuidUpdate;
            uiCommandBuilder.set("#AddUUIDBtn.Disabled", !validateUUID(uuidInput));
            sendUpdate(uiCommandBuilder, uiEventBuilder, false);
            return;
        }

        // Handle a change to the global config
        if (data.globalConfigUpdate != null) {
            switch(data.globalConfigUpdate) {
                case "ToggleGlobalProtection" -> globalProtection = !globalProtection;
                case "ToggleGlobalPlayerNotify" -> notifyPlayer = !notifyPlayer;
                case "ToggleVerboseLogging" -> verboseLogging = !verboseLogging;
            }
            rebuildEditGlobalConfigPanel(uiCommandBuilder);
            sendUpdate(uiCommandBuilder, uiEventBuilder, false);
            return;
        }

        if (data.playerUpdate != null) {
            UUID playerUUID = UUID.fromString(data.playerUpdate);
            if (currentConfig.allowedPlayers.contains(playerUUID)) {
                currentConfig.allowedPlayers.remove(playerUUID);
            } else {
                currentConfig.allowedPlayers.add(playerUUID);
            }
            rebuildAllowedPlayersPanel(uiCommandBuilder, uiEventBuilder);
            return;
        }

        if (data.uuidAction != null) {
            UUID playerUUID = UUID.fromString(uuidInput);
            currentConfig.allowedPlayers.add(playerUUID);
            sendUpdate(uiCommandBuilder, uiEventBuilder, false);
            return;
        }

        if (data.protectionUpdate != null) {
            EVENT_TYPE changed = EVENT_TYPE.valueOf(data.protectionUpdate);
            if (currentConfig.enabledProtections.contains(changed)) {
                currentConfig.enabledProtections.remove(changed);
            } else {
                currentConfig.enabledProtections.add(changed);
            }
            rebuildProtectionsPanel(uiCommandBuilder, uiEventBuilder);
            sendUpdate(uiCommandBuilder, uiEventBuilder, false);
            return;
        }

        if (data.worldConfigUpdate != null) {
            switch(data.worldConfigUpdate) {
                case "ToggleWorldProtection" -> currentConfig.protectionEnabled = !currentConfig.protectionEnabled;
                case "ToggleWorldNotify" -> currentConfig.notifyPlayer = !currentConfig.notifyPlayer;
            }
            rebuildEditWorldConfigPanel(uiCommandBuilder);
            sendUpdate(uiCommandBuilder, uiEventBuilder, false);
            return;
        }

        if (data.configAction != null) {
            switch(currentPanelView) {
                case PANEL_VIEW.EDIT_GLOBAL_CONFIG -> {
                    switch(data.configAction) {
                        case "SaveBtn" -> saveGlobalConfig();
                        case "LoadBtn" -> {
                            syncGlobalSettings();
                            rebuildEditGlobalConfigPanel(uiCommandBuilder);
                        }
                    }
                }
                case PANEL_VIEW.EDIT_WORLD -> {
                    switch (data.configAction) {
                        case "SaveBtn" -> saveWorldConfig();
                        case "LoadBtn" -> {
                            syncConfigSettings();
                            rebuildProtectionsPanel(uiCommandBuilder, uiEventBuilder);
                            rebuildEditWorldConfigPanel(uiCommandBuilder);
                        }
                        case "DeleteBtn" -> {
                            deleteWorldConfig();
                            currentPanelView = PANEL_VIEW.CLEAR;
                            worldSearch = "";
                            currentWorld = "";
                            rebuildMainConfigPanel(uiCommandBuilder, uiEventBuilder);
                        }
                    }
                }
                case PANEL_VIEW.CREATE_NEW_CONFIG -> {
                    switch (data.configAction) {
                        case "SaveBtn" -> {
                            currentWorld = nameForWorld;
                            currentPanelView = PANEL_VIEW.EDIT_WORLD;
                            saveWorldConfig();
                            rebuildMainConfigPanel(uiCommandBuilder, uiEventBuilder);
                            rebuildAdminPanel(uiCommandBuilder);
                        }
                        case "LoadBtn" -> {
                            syncConfigSettings();
                            rebuildProtectionsPanel(uiCommandBuilder, uiEventBuilder);
                            rebuildEditWorldConfigPanel(uiCommandBuilder);
                        }
                    }
                }
                case PANEL_VIEW.EDIT_DEFAULT_WORLD -> {
                    switch (data.configAction) {
                        case "SaveBtn" -> {
                            saveWorldConfig();
                        }
                        case "LoadBtn" -> {
                            syncConfigSettings();
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

        if (data.clicked != null) {
            // All panel changing actions cause the world list to rebuild to update the highlighting
            currentWorld = "";
            switch (data.clicked) {
                case "DefaultWorldBtn" -> currentPanelView = PANEL_VIEW.EDIT_DEFAULT_WORLD;
                case "CreateConfigBtn" -> currentPanelView = PANEL_VIEW.CREATE_NEW_CONFIG;
                case "EditGlobalConfigBtn" -> currentPanelView = PANEL_VIEW.EDIT_GLOBAL_CONFIG;
                case "ExitPanelBtn" -> currentPanelView = PANEL_VIEW.CLEAR;
                default -> {
                    currentWorld = data.clicked;
                    currentPanelView = PANEL_VIEW.EDIT_WORLD;
                }
            }
            // All panel changing actions cause the world list to rebuild to update the highlighting
            rebuildWorldListPanel(uiCommandBuilder, uiEventBuilder);
            rebuildMainConfigPanel(uiCommandBuilder, uiEventBuilder);
            if (canAdministrate) rebuildAdminPanel(uiCommandBuilder);
        }

        // Apply updates to the client
        sendUpdate(uiCommandBuilder, uiEventBuilder, false);
    }

    public static class Data {
        public static final BuilderCodec<Data> CODEC = BuilderCodec.builder(Data.class, Data::new)
                .append(new KeyedCodec<>("@WorldSearchInput", Codec.STRING),
                        (data, value) -> data.search = value,
                        data -> data.search).add()
                .append(new KeyedCodec<>(PANEL_ACTION, Codec.STRING),
                        (data, value) -> data.clicked = value,
                        data -> data.clicked).add()
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
                .build();

        private String search;
        private String clicked;
        private String configAction;
        private String globalConfigUpdate;
        private String worldConfigUpdate;
        private String protectionUpdate;
        private String playerUpdate;
        private String nameWorldUpdate;
        private String playerNameUpdate;
        private String uuidUpdate;
        private String uuidAction;
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
}
