package net.sistr.littlemaidrebirth.entity.goal;

import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

public class LMTeleportTameOwnerGoal extends TeleportTameOwnerGoal<LittleMaidEntity> {
    protected final LittleMaidEntity maid;

    public LMTeleportTameOwnerGoal(LittleMaidEntity maid, float teleportStart) {
        super(maid, teleportStart);
        this.maid = maid;
    }

    @Override
    public boolean canStart() {
        if (this.tameable.getMovingMode() != MovingMode.ESCORT
                || TameableUtil.isWait(this.maid)) {
            return false;
        }
        return super.canStart();
    }

    @Override
    public boolean shouldContinue() {
        if (this.tameable.getMovingMode() != MovingMode.ESCORT
                || TameableUtil.isWait(this.maid)) {
            return false;
        }
        return super.shouldContinue();
    }
}
