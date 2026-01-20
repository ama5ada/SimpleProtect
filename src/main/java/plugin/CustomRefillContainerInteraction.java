package plugin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.RefillContainerInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil.getLogger;

public class CustomRefillContainerInteraction extends RefillContainerInteraction {
    public static final BuilderCodec<CustomRefillContainerInteraction> CODEC;
    protected Map<String, RefillState> refillStateMap;
    @Nullable
    protected int[] allowedFluidIds;
    @Nullable
    protected Int2ObjectMap<String> fluidToState;

    protected int[] getAllowedFluidIds() {
        if (this.allowedFluidIds != null) {
            return this.allowedFluidIds;
        } else {
            this.allowedFluidIds = this.refillStateMap.values().stream().map(RefillState::getAllowedFluids).flatMap(Arrays::stream).mapToInt((key) -> Fluid.getAssetMap().getIndex(key)).sorted().toArray();
            return this.allowedFluidIds;
        }
    }

    protected Int2ObjectMap<String> getFluidToState() {
        if (this.fluidToState != null) {
            return this.fluidToState;
        } else {
            this.fluidToState = new Int2ObjectOpenHashMap();
            this.refillStateMap.forEach((s, refillState) -> {
                for(String key : refillState.getAllowedFluids()) {
                    this.fluidToState.put(Fluid.getAssetMap().getIndex(key), s);
                }

            });
            return this.fluidToState;
        }
    }

    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull Vector3i targetBlock, @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();

        Player player = context.getEntity().getStore().getComponent(context.getEntity(), Player.getComponentType());

        if (player.getReference().isValid() && ConfigState.get().isProtected()) {
            String permissionNode = ConfigState.get().getPermission();
            if (player.hasPermission(permissionNode)) {
                getLogger().info(
                        String.format("Player %s was allowed to refill a fluid container!",
                                player.getDisplayName()));
            } else {
                if (ConfigState.get().notifyPlayer()) {
                    player.sendMessage(Message.raw("[Simple Protect] Building is disabled."));
                }
                getLogger().info(
                        String.format("Player %s was prevented from refilling a fluid container.",
                                player.getDisplayName())
                );
                context.getState().state = InteractionState.Finished;
                return;
            }
        }

        Entity var10 = EntityUtils.getEntity(ref, commandBuffer);
        if (var10 instanceof LivingEntity livingEntity) {
            BlockPosition var24 = context.getClientState().blockPosition;
            InteractionSyncData state = context.getState();
            if (var24 == null) {
                state.state = InteractionState.Failed;
            } else {
                Ref<ChunkStore> section = world.getChunkStore().getChunkSectionReference(ChunkUtil.chunkCoordinate(var24.x), ChunkUtil.chunkCoordinate(var24.y), ChunkUtil.chunkCoordinate(var24.z));
                if (section != null) {
                    FluidSection fluidSection = (FluidSection)section.getStore().getComponent(section, FluidSection.getComponentType());
                    if (fluidSection != null) {
                        int fluidId = fluidSection.getFluidId(var24.x, var24.y, var24.z);
                        int[] allowedBlockIds = this.getAllowedFluidIds();
                        if (allowedBlockIds != null && Arrays.binarySearch(allowedBlockIds, fluidId) < 0) {
                            state.state = InteractionState.Failed;
                        } else {
                            String newState = (String)this.getFluidToState().get(fluidId);
                            if (newState == null) {
                                state.state = InteractionState.Failed;
                            } else {
                                ItemStack current = context.getHeldItem();
                                Item newItemAsset = current.getItem().getItemForState(newState);
                                if (newItemAsset == null) {
                                    state.state = InteractionState.Failed;
                                } else {
                                    RefillState refillState = (RefillState)this.refillStateMap.get(newState);
                                    if (newItemAsset.getId().equals(current.getItemId())) {
                                        if (refillState != null) {
                                            double newDurability = MathUtil.maxValue(refillState.durability, current.getMaxDurability());
                                            if (newDurability <= current.getDurability()) {
                                                state.state = InteractionState.Failed;
                                                return;
                                            }

                                            ItemStack newItem = current.withIncreasedDurability(newDurability);
                                            ItemStackSlotTransaction transaction = context.getHeldItemContainer().setItemStackForSlot((short)context.getHeldItemSlot(), newItem);
                                            if (!transaction.succeeded()) {
                                                state.state = InteractionState.Failed;
                                                return;
                                            }

                                            context.setHeldItem(newItem);
                                        }
                                    } else {
                                        ItemStackSlotTransaction removeEmptyTransaction = context.getHeldItemContainer().removeItemStackFromSlot((short)context.getHeldItemSlot(), current, 1);
                                        if (!removeEmptyTransaction.succeeded()) {
                                            state.state = InteractionState.Failed;
                                            return;
                                        }

                                        ItemStack refilledContainer = new ItemStack(newItemAsset.getId(), 1);
                                        if (refillState != null && refillState.durability > (double)0.0F) {
                                            refilledContainer = refilledContainer.withDurability(refillState.durability);
                                        }

                                        if (current.getQuantity() == 1) {
                                            ItemStackSlotTransaction addFilledTransaction = context.getHeldItemContainer().setItemStackForSlot((short)context.getHeldItemSlot(), refilledContainer);
                                            if (!addFilledTransaction.succeeded()) {
                                                state.state = InteractionState.Failed;
                                                return;
                                            }

                                            context.setHeldItem(refilledContainer);
                                        } else {
                                            SimpleItemContainer.addOrDropItemStack(commandBuffer, ref, livingEntity.getInventory().getCombinedHotbarFirst(), refilledContainer);
                                            context.setHeldItem(context.getHeldItemContainer().getItemStack((short)context.getHeldItemSlot()));
                                        }
                                    }

                                    if (refillState != null && refillState.getTransformFluid() != null) {
                                        int transformedFluid = Fluid.getFluidIdOrUnknown(refillState.getTransformFluid(), "Unknown fluid %s", new Object[]{refillState.getTransformFluid()});
                                        boolean placed = fluidSection.setFluid(var24.x, var24.y, var24.z, transformedFluid, (byte)((Fluid)Fluid.getAssetMap().getAsset(transformedFluid)).getMaxFluidLevel());
                                        if (!placed) {
                                            state.state = InteractionState.Failed;
                                        }

                                        world.performBlockUpdate(var24.x, var24.y, var24.z);
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull World world, @Nonnull Vector3i targetBlock) {
    }

    @Nonnull
    protected Interaction generatePacket() {
        return new com.hypixel.hytale.protocol.RefillContainerInteraction();
    }

    protected void configurePacket(Interaction packet) {
        super.configurePacket(packet);
        com.hypixel.hytale.protocol.RefillContainerInteraction p = (com.hypixel.hytale.protocol.RefillContainerInteraction)packet;
        p.refillFluids = this.getAllowedFluidIds();
    }

    @Nonnull
    public String toString() {
        String var10000 = String.valueOf(this.refillStateMap);
        return "CustomRefillContainerInteraction{refillStateMap=" + var10000 + ", allowedBlockIds=" + Arrays.toString(this.allowedFluidIds) + ", blockToState=" + String.valueOf(this.fluidToState) + "} " + super.toString();
    }

    static {
        CODEC =
                BuilderCodec
                        .builder(
                                CustomRefillContainerInteraction.class,
                                CustomRefillContainerInteraction::new,
                                SimpleBlockInteraction.CODEC
                        )
                        .documentation("Refills a container item that is currently held.")
                        .appendInherited(
                                new KeyedCodec<>(
                                        "States",
                                        new MapCodec<>(
                                                RefillState.CODEC,
                                                HashMap::new
                                        )
                                ),
                                (interaction, value) ->
                                        interaction.refillStateMap = value,
                                interaction ->
                                        interaction.refillStateMap,
                                (obj, parent) ->
                                        obj.refillStateMap = parent.refillStateMap
                        )
                        .addValidator(Validators.nonNull())
                        .add()
                        .afterDecode((customRefillContainerInteraction) -> {
                            customRefillContainerInteraction.allowedFluidIds = null;
                            customRefillContainerInteraction.fluidToState = null;
                        })
                        .build();

    }

    protected static class RefillState {
        public static final BuilderCodec<RefillState> CODEC;
        protected String[] allowedFluids;
        protected String transformFluid;
        protected double durability = (double) -1.0F;

        public String[] getAllowedFluids() {
            return this.allowedFluids;
        }

        public String getTransformFluid() {
            return this.transformFluid;
        }

        public double getDurability() {
            return this.durability;
        }

        @Nonnull
        public String toString() {
            String var10000 = Arrays.toString(this.allowedFluids);
            return "RefillState{allowedFluids=" + var10000 + ", transformFluid='" + this.transformFluid + "', durability=" + this.durability + "}";
        }

        static {
            CODEC =
                    BuilderCodec
                            .builder(
                                    RefillState.class,
                                    RefillState::new
                            )

                            // AllowedFluids (required)
                            .append(
                                    new KeyedCodec<>(
                                            "AllowedFluids",
                                            new ArrayCodec<>(Codec.STRING, String[]::new)
                                    ),
                                    (state, value) -> state.allowedFluids = value,
                                    state -> state.allowedFluids
                            )
                            .addValidator(Validators.nonNull())
                            .add()

                            // TransformFluid
                            .addField(
                                    new KeyedCodec<>("TransformFluid", Codec.STRING),
                                    (state, value) -> state.transformFluid = value,
                                    state -> state.transformFluid
                            )

                            // Durability
                            .addField(
                                    new KeyedCodec<>("Durability", Codec.DOUBLE),
                                    (state, value) -> state.durability = value,
                                    state -> state.durability
                            )

                            .build();
        }
    }
}
