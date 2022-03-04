package net.sistr.littlemaidrebirth.entity;

import net.minecraft.nbt.NbtCompound;

public interface Contractable {

    boolean isContract();

    void setContract(boolean isContract);

    boolean isStrike();

    void setStrike(boolean strike);

    void writeContractable(NbtCompound nbt);

    void readContractable(NbtCompound nbt);

}
