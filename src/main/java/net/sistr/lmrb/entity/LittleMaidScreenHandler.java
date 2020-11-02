package net.sistr.lmrb.entity;


import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.sistr.lmrb.setup.Registration;

public class LittleMaidScreenHandler extends ScreenHandler implements HasGuiEntitySupplier<LittleMaidEntity> {
    private final LittleMaidEntity maid;
    private final PlayerInventory playerInventory;
    private final Inventory maidInventory;

    public LittleMaidScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf packet) {
        this(syncId, playerInventory, packet.readVarInt());
    }

    public LittleMaidScreenHandler(int syncId, PlayerInventory playerInventory, int entityId) {
        super(Registration.LITTLE_MAID_SCREEN_HANDLER, syncId);
        this.playerInventory = playerInventory;

        LittleMaidEntity maid = (LittleMaidEntity) playerInventory.player.world.getEntityById(entityId);
        this.maid = maid;
        if (maid == null) {
            this.maidInventory = new SimpleInventory(18);
            return;
        }
        this.maidInventory = maid.getInventory();

        maidInventory.onOpen(playerInventory.player);

        layoutMaidInventorySlots();
        layoutPlayerInventorySlots(8, 126);
    }

    public PlayerEntity getPlayer() {
        return playerInventory.player;
    }

    public LittleMaidEntity getGuiEntity() {
        return maid;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.maid != null && this.maid.isAlive() && this.maid.squaredDistanceTo(player) < 8.0F * 8.0F;
    }

    //18 + 2 + 4 = 24、24 + 4 * 9 = 60
    //0~17メイドインベントリ、18~19メインサブ、20~23防具、24~59プレイヤーインベントリ
    @Override
    public ItemStack transferSlot(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot == null || !slot.hasStack()) {
            return newStack;
        }
        ItemStack originalStack = slot.getStack();
        newStack = originalStack.copy();
        if (invSlot < 18) {//メイド->プレイヤー
            if (!this.insertItem(originalStack, 24, 60, false)) {
                return ItemStack.EMPTY;
            }
        } else if (invSlot < 24) {//ハンド、防具->メイド
            if (!this.insertItem(originalStack, 0, 18, true)) {
                return ItemStack.EMPTY;
            }
        } else {//プレイヤー->メイド
            if (!this.insertItem(originalStack, 0, 18, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (originalStack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        return newStack;
    }

    private int addSlotRange(Inventory inventory, int index, int x, int y, int amount, int dx) {
        for (int i = 0; i < amount; i++) {
            addSlot(new Slot(inventory, index, x, y));
            x += dx;
            index++;
        }
        return index;
    }

    private int addSlotBox(Inventory inventory, int index, int x, int y, int horAmount, int dx, int verAmount, int dy) {
        for (int j = 0; j < verAmount; j++) {
            index = addSlotRange(inventory, index, x, y, horAmount, dx);
            y += dy;
        }
        return index;
    }

    private void layoutPlayerInventorySlots(int leftCol, int topRow) {
        //24~50
        //Player inventory
        addSlotBox(playerInventory, 9, leftCol, topRow, 9, 18, 3, 18);

        //51~59
        //Hotbar
        topRow += 58;
        addSlotRange(playerInventory, 0, leftCol, topRow, 9, 18);
    }

    private void layoutMaidInventorySlots() {
        //index 0~17
        addSlotBox(maidInventory, 0, 8, 76, 9, 18, 2, 18);

        //18~19
        Inventory handsInventory = new SimpleInventory(2) {
            @Override
            public void setStack(int slot, ItemStack stack) {
                if (slot == 0) {
                    maid.equipStack(EquipmentSlot.MAINHAND, stack);
                } else {
                    maid.equipStack(EquipmentSlot.OFFHAND, stack);
                }
                super.setStack(slot, stack);
            }
        };
        handsInventory.setStack(0, maid.getMainHandStack());
        handsInventory.setStack(1, maid.getOffHandStack());
        addSlot(new Slot(handsInventory, 0, 116, 44));
        addSlot(new Slot(handsInventory, 1, 152, 44));

        //20~23
        Inventory armorsInventory = new SimpleInventory(4) {
            @Override
            public void setStack(int slot, ItemStack stack) {
                EquipmentSlot equipmentSlot = EquipmentSlot.fromTypeIndex(EquipmentSlot.Type.ARMOR, slot);
                maid.equipStack(equipmentSlot, stack);
                super.setStack(slot, stack);
            }

            @Override
            public boolean isValid(int slot, ItemStack stack) {
                return false;
            }
        };
        for (int i = 0; i < 4; i++) {
            EquipmentSlot slot = EquipmentSlot.fromTypeIndex(EquipmentSlot.Type.ARMOR, i);
            armorsInventory.setStack(slot.getEntitySlotId(), maid.getEquippedStack(slot));
        }
        addSlot(new Slot(armorsInventory, EquipmentSlot.HEAD.getEntitySlotId(), 8, 8) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return MobEntity.getPreferredEquipmentSlot(stack) == EquipmentSlot.HEAD;
            }
        });
        addSlot(new Slot(armorsInventory, EquipmentSlot.CHEST.getEntitySlotId(), 8, 44) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return MobEntity.getPreferredEquipmentSlot(stack) == EquipmentSlot.CHEST;
            }
        });
        addSlot(new Slot(armorsInventory, EquipmentSlot.LEGS.getEntitySlotId(), 80, 8) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return MobEntity.getPreferredEquipmentSlot(stack) == EquipmentSlot.LEGS;
            }
        });
        addSlot(new Slot(armorsInventory, EquipmentSlot.FEET.getEntitySlotId(), 80, 44) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return MobEntity.getPreferredEquipmentSlot(stack) == EquipmentSlot.FEET;
            }
        });
    }

}
