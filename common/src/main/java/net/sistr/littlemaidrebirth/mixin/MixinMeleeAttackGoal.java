package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.sistr.littlemaidrebirth.util.MeleeAttackAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MeleeAttackGoal.class)
public class MixinMeleeAttackGoal implements MeleeAttackAccessor {

    @Shadow private int cooldown;

    @Override
    public void setCool_LM(int time) {
        this.cooldown = time;
    }
}
