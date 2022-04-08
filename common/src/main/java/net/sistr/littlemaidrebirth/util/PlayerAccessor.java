package net.sistr.littlemaidrebirth.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;

public interface PlayerAccessor {

    void onCollideWithEntity_LM(Entity entity);

    void setInventory(PlayerInventory inventory);

}
