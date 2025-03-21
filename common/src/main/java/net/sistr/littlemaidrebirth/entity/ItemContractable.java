package net.sistr.littlemaidrebirth.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.sistr.littlemaidrebirth.entity.util.Contractable;
import net.sistr.littlemaidrebirth.entity.util.HasInventory;

import java.util.function.Predicate;

//クライアント側では概ね役に立たない
public class ItemContractable<T extends LivingEntity & HasInventory> implements Contractable {
    protected final T mob;
    protected final int maxConsumeInterval;
    protected final int maxUnpaidTimes;
    protected final Predicate<ItemStack> salaryItems;
    protected int consumeInterval;
    protected int unpaidTimes;
    protected boolean contract;
    protected boolean strike;

    public ItemContractable(T mob, int maxConsumeInterval, int maxUnpaidTimes, Predicate<ItemStack> salaryItems) {
        this.mob = mob;
        this.maxConsumeInterval = maxConsumeInterval;
        this.maxUnpaidTimes = maxUnpaidTimes;
        this.salaryItems = salaryItems;
    }

    public void tick() {
        if (mob.getWorld().isClient() || !this.contract) {
            return;
        }
        this.consumeInterval++;
        if ((mob.getId() + mob.age) % 20 != 0) {
            return;
        }

        intervalTick();
    }

    protected void intervalTick() {
        if (this.maxConsumeInterval < this.consumeInterval) {
            this.consumeInterval = 0;
            this.unpaidTimes++;
        }

        if (this.strike) {
            return;
        }

        nonStrikeIntervalTick();
    }

    protected void nonStrikeIntervalTick() {
        if (0 < unpaidTimes) {
            receiveSalary(mob.getInventory());
            if (maxUnpaidTimes < unpaidTimes) {
                this.strike = true;
                onStrike();
            }
        }
    }

    protected void onStrike() {
    }

    public boolean isSalary(ItemStack stack) {
        return !stack.isEmpty() && this.salaryItems.test(stack);
    }

    public void receiveSalary(Inventory inventory) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            while (0 < this.unpaidTimes && isSalary(stack)) {
                this.unpaidTimes--;
                stack.decrement(1);
                postReceive();
                if (stack.isEmpty()) {
                    inventory.removeStack(i);
                }
            }
        }
    }

    public int checkSalarySlots() {
        int count = 0;
        var inv = mob.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (isSalary(stack)) {
                count++;
            }
        }
        return count;
    }

    protected void postReceive() {

    }

    public void setUnpaidTimes(int unpaidTimes) {
        this.unpaidTimes = unpaidTimes;
    }

    public int getUnpaidTimes() {
        return unpaidTimes;
    }

    @Override
    public boolean isContract() {
        return contract;
    }

    @Override
    public void setContract(boolean isContract) {
        this.contract = isContract;
    }

    @Override
    public boolean isStrike() {
        return this.strike;
    }

    @Override
    public void setStrike(boolean strike) {
        this.strike = strike;
    }

    @Override
    public void writeContractable(NbtCompound nbt) {
        NbtCompound itemContractable = new NbtCompound();
        itemContractable.putBoolean("contract", contract);
        itemContractable.putBoolean("strike", strike);
        itemContractable.putInt("consumeInterval", consumeInterval);
        nbt.put("ItemContractable", itemContractable);
    }

    @Override
    public void readContractable(NbtCompound nbt) {
        if (!nbt.contains("ItemContractable")) {
            return;
        }
        NbtCompound itemContractable = nbt.getCompound("ItemContractable");
        contract = itemContractable.getBoolean("contract");
        strike = itemContractable.getBoolean("strike");
        consumeInterval = itemContractable.getInt("consumeInterval");
    }
}
