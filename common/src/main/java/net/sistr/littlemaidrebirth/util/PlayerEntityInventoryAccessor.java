package net.sistr.littlemaidrebirth.util;

import net.minecraft.entity.player.PlayerInventory;

/**
 * Mixin Accessor
 * */
public interface PlayerEntityInventoryAccessor {
    PlayerInventory getPlayerInventory_LMRB();

    void setPlayerInventory_LMRB(PlayerInventory inventory);
}
