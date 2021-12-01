package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.sistr.littlemaidrebirth.util.LivingAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity implements LivingAccessor {


    @Shadow protected abstract void tickActiveItemStack();

    @Shadow public abstract boolean blockedByShield(DamageSource source);

    @Shadow protected abstract void sendEquipmentChanges();

    @Override
    public void applyEquipmentAttributes_LM() {
        sendEquipmentChanges();
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
