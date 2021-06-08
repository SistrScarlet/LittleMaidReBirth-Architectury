package net.sistr.littlemaidrebirth.network;

import io.netty.buffer.Unpooled;
import me.shedaniel.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LittleMaidReBirthMod;
import net.sistr.littlemaidrebirth.entity.iff.HasIFF;
import net.sistr.littlemaidrebirth.entity.iff.IFF;
import net.sistr.littlemaidrebirth.entity.iff.IFFTypeManager;

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
        NetworkManager.sendToServer(ID, buf);
    }

    public static void receiveC2SPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        CompoundTag tag = buf.readCompoundTag();
        context.queue(() -> applyIFFServer(context.getPlayer(), id, tag));
    }

    private static void applyIFFServer(PlayerEntity player, int id, CompoundTag tag) {
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
