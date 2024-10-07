package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;

public class PlaySnowGoal extends Goal {
    private final LittleMaidEntity mob;
    private final int maxCraftSnowballTime = 60;
    private final int maxLookTargetTime = 30;
    private final int maxWaitNextTime = 30;
    private int state;
    private int timer;
    @Nullable
    private LivingEntity target;

    public PlaySnowGoal(LittleMaidEntity mob) {
        this.mob = mob;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        var time = this.mob.getWorld().getTimeOfDay();
        // 朝～昼以外はやらない
        if (time < 0 || 12500 < time) {
            return false;
        }
        var block = this.mob.getBlockStateAtPos();
        return block.isIn(BlockTags.SNOW);
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue();
    }

    @Override
    public void start() {
        state = 0;
        timer = 0;
        target = null;
    }

    @Override
    public void tick() {
        // 雪玉を作る
        if (state == 0) {
            if (timer == 0) {
                this.mob.play(LMSounds.COLLECT_SNOW);
            }
            if (timer % 15 == 0 && timer % 30 != 0) {
                this.mob.swingHand(Hand.MAIN_HAND);
                this.mob.getWorld().playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(),
                        SoundEvents.BLOCK_SNOW_HIT, SoundCategory.NEUTRAL, 1.0f, 1.0f);
            }

            this.mob.setSneaking(true);
            var lookAt = this.mob.getPos()
                    .add(this.mob.getRotationVector()
                            .multiply(1, 0, 1)
                            .normalize()
                            .multiply(this.mob.getEyeHeight(this.mob.getPose())));
            this.mob.getLookControl().lookAt(lookAt);

            timer++;
            if (timer >= maxCraftSnowballTime) {
                state = 1;
                timer = 0;
            }
        }
        // 当てる相手を探す
        else if (state == 1) {
            this.mob.setSneaking(false);

            var world = this.mob.getWorld();

            if (target == null) {
                timer = 0;
                this.target = world.getEntitiesByClass(LivingEntity.class,
                                this.mob.getBoundingBox().expand(10),
                                entity -> this.mob != entity)
                        .stream()
                        .sorted(Comparator.comparingDouble(this.mob::squaredDistanceTo))
                        .filter(entity -> this.mob.getVisibilityCache().canSee(entity))
                        .findAny()
                        .orElse(null);
            } else {
                if (this.mob.getVisibilityCache().canSee(target)) {
                    timer++;
                    this.mob.getLookControl().lookAt(target);
                } else {
                    timer = 0;
                    this.target = null;
                }
            }

            if (timer >= maxLookTargetTime) {
                state = 2;
                timer = 0;
            }
        }
        // 投げる
        else {
            this.mob.setSneaking(false);

            if (target == null) {
                state = 1;
                timer = 0;
            } else {
                if (timer == 0) {
                    shootSnowBall(this.mob.getWorld(), this.mob);
                    this.mob.swingHand(Hand.MAIN_HAND);
                    this.mob.play(LMSounds.SHOOT);
                    this.mob.setYaw(this.mob.getHeadYaw());
                }
                this.mob.getLookControl().lookAt(target);
            }

            timer++;
            if (timer >= maxWaitNextTime) {
                state = 0;
                timer = 0;
                target = null;
            }
        }
    }

    private void shootSnowBall(World world, LivingEntity user) {
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.NEUTRAL,
                0.5f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
        if (!world.isClient) {
            SnowballEntity snowballEntity = new SnowballEntity(world, user);
            snowballEntity.setItem(Items.SNOWBALL.getDefaultStack());
            snowballEntity.setVelocity(user, user.getPitch(), user.getHeadYaw(), 0.0f, 1.5f, 1.0f);
            world.spawnEntity(snowballEntity);
        }
    }

    @Override
    public void stop() {
        this.mob.setSneaking(false);
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }
}
