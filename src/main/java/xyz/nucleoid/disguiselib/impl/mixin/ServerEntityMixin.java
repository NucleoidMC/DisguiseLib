package xyz.nucleoid.disguiselib.impl.mixin;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.disguiselib.api.DisguiseUtils;
import xyz.nucleoid.disguiselib.api.EntityDisguise;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

@Mixin(ServerEntity.class)
public class ServerEntityMixin {
    @Shadow
    private @Nullable List<SynchedEntityData.DataValue<?>> trackedDataValues;

    @Shadow
    @Final
    private Entity entity;

    @Inject(method = "sendPairingData", at = @At("TAIL"))
    private void sendEmptyPacketsToTriggerStuff(ServerPlayer player, Consumer<Packet<ClientGamePacketListener>> broadcast, CallbackInfo ci) {
        if (((EntityDisguise) this.entity).isDisguised()) {
            if (this.trackedDataValues == null) {
                broadcast.accept(new ClientboundSetEntityDataPacket(this.entity.getId(), List.of()));
            }

            if (this.entity instanceof LivingEntity livingEntity) {
                if (livingEntity.getAttributes().getSyncableAttributes().isEmpty()) {
                    broadcast.accept(new ClientboundUpdateAttributesPacket(this.entity.getId(), List.of()));
                }
            }
        }
    }
}
