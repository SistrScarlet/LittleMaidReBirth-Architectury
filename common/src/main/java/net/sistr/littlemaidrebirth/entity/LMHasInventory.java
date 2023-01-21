package net.sistr.littlemaidrebirth.entity;

import com.google.common.collect.Sets;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.collection.DefaultedList;
import net.sistr.littlemaidrebirth.entity.util.HasFakePlayer;
import net.sistr.littlemaidrebirth.entity.util.HasInventory;
import net.sistr.littlemaidrebirth.util.DefaultedListLimiter;
import net.sistr.littlemaidrebirth.util.PlayerEntityInventoryAccessor;
import net.sistr.littlemaidrebirth.util.PlayerInventoryAccessor;

import java.util.Set;

public class LMHasInventory implements HasInventory {
    private final Inventory inventory;

    public LMHasInventory(LivingEntity owner, HasFakePlayer player) {
        if (!owner.world.isClient) {
            FakePlayer fakePlayer = player.getFakePlayer();
            inventory = new LMInventory(fakePlayer, 19);
            ((PlayerEntityInventoryAccessor) fakePlayer).setPlayerInventory_LMRB((PlayerInventory) inventory);
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
        if (this.inventory instanceof LMInventory)
            nbt.put("Inventory", ((LMInventory) this.inventory).writeNbt(new NbtList()));
    }

    @Override
    public void readInventory(NbtCompound nbt) {
        if (this.inventory instanceof LMInventory)
            ((LMInventory) this.inventory).readNbt(nbt.getList("Inventory", 10));
    }

    public static class LMInventory extends PlayerInventory {
        private static final Set<Item> EXCLUDE_ITEM = Sets.newHashSet();

        public LMInventory(PlayerEntity player, int size) {
            super(player);
            ((DefaultedListLimiter) this.main).setSizeLimit_LM(size);
        }

        @Override
        public void updateItems() {
            //LMInventoryへFabricのInventoryStorageなどでアクセスするとクラッシュすることへの対処療法的な修正
            for (DefaultedList<ItemStack> defaultedList : ((PlayerInventoryAccessor) this).getCombinedInventory()) {
                for (int i = 0; i < defaultedList.size(); ++i) {
                    var itemStack = defaultedList.get(i);
                    var item = itemStack.getItem();
                    if (itemStack.isEmpty() || EXCLUDE_ITEM.contains(item)) continue;
                    try {
                        itemStack.inventoryTick(this.player.world, this.player, i, this.selectedSlot == i);
                    } catch (Exception e) {
                        EXCLUDE_ITEM.add(item);
                    }
                }
            }
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
