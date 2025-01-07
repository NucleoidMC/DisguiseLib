package xyz.nucleoid.disguiselib.impl.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;

@Mixin(EntityTrackerUpdateS2CPacket.class)
public interface EntityTrackerUpdateS2CPacketAccessor {
    @Accessor("id")
    int getEntityId();
}
