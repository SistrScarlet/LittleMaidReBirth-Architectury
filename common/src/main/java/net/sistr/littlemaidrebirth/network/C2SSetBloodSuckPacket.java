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

/**
 * クライアントからサーバーへBloodSuckを設定するパケット
 */
public class C2SSetBloodSuckPacket {
    public static final Identifier ID =
            new Identifier(LMRBMod.MODID, "set_blood_suck");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, boolean isBloodSuck) {
        PacketByteBuf buf = createC2SPacket(entity, isBloodSuck);
        NetworkManager.sendToServer(ID, buf);
    }

    public static PacketByteBuf createC2SPacket(Entity entity, boolean isBloodSuck) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getId());
        buf.writeBoolean(isBloodSuck);
        return buf;
    }

    public static void receiveC2SPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        boolean isBloodSuck = buf.readBoolean();
        context.queue(() -> applyBloodSuckServer(context.getPlayer(), id, isBloodSuck));
    }

    private static void applyBloodSuckServer(PlayerEntity player, int id, boolean isBloodSuck) {
        Entity entity = player.world.getEntityById(id);
        if (!(entity instanceof LittleMaidEntity)) {
            return;
        }
        //ご主人がいて、送信元のプレイヤーがご主人なら
        if (((LittleMaidEntity) entity).getTameOwnerUuid()
                .filter(uuid -> player.getUuid().equals(uuid))
                .isPresent()) {
            ((LittleMaidEntity) entity).setBloodSuck(isBloodSuck);
        }
    }
}
