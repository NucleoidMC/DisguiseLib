package xyz.nucleoid.disguiselib.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nucleoid.disguiselib.EntityDisguise;
import xyz.nucleoid.disguiselib.mixin.accessor.EntityTrackerEntryAccessor;
import xyz.nucleoid.disguiselib.mixin.accessor.PlayerListS2CPacketAccessor;
import xyz.nucleoid.disguiselib.mixin.accessor.ThreadedAnvilChunkStorageAccessor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.minecraft.entity.EntityType.PLAYER;
import static net.minecraft.entity.player.PlayerEntity.PLAYER_MODEL_PARTS;
import static net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action.ADD_PLAYER;
import static net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action.REMOVE_PLAYER;

@Mixin(Entity.class)
public abstract class EntityMixin_Disguise implements EntityDisguise {

    @Unique
    private Entity disguiselib$disguiseEntity;

    @Shadow public abstract EntityType<?> getType();

    @Shadow public World world;

    @Shadow private int entityId;

    @Shadow public abstract float getHeadYaw();

    @Shadow protected UUID uuid;

    @Shadow public abstract Text getName();

    @Shadow public abstract int getEntityId();

    @Shadow public abstract DataTracker getDataTracker();

    @Shadow @Nullable public abstract Text getCustomName();

    @Shadow private EntityDimensions dimensions;

    @Shadow private float standingEyeHeight;

    @Shadow protected abstract float getEyeHeight(EntityPose pose, EntityDimensions dimensions);

    @Unique
    private boolean disguiselib$disguised, disguiselib$disguiseAlive;
    @Unique
    private EntityType<?> disguiselib$disguiseType;
    @Unique
    private final Entity disguiselib$entity = (Entity) (Object) this;
    @Unique
    private GameProfile disguiselib$profile;

    /**
     * Tells you the disguised status.
     * @return true if entity is disguised, otherwise false.
     */
    @Override
    public boolean isDisguised() {
        return this.disguiselib$disguised;
    }

    /**
     * Sets entity's disguise from {@link EntityType}
     * @param entityType the type to disguise this entity into
     */
    @Override
    public void disguiseAs(EntityType<?> entityType) {
        this.disguiselib$disguised = true;
        this.disguiselib$disguiseType = entityType;

        PlayerManager manager = this.world.getServer().getPlayerManager();

        if(this.disguiselib$disguiseEntity != null && this.disguiselib$disguiseEntity.getType() != entityType && this.disguiselib$entity instanceof ServerPlayerEntity) {
            // Removing previous disguise if this is player
            // (we have it saved under a separate id)
            ((ServerPlayerEntity) this.disguiselib$entity).networkHandler.sendPacket(new EntitiesDestroyS2CPacket(this.disguiselib$disguiseEntity.getEntityId()));
        }

        if(entityType == PLAYER) {
            if(this.disguiselib$profile == null)
                this.disguiselib$profile = new GameProfile(this.uuid, this.getName().getString());
            //noinspection MixinInnerClass
            this.disguiselib$disguiseEntity = new PlayerEntity(this.world, this.disguiselib$entity.getBlockPos(), this.getHeadYaw(), this.disguiselib$profile) {
                @Override
                public boolean isSpectator() {
                    return false;
                }

                @Override
                public boolean isCreative() {
                    return false;
                }
            };
            // Showing all skin parts
            this.disguiselib$disguiseEntity.getDataTracker().set(PLAYER_MODEL_PARTS, (byte) 0x7f);
        } else {
            if(this.disguiselib$disguiseEntity == null)
                this.disguiselib$disguiseEntity = entityType.create(world);

            if(this.disguiselib$profile != null) {
                // Previous type was player, we have to send a player remove packet
                PlayerListS2CPacket listPacket = new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER);
                PlayerListS2CPacketAccessor listPacketAccessor = (PlayerListS2CPacketAccessor) listPacket;
                listPacketAccessor.setEntries(Collections.singletonList(listPacket.new Entry(this.disguiselib$profile, 0, GameMode.SURVIVAL, new LiteralText(this.disguiselib$profile.getName()))));
                manager.sendToAll(listPacket);
            }
            this.disguiselib$profile = null;
        }

        this.disguiselib$disguiseAlive = entityType == PLAYER || this.disguiselib$disguiseEntity instanceof LivingEntity;

        RegistryKey<World> worldRegistryKey = this.world.getRegistryKey();

        this.disguiselib$disguiseEntity.setNoGravity(true);
        this.disguiselib$disguiseEntity.setCustomName(this.getCustomName());

        // Dimensions ??
        this.dimensions = entityType.getDimensions();
        this.standingEyeHeight = this.getEyeHeight(EntityPose.STANDING, this.dimensions);

        // Updating entity on the client
        manager.sendToDimension(new EntitiesDestroyS2CPacket(this.entityId), worldRegistryKey);
        manager.sendToDimension(new EntitySpawnS2CPacket(this.disguiselib$entity), worldRegistryKey); // will be replaced by network handler

        manager.sendToDimension(new EntityTrackerUpdateS2CPacket(this.entityId, this.getDataTracker(), true), worldRegistryKey);
        manager.sendToDimension(new EntityEquipmentUpdateS2CPacket(this.entityId, this.getEquipment()), worldRegistryKey); // Reload equipment
        manager.sendToDimension(new EntitySetHeadYawS2CPacket(this.disguiselib$entity, (byte)((int)(this.getHeadYaw() * 256.0F / 360.0F))), worldRegistryKey); // Head correction
    }

    /**
     * Sets entity's disguise from {@link Entity}
     *
     * @param entity the entity to disguise into
     */
    @Override
    public void disguiseAs(Entity entity) {
        if(this.disguiselib$entity instanceof ServerPlayerEntity && this.disguiselib$disguiseEntity != null) {
            // Removing old disguise entity
            ((ServerPlayerEntity) this.disguiselib$entity).networkHandler.sendPacket(new EntitiesDestroyS2CPacket(this.disguiselib$disguiseEntity.getEntityId()));
        }

        this.disguiselib$disguiseEntity = entity;
        if(entity instanceof PlayerEntity) {
            this.disguiselib$profile = ((PlayerEntity) entity).getGameProfile();
        }
        this.disguiseAs(entity.getType());
    }

    /**
     * Gets equipment as list of {@link Pair Pairs}.
     * Requires entity to be an instanceof {@link LivingEntity}.
     *
     * @return equipment list of pairs.
     */
    @Unique
    private List<Pair<EquipmentSlot, ItemStack>> getEquipment() {
        if(disguiselib$entity instanceof LivingEntity)
            return Arrays.stream(EquipmentSlot.values()).map(slot -> new Pair<>(slot, ((LivingEntity) disguiselib$entity).getEquippedStack(slot))).collect(Collectors.toList());
        return Collections.emptyList();
    }

    /**
     * Clears the disguise - sets the {@link EntityMixin_Disguise#disguiselib$disguiseType} back to original.
     */
    @Override
    public void removeDisguise() {
        this.disguiseAs(this.getType());
        this.disguiselib$disguised = false;
        this.disguiselib$disguiseEntity = null;
    }

    /**
     * Gets the disguise entity type
     * @return disguise entity type or real type if there's no disguise
     */
    @Override
    public EntityType<?> getDisguiseType() {
        return this.disguiselib$disguiseType;
    }

    /**
     * Gets the disguise entity.
     *
     * @return disguise entity or null if there's no disguise
     */
    @Nullable
    @Override
    public Entity getDisguiseEntity() {
        return this.disguiselib$disguiseEntity;
    }

    /**
     * Whether disguise type entity is an instance of {@link LivingEntity}.
     * @return true whether the disguise type is an instance of {@link LivingEntity}, otherwise false.
     */
    @Override
    public boolean disguiseAlive() {
        return this.disguiselib$disguiseAlive;
    }

    /**
     * Sets the GameProfile
     *
     * @param gameProfile a new profile for the entity.
     */
    @Override
    public void setGameProfile(@Nullable GameProfile gameProfile) {
        this.disguiselib$profile = gameProfile;
        this.disguiselib$sendProfileUpdates();
    }

    /**
     * Gets the {@link GameProfile} for disguised entity,
     * used when disguising as player.
     *
     * @return GameProfile of the entity.
     */
    @Override
    public @Nullable GameProfile getGameProfile() {
        return this.disguiselib$profile;
    }

    /**
     * Updates the entity's GameProfile for other clients
     */
    @Unique
    private void disguiselib$sendProfileUpdates() {
        PlayerListS2CPacket packet = new PlayerListS2CPacket();
        //noinspection ConstantConditions
        PlayerListS2CPacketAccessor accessor = (PlayerListS2CPacketAccessor) packet;
        accessor.setEntries(Collections.singletonList(packet.new Entry(this.disguiselib$profile, 0, GameMode.SURVIVAL, new LiteralText(this.disguiselib$profile.getName()))));

        PlayerManager playerManager = this.world.getServer().getPlayerManager();
        accessor.setAction(REMOVE_PLAYER);
        playerManager.sendToAll(packet);
        accessor.setAction(ADD_PLAYER);
        playerManager.sendToAll(packet);

        ServerChunkManager manager = (ServerChunkManager) this.world.getChunkManager();
        ThreadedAnvilChunkStorage storage = manager.threadedAnvilChunkStorage;
        EntityTrackerEntryAccessor trackerEntry = ((ThreadedAnvilChunkStorageAccessor) storage).getEntityTrackers().get(this.getEntityId());
        if(trackerEntry != null)
            trackerEntry.getTrackingPlayers().forEach(tracking -> trackerEntry.getEntry().startTracking(tracking));
    }

    /**
     * Sends additional move packets to the client if
     * entity is disguised.
     * Prevents client desync and fixes "blocky" movement.
     * @param ci
     */
    @Inject(method = "tick()V", at = @At("TAIL"))
    private void postTick(CallbackInfo ci) {
        // Fixes #2, also makes non-living entities update their pos
        // more than once per second -> movement isn't as "blocky"
        if(this.isDisguised() && this.world.getServer() != null && !(this.disguiselib$disguiseEntity instanceof LivingEntity) && !(this.disguiselib$entity instanceof PlayerEntity))
            this.world.getServer().getPlayerManager().sendToDimension(new EntityPositionS2CPacket(this.disguiselib$entity), this.world.getRegistryKey());
    }

    /**
     * If entity is disguised as player, we need to send a player
     * remove packet on death as well, otherwise tablist still contains
     * it.
     * @param ci
     */
    @Inject(
            method = "remove()V",
            at = @At("TAIL")
    )
    private void onRemove(CallbackInfo ci) {
        if(this.isDisguised() && this.disguiselib$profile != null) {
            // If entity was killed, we should also send a remove player action packet
            PlayerListS2CPacket packet = new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER);
            PlayerListS2CPacketAccessor listS2CPacketAccessor = (PlayerListS2CPacketAccessor) packet;

            GameProfile profile = new GameProfile(this.disguiselib$entity.getUuid(), this.getName().getString());
            listS2CPacketAccessor.setEntries(Collections.singletonList(packet.new Entry(profile, 0, GameMode.SURVIVAL, this.getName())));

            PlayerManager manager = this.world.getServer().getPlayerManager();
            manager.sendToAll(packet);
        }
    }

    /**
     * Takes care of loading the fake entity data from tag.
     * @param tag
     * @param ci
     */
    @Inject(
            method = "fromTag(Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At("TAIL")
    )
    private void fromTag(CompoundTag tag, CallbackInfo ci) {
        CompoundTag disguiseTag = (CompoundTag) tag.get("DisguiseLib");

        if(disguiseTag != null) {
            this.disguiselib$disguised = true;
            Identifier disguiseTypeId = new Identifier(disguiseTag.getString("DisguiseType"));
            this.disguiselib$disguiseType = Registry.ENTITY_TYPE.get(disguiseTypeId);
            this.disguiselib$disguiseAlive = disguiseTag.getBoolean("DisguiseAlive");

            CompoundTag disguiseEntityTag = disguiseTag.getCompound("DisguiseEntity");
            if(!disguiseEntityTag.isEmpty())
                this.disguiselib$disguiseEntity = EntityType.loadEntityWithPassengers(disguiseEntityTag, this.world, (entityx) -> entityx);

            if(this.disguiselib$disguiseType == PLAYER) {
                this.disguiselib$profile = new GameProfile(this.uuid, this.getName().getString());
                //noinspection MixinInnerClass
                this.disguiselib$disguiseEntity = new PlayerEntity(this.world, this.disguiselib$entity.getBlockPos(), this.getHeadYaw(), this.disguiselib$profile) {
                    @Override
                    public boolean isSpectator() {
                        return false;
                    }

                    @Override
                    public boolean isCreative() {
                        return false;
                    }
                };
                // Showing all skin parts
                this.disguiselib$disguiseEntity.getDataTracker().set(PLAYER_MODEL_PARTS, (byte) 0x7f);
            }
        }
    }

    /**
     * Takes care of saving the fake entity data to tag.
     * @param tag
     * @param cir
     */
    @Inject(
            method = "toTag(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;",
            at = @At("TAIL")
    )
    private void toTag(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if(this.disguiselib$disguised) {
            CompoundTag disguiseTag = new CompoundTag();

            disguiseTag.putString("DisguiseType", Registry.ENTITY_TYPE.getId(this.disguiselib$disguiseType).toString());
            disguiseTag.putBoolean("DisguiseAlive", this.disguiselib$disguiseAlive);

            if(this.disguiselib$disguiseEntity != null && !this.disguiselib$entity.equals(this.disguiselib$disguiseEntity)) {
                CompoundTag disguiseEntityTag = new CompoundTag();
                this.disguiselib$disguiseEntity.toTag(disguiseEntityTag);

                Identifier identifier = Registry.ENTITY_TYPE.getId(this.disguiselib$disguiseEntity.getType());
                disguiseEntityTag.putString("id", identifier.toString());

                disguiseTag.put("DisguiseEntity", disguiseEntityTag);
            }

            tag.put("DisguiseLib", disguiseTag);
        }
    }
}
