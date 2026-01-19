package plugin;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil.getLogger;

public class BreakBlockEventSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    protected BreakBlockEventSystem(@Nonnull Class<BreakBlockEvent> eventType) {
        super(eventType);
    }

    @Override
    public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull BreakBlockEvent breakBlockEvent) {

        String permissionNode = ConfigState.get().getPermission();
        Player player = archetypeChunk.getComponent(i, Player.getComponentType());
        boolean shouldProtect = ConfigState.get().isProtected();

        if (player != null && shouldProtect) {
            if (player.hasPermission(permissionNode)) {
                getLogger().info(
                        String.format("Player %s was allowed to break a block!", player.getDisplayName()));
            } else {
                if (ConfigState.get().notifyPlayer()) {
                    player.sendMessage(Message.raw("[Simple Protect] Building is disabled."));
                }
                getLogger().info(
                        String.format("Player %s was prevented from breaking a block.", player.getDisplayName()));
                breakBlockEvent.setCancelled(true);
            }
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }
}
