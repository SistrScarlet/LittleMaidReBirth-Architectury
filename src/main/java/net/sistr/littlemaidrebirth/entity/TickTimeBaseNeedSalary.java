package net.sistr.littlemaidrebirth.entity;

import com.google.common.collect.Sets;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.util.Collection;
import java.util.Set;

public class TickTimeBaseNeedSalary implements NeedSalary {
    private final LivingEntity mob;
    private final InventorySupplier hasInventory;
    private final int maxSalary;
    private final Set<Item> salaries = Sets.newHashSet();
    private int salary;
    private int nextSalaryTicks;
    private boolean isStrike;
    private int checkInventoryCool;

    public TickTimeBaseNeedSalary(LivingEntity mob, InventorySupplier hasInventory, int maxSalary, Collection<Item> salaries) {
        this.mob = mob;
        this.salaries.addAll(salaries);
        this.maxSalary = maxSalary;
        this.hasInventory = hasInventory;
    }

    public void tick() {
        //クライアント側かストライキの場合処理しない
        if (mob.world.isClient || isStrike) {
            return;
        }
        //消費部分
        if (--nextSalaryTicks <= 0) {
            this.nextSalaryTicks = 24000;
            if (!consumeSalary(1)) {
                this.isStrike = true;
            }
        }
        //マックスの場合は補充しない
        if (maxSalary <= salary) {
            return;
        }
        //補充部分
        if (0 < --checkInventoryCool) {
            return;
        }
        checkInventoryCool = 200;
        Inventory inventory = hasInventory.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && isSalary(stack)) {
                while (true) {
                    if (receiveSalary(1)) {
                        stack.decrement(1);
                        if (stack.isEmpty()) {
                            //給料アイテムがなくなったら次のアイテムへ
                            break;
                        }
                    } else {
                        //給料を受け取れなくなったらreturn
                        return;
                    }
                }
            }
        }
    }

    @Override
    public boolean receiveSalary(int num) {
        if (maxSalary < salary + num) {
            salary = maxSalary;
            return false;
        }
        salary += num;
        return true;
    }

    @Override
    public boolean consumeSalary(int num) {
        if (salary < num) {
            salary = 0;
            return false;
        }
        salary -= num;
        return true;
    }

    @Override
    public int getSalary() {
        return salary;
    }

    @Override
    public boolean isSalary(ItemStack stack) {
        return salaries.contains(stack.getItem());
    }

    @Override
    public boolean isStrike() {
        return isStrike;
    }

    @Override
    public void setStrike(boolean strike) {
        isStrike = strike;
    }

    public void writeSalary(CompoundTag tag) {
        tag.putInt("salary", salary);
        tag.putInt("nextSalaryTicks", nextSalaryTicks);
        tag.putBoolean("isStrike", isStrike);
    }

    public void readSalary(CompoundTag tag) {
        salary = tag.getInt("salary");
        nextSalaryTicks = tag.getInt("nextSalaryTicks");
        isStrike = tag.getBoolean("isStrike");
    }
}
