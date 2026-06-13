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

/** クライアントからサーバーへBloodSuckを設定するパケット */
public record C2SSetBloodSuckPacket(int entityId, boolean isBloodSuck) implements CustomPayload {
    public static final CustomPayload.Id<C2SSetBloodSuckPacket> ID =
            new CustomPayload.Id<>(Identifier.of(LMRBMod.MODID, "set_blood_suck"));

    public static final PacketCodec<RegistryByteBuf, C2SSetBloodSuckPacket> CODEC =
            PacketCodec.of(
                    (packet, buf) -> {
                        buf.writeVarInt(packet.entityId());
                        buf.writeBoolean(packet.isBloodSuck());
                    },
                    buf -> new C2SSetBloodSuckPacket(buf.readVarInt(), buf.readBoolean()));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, boolean isBloodSuck) {
        NetworkManager.sendToServer(new C2SSetBloodSuckPacket(entity.getId(), isBloodSuck));
    }

    public static void receive(
            C2SSetBloodSuckPacket payload, NetworkManager.PacketContext context) {
        context.queue(
                () ->
                        applyBloodSuckServer(
                                context.getPlayer(), payload.entityId(), payload.isBloodSuck()));
    }

    private static void applyBloodSuckServer(PlayerEntity player, int id, boolean isBloodSuck) {
        Entity entity = player.getWorld().getEntityById(id);
        if (!(entity instanceof LittleMaidEntity maid)) {
            return;
        }
        // ご主人がいて、送信元のプレイヤーがご主人なら
        if (TameableUtil.getTameOwnerUuid(maid)
                .filter(uuid -> player.getUuid().equals(uuid))
                .isPresent()) {
            maid.setBloodSuck(isBloodSuck);
        }
    }
}
