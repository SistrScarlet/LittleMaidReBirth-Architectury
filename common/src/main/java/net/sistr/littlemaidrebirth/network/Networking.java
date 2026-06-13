package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class Networking {
  public static final Networking INSTANCE = new Networking();

  public void init() {
    if (Platform.getEnvironment() == Env.CLIENT) {
      // クライアントでは registerReceiver(S2C, ...) が type 登録も内部で行うため、
      // 明示登録は不要 (二重登録すると Fabric の PayloadTypeRegistry が IllegalArgumentException)。
      clientInit();
    } else {
      // 専用サーバには S2C receiver が無いため、送信用に type 登録のみ明示する。
      NetworkManager.registerS2CPayloadType(SyncSoundConfigPacket.ID, SyncSoundConfigPacket.CODEC);
      NetworkManager.registerS2CPayloadType(
          OpenTargetTagScreenPacket.ID, OpenTargetTagScreenPacket.CODEC);
      NetworkManager.registerS2CPayloadType(
          OpenMaidManagerScreenPacket.ID, OpenMaidManagerScreenPacket.CODEC);
    }
    serverInit();
  }

  @Environment(EnvType.CLIENT)
  private void clientInit() {
    NetworkManager.registerReceiver(
        NetworkManager.Side.S2C,
        SyncSoundConfigPacket.ID,
        SyncSoundConfigPacket.CODEC,
        SyncSoundConfigPacket::receiveS2CPacket);
    NetworkManager.registerReceiver(
        NetworkManager.Side.S2C,
        OpenTargetTagScreenPacket.ID,
        OpenTargetTagScreenPacket.CODEC,
        OpenTargetTagScreenPacket::receiveS2CPacket);
    NetworkManager.registerReceiver(
        NetworkManager.Side.S2C,
        OpenMaidManagerScreenPacket.ID,
        OpenMaidManagerScreenPacket.CODEC,
        OpenMaidManagerScreenPacket::receiveS2CPacket);
  }

  private void serverInit() {
    NetworkManager.registerReceiver(
        NetworkManager.Side.C2S,
        C2SSetMovingStatePacket.ID,
        C2SSetMovingStatePacket.CODEC,
        C2SSetMovingStatePacket::receive);
    NetworkManager.registerReceiver(
        NetworkManager.Side.C2S,
        SyncSoundConfigPacket.ID,
        SyncSoundConfigPacket.CODEC,
        SyncSoundConfigPacket::receiveC2SPacket);
    NetworkManager.registerReceiver(
        NetworkManager.Side.C2S,
        C2SSetBloodSuckPacket.ID,
        C2SSetBloodSuckPacket.CODEC,
        C2SSetBloodSuckPacket::receive);
    NetworkManager.registerReceiver(
        NetworkManager.Side.C2S,
        C2SSetWorkItemSlotSizePacket.ID,
        C2SSetWorkItemSlotSizePacket.CODEC,
        C2SSetWorkItemSlotSizePacket::receive);
    NetworkManager.registerReceiver(
        NetworkManager.Side.C2S,
        C2SSetTargetTagsPacket.ID,
        C2SSetTargetTagsPacket.CODEC,
        C2SSetTargetTagsPacket::receive);
    NetworkManager.registerReceiver(
        NetworkManager.Side.C2S,
        OpenTargetTagScreenPacket.ID,
        OpenTargetTagScreenPacket.CODEC,
        OpenTargetTagScreenPacket::receiveC2SPacket);
    NetworkManager.registerReceiver(
        NetworkManager.Side.C2S,
        OpenMaidManagerScreenPacket.ID,
        OpenMaidManagerScreenPacket.CODEC,
        OpenMaidManagerScreenPacket::receiveC2SPacket);
    NetworkManager.registerReceiver(
        NetworkManager.Side.C2S,
        C2SOpenInventoryPacket.ID,
        C2SOpenInventoryPacket.CODEC,
        C2SOpenInventoryPacket::receive);
    NetworkManager.registerReceiver(
        NetworkManager.Side.C2S,
        C2SCallWaitPacket.ID,
        C2SCallWaitPacket.CODEC,
        C2SCallWaitPacket::receive);
  }
}
