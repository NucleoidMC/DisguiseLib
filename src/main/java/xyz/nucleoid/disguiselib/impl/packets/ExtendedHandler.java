package xyz.nucleoid.disguiselib.impl.packets;

import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;

import java.util.function.Consumer;

public interface ExtendedHandler {
    void disguiselib$transformPacket(Packet<ClientPlayPacketListener> packet, Runnable remove, Consumer<Packet<ClientPlayPacketListener>> add);
    void disguiselib$onClientBrand();
}
