package xyz.nucleoid.disguiselib.mixin.accessor;

import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntitySpawnS2CPacket.class)
public interface EntitySpawnS2CPacketAccessor {
    @Mutable
    @Accessor("entityTypeId")
    void setEntityType(EntityType<?> entityType);
    @Mutable
    @Accessor("entityData")
    void setEntityData(int entityData);

    @Accessor("id")
    int getEntityId();

    @Mutable
    @Accessor("id")
    void setEntityId(int id);
}
