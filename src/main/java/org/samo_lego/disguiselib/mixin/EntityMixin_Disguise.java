package org.samo_lego.disguiselib.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.samo_lego.disguiselib.EntityDisguise;
import org.samo_lego.disguiselib.mixin.accessor.MobSpawnS2CPacketAccessor;
import org.samo_lego.disguiselib.mixin.accessor.PlayerListS2CPacketAccessor;
import org.samo_lego.disguiselib.mixin.accessor.PlayerSpawnS2CPacketAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;
import java.util.stream.Collectors;

import static net.minecraft.entity.EntityType.loadEntityWithPassengers;
import static org.samo_lego.disguiselib.packets.FakePackets.*;

@Mixin(Entity.class)
public abstract class EntityMixin_Disguise implements EntityDisguise {

    @Shadow public abstract EntityType<?> getType();

    @Shadow public World world;

    @Shadow private int entityId;

    @Shadow public abstract DataTracker getDataTracker();

    @Shadow public abstract double getX();

    @Shadow public abstract double getY();

    @Shadow public abstract double getZ();

    @Shadow public float yaw;

    @Shadow public abstract float getHeadYaw();

    @Shadow protected UUID uuid;
    @Shadow private Vec3d velocity;

    @Shadow public abstract String getEntityName();

    @Shadow public abstract Text getName();

    @Unique
    private boolean disguised, disguiseAlive;
    @Unique
    private EntityType<?> disguiseType;
    @Unique
    private final Entity entity = (Entity) (Object) this;


    @Override
    public boolean isDisguised() {
        return this.disguised;
    }

    @Override
    public void disguiseAs(Identifier entityId) {
        if(entityId.getPath().equals("player")) {
            //Minecraft has built-in protection against creating players :(
            this.disguiseAs(EntityType.PLAYER, true);
        } else {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", entityId.toString());
            Optional<Entity> optionalEntity = Optional.ofNullable(loadEntityWithPassengers(tag, this.world, (entity) -> entity));
            optionalEntity.ifPresent(entity -> this.disguiseAs(entity.getType(), entity instanceof LivingEntity));
        }
    }

    @Override
    public void disguiseAs(EntityType<?> entityType, boolean entityTypeAlive) {
        this.disguised = true;
        this.disguiseType = entityType;
        this.disguiseAlive = entityTypeAlive;

        PlayerManager manager = this.world.getServer().getPlayerManager();
        RegistryKey<World> worldRegistryKey = this.world.getRegistryKey();

        manager.sendToDimension(new EntitiesDestroyS2CPacket(entityId), worldRegistryKey);
        manager.sendToDimension(new EntitySpawnS2CPacket(entity), worldRegistryKey); // will be replaced by network handler

        manager.sendToDimension(new EntityTrackerUpdateS2CPacket(this.entityId, this.getDataTracker(), true), worldRegistryKey);
        manager.sendToDimension(new EntityEquipmentUpdateS2CPacket(this.entityId, this.getEquipment()), worldRegistryKey); // Reload equipment
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

    @Override
    public void removeDisguise() {
        this.disguiseAs(this.getType(), entity instanceof LivingEntity);
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
     * Whether disguise type entity is an instance of {@link LivingEntity}
     * @return
     */
    @Override
    public boolean disguiseAlive() {
        return this.disguiseAlive;
    }
}
