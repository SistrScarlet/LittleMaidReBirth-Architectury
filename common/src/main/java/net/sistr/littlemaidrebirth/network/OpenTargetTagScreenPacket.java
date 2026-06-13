package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
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
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.client.screen.TargetTagScreen;
import net.sistr.littlemaidrebirth.entity.targeting.TargetIdentifier;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManager;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManagerImpl;
import net.sistr.littlemaidrebirth.entity.targeting.TargetingSystem;

public record OpenTargetTagScreenPacket(int entityId, NbtCompound nbt) implements CustomPayload {
  public static final CustomPayload.Id<OpenTargetTagScreenPacket> ID =
      new CustomPayload.Id<>(Identifier.of(LMRBMod.MODID, "open_target_tag_screen"));

  public static final PacketCodec<RegistryByteBuf, OpenTargetTagScreenPacket> CODEC =
      PacketCodec.of(
          (packet, buf) -> {
            buf.writeVarInt(packet.entityId());
            buf.writeNbt(packet.nbt());
          },
          buf -> new OpenTargetTagScreenPacket(buf.readVarInt(), buf.readNbt()));

  @Override
  public CustomPayload.Id<? extends CustomPayload> getId() {
    return ID;
  }

  public static <T extends Entity & TargetTagManager> void sendS2CPacket(
      T entity, PlayerEntity player) {
    NbtCompound nbt = new NbtCompound();
    entity.writeTargetTags(nbt);
    NetworkManager.sendToPlayer(
        (ServerPlayerEntity) player, new OpenTargetTagScreenPacket(entity.getId(), nbt));
  }

  @Environment(EnvType.CLIENT)
  public static void sendC2SPacket(Entity entity) {
    NetworkManager.sendToServer(new OpenTargetTagScreenPacket(entity.getId(), new NbtCompound()));
  }

  @Environment(EnvType.CLIENT)
  public static void receiveS2CPacket(
      OpenTargetTagScreenPacket payload, NetworkManager.PacketContext context) {
    PlayerEntity player = context.getPlayer();
    if (player == null) return;
    context.queue(() -> openScreen(payload.entityId(), payload.nbt(), player));
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

  @SuppressWarnings("unchecked")
  private static <T extends Entity & TargetTagManager> void openScreen(
      int id, PlayerEntity player) {
    Entity entity = player.getWorld().getEntityById(id);
    if (!(entity instanceof TargetTagManager)
        || (entity instanceof TameableEntity
            && !player.getUuid().equals(((TameableEntity) entity).getOwnerUuid()))) {
      return;
    }
    sendS2CPacket((T) entity, player);
  }

  public static void receiveC2SPacket(
      OpenTargetTagScreenPacket payload, NetworkManager.PacketContext context) {
    context.queue(() -> openScreen(payload.entityId(), context.getPlayer()));
  }
}
