package net.sistr.lmrb.mixin;

import net.minecraft.item.RangedWeaponItem;
import net.sistr.lmrb.api.item.IRangedWeapon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RangedWeaponItem.class)
public abstract class MixinRangedWeaponItem implements IRangedWeapon {
    @Shadow public abstract int getRange();

    @Override
    public float getMaxRange_LMRB() {
        return getRange();
    }

    @Override
    public int getInterval_LMRB() {
        return 25;
    }
}
