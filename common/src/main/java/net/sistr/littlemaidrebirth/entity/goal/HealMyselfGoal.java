package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.sistr.littlemaidrebirth.entity.util.HasInventory;

import java.util.EnumSet;
import java.util.function.Predicate;

public class HealMyselfGoal<T extends PathAwareEntity & HasInventory> extends Goal {
    protected final T mob;
    protected final int healInterval;
    protected final int healAmount;
    protected final Predicate<ItemStack> healItemPred;
    protected int cool;
    protected int healItemSlot = -1;

    public HealMyselfGoal(T mob, int healInterval, int healAmount, Predicate<ItemStack> healItemPred) {
        this.mob = mob;
        this.healInterval = healInterval;
        this.healAmount = healAmount;
        this.healItemPred = healItemPred;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        //体力がフルならfalse
        if (isHealthFull()) {
            return false;
        }

        //無敵時間中で、体力に余裕があるならfalse
        if (hasHurtTime() && isEnoughHealth()) {
            return false;
        }

        //回復アイテム存在チェック
        this.healItemSlot = findHealItemSlot();
        if (this.healItemSlot == -1) {
            return false;
        }
        return true;
    }

    protected boolean isHealthFull() {
        return mob.getHealth() >= mob.getMaxHealth();
    }

    protected boolean hasHurtTime() {
        return mob.hurtTime > 0;
    }

    //todo コンフィグに低ヘルス判定しきい値を追加
    protected boolean isEnoughHealth() {
        return mob.getHealth() / mob.getMaxHealth() > 0.75f;
    }

    @Override
    public boolean shouldContinue() {
        //体力満タンで終了
        if (isHealthFull()) {
            return false;
        }
        //回復アイテムスロットを更新
        healItemSlot = findHealItemSlot();
        return healItemSlot != -1;
    }

    @Override
    public void start() {
        super.start();
        this.mob.getNavigation().stop();
        cool = 0;
    }

    @Override
    public void tick() {
        //回復インターバル
        if (0 < cool--) {
            return;
        }
        cool = healInterval;

        var healItem = getHealItem(healItemSlot);
        //回復アイテム存在チェック
        if (!isHealItem(healItem)) {
            healItemSlot = -1;
            return;
        }

        //回復実行
        heal(healItem);
    }

    public void heal(ItemStack healItem) {
        //回復
        mob.heal(healAmount);
        //アイテム消費
        consumeHealItem(healItem);
        //回復演出
        mob.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, mob.getRandom().nextFloat() * 0.1F + 1.0F);
        mob.swingHand(Hand.MAIN_HAND);
    }

    public int findHealItemSlot() {
        var inventory = this.mob.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack slotStack = inventory.getStack(i);
            if (healItemPred.test(slotStack)) {
                healItemSlot = i;
                return i;
            }
        }
        return -1;
    }

    public ItemStack getHealItem(int slot) {
        if (slot == -1) return ItemStack.EMPTY;

        var stack = this.mob.getInventory().getStack(slot);
        if (!isHealItem(stack)) {
            return ItemStack.EMPTY;
        }
        return stack;
    }

    public boolean isHealItem(ItemStack stack) {
        return healItemPred.test(stack);
    }

    public void consumeHealItem(ItemStack healItem) {
        //消費量のコンフィグ追加はさすがに要らない？
        healItem.decrement(1);
        if (healItem.isEmpty()) {
            this.mob.getInventory().removeStack(healItemSlot);
        }
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }
}
