package xyz.nucleoid.disguiselib.impl.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;

@Mixin(ClientboundPlayerInfoUpdatePacket.class)
public interface PlayerListS2CPacketAccessor {
    @Mutable
    @Accessor("entries")
    void setEntries(List<ClientboundPlayerInfoUpdatePacket.Entry> entries);
    @Accessor("entries")
    List<ClientboundPlayerInfoUpdatePacket.Entry> getEntries();
}
