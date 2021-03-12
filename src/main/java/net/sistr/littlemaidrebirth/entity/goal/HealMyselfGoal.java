package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.sistr.lmml.entity.compound.SoundPlayable;
import net.sistr.lmml.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.entity.InventorySupplier;

import java.util.EnumSet;
import java.util.Set;

public class HealMyselfGoal<T extends PathAwareEntity & InventorySupplier> extends Goal {
    private final T mob;
    private final Set<Item> healItems;
    private final int healInterval;
    private final int healAmount;
    private int cool;

    public HealMyselfGoal(T mob, Set<Item> healItems,
                          int healInterval, int healAmount) {
        this.mob = mob;
        this.healItems = healItems;
        this.healInterval = healInterval;
        this.healAmount = healAmount;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        assert mob.getMaxHealth() != 0;
        return (mob.hurtTime <= 0 && mob.getHealth() <= mob.getMaxHealth() - 1
                || mob.getHealth() / mob.getMaxHealth() < 0.75F)
                && getHealItemSlot() != -1;
    }

    @Override
    public boolean shouldContinue() {
        return mob.getHealth() < mob.getMaxHealth();
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
        Inventory inventory = this.mob.getInventory();
        int slot = getHealItemSlot();
        ItemStack healItem = inventory.getStack(slot);
        if (healItem.isEmpty()) {
            return;
        }
        healItem.decrement(1);
        if (healItem.isEmpty()) {
            inventory.removeStack(slot);
        }
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

    public int getHealItemSlot() {
        Inventory inventory = this.mob.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack slotStack = inventory.getStack(i);
            if (healItems.contains(slotStack.getItem())) {
                return i;
            }
        }
        return -1;
    }

}
