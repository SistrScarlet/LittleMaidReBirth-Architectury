package net.sistr.littlemaidrebirth.entity.mode;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

//空腹なら食料を食わせる。ただし害のあるものは食べさせない
//有用なポーション効果があるアイテムは、常時効果が切れないように使用する
//即時回復を含む食料は普通に使う
//即時回復を含むポーションは、体力が減るまで使わない
//…ご主人がアンデッドの場合でも、即時回復を使う。ご主人は死ぬ。
//todo コンフィグで害のあるものも食えるか調整可能にする
public class HealerMode extends Mode {
    protected final LittleMaidEntity mob;
    protected final int inventoryStart;
    protected final int inventoryEnd;
    protected LivingEntity owner;
    protected int foodIndex;
    protected int potionIndex;

    public HealerMode(ModeType<? extends Mode> modeType, String name, LittleMaidEntity mob, int inventoryStart, int inventoryEnd) {
        super(modeType, name);
        this.mob = mob;
        this.inventoryStart = inventoryStart;
        this.inventoryEnd = inventoryEnd;
    }

    @Override
    public boolean shouldExecute() {
        //ざっくり1秒に1回チェック
        if (this.mob.getRandom().nextFloat() > 1 / 20f) {
            return false;
        }
        LivingEntity owner = mob.getTameOwner().orElse(null);
        if (!(owner instanceof PlayerEntity)) return false;
        this.owner = owner;
        boolean isHunger = ((PlayerEntity) owner).getHungerManager().isNotFull();
        boolean fullHealth = owner.getHealth() >= owner.getMaxHealth();
        return searchInventory(owner, isHunger, fullHealth);
    }

    @Override
    public boolean shouldContinueExecuting() {
        LivingEntity owner = mob.getTameOwner().orElse(null);
        if (!(owner instanceof PlayerEntity)) return false;
        boolean isHunger = ((PlayerEntity) owner).getHungerManager().isNotFull();
        boolean fullHealth = owner.getMaxHealth() <= owner.getHealth();
        this.owner = owner;
        return searchInventory(owner, isHunger, fullHealth);
    }

    public boolean searchInventory(LivingEntity owner, boolean isHunger, boolean fullHealth) {
        boolean result = false;
        foodIndex = -1;
        potionIndex = -1;
        Inventory inventory = this.mob.getInventory();
        for (int i = inventoryStart; i < inventoryEnd; ++i) {
            ItemStack stack = inventory.getStack(i);
            if (isHunger && foodIndex == -1 && isFood(stack)) {
                foodIndex = i;
                result = true;
            }
            if (potionIndex == -1 && isBeneficialPotion(owner, stack, fullHealth)) {
                potionIndex = i;
                result = true;
            }
        }
        return result;
    }

    public boolean isFood(ItemStack stack) {
        return stack.isFood()
                && stack.getItem().getFoodComponent().getStatusEffects().stream()
                //コンフィグで害のあるものも食えるか調整可能にする
                //allMatchだとエフェクトが無い場合にfalseになってしまう
                .noneMatch(p -> p.getFirst().getEffectType().getCategory()
                        != StatusEffectCategory.BENEFICIAL);
    }

    public boolean isBeneficialPotion(LivingEntity owner, ItemStack stack, boolean fullHealth) {
        var potion = PotionUtil.getPotion(stack);
        if (potion == Potions.EMPTY) {
            return false;
        }
        //いずれかひとつでも有用でない効果がある場合はfalse
        if (potion.getEffects().stream()
                .anyMatch(e -> e.getEffectType().getCategory() != StatusEffectCategory.BENEFICIAL)) {
            return false;
        }
        //コンフィグで害のあるものも食えるか調整可能にする
        return potion.getEffects().stream()
                //即時回復ではないか、体力が減ってるならtrue
                //即時回復は体力が減っていないとfalse
                .filter(e -> e.getEffectType() != StatusEffects.INSTANT_HEALTH || !fullHealth)
                //ご主人が持っていないエフェクトか、レベルが上なら適用する
                .anyMatch(e -> owner.getStatusEffects().isEmpty()
                        || owner.getStatusEffects()
                        .stream()
                        //いずれか一つでもご主人が持ってたらダメ
                        .noneMatch(oE -> oE.getEffectType() == e.getEffectType()
                                && e.getAmplifier() <= oE.getAmplifier())
                );
    }

    @Override
    public void tick() {
        Inventory inventory = mob.getInventory();
        //飯
        if (foodIndex != -1) {
            ItemStack stack = inventory.getStack(foodIndex);
            stack = owner.eatFood(owner.world, stack);
            if (stack.isEmpty()) {
                inventory.removeStack(foodIndex);
            } else {
                inventory.setStack(foodIndex, stack);
            }
        }
        //薬
        if (potionIndex != -1) {
            ItemStack stack = inventory.getStack(potionIndex);
            stack = stack.finishUsing(owner.world, owner);
            if (stack.isEmpty()) {
                inventory.removeStack(potionIndex);
            } else {
                inventory.setStack(potionIndex, stack);
            }
            owner.world.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
        ((SoundPlayable) this.mob).play(LMSounds.HEALING);
    }
}
