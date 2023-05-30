package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;

import java.util.Optional;
import java.util.function.Predicate;

public class LMStoreItemToContainerGoal<T extends LittleMaidEntity> extends StoreItemToContainerGoal<T> {

    public LMStoreItemToContainerGoal(T mob, Predicate<ItemStack> exceptItems,
                                      int searchDistance) {
        super(mob, exceptItems, searchDistance);
    }

    @Override
    public boolean canStart() {
        return !this.mob.isStrike()
                && this.mob.hasTameOwner()
                && !this.mob.isWait()
                && (this.mob.getMovingMode() == MovingMode.FREEDOM
                || this.mob.getMovingMode() == MovingMode.TRACER)
                && super.canStart();
    }

    @Override
    public boolean shouldContinue() {
        return !this.mob.isWait()
                && this.mob.getMovingMode() == MovingMode.FREEDOM
                && super.shouldContinue();
    }

    @Override
    protected boolean isInventoryFull() {
        Inventory inventory = this.mob.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void storeItems() {
        if (containerPos == null) {
            return;
        }
        getInventory().ifPresent(chestInventory -> {
            Inventory inventory = this.mob.getInventory();
            this.mob.getWorld().playSound(null, containerPos,
                    SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS,
                    1.0f, 1.0f);
            this.mob.swingHand(Hand.MAIN_HAND);
            for (int i = 0; i < inventory.size(); i++) {
                var stack = inventory.getStack(i);
                if (this.exceptItems.test(stack)) {
                    continue;
                }
                var newStack = HopperBlockEntity.transfer(inventory, chestInventory, stack, Direction.UP);
                inventory.setStack(i, newStack);
            }
        });
    }

    protected Optional<Inventory> getInventory() {
        BlockState state = mob.getWorld().getBlockState(containerPos);
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            return Optional.ofNullable(ChestBlock.getInventory(chestBlock, state, mob.getWorld(), containerPos, false));
        }
        return Optional.empty();
    }
}
