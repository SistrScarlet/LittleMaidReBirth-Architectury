package net.sistr.lmrb.entity.goal;


import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.sistr.lmrb.entity.Tameable;

import java.util.EnumSet;

public class WaitGoal extends Goal {
    private final PathAwareEntity mob;
    private final Tameable tameable;

    public WaitGoal(PathAwareEntity mob, Tameable tameable) {
        this.mob = mob;
        this.tameable = tameable;
        setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        return tameable.getMovingState() == Tameable.MovingState.WAIT;
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
    }

}
