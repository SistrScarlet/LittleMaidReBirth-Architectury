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
        return switch (pIndex) {
            case caps_aimedBow -> maid.isAimingBow();
            case caps_isLookSuger -> maid.isBegging();
            case caps_interestedAngle -> maid.getInterestedAngle((Float) pArg[0]);
            case caps_isFreedom -> maid.getMovingMode() == MovingMode.FREEDOM;
            case caps_isTracer -> maid.getMovingMode() == MovingMode.TRACER;
            case caps_isContract -> maid.isContractMM();
            case caps_isClock -> maid.getMainHandStack().getItem() == Items.CLOCK
                    || maid.getOffHandStack().getItem() == Items.CLOCK;
            case caps_job -> maid.getMode()
                    .map(Mode::getName)
                    .map(String::toLowerCase)
                    .orElse(null);
            case caps_isLeeding -> maid.isLeashed();
            default -> super.getCapsValue(pIndex, pArg);
        };
    }
}
