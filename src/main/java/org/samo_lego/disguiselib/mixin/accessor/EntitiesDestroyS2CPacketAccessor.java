package org.samo_lego.disguiselib.mixin.accessor;

import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntitiesDestroyS2CPacket.class)
public interface EntitiesDestroyS2CPacketAccessor {
    @Accessor("entityIds")
    int[] getEntityIds();
}
