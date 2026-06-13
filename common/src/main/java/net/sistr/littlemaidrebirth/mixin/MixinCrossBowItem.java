package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CrossbowItem.class)
public abstract class MixinCrossBowItem extends MixinRangedWeaponItem {

    @Override
    public int getInterval_LMRB(ItemStack stack, LivingEntity user) {
        return CrossbowItem.getPullTime(stack, user);
    }
}
