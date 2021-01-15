package net.sistr.lmrb.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.sistr.lmml.SideChecker;

public class Networking {
    public static Networking INSTANCE = new Networking();

    @Environment(EnvType.CLIENT)
    public void clientInit() {
        ClientSidePacketRegistry.INSTANCE.register(SyncSoundConfigPacket.ID, SyncSoundConfigPacket::receiveS2CPacket);
        ClientSidePacketRegistry.INSTANCE.register(OpenIFFScreenPacket.ID, OpenIFFScreenPacket::receiveS2CPacket);
    }

    public void serverInit() {
        ServerSidePacketRegistry.INSTANCE.register(SyncMovingStatePacket.ID, SyncMovingStatePacket::receiveC2SPacket);
        ServerSidePacketRegistry.INSTANCE.register(SyncSoundConfigPacket.ID, SyncSoundConfigPacket::receiveC2SPacket);
        ServerSidePacketRegistry.INSTANCE.register(OpenIFFScreenPacket.ID, OpenIFFScreenPacket::receiveC2SPacket);
        ServerSidePacketRegistry.INSTANCE.register(SyncIFFPacket.ID, SyncIFFPacket::receiveC2SPacket);
    }

}
