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
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SpawnLittleMaidPacket.ID, SpawnLittleMaidPacket::receiveS2CPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenTargetTagScreenPacket.ID, OpenTargetTagScreenPacket::receiveS2CPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenMaidManagerScreenPacket.ID, OpenMaidManagerScreenPacket::receiveS2CPacket);
    }

    private void serverInit() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, C2SSetMovingStatePacket.ID, C2SSetMovingStatePacket::receiveC2SPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SyncSoundConfigPacket.ID, SyncSoundConfigPacket::receiveC2SPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, C2SSetBloodSuckPacket.ID, C2SSetBloodSuckPacket::receiveC2SPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, C2SSetWorkItemSlotSizePacket.ID, C2SSetWorkItemSlotSizePacket::receiveC2SPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, C2SSetTargetTagsPacket.ID, C2SSetTargetTagsPacket::receiveC2SPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, OpenTargetTagScreenPacket.ID, OpenTargetTagScreenPacket::receiveC2SPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, OpenMaidManagerScreenPacket.ID, OpenMaidManagerScreenPacket::receiveC2SPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, C2SOpenInventoryPacket.ID, C2SOpenInventoryPacket::receiveC2SPacket);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, C2SCallWaitPacket.ID, C2SCallWaitPacket::receiveC2SPacket);
    }

}
