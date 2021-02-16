package org.samo_lego.disguiselib.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.PlayerManager;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.samo_lego.disguiselib.EntityDisguise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(Entity.class)
public abstract class EntityMixin_Disguise implements EntityDisguise {

    @Shadow public abstract EntityType<?> getType();

    @Shadow public World world;

    @Shadow private int entityId;

    @Shadow public abstract DataTracker getDataTracker();

    @Shadow public abstract float getHeadYaw();

    @Unique
    private boolean disguised, disguiseAlive;
    @Unique
    private EntityType<?> disguiseType;
    @Unique
    private final Entity entity = (Entity) (Object) this;

    /**
     * Tells you the disguised status.
     * @return true if entity is disguised, otherwise false.
     */
    @Override
    public boolean isDisguised() {
        return this.disguised;
    }

    /**
     * Sets entity's disguise from {@link EntityType}
     * @param entityType the type to disguise this entity into
     */
    @Override
    public void disguiseAs(EntityType<?> entityType) {
        this.disguised = true;
        this.disguiseType = entityType;
        this.disguiseAlive = entityType == EntityType.PLAYER || entityType.create(world) instanceof LivingEntity;

        PlayerManager manager = this.world.getServer().getPlayerManager();
        RegistryKey<World> worldRegistryKey = this.world.getRegistryKey();

        manager.sendToDimension(new EntitiesDestroyS2CPacket(entityId), worldRegistryKey);
        manager.sendToDimension(new EntitySpawnS2CPacket(this.entity), worldRegistryKey); // will be replaced by network handler

        manager.sendToDimension(new EntityTrackerUpdateS2CPacket(this.entityId, this.getDataTracker(), true), worldRegistryKey);
        manager.sendToDimension(new EntityEquipmentUpdateS2CPacket(this.entityId, this.getEquipment()), worldRegistryKey); // Reload equipment
        manager.sendToDimension(new EntitySetHeadYawS2CPacket(this.entity, (byte)((int)(this.getHeadYaw() * 256.0F / 360.0F))), worldRegistryKey); // Head correction
    }

    /**
     * Gets equipment as list of {@link Pair Pairs}.
     * Requires entity to be an instanceof {@link LivingEntity}.
     *
     * @return equipment list of pairs.
     */
    @Unique
    private List<Pair<EquipmentSlot, ItemStack>> getEquipment() {
        if(entity instanceof LivingEntity)
            return Arrays.stream(EquipmentSlot.values()).map(slot -> new Pair<>(slot, ((LivingEntity) entity).getEquippedStack(slot))).collect(Collectors.toList());
        return Collections.emptyList();
    }

    /**
     * Clears the disguise - sets the {@link EntityMixin_Disguise#disguiseType} back to original.
     */
    @Override
    public void removeDisguise() {
        this.disguiseAs(this.getType());
    }

    /**
     * Gets the disguise entity type
     * @return disguise entity type or real type if there's no disguise
     */
    @Override
    public EntityType<?> getDisguiseType() {
        return this.disguiseType;
    }

    /**
     * Whether disguise type entity is an instance of {@link LivingEntity}.
     * @return true whether the disguise type is an instance of {@link LivingEntity}, otherwise false.
     */
    @Override
    public boolean disguiseAlive() {
        return this.disguiseAlive;
    }
}
