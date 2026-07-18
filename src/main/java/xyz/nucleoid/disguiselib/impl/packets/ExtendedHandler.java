package xyz.nucleoid.disguiselib.impl.packets;

import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;

public interface ExtendedHandler {
    void disguiselib$transformPacket(Packet<? super ClientGamePacketListener> packet, Runnable remove, Consumer<Packet<ClientGamePacketListener>> add);
    void disguiselib$onClientBrand();
}
