package net.sistr.littlemaidrebirth.entity.util;

import net.sistr.littlemaidrebirth.LMRBMod;

/**
 * メイドさんターゲティングシステムの設定クラス
 * 将来的にコンフィグファイルから読み込み可能にするため、メソッドベースで値を提供
 */
public class TargetingConfig {

    // ========== 距離関連設定 ==========

    /**
     * 最大先制攻撃距離
     */
    public static int getMaxPreemptiveDistance() {
        return LMRBMod.getConfig().target.maxPreemptiveDistance;
    }

    /**
     * 最大ターゲット可能距離
     */
    public static int getMaxTargetDistance() {
        return LMRBMod.getConfig().target.maxTargetDistance;
    }

    // ========== 優先度関連設定 ==========

    /**
     * 避難優先度（危険敵からの回避）
     */
    public static int getPriorityEvacuation() {
        return LMRBMod.getConfig().advancedTarget.priorityEvacuation;
    }

    /**
     * 自分を攻撃した相手への優先度
     */
    public static int getPrioritySelfAttacker() {
        return LMRBMod.getConfig().advancedTarget.prioritySelfAttacker;
    }

    /**
     * ご主人を攻撃した相手への優先度
     */
    public static int getPriorityMasterAttacker() {
        return LMRBMod.getConfig().advancedTarget.priorityMasterAttacker;
    }

    /**
     * 他のメイドさんを攻撃した相手への優先度
     */
    public static int getPriorityMaidAttacker() {
        return LMRBMod.getConfig().advancedTarget.priorityMaidAttacker;
    }

    /**
     * ご主人が攻撃した相手への優先度
     */
    public static int getPriorityMasterTarget() {
        return LMRBMod.getConfig().advancedTarget.priorityMasterTarget;
    }

    public static int getPriorityMaidTarget() {
        return LMRBMod.getConfig().advancedTarget.priorityMaidTarget;
    }

    /**
     * 通常の敵への優先度
     */
    public static int getPriorityNormalEnemy() {
        return LMRBMod.getConfig().advancedTarget.priorityNormalEnemy;
    }

    // ========== 武器関連設定 ==========

    /**
     * 弓の遠距離ボーナス適用閾値
     */
    public static int getBowLongRangeThreshold() {
        return LMRBMod.getConfig().target.bowLongRangeThreshold;
    }

    /**
     * 剣の近距離ボーナス適用閾値
     */
    public static int getSwordCloseRangeThreshold() {
        return LMRBMod.getConfig().target.swordCloseRangeThreshold;
    }

    // ========== 計算係数設定 ==========

    /**
     * メイドさん距離ペナルティの倍率
     */
    public static float getMaidDistancePenaltyMultiplier() {
        return LMRBMod.getConfig().target.maidDistancePenaltyMultiplier;
    }

    /**
     * ご主人距離ボーナスの倍率
     */
    public static float getMasterDistanceBonusMultiplier() {
        return LMRBMod.getConfig().target.masterDistanceBonusMultiplier;
    }

    /**
     * 弓の遠距離ボーナス
     */
    public static int getBowLongRangeBonus() {
        return LMRBMod.getConfig().advancedTarget.bowLongRangeBonus;
    }

    /**
     * 弓の近距離ペナルティ
     */
    public static int getBowCloseRangePenalty() {
        return LMRBMod.getConfig().advancedTarget.bowCloseRangePenalty;
    }

    /**
     * 剣の近距離ボーナス
     */
    public static int getSwordCloseRangeBonus() {
        return LMRBMod.getConfig().advancedTarget.swordCloseRangeBonus;
    }

    /**
     * 剣の遠距離ペナルティ
     */
    public static int getSwordLongRangePenalty() {
        return LMRBMod.getConfig().advancedTarget.swordLongRangePenalty;
    }

    /**
     * 危険敵への剣での近接ペナルティ
     */
    public static int getSwordDangerPenalty() {
        return LMRBMod.getConfig().advancedTarget.swordDangerPenalty;
    }

    /**
     * 危険敵への基本ペナルティ
     */
    public static int getDangerBasePenalty() {
        return LMRBMod.getConfig().advancedTarget.dangerBasePenalty;
    }
    /**
     * 危険敵近接追加ペナルティ
     */
    public static int getDangerClosePenalty() {
        return LMRBMod.getConfig().advancedTarget.dangerClosePenalty;
    }

    /**
     * 分散攻撃ペナルティの倍率
     */
    public static float getDistributionPenaltyMultiplier() {
        return LMRBMod.getConfig().target.distributionPenaltyMultiplier;
    }

    /**
     * 援護モード時のご主人ターゲットペナルティ
     */
    public static int getSupportModeMasterTargetPenalty() {
        return LMRBMod.getConfig().advancedTarget.supportModeMasterTargetPenalty;
    }

    /**
     * 援護モード時の他敵ボーナス
     */
    public static int getSupportModeOtherEnemyBonus() {
        return LMRBMod.getConfig().advancedTarget.supportModeOtherEnemyBonus;
    }

    /**
     * 距離制限によるペナルティ
     */
    public static int getDistanceLimitPenalty() {
        return LMRBMod.getConfig().advancedTarget.distanceLimitPenalty;
    }

    /**
     * 弾が当たらない敵への戦闘回避ペナルティ
     */
    public static int getNonProjectileHitPenalty() {
        return LMRBMod.getConfig().advancedTarget.nonProjectileHitPenalty;
    }

    // ========== 距離判定関連設定 ==========

    /**
     * メイドさんとの距離ペナルティ基準距離
     */
    public static float getMaidDistancePenaltyBaseDistance() {
        return LMRBMod.getConfig().target.maidDistancePenaltyBaseDistance;
    }

    /**
     * 主人との距離ボーナス計算の基準距離
     */
    public static float getMasterDistanceBonusBaseDistance() {
        return LMRBMod.getConfig().target.masterDistanceBonusBaseDistance;
    }

    /**
     * 危険敵への近接判定距離
     */
    public static float getDangerCloseRangeThreshold() {
        return LMRBMod.getConfig().target.dangerCloseRangeThreshold;
    }

    // ========== 体力判定関連設定 ==========

    /**
     * 負傷判定の体力閾値
     */
    public static float getInjuredHealthThreshold() {
        return LMRBMod.getConfig().target.injuredHealthThreshold;
    }

    /**
     * 負傷したご主人の攻撃者への優先度ボーナス
     */
    public static int getInjuredMasterAttackerBonus() {
        return LMRBMod.getConfig().advancedTarget.injuredMasterAttackerBonus;
    }

    /**
     * 負傷したメイドさんの攻撃者への優先度ボーナス
     */
    public static int getInjuredMaidAttackerBonus() {
        return LMRBMod.getConfig().advancedTarget.injuredMaidAttackerBonus;
    }

}