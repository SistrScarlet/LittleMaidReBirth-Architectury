package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.iff.HasIFF;
import net.sistr.littlemaidrebirth.entity.iff.IFF;
import net.sistr.littlemaidrebirth.entity.iff.IFFTypeManager;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * C2SでIFFをセットするパケット
 */
public class C2SSetIFFPacket {
    public static final Identifier ID =
            new Identifier(LMRBMod.MODID, "set_iff");

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket(Entity entity, List<IFF> iffs) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getId());
        NbtCompound tag = new NbtCompound();
        NbtList list = new NbtList();
        tag.put("IFFs", list);
        iffs.forEach(iff -> list.add(iff.writeTag()));
        buf.writeNbt(tag);
        NetworkManager.sendToServer(ID, buf);
    }

    public static void receiveC2SPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        NbtCompound tag = buf.readNbt();
        context.queue(() -> applyIFFServer(context.getPlayer(), id, tag));
    }

    private static void applyIFFServer(PlayerEntity player, int id, NbtCompound tag) {
        Entity entity = player.getWorld().getEntityById(id);
        if (!(entity instanceof HasIFF)) {
            return;
        }
        if (entity instanceof TameableEntity && !player.getUuid().equals(((TameableEntity) entity).getOwnerUuid())) {
            return;
        }
        NbtList list = tag.getList("IFFs", 10);
        List<IFF> iffs = list.stream()
                .map(t -> (NbtCompound) t)
                .map(t -> IFFTypeManager.getINSTANCE().loadIFF(t))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        ((HasIFF) entity).setIFFs(iffs);
    }
}
