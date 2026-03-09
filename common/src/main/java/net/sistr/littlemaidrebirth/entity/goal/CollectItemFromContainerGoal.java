package net.sistr.littlemaidrebirth.entity.goal;

import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidrebirth.LMRBMod;
import org.jetbrains.annotations.Nullable;

/** コンテナからアイテムを回収するGaol */
public abstract class CollectItemFromContainerGoal<T extends MobEntity> extends Goal {
    protected final T mob;
    @Nullable protected BlockPos targetContainerPos;
    @Nullable protected Path toContainerPath;
    protected int pathReCalcCool;
    protected int moveToContainerTime;

    protected CollectItemFromContainerGoal(T mob) {
        this.mob = mob;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (this.mob.getRandom().nextFloat() > (1.0f / getConfigCheckInterval())) {
            return false;
        }

        if (shouldCollect() && canCollectState()) {
            this.targetContainerPos = searchContainerPos().orElse(null);
            return this.targetContainerPos != null;
        }

        return false;
    }

    @Override
    public boolean shouldContinue() {
        return this.targetContainerPos != null && canCollectState();
    }

    @Override
    public void start() {
        super.start();
        moveToContainerTime = 0;
    }

    @Override
    public void tick() {
        BlockPos cachedPos = targetContainerPos;
        if (cachedPos == null) {
            return;
        }

        if (!isContainerAvailable() || !canCollectState()) {
            targetContainerPos = null;
            return;
        }

        if (!isInCollectRange(cachedPos, this.mob.getBlockPos())) {
            if (moveToContainerTime++ > getConfigMaxMoveToContainerTime()) {
                this.targetContainerPos = null;
                return;
            }
            moveToContainer();
        } else {
            this.mob.getNavigation().stop();
            if (collect()) {
                postCollect();
                this.targetContainerPos = null;
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        this.targetContainerPos = null;
        this.toContainerPath = null;
        this.pathReCalcCool = 0;
    }

    protected void moveToContainer() {
        if (targetContainerPos == null) {
            return;
        }

        if (!isContainerAvailable()) {
            targetContainerPos = null;
            return;
        }

        // ナビゲーションでコンテナ位置に向かう

        var navigation = this.mob.getNavigation();

        if (this.toContainerPath == null || --this.pathReCalcCool <= 0) {
            this.pathReCalcCool = getTickCount(getConfigPathReCalcCool());

            // コンテナが利用できない状態、またはコンテナ位置に到達できない場合、ターゲット位置をnullにして終了
            this.toContainerPath = navigation.findPathTo(targetContainerPos, 1);
            if (toContainerPath == null
                    || toContainerPath.getEnd() == null
                    || !isInCollectRange(
                            targetContainerPos, toContainerPath.getEnd().getBlockPos())) {
                targetContainerPos = null;
                return;
            }

            navigation.startMovingAlong(this.toContainerPath, 1);
        }
    }

    protected boolean isInCollectRange(BlockPos containerPos, BlockPos mobPos) {
        return Math.abs(containerPos.getX() - mobPos.getX()) <= 1
                && Math.abs(containerPos.getZ() - mobPos.getZ()) <= 1
                && containerPos.getY() >= mobPos.getY() - 1
                && containerPos.getY() <= mobPos.getY() + MathHelper.ceil(mob.getHeight() - 1) + 2;
    }

    protected boolean collect() {
        if (targetContainerPos == null) {
            throw new IllegalStateException("Target container pos is null");
        }

        var optional = getAvailableContainer();
        if (optional.isEmpty()) {
            targetContainerPos = null;
            return false;
        }
        var inventory = optional.get();

        boolean collected = false;
        for (int i = 0; i < inventory.size(); i++) {
            if (!canCollectState()) {
                break;
            }

            var stack = inventory.getStack(i);
            if (!isTargetItem(stack)) continue;

            stack = transfer(stack);

            inventory.setStack(i, stack);
            collected = true;
        }

        return collected;
    }

    protected abstract ItemStack transfer(ItemStack stack);

    protected boolean isContainerAvailable() {
        return getAvailableContainer().isPresent();
    }

    protected Optional<Inventory> getAvailableContainer() {
        return getAvailableContainer(this.targetContainerPos);
    }

    protected Optional<Inventory> getAvailableContainer(BlockPos containerPos) {
        var world = mob.getEntityWorld();

        boolean touchingAir = false;
        for (Direction direction : Direction.values()) {
            if (world.isAir(containerPos.offset(direction))) {
                touchingAir = true;
                break;
            }
        }
        if (!touchingAir) {
            return Optional.empty();
        }

        var blockEntity = world.getBlockEntity(containerPos);
        if (blockEntity instanceof Inventory inventory) {
            for (int i = 0; i < inventory.size(); i++) {
                if (isTargetItem(inventory.getStack(i))) {
                    return Optional.of(inventory);
                }
            }
        }
        return Optional.empty();
    }

    protected abstract boolean isTargetItem(ItemStack stack);

    protected abstract boolean shouldCollect();

    protected abstract boolean canCollectState();

    protected abstract Optional<BlockPos> searchContainerPos();

    protected abstract void postCollect();

    protected int getConfigCheckInterval() {
        return LMRBMod.getConfig().contract.startIntervalOfAutoSalaryReceipt;
    }

    protected int getConfigPathReCalcCool() {
        return LMRBMod.getConfig().contract.findPathIntervalOfAutoSalaryReceipt;
    }

    protected int getConfigMaxMoveToContainerTime() {
        return LMRBMod.getConfig().contract.maxMoveTimeOnAutoSalaryReceipt;
    }
}
