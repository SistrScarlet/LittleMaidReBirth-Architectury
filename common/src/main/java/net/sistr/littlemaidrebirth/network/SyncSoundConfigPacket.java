package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.manager.LMConfigManager;
import net.sistr.littlemaidmodelloader.util.PlayerList;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

/** サウンドコンフィグを同期するパケット */
public record SyncSoundConfigPacket(int entityId, String configName) implements CustomPayload {
  public static final CustomPayload.Id<SyncSoundConfigPacket> ID =
      new CustomPayload.Id<>(Identifier.of(LMRBMod.MODID, "sync_sound_config"));

  public static final PacketCodec<RegistryByteBuf, SyncSoundConfigPacket> CODEC =
      PacketCodec.of(
          (packet, buf) -> {
            buf.writeVarInt(packet.entityId());
            buf.writeString(packet.configName());
          },
          buf -> new SyncSoundConfigPacket(buf.readVarInt(), buf.readString()));

  @Override
  public CustomPayload.Id<? extends CustomPayload> getId() {
    return ID;
  }

  @Environment(EnvType.CLIENT)
  public static void sendC2SPacket(Entity entity, String configName) {
    NetworkManager.sendToServer(new SyncSoundConfigPacket(entity.getId(), configName));
  }

  public static void sendS2CPacket(Entity entity, String configName) {
    NetworkManager.sendToPlayers(
        PlayerList.tracking(entity), new SyncSoundConfigPacket(entity.getId(), configName));
  }

  @Environment(EnvType.CLIENT)
  public static void receiveS2CPacket(
      SyncSoundConfigPacket payload, NetworkManager.PacketContext context) {
    context.queue(() -> applySoundConfigClient(payload.entityId(), payload.configName()));
  }

  @Environment(EnvType.CLIENT)
  private static void applySoundConfigClient(int id, String configName) {
    PlayerEntity player = MinecraftClient.getInstance().player;
    if (player == null) return;
    World world = player.getWorld();
    Entity entity = world.getEntityById(id);
    if (entity instanceof SoundPlayable) {
      LMConfigManager.INSTANCE
          .getConfig(configName)
          .ifPresent(((SoundPlayable) entity)::setConfigHolder);
    }
  }

  public static void receiveC2SPacket(
      SyncSoundConfigPacket payload, NetworkManager.PacketContext context) {
    context.queue(
        () ->
            applySoundConfigServer(context.getPlayer(), payload.entityId(), payload.configName()));
  }

  private static void applySoundConfigServer(PlayerEntity player, int id, String configName) {
    World world = player.getWorld();
    Entity entity = world.getEntityById(id);
    if (!(entity instanceof SoundPlayable)) {
      return;
    }
    if (entity instanceof Tameable tameable
        && TameableUtil.getTameOwnerUuid(tameable)
            .filter(ownerId -> ownerId.equals(player.getUuid()))
            .isEmpty()) {
      return;
    }
    LMConfigManager.INSTANCE
        .getConfig(configName)
        .ifPresent(((SoundPlayable) entity)::setConfigHolder);
    sendS2CPacket(entity, configName);
  }
}
