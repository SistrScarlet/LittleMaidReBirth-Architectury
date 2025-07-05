package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.iff.IFFTag;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;
import net.sistr.littlemaidrebirth.entity.util.TargetingConfig;
import net.sistr.littlemaidrebirth.entity.util.TargetingSystem;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class LMTargetGoal extends Goal {
    private final LittleMaidEntity maid;
    private MobEntity target;
    private int recalc = 0;

    public LMTargetGoal(LittleMaidEntity maid) {
        this.maid = maid;
        setControls(EnumSet.of(Control.TARGET));
    }

    @Override
    public boolean canStart() {
        int chance = 10; //todo コンフィグ化検討
        if (this.maid.getRandom().nextInt(getTickCount(chance)) != 0) {
            return false;
        }

        return targeting();
    }

    private boolean targeting() {
        // 範囲内に敵がいるかチェック
        var aroundMobs = getAroundMobs();
        if (aroundMobs.isEmpty()) {
            return false;
        }
        var aroundMaids = getAroundMaids();
        // 各敵の優先度をチェック
        var priorities = calculateEnemyPriorities(aroundMobs, aroundMaids);

        // ターゲットできるなら実行
        var highestPriorityMob = priorities.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // 避難する
        var enemies = new ArrayList<>(priorities.keySet());
        var maidWrapper = new TargetingSystem.Maid(this.maid);
        if (TargetingSystem.needsEvacuation(maidWrapper, enemies)) {
            TargetingSystem.getDangerousEnemies(maidWrapper, enemies)
                    .forEach(mob -> this.maid.addFleeEntity(mob.getMob(), e ->
                            !e.isAlive()
                                    || this.maid.squaredDistanceTo(e) > (TargetingConfig.getDangerCloseRangeThreshold() + 4)
                                    * (TargetingConfig.getDangerCloseRangeThreshold() + 4))
                    );
        }

        // 最高優先度のモブをターゲットにする
        if (highestPriorityMob != null) {
            this.target = highestPriorityMob.getMob();
            this.maid.setTarget(highestPriorityMob.getMob());
            return true;
        }


        return false;
    }

    @Override
    public boolean shouldContinue() {
        // 現在のターゲットがまだ有効かチェック
        if (!isTargetable(this.target, TargetingConfig.getMaxTargetDistance())) {
            // ターゲットが居なくなったら再計算
            return targeting();
        }
        // 再計算カウンター
        recalc = Math.max(0, recalc - 1);
        if (recalc > 0) {
            recalc = getTickCount(10);
            return true;
        }
        // 状況の変化により優先度を再計算する
        return targeting();
    }

    @Override
    public void start() {
        super.start();
        // ターゲット確定時の初期設定
        recalc = getTickCount(10);
    }

    @Override
    public void stop() {
        super.stop();
        // ターゲットのクリア
        recalc = 0;
        this.target = null;
    }

    private List<MobEntity> getAroundMobs() {
        float distance = TargetingConfig.getMaxTargetDistance();
        return this.maid.getWorld().getEntitiesByClass(
                MobEntity.class,
                this.maid.getBoundingBox().expand(distance, distance / 2f, distance).expand(1),
                mob -> mob != this.maid
                        && isTargetable(mob, distance)
                        && this.maid.getVisibilityCache().canSee(mob));
    }

    private boolean isTargetable(MobEntity mob, float distance) {
        return this.maid.squaredDistanceTo(mob) <= distance * distance
                && maid.canTarget(mob) // isFriend()とcanTakeDamage()判定込み
                && mob.isAlive();
    }

    private Map<TargetingSystem.Mob, Float> calculateEnemyPriorities(
            List<MobEntity> aroundEntities, List<LittleMaidEntity> aroundMaids) {
        return TargetingSystem.calculateEnemyPriorities(
                new TargetingSystem.Maid(this.maid),
                aroundEntities.stream()
                        .map(mob -> new TargetingSystem.Mob(
                                mob,
                                this.maid.identify(mob)
                                        .map(tag -> {
                                            if (this.maid.isBloodSuck()) {
                                                return true;
                                            } else {
                                                return tag == IFFTag.ENEMY;
                                            }
                                        })
                                        .orElse(this.maid.isBloodSuck())
                        )).toList(),
                TameableUtil.getTameOwner(this.maid).map(TargetingSystem.Master::new).orElse(null),
                aroundMaids.stream().map(TargetingSystem.Maid::new).toList(),
                new TargetingSystem.CombatSettings(TargetingSystem.MasterStance.GUARD, 2));
    }

    private List<LittleMaidEntity> getAroundMaids() {
        float distance = TargetingConfig.getMaxTargetDistance();
        return this.maid.getWorld().getEntitiesByClass(
                LittleMaidEntity.class,
                this.maid.getBoundingBox()
                        .expand(distance, distance / 2f, distance).expand(1),
                maid -> maid != this.maid);
    }
}
