package xyz.nucleoid.disguiselib.mixin.accessor;

import net.minecraft.network.packet.s2c.play.MobSpawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(MobSpawnS2CPacket.class)
public interface MobSpawnS2CPacketAccessor {
    @Mutable
    @Accessor("id")
    void setEntityId(int entityId);
    @Accessor("id")
    int getEntityId();
    @Mutable
    @Accessor
    void setUuid(UUID uuid);
    @Mutable
    @Accessor("entityTypeId")
    void setEntityType(int entityTypeId);
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
    @Accessor("velocityX")
    void setVelocityX(int velocityX);
    @Mutable
    @Accessor("velocityY")
    void setVelocityY(int velocityY);
    @Mutable
    @Accessor("velocityZ")
    void setVelocityZ(int velocityZ);
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
