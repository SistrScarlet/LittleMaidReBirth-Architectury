package net.sistr.lmrb.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.sistr.lmrb.util.LivingAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity implements LivingAccessor {

    @Shadow protected abstract void method_30128();

    @Shadow protected abstract void tickActiveItemStack();

    @Shadow protected abstract boolean blockedByShield(DamageSource source);

    @Override
    public void applyEquipmentAttributes_LM() {
        method_30128();
    }

    @Override
    public void tickActiveItemStack_LM() {
        tickActiveItemStack();
    }

    @Override
    public boolean blockedByShield_LM(DamageSource source) {
        return blockedByShield(source);
    }
}
