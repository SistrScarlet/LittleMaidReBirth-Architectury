package net.sistr.littlemaidrebirth.entity.mode;

import com.google.common.collect.Lists;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Shearable;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

import java.util.Collection;
import java.util.Queue;

public class RipperMode extends Mode {
    protected final LittleMaidEntity mob;
    protected final float radius;
    protected final Queue<Entity> shearable = Lists.newLinkedList();
    protected int timeToRecalcPath;
    protected int timeToIgnore;
    protected int cool;

    public RipperMode(ModeType<? extends Mode> modeType, String name, LittleMaidEntity mob, float radius) {
        super(modeType, name);
        this.mob = mob;
        this.radius = radius;
    }

    @Override
    public boolean shouldExecute() {
        if (0 < cool--) {
            return false;
        }
        cool = 40;
        this.shearable.addAll(findCanShearableMob());
        return !this.shearable.isEmpty();
    }

    public Collection<Entity> findCanShearableMob() {
        Box bb = new Box(
                this.mob.getX() + radius,
                this.mob.getY() + radius / 2F,
                this.mob.getZ() + radius,
                this.mob.getX() - radius,
                this.mob.getY() - radius / 2F,
                this.mob.getZ() - radius);
        return this.mob.getEntityWorld().getOtherEntities(this.mob, bb, (entity) ->
                entity instanceof LivingEntity && entity instanceof Shearable
                        && ((Shearable) entity).isShearable());
    }

    @Override
    public boolean shouldContinueExecuting() {
        return !this.shearable.isEmpty();
    }

    @Override
    public void tick() {
        if (this.shearable.isEmpty()) {
            return;
        }
        Entity target = this.shearable.peek();
        if (!(target instanceof LivingEntity) || !(target instanceof Shearable)) {
            this.shearable.remove();
            this.timeToIgnore = 0;
            return;
        }
        if (200 < ++this.timeToIgnore) {
            this.shearable.remove();
            this.timeToIgnore = 0;
            return;
        }
        if (target.squaredDistanceTo(this.mob) < 2.5f * 2.5f) {
            ItemStack stack = this.mob.getMainHandStack();
            if (((Shearable) target).isShearable()) {
                ((Shearable) target).sheared(SoundCategory.PLAYERS);
                stack.damage(1, this.mob, e -> e.sendToolBreakStatus(Hand.MAIN_HAND));
            }
            this.shearable.remove();
            this.timeToIgnore = 0;
            this.mob.getNavigation().stop();
            return;
        }
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            Path path = this.mob.getNavigation().findPathTo(target.getX(), target.getY(), target.getZ(), 1);
            if (path == null || path.getEnd() == null
                    || Vec3d.of(path.getEnd().getBlockPos()).add(0.5, 0, 0.5)
                    .squaredDistanceTo(target.getPos()) > 2.5f * 2.5f) {
                this.shearable.remove();
                this.timeToIgnore = 0;
            } else {
                this.mob.getNavigation().startMovingAlong(path, 1.0f);
            }
        }
    }

    @Override
    public void resetTask() {
        this.timeToIgnore = 0;
        this.timeToRecalcPath = 0;
        this.shearable.clear();
    }

}
