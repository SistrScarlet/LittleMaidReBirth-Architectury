package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.client.screen.TargetTagScreen;
import net.sistr.littlemaidrebirth.entity.targeting.TargetIdentifier;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManager;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManagerImpl;
import net.sistr.littlemaidrebirth.entity.targeting.TargetingSystem;

public class OpenTargetTagScreenPacket {
    public static final Identifier ID = Identifier.of(LMRBMod.MODID, "open_target_tag_screen");

    public static <T extends Entity & TargetTagManager> void sendS2CPacket(
            T entity, PlayerEntity player) {
        PacketByteBuf buf = createS2CPacket(entity, player);
        NetworkManager.sendToPlayer((ServerPlayerEntity) player, ID, buf);
    }

    public static <T extends Entity & TargetTagManager> PacketByteBuf createS2CPacket(
            T entity, PlayerEntity player) {
        NbtCompound nbt = new NbtCompound();
        entity.writeTargetTags(nbt);

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getId());
        buf.writeNbt(nbt);
        return buf;
    }

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity) {
        PacketByteBuf buf = createC2SPacket(entity);
        NetworkManager.sendToServer(ID, buf);
    }

    public static PacketByteBuf createC2SPacket(Entity entity) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getId());
        return buf;
    }

    @Environment(EnvType.CLIENT)
    public static void receiveS2CPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null) return;
        int id = buf.readVarInt();
        NbtCompound nbt = buf.readNbt();
        context.queue(() -> openScreen(id, nbt, player));
    }

    @Environment(EnvType.CLIENT)
    private static void openScreen(int id, NbtCompound nbt, PlayerEntity player) {
        Entity entity = player.getWorld().getEntityById(id);
        if (!(entity instanceof TargetTagManager)) {
            return;
        }
        Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTagMap = new HashMap<>();
        TargetTagManagerImpl.read(targetTagMap, nbt);

        MinecraftClient.getInstance().setScreen(new TargetTagScreen(entity, targetTagMap));
    }

    private static <T extends Entity & TargetTagManager> void openScreen(
            int id, PlayerEntity player) {
        Entity entity = player.getWorld().getEntityById(id);
        if (!(entity instanceof TargetTagManager)
                || (entity instanceof TameableEntity
                        && !player.getUuid().equals(((TameableEntity) entity).getOwnerUuid()))) {
            return;
        }
        //noinspection unchecked
        sendS2CPacket((T) entity, player);
    }

    public static void receiveC2SPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        context.queue(() -> openScreen(id, context.getPlayer()));
    }
}
