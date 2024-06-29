package net.sistr.littlemaidrebirth.entity.mode;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LMHasInventory;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

//空腹なら食料を食わせる。ただし害のあるものは食べさせない
//有用なポーション効果があるアイテムは、常時効果が切れないように使用する
//即時回復を含む食料は普通に使う
//即時回復を含むポーションは、体力が減るまで使わない
//…ご主人がアンデッドの場合でも、即時回復を使う。ご主人は死ぬ。
//todo コンフィグで害のあるものも食えるか調整可能にする
public class HealerMode extends Mode {
    protected final LittleMaidEntity mob;
    protected LivingEntity owner;
    protected int foodIndex;
    protected int potionIndex;

    public HealerMode(ModeType<? extends Mode> modeType, String name, LittleMaidEntity mob) {
        super(modeType, name);
        this.mob = mob;
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
        Inventory inventory = LMHasInventory.getInvAndHands(mob);
        for (int i = 0; i < inventory.size(); ++i) {
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
        var components = stack.getComponents();
        var foodComponent = components.get(DataComponentTypes.FOOD);

        return foodComponent != null
                && foodComponent.effects().stream()
                .allMatch(p -> p.effect().getEffectType().value().isBeneficial());
    }

    //todo コンフィグで害のあるものも食えるか調整可能にする
    public boolean isBeneficialPotion(LivingEntity owner, ItemStack stack, boolean fullHealth) {
        var components = stack.getComponents();
        var potionContents = components.get(DataComponentTypes.POTION_CONTENTS);

        if (potionContents == null) {
            return false;
        }

        //いずれかひとつでも有用でない効果がある場合はfalse
        for (StatusEffectInstance statusEffectInstance : potionContents.getEffects()) {
            var statusEffect = statusEffectInstance.getEffectType().value();
            if (!statusEffect.isBeneficial()) {
                return false;
            }
        }

        //即時回復かつご主人が体力満タンならfalse
        for (StatusEffectInstance statusEffectInstance : potionContents.getEffects()) {
            if (statusEffectInstance.getEffectType() == StatusEffects.INSTANT_HEALTH && fullHealth) {
                return false;
            }
        }

        //付与される以上のエフェクトをご主人が持っていたらfalse
        var effects = owner.getStatusEffects();
        if (!effects.isEmpty()) {
            for (StatusEffectInstance statusEffectInstance : potionContents.getEffects()) {
                for (StatusEffectInstance ownerEffect : effects) {

                    if (statusEffectInstance.getEffectType() == ownerEffect.getEffectType()) {
                        if (ownerEffect.getAmplifier() > statusEffectInstance.getAmplifier()) {
                            return false;
                        }
                        break;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public void tick() {
        Inventory inventory = LMHasInventory.getInvAndHands(mob);
        //飯
        if (foodIndex != -1) {
            ItemStack stack = inventory.getStack(foodIndex);
            var foodComponent = stack.getComponents().get(DataComponentTypes.FOOD);
            if (foodComponent != null) {
                stack = owner.eatFood(owner.getWorld(), stack, foodComponent);
                if (stack.isEmpty()) {
                    inventory.removeStack(foodIndex);
                } else {
                    inventory.setStack(foodIndex, stack);
                }
            }
        }
        //薬
        if (potionIndex != -1) {
            ItemStack stack = inventory.getStack(potionIndex);
            stack = stack.finishUsing(owner.getWorld(), owner);
            if (stack.isEmpty()) {
                inventory.removeStack(potionIndex);
            } else {
                inventory.setStack(potionIndex, stack);
            }
            owner.getWorld().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
        ((SoundPlayable) this.mob).play(LMSounds.HEALING);
    }
}
