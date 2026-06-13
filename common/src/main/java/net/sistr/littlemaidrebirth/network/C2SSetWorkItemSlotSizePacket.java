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

/** クライアントからサーバーへ仕事アイテムスロット数をセットするパケット */
public record C2SSetWorkItemSlotSizePacket(int entityId, int num) implements CustomPayload {
    public static final CustomPayload.Id<C2SSetWorkItemSlotSizePacket> ID =
            new CustomPayload.Id<>(Identifier.of(LMRBMod.MODID, "set_work_item_slot_size"));

    public static final PacketCodec<RegistryByteBuf, C2SSetWorkItemSlotSizePacket> CODEC =
            PacketCodec.of(
                    (packet, buf) -> {
                        buf.writeVarInt(packet.entityId());
                        buf.writeByte(packet.num());
                    },
                    buf ->
                            new C2SSetWorkItemSlotSizePacket(
                                    buf.readVarInt(), buf.readByte() & 255));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(LittleMaidEntity entity, int num) {
        NetworkManager.sendToServer(new C2SSetWorkItemSlotSizePacket(entity.getId(), num));
    }

    public static void receive(
            C2SSetWorkItemSlotSizePacket payload, NetworkManager.PacketContext context) {
        context.queue(() -> applyServer(context.getPlayer(), payload.entityId(), payload.num()));
    }

    private static void applyServer(PlayerEntity player, int id, int num) {
        Entity entity = player.getWorld().getEntityById(id);
        if (!(entity instanceof LittleMaidEntity maid)) {
            return;
        }
        // ご主人がいて、送信元のプレイヤーがご主人なら
        if (TameableUtil.getTameOwnerUuid(maid)
                .filter(uuid -> player.getUuid().equals(uuid))
                .isPresent()) {
            maid.setWorkItemSlotNum(num);
        }
    }
}
