package net.sistr.littlemaidrebirth.network;

import me.shedaniel.architectury.networking.NetworkManager;
import me.shedaniel.architectury.platform.Platform;
import me.shedaniel.architectury.utils.Env;
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
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SyncMovingStatePacket.ID, SyncMovingStatePacket::receiveC2SPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SyncSoundConfigPacket.ID, SyncSoundConfigPacket::receiveC2SPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, OpenIFFScreenPacket.ID, OpenIFFScreenPacket::receiveC2SPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SyncIFFPacket.ID, SyncIFFPacket::receiveC2SPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SyncBloodSuckPacket.ID, SyncBloodSuckPacket::receiveC2SPacket);
    }

}
