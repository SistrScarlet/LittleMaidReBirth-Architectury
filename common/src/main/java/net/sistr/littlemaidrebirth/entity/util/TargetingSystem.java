package net.sistr.littlemaidrebirth.entity.util;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * メイドさん統合ターゲティングシステム
 * 3段階優先度システム（CRITICAL > HIGH > NORMAL）
 */
public class TargetingSystem {

    /**
     * 3段階ターゲット優先度
     */
    public enum TargetPriority {
        CRITICAL,  // 自分の身を守る
        HIGH,       // ご主人の身を守る
        NORMAL     // 味方の身を守る・周囲の敵
    }

    /**
     * 敵タグ（特殊な対応が必要な敵の分類）
     */
    public enum EnemyTag {
        APPROACH_PROHIBITED,     // 接近禁止
        ATTACK_PROHIBITED,       // 攻撃禁止
        MELEE_ATTACK_PROHIBITED, // 近距離攻撃禁止
        RANGED_ATTACK_PROHIBITED // 遠距離攻撃禁止
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
            return this.entity.getHealth() / this.entity.getMaxHealth() < TargetingConfig.getInjuredThreshold();
        }

        public Vec3d getPosition() {
            return this.entity.getPos();
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

        public Mode.BattleModeType getCombatType() {
            return maid.getMode()
                    .map(mode -> mode.getBattleModeType())
                    .orElse(Mode.BattleModeType.NONE);
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

    /**
     * 3段階優先度システムでターゲットを選択
     *
     * @param maid       判断するメイドさん
     * @param enemies    周囲の敵リスト
     * @param master     ご主人の情報（nullの場合は主人関連の判定をスキップ）
     * @param otherMaids 他のメイドさんのリスト
     * @return 選択されたターゲット（ない場合はOptional.empty()）
     */
    public static Optional<MobEntity> selectTarget(
            Maid maid, List<Mob> enemies, @Nullable Master master, List<Maid> otherMaids) {

        int maxAttackers = calculateMaxAttackers(otherMaids);

        // 全敵を評価し、攻撃可能な敵を優先度付きで収集
        Optional<Mob> bestTarget = enemies.stream()
                .filter(enemy -> !shouldAvoidDangerous(enemy, maid))
                .filter(enemy -> !isOverTargeted(enemy, otherMaids, maxAttackers))
                .map(enemy -> new TargetCandidate(enemy, determinePriority(enemy, maid, master, otherMaids)))
                .filter(candidate -> candidate.priority != null)
                .min(Comparator
                        .comparing((TargetCandidate c) -> c.priority)
                        .thenComparingDouble(c -> c.enemy.getPosition().distanceTo(maid.getPosition())))
                .map(candidate -> candidate.enemy);

        return bestTarget.map(Mob::getMob);
    }

    private record TargetCandidate(Mob enemy, TargetPriority priority) {
    }

    /**
     * 3段階優先度判定
     *
     * @param enemy      判定対象の敵
     * @param maid       判断するメイドさん
     * @param master     ご主人の情報（null可）
     * @param otherMaids 他のメイドさんのリスト
     * @return 優先度（CRITICAL > HIGH > NORMAL）またはnull（ターゲット対象外）
     */
    private static TargetPriority determinePriority(Mob enemy, Maid maid, Master master, List<Maid> otherMaids) {
        // CRITICAL: 自分の身を守る
        if (maid.isAttackedBy(enemy)) {
            return TargetPriority.CRITICAL;
        }

        // HIGH: ご主人の身を守る
        if (master != null && (master.isAttackedBy(enemy) || master.isTargeting(enemy) || enemy.isAttackedBy(master))) {
            return TargetPriority.HIGH;
        }

        // NORMAL: 味方の身を守る・周囲の敵
        for (Maid otherMaid : otherMaids) {
            if (otherMaid.isAttackedBy(enemy) || otherMaid.isTargeting(enemy)) {
                return TargetPriority.NORMAL;
            }
        }

        // 先制攻撃対象（距離内の敵対モブ、攻撃禁止敵以外）
        if (enemy.isEnemy()) {
            // 攻撃禁止敵は攻撃対象外（常に避けるため）
            if (hasAttackProhibitedTag(enemy)) {
                return null;
            }

            float distanceToMaid = (float) enemy.getPosition().distanceTo(maid.getPosition());
            if (distanceToMaid <= TargetingConfig.getAlertRange()) {
                return TargetPriority.NORMAL;
            }
        }

        return null; // ターゲット対象外
    }

    /**
     * 分散ターゲティング判定（攻撃者が上限を越えているか）
     * 集中攻撃を防ぎ、メイドさんたちがバランス良く敵を攻撃するための判定
     *
     * @param enemy        判定対象の敵
     * @param otherMaids   他のメイドさんのリスト
     * @param maxAttackers 最大攻撃者数
     * @return 上限を越えているtrue
     */
    private static boolean isOverTargeted(Mob enemy, List<Maid> otherMaids, int maxAttackers) {
        long currentAttackers = otherMaids.stream()
                .filter(maid -> maid.isTargeting(enemy))
                .filter(maid -> maid.getCombatType() != Mode.BattleModeType.NONE)
                .filter(maid -> !maid.isInjured())
                .count();

        return currentAttackers >= maxAttackers;
    }

    /**
     * 敵を回避すべきかの判定
     * 新敵タグシステムに基づいて攻撃可能性を判定
     *
     * @param enemy 判定対象の敵
     * @param maid  判断するメイドさん
     * @return 回避すべき場合true
     */
    private static boolean shouldAvoidDangerous(Mob enemy, Maid maid) {
        // 攻撃禁止敵は常に回避
        if (hasAttackProhibitedTag(enemy)) {
            return true;
        }

        // 接近禁止敵は武器に応じて判定
        if (hasApproachProhibitedTag(enemy)) {
            // 弓持ちの場合は遠距離から攻撃可能（ただし遠距離攻撃禁止でない場合のみ）
            if (maid.getCombatType() == Mode.BattleModeType.BOW && !hasRangedAttackProhibitedTag(enemy)) {
                float distance = (float) enemy.getPosition().distanceTo(maid.getPosition());
                return distance < TargetingConfig.getCombatRange(); // 近い場合は回避
            }
            // 剣持ちの場合は基本的に回避
            return maid.getCombatType() == Mode.BattleModeType.SWORD;
        }

        // 武器種別に応じた攻撃禁止判定
        if (maid.getCombatType() == Mode.BattleModeType.SWORD && hasMeleeAttackProhibitedTag(enemy)) {
            return true; // 近距離攻撃禁止敵は剣で攻撃不可
        }

        if (maid.getCombatType() == Mode.BattleModeType.BOW && hasRangedAttackProhibitedTag(enemy)) {
            return true; // 遠距離攻撃禁止敵は弓で攻撃不可
        }

        return false;
    }


    /**
     * 最大攻撃者数を計算（min(3体, ceil(メイドさん数×50%))）
     * 集中攻撃を防ぐための上限値をメイドさんの数に応じて計算
     *
     * @param otherMaids 他のメイドさんのリスト
     * @return 一体の敵あたりの最大攻撃者数
     */
    private static int calculateMaxAttackers(List<Maid> otherMaids) {
        int totalMaids = otherMaids.size() + 1; // 自分も含める
        int distributedCount = (int) Math.ceil(totalMaids * TargetingConfig.getDistributionRatio());
        return Math.min(TargetingConfig.getMaxAttackersPerEnemy(), distributedCount);
    }

    /**
     * 攻撃禁止タグの判定（一切攻撃してはいけない敵）
     * TODO: IFFシステム統合または設定ファイルで管理するように変更
     */
    private static boolean hasAttackProhibitedTag(Mob enemy) {
        EntityType<?> type = enemy.getMob().getType();
        return type == EntityType.WITHER ||
                type == EntityType.ENDER_DRAGON ||
                type == EntityType.WARDEN;
    }

    /**
     * 接近禁止タグの判定（弓持ちなら遠距離攻撃可能）
     * TODO: IFFシステム統合または設定ファイルで管理するように変更
     */
    private static boolean hasApproachProhibitedTag(Mob enemy) {
        EntityType<?> type = enemy.getMob().getType();
        return type == EntityType.CREEPER;
    }

    /**
     * 近距離攻撃禁止タグの判定（剣などでは攻撃できない敵）
     * TODO: IFFシステム統合または設定ファイルで管理するように変更
     */
    private static boolean hasMeleeAttackProhibitedTag(Mob enemy) {
        EntityType<?> type = enemy.getMob().getType();
        return type == EntityType.WITHER_SKELETON ||
                type == EntityType.RAVAGER;
    }

    /**
     * 遠距離攻撃禁止タグの判定（弓では攻撃できない敵）
     * TODO: IFFシステム統合または設定ファイルで管理するように変更
     */
    private static boolean hasRangedAttackProhibitedTag(Mob enemy) {
        return enemy.getMob().getType() == EntityType.ENDERMAN;
    }

    /**
     * 避難が必要かどうかを判定（攻撃禁止敵・接近禁止敵からの避難）
     *
     * @param maid    判断するメイドさん
     * @param enemies 周囲の敵リスト
     * @return 避難が必要な場合true
     */
    public static boolean needsEvacuation(Maid maid, List<Mob> enemies) {
        return enemies.stream()
                .anyMatch(enemy -> {
                    double distance = maid.getPosition().distanceTo(enemy.getPosition());
                    return (hasAttackProhibitedTag(enemy) || hasApproachProhibitedTag(enemy))
                            && distance < TargetingConfig.getDangerousAvoidDistance();
                });
    }

    /**
     * 危険な敵（避難対象）を取得
     *
     * @param maid    判断するメイドさん
     * @param enemies 周囲の敵リスト
     * @return 避難対象の敵のリスト（距離順）
     */
    public static List<Mob> getDangerousEnemies(Maid maid, List<Mob> enemies) {
        return enemies.stream()
                .filter(enemy -> hasAttackProhibitedTag(enemy) || hasApproachProhibitedTag(enemy))
                .filter(enemy -> maid.getPosition().distanceTo(enemy.getPosition())
                        < TargetingConfig.getDangerousAvoidDistance())
                .sorted(Comparator.comparingDouble(e -> e.getPosition().distanceTo(maid.getPosition())))
                .collect(Collectors.toList()); // 最も近い避難対象敵を優先
    }
}