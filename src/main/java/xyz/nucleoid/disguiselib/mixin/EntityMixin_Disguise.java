package xyz.nucleoid.disguiselib.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
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
import xyz.nucleoid.disguiselib.casts.DisguiseMethods;
import xyz.nucleoid.disguiselib.casts.EntityDisguise;
import xyz.nucleoid.disguiselib.mixin.accessor.EntityTrackerEntryAccessor;
import xyz.nucleoid.disguiselib.mixin.accessor.PlayerListS2CPacketAccessor;
import xyz.nucleoid.disguiselib.mixin.accessor.ThreadedAnvilChunkStorageAccessor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.minecraft.entity.EntityType.PLAYER;
import static net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action.ADD_PLAYER;
import static net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action.REMOVE_PLAYER;
import static xyz.nucleoid.disguiselib.mixin.accessor.PlayerEntityAccessor.getPLAYER_MODEL_PARTS;

@Mixin(Entity.class)
public abstract class EntityMixin_Disguise implements EntityDisguise, DisguiseMethods {

    @Unique
    private final Entity disguiselib$entity = (Entity) (Object) this;
    @Shadow
    public World world;
    @Shadow
    protected UUID uuid;
    @Unique
    private Entity disguiselib$disguiseEntity;
    @Unique
    private int disguiselib$ticks;
    @Shadow
    private int entityId;
    @Unique
    private boolean disguiselib$disguised, disguiselib$disguiseAlive;
    @Unique
    private EntityType<?> disguiselib$disguiseType;
    @Unique
    private GameProfile disguiselib$profile;

    @Shadow
    public abstract EntityType<?> getType();

    @Shadow
    public abstract float getHeadYaw();

    @Shadow
    public abstract Text getName();

    @Shadow
    public abstract int getEntityId();

    @Shadow
    public abstract DataTracker getDataTracker();

    @Shadow
    @Nullable
    public abstract Text getCustomName();

    @Shadow
    @Nullable
    public abstract MinecraftServer getServer();

    @Shadow
    public abstract boolean isCustomNameVisible();

    /**
     * Tells you the disguised status.
     *
     * @return true if entity is disguised, otherwise false.
     */
    @Override
    public boolean isDisguised() {
        return this.disguiselib$disguised;
    }

    /**
     * Sets entity's disguise from {@link EntityType}
     *
     * @param entityType the type to disguise this entity into
     */
    @Override
    public void disguiseAs(EntityType<?> entityType) {
        this.disguiselib$disguised = true;
        this.disguiselib$disguiseType = entityType;

        PlayerManager manager = this.world.getServer().getPlayerManager();

        if(this.disguiselib$disguiseEntity != null && this.disguiselib$disguiseEntity.getType() != entityType && this.disguiselib$entity instanceof ServerPlayerEntity) {
            this.disguiselib$hideSelfView();
        }

        if(entityType == PLAYER) {
            if(this.disguiselib$profile == null)
                this.disguiselib$profile = new GameProfile(this.uuid, this.getName().getString());
            this.disguiselib$constructFakePlayer();
        } else {
            // Why null check? Well, if entity was disguised via EntityDisguise#disguiseAs(Entity), this field is already set
            if(this.disguiselib$disguiseEntity == null || this.disguiselib$disguiseEntity.getType() != entityType)
                this.disguiselib$disguiseEntity = entityType.create(world);

            if(this.disguiselib$profile != null) {
                // Previous type was player, we have to send a player remove packet
                PlayerListS2CPacket listPacket = new PlayerListS2CPacket(REMOVE_PLAYER);
                PlayerListS2CPacketAccessor listPacketAccessor = (PlayerListS2CPacketAccessor) listPacket;
                listPacketAccessor.setEntries(Arrays.asList(listPacket.new Entry(this.disguiselib$profile, 0, GameMode.SURVIVAL, new LiteralText(this.disguiselib$profile.getName()))));
                manager.sendToAll(listPacket);
            }
            this.disguiselib$profile = null;
        }

        this.disguiselib$disguiseAlive = this.disguiselib$disguiseEntity instanceof LivingEntity;

        RegistryKey<World> worldRegistryKey = this.world.getRegistryKey();

        // Minor datatracker thingies
        this.updateTrackedData();

        // Updating entity on the client
        manager.sendToDimension(new EntitiesDestroyS2CPacket(this.entityId), worldRegistryKey);
        manager.sendToDimension(new EntitySpawnS2CPacket(this.disguiselib$entity), worldRegistryKey); // will be replaced by network handler

        manager.sendToDimension(new EntityTrackerUpdateS2CPacket(this.entityId, this.getDataTracker(), true), worldRegistryKey);
        manager.sendToDimension(new EntityEquipmentUpdateS2CPacket(this.entityId, this.disguiselib$getEquipment()), worldRegistryKey); // Reload equipment
        manager.sendToDimension(new EntitySetHeadYawS2CPacket(this.disguiselib$entity, (byte) ((int) (this.getHeadYaw() * 256.0F / 360.0F))), worldRegistryKey); // Head correction
    }

    /**
     * Sets entity's disguise from {@link Entity}
     *
     * @param entity the entity to disguise into
     */
    @Override
    public void disguiseAs(Entity entity) {
        if(this.disguiselib$disguiseEntity != null && this.disguiselib$entity instanceof ServerPlayerEntity) {
            // Removing previous disguise if this is player
            // (we have it saved under a separate id)
            this.disguiselib$hideSelfView();
        }

        this.disguiselib$disguiseEntity = entity;
        if(entity instanceof PlayerEntity) {
            this.disguiselib$profile = ((PlayerEntity) entity).getGameProfile();
        }
        this.disguiseAs(entity.getType());
    }

    /**
     * Clears the disguise - sets the {@link EntityMixin_Disguise#disguiselib$disguiseType} back to original.
     */
    @Override
    public void removeDisguise() {
        if(this.disguiselib$disguiseEntity != null && this.disguiselib$entity instanceof ServerPlayerEntity) {
            // Removing previous disguise if this is player
            // (we have it saved under a separate id)
            this.disguiselib$hideSelfView();
        }
        // Disguising entity as itself
        this.disguiselib$disguiseEntity = null;
        this.disguiseAs(this.getType());

        // Setting as not-disguised
        this.disguiselib$disguised = false;
        this.disguiselib$disguiseEntity = null;
    }

    /**
     * Gets the disguise entity type
     *
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
     *
     * @return true whether the disguise type is an instance of {@link LivingEntity}, otherwise false.
     */
    @Override
    public boolean disguiseAlive() {
        return this.disguiselib$disguiseAlive;
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
     * Hides player's self-disguise-entity
     */
    @Unique
    private void disguiselib$hideSelfView() {
        // Removing previous disguise if this is player
        // (we have it saved under a separate id)
        ((ServerPlayerEntity) this.disguiselib$entity).networkHandler.sendPacket(new EntitiesDestroyS2CPacket(this.disguiselib$disguiseEntity.getEntityId()));
    }

    /**
     * Constructs fake player entity for use
     * when entities are disguised as players.
     */
    @Unique
    private void disguiselib$constructFakePlayer() {
        this.disguiselib$disguiseEntity = new ServerPlayerEntity(this.getServer(), (ServerWorld) this.world, this.disguiselib$profile, new ServerPlayerInteractionManager((ServerWorld) this.world));
        // Showing all skin parts
        this.disguiselib$disguiseEntity.getDataTracker().set(getPLAYER_MODEL_PARTS(), (byte) 0x7f);
    }

    /**
     * Gets equipment as list of {@link Pair Pairs}.
     * Requires entity to be an instanceof {@link LivingEntity}.
     *
     * @return equipment list of pairs.
     */
    @Unique
    private List<Pair<EquipmentSlot, ItemStack>> disguiselib$getEquipment() {
        if(disguiselib$entity instanceof LivingEntity)
            return Arrays.stream(EquipmentSlot.values()).map(slot -> new Pair<>(slot, ((LivingEntity) disguiselib$entity).getEquippedStack(slot))).collect(Collectors.toList());
        return Collections.emptyList();
    }

    /**
     * Updates the entity's GameProfile for other clients
     */
    @Unique
    private void disguiselib$sendProfileUpdates() {
        PlayerListS2CPacket packet = new PlayerListS2CPacket();
        //noinspection ConstantConditions
        PlayerListS2CPacketAccessor accessor = (PlayerListS2CPacketAccessor) packet;
        accessor.setEntries(Arrays.asList(packet.new Entry(this.disguiselib$profile, 0, GameMode.SURVIVAL, new LiteralText(this.disguiselib$profile.getName()))));

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
     * Updates custom name and its visibility.
     * Also sets no-gravity to true in order
     * to prevent the client from predicting
     * the entity position and velocity.
     */
    @Override
    public void updateTrackedData() {
        // Minor datatracker thingies
        this.disguiselib$disguiseEntity.setNoGravity(true);
        this.disguiselib$disguiseEntity.setCustomName(this.getCustomName());
        this.disguiselib$disguiseEntity.setCustomNameVisible(this.isCustomNameVisible());
    }

    /**
     * Sends additional move packets to the client if
     * entity is disguised.
     * Prevents client desync and fixes "blocky" movement.
     */
    @Inject(method = "tick()V", at = @At("TAIL"))
    private void postTick(CallbackInfo ci) {
        // Fixes #2, also makes non-living entities update their pos
        // more than once per second -> movement isn't as "blocky"
        if(this.isDisguised()) {
            if(this.world.getServer() != null && !(this.disguiselib$disguiseEntity instanceof LivingEntity) && !(this.disguiselib$entity instanceof PlayerEntity))
                this.world.getServer().getPlayerManager().sendToDimension(new EntityPositionS2CPacket(this.disguiselib$entity), this.world.getRegistryKey());
            else if(this.disguiselib$entity instanceof ServerPlayerEntity && ++this.disguiselib$ticks % 40 == 0) {
                // "Disguised as" message
                MutableText msg = new LiteralText("You are disguised as ")
                        .append(new TranslatableText(this.disguiselib$disguiseEntity.getType().getTranslationKey()))
                        .formatted(Formatting.GREEN);

                ((ServerPlayerEntity) this.disguiselib$entity).sendMessage(msg, true);
                this.disguiselib$ticks = 0;
            }
        }

    }

    /**
     * If entity is disguised as player, we need to send a player
     * remove packet on death as well, otherwise tablist still contains
     * it.
     */
    @Inject(
            method = "remove()V",
            at = @At("TAIL")
    )
    private void onRemove(CallbackInfo ci) {
        if(this.isDisguised() && this.disguiselib$profile != null) {
            // If entity was killed, we should also send a remove player action packet
            PlayerListS2CPacket packet = new PlayerListS2CPacket(REMOVE_PLAYER);
            PlayerListS2CPacketAccessor listS2CPacketAccessor = (PlayerListS2CPacketAccessor) packet;

            GameProfile profile = new GameProfile(this.disguiselib$entity.getUuid(), this.getName().getString());
            listS2CPacketAccessor.setEntries(Arrays.asList(packet.new Entry(profile, 0, GameMode.SURVIVAL, this.getName())));

            PlayerManager manager = this.world.getServer().getPlayerManager();
            manager.sendToAll(packet);
        }
    }

    /**
     * Takes care of loading the fake entity data from tag.
     *
     * @param tag tag to load data from.
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

            if(this.disguiselib$disguiseType == PLAYER) {
                this.disguiselib$profile = new GameProfile(this.uuid, this.getName().getString());
                this.disguiselib$constructFakePlayer();
            } else {
                CompoundTag disguiseEntityTag = disguiseTag.getCompound("DisguiseEntity");
                if(!disguiseEntityTag.isEmpty())
                    this.disguiselib$disguiseEntity = EntityType.loadEntityWithPassengers(disguiseEntityTag, this.world, (entityx) -> entityx);
            }
        }
    }

    /**
     * Takes care of saving the fake entity data to tag.
     *
     * @param tag tag to save data to.
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
