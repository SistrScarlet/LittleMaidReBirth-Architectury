package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class Networking {
    public static Networking INSTANCE = new Networking();

    public void init() {
        if (Platform.getEnvironment() == Env.CLIENT) clientInit();
        serverInit();
    }

    @Environment(EnvType.CLIENT)
    private void clientInit() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncSoundConfigPacket.ID, SyncSoundConfigPacket::receiveS2CPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenIFFScreenPacket.ID, OpenIFFScreenPacket::receiveS2CPacket);
    }

    private void serverInit() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, C2SSetMovingStatePacket.ID, C2SSetMovingStatePacket::receiveC2SPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SyncSoundConfigPacket.ID, SyncSoundConfigPacket::receiveC2SPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, OpenIFFScreenPacket.ID, OpenIFFScreenPacket::receiveC2SPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, C2SSetIFFPacket.ID, C2SSetIFFPacket::receiveC2SPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, C2SSetBloodSuckPacket.ID, C2SSetBloodSuckPacket::receiveC2SPacket);
    }

}
