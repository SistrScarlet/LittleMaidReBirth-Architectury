package net.sistr.littlemaidrebirth.entity;

import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.mob.MobEntity;
import net.sistr.littlemaidrebirth.config.LMRBConfig;
import net.sistr.littlemaidrebirth.entity.goal.*;
import net.sistr.littlemaidrebirth.entity.mode.ModeWrapperGoal;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;
import net.sistr.littlemaidrebirth.tags.LMTags;

final class LMGoalInitializer {

  private LMGoalInitializer() {}

  static void initGoals(LittleMaidEntity maid) {
    int priority = -1;
    LMRBConfig config = LittleMaidEntity.getConfig();

    // 緊急テレポート
    maid.getGoalSelector()
        .add(
            priority,
            new LMTeleportTameOwnerGoal(
                maid, () -> config.movement.emergencyTeleportStartDistance) {
              @Override
              public boolean canStart() {
                return maid.isEmergency()
                    && maid.hurtTime > 0
                    && !TameableUtil.isWait(maid)
                    && super.canStart();
              }
            });

    maid.getGoalSelector().add(++priority, new SwimGoal(maid));
    maid.getGoalSelector().add(++priority, new LongDoorInteractGoal(maid, true));

    maid.getGoalSelector()
        .add(
            ++priority,
            new LMHealMyselfGoal(
                maid,
                () -> config.health.healInterval,
                () -> config.health.healAmount,
                stack -> stack.isIn(LMTags.Items.MAIDS_SALARY)));

    maid.getGoalSelector().add(++priority, new LMCollectSalaryFromContainerGoal<>(maid));

    maid.getGoalSelector().add(++priority, new WaitGoal<>(maid));

    maid.getGoalSelector()
        .add(
            ++priority,
            new LMTeleportTameOwnerGoal(maid, () -> config.movement.teleportStartDistance));

    // 危険な敵からの逃避
    maid.getGoalSelector()
        .add(
            ++priority,
            new FleeEntityGoal<>(
                maid,
                MobEntity.class,
                config.target.dangerousAvoidDistance,
                config.movement.followSpeed,
                config.movement.sprintSpeed,
                entity -> maid.getFleeEntities().containsKey(entity)) {
              @Override
              public void tick() {
                maid.getFleeEntities()
                    .entrySet()
                    .removeIf(entry -> entry.getValue().test(entry.getKey()));
                super.tick();
              }

              @Override
              public void stop() {
                super.stop();
                this.mob.getNavigation().stop();
              }
            });

    maid.getGoalSelector()
        .add(
            ++priority,
            new ModeWrapperGoal<>(maid) {
              @Override
              public boolean canStart() {
                return !this.owner.isStrike()
                    && (config.health.enableWorkInEmergency || !maid.isEmergency())
                    && super.canStart();
              }

              @Override
              public boolean shouldContinue() {
                return !this.owner.isStrike()
                    && (config.health.enableWorkInEmergency || !maid.isEmergency())
                    && super.shouldContinue();
              }
            });

    maid.getGoalSelector()
        .add(
            ++priority,
            new HasMMFollowTameOwnerGoal<>(
                maid,
                () -> config.movement.sprintSpeed,
                () -> config.movement.sprintStartDistance,
                () -> config.movement.sprintEndDistance) {
              @Override
              public void start() {
                super.start();
                this.tameable.setSprinting(true);
              }

              @Override
              public void stop() {
                super.stop();
                this.tameable.setSprinting(false);
              }
            });

    maid.getGoalSelector()
        .add(
            ++priority,
            new FollowAtHeldItemGoal<>(
                maid,
                () -> config.misc.stareAtSalaryRange,
                stack -> stack.isIn(LMTags.Items.MAIDS_SALARY),
                () -> config.misc.followAtHeldSalaryRange,
                true));
    maid.getGoalSelector()
        .add(
            ++priority,
            new LittleMaidEntity.LMStareAtHeldItemGoal<>(
                maid,
                () -> config.misc.stareAtSalaryRange,
                stack -> stack.isIn(LMTags.Items.MAIDS_SALARY),
                true));

    // todo 頭の装飾品を仕舞わないようにする
    maid.getGoalSelector()
        .add(
            ++priority,
            new LMStoreItemToContainerGoal<>(
                maid,
                stack ->
                    stack.isIn(LMTags.Items.MAIDS_SALARY)
                        || maid.hasModeImpl
                            .getMode()
                            .filter(mode -> mode.getModeType().isModeItem(stack))
                            .isPresent(),
                () -> config.work.searchContainerRange));

    maid.getGoalSelector()
        .add(
            ++priority,
            new LMMoveToDropItemGoal(
                maid,
                () -> config.movement.pickupItemRange,
                () -> config.movement.pickupItemFrequency,
                () -> config.movement.pickupItemSpeed) {
              @Override
              public boolean canStart() {
                return TameableUtil.hasTameOwner(maid)
                    && (config.health.enableWorkInEmergency || !maid.isEmergency())
                    && super.canStart();
              }

              @Override
              public List<ItemEntity> findAroundDropItem() {
                return TameableUtil.getTameOwner(maid)
                    .map(
                        owner ->
                            super.findAroundDropItem().stream()
                                .filter(item -> !this.isOwnerRange(item, owner))
                                .collect(Collectors.toList()))
                    .orElse(super.findAroundDropItem());
              }
            });

    maid.getGoalSelector()
        .add(
            ++priority,
            new HasMMFollowTameOwnerGoal<>(
                maid,
                () -> config.movement.followSpeed,
                () -> config.movement.followStartDistance,
                () -> config.movement.followEndDistance));

    maid.getGoalSelector().add(++priority, new PlaySnowGoal(maid));

    maid.getGoalSelector()
        .add(++priority, new RedstoneTraceGoal(maid, () -> config.movement.tracerSpeed));
    maid.getGoalSelector()
        .add(
            ++priority,
            new FreedomGoal<>(
                maid, config.movement.freedomSpeed, () -> config.movement.freedomRange));

    // 野良
    maid.getGoalSelector()
        .add(
            ++priority,
            new LMMoveToDropItemGoal(
                maid,
                () -> config.movement.pickupItemRange,
                () -> config.movement.pickupItemFrequency,
                () -> config.movement.pickupItemSpeed) {
              @Override
              public boolean canStart() {
                return !TameableUtil.hasTameOwner(maid)
                    && config.misc.canPickupItemByNoOwner
                    && (config.health.enableWorkInEmergency || !maid.isEmergency())
                    && super.canStart();
              }
            });
    maid.getGoalSelector()
        .add(
            ++priority,
            new EscapeDangerGoal(maid, config.movement.escapeSpeed) {
              @Override
              public boolean canStart() {
                return !TameableUtil.hasTameOwner(maid) && super.canStart();
              }
            });
    maid.getGoalSelector()
        .add(
            ++priority,
            new FollowAtHeldItemGoal<>(
                maid,
                () -> config.misc.stareAtEmployItemRange,
                stack -> stack.isIn(LMTags.Items.MAIDS_EMPLOYABLE),
                () -> config.misc.followAtHeldEmployItemRange,
                false));
    maid.getGoalSelector()
        .add(
            ++priority,
            new LittleMaidEntity.LMStareAtHeldItemGoal<>(
                maid,
                () -> config.misc.stareAtEmployItemRange,
                stack -> stack.isIn(LMTags.Items.MAIDS_EMPLOYABLE),
                false));

    maid.getGoalSelector()
        .add(
            ++priority,
            new WanderAroundFarGoal(maid, config.movement.freedomSpeed) {
              @Override
              public boolean canStart() {
                return !TameableUtil.hasTameOwner(maid) && super.canStart();
              }
            });

    // 視線
    maid.getGoalSelector().add(++priority, new LookAtEntityGoal(maid, LivingEntity.class, 8.0F));
    maid.getGoalSelector().add(priority, new LookAroundGoal(maid));

    // ターゲット系
    maid.getTargetSelector().add(0, new LMTargetGoal(maid));
  }
}
