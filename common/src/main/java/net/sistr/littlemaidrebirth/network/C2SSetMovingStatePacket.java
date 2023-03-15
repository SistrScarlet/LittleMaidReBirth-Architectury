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
import net.sistr.littlemaidrebirth.entity.util.MovingMode;

/**
 * C2Sで移動状態をセットするパケット
 */
public class C2SSetMovingStatePacket {
    public static final Identifier ID =
            new Identifier(LMRBMod.MODID, "set_moving_state");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, MovingMode state) {
        PacketByteBuf buf = createC2SPacket(entity, state);
        NetworkManager.sendToServer(ID, buf);
    }

    public static PacketByteBuf createC2SPacket(Entity entity, MovingMode state) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getId());
        buf.writeEnumConstant(state);
        return buf;
    }

    public static void receiveC2SPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        MovingMode movingMode = buf.readEnumConstant(MovingMode.class);
        context.queue(() -> applyMovingStateServer(context.getPlayer(), id, movingMode));
    }

    private static void applyMovingStateServer(PlayerEntity player, int id, MovingMode movingMode) {
        Entity entity = player.world.getEntityById(id);
        if (!(entity instanceof LittleMaidEntity)
                || ((LittleMaidEntity) entity).getTameOwnerUuid()
                .filter(ownerId -> ownerId.equals(player.getUuid()))
                .isEmpty()) {
            return;
        }
        ((LittleMaidEntity) entity).setMovingMode(movingMode);
        if (movingMode == MovingMode.FREEDOM) {
            ((LittleMaidEntity) entity).setFreedomPos(entity.getBlockPos());
        }
    }

}
