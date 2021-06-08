package net.sistr.littlemaidrebirth.entity;

import net.minecraft.item.ItemStack;

public interface NeedSalary {

    boolean receiveSalary(int num);

    boolean consumeSalary(int num);

    int getSalary();

    boolean isSalary(ItemStack stack);

    boolean isStrike();

    void setStrike(boolean strike);

    /*void writeSalary(CompoundNBT nbt);

    void readSalary(CompoundNBT nbt);*/

}
