package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.ItemStack;

import java.util.EnumSet;
import java.util.function.Predicate;

public class FollowAtHeldItemGoal<T extends TameableEntity> extends TameableStareAtHeldItemGoal<T> {
    protected int reCalcCool;

    public FollowAtHeldItemGoal(T mob, boolean isTamed, Predicate<ItemStack> targetItem) {
        super(mob, isTamed, targetItem);
        setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public void tick() {
        super.tick();
        //todo config化
        if (mob.squaredDistanceTo(stareAt) < 1.5f * 1.5f) {
            mob.getNavigation().stop();
            return;
        }
        if (0 < reCalcCool--) {
            return;
        }
        reCalcCool = getTickCount(10);
        mob.getNavigation().startMovingTo(stareAt, 1);
    }
}
