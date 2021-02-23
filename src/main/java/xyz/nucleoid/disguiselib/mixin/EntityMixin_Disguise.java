package xyz.nucleoid.disguiselib.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.PlayerManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(Entity.class)
public abstract class EntityMixin_Disguise implements EntityDisguise {

    @Unique
    private Entity disguiselib$disguiseEntity;

    @Shadow public abstract EntityType<?> getType();

    @Shadow public World world;

    @Shadow private int entityId;

    @Shadow public abstract float getHeadYaw();

    @Unique
    private boolean disguiselib$disguised, disguiselib$disguiseAlive;
    @Unique
    private EntityType<?> disguiselib$disguiseType;
    @Unique
    private final Entity disguiselib$entity = (Entity) (Object) this;

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
        if(this.disguiselib$disguiseEntity != null && this.disguiselib$disguiseEntity.getType() != entityType) {
            this.disguiselib$disguiseEntity = this.disguiselib$entity;
        }
        this.disguiselib$disguiseAlive = entityType == EntityType.PLAYER || entityType.create(world) instanceof LivingEntity;

        PlayerManager manager = this.world.getServer().getPlayerManager();
        RegistryKey<World> worldRegistryKey = this.world.getRegistryKey();

        manager.sendToDimension(new EntitiesDestroyS2CPacket(entityId), worldRegistryKey);
        manager.sendToDimension(new EntitySpawnS2CPacket(this.disguiselib$entity), worldRegistryKey); // will be replaced by network handler

        manager.sendToDimension(new EntityTrackerUpdateS2CPacket(this.entityId, this.disguiselib$disguiseEntity.getDataTracker(), true), worldRegistryKey);
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
        this.disguiselib$disguiseEntity = entity;
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

            if(this.disguiselib$disguiseEntity != null) {
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
