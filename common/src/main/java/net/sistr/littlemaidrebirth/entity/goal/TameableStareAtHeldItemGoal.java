package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidrebirth.entity.util.Tameable;

import java.util.function.Predicate;

public class TameableStareAtHeldItemGoal<T extends PathAwareEntity & Tameable> extends StareAtHeldItemGoal<T> {
    protected final boolean isTamed;

    public TameableStareAtHeldItemGoal(T mob, boolean isTamed, Predicate<ItemStack> targetItem) {
        super(mob, targetItem);
        this.isTamed = isTamed;
    }

    @Override
    public boolean canStart() {
        return this.mob.getTameOwner().isPresent() == isTamed && super.canStart();
    }
}
