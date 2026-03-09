package net.sistr.littlemaidrebirth.entity.goal;

import java.util.EnumSet;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.TameableEntity;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

public class WaitGoal<T extends TameableEntity> extends Goal {
    private final T mob;

    public WaitGoal(T mob) {
        this.mob = mob;
        setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        return TameableUtil.isWait(mob);
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
    }
}
