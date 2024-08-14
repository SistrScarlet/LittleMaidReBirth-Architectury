package net.sistr.littlemaidrebirth.entity.goal;


import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.TameableEntity;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

import java.util.EnumSet;

public class WaitGoal<T extends TameableEntity> extends Goal {
    private final T mob;

    public WaitGoal(T mob) {
        this.mob = mob;
        setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        return TameableUtil.isWait(mob)
                //主人が居るが、同じ世界に居ない場合
                || (TameableUtil.getTameOwnerUuid(mob).isPresent()
                && TameableUtil.getTameOwner(mob).isEmpty());
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
    }

}
