package plugin.gui.pages;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.gui.pages.SimpleProtectUIRenderer.RenderScope;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

/**
 * Main UI controller that coordinates between the state (Model), renderer (View), and handler (Controller).
 * - Model: SimpleProtectUIState - holds all UI state
 * - View: SimpleProtectUIRenderer - handles all rendering logic
 * - Controller: SimpleProtectUIHandler - handles all event processing and business logic
 */
public class SimpleProtectUI extends InteractiveCustomUIPage<SimpleProtectUI.Data> {

    // MVC Components
    private final SimpleProtectUIState uiState;
    private final SimpleProtectUIRenderer renderer;
    private final SimpleProtectUIHandler handler;

    // Event type constants for data codec
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

        UUID playerUUID = playerRef.getUuid();
        this.uiState = new SimpleProtectUIState(playerUUID);
        this.renderer = new SimpleProtectUIRenderer(uiState);
        this.handler = new SimpleProtectUIHandler(uiState);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder uiCommandBuilder,
                      @Nonnull UIEventBuilder uiEventBuilder,
                      @Nonnull Store<EntityStore> store) {

        // Build initial static UI
        renderer.buildInitialUI(uiCommandBuilder, uiEventBuilder);

        // Render the main panel according to normal rules
        renderer.render(RenderScope.MAIN_PANEL, uiCommandBuilder, uiEventBuilder);

        // Render admin config options and world list if the player running the command is admin
        // Eventually, this will be updated so the world list is always rendered based on worlds that the player
        // Has some level of permission to manage
        if (uiState.canAdministrate()) {
            renderer.renderAdminPanel(uiCommandBuilder, uiEventBuilder);
            renderer.render(RenderScope.WORLD_LIST, uiCommandBuilder, uiEventBuilder);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                Data data) {
        super.handleDataEvent(ref, store, data);

        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        UIEventBuilder uiEventBuilder = new UIEventBuilder();

        // Process event and get scopes that need re-rendering
        Set<RenderScope> changedComponents = handler.handleEvent(data);

        // Render all changed components
        for (RenderScope scope : changedComponents) {
            renderer.render(scope, uiCommandBuilder, uiEventBuilder);
        }

        // Send updates to client
        sendUpdate(uiCommandBuilder, uiEventBuilder, false);

        // Refresh player panels if world player settings changed
        if (changedComponents.contains(RenderScope.WORLD_PLAYER_SETTINGS)) {
            refreshPlayerPanels();
        }
    }

    /**
     * Refresh player panels asynchronously
     */
    private void refreshPlayerPanels() {
        handler.refreshPlayerLists(
                this::runOnWorldThread,
                (pageResult, isAllowed) -> {
                    UICommandBuilder uiCommandBuilder = new UICommandBuilder();
                    UIEventBuilder uiEventBuilder = new UIEventBuilder();

                    if (isAllowed) {
                        renderer.renderAllowedPlayersPanel(uiCommandBuilder, uiEventBuilder, pageResult);
                    } else {
                        renderer.renderDisallowedPlayersPanel(uiCommandBuilder, uiEventBuilder, pageResult);
                    }

                    sendUpdate(uiCommandBuilder, uiEventBuilder, false);
                }
        );
    }

    /**
     * Execute a task on the world thread
     */
    private void runOnWorldThread(Runnable task) {
        if (playerRef.getWorldUuid() == null) return;

        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world != null && world.isAlive()) {
            world.execute(task);
        }
    }

    /**
     * Data class for event handling (serialize UI events to an object)
     */
    public static class Data {
        public static final BuilderCodec<Data> CODEC = BuilderCodec.builder(Data.class, Data::new)
                .append(new KeyedCodec<>("@WorldSearchInput", Codec.STRING),
                        (data, value) -> data.worldFilter = value,
                        data -> data.worldFilter).add()
                .append(new KeyedCodec<>(PANEL_CLICK, Codec.STRING),
                        (data, value) -> data.mainPanelUpdate = value,
                        data -> data.mainPanelUpdate).add()
                .append(new KeyedCodec<>(CONFIG_ACTION, Codec.STRING),
                        (data, value) -> data.configSyncAction = value,
                        data -> data.configSyncAction).add()
                .append(new KeyedCodec<>(GLOBAL_CONFIG_UPDATE, Codec.STRING),
                        (data, value) -> data.globalConfigUpdate = value,
                        data -> data.globalConfigUpdate).add()
                .append(new KeyedCodec<>(WORLD_CONFIG_UPDATE, Codec.STRING),
                        (data, value) -> data.worldConfigUpdate = value,
                        data -> data.worldConfigUpdate).add()
                .append(new KeyedCodec<>(PROTECTION_UPDATE, Codec.STRING),
                        (data, value) -> data.worldProtectionUpdate = value,
                        data -> data.worldProtectionUpdate).add()
                .append(new KeyedCodec<>(PLAYER_ACTION, Codec.STRING),
                        (data, value) -> data.playerGroupUpdate = value,
                        data -> data.playerGroupUpdate).add()
                .append(new KeyedCodec<>("@WorldNameInput", Codec.STRING),
                        (data, value) -> data.nameWorldUpdate = value,
                        data -> data.nameWorldUpdate).add()
                .append(new KeyedCodec<>("@PlayerSearchInput", Codec.STRING),
                        (data, value) -> data.playerSearchUpdate = value,
                        data -> data.playerSearchUpdate).add()
                .append(new KeyedCodec<>("@UUIDInput", Codec.STRING),
                        (data, value) -> data.uuidInputUpdate = value,
                        data -> data.uuidInputUpdate).add()
                .append(new KeyedCodec<>(UUID_ACTION, Codec.STRING),
                        (data, value) -> data.addUuidAction = value,
                        data -> data.addUuidAction).add()
                .append(new KeyedCodec<>(GROUP_ACTION, Codec.STRING),
                        (data, value) -> data.groupClicked = value,
                        data -> data.groupClicked).add()
                .build();

        String worldFilter;
        String mainPanelUpdate;
        String configSyncAction;
        String globalConfigUpdate;
        String worldConfigUpdate;
        String worldProtectionUpdate;
        String playerGroupUpdate;
        String nameWorldUpdate;
        String playerSearchUpdate;
        String uuidInputUpdate;
        String addUuidAction;
        String groupClicked;
    }
}