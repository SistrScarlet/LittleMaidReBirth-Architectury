package net.sistr.littlemaidrebirth.entity;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.sistr.littlemaidrebirth.entity.util.HasInventory;

public class LMHasInventory implements HasInventory {
    private final Inventory inventory;

    public LMHasInventory() {
        this.inventory = new SimpleInventory(18);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void writeInventory(NbtCompound nbt) {
        nbt.put("Inventory", this.writeNbt(new NbtList()));
    }

    @Override
    public void readInventory(NbtCompound nbt) {
        int maidVersion = nbt.getByte("maidVersion") & 255;
        if (maidVersion == 0) {
            this.readNbtOld(nbt.getList("Inventory", 10));
        } else {
            this.readNbt(nbt.getList("Inventory", 10));
        }
    }

    public NbtList writeNbt(NbtList nbtList) {
        int i;
        NbtCompound nbt;
        for (i = 0; i < 18; ++i) {
            var stack = this.inventory.getStack(i);
            if (!stack.isEmpty()) {
                nbt = new NbtCompound();
                nbt.putByte("Slot", (byte) i);
                stack.writeNbt(nbt);
                nbtList.add(nbt);
            }
        }

        return nbtList;
    }

    public void readNbt(NbtList nbtList) {
        this.inventory.clear();

        for (int i = 0; i < nbtList.size(); ++i) {
            NbtCompound nbtCompound = nbtList.getCompound(i);
            int j = nbtCompound.getByte("Slot") & 255;
            ItemStack stack = ItemStack.fromNbt(nbtCompound);
            if (!stack.isEmpty()) {
                if (j < 18) {
                    this.inventory.setStack(j, stack);
                }
            }
        }
    }

    public void readNbtOld(NbtList nbtList) {
        this.inventory.clear();

        for (int i = 0; i < nbtList.size(); ++i) {
            NbtCompound nbtCompound = nbtList.getCompound(i);
            int j = nbtCompound.getByte("Slot") & 255;
            ItemStack stack = ItemStack.fromNbt(nbtCompound);
            if (!stack.isEmpty()) {
                if (1 <= j && j <= 18) {
                    this.inventory.setStack(j - 1, stack);
                }
            }
        }
    }

}
