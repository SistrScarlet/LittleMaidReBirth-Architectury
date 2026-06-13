package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

/** クライアントからサーバーへ仕事アイテムスロット数をセットするパケット */
public class C2SSetWorkItemSlotSizePacket {
    public static final Identifier ID = Identifier.of(LMRBMod.MODID, "set_work_item_slot_size");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(LittleMaidEntity entity, int num) {
        PacketByteBuf buf = createC2SPacket(entity, num);
        NetworkManager.sendToServer(ID, buf);
    }

    public static PacketByteBuf createC2SPacket(LittleMaidEntity entity, int num) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getId());
        buf.writeByte(num);
        return buf;
    }

    public static void receiveC2SPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        int num = buf.readByte() & 255;
        context.queue(() -> applyBloodSuckServer(context.getPlayer(), id, num));
    }

    private static void applyBloodSuckServer(PlayerEntity player, int id, int num) {
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
