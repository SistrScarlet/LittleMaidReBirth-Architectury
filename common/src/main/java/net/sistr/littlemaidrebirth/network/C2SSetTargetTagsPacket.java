package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.targeting.TargetIdentifier;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManager;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManagerImpl;
import net.sistr.littlemaidrebirth.entity.targeting.TargetingSystem;

public record C2SSetTargetTagsPacket(int entityId, NbtCompound tag) implements CustomPayload {
  public static final CustomPayload.Id<C2SSetTargetTagsPacket> ID =
      new CustomPayload.Id<>(Identifier.of(LMRBMod.MODID, "set_target_tags"));

  public static final PacketCodec<RegistryByteBuf, C2SSetTargetTagsPacket> CODEC =
      PacketCodec.of(
          (packet, buf) -> {
            buf.writeVarInt(packet.entityId());
            buf.writeNbt(packet.tag());
          },
          buf -> new C2SSetTargetTagsPacket(buf.readVarInt(), buf.readNbt()));

  @Override
  public CustomPayload.Id<? extends CustomPayload> getId() {
    return ID;
  }

  @Environment(EnvType.CLIENT)
  public static <T extends Entity & TargetTagManager> void sendC2SPacket(
      T entity, Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTags) {
    NbtCompound tag = new NbtCompound();
    TargetTagManagerImpl.write(targetTags, tag);
    NetworkManager.sendToServer(new C2SSetTargetTagsPacket(entity.getId(), tag));
  }

  public static void receive(C2SSetTargetTagsPacket payload, NetworkManager.PacketContext context) {
    context.queue(() -> applyServer(context.getPlayer(), payload.entityId(), payload.tag()));
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
