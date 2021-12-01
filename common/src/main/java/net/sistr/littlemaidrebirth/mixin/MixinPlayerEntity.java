package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.stat.Stat;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.sistr.littlemaidrebirth.util.PlayerAccessor;
import net.sistr.littlemaidrebirth.util.PlayerInventoryAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity implements PlayerAccessor, PlayerInventoryAccessor {

    @Shadow protected abstract void collideWithEntity(Entity entity);


    @Shadow public abstract ActionResult interact(Entity entity, Hand hand);

    @Mutable @Shadow @Final private PlayerInventory inventory;

    @Override
    public void onCollideWithEntity_LM(Entity entity) {
        collideWithEntity(entity);
    }

    @Override
    public PlayerInventory getPlayerInventory_LMRB() {
        return this.inventory;
    }

    @Override
    public void setPlayerInventory_LMRB(PlayerInventory inventory) {
        this.inventory = inventory;
    }
}
