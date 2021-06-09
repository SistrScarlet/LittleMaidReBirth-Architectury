package net.sistr.littlemaidrebirth.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.sistr.littlemaidrebirth.util.DefaultedListLimiter;

public class LMInventorySupplier implements InventorySupplier {
    private final Inventory inventory;

    public LMInventorySupplier(LivingEntity owner, FakePlayerSupplier player) {
        if (!owner.world.isClient) {
            FakePlayer fakePlayer = player.getFakePlayer();
            inventory = new LMInventory(fakePlayer, 19);
            fakePlayer.inventory = (PlayerInventory) inventory;
        } else {
            inventory = new SimpleInventory(18 + 4 + 2);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void writeInventory(NbtCompound nbt) {
        nbt.put("Inventory", ((LMInventory) this.inventory).writeNbt(new NbtList()));
    }

    @Override
    public void readInventory(NbtCompound nbt) {
        ((LMInventory) this.inventory).readNbt(nbt.getList("Inventory", 10));
    }

    public static class LMInventory extends PlayerInventory {

        public LMInventory(PlayerEntity player, int size) {
            super(player);
            ((DefaultedListLimiter) this.main).setSizeLimit_LM(size);
        }

        @Override
        public boolean insertStack(ItemStack stack) {
            boolean isMainEmpty = main.get(0).isEmpty();
            if (isMainEmpty) {
                main.set(0, Items.GOLDEN_SWORD.getDefaultStack());
            }
            boolean temp = super.insertStack(stack);
            if (isMainEmpty) {
                main.set(0, ItemStack.EMPTY);
            }
            return temp;
        }
    }

}
