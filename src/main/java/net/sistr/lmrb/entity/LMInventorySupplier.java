package net.sistr.lmrb.entity;

import com.google.common.collect.Lists;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.sistr.lmrb.util.DefaultedListLimiter;

import java.util.List;

public class LMInventorySupplier implements InventorySupplier {
    private final LivingEntity owner;
    private final FakePlayerSupplier player;
    private final int size = 18;
    private Inventory inventory;
    private ListTag inventoryTag;

    public LMInventorySupplier(LivingEntity owner, FakePlayerSupplier player) {
        this.owner = owner;
        this.player = player;
    }

    @Override
    public Inventory getInventory() {
        if (inventory == null) {
            if (!owner.world.isClient) {
                FakePlayer fakePlayer = player.getFakePlayer();
                inventory = new LMInventory(fakePlayer, owner, size);
                fakePlayer.inventory = (PlayerInventory) inventory;
                if (inventoryTag != null) readInventory(inventoryTag);
            } else {
                inventory = new SimpleInventory(size);
            }
        }
        return inventory;
    }

    //デフォのserializeとdeserializeは使うとエラー吐く
    public void writeInventory(CompoundTag nbt) {
        if (inventory == null) {
            if (inventoryTag != null)
                nbt.put("Inventory", inventoryTag);
            return;
        }

        ListTag listnbt = new ListTag();

        for(int index = 0; index < this.size; ++index) {
            if (!inventory.getStack(index).isEmpty()) {
                CompoundTag compoundnbt = new CompoundTag();
                compoundnbt.putByte("Slot", (byte)index);
                inventory.getStack(index).toTag(compoundnbt);
                listnbt.add(compoundnbt);
            }
        }

        List<ItemStack> armorInventory = Lists.newArrayList(this.owner.getArmorItems());
        for(int index = 0; index < armorInventory.size(); ++index) {
            if (!armorInventory.get(index).isEmpty()) {
                CompoundTag armor = new CompoundTag();
                armor.putByte("Slot", (byte)(index + 100));
                armorInventory.get(index).toTag(armor);
                listnbt.add(armor);
            }
        }

        List<ItemStack> offHandInventory = Lists.newArrayList(this.owner.getOffHandStack());
        for(int index = 0; index < offHandInventory.size(); ++index) {
            if (!offHandInventory.get(index).isEmpty()) {
                CompoundTag offhand = new CompoundTag();
                offhand.putByte("Slot", (byte)(index + 150));
                offHandInventory.get(index).toTag(offhand);
                listnbt.add(offhand);
            }
        }
        nbt.put("Inventory", listnbt);
    }

    public void readInventory(CompoundTag nbt) {
        this.inventoryTag = nbt.getList("Inventory", 10);
        if (inventory != null) readInventory(inventoryTag);
    }

    private void readInventory(ListTag inventoryTag) {
        for(int i = 0; i < inventoryTag.size(); ++i) {
            CompoundTag tag = inventoryTag.getCompound(i);
            int slot = tag.getByte("Slot") & 255;
            ItemStack itemstack = ItemStack.fromTag(tag);
            if (!itemstack.isEmpty()) {
                if (slot < this.size) {
                    inventory.setStack(slot, itemstack);
                } else if (slot < 100) {
                    this.owner.dropStack(itemstack);
                } else if (slot < 4 + 100) {
                    this.owner.equipStack(EquipmentSlot
                            .fromTypeIndex(EquipmentSlot.Type.ARMOR, slot - 100), itemstack);
                } else if (slot >= 150 && slot < 1 + 150) {
                    this.owner.equipStack(EquipmentSlot.OFFHAND, itemstack);
                }
            }
        }
    }

    private static class LMInventory extends PlayerInventory {
        private final LivingEntity owner;

        public LMInventory(PlayerEntity player, LivingEntity owner, int size) {
            super(player);
            this.owner = owner;
            ((DefaultedListLimiter)this.main).setSizeLimit_LM(size);
        }

        @Override
        public ItemStack getMainHandStack() {
            return owner.getMainHandStack();
        }



    }

}
