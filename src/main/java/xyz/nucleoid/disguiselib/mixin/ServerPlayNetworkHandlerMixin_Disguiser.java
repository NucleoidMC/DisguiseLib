package xyz.nucleoid.disguiselib.mixin;

import com.mojang.authlib.GameProfile;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.disguiselib.EntityDisguise;
import xyz.nucleoid.disguiselib.mixin.accessor.*;
import xyz.nucleoid.disguiselib.packets.FakePackets;

import java.util.Collections;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin_Disguiser {
    @Shadow public ServerPlayerEntity player;
    @Unique private boolean disguiselib$skipCheck;
    @Shadow public abstract void sendPacket(Packet<?> packet);

    /**
     * Checks the packet that was sent. If the entity in the packet is disguised, the
     * entity type in the packet will be changed.
     *
     * @param packet packet being sent
     * @param listener
     * @param ci
     */
    @Inject(
            method = "sendPacket(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/ClientConnection;send(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V"
            ),
            cancellable = true
    )
    private void disguiseEntity(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>> listener, CallbackInfo ci) {
        if(!this.disguiselib$skipCheck) {
            World world = this.player.getEntityWorld();
            Entity entity = null;

            if(packet instanceof PlayerSpawnS2CPacket) {
                entity = world.getEntityById(((PlayerSpawnS2CPacketAccessor) packet).getId());
            } else if(packet instanceof MobSpawnS2CPacket) {
                entity = world.getEntityById(((MobSpawnS2CPacketAccessor) packet).getEntityId());
            } else if(packet instanceof EntitySpawnS2CPacket) {
                entity = world.getEntityById(((EntitySpawnS2CPacketAccessor) packet).getEntityId());
            } else if(packet instanceof EntitiesDestroyS2CPacket && ((EntitiesDestroyS2CPacketAccessor) packet).getEntityIds()[0] == this.player.getEntityId()) {
                ci.cancel();
            }
            EntityDisguise disguise = (EntityDisguise) entity;
            if(
                    disguise != null &&
                    disguise.isDisguised() &&
                    entity.getEntityId() != this.player.getEntityId() // do not send the packet to the player themselves
            ) {
                disguiselib$sendFakePacket(entity);
                ci.cancel();
            }
        }
    }

    /**
     * Sends fake packet instead of the real one
     * @param entity the entity that is disguised and needs to have a custom packet sent.
     */
    @Unique
    private void disguiselib$sendFakePacket(Entity entity) {
        EntityDisguise disguise = (EntityDisguise) entity;
        GameProfile profile = disguise.getGameProfile();

        this.sendPacket(new EntitiesDestroyS2CPacket(entity.getEntityId()));

        this.disguiselib$skipCheck = true;
        if(disguise.getDisguiseType() == EntityType.PLAYER) {
            PlayerListS2CPacket packet = new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER);
            PlayerListS2CPacketAccessor listS2CPacketAccessor = (PlayerListS2CPacketAccessor) packet;
            listS2CPacketAccessor.setEntries(Collections.singletonList(packet.new Entry(profile, 0, GameMode.SURVIVAL, entity.getName())));

            this.sendPacket(packet);
            this.sendPacket(FakePackets.fakePlayerSpawnS2CPacket(entity));

        } else {
            if(disguise.disguiseAlive()) {
                this.sendPacket(FakePackets.fakeMobSpawnS2CPacket(entity));
            } else {
                this.sendPacket(FakePackets.fakeEntitySpawnS2CPacket(entity));
            }
        }

        this.disguiselib$skipCheck = false;
    }
}
