package net.sistr.littlemaidrebirth.entity;

import net.minecraft.item.Items;
import net.sistr.littlemaidmodelloader.maidmodel.EntityCaps;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;

public class LittleMaidModelCaps extends EntityCaps {
    private final LittleMaidEntity maid;

    public LittleMaidModelCaps(LittleMaidEntity maid) {
        super(maid);
        this.maid = maid;
    }

    //todo インベントリ系
    @Override
    public Object getCapsValue(int pIndex, Object... pArg) {
        if (pIndex == caps_entityIdFactor) {
            return maid.getIdFactor();
        } else if (pIndex == caps_aimedBow) {
            return maid.isAimingBow();
        } else if (pIndex == caps_isLookSuger) {
            return maid.isBegging();
        } else if (pIndex == caps_interestedAngle) {
            return maid.getInterestedAngle((Float) pArg[0]);
        } else if (pIndex == caps_isFreedom) {
            return maid.getMovingMode() == MovingMode.FREEDOM;
        } else if (pIndex == caps_isTracer) {
            return maid.getMovingMode() == MovingMode.TRACER;
        } else if (pIndex == caps_isContract) {
            return maid.isContractMM();
        } else if (pIndex == caps_isClock) {
            return maid.getMainHandStack().getItem() == Items.CLOCK
                    || maid.getOffHandStack().getItem() == Items.CLOCK;
        } else if (pIndex == caps_job) {
            return maid.getMode()
                    .map(Mode::getName)
                    .map(String::toLowerCase)
                    .orElse(null);
        } else if (pIndex == caps_isLeeding) {
            return maid.isLeashed();
        } else {
            return super.getCapsValue(pIndex, pArg);
        }
    }
}
