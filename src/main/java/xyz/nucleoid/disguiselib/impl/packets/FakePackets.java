package xyz.nucleoid.disguiselib.impl.packets;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.disguiselib.api.EntityDisguise;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.EntitySpawnS2CPacketAccessor;

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

        try {
            Packet<?> packet = disguise.createSpawnPacket();

            if(packet instanceof EntitySpawnS2CPacket) {
                packet = fakeMobSpawnS2CPacket(entity);
            }

            return packet;
        } catch (Throwable e) {
            return entity.createSpawnPacket();
        }
    }

    /**
     * Constructs a fake {@link EntitySpawnS2CPacket} for the given entity.
     *
     * @param entity entity that requires fake packet
     *
     * @return fake {@link EntitySpawnS2CPacket}
     */
    public static EntitySpawnS2CPacket fakeMobSpawnS2CPacket(Entity entity) {
        EntityDisguise disguise = (EntityDisguise) entity;
        EntitySpawnS2CPacket packet = new EntitySpawnS2CPacket(disguise.getDisguiseEntity());

        EntitySpawnS2CPacketAccessor accessor = (EntitySpawnS2CPacketAccessor) packet;
        accessor.setEntityId(entity.getId());
        accessor.setUuid(entity.getUuid());

        var type = disguise.getDisguiseType();

        accessor.setEntityType(type != EntityType.MARKER ? type : EntityType.PIG);
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
}
