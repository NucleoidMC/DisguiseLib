package xyz.nucleoid.disguiselib.impl.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.disguiselib.api.DisguiseUtils;
import xyz.nucleoid.disguiselib.api.EntityDisguise;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.EntityTrackerEntryAccessor;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.MannequinAccessor;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.ServerChunkLoadingManagerAccessor;

import java.util.*;
import java.util.stream.Collectors;

import static net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER;
import static net.minecraft.world.entity.EntityTypes.PLAYER;
import static xyz.nucleoid.disguiselib.impl.mixin.accessor.PlayerEntityAccessor.getPLAYER_MODEL_PARTS;

@Mixin(Entity.class)
public abstract class EntityMixin_Disguise implements EntityDisguise, DisguiseUtils {

    @Unique
    private final Entity disguiselib$entity = (Entity) (Object) this;
    @Shadow
    public Level level;
    @Shadow
    protected UUID uuid;
    @Unique
    private Entity disguiselib$disguiseEntity;
    @Unique
    private int disguiselib$ticks;
    @Unique
    private EntityType<?> disguiselib$disguiseType;
    @Unique
    private GameProfile disguiselib$profile;
    @Unique
    private boolean disguiselib$trueSight = false;

    @Shadow
    public abstract EntityType<?> getType();

    @Shadow
    public abstract float getYHeadRot();

    @Shadow
    public abstract Component getName();

    @Shadow
    public abstract SynchedEntityData getEntityData();

    @Shadow
    @Nullable
    public abstract Component getCustomName();

    @Shadow
    public abstract boolean isCustomNameVisible();

    @Shadow
    public abstract boolean isSprinting();

    @Shadow
    public abstract boolean isShiftKeyDown();

    @Shadow
    public abstract boolean isSwimming();

    @Shadow
    public abstract boolean isCurrentlyGlowing();

    @Shadow
    public abstract boolean isSilent();

    @Shadow
    private int id;

    @Shadow
    public abstract Pose getPose();

    @Shadow
    public abstract int getId();

    @Shadow
    public abstract boolean isOnFire();

    @Shadow
    public abstract Component getDisplayName();

    @Shadow
    private boolean onGround;

    /**
     * Tells you the disguised status.
     *
     * @return true if entity is disguised, otherwise false.
     */
    @Override
    public boolean isDisguised() {
        return this.disguiselib$disguiseEntity != null;
    }

    /**
     * Sets entity's disguise from {@link EntityType}
     *
     * @param entityType the type to disguise this entity into
     */
    @Override
    public void disguiseAs(EntityType<?> entityType) {
        this.disguiselib$disguiseType = entityType;


        if (entityType == PLAYER) {
            if (this.disguiselib$profile == null)
                this.disguiseLib$setGameProfile(new GameProfile(this.uuid, this.getDisplayName().getString()));
            this.disguiselib$constructFakePlayer(this.disguiselib$profile);
        } else {
            // Why null check? Well, if entity was disguised via EntityDisguise#disguiseAs(Entity), this field is already set
            if (this.disguiselib$disguiseEntity == null || this.disguiselib$disguiseEntity.getType() != entityType)
                this.disguiselib$disguiseEntity = entityType.create(level, EntitySpawnReason.LOAD);

            // We don't need gameprofile anymore
            this.disguiselib$profile = null;
        }


        // Fix some client predictions
        if (this.disguiselib$disguiseEntity instanceof Mob)
            ((Mob) this.disguiselib$disguiseEntity).setNoAi(true);

        //noinspection ReferenceToMixin
        var tracker = ((ServerChunkLoadingManagerAccessor) ((ServerLevel) this.level).getChunkSource().chunkMap).getEntityTrackers().get(this.getId());

        for (var listener : tracker.getListeners()) {
            tracker.getEntry().removePairing(listener.getPlayer());
            tracker.getEntry().addPairing(listener.getPlayer());
        }
    }

    /**
     * Sets entity's disguise from {@link Entity}
     *
     * @param entity the entity to disguise into
     */
    @Override
    public void disguiseAs(Entity entity) {
        this.disguiselib$disguiseEntity = entity;
        if (entity instanceof Player) {
            this.disguiseLib$setGameProfile(((Player) entity).getGameProfile());
        }
        this.disguiseAs(entity.getType());
    }

    /**
     * Clears the disguise - sets the {@link EntityMixin_Disguise#disguiselib$disguiseType} back to original.
     */
    @Override
    public void disguiseLib$removeDisguise() {
        this.disguiselib$disguiseEntity = null;
        this.disguiselib$disguiseType = null;

        var tracker = ((ServerChunkLoadingManagerAccessor) ((ServerLevel) this.level).getChunkSource().chunkMap).getEntityTrackers().get(this.getId());

        for (var listener : tracker.getListeners()) {
            tracker.getEntry().removePairing(listener.getPlayer());
            tracker.getEntry().addPairing(listener.getPlayer());
        }
    }

    /**
     * Gets the disguise entity type
     *
     * @return disguise entity type or real type if there's no disguise
     */
    @Override
    public EntityType<?> disguiseLib$getDisguiseType() {
        return this.disguiselib$disguiseType;
    }

    /**
     * Gets the disguise entity.
     *
     * @return disguise entity or null if there's no disguise
     */
    @Nullable
    @Override
    public Entity disguiseLib$getDisguiseEntity() {
        return this.disguiselib$disguiseEntity;
    }

    /**
     * Whether disguise type entity is an instance of {@link LivingEntity}.
     *
     * @return true if the disguise type is an instance of {@link LivingEntity}, otherwise false.
     */
    @Override
    public boolean disguiseLib$disguiseAlive() {
        return this.disguiselib$disguiseEntity instanceof LivingEntity;
    }

    /**
     * Whether this entity can bypass the
     * "disguises" and see entities normally
     * Intended more for admins (to not get trolled themselves).
     *
     * @return if entity can be "fooled" by disguise
     */
    @Override
    public boolean disguiseLib$hasTrueSight() {
        return this.disguiselib$trueSight;
    }

    /**
     * Toggles true sight - whether entity
     * can see disguises or not.
     * Intended more for admins (to not get trolled themselves).
     *
     * @param trueSight if entity should not see disguises
     */
    @Override
    public void disguiseLib$setTrueSight(boolean trueSight) {
        this.disguiselib$trueSight = trueSight;
    }

    /**
     * Gets the {@link GameProfile} for disguised entity,
     * used when disguising as player.
     *
     * @return GameProfile of the entity.
     */
    @Override
    public @Nullable GameProfile disguiseLib$getGameProfile() {
        return this.disguiselib$profile;
    }

    /**
     * Sets the GameProfile
     *
     * @param gameProfile a new profile for the entity.
     */
    @Override
    public void disguiseLib$setGameProfile(@Nullable GameProfile gameProfile) {
        this.disguiselib$profile = gameProfile;
        if (gameProfile != null) {
            String name = gameProfile.name();
            if (name.length() > 16) {
                // Minecraft kicks players on such profile name received
                name = name.substring(0, 16);
                PropertyMap properties = gameProfile.properties();
                this.disguiselib$profile = new GameProfile(gameProfile.id(), name);
                Collection<Property> textures = properties.get("textures");
                if (!textures.isEmpty())
                    this.disguiselib$profile.properties().put("textures", textures.stream().findFirst().get());
            }
        }

        this.disguiselib$sendProfileUpdates();
    }

    /**
     * Constructs fake player entity for use
     * when entities are disguised as players.
     *
     * @param profile gameprofile to use for new player.
     */
    @Unique
    private void disguiselib$constructFakePlayer(@NotNull GameProfile profile) {
        var mannequin = EntityTypes.MANNEQUIN.create(this.level, EntitySpawnReason.SPAWN_ITEM_USE);
        ((MannequinAccessor) mannequin).callSetProfile(ResolvableProfile.createResolved(profile));
        this.disguiselib$disguiseEntity = mannequin;
        this.disguiselib$disguiseEntity.getEntityData().set(getPLAYER_MODEL_PARTS(), (byte) 0x7f);
    }

    /**
     * Gets equipment as list of {@link Pair Pairs}.
     * Requires entity to be an instanceof {@link LivingEntity}.
     *
     * @return equipment list of pairs.
     */
    @Unique
    private List<Pair<EquipmentSlot, ItemStack>> disguiselib$getEquipment() {
        if (disguiselib$entity instanceof LivingEntity)
            return Arrays.stream(EquipmentSlot.values()).map(slot -> new Pair<>(slot, ((LivingEntity) disguiselib$entity).getItemBySlot(slot))).collect(Collectors.toList());
        return Collections.emptyList();
    }

    /**
     * Updates the entity's GameProfile for other clients
     */
    @Unique
    private void disguiselib$sendProfileUpdates() {
        PlayerList playerManager = this.level.getServer().getPlayerList();
        ServerChunkCache manager = (ServerChunkCache) this.level.getChunkSource();
        var storage = manager.chunkMap;
        EntityTrackerEntryAccessor trackerEntry = ((ServerChunkLoadingManagerAccessor) storage).getEntityTrackers().get(this.getId());
        if (trackerEntry != null)
            trackerEntry.getListeners().forEach(tracking -> trackerEntry.getEntry().addPairing(tracking.getPlayer()));

        // Changing entity on client
        if (((Object) this) instanceof ServerPlayer player) {
            ServerLevel targetWorld = player.level();

            player.connection.send(new ClientboundRespawnPacket(player.createCommonSpawnInfo(targetWorld), ClientboundRespawnPacket.KEEP_ALL_DATA));
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());

            player.level().getServer().getPlayerList().sendPlayerPermissionLevel(player);

            player.connection.send(new ClientboundSetExperiencePacket(player.experienceProgress, player.totalExperience, player.experienceLevel));
            player.connection.send(new ClientboundSetHealthPacket(player.getHealth(), player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel()));

            for (MobEffectInstance statusEffect : player.getActiveEffects()) {
                player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), statusEffect, false));
            }

            player.onUpdateAbilities();
            playerManager.sendLevelInfo(player, targetWorld);
            playerManager.sendAllPlayerInfo(player);
        }
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
        this.disguiselib$disguiseEntity.setSprinting(this.isSprinting());
        this.disguiselib$disguiseEntity.setShiftKeyDown(this.isShiftKeyDown());
        this.disguiselib$disguiseEntity.setSwimming(this.isSwimming());
        this.disguiselib$disguiseEntity.setGlowingTag(this.isCurrentlyGlowing());
        this.disguiselib$disguiseEntity.setSharedFlagOnFire(this.isOnFire());
        this.disguiselib$disguiseEntity.setSilent(this.isSilent());
        this.disguiselib$disguiseEntity.setPose(this.getPose());
        //noinspection ConstantValue
        if (this.disguiselib$disguiseEntity instanceof LivingEntity disguise && ((Object) this) instanceof LivingEntity self) {
            disguise.getAttributes().assignAllValues(self.getAttributes());
        }
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
        if (this.isDisguised()) {
            if (this.level.getServer() != null && !(this.disguiselib$disguiseEntity instanceof LivingEntity) && !(this.disguiselib$entity instanceof Player))
                this.level.getServer().getPlayerList().broadcastAll(
                        new ClientboundTeleportEntityPacket(
                                this.disguiselib$entity.getId(),
                                new PositionMoveRotation(
                                        this.disguiselib$entity.trackingPosition(),
                                        this.disguiselib$entity.getDeltaMovement(),
                                        this.disguiselib$entity.getYRot(),
                                        this.disguiselib$entity.getXRot()
                                ), Set.of(), this.onGround), this.level.dimension());
            else if (this.disguiselib$entity instanceof ServerPlayer && ++this.disguiselib$ticks % 40 == 0 && this.disguiselib$disguiseEntity != null) {
                // "Disguised as" message
                MutableComponent msg = Component.literal("You are disguised as ")
                        .append(Component.translatable(this.disguiselib$disguiseEntity.getType().getDescriptionId()))
                        .withStyle(ChatFormatting.GREEN);

                ((ServerPlayer) this.disguiselib$entity).sendSystemMessage(msg, true);
                this.disguiselib$ticks = 0;
            }
        }

    }

    /**
     * Takes care of loading the fake entity data from tag.
     *
     * @param tag tag to load data from.
     */
    @Inject(
            method = "load",
            at = @At("TAIL")
    )
    private void fromTag(ValueInput tag, CallbackInfo ci) {
        var disguiseTag = tag.child("DisguiseLib");

        if (disguiseTag.isPresent()) {
            Identifier disguiseTypeId = Identifier.tryParse(disguiseTag.get().getStringOr("DisguiseType", ""));
            this.disguiselib$disguiseType = BuiltInRegistries.ENTITY_TYPE.getValue(disguiseTypeId);

            if (this.disguiselib$disguiseType == PLAYER) {
                this.disguiseLib$setGameProfile(new GameProfile(this.uuid, this.getName().getString()));
                this.disguiselib$constructFakePlayer(this.disguiselib$profile);
            } else {
                var disguiseEntityTag = disguiseTag.get().child("DisguiseEntity");
                if (disguiseEntityTag.isPresent())
                    this.disguiselib$disguiseEntity = EntityType.loadEntityRecursive(disguiseEntityTag.get(), this.level, EntitySpawnReason.LOAD, (entityx) -> entityx);
            }
        }
    }

    /**
     * Takes care of saving the fake entity data to tag.
     *
     * @param tag tag to save data to.
     */
    @Inject(
            method = "saveWithoutId",
            at = @At("TAIL")
    )
    private void toTag(ValueOutput tag, CallbackInfo ci) {
        if (this.isDisguised()) {
            var disguiseTag = tag.child("DisguiseLib");

            disguiseTag.putString("DisguiseType", BuiltInRegistries.ENTITY_TYPE.getKey(this.disguiselib$disguiseType).toString());

            if (this.disguiselib$disguiseEntity != null && !this.disguiselib$entity.equals(this.disguiselib$disguiseEntity)) {
                var disguiseEntityTag = disguiseTag.child("DisguiseEntity");
                this.disguiselib$disguiseEntity.saveWithoutId(disguiseEntityTag);

                Identifier identifier = BuiltInRegistries.ENTITY_TYPE.getKey(this.disguiselib$disguiseEntity.getType());
                disguiseEntityTag.putString("id", identifier.toString());
            }
        }
    }
}
