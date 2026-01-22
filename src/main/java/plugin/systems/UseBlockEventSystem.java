package plugin.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.ConfigState;
import plugin.ProtectionUtil;
import plugin.types.EVENT_TYPE;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil.getLogger;

public class UseBlockEventSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {
    public UseBlockEventSystem(@Nonnull Class<UseBlockEvent.Pre> eventType) {
        super(eventType);
    }

    @Override
    public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull UseBlockEvent.Pre useBlockEvent) {
        Player player = archetypeChunk.getComponent(i, Player.getComponentType());
        String worldName = player.getWorld().getName();
        if (ProtectionUtil.ShouldProtect(player, worldName, EVENT_TYPE.BLOCK_USE)) useBlockEvent.setCancelled(true);
        InteractionManager manager = useBlockEvent.getContext().getInteractionManager();
        if (manager != null) {
            InteractionChain chain = useBlockEvent.getContext().getChain();
            if (chain != null) {
                manager.cancelChains(chain);
            }
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }
}
