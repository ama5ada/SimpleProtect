package plugin;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil.getLogger;

public class PlaceBlockEventSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
    protected PlaceBlockEventSystem(@Nonnull Class<PlaceBlockEvent> eventType) {
        super(eventType);
    }

    @Override
    public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull PlaceBlockEvent placeBlockEvent) {
        String permissionNode = ConfigState.get().getPermission();
        Player player = archetypeChunk.getComponent(i, Player.getComponentType());
        boolean shouldProtect = ConfigState.get().isProtected();

        if (player != null && shouldProtect) {
            if (player.hasPermission(permissionNode)) {
                getLogger().info(
                        String.format("Player %s was allowed to place a block!", player.getDisplayName()));
            } else {
                if (ConfigState.get().notifyPlayer()) {
                    player.sendMessage(Message.raw("[Simple Protect] Building is disabled."));
                }
                getLogger().info(
                        String.format("Player %s was prevented from placing a block.", player.getDisplayName()));
                placeBlockEvent.setCancelled(true);
            }
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }
}
