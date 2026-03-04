package net.sistr.littlemaidrebirth.entity.goal;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManager;
import net.sistr.littlemaidrebirth.entity.targeting.TargetingConfig;
import net.sistr.littlemaidrebirth.entity.targeting.TargetingSystem;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

/**
 * メイドさんのターゲット選択ゴール 3段階優先度システムで敵を選択し、危険な敵からの避難も処理する
 *
 * <p>優先度階層: - CRITICAL: 自分を攻撃した敵 - HIGH: ご主人を攻撃した敵、ご主人が攻撃した敵 - NORMAL: 他のメイドさんを攻撃した敵、周囲の敵対モブ
 */
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
    int chance = 10; // todo コンフィグ化検討
    if (this.maid.getRandom().nextInt(getTickCount(chance)) != 0) {
      return false;
    }

    return targeting();
  }

  private boolean targeting() {
    // 範囲内に敵がいるかチェック
    var aroundMobs = getAroundMobs();
    if (aroundMobs.isEmpty()) {
      this.maid.setTarget(null);
      return false;
    }
    var aroundMaids = getAroundMaids();
    TargetTagManager targetTagManager = this.maid;

    // 3段階優先度システムでターゲット選択、分散ターゲティングも考慮
    var target =
        TargetingSystem.selectTarget(
            new TargetingSystem.Maid(this.maid),
            aroundMobs.stream().map(mob -> new TargetingSystem.Mob(mob)).toList(),
            TameableUtil.getTameOwner(this.maid).map(TargetingSystem.Master::new).orElse(null),
            aroundMaids.stream().map(TargetingSystem.Maid::new).toList(),
            this.maid.isBloodSuck(),
            targetTagManager);

    // 危険敵からの避難処理（クリーパー等から距離を取る）
    var enemies = aroundMobs.stream().map(mob -> new TargetingSystem.Mob(mob)).toList();
    var maidWrapper = new TargetingSystem.Maid(this.maid);
    if (TargetingSystem.needsEvacuation(maidWrapper, enemies, targetTagManager)) {
      TargetingSystem.getDangerousEnemies(maidWrapper, enemies, targetTagManager)
          .forEach(
              mob ->
                  this.maid.addFleeEntity(
                      mob.getMob(),
                      e ->
                          !e.isAlive()
                              || this.maid.squaredDistanceTo(e)
                                  > (TargetingConfig.getDangerousAvoidDistance() + 4)
                                      * (TargetingConfig.getDangerousAvoidDistance() + 4)));
    }

    // ターゲット設定
    if (target.isPresent()) {
      this.target = target.get();
      this.maid.setTarget(target.get());
      return true;
    }

    this.maid.setTarget(null);
    return false;
  }

  @Override
  public boolean shouldContinue() {
    // 攻撃を受けたら再計算(tick順の関係で実行されないことを防ぐため、ageに-1する)
    if (getTickCount(this.maid.getLastAttackedTime()) == getTickCount(this.maid.age - 1)) {
      return targeting();
    }
    // 現在のターゲットがまだ有効かチェック
    if (!isTargetable(this.target, TargetingConfig.getAlertRange())) {
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
    this.maid.setTarget(null);
  }

  private List<MobEntity> getAroundMobs() {
    float distance = TargetingConfig.getAlertRange();
    return this.maid
        .getWorld()
        .getEntitiesByClass(
            MobEntity.class,
            this.maid.getBoundingBox().expand(distance, distance / 2f, distance).expand(1),
            mob ->
                mob != this.maid
                    && isTargetable(mob, distance)
                    && this.maid.getVisibilityCache().canSee(mob));
  }

  private boolean isTargetable(MobEntity mob, float distance) {
    return this.maid.squaredDistanceTo(mob) <= distance * distance
        && maid.canTarget(mob) // isFriend()とcanTakeDamage()判定込み
        && mob.isAlive();
  }

  private List<LittleMaidEntity> getAroundMaids() {
    float distance = TargetingConfig.getAlertRange();
    return this.maid
        .getWorld()
        .getEntitiesByClass(
            LittleMaidEntity.class,
            this.maid.getBoundingBox().expand(distance, distance / 2f, distance).expand(1),
            maid -> maid != this.maid);
  }
}
