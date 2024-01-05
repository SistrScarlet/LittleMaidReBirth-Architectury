package net.sistr.littlemaidrebirth.entity.goal;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.sistr.littlemaidrebirth.util.BlockFinderPD;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Predicate;

public abstract class StoreItemToContainerGoal<T extends PathAwareEntity> extends Goal {
    protected final T mob;
    protected final Predicate<ItemStack> exceptItems;
    protected final int searchDistanceSq;
    @Nullable
    protected BlockPos containerPos;
    @Nullable
    protected BlockFinderPD blockFinder;
    protected int count;

    public StoreItemToContainerGoal(T mob, Predicate<ItemStack> exceptItems, int searchDistance) {
        this.mob = mob;
        this.exceptItems = exceptItems;
        this.searchDistanceSq = searchDistance * searchDistance;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (!isInventoryFull()) return false;

        if (blockFinder == null || blockFinder.isEnd() || count++ > 1000) {
            this.count = 0;
            blockFinder = new BlockFinderPD(ImmutableList.of(this.mob.getBlockPos().up()),
                    this::isContainer,
                    pos -> mob.getEntityWorld().isAir(pos)
                            && Math.abs(pos.getY() - mob.getY()) < 2
                            && pos.getSquaredDistance(this.mob.getBlockPos()) < searchDistanceSq,
                    searchDistanceSq * 8);
        }

        blockFinder.tick();

        containerPos = blockFinder.getResult().orElse(null);

        return containerPos != null;
    }

    @Override
    public boolean shouldContinue() {
        return false;
    }

    protected boolean isContainer(BlockPos pos) {
        BlockState state = mob.getEntityWorld().getBlockState(pos);
        return state.getBlock() instanceof ChestBlock
                || state.getBlock() instanceof BarrelBlock;
    }

    protected abstract boolean isInventoryFull();

    protected abstract void storeItems();

    @Override
    public void start() {
        storeItems();
    }

    @Override
    public void stop() {
        containerPos = null;
    }

}
