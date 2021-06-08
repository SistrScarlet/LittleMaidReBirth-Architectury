package net.sistr.littlemaidrebirth.api.mode;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public interface IRangedWeapon {

    float getMaxRange_LMRB(ItemStack stack, LivingEntity user);

    int getInterval_LMRB(ItemStack stack, LivingEntity user);

}
