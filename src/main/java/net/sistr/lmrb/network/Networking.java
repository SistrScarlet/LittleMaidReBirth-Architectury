package net.sistr.lmrb.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class Networking {
    public static Networking INSTANCE = new Networking();

    @Environment(EnvType.CLIENT)
    public void clientInit() {
        ClientPlayNetworking.registerGlobalReceiver(SyncSoundConfigPacket.ID, SyncSoundConfigPacket::receiveS2CPacket);
        ClientPlayNetworking.registerGlobalReceiver(OpenIFFScreenPacket.ID, OpenIFFScreenPacket::receiveS2CPacket);
    }

    public void serverInit() {
        ServerPlayNetworking.registerGlobalReceiver(SyncMovingStatePacket.ID, SyncMovingStatePacket::receiveC2SPacket);
        ServerPlayNetworking.registerGlobalReceiver(SyncSoundConfigPacket.ID, SyncSoundConfigPacket::receiveC2SPacket);
        ServerPlayNetworking.registerGlobalReceiver(OpenIFFScreenPacket.ID, OpenIFFScreenPacket::receiveC2SPacket);
        ServerPlayNetworking.registerGlobalReceiver(SyncIFFPacket.ID, SyncIFFPacket::receiveC2SPacket);
    }

}
