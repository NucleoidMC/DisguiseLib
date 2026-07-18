package xyz.nucleoid.disguiselib.impl.mixin.accessor;

import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundUpdateAttributesPacket.class)
public interface EntityAttributesS2CPacketAccessor {
    @Accessor("entityId")
    int getEntityId();
}
