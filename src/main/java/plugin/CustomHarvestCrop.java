package plugin;

import com.hypixel.hytale.builtin.adventure.farming.interactions.HarvestCropInteraction;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil.getLogger;

/**
 * Currently unused since CustomUseBlockInteraction covers more cases.
 */

public class CustomHarvestCrop extends HarvestCropInteraction {
    public static final BuilderCodec<CustomHarvestCrop> CODEC;

    @Override
    protected void interactWithBlock(@Nonnull World world,
                                     @Nonnull CommandBuffer<EntityStore> commandBuffer,
                                     @Nonnull InteractionType type,
                                     @Nonnull InteractionContext context,
                                     @Nullable ItemStack itemInHand,
                                     @Nonnull Vector3i targetBlock,
                                     @Nonnull CooldownHandler cooldownHandler) {

        Ref<EntityStore> entityRef = context.getEntity();
        Player player = entityRef.getStore().getComponent(entityRef, Player.getComponentType());
        getLogger().info(String.format("%s harvested a crop using the custom interaction class",
                player.getReference().isValid() ? player.getDisplayName() : "No player found"));

        super.interactWithBlock(world, commandBuffer, type, context, itemInHand, targetBlock, cooldownHandler);
    }

    static {
        CODEC = BuilderCodec.builder(
                CustomHarvestCrop.class,
                CustomHarvestCrop::new,
                SimpleBlockInteraction.CODEC)
        .documentation("Custom harvest logic.")
                .build();
    }
}
