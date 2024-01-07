package net.sistr.littlemaidrebirth.entity.goal;


import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.sistr.littlemaidrebirth.entity.util.Tameable;

import java.util.EnumSet;

public class WaitGoal<T extends PathAwareEntity & Tameable> extends Goal {
    private final T mob;

    public WaitGoal(T mob) {
        this.mob = mob;
        setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        return mob.isWait()
                //主人が居るが、同じ世界に居ない場合
                || (mob.getTameOwnerUuid().isPresent()
                && !mob.getTameOwner().isPresent());
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
    }

}
