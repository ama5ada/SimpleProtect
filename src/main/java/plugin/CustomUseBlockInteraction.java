package plugin;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.Interaction;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityUseBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.UseBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

import static com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil.getLogger;

public class CustomUseBlockInteraction extends UseBlockInteraction {
    @Nonnull
    public static final BuilderCodec<CustomUseBlockInteraction> CODEC;

    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull Vector3i targetBlock, @Nonnull CooldownHandler cooldownHandler) {
        doInteraction(type, context, world, targetBlock, true);
    }

    protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull World world, @Nonnull Vector3i targetBlock) {
        doInteraction(type, context, world, targetBlock, false);
    }

    private static void doInteraction(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull World world, @Nonnull Vector3i targetBlock, boolean fireEvent) {
        BlockType blockType = world.getBlockType(targetBlock);
        boolean shouldProtect = ConfigState.get().isProtected();

        if (type.equals(InteractionType.Use) && shouldProtect) {
            String blockGroup = blockType.getGroup();
            List<String> ignore = List.of("Plant", "Stone");
            Player player = context.getEntity().getStore().getComponent(context.getEntity(), Player.getComponentType());

            if (player != null && (blockGroup == null || ignore.contains(blockGroup))) {
                String permissionNode = ConfigState.get().getPermission();
                if (player.hasPermission(permissionNode)) {
                    getLogger().info(
                            String.format("Player %s was allowed to interact with a block!",
                                    player.getDisplayName()));
                } else {
                    if (ConfigState.get().notifyPlayer()) {
                        player.sendMessage(Message.raw("[Simple Protect] Building is disabled."));
                    }
                    getLogger().info(
                            String.format("Player %s was prevented from interacting with a block.",
                            player.getDisplayName())
                    );
                    context.getState().state = InteractionState.Finished;
                    return;
                }
            }
        }

        String blockTypeInteraction = (String)blockType.getInteractions().get(type);
        if (blockTypeInteraction == null) {
            context.getState().state = InteractionState.Failed;
        } else {
            Ref<EntityStore> ref = context.getEntity();
            CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();

            assert commandBuffer != null;

            if (fireEvent) {
                UseBlockEvent.Pre event = new UseBlockEvent.Pre(type, context, targetBlock, blockType);
                commandBuffer.invoke(ref, event);
                if (event.isCancelled()) {
                    context.getState().state = InteractionState.Failed;
                    return;
                }
            }

            context.getState().state = InteractionState.Finished;
            context.execute(RootInteraction.getRootInteractionOrUnknown(blockTypeInteraction));
            if (fireEvent) {
                UseBlockEvent.Post event = new UseBlockEvent.Post(type, context, targetBlock, blockType);
                commandBuffer.invoke(ref, event);
                HytaleServer.get().getEventBus().dispatchFor(LivingEntityUseBlockEvent.class, world.getName()).dispatch(new LivingEntityUseBlockEvent(context.getEntity(), blockType.getId()));
            }
        }
    }

    @Nonnull
    protected Interaction generatePacket() {
        return new com.hypixel.hytale.protocol.UseBlockInteraction();
    }

    @Nonnull
    public String toString() {
        return "UseBlockInteraction{} " + super.toString();
    }

    static {
        CODEC = BuilderCodec.builder(
                CustomUseBlockInteraction.class,
                        CustomUseBlockInteraction::new,
                        UseBlockInteraction.CODEC)
                .documentation("Custom block use logic.")
                .build();
    }
}
