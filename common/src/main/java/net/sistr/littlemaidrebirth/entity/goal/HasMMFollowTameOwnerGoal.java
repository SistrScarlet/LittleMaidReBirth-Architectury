package net.sistr.littlemaidrebirth.entity.goal;

import java.util.function.Supplier;
import net.minecraft.entity.passive.TameableEntity;
import net.sistr.littlemaidrebirth.entity.util.HasMovingMode;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;

public class HasMMFollowTameOwnerGoal<T extends TameableEntity & HasMovingMode>
        extends FollowTameOwnerGoal<T> {

    public HasMMFollowTameOwnerGoal(
            T tameable,
            Supplier<Float> speed,
            Supplier<Float> followStart,
            Supplier<Float> followEnd) {
        super(tameable, speed, followStart, followEnd);
    }

    @Override
    public boolean canStart() {
        return this.tameable.getMovingMode() == MovingMode.ESCORT && super.canStart();
    }
}
