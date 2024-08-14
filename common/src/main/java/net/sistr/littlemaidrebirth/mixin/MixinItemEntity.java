package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.util.LMCollidable;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(ItemEntity.class)
public abstract class MixinItemEntity extends Entity implements LMCollidable {

    @Shadow
    public abstract ItemStack getStack();

    @Shadow
    private @Nullable UUID owner;

    @Shadow
    public abstract boolean cannotPickup();

    public MixinItemEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public void onCollision_LMRB(LittleMaidEntity maid) {
        if (this.getWorld().isClient) {
            return;
        }

        if (this.cannotPickup()
                || (this.owner != null && !this.owner.equals(maid.getUuid()))) {
            return;
        }

        ItemStack stack = this.getStack();
        int prevCount = stack.getCount();

        stack = HopperBlockEntity.transfer(null, maid.getInventory(), stack, null);
        if (stack.getCount() != prevCount) {
            maid.sendPickup(this, prevCount);
            if (stack.isEmpty()) {
                this.discard();
                stack.setCount(prevCount);
            }
            maid.triggerItemPickedUpByEntityCriteria((ItemEntity) (Object) this);
        }
    }
}
