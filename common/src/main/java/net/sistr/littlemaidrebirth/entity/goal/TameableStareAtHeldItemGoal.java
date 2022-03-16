package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidrebirth.entity.Tameable;

import java.util.Set;
import java.util.function.Predicate;

public class TameableStareAtHeldItemGoal extends StareAtHeldItemGoal {
    protected final Tameable tameable;
    protected final boolean isTamed;

    public TameableStareAtHeldItemGoal(PathAwareEntity mob, Tameable tameable, boolean isTamed, Predicate<ItemStack> targetItem) {
        super(mob, targetItem);
        this.tameable = tameable;
        this.isTamed = isTamed;
    }

    @Override
    public boolean canStart() {
        return tameable.getTameOwner().isPresent() == isTamed && super.canStart();
    }
}
