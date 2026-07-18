package xyz.nucleoid.disguiselib.impl.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.network.ServerPlayerConnection;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public interface EntityTrackerEntryAccessor {
    @Accessor("serverEntity")
    ServerEntity getEntry();
    @Accessor("seenBy")
    Set<ServerPlayerConnection> getListeners();
}
