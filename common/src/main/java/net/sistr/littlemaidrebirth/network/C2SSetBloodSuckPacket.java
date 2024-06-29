package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

/**
 * クライアントからサーバーへBloodSuckを設定するパケット
 */
public class C2SSetBloodSuckPacket {
    public static final Identifier ID =
            Identifier.of(LMRBMod.MODID, "set_blood_suck");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, boolean isBloodSuck,
                                     DynamicRegistryManager registryManager) {
        RegistryByteBuf buf = createC2SPacket(entity, isBloodSuck, registryManager);
        NetworkManager.sendToServer(ID, buf);
    }

    public static RegistryByteBuf createC2SPacket(Entity entity, boolean isBloodSuck,
                                                  DynamicRegistryManager registryManager) {
        RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), registryManager);
        buf.writeVarInt(entity.getId());
        buf.writeBoolean(isBloodSuck);
        return buf;
    }

    public static void receiveC2SPacket(RegistryByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        boolean isBloodSuck = buf.readBoolean();
        context.queue(() -> applyBloodSuckServer(context.getPlayer(), id, isBloodSuck));
    }

    private static void applyBloodSuckServer(PlayerEntity player, int id, boolean isBloodSuck) {
        Entity entity = player.getWorld().getEntityById(id);
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
