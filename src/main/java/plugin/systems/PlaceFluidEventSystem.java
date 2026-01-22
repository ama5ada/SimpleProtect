package plugin.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.ProtectionUtil;
import plugin.events.PlaceFluidEvent;
import plugin.types.EVENT_TYPE;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PlaceFluidEventSystem extends EntityEventSystem<EntityStore, PlaceFluidEvent> {
    public PlaceFluidEventSystem(@Nonnull Class<PlaceFluidEvent> eventType) {
        super(eventType);
    }

    @Override
    public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull PlaceFluidEvent placeFluidEvent) {
        Player player = archetypeChunk.getComponent(i, Player.getComponentType());
        String worldName = player.getWorld().getName();
        if (ProtectionUtil.ShouldProtect(player, worldName, EVENT_TYPE.FLUID_PLACE)) placeFluidEvent.setCancelled(true);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }
}
