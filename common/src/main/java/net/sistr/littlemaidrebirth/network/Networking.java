package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.sistr.littlemaidmodelloader.util.SideChecker;

public class Networking {
    public static Networking INSTANCE = new Networking();

    public void init() {
        if (SideChecker.isClient()) clientInit();
        serverInit();
    }

    @Environment(EnvType.CLIENT)
    private void clientInit() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncSoundConfigPacket.ID, SyncSoundConfigPacket::receiveS2CPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenIFFScreenPacket.ID, OpenIFFScreenPacket::receiveS2CPacket);
    }

    private void serverInit() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SyncMovingStatePacket.ID, SyncMovingStatePacket::receiveC2SPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SyncSoundConfigPacket.ID, SyncSoundConfigPacket::receiveC2SPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, OpenIFFScreenPacket.ID, OpenIFFScreenPacket::receiveC2SPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SyncIFFPacket.ID, SyncIFFPacket::receiveC2SPacket);
    }

}
