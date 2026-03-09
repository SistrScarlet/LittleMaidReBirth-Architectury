package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.sistr.littlemaidrebirth.api.mode.IRangedWeapon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RangedWeaponItem.class)
public abstract class MixinRangedWeaponItem implements IRangedWeapon {
    @Shadow
    public abstract int getRange();

    @Override
    public float getMaxRange_LMRB(ItemStack stack, LivingEntity user) {
        return getRange();
    }

    @Override
    public int getInterval_LMRB(ItemStack stack, LivingEntity user) {
        return 20;
    }
}
