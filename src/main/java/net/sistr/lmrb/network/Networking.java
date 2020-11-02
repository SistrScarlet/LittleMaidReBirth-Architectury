package net.sistr.lmrb.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.sistr.lmml.SideChecker;

public class Networking {
    public static Networking INSTANCE = new Networking();

    public void init() {
        if (SideChecker.isClient()) {
            clientInit();
        }
        serverInit();
    }

    @Environment(EnvType.CLIENT)
    private void clientInit() {
        ClientSidePacketRegistry.INSTANCE.register(SyncSoundConfigPacket.ID, SyncSoundConfigPacket::receiveS2CPacket);
    }

    private void serverInit() {
        ServerSidePacketRegistry.INSTANCE.register(SyncMovingStatePacket.ID, SyncMovingStatePacket::receiveC2SPacket);
        ServerSidePacketRegistry.INSTANCE.register(SyncSoundConfigPacket.ID, SyncSoundConfigPacket::receiveC2SPacket);
    }

}
