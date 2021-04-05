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
     * entity type / id in the packet will be changed.
     *
     * As minecraft client doesn't allow moving if you send it an entity with the same
     * id as player, we send the disguised player another entity, so they will see their
     * own disguise.
     *
     * @param packet packet being sent
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

            if(packet instanceof PlayerListS2CPacket && ((PlayerListS2CPacketAccessor) packet).getAction().equals(PlayerListS2CPacket.Action.ADD_PLAYER)) {
                PlayerListS2CPacket.Entry entry = ((PlayerListS2CPacketAccessor) packet).getEntries().get(0);
                if(this.player.getGameProfile().getId().equals(entry.getProfile().getId())) {
                    entity = this.player;
                }
            } else if(packet instanceof PlayerSpawnS2CPacket) {
                entity = world.getEntityById(((PlayerSpawnS2CPacketAccessor) packet).getId());
            } else if(packet instanceof MobSpawnS2CPacket) {
                entity = world.getEntityById(((MobSpawnS2CPacketAccessor) packet).getEntityId());
            } else if(packet instanceof EntitySpawnS2CPacket) {
                entity = world.getEntityById(((EntitySpawnS2CPacketAccessor) packet).getEntityId());
            } else if(packet instanceof EntitiesDestroyS2CPacket && ((EntitiesDestroyS2CPacketAccessor) packet).getEntityIds()[0] == this.player.getEntityId()) {
                ci.cancel();
                return;
            } else if(packet instanceof EntityTrackerUpdateS2CPacket) {
                // Fixing "wrong data" client issue (#1)
                // Just prevents the client from spamming the log
                Entity original = world.getEntityById(((EntityTrackerUpdateS2CPacketAccessor) packet).getEntityId());

                if(original != null && ((EntityDisguise) original).isDisguised()) {
                    Entity disguised = ((EntityDisguise) original).getDisguiseEntity();
                    if(disguised != null) {
                        if(((EntityTrackerUpdateS2CPacketAccessor) packet).getEntityId() != this.player.getEntityId()) {
                            // Only change the content if entity is disguised
                            List<DataTracker.Entry<?>> trackedValues = disguised.getDataTracker().getAllEntries();
                            ((EntityTrackerUpdateS2CPacketAccessor) packet).setTrackedValues(trackedValues);
                        } else {
                            this.player.setInvisible(disguised.getType() != EntityType.PLAYER);
                        }
                        return;
                    }
                }
            } else if(packet instanceof EntityAttributesS2CPacket) {
                // Fixing #2
                // Another client spam
                Entity original = world.getEntityById(((EntityAttributesS2CPacketAccessor) packet).getEntityId());
                EntityDisguise entityDisguise = (EntityDisguise) original;

                if(original != null && entityDisguise.isDisguised()) {
                    /*if(((EntityAttributesS2CPacketAccessor) packet).getEntityId() != this.player.getEntityId()) {
                        ((EntityAttributesS2CPacketAccessor) packet).setEntityId(entityDisguise.getDisguiseEntity().getEntityId());
                    } else */if(!entityDisguise.disguiseAlive()) {
                        ci.cancel();
                    }
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

        if(disguise.getDisguiseType() == EntityType.PLAYER) {
            PlayerListS2CPacket packet = new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER);
            //noinspection ConstantConditions
            PlayerListS2CPacketAccessor listS2CPacketAccessor = (PlayerListS2CPacketAccessor) packet;
            listS2CPacketAccessor.setEntries(Collections.singletonList(packet.new Entry(profile, 0, GameMode.SURVIVAL, new LiteralText(profile.getName()))));

            this.sendPacket(packet);
        }
        if(entity.getEntityId() == this.player.getEntityId()) {
            // We must treat disguised player differently
            // Why, I hear you ask ..?
            // Well, sending spawn packet of the new entity makes the player not being able to move :(
            if(disguise.getDisguiseType() != EntityType.PLAYER && disguise.isDisguised()) {
                if(disguiseEntity != null) {
                    if(disguise.disguiseAlive()) {
                        MobSpawnS2CPacket packet = FakePackets.fakeMobSpawnS2CPacket(entity);
                        ((MobSpawnS2CPacketAccessor) packet).setEntityId(disguiseEntity.getEntityId());
                        this.sendPacket(packet);
                    } else {
                        EntitySpawnS2CPacket packet = FakePackets.fakeEntitySpawnS2CPacket(entity);
                        ((EntitySpawnS2CPacketAccessor) packet).setEntityId(disguiseEntity.getEntityId());
                        this.sendPacket(packet);
                    }
                }
            }
            ci.cancel();
        } else if(disguise.isDisguised()) {
            if(disguise.getDisguiseType() == EntityType.PLAYER) {
                this.sendPacket(FakePackets.fakePlayerSpawnS2CPacket(entity));
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
        if(((EntityDisguise) this.player).isDisguised() && ((EntityDisguise) this.player).getDisguiseType() != EntityType.PLAYER) {
            // Moving disguise for the disguised player
            EntityPositionS2CPacket s2CPacket = new EntityPositionS2CPacket(this.player);
            EntitySetHeadYawS2CPacket headYawS2CPacket = new EntitySetHeadYawS2CPacket(this.player, (byte)((int)(this.player.getHeadYaw() * 256.0F / 360.0F)));

            //noinspection ConstantConditions
            ((EntityPositionS2CPacketAccessor) s2CPacket).setEntityId(((EntityDisguise) this.player).getDisguiseEntity().getEntityId());
            //noinspection ConstantConditions
            ((EntitySetHeadYawS2CPacketAccessor) headYawS2CPacket).setEntityId(((EntityDisguise) this.player).getDisguiseEntity().getEntityId());
            this.sendPacket(s2CPacket);
        }
    }
}
