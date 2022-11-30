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

    //todo インベントリ系、トレーサー
    @Override
    public Object getCapsValue(int pIndex, Object... pArg) {
        switch (pIndex) {
            case caps_aimedBow:
                return maid.isAimingBow();
            case caps_isLookSuger:
                return maid.isBegging();
            case caps_interestedAngle:
                return maid.getInterestedAngle((Float) pArg[0]);
            case caps_isFreedom:
                return maid.getMovingMode() == MovingMode.FREEDOM || maid.getMovingMode() == MovingMode.TRACER;
            case caps_isContract:
                return maid.isContractMM();
            case caps_isClock:
                return maid.getMainHandStack().getItem() == Items.CLOCK
                        || maid.getOffHandStack().getItem() == Items.CLOCK;
            case caps_job:
                return maid.getMode()
                        .map(Mode::getName)
                        .map(String::toLowerCase)
                        .orElse(null);
            case caps_isLeeding:
                return maid.isLeashed();
        }
        return super.getCapsValue(pIndex, pArg);
    }
}
