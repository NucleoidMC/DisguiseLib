package xyz.nucleoid.disguiselib.impl.packets;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import xyz.nucleoid.disguiselib.api.EntityDisguise;

public class FakePackets {
    /**
     * Creates a fake spawn packet for entity.
     * Make sure entity is disguised, otherwise packet will stay the same.
     *
     * @param entity entity that requires fake spawn packet
     * @param entry
     * @param b
     * @return fake entity spawn packet (Either player)
     */
    public static Packet<?> universalSpawnPacket(Entity entity, ServerEntity entry, boolean replace) {
        // fixme - disguising non-living kicks you (just upon disguise)
        Entity disguise = ((EntityDisguise) entity).getDisguiseEntity();
        if(disguise == null) {
            disguise = entity;
        }

        try {
            if (replace) {
                var x = disguise.getId();
                var y = disguise.getUUID();
                disguise.setId(entity.getId());
                disguise.setUUID(entity.getUUID());
                Packet<?> packet = disguise.getAddEntityPacket(entry);
                disguise.setId(x);
                disguise.setUUID(y);
                return packet;
            } else {
                return disguise.getAddEntityPacket(entry);
            }
        } catch (Throwable e) {
            return entity.getAddEntityPacket(entry);
        }
    }
}
