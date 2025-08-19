package net.sistr.littlemaidrebirth.entity.targeting;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
        NORMAL     // 味方の身を守る・周囲の対象
    }

    /**
     * ターゲットタグ（特殊な対応が必要な対象の分類）
     */
    public enum TargetTag {
        APPROACH_PROHIBITED,         // 接近禁止
        ATTACK_PROHIBITED,           // 攻撃禁止
        PREEMPTIVE_ATTACK_PROHIBITED, // 先制攻撃禁止（反撃は可能）
        MELEE_WEAPON_PROHIBITED,     // 近距離攻撃禁止
        RANGED_WEAPON_PROHIBITED    // 遠距離攻撃禁止
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
            super(maid);
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

        public Mob(MobEntity mob) {
            super(mob);
            this.mob = mob;
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
     * @param maid                              判断するメイドさん
     * @param mobs                              周囲のモブリスト
     * @param master                            ご主人の情報（nullの場合は主人関連の判定をスキップ）
     * @param otherMaids                        他のメイドさんのリスト
     * @param ignorePreemptiveAttackProhibition 先制攻撃禁止を無視するか
     * @param targetTagManager                  ターゲットタグ管理インスタンス
     * @return 選択されたターゲット（ない場合はOptional.empty()）
     */
    public static Optional<MobEntity> selectTarget(
            Maid maid, List<Mob> mobs, @Nullable Master master, List<Maid> otherMaids, boolean ignorePreemptiveAttackProhibition, TargetTagManager targetTagManager) {
        int maxAttackers = calculateMaxAttackers(otherMaids);

        // 全対象を評価し、攻撃可能な対象を優先度付きで収集
        Optional<Mob> bestTarget = mobs.stream()
                .filter(target -> !shouldAvoidDangerous(target, maid, targetTagManager))
                .filter(target -> !isOverTargeted(target, otherMaids, maxAttackers))
                .map(target -> new TargetCandidate(target, determinePriority(target, maid, master, otherMaids, ignorePreemptiveAttackProhibition, targetTagManager)))
                .filter(candidate -> candidate.priority != null)
                .min(Comparator
                        .comparing((TargetCandidate c) -> c.priority)
                        .thenComparingDouble(c -> c.target.getPosition().distanceTo(maid.getPosition())))
                .map(candidate -> candidate.target);

        return bestTarget.map(Mob::getMob);
    }

    private record TargetCandidate(Mob target, TargetPriority priority) {
    }

    /**
     * 3段階優先度判定
     *
     * @param mob                               判定対象のモブ
     * @param maid                              判断するメイドさん
     * @param master                            ご主人の情報（null可）
     * @param otherMaids                        他のメイドさんのリスト
     * @param ignorePreemptiveAttackProhibition 先制攻撃禁止を無視するか
     * @param targetTagManager                  ターゲットタグ管理インスタンス
     * @return 優先度（CRITICAL > HIGH > NORMAL）またはnull（ターゲット対象外）
     */
    private static TargetPriority determinePriority(Mob mob, Maid maid, Master master, List<Maid> otherMaids, boolean ignorePreemptiveAttackProhibition, TargetTagManager targetTagManager) {
        // 攻撃禁止対象は攻撃対象外（常に避けるため）
        if (hasAttackProhibitedTag(mob, targetTagManager)) {
            return null;
        }

        // CRITICAL: 自分の身を守る
        if (maid.isAttackedBy(mob)) {
            return TargetPriority.CRITICAL;
        }

        // HIGH: ご主人の身を守る
        if (master != null && (master.isAttackedBy(mob))) {
            return TargetPriority.HIGH;
        }

        // NORMAL: 味方の身を守る
        for (Maid otherMaid : otherMaids) {
            if (otherMaid.isAttackedBy(mob)
                    || (master != null && master.isTargeting(mob))) {
                return TargetPriority.NORMAL;
            }
        }

        // 先制攻撃禁止対象は先制攻撃対象外（反撃は可能、ただし無視フラグがtrueの場合は除く）
        if (hasPreemptiveAttackProhibitedTag(mob, targetTagManager) && !ignorePreemptiveAttackProhibition) {
            return null;
        }

        // 警戒範囲内の対象への先制攻撃
        float distanceToMaid = (float) mob.getPosition().distanceTo(maid.getPosition());
        if (distanceToMaid <= TargetingConfig.getAlertRange()) {
            return TargetPriority.NORMAL;
        }

        return null; // ターゲット対象外
    }

    /**
     * 分散ターゲティング判定（攻撃者が上限を越えているか）
     * 集中攻撃を防ぎ、メイドさんたちがバランス良く対象を攻撃するための判定
     *
     * @param target       判定対象
     * @param otherMaids   他のメイドさんのリスト
     * @param maxAttackers 最大攻撃者数
     * @return 上限を越えているtrue
     */
    private static boolean isOverTargeted(Mob target, List<Maid> otherMaids, int maxAttackers) {
        long currentAttackers = otherMaids.stream()
                .filter(maid -> !TameableUtil.isWait(maid.maid))
                .filter(maid -> maid.isTargeting(target))
                .filter(maid -> maid.getCombatType() != Mode.BattleModeType.NONE)
                .filter(maid -> !maid.isInjured())
                .count();

        return currentAttackers >= maxAttackers;
    }

    /**
     * 対象を回避すべきかの判定
     * ターゲットタグシステムに基づいて攻撃可能性を判定
     *
     * @param target 判定対象のモブ
     * @param maid   判断するメイドさん
     * @return 回避すべき場合true
     */
    private static boolean shouldAvoidDangerous(Mob target, Maid maid, TargetTagManager targetTagManager) {
        // 攻撃禁止対象は常に回避
        if (hasAttackProhibitedTag(target, targetTagManager)) {
            return true;
        }

        // 接近禁止対象は武器に応じて判定
        if (hasApproachProhibitedTag(target, targetTagManager)) {
            // 弓持ちの場合は遠距離から攻撃可能（ただし遠距離攻撃禁止でない場合のみ）
            if (maid.getCombatType() == Mode.BattleModeType.BOW
                    && !hasRangedAttackProhibitedTag(target, targetTagManager)) {
                float distance = (float) target.getPosition().distanceTo(maid.getPosition());
                return distance < TargetingConfig.getCombatRange(); // 近い場合は回避
            }
            // 剣持ちの場合は基本的に回避
            return maid.getCombatType() == Mode.BattleModeType.SWORD;
        }

        // 武器種別に応じた攻撃禁止判定
        if (maid.getCombatType() == Mode.BattleModeType.SWORD
                && hasMeleeAttackProhibitedTag(target, targetTagManager)) {
            return true; // 近距離攻撃禁止対象は剣で攻撃不可
        }

        if (maid.getCombatType() == Mode.BattleModeType.BOW
                && hasRangedAttackProhibitedTag(target, targetTagManager)) {
            return true; // 遠距離攻撃禁止対象は弓で攻撃不可
        }

        return false;
    }

    /**
     * 最大攻撃者数を計算（min(3体, ceil(メイドさん数×50%))）
     * 集中攻撃を防ぐための上限値をメイドさんの数に応じて計算
     *
     * @param otherMaids 他のメイドさんのリスト
     * @return 一体の対象あたりの最大攻撃者数
     */
    private static int calculateMaxAttackers(List<Maid> otherMaids) {
        int totalMaids = otherMaids.size() + 1; // 自分も含める
        int distributedCount = (int) Math.ceil(totalMaids * TargetingConfig.getDistributionRatio());
        return Math.min(TargetingConfig.getMaxAttackersPerTarget(), distributedCount);
    }

    /**
     * エンティティからTargetTagセットを取得
     *
     * @param target           判定対象のモブ
     * @param targetTagManager ターゲットタグ管理インスタンス
     * @return 該当するTargetTagのセット
     */
    private static Set<TargetTag> getTargetTags(Mob target, TargetTagManager targetTagManager) {
        TargetIdentifier identifier = new TargetIdentifier(target.getMob());
        return targetTagManager.getTargetTag(identifier);
    }

    /**
     * 攻撃禁止タグの判定（一切攻撃してはいけない対象）
     */
    private static boolean hasAttackProhibitedTag(Mob target, TargetTagManager targetTagManager) {
        return getTargetTags(target, targetTagManager).contains(TargetTag.ATTACK_PROHIBITED);
    }

    /**
     * 接近禁止タグの判定（弓持ちなら遠距離攻撃可能）
     */
    private static boolean hasApproachProhibitedTag(Mob target, TargetTagManager targetTagManager) {
        return getTargetTags(target, targetTagManager).contains(TargetTag.APPROACH_PROHIBITED);
    }

    /**
     * 近距離攻撃禁止タグの判定（剣などでは攻撃できない対象）
     */
    private static boolean hasMeleeAttackProhibitedTag(Mob target, TargetTagManager targetTagManager) {
        return getTargetTags(target, targetTagManager).contains(TargetTag.MELEE_WEAPON_PROHIBITED);
    }

    /**
     * 遠距離攻撃禁止タグの判定（弓では攻撃できない対象）
     */
    private static boolean hasRangedAttackProhibitedTag(Mob target, TargetTagManager targetTagManager) {
        return getTargetTags(target, targetTagManager).contains(TargetTag.RANGED_WEAPON_PROHIBITED);
    }

    /**
     * 先制攻撃禁止タグの判定（先制攻撃はできないが反撃は可能）
     */
    private static boolean hasPreemptiveAttackProhibitedTag(Mob target, TargetTagManager targetTagManager) {
        return getTargetTags(target, targetTagManager).contains(TargetTag.PREEMPTIVE_ATTACK_PROHIBITED);
    }

    /**
     * 避難が必要かどうかを判定（接近禁止対象からの避難）
     *
     * @param maid 判断するメイドさん
     * @param mobs 周囲のモブリスト
     * @return 避難が必要な場合true
     */
    public static boolean needsEvacuation(Maid maid, List<Mob> mobs, TargetTagManager targetTagManager) {
        return mobs.stream()
                .anyMatch(target -> {
                    double distance = maid.getPosition().distanceTo(target.getPosition());
                    return hasApproachProhibitedTag(target, targetTagManager)
                            && distance < TargetingConfig.getDangerousAvoidDistance();
                });
    }

    /**
     * 避難対象を取得
     *
     * @param maid 判断するメイドさん
     * @param mobs 周囲のモブリスト
     * @return 避難対象のリスト（距離順）
     */
    public static List<Mob> getDangerousEnemies(Maid maid, List<Mob> mobs, TargetTagManager targetTagManager) {
        return mobs.stream()
                .filter(target -> hasApproachProhibitedTag(target, targetTagManager))
                .filter(target -> maid.getPosition().distanceTo(target.getPosition())
                        < TargetingConfig.getDangerousAvoidDistance())
                .sorted(Comparator.comparingDouble(e -> e.getPosition().distanceTo(maid.getPosition())))
                .collect(Collectors.toList()); // 最も近い避難対象を優先
    }
}