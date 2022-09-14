package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.manager.LMConfigManager;
import net.sistr.littlemaidmodelloader.util.PlayerList;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.util.Tameable;

public class SyncSoundConfigPacket {
    public static final Identifier ID =
            new Identifier(LMRBMod.MODID, "sync_sound_config");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, String configName) {
        PacketByteBuf buf = createC2SPacket(entity, configName);
        NetworkManager.sendToServer(ID, buf);
    }

    public static PacketByteBuf createC2SPacket(Entity entity, String configName) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getId());
        buf.writeString(configName);
        return buf;
    }

    public static void sendS2CPacket(Entity entity, String configName) {
        PacketByteBuf buf = createS2CPacket(entity, configName);
        PlayerList.tracking(entity).forEach(player -> NetworkManager.sendToPlayer(player, ID, buf));
    }

    public static PacketByteBuf createS2CPacket(Entity entity, String configName) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getId());
        buf.writeString(configName);
        return buf;
    }

    @Environment(EnvType.CLIENT)
    public static void receiveS2CPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        String configName = buf.readString();
        context.queue(() -> applySoundConfigClient(id, configName));
    }

    @Environment(EnvType.CLIENT)
    private static void applySoundConfigClient(int id, String configName) {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        World world = player.world;
        Entity entity = world.getEntityById(id);
        if (entity instanceof SoundPlayable) {
            LMConfigManager.INSTANCE.getConfig(configName)
                    .ifPresent(((SoundPlayable) entity)::setConfigHolder);
        }
    }

    public static void receiveC2SPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        String configName = buf.readString(32767);
        context.queue(() -> applySoundConfigServer(context.getPlayer(), id, configName));
    }

    private static void applySoundConfigServer(PlayerEntity player, int id, String configName) {
        World world = player.world;
        Entity entity = world.getEntityById(id);
        if (!(entity instanceof SoundPlayable)) {
            return;
        }
        if (entity instanceof Tameable
                && ((Tameable) entity).getTameOwnerUuid()
                .filter(ownerId -> ownerId.equals(player.getUuid()))
                .isEmpty()) {
            return;
        }
        LMConfigManager.INSTANCE.getConfig(configName)
                .ifPresent(((SoundPlayable) entity)::setConfigHolder);
        sendS2CPacket(entity, configName);
    }

}
