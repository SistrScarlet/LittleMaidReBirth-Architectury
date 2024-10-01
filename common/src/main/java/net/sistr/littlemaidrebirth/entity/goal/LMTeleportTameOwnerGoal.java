package net.sistr.littlemaidrebirth.entity.goal;

import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;

public class LMTeleportTameOwnerGoal extends TeleportTameOwnerGoal<LittleMaidEntity> {
    protected final LittleMaidEntity maid;
    protected final boolean isEmergencyGoal;

    public LMTeleportTameOwnerGoal(LittleMaidEntity maid, float teleportStart, boolean isEmergencyGoal) {
        super(maid, teleportStart);
        this.maid = maid;
        this.isEmergencyGoal = isEmergencyGoal;
    }

    @Override
    public boolean canStart() {
        if (this.maid.isEmergency() != isEmergencyGoal) {
            return false;
        }
        if (this.tameable.getMovingMode() != MovingMode.ESCORT) {
            return false;
        }
        return super.canStart();
    }

    @Override
    public boolean shouldContinue() {
        if (this.maid.isEmergency() != isEmergencyGoal) {
            return false;
        }
        if (this.tameable.getMovingMode() != MovingMode.ESCORT) {
            return false;
        }
        return super.shouldContinue();
    }
}
