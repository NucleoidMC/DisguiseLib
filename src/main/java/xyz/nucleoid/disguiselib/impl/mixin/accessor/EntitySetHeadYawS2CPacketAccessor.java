package xyz.nucleoid.disguiselib.impl.mixin.accessor;

import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntitySetHeadYawS2CPacket.class)
public interface EntitySetHeadYawS2CPacketAccessor {
    @Mutable
    @Accessor("entityId")
    void setEntityId(int id);
}
