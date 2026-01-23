package plugin.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.ProtectionUtil;
import plugin.types.EVENT_TYPE;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DamageBlockEventSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {
    public DamageBlockEventSystem(@Nonnull Class<DamageBlockEvent> eventType) {
        super(eventType);
    }

    @Override
    public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull DamageBlockEvent damageBlockEvent) {
        Player player = archetypeChunk.getComponent(i, Player.getComponentType());
        if (ProtectionUtil.ShouldProtect(player, EVENT_TYPE.BLOCK_DAMAGE)) damageBlockEvent.setCancelled(true);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }
}
