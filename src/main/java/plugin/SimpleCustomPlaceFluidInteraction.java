//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package plugin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockFace;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.asset.type.fluid.FluidTicker;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.PlaceFluidInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil.getLogger;

public class SimpleCustomPlaceFluidInteraction extends SimpleBlockInteraction {
    @Nonnull
    public static final BuilderCodec<SimpleCustomPlaceFluidInteraction> CODEC;
    @Nullable
    protected String fluidKey;
    protected boolean removeItemInHand = true;

    @Nullable
    public String getFluidKey() {
        return this.fluidKey;
    }

    @Nonnull
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.Client;
    }

    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull Vector3i targetBlock, @Nonnull CooldownHandler cooldownHandler) {
        Store<ChunkStore> store = world.getChunkStore().getStore();

        Player player = context.getEntity().getStore().getComponent(context.getEntity(), Player.getComponentType());

        if (player.getReference().isValid() && ConfigState.get().isProtected()) {
            String permissionNode = ConfigState.get().getPermission();
            if (player.hasPermission(permissionNode)) {
                getLogger().info(
                        String.format("Player %s was allowed to place a fluid!",
                                player.getDisplayName()));
            } else {
                if (ConfigState.get().notifyPlayer()) {
                    player.sendMessage(Message.raw("[Simple Protect] Building is disabled."));
                }
                getLogger().info(
                        String.format("Player %s was prevented from placing a fluid.",
                                player.getDisplayName())
                );
                context.getState().state = InteractionState.Finished;
                return;
            }
        }

        int fluidIndex = Fluid.getFluidIdOrUnknown(this.fluidKey, "Unknown fluid: %s", new Object[]{this.fluidKey});
        Fluid fluid = (Fluid)Fluid.getAssetMap().getAsset(fluidIndex);
        Vector3i target = targetBlock;
        BlockType targetBlockType = world.getBlockType(targetBlock);
        if (FluidTicker.isSolid(targetBlockType)) {
            target = targetBlock.clone();
            BlockFace face = BlockFace.fromProtocolFace(context.getClientState().blockFace);
            target.add(face.getDirection());
        }

        Ref<ChunkStore> section = world.getChunkStore().getChunkSectionReference(ChunkUtil.chunkCoordinate(target.x), ChunkUtil.chunkCoordinate(target.y), ChunkUtil.chunkCoordinate(target.z));
        if (section != null) {
            FluidSection fluidSectionComponent = (FluidSection)store.getComponent(section, FluidSection.getComponentType());
            if (fluidSectionComponent != null) {
                fluidSectionComponent.setFluid(target.x, target.y, target.z, fluid, (byte)fluid.getMaxFluidLevel());
                Ref<ChunkStore> chunkColumn = world.getChunkStore().getChunkReference(ChunkUtil.indexChunkFromBlock(target.x, target.z));
                if (chunkColumn != null) {
                    BlockChunk blockChunkComponent = (BlockChunk)store.getComponent(chunkColumn, BlockChunk.getComponentType());
                    blockChunkComponent.setTicking(target.x, target.y, target.z, true);
                    Ref<EntityStore> ref = context.getEntity();
                    Player playerComponent = (Player)commandBuffer.getComponent(ref, Player.getComponentType());
                    PlayerRef playerRefComponent = (PlayerRef)commandBuffer.getComponent(ref, PlayerRef.getComponentType());
                    if ((playerRefComponent == null || playerComponent != null && playerComponent.getGameMode() == GameMode.Adventure) && itemInHand.getQuantity() == 1 && this.removeItemInHand) {
                        context.setHeldItem((ItemStack)null);
                    }

                }
            }
        }
    }

    protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull World world, @Nonnull Vector3i targetBlock) {
    }

    public boolean needsRemoteSync() {
        return true;
    }

    @Nonnull
    public String toString() {
        String var10000 = this.fluidKey;
        return "PlaceBlockInteraction{blockTypeKey=" + var10000 + ", removeItemInHand=" + this.removeItemInHand + "} " + super.toString();
    }

    static {
        CODEC =
                BuilderCodec
                        .builder(
                                SimpleCustomPlaceFluidInteraction.class,
                                SimpleCustomPlaceFluidInteraction::new,
                                SimpleInteraction.CODEC
                        )
                        .documentation("Places the current or given block.")

                        // FluidToPlace
                        .append(
                                new KeyedCodec<>("FluidToPlace", Codec.STRING),
                                (i, v) -> i.fluidKey = v,
                                i -> i.fluidKey
                        )
                        .addValidatorLate(() ->
                                Fluid.VALIDATOR_CACHE.getValidator().late()
                        )
                        .add()

                        // RemoveItemInHand
                        .append(
                                new KeyedCodec<>("RemoveItemInHand", Codec.BOOLEAN),
                                (i, v) -> i.removeItemInHand = v,
                                i -> i.removeItemInHand
                        )
                        .add()

                        .build();
    }

}