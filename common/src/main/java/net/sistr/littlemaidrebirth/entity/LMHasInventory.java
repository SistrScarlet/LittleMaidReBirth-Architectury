package net.sistr.littlemaidrebirth.entity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Hand;
import net.sistr.littlemaidrebirth.entity.util.HasInventory;

public class LMHasInventory implements HasInventory {
    private final LittleMaidEntity maid;
    private final Inventory inventory;

    public LMHasInventory(LittleMaidEntity maid) {
        this.maid = maid;
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
                stack.encode(this.maid.getRegistryManager(), nbt);
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
            ItemStack stack = ItemStack.fromNbtOrEmpty(this.maid.getRegistryManager(), nbtCompound);
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
            ItemStack stack = ItemStack.fromNbtOrEmpty(this.maid.getRegistryManager(), nbtCompound);
            if (!stack.isEmpty()) {
                if (1 <= j && j <= 18) {
                    this.inventory.setStack(j - 1, stack);
                }
            }
        }
    }

    public static Inventory getInvAndHands(LittleMaidEntity maid) {
        var inv = maid.getInventory();
        return new Inventory() {
            @Override
            public int size() {
                return 20;
            }

            @Override
            public boolean isEmpty() {
                return inv.isEmpty()
                        && maid.getMainHandStack().isEmpty()
                        && maid.getOffHandStack().isEmpty();
            }

            @Override
            public ItemStack getStack(int slot) {
                if (slot == 0) {
                    return maid.getMainHandStack();
                } else if (slot == 1) {
                    return maid.getOffHandStack();
                }
                return inv.getStack(slot - 2);
            }

            @Override
            public ItemStack removeStack(int slot, int amount) {
                if (slot == 0) {
                    ItemStack itemStack = maid.getMainHandStack();
                    if (itemStack.isEmpty() || amount <= 0) {
                        return ItemStack.EMPTY;
                    }
                    itemStack = itemStack.split(amount);
                    if (!itemStack.isEmpty()) {
                        this.markDirty();
                    }
                    return itemStack;
                } else if (slot == 1) {
                    ItemStack itemStack = maid.getOffHandStack();
                    if (itemStack.isEmpty() || amount <= 0) {
                        return ItemStack.EMPTY;
                    }
                    itemStack = itemStack.split(amount);
                    if (!itemStack.isEmpty()) {
                        this.markDirty();
                    }
                    return itemStack;
                }
                return inv.removeStack(slot - 2, amount);
            }

            @Override
            public ItemStack removeStack(int slot) {
                if (slot == 0) {
                    var stack = maid.getMainHandStack();
                    if (stack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                    maid.setStackInHand(Hand.MAIN_HAND, stack);
                    return stack;
                } else if (slot == 1) {
                    var stack = maid.getOffHandStack();
                    if (stack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                    maid.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
                    return stack;
                }
                return inv.removeStack(slot - 2);
            }

            @Override
            public void setStack(int slot, ItemStack stack) {
                if (slot == 0) {
                    maid.setStackInHand(Hand.MAIN_HAND, stack);
                } else if (slot == 1) {
                    maid.setStackInHand(Hand.OFF_HAND, stack);
                } else {
                    inv.setStack(slot - 2, stack);
                }
            }

            @Override
            public void markDirty() {
                inv.markDirty();
            }

            @Override
            public boolean canPlayerUse(PlayerEntity player) {
                return inv.canPlayerUse(player);
            }

            @Override
            public void clear() {
                inv.clear();
            }
        };
    }

}
