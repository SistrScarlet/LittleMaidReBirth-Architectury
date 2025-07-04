package net.sistr.littlemaidrebirth.entity.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.WardenEntity;
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
        private final int maxMaidsPerEnemy;

        public CombatSettings() {
            this(MasterStance.GUARD, 2);
        }

        public CombatSettings(MasterStance masterStance, int maxMaidsPerEnemy) {
            this.masterStance = masterStance;
            this.maxMaidsPerEnemy = maxMaidsPerEnemy;
        }

        public MasterStance getMasterStance() {
            return masterStance;
        }

        public int getMaxMaidsPerEnemy() {
            return maxMaidsPerEnemy;
        }
    }

    /**
     * 基本エンティティクラス
     */
    public static class EntityWrapper {
        private final Entity entity;

        public EntityWrapper(Entity entity) {
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
    public static class Maid extends EntityWrapper {
        private final LittleMaidEntity maid;

        public Maid(LittleMaidEntity maid) {
            super(maid);
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

        public float getHealthPercent() {
            return maid.getHealth() / maid.getMaxHealth();
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
                    || this.mob instanceof RavagerEntity;
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
    }

    public enum CombatType {
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
    public static Map<Mob, Double> calculateEnemyPriorities(
            Maid maid, List<Mob> enemies, @Nullable Master master, List<Maid> otherMaids,
            @Nullable CombatSettings settings) {

        CombatSettings combatSettings = settings != null ? settings : new CombatSettings();  // デフォルトは護衛モード
        Map<Mob, Double> priorities = new HashMap<>();

        for (Mob mob : enemies) {
            double basePriority = calculateBasePriority(mob, maid, master, otherMaids);

            // ベースが0以下なら敵対しない
            if (basePriority <= 0) {
                priorities.put(mob, basePriority);
                continue;
            }

            double score = basePriority;
            score += calculateDistanceModifier(mob, maid, master);
            score += calculateWeaponCompatibility(mob, maid);
            score += calculateDangerModifier(mob, maid, master);
            score += calculateDistributionModifier(mob, otherMaids);
            score += calculateAssignmentModifier(mob, otherMaids, combatSettings);
            score += calculateMasterStanceModifier(mob, master, combatSettings);

            priorities.put(mob, score);
        }

        return priorities;
    }

    /**
     * 基本優先度を階層化に基づいて算出
     */
    public static double calculateBasePriority(Mob mob, Maid maid, Master master, List<Maid> otherMaids) {
        double distanceToMaid = mob.getPosition().distanceTo(maid.getPosition());

        // 特別優先度: 危険敵からの避難
        if (mob.isDangerous() && mob.isTargeting(maid)) {
            return TargetingConfig.getPriorityEvacuation();  // 避難優先（負の値で戦闘回避）
        }

        // 優先度1: 攻撃してきた相手
        if (mob.isTargeting(maid)) {
            return TargetingConfig.getPrioritySelfAttacker();
        }

        // 優先度2: ご主人を攻撃した相手
        if (master != null && mob.isTargeting(master)) {
            return TargetingConfig.getPriorityMasterAttacker();
        }

        // 優先度3: 他メイドさんを攻撃した相手
        for (Maid otherMaid : otherMaids) {
            if (mob.isTargeting(otherMaid)) {
                return TargetingConfig.getPriorityMaidAttacker();
            }
        }

        // 優先度4: ご主人が攻撃した相手
        if (master != null && master.isTargeting(mob)) {
            return TargetingConfig.getPriorityMasterTarget();
        }

        // 優先度5: 負傷メイドさんの支援・カバー
        for (Maid otherMaid : otherMaids) {
            if (otherMaid.getHealthPercent() < TargetingConfig.getInjuredHealthThreshold() && otherMaid.getCurrentTarget().isPresent() &&
                    otherMaid.getCurrentTarget().get().equals(mob)) {
                return TargetingConfig.getPriorityInjuredSupport();
            }
        }

        // 優先度6: 周囲のMob（危険敵は先制攻撃しない）
        if (mob.isDangerous()) {
            return 0;  // 先制攻撃しない
        } else {
            // 先制攻撃距離制限
            if (distanceToMaid > TargetingConfig.getMaxPreemptiveDistance()) {
                return 0;  // 先制攻撃範囲外
            }

            // 敵対モブは常にターゲット対象
            if (mob.isEnemy()) {
                return TargetingConfig.getPriorityNormalEnemy();
            }

            // 中立モブは戦闘状態の場合のみターゲット対象
            if (isNeutralMobTargetable(mob, maid, otherMaids, master)) {
                return TargetingConfig.getPriorityNormalEnemy();
            }

            return 0; // 中立モブで条件を満たさない場合はターゲット対象外
        }
    }

    /**
     * 距離に基づく優先度修正
     */
    public static double calculateDistanceModifier(Mob mob, Maid maid, @Nullable Master master) {
        double distanceToMaid = mob.getPosition().distanceTo(maid.getPosition());

        // 最大ターゲット距離チェック
        if (distanceToMaid > TargetingConfig.getMaxTargetDistance()) {
            return TargetingConfig.getDistanceLimitPenalty();  // 距離が遠すぎる場合は大幅減点
        }

        // 近距離ボーナス（メイドさんに近いほど高優先度）
        double distanceBonus = Math.max(0, (TargetingConfig.getDistanceBonusBaseDistance() - distanceToMaid) * TargetingConfig.getDistanceBonusMultiplier());

        // ご主人との距離考慮（ご主人から離れすぎた敵は減点）
        double masterDistancePenalty = 0;
        if (master != null) {
            double distanceToMaster = mob.getPosition().distanceTo(master.getPosition());
            masterDistancePenalty = Math.max(0, (distanceToMaster - TargetingConfig.getMasterDistancePenaltyBaseDistance()) * TargetingConfig.getMasterDistancePenaltyMultiplier());
        }

        return distanceBonus - masterDistancePenalty;
    }

    /**
     * 武器とターゲットの相性による修正
     */
    public static double calculateWeaponCompatibility(Mob mob, Maid maid) {
        double distanceToEnemy = mob.getPosition().distanceTo(maid.getPosition());

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
            if (distanceToEnemy <= TargetingConfig.getSwordCloseRangeThreshold()) {  // 近距離ボーナス
                // 危険敵への近接リスク
                if (mob.isDangerous()) {
                    return TargetingConfig.getSwordDangerPenalty();  // 危険敵に近づくリスクペナルティ
                }
                return TargetingConfig.getSwordCloseRangeBonus();
            } else {
                return TargetingConfig.getSwordLongRangePenalty();  // 遠距離ペナルティ
            }
        }

        return 0;
    }

    /**
     * 危険な敵に対する特殊修正
     */
    public static double calculateDangerModifier(Mob mob, Maid maid, @Nullable Master master) {
        if (!mob.isDangerous()) {
            return 0;
        }

        double distanceToEnemy = mob.getPosition().distanceTo(maid.getPosition());

        // 危険敵への基本ペナルティ
        double dangerPenalty = TargetingConfig.getDangerBasePenalty();

        // 近距離武器での危険敵は更にペナルティ
        if (maid.getCombatType() == CombatType.SWORD && distanceToEnemy < TargetingConfig.getDangerCloseRangeThreshold()) {
            dangerPenalty += TargetingConfig.getSwordDangerClosePenalty();
        }

        // 攻撃されている場合は例外的に対応
        if (mob.isTargeting(maid) || (master != null && mob.isTargeting(master))) {
            dangerPenalty += TargetingConfig.getDangerTargetExceptionBonus();  // ペナルティ軽減
        }

        return dangerPenalty;
    }

    /**
     * 他メイドさんとの分散攻撃を考慮した修正
     */
    public static double calculateDistributionModifier(Mob mob, List<Maid> otherMaids) {
        // 同じ敵をターゲットしている他メイドさんの数
        long targetingCount = otherMaids.stream()
                .filter(maid -> maid.getCurrentTarget().isPresent()
                        && maid.getCurrentTarget().get().equals(mob))
                .count();

        // 負傷メイドさん支援の詳細チェック
        List<Maid> injuredMaidsTargeting = otherMaids.stream()
                .filter(maid -> maid.getHealthPercent() < TargetingConfig.getInjuredHealthThreshold()
                        && maid.getCurrentTarget().isPresent() &&
                        maid.getCurrentTarget().get().equals(mob))
                .toList();

        List<Maid> healthyMaidsTargeting = otherMaids.stream()
                .filter(maid -> maid.getHealthPercent() >= TargetingConfig.getInjuredHealthThreshold()
                        && maid.getCurrentTarget().isPresent() &&
                        maid.getCurrentTarget().get().equals(mob))
                .toList();

        if (!injuredMaidsTargeting.isEmpty()) {
            // 負傷メイドさんがいる場合：健康なメイドさん1体のみ支援を許可
            if (healthyMaidsTargeting.isEmpty()) {
                return 0;  // 支援なし：ペナルティなし
            } else if (healthyMaidsTargeting.size() == 1) {
                return TargetingConfig.getInjuredSupportLightPenalty();  // 支援1体：軽いペナルティ
            } else {
                return TargetingConfig.getInjuredSupportHeavyPenalty();  // 過剰支援：重いペナルティ（強化）
            }
        }

        // 通常の集中攻撃回避（強化）
        double distributionPenalty = targetingCount * TargetingConfig.getDistributionPenaltyMultiplier();  // 50 -> 80に強化

        return -distributionPenalty;
    }

    /**
     * 敵1体への対応人数制御による修正
     */
    public static double calculateAssignmentModifier(Mob mob, List<Maid> otherMaids, CombatSettings settings) {
        // 同じ敵をターゲットしている他メイドさんの数
        long targetingCount = otherMaids.stream()
                .filter(maid -> maid.getCurrentTarget().isPresent() && maid.getCurrentTarget().get().equals(mob))
                .count();

        // 対応人数上限チェック
        if (targetingCount >= settings.getMaxMaidsPerEnemy()) {
            // 上限到達時は大幅ペナルティ
            long excessCount = targetingCount - settings.getMaxMaidsPerEnemy() + 1;
            return -excessCount * TargetingConfig.getAssignmentExcessPenalty();
        }

        return 0;  // 上限内は修正なし
    }

    /**
     * ご主人のスタンスに基づく優先度修正
     */
    public static double calculateMasterStanceModifier(Mob mob, @Nullable Master master, CombatSettings settings) {
        if (master == null || settings.getMasterStance() == MasterStance.GUARD) {
            // 主人不在または護衛モード：現状維持（修正なし）
            return 0;
        } else if (settings.getMasterStance() == MasterStance.SUPPORT) {
            // 援護モード：ご主人の戦闘を邪魔しない
            if (master.isTargeting(mob)) {
                // ご主人がターゲットしている敵は低優先度に
                return TargetingConfig.getSupportModeMasterTargetPenalty();  // 700→300に減少させる
            } else {
                // ご主人以外の敵は優先度アップ
                return TargetingConfig.getSupportModeOtherEnemyBonus();   // 500→600に増強
            }
        }

        return 0;
    }

    /**
     * 中立モブがターゲット対象となるかチェック
     * 条件：他の味方が攻撃している場合
     *
     * @param mob        判断対象のモブ
     * @param maid       判断するメイドさん
     * @param otherMaids 他のメイドさんのリスト
     * @param master     ご主人の情報
     * @return ターゲット対象の場合true
     */
    private static boolean isNeutralMobTargetable(Mob mob, Maid maid, List<Maid> otherMaids, @Nullable Master master) {
        // 他のメイドが攻撃している場合
        boolean otherMaidTargeting = otherMaids.stream().anyMatch(otherMaid ->
                otherMaid.getCurrentTarget().isPresent() &&
                        otherMaid.getCurrentTarget().get().equals(mob));

        return otherMaidTargeting;
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
                .anyMatch(mob -> mob.isDangerous() && mob.isTargeting(maid));
    }

    /**
     * 最も危険な敵（避難対象）を取得（危険敵からの避難のみ）
     *
     * @param maid    判断するメイドさん
     * @param enemies 周囲の敵リスト
     * @return 最も危険な敵のOptional（避難不要の場合はempty）
     */
    public static Optional<Mob> getMostDangerousEnemy(Maid maid, List<Mob> enemies) {
        return enemies.stream()
                .filter(mob -> mob.isDangerous() && mob.isTargeting(maid))
                .min(Comparator.comparingDouble(e -> e.getPosition().distanceTo(maid.getPosition()))); // 最も近い危険敵を優先
    }
}