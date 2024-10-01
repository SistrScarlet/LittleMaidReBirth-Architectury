package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.item.ItemStack;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

import java.util.function.Predicate;

public class LMHealMyselfGoal extends HealMyselfGoal<LittleMaidEntity> {
    public LMHealMyselfGoal(LittleMaidEntity mob, int healInterval, int healAmount, Predicate<ItemStack> healItemPred) {
        super(mob, healInterval, healAmount, healItemPred);
    }

    @Override
    public void heal(ItemStack healItem) {
        super.heal(healItem);
        var sound = isHealthFull() ? LMSounds.EAT_SUGAR_MAX_POWER : LMSounds.EAT_SUGAR;
        mob.play(sound);
    }
}
