package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class TameableStareAtHeldItemGoal<T extends TameableEntity> extends StareAtHeldItemGoal<T> {
    protected final boolean isTamed;

    public TameableStareAtHeldItemGoal(T mob, Supplier<Float> stareAtRange, Predicate<ItemStack> targetItem, boolean isTamed) {
        super(mob, stareAtRange, targetItem);
        this.isTamed = isTamed;
    }

    @Override
    public boolean canStart() {
        return TameableUtil.hasTameOwner(this.mob) == isTamed && super.canStart();
    }
}
