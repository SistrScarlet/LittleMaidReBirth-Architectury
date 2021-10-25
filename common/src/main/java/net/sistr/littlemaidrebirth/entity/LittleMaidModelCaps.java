package net.sistr.littlemaidrebirth.entity;

import net.sistr.littlemaidmodelloader.maidmodel.EntityCaps;

public class LittleMaidModelCaps extends EntityCaps {
    private final LittleMaidEntity maid;

    public LittleMaidModelCaps(LittleMaidEntity maid) {
        super(maid);
        this.maid = maid;
    }

    //todo 色々追加
    @Override
    public Object getCapsValue(int pIndex, Object... pArg) {
        switch (pIndex) {
            case caps_aimedBow:
                return maid.isAimingBow();
            case caps_isLookSuger:
                return maid.isBegging();
            case caps_interestedAngle:
                return maid.getInterestedAngle((Float)pArg[0]);
        }
        return super.getCapsValue(pIndex, pArg);
    }
}
