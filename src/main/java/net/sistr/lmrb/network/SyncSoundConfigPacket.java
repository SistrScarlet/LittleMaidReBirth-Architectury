package net.sistr.lmrb.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.sistr.lmml.entity.compound.SoundPlayable;
import net.sistr.lmml.resource.manager.LMConfigManager;
import net.sistr.lmrb.LittleMaidReBirthMod;
import net.sistr.lmrb.entity.Tameable;

public class SyncSoundConfigPacket {
    public static final Identifier ID =
            new Identifier(LittleMaidReBirthMod.MODID, "sync_sound_config");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, String configName) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getEntityId());
        buf.writeString(configName);
        ClientPlayNetworking.send(ID, buf);
    }

    public static void sendS2CPacket(Entity entity, String configName) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getEntityId());
        buf.writeString(configName);
        PlayerLookup.tracking(entity).forEach(player -> ServerPlayNetworking.send(player, ID, buf));
    }

    @Environment(EnvType.CLIENT)
    public static void receiveS2CPacket(MinecraftClient client, ClientPlayNetworkHandler handler,
                                        PacketByteBuf buf, PacketSender responseSender) {
        int id = buf.readVarInt();
        String configName = buf.readString();
        client.execute(() -> applySoundConfigClient(id, configName));
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

    public static void receiveC2SPacket(MinecraftServer server, ServerPlayerEntity player,
                                        ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        int id = buf.readVarInt();
        String configName = buf.readString(32767);
        server.execute(() -> applySoundConfigServer(player, id, configName));
    }

    private static void applySoundConfigServer(PlayerEntity player, int id, String configName) {
        World world = player.world;
        Entity entity = world.getEntityById(id);
        if (!(entity instanceof SoundPlayable)) {
            return;
        }
        if (entity instanceof Tameable
                && !((Tameable) entity).getTameOwnerUuid()
                .filter(ownerId -> ownerId.equals(player.getUuid()))
                .isPresent()) {
            return;
        }
        LMConfigManager.INSTANCE.getConfig(configName)
                .ifPresent(((SoundPlayable) entity)::setConfigHolder);
        sendS2CPacket(entity, configName);
    }

}
