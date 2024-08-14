package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

import java.util.function.Predicate;

public class TameableStareAtHeldItemGoal<T extends TameableEntity> extends StareAtHeldItemGoal<T> {
    protected final boolean isTamed;

    public TameableStareAtHeldItemGoal(T mob, boolean isTamed, Predicate<ItemStack> targetItem) {
        super(mob, targetItem);
        this.isTamed = isTamed;
    }

    @Override
    public boolean canStart() {
        return TameableUtil.getTameOwnerUuid(mob).isPresent() == isTamed && super.canStart();
    }
}
