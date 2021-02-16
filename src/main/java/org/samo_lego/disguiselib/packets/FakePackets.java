package org.samo_lego.disguiselib.packets;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.MobSpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerSpawnS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import org.samo_lego.disguiselib.EntityDisguise;
import org.samo_lego.disguiselib.mixin.accessor.EntitySpawnS2CPacketAccessor;
import org.samo_lego.disguiselib.mixin.accessor.MobSpawnS2CPacketAccessor;
import org.samo_lego.disguiselib.mixin.accessor.PlayerSpawnS2CPacketAccessor;

public class FakePackets {

    /**
     * Constructs a fake {@link MobSpawnS2CPacket} for the given entity.
     * @param entity entity that requires fake packet
     * @return fake {@link MobSpawnS2CPacket}
     */
    public static MobSpawnS2CPacket fakeMobSpawnS2CPacket(Entity entity) {
        MobSpawnS2CPacket packet = new MobSpawnS2CPacket();

        MobSpawnS2CPacketAccessor accessor = (MobSpawnS2CPacketAccessor) packet;
        accessor.setEntityId(entity.getEntityId());
        accessor.setUuid(entity.getUuid());

        accessor.setEntityType(Registry.ENTITY_TYPE.getRawId(((EntityDisguise) entity).getDisguiseType()));
        accessor.setX(entity.getX());
        accessor.setY(entity.getY());
        accessor.setZ(entity.getZ());

        accessor.setYaw((byte)((int)(entity.yaw * 256.0F / 360.0F)));
        accessor.setHeadYaw((byte)((int)(entity.getHeadYaw() * 256.0F / 360.0F)));
        accessor.setPitch((byte)((int)(entity.pitch * 256.0F / 360.0F)));

        double max = 3.9D;
        Vec3d vec3d = entity.getVelocity();
        double e = MathHelper.clamp(vec3d.x, -max, max);
        double f = MathHelper.clamp(vec3d.y, -max, max);
        double g = MathHelper.clamp(vec3d.z, -max, max);
        accessor.setVelocityX((int)(e * 8000.0D));
        accessor.setVelocityY((int)(f * 8000.0D));
        accessor.setVelocityZ((int)(g * 8000.0D));

        return packet;
    }

    /**
     * Constructs a fake {@link EntitySpawnS2CPacket} for the given entity.
     * @param entity entity that requires fake packet
     * @return fake {@link EntitySpawnS2CPacket}
     */
    public static EntitySpawnS2CPacket fakeEntitySpawnS2CPacket(Entity entity) {
        EntitySpawnS2CPacket packet = new EntitySpawnS2CPacket(entity);
        ((EntitySpawnS2CPacketAccessor) packet).setEntityType(((EntityDisguise) entity).getDisguiseType());

        return packet;
    }

    /**
     * Constructs a fake {@link PlayerSpawnS2CPacket} for the given entity.
     * Make sure you send the {@link net.minecraft.network.packet.s2c.play.PlayerListS2CPacket} as well
     * if you're using this for yourself!
     *
     * @param entity entity that requires fake packet
     * @return fake {@link PlayerSpawnS2CPacket}
     */
    public static PlayerSpawnS2CPacket fakePlayerSpawnS2CPacket(Entity entity) {
        PlayerSpawnS2CPacket packet = new PlayerSpawnS2CPacket();
        PlayerSpawnS2CPacketAccessor accessor = (PlayerSpawnS2CPacketAccessor) packet;

        accessor.setId(entity.getEntityId());
        accessor.setUuid(entity.getUuid());

        accessor.setX(entity.getX());
        accessor.setY(entity.getY());
        accessor.setZ(entity.getZ());

        accessor.setYaw((byte)((int)(entity.yaw * 256.0F / 360.0F)));
        accessor.setPitch((byte)((int)(entity.pitch * 256.0F / 360.0F)));

        return packet;
    }
}
