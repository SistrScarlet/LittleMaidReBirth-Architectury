package net.sistr.lmrb.entity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class LMScreenHandlerFactory implements ExtendedScreenHandlerFactory {
    private final LittleMaidEntity maid;

    public LMScreenHandlerFactory(LittleMaidEntity maid) {
        this.maid = maid;
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeVarInt(maid.getEntityId());
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new LittleMaidScreenHandler(syncId, inv, maid.getEntityId());
    }

    @Override
    public Text getDisplayName() {
        return maid.getDisplayName();
    }
}
