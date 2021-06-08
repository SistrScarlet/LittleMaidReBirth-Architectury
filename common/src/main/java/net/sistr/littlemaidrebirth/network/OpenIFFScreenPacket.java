package net.sistr.littlemaidrebirth.network;

import io.netty.buffer.Unpooled;
import me.shedaniel.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LittleMaidReBirthMod;
import net.sistr.littlemaidrebirth.client.IFFScreen;
import net.sistr.littlemaidrebirth.entity.iff.HasIFF;
import net.sistr.littlemaidrebirth.entity.iff.IFF;
import net.sistr.littlemaidrebirth.entity.iff.IFFTypeManager;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OpenIFFScreenPacket {
    public static final Identifier ID =
            new Identifier(LittleMaidReBirthMod.MODID, "open_iff_screen");

    public static void sendS2CPacket(Entity entity, List<IFF> iffs, PlayerEntity player) {
        PacketByteBuf buf = createS2CPacket(entity, iffs, player);
        NetworkManager.sendToPlayer((ServerPlayerEntity) player, ID, buf);
    }

    public static PacketByteBuf createS2CPacket(Entity entity, List<IFF> iffs, PlayerEntity player) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getEntityId());
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        tag.put("IFFs", list);
        iffs.forEach(iff -> list.add(iff.writeTag()));
        buf.writeCompoundTag(tag);
        return buf;
    }

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity) {
        if (!(entity instanceof HasIFF)) {
            return;
        }
        PacketByteBuf buf = createC2SPacket(entity);
        NetworkManager.sendToServer(ID, buf);
    }

    public static PacketByteBuf createC2SPacket(Entity entity) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getEntityId());
        return buf;
    }

    @Environment(EnvType.CLIENT)
    public static void receiveS2CPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null) return;
        int id = buf.readVarInt();
        CompoundTag tag = buf.readCompoundTag();
        context.queue(() -> openIFFScreen(id, tag, player));
    }

    @Environment(EnvType.CLIENT)
    private static void openIFFScreen(int id, CompoundTag tag, PlayerEntity player) {
        Entity entity = player.world.getEntityById(id);
        if (!(entity instanceof HasIFF)) {
            return;
        }
        ListTag list = tag.getList("IFFs", 10);
        List<IFF> iffs = list.stream()
                .map(t -> (CompoundTag) t)
                .map(t -> IFFTypeManager.getINSTANCE().loadIFF(t))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        MinecraftClient.getInstance().openScreen(new IFFScreen(entity, iffs));
    }

    public static void receiveC2SPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        context.queue(() -> openIFFScreen(id, context.getPlayer()));
    }

    private static void openIFFScreen(int id, PlayerEntity player) {
        Entity entity = player.world.getEntityById(id);
        if (!(entity instanceof HasIFF)
                || (entity instanceof TameableEntity
                && !player.getUuid().equals(((TameableEntity) entity).getOwnerUuid()))) {
            return;
        }
        sendS2CPacket(entity, ((HasIFF) entity).getIFFs(), player);
    }

}
