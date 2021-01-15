package net.sistr.lmrb.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.sistr.lmrb.LittleMaidReBirthMod;
import net.sistr.lmrb.client.IFFScreen;
import net.sistr.lmrb.entity.iff.HasIFF;
import net.sistr.lmrb.entity.iff.IFF;
import net.sistr.lmrb.entity.iff.IFFTypeManager;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OpenIFFScreenPacket {
    public static final Identifier ID =
            new Identifier(LittleMaidReBirthMod.MODID, "open_iff_screen");

    public static void sendS2CPacket(Entity entity, List<IFF> iffs, PlayerEntity player) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getEntityId());
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        tag.put("IFFs", list);
        iffs.forEach(iff -> list.add(iff.writeTag()));
        buf.writeCompoundTag(tag);
        ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, ID, buf);
    }

    public static void sendC2SPacket(Entity entity) {
        if (!(entity instanceof HasIFF)) {
            return;
        }
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getEntityId());
        ClientSidePacketRegistry.INSTANCE.sendToServer(ID, buf);
    }

    @Environment(EnvType.CLIENT)
    public static void receiveS2CPacket(PacketContext context, PacketByteBuf buf) {
        int id = buf.readVarInt();
        CompoundTag tag = buf.readCompoundTag();
        context.getTaskQueue().execute(() ->
                openIFFScreen(id, tag, context.getPlayer()));
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

    public static void receiveC2SPacket(PacketContext context, PacketByteBuf buf) {
        int id = buf.readVarInt();
        PlayerEntity player = context.getPlayer();
        context.getTaskQueue().execute(() ->
                openIFFScreen(id, player));
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
