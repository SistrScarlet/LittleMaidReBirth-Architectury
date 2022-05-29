package net.sistr.littlemaidrebirth.entity;

import me.shedaniel.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

public class LMScreenHandlerFactory implements ExtendedMenuProvider {
    private final LittleMaidEntity maid;

    public LMScreenHandlerFactory(LittleMaidEntity maid) {
        this.maid = maid;
    }

    @Override
    public void saveExtraData(PacketByteBuf buf) {
        buf.writeVarInt(maid.getEntityId());
        buf.writeByte(maid.getUnpaidDays());
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new LittleMaidScreenHandler(syncId, inv, maid.getEntityId(), maid.getUnpaidDays());
    }

    @Override
    public Text getDisplayName() {
        return maid.getDisplayName();
    }

}
