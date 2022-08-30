package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.sistr.littlemaidrebirth.util.BlockFinder;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Predicate;

public abstract class StoreItemToContainerGoal<T extends PathAwareEntity> extends Goal {
    protected final T mob;
    protected final Predicate<ItemStack> exceptItems;
    protected final int maxSearchCount;
    protected final int maxStartInterval;
    @Nullable
    protected BlockPos containerPos;

    public StoreItemToContainerGoal(T mob, Predicate<ItemStack> exceptItems, int maxStartInterval, int maxSearchCount) {
        this.mob = mob;
        this.exceptItems = exceptItems;
        this.maxStartInterval = maxStartInterval;
        this.maxSearchCount = maxSearchCount;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (this.mob.getRandom().nextInt(100) != 0) return false;
        if (!isInventoryFull()) return false;

        containerPos = searchContainer().orElse(null);

        return containerPos != null;
    }

    @Override
    public boolean shouldContinue() {
        return false;
    }

    protected Optional<BlockPos> searchContainer() {
        return BlockFinder.searchTargetBlock(
                this.mob.getBlockPos(),
                this::isContainer,
                pos -> !mob.world.isAir(pos),
                Arrays.asList(Direction.values()),
                this.maxSearchCount);
    }

    protected boolean isContainer(BlockPos pos) {
        BlockState state = mob.world.getBlockState(pos);
        return state.getBlock() instanceof ChestBlock;
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
