package net.sistr.littlemaidrebirth.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.client.screen.MaidManagerScreen;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManager;
import net.sistr.littlemaidrebirth.entity.util.MaidManager;
import net.sistr.littlemaidrebirth.entity.util.MaidManagerImpl;

public class OpenMaidManagerScreenPacket {
    public static final Identifier ID = Identifier.of(LMRBMod.MODID, "open_maid_manager_screen");

    public static void sendS2CPacket(PlayerEntity player) {
        PacketByteBuf buf = createS2CPacket(player);
        NetworkManager.sendToPlayer((ServerPlayerEntity) player, ID, buf);
    }

    public static PacketByteBuf createS2CPacket(PlayerEntity player) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        var nbt = new NbtCompound();
        var lmInfos = ((MaidManager) player).getMaidList();
        MaidManagerImpl.write(nbt, lmInfos);
        buf.writeNbt(nbt);
        return buf;
    }

    @Environment(EnvType.CLIENT)
    public static void sendC2SPacket() {
        PacketByteBuf buf = createC2SPacket();
        NetworkManager.sendToServer(ID, buf);
    }

    public static PacketByteBuf createC2SPacket() {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        return buf;
    }

    @Environment(EnvType.CLIENT)
    public static void receiveS2CPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null) return;
        var nbt = buf.readNbt();
        var lmInfos = new ArrayList<MaidManager.LMInfo>();
        MaidManagerImpl.read(nbt, lmInfos);
        context.queue(() -> openScreen(player, lmInfos));
    }

    @Environment(EnvType.CLIENT)
    private static void openScreen(PlayerEntity player, List<MaidManager.LMInfo> lmInfos) {
        MinecraftClient.getInstance().setScreen(new MaidManagerScreen(lmInfos));
    }

    private static <T extends Entity & TargetTagManager> void openScreen(PlayerEntity player) {
        sendS2CPacket(player);
    }

    public static void receiveC2SPacket(PacketByteBuf buf, NetworkManager.PacketContext context) {
        context.queue(() -> openScreen(context.getPlayer()));
    }
}
