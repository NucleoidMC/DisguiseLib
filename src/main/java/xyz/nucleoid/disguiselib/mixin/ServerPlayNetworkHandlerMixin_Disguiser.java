package xyz.nucleoid.disguiselib.mixin;

import com.mojang.authlib.GameProfile;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
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
import java.util.List;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin_Disguiser {
    @Shadow public ServerPlayerEntity player;
    @Unique private boolean disguiselib$skipCheck;
    @Shadow public abstract void sendPacket(Packet<?> packet);

    @Shadow @Final private MinecraftServer server;

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
                // This means player has un-disguised
                // so we destroy the fake disguise entity
                //((EntitiesDestroyS2CPacketAccessor) packet).setEntityIds(new int[]{((EntityDisguise) player).getDisguiseEntity().getEntityId()});
                System.out.println("Un disguise: " + ((EntityDisguise) player).getDisguiseEntity().getEntityId());
                ci.cancel();
                return;
            } else if(packet instanceof EntityTrackerUpdateS2CPacket) {
                // Fixing "wrong data" client issue (#1)
                // Just prevents the client from spamming the log
                Entity original = world.getEntityById(((EntityTrackerUpdateS2CPacketAccessor) packet).getEntityId());

                if(((EntityTrackerUpdateS2CPacketAccessor) packet).getEntityId() != this.player.getEntityId()) {
                    // Only change the content if entity is disguised
                    if(original != null && ((EntityDisguise) original).isDisguised()) {
                        Entity disguised = ((EntityDisguise) original).getDisguiseEntity();
                        if(disguised != null) {
                            List<DataTracker.Entry<?>> trackedValues = disguised.getDataTracker().getAllEntries();
                            ((EntityTrackerUpdateS2CPacketAccessor) packet).setTrackedValues(trackedValues);
                            return;
                        }
                    }
                }
            } else if(packet instanceof EntityAttributesS2CPacket) {
                // Fixing #2
                // Another client spam
                Entity original = world.getEntityById(((EntityAttributesS2CPacketAccessor) packet).getEntityId());

                if(original != null && ((EntityDisguise) original).isDisguised() && !((EntityDisguise) original).disguiseAlive()) {
                    ci.cancel();
                    return;
                }
            }
            EntityDisguise disguise = (EntityDisguise) entity;
            if(
                    disguise != null /*&&
                    disguise.isDisguised() /*&&
                    entity.getEntityId() != this.player.getEntityId()*/ // do not send the packet to the player themselves
            ) {
                disguiselib$sendFakePacket(entity, ci);
                //ci.cancel();
            }
        }
    }

    /**
     * Sends fake packet instead of the real one
     * @param entity the entity that is disguised and needs to have a custom packet sent.
     */
    @Unique
    private void disguiselib$sendFakePacket(Entity entity, CallbackInfo ci) {
        EntityDisguise disguise = (EntityDisguise) entity;
        GameProfile profile = disguise.getGameProfile();
        Entity disguiseEntity = disguise.getDisguiseEntity();

        this.disguiselib$skipCheck = true;

        if(entity.getEntityId() == this.player.getEntityId()) {
            // We must treat disguised player differently
            if(disguise.getDisguiseType() != EntityType.PLAYER && disguise.isDisguised()) {
                if(disguiseEntity != null) {
                    System.out.println("Disguise Entity : " + disguiseEntity.getEntityId());
                    if(disguise.disguiseAlive()) {
                        //disguiseEntity.set
                        MobSpawnS2CPacket packet = FakePackets.fakeMobSpawnS2CPacket(entity);
                        // We must set
                        ((MobSpawnS2CPacketAccessor) packet).setEntityId(disguiseEntity.getEntityId());
                        this.sendPacket(packet);
                    } else {
                        EntitySpawnS2CPacket packet = FakePackets.fakeEntitySpawnS2CPacket(entity);
                        ((EntitySpawnS2CPacketAccessor) packet).setEntityId(disguiseEntity.getEntityId());
                        this.sendPacket(packet);
                    }
                    ci.cancel();
                }
            }
        } else if(disguise.isDisguised()) {
            if(disguise.getDisguiseType() == EntityType.PLAYER) {
                PlayerListS2CPacket packet = new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER);
                PlayerListS2CPacketAccessor listS2CPacketAccessor = (PlayerListS2CPacketAccessor) packet;
                listS2CPacketAccessor.setEntries(Collections.singletonList(packet.new Entry(profile, 0, GameMode.SURVIVAL, new LiteralText(profile.getName()))));

                this.sendPacket(packet);
                this.sendPacket(FakePackets.fakePlayerSpawnS2CPacket(entity));

                // Removes player from tab screen
                // so disguised entities don't show up there
                // todo player disguising
                PlayerListS2CPacket listPacket = new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER);
                //noinspection ConstantConditions
                PlayerListS2CPacketAccessor listPacketAccessor = (PlayerListS2CPacketAccessor) listPacket;
                listPacketAccessor.setEntries(Collections.singletonList(listPacket.new Entry(profile, 0, GameMode.SURVIVAL, new LiteralText(profile.getName()))));

                this.sendPacket(listPacket);
            } else {
                if(disguise.disguiseAlive()) {
                    this.sendPacket(FakePackets.fakeMobSpawnS2CPacket(entity));
                } else {
                    this.sendPacket(FakePackets.fakeEntitySpawnS2CPacket(entity));
                }
            }
            ci.cancel();
        }
        this.disguiselib$skipCheck = false;
    }


    @Inject(
        method = "onPlayerMove(Lnet/minecraft/network/packet/c2s/play/PlayerMoveC2SPacket;)V",
        at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
                shift = At.Shift.AFTER
        )
    )
    private void disguiselib$moveDisguiseEntity(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if(((EntityDisguise) this.player).isDisguised()) {
            // Moving disguise for the disguised player
            EntityPositionS2CPacket s2CPacket = new EntityPositionS2CPacket(this.player);
            //noinspection ConstantConditions
            ((EntityPositionS2CPacketAccessor) s2CPacket).setEntityId(((EntityDisguise) this.player).getDisguiseEntity().getEntityId());
            this.sendPacket(s2CPacket);
        }
    }
}
