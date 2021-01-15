package net.sistr.lmrb.entity.mode;

import com.google.common.collect.Lists;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Shearable;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RipperMode implements Mode {
    protected final PathAwareEntity mob;
    protected final int radius;
    protected final List<Entity> shearable = Lists.newArrayList();
    protected int timeToRecalcPath;
    protected int timeToIgnore;


    public RipperMode(PathAwareEntity mob, int radius) {
        this.mob = mob;
        this.radius = radius;
    }

    @Override
    public void startModeTask() {

    }

    @Override
    public boolean shouldExecute() {
        this.shearable.addAll(findCanShearableMob());
        return !this.shearable.isEmpty();
    }

    public Collection<Entity> findCanShearableMob() {
        Box bb = new Box(
                this.mob.getX() + radius,
                this.mob.getY() + radius / 4F,
                this.mob.getZ() + radius,
                this.mob.getX() - radius,
                this.mob.getY() - radius / 4F,
                this.mob.getZ() - radius);
        return this.mob.world.getOtherEntities(this.mob, bb, (entity) ->
                entity instanceof LivingEntity && entity instanceof Shearable
                        && ((Shearable) entity).isShearable()
                        && this.mob.getVisibilityCache().canSee(entity));
    }

    @Override
    public boolean shouldContinueExecuting() {
        return !this.shearable.isEmpty();
    }

    @Override
    public void startExecuting() {
        this.mob.getNavigation().stop();
        List<Entity> tempList = this.shearable.stream()
                .sorted(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(this.mob)))
                .collect(Collectors.toList());
        this.shearable.clear();
        this.shearable.addAll(tempList);
    }

    @Override
    public void tick() {
        Entity target = this.shearable.get(0);
        if (!(target instanceof LivingEntity) || !(target instanceof Shearable)) {
            this.shearable.remove(0);
            this.timeToIgnore = 0;
            return;
        }
        if (200 < ++this.timeToIgnore) {
            this.shearable.remove(0);
            this.timeToIgnore = 0;
            return;
        }
        if (target.squaredDistanceTo(this.mob) < 2 * 2) {
            ItemStack stack = this.mob.getMainHandStack();
            if (((Shearable) target).isShearable()) {
                ((Shearable) target).sheared(SoundCategory.PLAYERS);
                stack.damage(1, (LivingEntity) target, e -> e.sendToolBreakStatus(Hand.MAIN_HAND));
            }
            this.shearable.remove(0);
            this.timeToIgnore = 0;
            return;
        }
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            this.mob.getNavigation().startMovingTo(target.getX(), target.getY(), target.getZ(), 1);
        }
    }

    @Override
    public void resetTask() {
        this.timeToIgnore = 0;
        this.timeToRecalcPath = 0;
        this.shearable.clear();
    }

    @Override
    public void endModeTask() {

    }

    @Override
    public void writeModeData(CompoundTag tag) {

    }

    @Override
    public void readModeData(CompoundTag tag) {

    }

    @Override
    public String getName() {
        return "Ripper";
    }

    static {
        ModeManager.ModeItems items = new ModeManager.ModeItems();
        items.add(ShearsItem.class);
        ModeManager.INSTANCE.register(RipperMode.class, items);
    }
}
