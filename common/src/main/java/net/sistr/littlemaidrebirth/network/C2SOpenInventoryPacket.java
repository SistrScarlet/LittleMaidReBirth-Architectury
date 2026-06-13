package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

/** クライアントからサーバーへメイドさんのインベントリを開くパケット */
public record C2SOpenInventoryPacket(int entityId) implements CustomPayload {
  public static final CustomPayload.Id<C2SOpenInventoryPacket> ID =
      new CustomPayload.Id<>(Identifier.of(LMRBMod.MODID, "open_inventory"));

  public static final PacketCodec<RegistryByteBuf, C2SOpenInventoryPacket> CODEC =
      PacketCodec.of(
          (packet, buf) -> buf.writeVarInt(packet.entityId()),
          buf -> new C2SOpenInventoryPacket(buf.readVarInt()));

  @Override
  public CustomPayload.Id<? extends CustomPayload> getId() {
    return ID;
  }

  @Environment(EnvType.CLIENT)
  public static void sendC2SPacket(Entity entity) {
    NetworkManager.sendToServer(new C2SOpenInventoryPacket(entity.getId()));
  }

  public static void receive(C2SOpenInventoryPacket payload, NetworkManager.PacketContext context) {
    context.queue(() -> applyOpenInventoryServer(context.getPlayer(), payload.entityId()));
  }

  private static void applyOpenInventoryServer(PlayerEntity player, int id) {
    Entity entity = player.getWorld().getEntityById(id);
    if (!(entity instanceof LittleMaidEntity maid)) {
      return;
    }
    // ご主人がいて、送信元のプレイヤーがご主人なら
    if (TameableUtil.getTameOwnerUuid(maid)
        .filter(uuid -> player.getUuid().equals(uuid))
        .isPresent()) {
      maid.openInventory(player);
    }
  }
}
