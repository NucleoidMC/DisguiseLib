package xyz.nucleoid.disguiselib.mixin.accessor;

import net.minecraft.network.packet.s2c.play.MobSpawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(MobSpawnS2CPacket.class)
public interface MobSpawnS2CPacketAccessor {
    @Accessor("id")
    void setEntityId(int entityId);
    @Accessor("id")
    int getEntityId();
    @Accessor
    void setUuid(UUID uuid);
    @Accessor("entityTypeId")
    void setEntityType(int entityTypeId);
    @Accessor("entityTypeId")
    int getEntityType();
    @Accessor("x")
    void setX(double x);
    @Accessor("y")
    void setY(double y);
    @Accessor("z")
    void setZ(double z);
    @Accessor("velocityX")
    void setVelocityX(int velocityX);
    @Accessor("velocityY")
    void setVelocityY(int velocityY);
    @Accessor("velocityZ")
    void setVelocityZ(int velocityZ);
    @Accessor("yaw")
    void setYaw(byte yaw);
    @Accessor("pitch")
    void setPitch(byte pitch);
    @Accessor("headYaw")
    void setHeadYaw(byte headYaw);
}
