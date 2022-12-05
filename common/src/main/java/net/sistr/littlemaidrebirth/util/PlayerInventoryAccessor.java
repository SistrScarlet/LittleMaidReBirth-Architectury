package net.sistr.littlemaidrebirth.util;

import net.minecraft.entity.player.PlayerInventory;

/**
 * Mixin Accessor
 * */
public interface PlayerInventoryAccessor {
    PlayerInventory getPlayerInventory_LMRB();

    void setPlayerInventory_LMRB(PlayerInventory inventory);
}
