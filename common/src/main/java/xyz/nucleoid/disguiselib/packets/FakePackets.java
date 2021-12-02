package xyz.nucleoid.disguiselib.packets;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.MobSpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerSpawnS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import xyz.nucleoid.disguiselib.casts.EntityDisguise;
import xyz.nucleoid.disguiselib.mixin.accessor.EntitySpawnS2CPacketAccessor;
import xyz.nucleoid.disguiselib.mixin.accessor.MobSpawnS2CPacketAccessor;
import xyz.nucleoid.disguiselib.mixin.accessor.PlayerSpawnS2CPacketAccessor;

public class FakePackets {
    /**
     * Creates a fake spawn packet for entity.
     * Make sure entity is disguised, otherwise packet will stay the same.
     *
     * @param entity entity that requires fake spawn packet
     * @return fake entity spawn packet (Either player)
     */
    public static Packet<?> universalSpawnPacket(Entity entity) {
        // fixme - disguising non-living kicks you (just upon disguise)
        Entity disguise = ((EntityDisguise) entity).getDisguiseEntity();
        if(disguise == null) {
            disguise = entity;
        }

        Packet<?> packet = disguise.createSpawnPacket();

        if(packet instanceof MobSpawnS2CPacket) {
            packet = fakeMobSpawnS2CPacket(entity);
        } else if(packet instanceof EntitySpawnS2CPacket) {
            packet = fakeEntitySpawnS2CPacket(entity);
        } else if(packet instanceof PlayerSpawnS2CPacket) {
            packet = fakePlayerSpawnS2CPacket(entity);
        }

        return packet;
    }

    /**
     * Constructs a fake {@link MobSpawnS2CPacket} for the given entity.
     *
     * @param entity entity that requires fake packet
     *
     * @return fake {@link MobSpawnS2CPacket}
     */
    public static MobSpawnS2CPacket fakeMobSpawnS2CPacket(Entity entity) {
        EntityDisguise disguise = (EntityDisguise) entity;
        MobSpawnS2CPacket packet = new MobSpawnS2CPacket((LivingEntity) disguise.getDisguiseEntity());

        MobSpawnS2CPacketAccessor accessor = (MobSpawnS2CPacketAccessor) packet;
        accessor.setEntityId(entity.getId());
        accessor.setUuid(entity.getUuid());

        accessor.setEntityType(Registry.ENTITY_TYPE.getRawId(disguise.getDisguiseType()));
        accessor.setX(entity.getX());
        accessor.setY(entity.getY());
        accessor.setZ(entity.getZ());

        accessor.setYaw((byte) ((int) (entity.getY() * 256.0F / 360.0F)));
        accessor.setHeadYaw((byte) ((int) (entity.getHeadYaw() * 256.0F / 360.0F)));
        accessor.setPitch((byte) ((int) (entity.getPitch() * 256.0F / 360.0F)));

        double max = 3.9D;
        Vec3d vec3d = entity.getVelocity();
        double e = MathHelper.clamp(vec3d.x, -max, max);
        double f = MathHelper.clamp(vec3d.y, -max, max);
        double g = MathHelper.clamp(vec3d.z, -max, max);
        accessor.setVelocityX((int) (e * 8000.0D));
        accessor.setVelocityY((int) (f * 8000.0D));
        accessor.setVelocityZ((int) (g * 8000.0D));

        return packet;
    }

    /**
     * Constructs a fake {@link EntitySpawnS2CPacket} for the given entity.
     *
     * @param entity entity that requires fake packet
     *
     * @return fake {@link EntitySpawnS2CPacket}
     */
    public static EntitySpawnS2CPacket fakeEntitySpawnS2CPacket(Entity entity) {
        EntitySpawnS2CPacket packet = new EntitySpawnS2CPacket(entity);
        EntityDisguise fake = (EntityDisguise) entity;
        ((EntitySpawnS2CPacketAccessor) packet).setEntityType(fake.getDisguiseType());
        if(fake.getDisguiseType() == EntityType.FALLING_BLOCK && fake.getDisguiseEntity() instanceof FallingBlockEntity) {
            ((EntitySpawnS2CPacketAccessor) packet).setEntityData(
                    Block.getRawIdFromState(
                            ((FallingBlockEntity) fake.getDisguiseEntity()).getBlockState()
                    )
            );
        }

        return packet;
    }

    /**
     * Constructs a fake {@link PlayerSpawnS2CPacket} for the given entity.
     * Make sure you send the {@link net.minecraft.network.packet.s2c.play.PlayerListS2CPacket} as well
     * if you're using this for yourself!
     *
     * @param entity entity that requires fake packet
     *
     * @return fake {@link PlayerSpawnS2CPacket}
     */
    public static PlayerSpawnS2CPacket fakePlayerSpawnS2CPacket(Entity entity) {
        Entity disguise = ((EntityDisguise) entity).getDisguiseEntity();
        PlayerSpawnS2CPacket packet;

        if (disguise instanceof PlayerEntity playerDisguise)  // Needed in case of taterzens - when they're disguised "back" to players, the check will be false
            packet = new PlayerSpawnS2CPacket(playerDisguise);
        else
            packet = new PlayerSpawnS2CPacket(entity.getServer().getPlayerManager().getPlayerList().get(0));

        PlayerSpawnS2CPacketAccessor accessor = (PlayerSpawnS2CPacketAccessor) packet;

        accessor.setId(entity.getId());
        accessor.setUuid(entity.getUuid());

        accessor.setX(entity.getX());
        accessor.setY(entity.getY());
        accessor.setZ(entity.getZ());

        accessor.setYaw((byte) ((int) (entity.getYaw() * 256.0F / 360.0F)));
        accessor.setPitch((byte) ((int) (entity.getPitch() * 256.0F / 360.0F)));

        return packet;
    }
}
