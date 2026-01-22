package plugin.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.ConfigState;
import plugin.ProtectionUtil;
import plugin.events.PlaceFluidEvent;
import plugin.events.RefillContainerEvent;
import plugin.types.EVENT_TYPE;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil.getLogger;

public class RefillContainerEventSystem extends EntityEventSystem<EntityStore, RefillContainerEvent> {
    public RefillContainerEventSystem(@Nonnull Class<RefillContainerEvent> eventType) {
        super(eventType);
    }

    @Override
    public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull RefillContainerEvent refillContainerEvent) {
        Player player = archetypeChunk.getComponent(i, Player.getComponentType());
        String worldName = player.getWorld().getName();
        if (ProtectionUtil.ShouldProtect(player, worldName, EVENT_TYPE.FLUID_PICKUP)) refillContainerEvent.setCancelled(true);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }
}
