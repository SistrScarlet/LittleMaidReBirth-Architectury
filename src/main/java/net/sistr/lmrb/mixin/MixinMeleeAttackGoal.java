package net.sistr.lmrb.mixin;

import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.sistr.lmrb.util.MeleeAttackAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MeleeAttackGoal.class)
public class MixinMeleeAttackGoal implements MeleeAttackAccessor {

    @Shadow private int field_24667;

    @Override
    public void setCool_LM(int time) {
        this.field_24667 = time;
    }
}
