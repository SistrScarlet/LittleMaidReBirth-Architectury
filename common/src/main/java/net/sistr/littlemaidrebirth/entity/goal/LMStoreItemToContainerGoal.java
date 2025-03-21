package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class LMStoreItemToContainerGoal<T extends LittleMaidEntity> extends StoreItemToContainerGoal<T> {

    public LMStoreItemToContainerGoal(T mob, Predicate<ItemStack> exceptItems,
                                      Supplier<Float> searchRange) {
        super(mob, exceptItems, searchRange);
    }

    @Override
    public boolean canStart() {
        return !this.mob.isStrike()
                && TameableUtil.getTameOwnerUuid(mob).isPresent()
                && !TameableUtil.isWait(mob)
                && (this.mob.getMovingMode() == MovingMode.FREEDOM
                || this.mob.getMovingMode() == MovingMode.TRACER)
                && super.canStart();
    }

    @Override
    protected boolean hasStoreItems() {
        Inventory inventory = this.mob.getInventory();
        boolean hasStoreItem = false;
        for (int i = this.mob.getWorkItemSlotSize(); i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                return false;
            }
            // 仕舞うべきアイテムならフラグを立てる
            if (!hasStoreItem && !this.exceptItems.test(stack)) {
                hasStoreItem = true;
            }
        }
        // 仕舞うべきアイテムがあればtrue
        return hasStoreItem;
    }

    //todo チェストに仕舞うときの演出を強化する
    //todo チェストに仕舞わない条件を追加する
    @Override
    protected void storeItems() {
        if (containerPos == null) {
            return;
        }

        Inventory container = HopperBlockEntity.getInventoryAt(this.mob.getWorld(), containerPos);
        if (container == null) {
            return;
        }

        this.mob.getWorld().playSound(null, containerPos,
                SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS,
                1.0f, 1.0f);
        this.mob.swingHand(Hand.MAIN_HAND);

        var inventory = this.mob.getInventory();
        for (int i = this.mob.getWorkItemSlotSize(); i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            if (this.exceptItems.test(stack)) {
                continue;
            }
            var newStack = HopperBlockEntity.transfer(inventory, container, stack, Direction.UP);
            inventory.setStack(i, newStack);
        }
    }

}
