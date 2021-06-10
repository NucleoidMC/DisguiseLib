package xyz.nucleoid.disguiselib.mixin.accessor;

import net.minecraft.network.packet.s2c.play.EntityDestroyS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityDestroyS2CPacket.class)
public interface EntityDestroyS2CPacketAccessor {
    @Accessor("entityId")
    int getEntityIds();
}
