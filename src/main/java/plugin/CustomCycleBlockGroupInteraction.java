package plugin;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.CycleBlockGroupInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil.getLogger;

public class CustomCycleBlockGroupInteraction extends CycleBlockGroupInteraction {
    public static final BuilderCodec<CustomCycleBlockGroupInteraction> CODEC;

    @Override
    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull Vector3i targetBlock, @Nonnull CooldownHandler cooldownHandler) {
        Player player = context.getEntity().getStore().getComponent(context.getEntity(), Player.getComponentType());

        if (ConfigState.get().isProtected() && player != null) {
            String permissionNode = ConfigState.get().getPermission();
            if (player.hasPermission(permissionNode)) {
                getLogger().info(
                        String.format("Player %s was allowed to cycle a block group!",
                                player.getDisplayName()));
            } else {
                if (ConfigState.get().notifyPlayer()) {
                    player.sendMessage(Message.raw("[Simple Protect] Building is disabled."));
                }
                getLogger().info(
                        String.format("Player %s was prevented from cycling a block group.",
                                player.getDisplayName())
                );
                return;
            }
        }

        super.interactWithBlock(world, commandBuffer, type, context, itemInHand, targetBlock, cooldownHandler);
    }

    static {
        CODEC = BuilderCodec.builder(
                        CustomCycleBlockGroupInteraction.class,
                        CustomCycleBlockGroupInteraction::new,
                        SimpleBlockInteraction.CODEC)
                .documentation("Custom CycleBlockGroup logic.")
                .build();
    }
}
