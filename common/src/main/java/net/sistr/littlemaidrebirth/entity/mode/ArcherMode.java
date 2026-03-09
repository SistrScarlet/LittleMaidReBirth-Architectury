package net.sistr.littlemaidrebirth.entity.mode;

import java.util.Optional;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.api.mode.IRangedWeapon;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

public class ArcherMode extends AbstractArcherMode<Item> {
    protected int cool;

    public ArcherMode(ModeType<? extends ArcherMode> modeType, String name, LittleMaidEntity mob) {
        super(modeType, name, mob);
    }

    @Override
    public boolean shouldExecute() {
        return (!this.mob.getProjectileType(this.mob.getMainHandStack()).isEmpty()
                        || EnchantmentHelper.getLevel(
                                        Enchantments.INFINITY, this.mob.getMainHandStack())
                                > 0)
                && super.shouldExecute();
    }

    @Override
    protected void tickRangedAttack(
            LivingEntity target,
            ItemStack itemStack,
            boolean canSee,
            double distanceSq,
            float maxRange) {
        if (itemStack.getItem() instanceof BowItem) {
            if (0 < --cool) {
                if (cool == 0) {
                    mob.play(LMSounds.SIGHTING);
                }
                return;
            }
            if (!this.mob.isUsingItem()) {
                this.mob.setCurrentHand(Hand.MAIN_HAND);
            }
            int interval = getInterval(itemStack);
            if (interval <= this.mob.getItemUseTime()) {
                // 射線チェック、射線に味方が居る場合は撃たない
                var result =
                        this.raycastShootLine(
                                target,
                                maxRange,
                                e ->
                                        e instanceof LivingEntity living
                                                && !this.mob.canTarget(living));
                if (result.isPresent()) {
                    this.cool = 10;
                } else {
                    this.cool = 10;
                    this.mob.clearActiveItem();
                    this.mob.attack(target, 1.0f);
                    this.mob.play(LMSounds.SHOOT);
                    this.mob.swingHand(Hand.MAIN_HAND);
                    itemStack.damage(1, this.mob, e -> e.sendToolBreakStatus(Hand.MAIN_HAND));
                }
            }
        } else if (itemStack.getItem() instanceof CrossbowItem) {
            if (!CrossbowItem.isCharged(itemStack)) {
                // チャージ前か、チャージしていない
                if (!this.mob.isCharging() || !this.mob.isUsingItem()) {
                    this.mob.setCurrentHand(Hand.MAIN_HAND);
                    this.mob.setCharging(true);
                } else { // チャージ中
                    // チャージが終わった
                    if (this.mob.getItemUseTime() >= getInterval(itemStack)) {
                        // チャージはこのメソッドから行われる
                        this.mob.stopUsingItem();
                        this.mob.setCharging(false);
                        this.cool = 10;
                        mob.play(LMSounds.SIGHTING);
                        this.mob.swingHand(Hand.MAIN_HAND);
                    }
                }
            } else { // チャージ完了
                if (0 < --cool) {
                    return;
                }
                // 射線チェック
                var result =
                        raycastShootLine(
                                target,
                                maxRange,
                                e ->
                                        e instanceof LivingEntity living
                                                && !this.mob.canTarget(living));
                if (result.isPresent()) {
                    this.cool = 10;
                } else { // 射撃
                    this.mob.attack(target, 1.0f);
                    CrossbowItem.setCharged(itemStack, false);
                    this.mob.play(LMSounds.SHOOT);
                    this.mob.swingHand(Hand.MAIN_HAND);
                }
            }
        }
    }

    protected int getInterval(ItemStack itemStack) {
        return itemStack.getItem() instanceof IRangedWeapon rangedWeapon
                ? rangedWeapon.getInterval_LMRB(itemStack, this.mob)
                : 20;
    }

    @Override
    protected float getMaxRange(ItemStack itemStack) {
        return (itemStack.getItem() instanceof IRangedWeapon rangedWeapon
                        ? rangedWeapon.getMaxRange_LMRB(itemStack, this.mob)
                        : 16F)
                * LMRBMod.getConfig().work.archerShootDistanceFactor;
    }

    @Override
    public void resetTask() {
        super.resetTask();
        this.cool = 10;
        if (this.mob.isUsingItem()) {
            this.mob.clearActiveItem();
            this.mob.setCharging(false);
        }
    }

    @Override
    protected Optional<Item> getWeaponInstance(ItemStack stack) {
        var item = stack.getItem();
        return Optional.of(item);
    }
}
