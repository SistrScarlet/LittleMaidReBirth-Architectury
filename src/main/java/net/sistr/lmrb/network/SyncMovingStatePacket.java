package net.sistr.lmrb.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
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
        ClientPlayNetworking.send(ID, buf);
    }

    public static void receiveC2SPacket(MinecraftServer server, ServerPlayerEntity player,
                                        ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        int id = buf.readVarInt();
        Tameable.MovingState state = buf.readEnumConstant(Tameable.MovingState.class);
        server.execute(() -> applyMovingStateServer(player, id, state));
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
