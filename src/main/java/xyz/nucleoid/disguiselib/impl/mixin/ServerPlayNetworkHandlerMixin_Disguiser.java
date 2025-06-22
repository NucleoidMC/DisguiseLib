package xyz.nucleoid.disguiselib.impl.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
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

import static xyz.nucleoid.disguiselib.impl.DisguiseLib.DISGUISE_TEAM;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin_Disguiser extends ServerCommonNetworkHandler implements ExtendedHandler {
    @Shadow public ServerPlayerEntity player;

    @Unique
    private final Set<Packet<?>> disguiselib$q = new HashSet<>();
    @Unique
    private int disguiselib$qTimer;
    @Unique
    private boolean disguiselib$sentTeamPacket;

    public ServerPlayNetworkHandlerMixin_Disguiser(MinecraftServer server, ClientConnection connection, ConnectedClientData clientData) {
        super(server, connection, clientData);
    }

    public void disguiselib$transformPacket(Packet<? super ClientPlayPacketListener> packet, Runnable remove, Consumer<Packet<ClientPlayPacketListener>> add) {
        World world = this.player.getWorld();
        if (packet instanceof EntitySpawnS2CPacket) {
            var entity = world.getEntityById(((EntitySpawnS2CPacketAccessor) packet).getEntityId());

            if(entity != null) {
                disguiselib$sendFakePacket(entity, remove, add);
            }
        } else if (packet instanceof EntitiesDestroyS2CPacket && !((EntitiesDestroyS2CPacketAccessor) packet).getEntityIds().isEmpty() && ((EntitiesDestroyS2CPacketAccessor) packet).getEntityIds().getInt(0) == this.player.getId()) {
            remove.run();
            return;
        } else if(packet instanceof EntityTrackerUpdateS2CPacket) {
            // an ugly fix for #6
            int entityId = ((EntityTrackerUpdateS2CPacketAccessor) packet).getEntityId();
            if(entityId == this.player.getId() && ((EntityDisguise) this.player).isDisguised()) {
                List<DataTracker.SerializedEntry<?>> trackedValues = this.player.getDataTracker().getChangedEntries();
                if(((EntityDisguise) this.player).getDisguiseType() != EntityType.PLAYER) {
                    Byte flags = this.player.getDataTracker().get(EntityAccessor.getFLAGS());

                    boolean removed = trackedValues.removeIf(entry -> entry.value().equals(flags));
                    if(removed) {
                        DataTracker.SerializedEntry<Byte> fakeInvisibleFlag = DataTracker.SerializedEntry.of(EntityAccessor.getFLAGS(), (byte) (flags | 1 << 5));
                        trackedValues.add(fakeInvisibleFlag);
                    }
                }
                ((EntityTrackerUpdateS2CPacketAccessor) packet).setTrackedValues(trackedValues);
            } else if(!((EntityDisguise) this.player).hasTrueSight()) {
                // Fixing "wrong data" client issue (#1)
                // Just prevents the client from spamming the log
                Entity original = world.getEntityById(entityId);

                // Only change the content if entity is disguised
                if(original != null && ((EntityDisguise) original).isDisguised()) {
                    Entity disguised = ((EntityDisguise) original).getDisguiseEntity();
                    if(disguised != null) {
                        ((DisguiseUtils) original).updateTrackedData();
                        List<DataTracker.SerializedEntry<?>> trackedValues = disguised.getDataTracker().getChangedEntries();
                        ((EntityTrackerUpdateS2CPacketAccessor) packet).setTrackedValues(trackedValues);
                    }
                }
            }
            return;
        } else if(packet instanceof EntityAttributesS2CPacket && !((EntityDisguise) this.player).hasTrueSight()) {
            // Fixing #2
            // Another client spam
            // Entity attributes "cannot" be sent for non-living entities
            Entity original = world.getEntityById(((EntityAttributesS2CPacketAccessor) packet).getEntityId());
            EntityDisguise entityDisguise = (EntityDisguise) original;

            if(original != null && entityDisguise.isDisguised() && !((DisguiseUtils) original).disguiseAlive()) {
                remove.run();
                return;
            }
        } else if(packet instanceof EntityVelocityUpdateS2CPacket velocityPacket) {
            int id = velocityPacket.getEntityId();
            if(id != this.player.getId()) {

                Entity entity1 = world.getEntityById(id);
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
    private void disguiselib$sendFakePacket(Entity entity, Runnable remove, Consumer<Packet<ClientPlayPacketListener>> add) {
        EntityDisguise disguise = (EntityDisguise) entity;
        GameProfile profile = disguise.getGameProfile();
        Entity disguiseEntity = disguise.getDisguiseEntity();
        if (disguiseEntity == null) {
            return;
        }

        Packet<?> spawnPacket;
        var entry = new EntityTrackerEntry((ServerWorld) entity.getWorld(), entity, 1, true, (c) -> {}, (a, b) -> {});
        if(((EntityDisguise) this.player).hasTrueSight() || !disguise.isDisguised())
            spawnPacket = entity.createSpawnPacket(entry);
        else
            spawnPacket = FakePackets.universalSpawnPacket(entity, entry, entity.getId() != this.player.getId());

        if (disguise.getDisguiseType() == EntityType.PLAYER) {
            PlayerListS2CPacket packet = new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, (ServerPlayerEntity) disguiseEntity);
            add.accept(packet);

            if (!(entity instanceof PlayerEntity)) {
                var playerRemovePacket = new PlayerRemoveS2CPacket(new ArrayList<>(Collections.singletonList(profile.getId())));
                this.disguiselib$q.add(playerRemovePacket);
                this.disguiselib$qTimer = 50;
            }
        }
        if (entity.getId() == this.player.getId()) {
            // We must treat disguised player differently
            // Why, I hear you ask ..?
            // Well, sending spawn packet of the new entity makes the player not being able to move :(
            if (disguise.getDisguiseType() != EntityType.PLAYER && disguise.isDisguised()) {
                if (disguiseEntity != null) {
                    if (spawnPacket instanceof EntitySpawnS2CPacket) {
                        ((EntitySpawnS2CPacketAccessor) spawnPacket).setEntityId(disguiseEntity.getId());
                        ((EntitySpawnS2CPacketAccessor) spawnPacket).setUuid(disguiseEntity.getUuid());
                    }
                    disguiseEntity.startRiding(this.player, true);
                    add.accept((Packet<ClientPlayPacketListener>) spawnPacket);

                    TeamS2CPacket joinTeamPacket = TeamS2CPacket.changePlayerTeam(DISGUISE_TEAM, this.player.getGameProfile().getName(), TeamS2CPacket.Operation.ADD); // join team
                    add.accept(joinTeamPacket);
                }
            }
            remove.run();
        } else if(disguise.isDisguised()) {
            //this.player.getX()
            //ArmorStandEntity fakeStand = new ArmorStandEntity(this.player.world, );
            //fakeStand.startRiding(fakeStand, true);
            //new EntitySpawnS2CPacket(fakeStand);
            add.accept((Packet<ClientPlayPacketListener>) spawnPacket);
            remove.run();
        }
    }


    @Inject(
        method = "onPlayerMove(Lnet/minecraft/network/packet/c2s/play/PlayerMoveC2SPacket;)V",
        at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
                shift = At.Shift.AFTER
        )
    )
    private void disguiselib$moveDisguiseEntity(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if(((EntityDisguise) this.player).isDisguised() && ((EntityDisguise) this.player).getDisguiseType() != EntityType.PLAYER) {
            // Moving disguise for the disguised player
            EntityPositionS2CPacket s2CPacket = new EntityPositionS2CPacket(((EntityDisguise) this.player).getDisguiseEntity().getId(), new PlayerPosition(player.getSyncedPos(), Vec3d.ZERO, player.getYaw(), player.getPitch()), Set.of(), false);
            EntitySetHeadYawS2CPacket headYawS2CPacket = new EntitySetHeadYawS2CPacket(this.player, (byte)((int)(this.player.getHeadYaw() * 256.0F / 360.0F)));

            //noinspection ConstantConditions
            ((EntitySetHeadYawS2CPacketAccessor) headYawS2CPacket).setEntityId(((EntityDisguise) this.player).getDisguiseEntity().getId());
            this.sendPacket(s2CPacket);
            this.sendPacket(headYawS2CPacket);
        }
    }


    @Inject(method = "onPlayerMove(Lnet/minecraft/network/packet/c2s/play/PlayerMoveC2SPacket;)V", at = @At("RETURN"))
    private void removeFromTablist(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if(!this.disguiselib$q.isEmpty() && --this.disguiselib$qTimer <= 0) {
            // fixme - non-living disguised as player still not showing up
            // fixme - player sometimes gets removed from tablist :(
            this.disguiselib$q.forEach(this::sendPacket);
            this.disguiselib$q.clear();}
    }

    public void disguiselib$onClientBrand() {
        if (!this.disguiselib$sentTeamPacket) {
            // Disabling collisions with the disguised entity itself
            TeamS2CPacket addTeamPacket = TeamS2CPacket.updateTeam(DISGUISE_TEAM, true); // create team
            this.disguiselib$sentTeamPacket = true;
            this.sendPacket(addTeamPacket);

            if (((EntityDisguise) this.player).isDisguised()) {
                // Send join team packet to prevent "sliding"
                TeamS2CPacket joinTeamPacket = TeamS2CPacket.changePlayerTeam(DISGUISE_TEAM, this.player.getGameProfile().getName(), TeamS2CPacket.Operation.ADD); // join team
                this.sendPacket(joinTeamPacket);
            }
        }
    }
}
