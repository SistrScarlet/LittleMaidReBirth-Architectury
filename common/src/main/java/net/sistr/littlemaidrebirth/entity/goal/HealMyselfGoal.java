package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.entity.util.InventorySupplier;

import java.util.EnumSet;
import java.util.function.Predicate;

public class HealMyselfGoal<T extends PathAwareEntity & InventorySupplier> extends Goal {
    private final T mob;
    private final int healInterval;
    private final int healAmount;
    private final Predicate<ItemStack> healItem;
    private int cool;
    private int cache;

    public HealMyselfGoal(T mob,
                          int healInterval, int healAmount, Predicate<ItemStack> healItem) {
        this.mob = mob;
        this.healInterval = healInterval;
        this.healAmount = healAmount;
        this.healItem = healItem;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        assert mob.getMaxHealth() != 0;
        return (mob.hurtTime <= 0 && mob.getHealth() <= mob.getMaxHealth() - 1
                || mob.getHealth() / mob.getMaxHealth() < 0.75F)
                && hasHealItem();
    }

    @Override
    public boolean shouldContinue() {
        return mob.getHealth() < mob.getMaxHealth() && hasHealItem();
    }

    @Override
    public void start() {
        super.start();
        this.mob.getNavigation().stop();
        cool = healInterval;
    }

    @Override
    public void tick() {
        if (0 < cool--) {
            return;
        }
        cool = healInterval;

        int slot = getHealItemSlot();
        ItemStack healItem = getHealItem(slot);
        if (healItem.isEmpty()) return;
        consumeHealItem(slot, healItem);

        mob.heal(healAmount);
        mob.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, mob.getRandom().nextFloat() * 0.1F + 1.0F);
        mob.swingHand(Hand.MAIN_HAND);
        if (this.mob instanceof SoundPlayable) {
            if (mob.getHealth() < mob.getMaxHealth()) {
                ((SoundPlayable) mob).play(LMSounds.EAT_SUGAR);
            } else {
                ((SoundPlayable) mob).play(LMSounds.EAT_SUGAR_MAX_POWER);
            }
        }
    }

    public boolean hasHealItem() {
        return getHealItemSlot() != -1;
    }

    public int getHealItemSlot() {
        Inventory inventory = this.mob.getInventory();
        if (cache != -1) {
            ItemStack slotStack = inventory.getStack(cache);
            if (healItem.test(slotStack)) {
                return cache;
            } else {
                cache = -1;
            }
        }
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack slotStack = inventory.getStack(i);
            if (healItem.test(slotStack)) {
                cache = i;
                return i;
            }
        }
        return -1;
    }

    public ItemStack getHealItem(int slot) {
        if (slot == -1) return ItemStack.EMPTY;
        return this.mob.getInventory().getStack(slot);
    }

    public void consumeHealItem(int slot, ItemStack healItem) {
        healItem.decrement(1);
        if (healItem.isEmpty()) {
            this.mob.getInventory().removeStack(slot);
        }
    }

}
