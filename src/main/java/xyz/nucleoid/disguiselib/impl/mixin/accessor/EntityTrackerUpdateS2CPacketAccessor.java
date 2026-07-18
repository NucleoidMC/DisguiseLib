package xyz.nucleoid.disguiselib.impl.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;

@Mixin(ClientboundSetEntityDataPacket.class)
public interface EntityTrackerUpdateS2CPacketAccessor {
    @Accessor("id")
    int getEntityId();

    @Mutable
    @Accessor("packedItems")
    void setTrackedValues(List<SynchedEntityData.DataValue<?>> trackedValues);
}
