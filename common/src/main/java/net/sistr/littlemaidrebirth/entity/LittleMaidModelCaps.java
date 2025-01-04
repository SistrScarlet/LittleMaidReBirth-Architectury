package net.sistr.littlemaidrebirth.entity;

import net.minecraft.block.PlantBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.sistr.littlemaidmodelloader.maidmodel.EntityCaps;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

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
            case caps_isLeeding -> maid.isLeashed();// MobEntityのメソッドなのでLMMLでなくこっち

            case caps_isBloodsuck -> maid.isBloodSuck();
            case caps_isFreedom -> maid.getMovingMode() == MovingMode.FREEDOM;
            case caps_isTracer -> maid.getMovingMode() == MovingMode.TRACER;
            case caps_isPlaying -> maid.isPlayingSnow();
            case caps_isLookSuger -> maid.isBegging();
            case caps_isWait -> TameableUtil.isWait(maid);
            case caps_isWorking -> maid.getMode().isPresent();
            case caps_isContract -> maid.isContractMM();
            case caps_isClock -> maid.getMainHandStack().getItem() == Items.CLOCK
                    || maid.getOffHandStack().getItem() == Items.CLOCK;
            case caps_isPlanter -> maid.getInventory().getStack(17).getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock() instanceof PlantBlock;
            case caps_isOverdrive -> maid.getAccelerationTicks() > 0;

            case caps_entityIdFactor -> maid.getIdFactor();

            case caps_interestedAngle -> maid.getInterestedAngle((Float) pArg[0]);

            case caps_job -> maid.getMode()
                    .map(Mode::getName)
                    .map(String::toLowerCase)
                    .orElse(null);
            default -> super.getCapsValue(pIndex, pArg);
        };
    }
}
