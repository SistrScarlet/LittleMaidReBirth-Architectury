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
import net.sistr.littlemaidrebirth.entity.util.MovingMode;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

/** C2Sで移動状態をセットするパケット */
public record C2SSetMovingStatePacket(int entityId, MovingMode state) implements CustomPayload {
    public static final CustomPayload.Id<C2SSetMovingStatePacket> ID =
            new CustomPayload.Id<>(Identifier.of(LMRBMod.MODID, "set_moving_state"));

    public static final PacketCodec<RegistryByteBuf, C2SSetMovingStatePacket> CODEC =
            PacketCodec.of(
                    (packet, buf) -> {
                        buf.writeVarInt(packet.entityId());
                        buf.writeEnumConstant(packet.state());
                    },
                    buf ->
                            new C2SSetMovingStatePacket(
                                    buf.readVarInt(), buf.readEnumConstant(MovingMode.class)));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, MovingMode state) {
        NetworkManager.sendToServer(new C2SSetMovingStatePacket(entity.getId(), state));
    }

    public static void receive(
            C2SSetMovingStatePacket payload, NetworkManager.PacketContext context) {
        context.queue(
                () ->
                        applyMovingStateServer(
                                context.getPlayer(), payload.entityId(), payload.state()));
    }

    private static void applyMovingStateServer(PlayerEntity player, int id, MovingMode movingMode) {
        Entity entity = player.getWorld().getEntityById(id);
        if (!(entity instanceof LittleMaidEntity maid)
                || TameableUtil.getTameOwnerUuid(maid)
                        .filter(ownerId -> ownerId.equals(player.getUuid()))
                        .isEmpty()) {
            return;
        }
        if (maid.isStrike()) {
            return;
        }
        maid.setMovingMode(movingMode);
        maid.getNavigation().stop();
        if (movingMode == MovingMode.FREEDOM) {
            maid.setFreedomPos(entity.getBlockPos());
        }
    }
}
