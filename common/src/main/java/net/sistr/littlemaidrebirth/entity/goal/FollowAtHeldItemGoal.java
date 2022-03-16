package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidrebirth.entity.Tameable;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

public class FollowAtHeldItemGoal extends TameableStareAtHeldItemGoal {
    protected int reCalcCool;

    public FollowAtHeldItemGoal(PathAwareEntity mob, Tameable tameable, boolean isTamed, Predicate<ItemStack> targetItem) {
        super(mob, tameable, isTamed, targetItem);
        setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public void tick() {
        super.tick();
        if (mob.squaredDistanceTo(stareAt) < 2 * 2) {
            mob.getNavigation().stop();
            return;
        }
        if (0 < reCalcCool--) {
            return;
        }
        reCalcCool = 10;
        mob.getNavigation().startMovingTo(stareAt, 1);
    }
}
