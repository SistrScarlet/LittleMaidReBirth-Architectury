package net.sistr.littlemaidrebirth.entity.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.util.math.Vec3d;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * メイドさん統合ターゲティングシステム
 */
public class TargetingSystem {

    // 定数はTargetingConfigクラスで管理

    /**
     * ご主人のスタンス設定
     */
    public enum MasterStance {
        GUARD("guard"),      // 護衛：ご主人保護優先（現状維持）
        SUPPORT("support");  // 援護：ご主人の戦闘を邪魔せず周囲処理

        private final String value;

        MasterStance(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 戦闘設定クラス
     */
    public static class CombatSettings {
        private final MasterStance masterStance;

        public CombatSettings() {
            this(MasterStance.GUARD);
        }

        public CombatSettings(MasterStance masterStance) {
            this.masterStance = masterStance;
        }

        public MasterStance getMasterStance() {
            return masterStance;
        }

    }

    /**
     * 基本エンティティクラス
     */
    public static class EntityWrapper {
        private final LivingEntity entity;

        public EntityWrapper(LivingEntity entity) {
            this.entity = entity;
        }

        /**
         * 指定されたエンティティをターゲットしているかチェック
         */
        public boolean isTargeting(EntityWrapper target) {
            if (this.entity instanceof MobEntity mobEntity) {
                return mobEntity.getTarget() == target.entity;
            }
            return false;
        }

        /**
         * 指定されたエンティティから攻撃を受けているかチェック
         */
        public boolean isAttackedBy(EntityWrapper attacker) {
            // 攻撃を受けて指定tick以内かつアタッカーが一致
            return entity.age - TargetingConfig.getAttackedByValidTicks() < entity.getLastAttackedTime()
                    && entity.getAttacker() == attacker.entity;
        }

        public boolean isInjured() {
            return this.entity.getHealth() / this.entity.getMaxHealth() < TargetingConfig.getInjuredHealthThreshold();
        }

        public Vec3d getPosition() {
            return this.entity.getPos();
        }

        public Optional<EntityWrapper> getCurrentTarget() {
            if (this.entity instanceof MobEntity mobEntity) {
                return Optional.ofNullable(mobEntity.getTarget()).map(EntityWrapper::new);
            }
            return Optional.empty();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof EntityWrapper entityWrapper)) return false;
            return this.entity.equals(entityWrapper.entity);
        }

        @Override
        public int hashCode() {
            return entity.hashCode();
        }
    }

    /**
     * メイドさんクラス
     */
    public static class Maid extends Mob {
        private final LittleMaidEntity maid;

        public Maid(LittleMaidEntity maid) {
            super(maid, false);
            this.maid = maid;
        }

        public CombatType getCombatType() {
            return maid.getMode().map(mode -> {
                if (mode.getBattleModeType() == Mode.BattleModeType.BOW) {
                    return CombatType.BOW;
                } else {
                    return CombatType.SWORD;
                }
            }).orElse(CombatType.SWORD);
        }
    }

    /**
     * モブエンティティクラス
     */
    public static class Mob extends EntityWrapper {
        private final MobEntity mob;
        private final boolean isEnemy;

        public Mob(MobEntity mob, boolean isEnemy) {
            super(mob);
            this.mob = mob;
            this.isEnemy = isEnemy;
        }

        public boolean canHitProjectile() {
            return !(mob instanceof EndermanEntity
                    || this.mob instanceof EnderDragonEntity);
        }

        public boolean isDangerous() {
            return this.mob instanceof EnderDragonEntity
                    || this.mob instanceof WitherEntity
                    || this.mob instanceof WardenEntity
                    || this.mob instanceof RavagerEntity
                    || this.mob instanceof CreeperEntity
                    || this.mob instanceof EvokerEntity;
        }

        public boolean isEnemy() {
            return this.isEnemy;
        }

        public MobEntity getMob() {
            return mob;
        }
    }

    /**
     * ご主人クラス
     */
    public static class Master extends EntityWrapper {

        public Master(LivingEntity master) {
            super(master);
        }

        @Override
        public boolean isTargeting(EntityWrapper target) {
            return target.isAttackedBy(this);
        }
    }

    public enum CombatType {
        NONE,
        SWORD,
        BOW
    }

    /**
     * 指定されたメイドさんから見た各敵の優先度を算出
     *
     * @param maid       判断するメイドさん
     * @param enemies    周囲の敵リスト
     * @param master     ご主人の情報（nullの場合は主人関連の判定をスキップ）
     * @param otherMaids 他のメイドさんのリスト
     * @param settings   戦闘設定（オプション、デフォルトは護衛モード）
     * @return {enemy: priority_score} のマップ
     */
    public static Map<Mob, Float> calculateEnemyPriorities(
            Maid maid, List<Mob> enemies, @Nullable Master master, List<Maid> otherMaids,
            @Nullable CombatSettings settings) {

        CombatSettings combatSettings = settings != null ? settings : new CombatSettings();  // デフォルトは護衛モード
        Map<Mob, Float> priorities = new HashMap<>();

        for (Mob mob : enemies) {
            float basePriority = calculateBasePriority(mob, maid, master, otherMaids);

            // ベースが0以下なら敵対しない
            if (basePriority <= 0) {
                priorities.put(mob, basePriority);
                continue;
            }

            float score = basePriority;
            score += calculateDistanceModifier(mob, maid, master);
            score += calculateWeaponCompatibility(mob, maid);
            score += calculateDangerModifier(mob, maid, master, otherMaids);
            score += calculateDistributionModifier(mob, maid, otherMaids);
            score += calculateMasterStanceModifier(mob, master, combatSettings);
            score += calculateInjuredAllyAttackerModifier(mob, maid, master, otherMaids);

            priorities.put(mob, score);
        }

        return priorities;
    }

    /**
     * 基本優先度を階層化に基づいて算出
     */
    public static float calculateBasePriority(Mob mob, Maid maid, Master master, List<Maid> otherMaids) {
        float distanceToMaid = (float) mob.getPosition().distanceTo(maid.getPosition());

        // 特別優先度: 危険敵からの避難
        if (mob.isDangerous() && (mob.isTargeting(maid) || maid.isAttackedBy(mob))) {
            return TargetingConfig.getPriorityEvacuation();  // 避難優先（負の値で戦闘回避）
        }

        // 優先度1: 攻撃してきた相手
        if (maid.isAttackedBy(mob)) {
            return TargetingConfig.getPrioritySelfAttacker();
        }

        // 優先度2: ご主人を攻撃した相手
        if (master != null && master.isAttackedBy(mob)) {
            return TargetingConfig.getPriorityMasterAttacker();
        }

        // 優先度4: 他メイドさんを攻撃した相手
        for (Maid otherMaid : otherMaids) {
            if (otherMaid.isAttackedBy(mob)) {
                return TargetingConfig.getPriorityMaidAttacker();
            }
        }

        // 優先度5: ご主人が攻撃した相手
        if (master != null && (master.isTargeting(mob) || mob.isAttackedBy(master))) {
            return TargetingConfig.getPriorityMasterTarget();
        }

        // 優先度6: 他メイドさんがターゲットしている相手
        for (Maid otherMaid : otherMaids) {
            if (otherMaid.isTargeting(mob)) {
                return TargetingConfig.getPriorityMaidTarget();
            }
        }

        // 優先度7: 周囲のMob
        if (mob.isDangerous()) {
            return 0;  // 危険な敵には先制攻撃しない
        }
        // 先制攻撃距離制限（メイドさんとご主人両方からの距離を考慮）
        boolean withinPreemptiveRange = distanceToMaid <= TargetingConfig.getMaxPreemptiveDistance();
        if (master != null) {
            float distanceToMaster = (float) mob.getPosition().distanceTo(master.getPosition());
            withinPreemptiveRange = withinPreemptiveRange && distanceToMaster <= TargetingConfig.getMaxPreemptiveDistance();
        }
        if (!withinPreemptiveRange) {
            return 0;  // 先制攻撃範囲外
        }

        // 敵対モブは常にターゲット対象
        if (mob.isEnemy()) {
            return TargetingConfig.getPriorityNormalEnemy();
        } else {
            return 0;  // 中立モブはターゲット対象外
        }
    }

    /**
     * 距離に基づく優先度修正
     */
    public static float calculateDistanceModifier(Mob mob, Maid maid, @Nullable Master master) {
        float distanceToMaid = (float) mob.getPosition().distanceTo(maid.getPosition());

        // 最大ターゲット距離チェック（メイドさんとご主人両方からの距離を考慮）
        boolean withinTargetRange = distanceToMaid <= TargetingConfig.getMaxTargetDistance();
        if (master != null) {
            float distanceToMaster = (float) mob.getPosition().distanceTo(master.getPosition());
            withinTargetRange = withinTargetRange && distanceToMaster <= TargetingConfig.getMaxTargetDistance();
        }
        if (!withinTargetRange) {
            return TargetingConfig.getDistanceLimitPenalty();  // 距離が遠すぎる場合は大幅減点
        }

        // メイドさんからの距離ペナルティ（メイドさんから遠いほど低優先度）
        float maidDistancePenalty = Math.max(0,
                (distanceToMaid - TargetingConfig.getMaidDistancePenaltyBaseDistance())
                        * TargetingConfig.getMaidDistancePenaltyMultiplier());

        // ご主人との距離ボーナス（ご主人に近いほど高優先度）
        float masterDistanceBonus = 0;
        if (master != null) {
            float distanceToMaster = (float) mob.getPosition().distanceTo(master.getPosition());
            masterDistanceBonus = Math.max(0,
                    (TargetingConfig.getMasterDistanceBonusBaseDistance() - distanceToMaster)
                            * TargetingConfig.getMasterDistanceBonusMultiplier());
        }

        // ご主人距離ペナルティ（ご主人から遠いほど低優先度）
        float masterDistancePenalty = 0;
        if (master != null) {
            float distanceToMaster = (float) mob.getPosition().distanceTo(master.getPosition());
            masterDistancePenalty = Math.max(0,
                    (distanceToMaster - TargetingConfig.getMasterDistancePenaltyBaseDistance())
                            * TargetingConfig.getMasterDistancePenaltyMultiplier());
        }

        // メイドさん距離ボーナス（メイドさんに近いほど高優先度）
        float maidDistanceBonus = Math.max(0,
                (TargetingConfig.getMaidDistanceBonusBaseDistance() - distanceToMaid)
                        * TargetingConfig.getMaidDistanceBonusMultiplier());

        return masterDistanceBonus - maidDistancePenalty + maidDistanceBonus - masterDistancePenalty;
    }

    /**
     * 武器とターゲットの相性による修正
     */
    public static float calculateWeaponCompatibility(Mob mob, Maid maid) {
        float distanceToEnemy = (float) mob.getPosition().distanceTo(maid.getPosition());

        if (maid.getCombatType() == CombatType.BOW) {
            // 弓の場合
            if (!mob.canHitProjectile()) {
                return TargetingConfig.getNonProjectileHitPenalty();  // 弾が当たらない場合は戦わない
            } else if (distanceToEnemy >= TargetingConfig.getBowLongRangeThreshold()) {  // 遠距離ボーナス
                return TargetingConfig.getBowLongRangeBonus();
            } else {
                return TargetingConfig.getBowCloseRangePenalty();  // 近距離ペナルティ
            }
        } else if (maid.getCombatType() == CombatType.SWORD) {
            // 剣の場合
            // 危険敵への近接リスク
            if (mob.isDangerous()) {
                return TargetingConfig.getSwordDangerPenalty();  // 危険敵と戦うリスクペナルティ
            }
            if (distanceToEnemy <= TargetingConfig.getSwordCloseRangeThreshold()) {
                return TargetingConfig.getSwordCloseRangeBonus();  // 近距離ボーナス
            } else {
                return TargetingConfig.getSwordLongRangePenalty();  // 遠距離ペナルティ
            }
        }

        return 0;
    }

    /**
     * 危険な敵に対する特殊修正
     */
    public static float calculateDangerModifier(Mob mob, Maid maid, @Nullable Master master, List<Maid> otherMaids) {
        if (!mob.isDangerous()) {
            return 0;
        }

        float distanceToEnemy = (float) mob.getPosition().distanceTo(maid.getPosition());

        // 危険敵への基本ペナルティ
        float dangerPenalty = TargetingConfig.getDangerBasePenalty();

        // 近距離での危険敵は更にペナルティ
        if (distanceToEnemy < TargetingConfig.getDangerCloseRangeThreshold()) {
            dangerPenalty += TargetingConfig.getDangerClosePenalty();
        }

        return dangerPenalty;
    }

    /**
     * 他メイドさんとの分散攻撃を考慮した修正
     */
    public static float calculateDistributionModifier(Mob mob, Maid maid, List<Maid> otherMaids) {
        CombatType currentCombatType = maid.getCombatType();
        
        // 同じ武器種で同じ敵をターゲットしている他メイドさんの数
        long sameWeaponTargetingCount = otherMaids.stream()
                .filter(otherMaid -> otherMaid.isTargeting(mob))
                .filter(otherMaid -> otherMaid.getCombatType() != CombatType.NONE)  //非戦闘メイドさんのターゲットは省く
                .filter(otherMaid -> !otherMaid.isInjured())  // 負傷メイドさんのターゲットは省く
                .filter(otherMaid -> otherMaid.getCombatType() == currentCombatType)  // 同じ武器種
                .count();

        // 異なる武器種で同じ敵をターゲットしている他メイドさんの数
        long differentWeaponTargetingCount = otherMaids.stream()
                .filter(otherMaid -> otherMaid.isTargeting(mob))
                .filter(otherMaid -> otherMaid.getCombatType() != CombatType.NONE)  //非戦闘メイドさんのターゲットは省く
                .filter(otherMaid -> !otherMaid.isInjured())  // 負傷メイドさんのターゲットは省く
                .filter(otherMaid -> otherMaid.getCombatType() != currentCombatType)  // 異なる武器種
                .count();

        // 集中攻撃回避
        float sameWeaponPenalty = sameWeaponTargetingCount * TargetingConfig.getDistributionPenaltyMultiplier();
        float differentWeaponPenalty = differentWeaponTargetingCount * TargetingConfig.getDistributionPenaltyMultiplierDifferentWeapon();

        return -(sameWeaponPenalty + differentWeaponPenalty);
    }

    /**
     * ご主人のスタンスに基づく優先度修正
     */
    public static float calculateMasterStanceModifier(Mob mob, @Nullable Master master, CombatSettings settings) {
        if (master == null || settings.getMasterStance() == MasterStance.GUARD) {
            // 主人不在または護衛モード：現状維持（修正なし）
            return 0;
        } else if (settings.getMasterStance() == MasterStance.SUPPORT) {
            // 援護モード：ご主人の戦闘を邪魔しない
            if (master.isTargeting(mob) || mob.isAttackedBy(master)) {
                // ご主人がターゲットしている敵は低優先度に
                return TargetingConfig.getSupportModeMasterTargetPenalty();
            } else {
                // ご主人以外の敵は優先度アップ
                return TargetingConfig.getSupportModeOtherEnemyBonus();
            }
        }

        return 0;
    }

    /**
     * 負傷している身内の攻撃者に対する優先度ボーナス
     */
    public static float calculateInjuredAllyAttackerModifier(Mob mob, Maid maid, @Nullable Master master, List<Maid> otherMaids) {
        float injuredAllyAttackerBonus = 0;

        // ご主人が負傷している場合
        if (master != null && master.isInjured() && master.isAttackedBy(mob)) {
            injuredAllyAttackerBonus += TargetingConfig.getInjuredMasterAttackerBonus();
        }

        // 他のメイドさんが負傷している場合
        for (Maid otherMaid : otherMaids) {
            if (otherMaid.isInjured() && otherMaid.isAttackedBy(mob)) {
                injuredAllyAttackerBonus += TargetingConfig.getInjuredMaidAttackerBonus();
            }
        }

        return injuredAllyAttackerBonus;
    }

    /**
     * 避難が必要かどうかを判定（危険敵からの避難のみ）
     *
     * @param maid    判断するメイドさん
     * @param enemies 周囲の敵リスト
     * @return 避難が必要な場合true
     */
    public static boolean needsEvacuation(Maid maid, List<Mob> enemies) {
        return enemies.stream()
                .anyMatch(mob -> mob.isDangerous()
                        && (mob.isTargeting(maid)
                        || mob.getPosition().distanceTo(maid.getPosition())
                        <= TargetingConfig.getDangerCloseRangeThreshold())
                );
    }

    /**
     * 危険な敵（避難対象）を取得
     *
     * @param maid    判断するメイドさん
     * @param enemies 周囲の敵リスト
     * @return 最も危険な敵のリスト
     */
    public static List<Mob> getDangerousEnemies(Maid maid, List<Mob> enemies) {
        return enemies.stream()
                .filter(mob -> mob.isDangerous() && (mob.isTargeting(maid)
                        || mob.getPosition().distanceTo(maid.getPosition())
                        <= TargetingConfig.getDangerCloseRangeThreshold())
                ).sorted(Comparator.comparingDouble(e -> e.getPosition().distanceTo(maid.getPosition())))
                .toList(); // 最も近い危険敵を優先
    }
}