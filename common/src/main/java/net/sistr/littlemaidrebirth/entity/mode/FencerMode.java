package net.sistr.littlemaidrebirth.entity.mode;

import java.util.Optional;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.util.ReachAttributeUtil;

public class FencerMode extends AbstractFencerMode<Item> {
  protected final LittleMaidEntity mob;
  protected int cooldown;

  // todo 相手が無敵時間中は殴らない
  public FencerMode(
      ModeType<? extends FencerMode> modeType, String name, LittleMaidEntity mob, float speed) {
    super(mob, modeType, name, speed);
    this.mob = mob;
  }

  @Override
  public void startExecuting() {
    this.mob.play(LMSounds.FIND_TARGET_N);
  }

  @Override
  public void tick() {
    this.cooldown = Math.max(0, this.cooldown - 1);
    super.tick();
  }

  @Override
  protected void attack() {
    resetCooldown();
    this.mob.swingHand(Hand.MAIN_HAND);
    this.mob.tryAttack(target);
  }

  protected void resetCooldown() {
    double attackSpeed = this.mob.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED);
    this.cooldown =
        MathHelper.ceil(1 / attackSpeed * 20 / LMRBMod.getConfig().work.fencerAttackRateFactor);
  }

  @Override
  protected boolean canAttack() {
    return this.cooldown <= 0 && super.canAttack();
  }

  @Override
  protected boolean isClose(double distanceSq) {
    return distanceSq
        < ReachAttributeUtil.getAttackRangeSq(mob)
            * LMRBMod.getConfig().work.fencerAttackDistanceFactor;
  }

  @Override
  protected Optional<Item> getWeaponInstance(ItemStack stack) {
    return Optional.of(stack.getItem());
  }
}
