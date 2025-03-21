package net.sistr.littlemaidrebirth.entity.goal;

import com.google.common.collect.Lists;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;


public class LMCollectSalaryFromContainerGoal<T extends LittleMaidEntity> extends CollectItemFromContainerGoal<T> {
    @Nullable
    protected BlockPos prevWaitPos;
    protected int moveToPrevWaitPosTime;

    public LMCollectSalaryFromContainerGoal(T mob) {
        super(mob);
    }

    @Override
    public boolean canStart() {
        return this.mob.itemContractable.hasSalaryBoxPositions()
                && TameableUtil.hasTameOwner(this.mob)
                && super.canStart();
    }

    @Override
    public void start() {
        super.start();
        if (TameableUtil.isWait(mob)) {
            prevWaitPos = this.mob.getBlockPos();
            moveToPrevWaitPosTime = 0;
        }
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue() || prevWaitPos != null;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.targetContainerPos == null && prevWaitPos != null) {
            if (prevWaitPos.equals(this.mob.getBlockPos())
            || moveToPrevWaitPosTime++ > getConfigMaxMoveTimePrevPos()) {
                prevWaitPos = null;
                return;
            }

            var nav = this.mob.getNavigation();
            var path = nav.findPathTo(prevWaitPos, 0);
            if (path != null) {
                nav.startMovingAlong(path, 1);
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        this.prevWaitPos = null;
        moveToPrevWaitPosTime = 0;
    }

    @Override
    protected ItemStack transfer(ItemStack stack) {
        return HopperBlockEntity.transfer(null, this.mob.getInventory(), stack, null);
    }

    @Override
    protected boolean isTargetItem(ItemStack stack) {
        return this.mob.itemContractable.isSalary(stack);
    }

    @Override
    protected void postCollect() {
        mob.swingHand(Hand.MAIN_HAND);
        mob.playSound(SoundEvents.ENTITY_ITEM_PICKUP,
                1.0F, mob.getRandom().nextFloat() * 0.1F + 1.0F);
    }

    @Override
    protected boolean shouldCollect() {
        int salarySlots = this.mob.itemContractable.checkSalarySlots();
        return salarySlots <= getConfigMinSalarySlots();
    }

    @Override
    protected boolean canCollectState() {
        boolean hasEmptySlot = false;
        int salarySlots = this.mob.itemContractable.checkSalarySlots();
        var inv = this.mob.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            var stack = inv.getStack(i);
            if (stack.isEmpty()) {
                hasEmptySlot = true;
                break;
            }
        }
        return hasEmptySlot && salarySlots < getConfigMaxSalarySlots();
    }

    @Override
    protected Optional<BlockPos> searchContainerPos() {
        var salaryBoxList = this.mob.itemContractable.getSalaryBoxPositions();

        if (salaryBoxList.isEmpty()) {
            return Optional.empty();
        }

        List<BlockPos> newSalaryBoxList = Lists.newArrayList();

        BlockPos result = null;
        Path resultPath = null;
        int minDistance = Integer.MAX_VALUE;
        for (BlockPos pos : salaryBoxList) {
            if (getAvailableContainer(pos).isEmpty()) {
                continue;
            }

            var distance = (int) pos.getSquaredDistance(this.mob.getPos());

            var sq = getConfigSalaryBoxRange() * getConfigSalaryBoxRange();
            if (distance > sq) {
                continue;
            }

            var nav = this.mob.getNavigation();
            var path = nav.findPathTo(pos, 1);
            if (path == null
                    || path.getEnd() == null
                    || !this.isInCollectRange(pos, BlockPos.ofFloored(path.getEnd().getPos()))) {
                continue;
            }

            newSalaryBoxList.add(pos);

            if (distance < minDistance) {
                minDistance = distance;
                result = pos;
                resultPath = path;
            }
        }
        this.mob.itemContractable.setSalaryBoxPositions(newSalaryBoxList);
        if (resultPath != null) {
            this.toContainerPath = resultPath;
            return Optional.of(result);
        }
        return Optional.empty();
    }

    protected int getConfigMaxSalarySlots() {
        return LMRBMod.getConfig().contract.maxAutoSalaryReceiptSlotSize;
    }

    protected int getConfigMinSalarySlots() {
        return LMRBMod.getConfig().contract.startAutoSalaryReceiptSlotThreshold;
    }

    protected float getConfigSalaryBoxRange() {
        return LMRBMod.getConfig().contract.searchSalaryBoxDistance;
    }

    protected int getConfigMaxMoveTimePrevPos() {
        return LMRBMod.getConfig().contract.maxMoveTimeAfterAutoSalaryReceipt;
    }

}
