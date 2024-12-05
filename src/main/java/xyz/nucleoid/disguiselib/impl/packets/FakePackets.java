package xyz.nucleoid.disguiselib.impl.packets;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.EntityTrackerEntry;
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
    public static Packet<?> universalSpawnPacket(Entity entity, EntityTrackerEntry entry, boolean replace) {
        // fixme - disguising non-living kicks you (just upon disguise)
        Entity disguise = ((EntityDisguise) entity).getDisguiseEntity();
        if(disguise == null) {
            disguise = entity;
        }

        try {
            if (replace) {
                var x = disguise.getId();
                var y = disguise.getUuid();
                disguise.setId(entity.getId());
                disguise.setUuid(entity.getUuid());
                Packet<?> packet = disguise.createSpawnPacket(entry);
                disguise.setId(x);
                disguise.setUuid(y);
                return packet;
            } else {
                return disguise.createSpawnPacket(entry);
            }
        } catch (Throwable e) {
            return entity.createSpawnPacket(entry);
        }
    }
}
