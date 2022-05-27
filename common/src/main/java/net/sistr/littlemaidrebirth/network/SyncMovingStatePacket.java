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
import net.sistr.littlemaidrebirth.entity.Tameable;

public class SyncMovingStatePacket {
    public static final Identifier ID =
            new Identifier(LMRBMod.MODID, "sync_moving_state");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, Tameable.MovingState state) {
        PacketByteBuf buf = createC2SPacket(entity, state);
        NetworkManager.sendToServer(ID, buf);
    }

    public static PacketByteBuf createC2SPacket(Entity entity, Tameable.MovingState state) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getId());
        buf.writeEnumConstant(state);
        return buf;
    }

    public static void receiveC2SPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        Tameable.MovingState state = buf.readEnumConstant(Tameable.MovingState.class);
        context.queue(() -> applyMovingStateServer(context.getPlayer(), id, state));
    }

    private static void applyMovingStateServer(PlayerEntity player, int id, Tameable.MovingState state) {
        Entity entity = player.world.getEntityById(id);
        if (!(entity instanceof Tameable)
                || ((Tameable) entity).getTameOwnerUuid()
                .filter(ownerId -> ownerId.equals(player.getUuid()))
                .isEmpty()) {
            return;
        }
        ((Tameable) entity).setMovingState(state);
        if (state == Tameable.MovingState.FREEDOM) {
            ((Tameable) entity).setFreedomPos(entity.getBlockPos());
        }
    }

}
