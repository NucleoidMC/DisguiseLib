package xyz.nucleoid.disguiselib.impl.mixin.accessor;

import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(EntitySpawnS2CPacket.class)
public interface EntitySpawnS2CPacketAccessor {
    @Mutable
    @Accessor("entityType")
    void setEntityType(EntityType<?> entityType);
    @Mutable
    @Accessor("entityData")
    void setEntityData(int entityData);

    @Accessor("entityId")
    int getEntityId();

    @Mutable
    @Accessor("entityId")
    void setEntityId(int id);

    @Mutable
    @Accessor("uuid")
    void setUuid(UUID uuid);

    @Mutable
    @Accessor("x")
    void setX(double x);
    @Mutable
    @Accessor("y")
    void setY(double y);
    @Mutable
    @Accessor("z")
    void setZ(double z);
    @Mutable
    @Accessor("yaw")
    void setYaw(byte yaw);
    @Mutable
    @Accessor("pitch")
    void setPitch(byte pitch);
    @Mutable
    @Accessor("headYaw")
    void setHeadYaw(byte headYaw);
}
