package xyz.nucleoid.disguiselib.impl.mixin;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.disguiselib.api.DisguiseUtils;
import xyz.nucleoid.disguiselib.api.EntityDisguise;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.*;
import xyz.nucleoid.disguiselib.impl.packets.ExtendedHandler;
import xyz.nucleoid.disguiselib.impl.packets.FakePackets;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static xyz.nucleoid.disguiselib.impl.DisguiseLib.DISGUISE_TEAM;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonNetworkHandlerMixin_Disguiser {
    @Shadow
    public abstract void send(Packet<?> packet);

    @Unique
    private boolean disguiselib$skipCheck;

    /**
     * Checks the packet that was sent. If the entity in the packet is disguised, the
     * entity type / id in the packet will be changed.
     *
     * As minecraft client doesn't allow moving if you send it an entity with the same
     * id as player, we send the disguised player another entity, so they will see their
     * own disguise.
     *
     * @param packet packet being sent
     */
    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V"
            ),
            cancellable = true
    )
    private void disguiseEntity(Packet<? super ClientGamePacketListener> packet, ChannelFutureListener channelFutureListener, CallbackInfo ci) {
        if (!this.disguiselib$skipCheck) {
            if (!(this instanceof ExtendedHandler self)) {
                return;
            }
            if (packet instanceof ClientboundBundlePacket bundleS2CPacket) {
                if (bundleS2CPacket.subPackets() instanceof ArrayList<Packet<? super ClientGamePacketListener>> list) {
                    var list2 = new ArrayList<Packet<? super ClientGamePacketListener>>();
                    var adder = new ArrayList<Packet<? super ClientGamePacketListener>>();
                    var atomic = new AtomicBoolean(true);
                    for (var packet2 : list) {
                        atomic.set(true);
                        adder.clear();
                        self.disguiselib$transformPacket(packet2, () -> atomic.set(false), list2::add);

                        if (atomic.get()) {
                            list2.add(packet2);
                        }

                        list2.addAll(adder);
                    }

                    list.clear();
                    list.addAll(list2);
                }
            } else {
                this.disguiselib$skipCheck = true;
                self.disguiselib$transformPacket(packet, ci::cancel, this::send);
                this.disguiselib$skipCheck = false;
            }
        }
    }

    @Inject(method = "handleCustomPayload", at = @At("TAIL"))
    private void onClientBrand(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        if (packet.payload() instanceof BrandPayload && this instanceof ExtendedHandler self) {
            self.disguiselib$onClientBrand();
        }
    }
}
