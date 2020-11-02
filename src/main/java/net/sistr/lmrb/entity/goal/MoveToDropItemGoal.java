package net.sistr.lmrb.entity.goal;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.sistr.lmml.entity.compound.SoundPlayable;
import net.sistr.lmml.resource.util.LMSounds;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

//ドロップアイテムに向かうGoal
public class MoveToDropItemGoal extends Goal {
    private final PathAwareEntity mob;
    private final int range;
    private final double speed;
    private int cool;

    public MoveToDropItemGoal(PathAwareEntity mob, int range, double speed) {
        this.mob = mob;
        this.range = range;
        this.speed = speed;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (--cool < 0) {
            cool = 60;
            Stream<BlockPos> positions = findAroundDropItem().stream().map(Entity::getBlockPos);
            Path path = positions.map(pos -> mob.getNavigation().findPathTo(pos, 0))
                    .filter(Objects::nonNull)
                    .filter(Path::reachesTarget)
                    .findAny().orElse(null);
            if (path != null) {
                mob.getNavigation().startMovingAlong(path, speed);
                return true;
            }

        }
        return false;
    }

    @Override
    public void start() {
        super.start();
        if (mob instanceof SoundPlayable) {
            ((SoundPlayable) mob).play(LMSounds.FIND_TARGET_I);
        }
    }

    @Override
    public boolean shouldContinue() {
        return !mob.getNavigation().isIdle();
    }

    @Override
    public void tick() {
        super.tick();
    }

    public List<ItemEntity> findAroundDropItem() {
        return mob.world.getEntitiesByClass(ItemEntity.class,
                mob.getBoundingBox().expand(range,range / 4F, range),
                item -> !item.cannotPickup() && item.squaredDistanceTo(mob) < range * range);
    }
}
