package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

import java.util.UUID;

public class SpawnLittleMaidPacket {
    public static final Identifier ID = Identifier.of(LMRBMod.MODID, "spawn_littlemaid");
/*
    @SuppressWarnings({"unchecked", "UnstableApiUsage", "deprecation"})
    public static Packet<ClientPlayPacketListener> create(LittleMaidEntity maid) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(maid.getId());
        buf.writeUuid(maid.getUuid());
        buf.writeRegistryValue(Registries.ENTITY_TYPE, maid.getType());
        buf.writeDouble(maid.getX());
        buf.writeDouble(maid.getY());
        buf.writeDouble(maid.getZ());
        buf.writeFloat(maid.getPitch());
        buf.writeFloat(maid.getYaw());
        buf.writeFloat(maid.getHeadYaw());
        var velocity = maid.getVelocity();
        buf.writeDouble(velocity.getX());
        buf.writeDouble(velocity.getY());
        buf.writeDouble(velocity.getZ());
        maid.saveAdditionalSpawnData(buf);
        return (Packet<ClientPlayPacketListener>) NetworkManager.toPacket(NetworkManager.Side.S2C, ID, buf);
    }

    @Environment(EnvType.CLIENT)
    public static void receiveS2CPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        var id = buf.readVarInt();
        var uuid = buf.readUuid();
        var entityType = buf.readRegistryValue(Registries.ENTITY_TYPE);
        var x = buf.readDouble();
        var y = buf.readDouble();
        var z = buf.readDouble();
        var pitch = buf.readFloat();
        var yaw = buf.readFloat();
        var headYaw = buf.readFloat();
        var velocityX = buf.readDouble();
        var velocityY = buf.readDouble();
        var velocityZ = buf.readDouble();

        buf.retain();

        var client = MinecraftClient.getInstance();
        //こちらだと実行が遅れ、最初の同期パケットより後にスポーンしてしまう
        //context.queue(() -> acceptS2C(client, id, uuid, entityType, x, y, z, pitch, yaw, headYaw, velocityX, velocityY, velocityZ));
        if (!client.isOnThread()) {
            client.executeSync(() -> {
                acceptS2C(client, id, uuid, entityType, x, y, z, pitch, yaw, headYaw, velocityX, velocityY, velocityZ, buf);
            });
        } else {
            acceptS2C(client, id, uuid, entityType, x, y, z, pitch, yaw, headYaw, velocityX, velocityY, velocityZ, buf);
        }
    }

    public static void acceptS2C(MinecraftClient client, int id, UUID uuid, EntityType<?> entityType,
                                 double x, double y, double z, float pitch, float yaw, float headYaw,
                                 double velocityX, double velocityY, double velocityZ, PacketByteBuf buf) {
        var player = client.player;
        if (player == null) return;
        var world = player.getWorld();
        assert entityType != null;
        var entity = entityType.create(world);
        if (entity instanceof LittleMaidEntity maid) {
            maid.updateTrackedPosition(x, y, z);
            maid.bodyYaw = headYaw;
            maid.headYaw = headYaw;
            maid.prevBodyYaw = maid.bodyYaw;
            maid.prevHeadYaw = maid.headYaw;
            maid.setId(id);
            maid.setUuid(uuid);
            maid.updatePositionAndAngles(x, y, z, yaw, pitch);
            maid.setVelocity(velocityX, velocityY, velocityZ);
            maid.loadAdditionalSpawnData(buf);
            buf.release();

            ((ClientWorld) world).addEntity(maid);
        }
    }*/

}
