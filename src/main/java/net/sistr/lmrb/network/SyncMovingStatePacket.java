package net.sistr.lmrb.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.sistr.lmrb.LittleMaidReBirthMod;
import net.sistr.lmrb.entity.Tameable;

public class SyncMovingStatePacket {
    public static final Identifier ID =
            new Identifier(LittleMaidReBirthMod.MODID, "sync_moving_state");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, Tameable.MovingState state) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getEntityId());
        buf.writeEnumConstant(state);
        ClientSidePacketRegistry.INSTANCE.sendToServer(ID, buf);
    }

    public static void receiveC2SPacket(PacketContext context, PacketByteBuf buf) {
        int id = buf.readVarInt();
        Tameable.MovingState state = buf.readEnumConstant(Tameable.MovingState.class);
        context.getTaskQueue().execute(() ->
                applyMovingStateServer(context.getPlayer(), id, state));
    }

    private static void applyMovingStateServer(PlayerEntity player, int id, Tameable.MovingState state) {
        Entity entity = player.world.getEntityById(id);
        if (!(entity instanceof Tameable)
                || !((Tameable) entity).getTameOwnerUuid()
                .filter(ownerId -> ownerId.equals(player.getUuid()))
                .isPresent()) {
            return;
        }
        ((Tameable) entity).setMovingState(state);
        if (state == Tameable.MovingState.FREEDOM) {
            ((Tameable) entity).setFreedomPos(entity.getBlockPos());
        }
    }

}
