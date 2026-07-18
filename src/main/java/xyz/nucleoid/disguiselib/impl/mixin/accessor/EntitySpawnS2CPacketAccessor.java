package xyz.nucleoid.disguiselib.impl.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.EntityType;

@Mixin(ClientboundAddEntityPacket.class)
public interface EntitySpawnS2CPacketAccessor {
    @Mutable
    @Accessor("type")
    void setEntityType(EntityType<?> entityType);
    @Mutable
    @Accessor("data")
    void setEntityData(int entityData);

    @Accessor("id")
    int getEntityId();

    @Mutable
    @Accessor("id")
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
    @Accessor("yRot")
    void setYaw(byte yaw);
    @Mutable
    @Accessor("xRot")
    void setPitch(byte pitch);
    @Mutable
    @Accessor("yHeadRot")
    void setHeadYaw(byte headYaw);
}
