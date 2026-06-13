package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.client.screen.MaidManagerScreen;
import net.sistr.littlemaidrebirth.entity.util.MaidManager;
import net.sistr.littlemaidrebirth.entity.util.MaidManagerImpl;

public record OpenMaidManagerScreenPacket(NbtCompound nbt) implements CustomPayload {
    public static final CustomPayload.Id<OpenMaidManagerScreenPacket> ID =
            new CustomPayload.Id<>(Identifier.of(LMRBMod.MODID, "open_maid_manager_screen"));

    public static final PacketCodec<RegistryByteBuf, OpenMaidManagerScreenPacket> CODEC =
            PacketCodec.of(
                    (packet, buf) -> buf.writeNbt(packet.nbt()),
                    buf -> new OpenMaidManagerScreenPacket(buf.readNbt()));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void sendS2CPacket(PlayerEntity player) {
        var nbt = new NbtCompound();
        var lmInfos = ((MaidManager) player).getMaidList();
        MaidManagerImpl.write(nbt, lmInfos);
        NetworkManager.sendToPlayer(
                (ServerPlayerEntity) player, new OpenMaidManagerScreenPacket(nbt));
    }

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket() {
        NetworkManager.sendToServer(new OpenMaidManagerScreenPacket(new NbtCompound()));
    }

    @Environment(EnvType.CLIENT)
    public static void receiveS2CPacket(
            OpenMaidManagerScreenPacket payload, NetworkManager.PacketContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null) return;
        var lmInfos = new ArrayList<MaidManager.LMInfo>();
        MaidManagerImpl.read(payload.nbt(), lmInfos);
        context.queue(() -> openScreen(player, lmInfos));
    }

    @Environment(EnvType.CLIENT)
    private static void openScreen(PlayerEntity player, List<MaidManager.LMInfo> lmInfos) {
        MinecraftClient.getInstance().setScreen(new MaidManagerScreen(lmInfos));
    }

    public static void receiveC2SPacket(
            OpenMaidManagerScreenPacket payload, NetworkManager.PacketContext context) {
        context.queue(() -> sendS2CPacket(context.getPlayer()));
    }
}
