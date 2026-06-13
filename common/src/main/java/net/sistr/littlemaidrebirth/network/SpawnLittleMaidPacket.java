package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.setup.Registration;

/**
 * メイドさんのスポーンパケット。
 *
 * <p>Architectury 標準の {@link dev.architectury.networking.SpawnEntityPacket} はクライアントで {@code
 * context.queue()} によりエンティティ生成を次 tick へ遅延させる。そのため再トラッキング時、同一バンドルで送られる DataTracker / 装備の follow-up
 * パケットが「まだ生成されていないエンティティ」に届いて破棄され、防具・移動モード・ 所有者などが同期されない（初回スポーンは生成後の dirty 更新で復旧するが、再トラッキングは
 * pairing バンドルが唯一の同期機会のため失われる）。
 *
 * <p>本パケットは生成を {@code executeSync} で即時実行し、follow-up が確実に着地するようにする。
 */
public record SpawnLittleMaidPacket(
        int id,
        UUID uuid,
        double x,
        double y,
        double z,
        float pitch,
        float yaw,
        float headYaw,
        double velocityX,
        double velocityY,
        double velocityZ,
        byte[] data)
        implements CustomPayload {

    public static final CustomPayload.Id<SpawnLittleMaidPacket> ID =
            new CustomPayload.Id<>(Identifier.of(LMRBMod.MODID, "spawn_littlemaid"));

    public static final PacketCodec<RegistryByteBuf, SpawnLittleMaidPacket> CODEC =
            PacketCodec.of(SpawnLittleMaidPacket::write, SpawnLittleMaidPacket::new);

    private SpawnLittleMaidPacket(RegistryByteBuf buf) {
        this(
                buf.readVarInt(),
                buf.readUuid(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readByteArray());
    }

    private void write(RegistryByteBuf buf) {
        buf.writeVarInt(id);
        buf.writeUuid(uuid);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(pitch);
        buf.writeFloat(yaw);
        buf.writeFloat(headYaw);
        buf.writeDouble(velocityX);
        buf.writeDouble(velocityY);
        buf.writeDouble(velocityZ);
        buf.writeByteArray(data);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    /** サーバ側でメイドさんのスポーンパケットを生成する。 */
    public static Packet<ClientPlayPacketListener> create(LittleMaidEntity maid) {
        PacketByteBuf extra = new PacketByteBuf(Unpooled.buffer());
        try {
            maid.saveAdditionalSpawnData(extra);
            byte[] data = new byte[extra.readableBytes()];
            extra.readBytes(data);
            var velocity = maid.getVelocity();
            SpawnLittleMaidPacket payload =
                    new SpawnLittleMaidPacket(
                            maid.getId(),
                            maid.getUuid(),
                            maid.getX(),
                            maid.getY(),
                            maid.getZ(),
                            maid.getPitch(),
                            maid.getYaw(),
                            maid.getHeadYaw(),
                            velocity.x,
                            velocity.y,
                            velocity.z,
                            data);
            @SuppressWarnings("unchecked")
            Packet<ClientPlayPacketListener> packet =
                    (Packet<ClientPlayPacketListener>)
                            NetworkManager.toPacket(
                                    NetworkManager.Side.S2C, payload, maid.getRegistryManager());
            return packet;
        } finally {
            extra.release();
        }
    }

    @Environment(EnvType.CLIENT)
    public static void receiveS2CPacket(
            SpawnLittleMaidPacket payload, NetworkManager.PacketContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        // Architectury 標準の context.queue() は生成を次 tick へ遅延させ follow-up を取りこぼすため、
        // executeSync で即時生成する（同一バンドルの DataTracker / 装備が確実に着地する）。
        if (client.isOnThread()) {
            spawn(client, payload);
        } else {
            client.executeSync(() -> spawn(client, payload));
        }
    }

    @Environment(EnvType.CLIENT)
    private static void spawn(MinecraftClient client, SpawnLittleMaidPacket payload) {
        ClientWorld world = client.world;
        if (world == null) {
            return;
        }
        LittleMaidEntity maid = Registration.LITTLE_MAID_MOB.get().create(world);
        if (maid == null) {
            return;
        }
        maid.setId(payload.id());
        maid.setUuid(payload.uuid());
        maid.updateTrackedPosition(payload.x(), payload.y(), payload.z());
        maid.updatePositionAndAngles(
                payload.x(), payload.y(), payload.z(), payload.yaw(), payload.pitch());
        maid.setHeadYaw(payload.headYaw());
        maid.setBodyYaw(payload.headYaw());
        maid.setVelocity(payload.velocityX(), payload.velocityY(), payload.velocityZ());
        PacketByteBuf extra = new PacketByteBuf(Unpooled.wrappedBuffer(payload.data()));
        try {
            maid.loadAdditionalSpawnData(extra);
        } finally {
            extra.release();
        }
        world.addEntity(maid);
    }
}
