package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.Item;
import net.sistr.littlemaidrebirth.entity.Tameable;

import java.util.EnumSet;
import java.util.Set;

public class FollowAtHeldItemGoal extends TameableStareAtHeldItemGoal {
    protected int reCalcCool;

    public FollowAtHeldItemGoal(PathAwareEntity mob, Tameable tameable, boolean isTamed, Set<Item> items) {
        super(mob, tameable, isTamed, items);
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
