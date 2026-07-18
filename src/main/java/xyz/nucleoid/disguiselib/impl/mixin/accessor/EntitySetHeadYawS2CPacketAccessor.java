package xyz.nucleoid.disguiselib.impl.mixin.accessor;

import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundRotateHeadPacket.class)
public interface EntitySetHeadYawS2CPacketAccessor {
    @Mutable
    @Accessor("entityId")
    void setEntityId(int id);
}
