package net.sistr.littlemaidrebirth.entity.util;

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
        return 16;
    }

    /**
     * 最大ターゲット可能距離
     */
    public static int getMaxTargetDistance() {
        return 24;
    }

    // ========== 優先度関連設定 ==========

    /**
     * 避難優先度（危険敵からの回避）
     */
    public static int getPriorityEvacuation() {
        return -1000;
    }

    /**
     * 自分を攻撃した相手への優先度
     */
    public static int getPrioritySelfAttacker() {
        return 1000;
    }

    /**
     * ご主人を攻撃した相手への優先度
     */
    public static int getPriorityMasterAttacker() {
        return 900;
    }

    /**
     * 他のメイドさんを攻撃した相手への優先度
     */
    public static int getPriorityMaidAttacker() {
        return 800;
    }

    /**
     * ご主人が攻撃した相手への優先度
     */
    public static int getPriorityMasterTarget() {
        return 700;
    }

    /**
     * 負傷メイドさんの支援優先度
     */
    public static int getPriorityInjuredSupport() {
        return 600;
    }

    /**
     * 通常の敵への優先度
     */
    public static int getPriorityNormalEnemy() {
        return 500;
    }

    // ========== 武器関連設定 ==========

    /**
     * 弓の遠距離ボーナス適用閾値
     */
    public static int getBowLongRangeThreshold() {
        return 8;
    }

    /**
     * 剣の近距離ボーナス適用閾値
     */
    public static int getSwordCloseRangeThreshold() {
        return 6;
    }

    // ========== 計算係数設定 ==========

    /**
     * 距離ボーナスの倍率
     */
    public static double getDistanceBonusMultiplier() {
        return 5.0;
    }

    /**
     * ご主人距離ペナルティの倍率
     */
    public static double getMasterDistancePenaltyMultiplier() {
        return 2.0;
    }

    /**
     * 弓の遠距離ボーナス
     */
    public static double getBowLongRangeBonus() {
        return 50.0;
    }

    /**
     * 弓の近距離ペナルティ
     */
    public static double getBowCloseRangePenalty() {
        return -10.0;
    }

    /**
     * 剣の近距離ボーナス
     */
    public static double getSwordCloseRangeBonus() {
        return 20.0;
    }

    /**
     * 剣の遠距離ペナルティ
     */
    public static double getSwordLongRangePenalty() {
        return -5.0;
    }

    /**
     * 危険敵への剣での近接ペナルティ
     */
    public static double getSwordDangerPenalty() {
        return -30.0;
    }

    /**
     * 危険敵への基本ペナルティ
     */
    public static double getDangerBasePenalty() {
        return -100.0;
    }

    /**
     * 剣使用時の危険敵近接追加ペナルティ
     */
    public static double getSwordDangerClosePenalty() {
        return -200.0;
    }

    /**
     * 危険敵に攻撃されている場合の例外ボーナス
     */
    public static double getDangerTargetExceptionBonus() {
        return 300.0;
    }

    /**
     * 分散攻撃ペナルティの倍率
     */
    public static double getDistributionPenaltyMultiplier() {
        return 80.0;
    }

    /**
     * 負傷支援時の軽いペナルティ
     */
    public static double getInjuredSupportLightPenalty() {
        return -25.0;
    }

    /**
     * 負傷支援時の重いペナルティ
     */
    public static double getInjuredSupportHeavyPenalty() {
        return -120.0;
    }

    /**
     * 割り当て人数超過時のペナルティ
     */
    public static double getAssignmentExcessPenalty() {
        return 100.0;
    }

    /**
     * 援護モード時のご主人ターゲットペナルティ
     */
    public static double getSupportModeMasterTargetPenalty() {
        return -400.0;
    }

    /**
     * 援護モード時の他敵ボーナス
     */
    public static double getSupportModeOtherEnemyBonus() {
        return 100.0;
    }

    /**
     * 距離制限によるペナルティ
     */
    public static double getDistanceLimitPenalty() {
        return -500.0;
    }

    /**
     * 弾が当たらない敵への戦闘回避ペナルティ
     */
    public static double getNonProjectileHitPenalty() {
        return -1000.0;
    }

    // ========== 距離判定関連設定 ==========

    /**
     * 距離ボーナス計算の基準距離
     */
    public static double getDistanceBonusBaseDistance() {
        return 10.0;
    }

    /**
     * 主人との距離ペナルティ基準距離
     */
    public static double getMasterDistancePenaltyBaseDistance() {
        return 10.0;
    }

    /**
     * 危険敵への近接判定距離
     */
    public static double getDangerCloseRangeThreshold() {
        return 5.0;
    }

    // ========== 体力判定関連設定 ==========

    /**
     * 負傷判定の体力閾値
     */
    public static float getInjuredHealthThreshold() {
        return 0.5f;
    }

    // ========== エンティティ検索関連設定 ==========

    /**
     * エンティティ検索範囲（水平方向）
     */
    public static double getEntitySearchRangeHorizontal() {
        return 8.0;
    }

    /**
     * エンティティ検索範囲（垂直方向）
     */
    public static double getEntitySearchRangeVertical() {
        return 4.0;
    }
}