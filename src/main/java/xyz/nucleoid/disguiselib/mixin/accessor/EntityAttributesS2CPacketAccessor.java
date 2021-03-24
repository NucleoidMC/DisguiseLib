package xyz.nucleoid.disguiselib.mixin.accessor;

import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityAttributesS2CPacket.class)
public interface EntityAttributesS2CPacketAccessor {
    @Accessor("entityId")
    int getEntityId();
    @Accessor("entityId")
    void setEntityId(int id);
}
