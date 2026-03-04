package net.sistr.littlemaidrebirth.entity.goal;

import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

public class LMHealMyselfGoal extends HealMyselfGoal<LittleMaidEntity> {
  public LMHealMyselfGoal(
      LittleMaidEntity mob,
      Supplier<Integer> healInterval,
      Supplier<Integer> healAmount,
      Predicate<ItemStack> healItemPred) {
    super(mob, healInterval, healAmount, healItemPred);
  }

  @Override
  public void heal(ItemStack healItem) {
    super.heal(healItem);
    var sound = isHealthFull() ? LMSounds.EAT_SUGAR_MAX_POWER : LMSounds.EAT_SUGAR;
    mob.play(sound);
  }
}
