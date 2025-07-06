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
import net.sistr.littlemaidrebirth.entity.util.TargetingSystem;

/**
 * クライアントからサーバーへBloodSuckを設定するパケット
 */
public class C2SSetMasterStancePacket {
    public static final Identifier ID =
            new Identifier(LMRBMod.MODID, "set_master_stance");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, TargetingSystem.MasterStance masterStance) {
        PacketByteBuf buf = createC2SPacket(entity, masterStance);
        NetworkManager.sendToServer(ID, buf);
    }

    public static PacketByteBuf createC2SPacket(Entity entity, TargetingSystem.MasterStance masterStance) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getId());
        buf.writeEnumConstant(masterStance);
        return buf;
    }

    public static void receiveC2SPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        var masterStance = buf.readEnumConstant(TargetingSystem.MasterStance.class);
        context.queue(() -> applyBloodSuckServer(context.getPlayer(), id, masterStance));
    }

    private static void applyBloodSuckServer(PlayerEntity player, int id, TargetingSystem.MasterStance masterStance) {
        Entity entity = player.getWorld().getEntityById(id);
        if (!(entity instanceof LittleMaidEntity maid)) {
            return;
        }
        //ご主人がいて、送信元のプレイヤーがご主人なら
        if (TameableUtil.getTameOwnerUuid(maid)
                .filter(uuid -> player.getUuid().equals(uuid))
                .isPresent()) {
            maid.setMasterStance(masterStance);
        }
    }
}
