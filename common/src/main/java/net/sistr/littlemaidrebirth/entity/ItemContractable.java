package net.sistr.littlemaidrebirth.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.sistr.littlemaidrebirth.entity.util.Contractable;
import net.sistr.littlemaidrebirth.entity.util.HasInventory;

import java.util.function.Consumer;
import java.util.function.Predicate;

//クライアント側では概ね役に立たない
public class ItemContractable<T extends LivingEntity & HasInventory> implements Contractable {
    private final T mob;
    private final int maxConsumeInterval;
    private final int maxUnpaidTimes;
    private final Predicate<ItemStack> salaryItems;
    private final Consumer<T> strikeCallback;
    private int consumeInterval;
    private int unpaidTimes;
    private boolean contract;
    private boolean strike;

    public ItemContractable(T mob, int maxConsumeInterval, int maxUnpaidTimes, Predicate<ItemStack> salaryItems, Consumer<T> strikeCallback) {
        this.mob = mob;
        this.maxConsumeInterval = maxConsumeInterval;
        this.maxUnpaidTimes = maxUnpaidTimes;
        this.salaryItems = salaryItems;
        this.strikeCallback = strikeCallback;
    }

    public void tick() {
        if (mob.world.isClient() || !this.contract) {
            return;
        }
        this.consumeInterval++;
        if ((mob.getId() + mob.age) % 20 != 0) {
            return;
        }

        if (this.maxConsumeInterval < this.consumeInterval) {
            this.consumeInterval = 0;
            this.unpaidTimes++;
        }

        if (this.strike) {
            return;
        }

        if (0 < unpaidTimes) {
            receiveSalary(mob.getInventory());
            if (maxUnpaidTimes < unpaidTimes) {
                this.strike = true;
                this.strikeCallback.accept(this.mob);
            }
        }
    }

    public boolean isSalary(ItemStack stack) {
        return this.salaryItems.test(stack);
    }

    public void receiveSalary(Inventory inventory) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            while (!stack.isEmpty() && 0 < this.unpaidTimes && this.salaryItems.test(stack)) {
                this.unpaidTimes--;
                stack.decrement(1);
            }
        }
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
