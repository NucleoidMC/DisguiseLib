package xyz.nucleoid.disguiselib.impl.mixin.accessor;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundRemoveEntitiesPacket.class)
public interface EntitiesDestroyS2CPacketAccessor {
    @Accessor("entityIds")
    IntList getEntityIds();
}
