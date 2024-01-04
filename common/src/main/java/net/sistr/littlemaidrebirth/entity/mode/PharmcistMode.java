package net.sistr.littlemaidrebirth.entity.mode;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.util.BlockFinder;

import java.util.Arrays;
import java.util.Optional;

//todo 実装はよ
public class PharmcistMode extends Mode {
    private final LittleMaidEntity mob;

    public PharmcistMode(ModeType<? extends PharmcistMode> modeType, String name, LittleMaidEntity mob) {
        super(modeType, name);
        this.mob = mob;
    }

    @Override
    public void startModeTask() {

    }

    @Override
    public boolean shouldExecute() {

        return true;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return false;
    }

    public Optional<BlockPos> findBrewingStandPos() {
        return BlockFinder.searchTargetBlock(mob.getBlockPos(), this::isNotUsedBrewingStand, this::canSeeThrough,
                        Arrays.asList(Direction.values()), 1000)
                .filter(pos -> pos.getManhattanDistance(mob.getBlockPos()) < 8);
    }

    public boolean isNotUsedBrewingStand(BlockPos pos) {
        return getBrewingStand(pos)
                .filter(this::isNotUsedBrewingStand)
                .isPresent();
    }

    public Optional<BrewingStandBlockEntity> getBrewingStand(BlockPos pos) {
        if (pos == null) {
            return Optional.empty();
        }
        BlockEntity tile = mob.getWorld().getBlockEntity(pos);
        if (tile instanceof BrewingStandBlockEntity) {
            return Optional.of((BrewingStandBlockEntity) tile);
        }
        return Optional.empty();
    }

    public boolean isNotUsedBrewingStand(BrewingStandBlockEntity tile) {
        for (int slot : tile.getAvailableSlots(Direction.UP)) {
            ItemStack stack = tile.getStack(slot);
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean canUseBrewingStand(BrewingStandBlockEntity tile) {
        for (int slot : tile.getAvailableSlots(Direction.UP)) {
            ItemStack stack = tile.getStack(slot);
            if (!stack.isEmpty()) continue;

        }
        return false;
    }

    public boolean canSeeThrough(BlockPos pos) {
        return true;//!mob.world.getBlockState(pos).isSolidBlock(mob.world, pos);
    }

}
