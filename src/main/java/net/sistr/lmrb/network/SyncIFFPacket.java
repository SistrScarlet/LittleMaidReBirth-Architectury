package net.sistr.lmrb.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sistr.lmrb.LittleMaidReBirthMod;
import net.sistr.lmrb.entity.iff.HasIFF;
import net.sistr.lmrb.entity.iff.IFF;
import net.sistr.lmrb.entity.iff.IFFTypeManager;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SyncIFFPacket {
    public static final Identifier ID =
            new Identifier(LittleMaidReBirthMod.MODID, "sync_iff");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, List<IFF> iffs) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getEntityId());
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        tag.put("IFFs", list);
        iffs.forEach(iff -> list.add(iff.writeTag()));
        buf.writeCompoundTag(tag);
        ClientPlayNetworking.send(ID, buf);
    }

    public static void receiveC2SPacket(MinecraftServer server, ServerPlayerEntity player,
                                        ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        int id = buf.readVarInt();
        CompoundTag tag = buf.readCompoundTag();
        server.execute(() -> applyIFFServer(id, tag, player));
    }

    private static void applyIFFServer(int id, CompoundTag tag, PlayerEntity player) {
        Entity entity = player.world.getEntityById(id);
        if (!(entity instanceof HasIFF)) {
            return;
        }
        if (entity instanceof TameableEntity && !player.getUuid().equals(((TameableEntity) entity).getOwnerUuid())) {
            return;
        }
        ListTag list = tag.getList("IFFs", 10);
        List<IFF> iffs = list.stream()
                .map(t -> (CompoundTag) t)
                .map(t -> IFFTypeManager.getINSTANCE().loadIFF(t))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        ((HasIFF) entity).setIFFs(iffs);
    }
}
