package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.EnumSet;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class StareAtHeldItemGoal<T extends PathAwareEntity> extends Goal {
    protected final T mob;
    protected final Supplier<Float> stareAtRange;
    protected final Predicate<ItemStack> targetItem;
    protected PlayerEntity stareAt;

    public StareAtHeldItemGoal(T mob, Supplier<Float> stareAtRange, Predicate<ItemStack> targetItem) {
        this.mob = mob;
        this.stareAtRange = stareAtRange;
        this.targetItem = targetItem;
        setControls(EnumSet.of(Control.LOOK));
    }

    @Override
    public boolean canStart() {
        stareAt = mob.getWorld().getClosestPlayer(mob, stareAtRange.get());
        return stareAt != null && isHeldTargetItem(stareAt);
    }

    @Override
    public boolean shouldContinue() {
        return isHeldTargetItem(stareAt);
    }

    public boolean isHeldTargetItem(PlayerEntity player) {
        return targetItem.test(player.getMainHandStack()) || targetItem.test(player.getOffHandStack());
    }

    @Override
    public void tick() {
        mob.getLookControl().lookAt(stareAt, 30F, 30F);
    }
}
