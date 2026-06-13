package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.targeting.TargetIdentifier;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManager;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManagerImpl;
import net.sistr.littlemaidrebirth.entity.targeting.TargetingSystem;

public class C2SSetTargetTagsPacket {
    public static final Identifier ID = Identifier.of(LMRBMod.MODID, "set_target_tags");

    @Environment(EnvType.CLIENT)
    public static <T extends Entity & TargetTagManager> void sendC2SPacket(
            T entity, Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTags) {
        NbtCompound tag = new NbtCompound();
        TargetTagManagerImpl.write(targetTags, tag);

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getId());
        buf.writeNbt(tag);

        NetworkManager.sendToServer(ID, buf);
    }

    public static void receiveC2SPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        int id = buf.readVarInt();
        NbtCompound tag = buf.readNbt();
        context.queue(() -> applyServer(context.getPlayer(), id, tag));
    }

    private static void applyServer(PlayerEntity player, int id, NbtCompound tag) {
        Entity entity = player.getWorld().getEntityById(id);
        if (!(entity instanceof TargetTagManager targetTagManager)) {
            return;
        }
        if (entity instanceof TameableEntity
                && !player.getUuid().equals(((TameableEntity) entity).getOwnerUuid())) {
            return;
        }
        targetTagManager.readTargetTags(tag);
    }
}
