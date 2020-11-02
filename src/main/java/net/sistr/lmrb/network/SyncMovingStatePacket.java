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
    public static void sendC2SPacket(Entity entity, String state) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getEntityId());
        int num;
        switch (state) {
            case Tameable.ESCORT:
                num = 1;
                break;
            case Tameable.WAIT:
                num = 2;
                break;
            default:
                num = 0;
                break;
        }
        buf.writeByte(num);
        ClientSidePacketRegistry.INSTANCE.sendToServer(ID, buf);
    }

    public static void receiveC2SPacket(PacketContext context, PacketByteBuf buf) {
        int id = buf.readVarInt();
        int stateId = buf.readByte();
        String state;
        if (stateId <= 0) {
            state = Tameable.FREEDOM;
        } else if (stateId == 1) {
            state = Tameable.ESCORT;
        } else {
            state = Tameable.WAIT;
        }
        context.getTaskQueue().execute(() -> {
            PlayerEntity player = context.getPlayer();
            Entity entity = context.getPlayer().world.getEntityById(id);
            if (entity instanceof Tameable) {
                if (!((Tameable) entity).getTameOwnerUuid()
                        .filter(ownerId -> ownerId.equals(player.getUuid()))
                        .isPresent()) {
                    return;
                }
                ((Tameable) entity).setMovingState(state);
            }
        });
    }

}
