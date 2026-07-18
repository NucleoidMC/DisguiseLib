package xyz.nucleoid.disguiselib.impl.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
import java.util.function.Consumer;
import java.util.function.Predicate;

import static xyz.nucleoid.disguiselib.impl.DisguiseLib.DISGUISE_TEAM;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayNetworkHandlerMixin_Disguiser extends ServerCommonPacketListenerImpl implements ExtendedHandler {
    @Shadow public ServerPlayer player;

    @Unique
    private final Set<Packet<?>> disguiselib$q = new HashSet<>();
    @Unique
    private int disguiselib$qTimer;
    @Unique
    private boolean disguiselib$sentTeamPacket;

    public ServerPlayNetworkHandlerMixin_Disguiser(MinecraftServer server, Connection connection, CommonListenerCookie clientData) {
        super(server, connection, clientData);
    }

    public void disguiselib$transformPacket(Packet<? super ClientGamePacketListener> packet, Runnable remove, Consumer<Packet<ClientGamePacketListener>> add) {
        Level world = this.player.level();
        if (packet instanceof ClientboundAddEntityPacket) {
            var entity = world.getEntity(((EntitySpawnS2CPacketAccessor) packet).getEntityId());

            if(entity != null) {
                disguiselib$sendFakePacket(entity, remove, add);
            }
        } else if (packet instanceof ClientboundRemoveEntitiesPacket && !((EntitiesDestroyS2CPacketAccessor) packet).getEntityIds().isEmpty() && ((EntitiesDestroyS2CPacketAccessor) packet).getEntityIds().getInt(0) == this.player.getId()) {
            remove.run();
            return;
        } else if(packet instanceof ClientboundSetEntityDataPacket) {
            // an ugly fix for #6
            int entityId = ((EntityTrackerUpdateS2CPacketAccessor) packet).getEntityId();
            if(entityId == this.player.getId() && ((EntityDisguise) this.player).isDisguised()) {
                List<SynchedEntityData.DataValue<?>> trackedValues = this.player.getEntityData().getNonDefaultValues();
                if(((EntityDisguise) this.player).getDisguiseType() != EntityTypes.PLAYER) {
                    Byte flags = this.player.getEntityData().get(EntityAccessor.getFLAGS());

                    boolean removed = trackedValues.removeIf(entry -> entry.value().equals(flags));
                    if(removed) {
                        SynchedEntityData.DataValue<Byte> fakeInvisibleFlag = SynchedEntityData.DataValue.create(EntityAccessor.getFLAGS(), (byte) (flags | 1 << 5));
                        trackedValues.add(fakeInvisibleFlag);
                    }
                }
                ((EntityTrackerUpdateS2CPacketAccessor) packet).setTrackedValues(trackedValues);
            } else if(!((EntityDisguise) this.player).hasTrueSight()) {
                // Fixing "wrong data" client issue (#1)
                // Just prevents the client from spamming the log
                Entity original = world.getEntity(entityId);

                // Only change the content if entity is disguised
                if(original != null && ((EntityDisguise) original).isDisguised()) {
                    Entity disguised = ((EntityDisguise) original).getDisguiseEntity();
                    if(disguised != null) {
                        ((DisguiseUtils) original).updateTrackedData();
                        List<SynchedEntityData.DataValue<?>> trackedValues = disguised.getEntityData().getNonDefaultValues();
                        ((EntityTrackerUpdateS2CPacketAccessor) packet).setTrackedValues(trackedValues);
                    }
                }
            }
            return;
        } else if(packet instanceof ClientboundUpdateAttributesPacket && !((EntityDisguise) this.player).hasTrueSight()) {
            // Fixing #2
            // Another client spam
            // Entity attributes "cannot" be sent for non-living entities
            Entity original = world.getEntity(((EntityAttributesS2CPacketAccessor) packet).getEntityId());
            EntityDisguise entityDisguise = (EntityDisguise) original;

            if(original != null && entityDisguise.isDisguised() && !((DisguiseUtils) original).disguiseAlive()) {
                remove.run();
                return;
            }
        } else if(packet instanceof ClientboundSetEntityMotionPacket velocityPacket) {
            int id = velocityPacket.id();
            if(id != this.player.getId()) {

                Entity entity1 = world.getEntity(id);
                if(entity1 != null && ((EntityDisguise) entity1).isDisguised()) {
                    // Cancels some client predictions
                    remove.run();
                }
            }
        }
    }

    /**
     * Sends fake packet instead of the real one.
     *
     * @param entity the entity that is disguised and needs to have a custom packet sent.
     */
    @Unique
    private void disguiselib$sendFakePacket(Entity entity, Runnable remove, Consumer<Packet<ClientGamePacketListener>> add) {
        EntityDisguise disguise = (EntityDisguise) entity;
        GameProfile profile = disguise.getGameProfile();
        Entity disguiseEntity = disguise.getDisguiseEntity();
        if (disguiseEntity == null) {
            return;
        }

        Packet<?> spawnPacket;
        var entry = new ServerEntity((ServerLevel) entity.level(), entity, 1, true, new ServerEntity.Synchronizer() {
            @Override
            public void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet) {

            }

            @Override
            public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet) {

            }

            @Override
            public void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> packet, Predicate<ServerPlayer> predicate) {

            }
        });
        if(((EntityDisguise) this.player).hasTrueSight() || !disguise.isDisguised())
            spawnPacket = entity.getAddEntityPacket(entry);
        else
            spawnPacket = FakePackets.universalSpawnPacket(entity, entry, entity.getId() != this.player.getId());

        if (disguise.getDisguiseType() == EntityTypes.PLAYER) {
            ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, (ServerPlayer) disguiseEntity);
            add.accept(packet);

            if (!(entity instanceof Player)) {
                var playerRemovePacket = new ClientboundPlayerInfoRemovePacket(new ArrayList<>(Collections.singletonList(profile.id())));
                this.disguiselib$q.add(playerRemovePacket);
                this.disguiselib$qTimer = 50;
            }
        }
        if (entity.getId() == this.player.getId()) {
            // We must treat disguised player differently
            // Why, I hear you ask ..?
            // Well, sending spawn packet of the new entity makes the player not being able to move :(
            if (disguise.getDisguiseType() != EntityTypes.PLAYER && disguise.isDisguised()) {
                if (disguiseEntity != null) {
                    if (spawnPacket instanceof ClientboundAddEntityPacket) {
                        ((EntitySpawnS2CPacketAccessor) spawnPacket).setEntityId(disguiseEntity.getId());
                        ((EntitySpawnS2CPacketAccessor) spawnPacket).setUuid(disguiseEntity.getUUID());
                    }
                    disguiseEntity.startRiding(this.player, true, false);
                    add.accept((Packet<ClientGamePacketListener>) spawnPacket);

                    ClientboundSetPlayerTeamPacket joinTeamPacket = ClientboundSetPlayerTeamPacket.createPlayerPacket(DISGUISE_TEAM, this.player.getGameProfile().name(), ClientboundSetPlayerTeamPacket.Action.ADD); // join team
                    add.accept(joinTeamPacket);
                }
            }
            remove.run();
        } else if(disguise.isDisguised()) {
            //this.player.getX()
            //ArmorStandEntity fakeStand = new ArmorStandEntity(this.player.world, );
            //fakeStand.startRiding(fakeStand, true);
            //new EntitySpawnS2CPacket(fakeStand);
            add.accept((Packet<ClientGamePacketListener>) spawnPacket);
            remove.run();
        }
    }


    @Inject(
        method = "handleMovePlayer(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V",
        at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
                shift = At.Shift.AFTER
        )
    )
    private void disguiselib$moveDisguiseEntity(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if(((EntityDisguise) this.player).isDisguised() && ((EntityDisguise) this.player).getDisguiseType() != EntityTypes.PLAYER) {
            // Moving disguise for the disguised player
            ClientboundTeleportEntityPacket s2CPacket = new ClientboundTeleportEntityPacket(((EntityDisguise) this.player).getDisguiseEntity().getId(), new PositionMoveRotation(player.trackingPosition(), Vec3.ZERO, player.getYRot(), player.getXRot()), Set.of(), false);
            ClientboundRotateHeadPacket headYawS2CPacket = new ClientboundRotateHeadPacket(this.player, (byte)((int)(this.player.getYHeadRot() * 256.0F / 360.0F)));

            //noinspection ConstantConditions
            ((EntitySetHeadYawS2CPacketAccessor) headYawS2CPacket).setEntityId(((EntityDisguise) this.player).getDisguiseEntity().getId());
            this.send(s2CPacket);
            this.send(headYawS2CPacket);
        }
    }


    @Inject(method = "handleMovePlayer(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V", at = @At("RETURN"))
    private void removeFromTablist(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if(!this.disguiselib$q.isEmpty() && --this.disguiselib$qTimer <= 0) {
            // fixme - non-living disguised as player still not showing up
            // fixme - player sometimes gets removed from tablist :(
            this.disguiselib$q.forEach(this::send);
            this.disguiselib$q.clear();}
    }

    public void disguiselib$onClientBrand() {
        if (!this.disguiselib$sentTeamPacket) {
            // Disabling collisions with the disguised entity itself
            ClientboundSetPlayerTeamPacket addTeamPacket = ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(DISGUISE_TEAM, true); // create team
            this.disguiselib$sentTeamPacket = true;
            this.send(addTeamPacket);

            if (((EntityDisguise) this.player).isDisguised()) {
                // Send join team packet to prevent "sliding"
                ClientboundSetPlayerTeamPacket joinTeamPacket = ClientboundSetPlayerTeamPacket.createPlayerPacket(DISGUISE_TEAM, this.player.getGameProfile().name(), ClientboundSetPlayerTeamPacket.Action.ADD); // join team
                this.send(joinTeamPacket);
            }
        }
    }
}
