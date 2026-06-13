package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

public record C2SCallWaitPacket(int entityId, State state) implements CustomPayload {
  public static final CustomPayload.Id<C2SCallWaitPacket> ID =
      new CustomPayload.Id<>(Identifier.of(LMRBMod.MODID, "call_wait"));

  public static final PacketCodec<RegistryByteBuf, C2SCallWaitPacket> CODEC =
      PacketCodec.of(
          (packet, buf) -> {
            buf.writeVarInt(packet.entityId());
            buf.writeEnumConstant(packet.state());
          },
          buf -> new C2SCallWaitPacket(buf.readVarInt(), buf.readEnumConstant(State.class)));

  @Override
  public CustomPayload.Id<? extends CustomPayload> getId() {
    return ID;
  }

  @Environment(EnvType.CLIENT)
  public static void sendC2SPacket(Entity entity, State state) {
    NetworkManager.sendToServer(new C2SCallWaitPacket(entity.getId(), state));
  }

  public static void receive(C2SCallWaitPacket payload, NetworkManager.PacketContext context) {
    context.queue(
        () -> applyMovingStateServer(context.getPlayer(), payload.entityId(), payload.state()));
  }

  private static void applyMovingStateServer(PlayerEntity player, int id, State state) {
    Entity entity = player.getWorld().getEntityById(id);
    if (!(entity instanceof LittleMaidEntity maid) || !TameableUtil.isTameOwner(maid, player)) {
      return;
    }
    if (maid.isStrike()) {
      return;
    }
    if (state == State.WAIT) {
      TameableUtil.setWait(maid, true);
    } else {
      TameableUtil.setWait(maid, false);
      maid.setMovingMode(MovingMode.ESCORT);
    }
  }

  public enum State {
    WAIT,
    CALL
  }
}
