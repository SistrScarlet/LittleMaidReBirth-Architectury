package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.mob.PathAwareEntity;
import net.sistr.littlemaidrebirth.entity.HasMovingMode;
import net.sistr.littlemaidrebirth.entity.MovingMode;
import net.sistr.littlemaidrebirth.entity.Tameable;

public class HasMMFollowTameOwnerGoal
        <T extends PathAwareEntity
                & Tameable
                & HasMovingMode>
        extends FollowTameOwnerGoal<T> {

    public HasMMFollowTameOwnerGoal(T tameable, float speed, float followStart, float followEnd) {
        super(tameable, speed, followStart, followEnd);
    }

    @Override
    public boolean canStart() {
        return this.tameable.getMovingMode() == MovingMode.ESCORT && super.canStart();
    }
}
