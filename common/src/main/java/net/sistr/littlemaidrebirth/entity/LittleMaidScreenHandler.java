package net.sistr.littlemaidrebirth.entity;


import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.entity.util.GuiEntitySupplier;
import net.sistr.littlemaidrebirth.setup.Registration;

public class LittleMaidScreenHandler extends ScreenHandler implements GuiEntitySupplier<LittleMaidEntity> {
    private final PlayerInventory playerInventory;
    private final Inventory maidInventory;
    private final Inventory handInventory;
    private final Inventory armorInventory;
    private final LittleMaidEntity maid;
    private final int unpaidDays;

    public LittleMaidScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf packet) {
        this(syncId, playerInventory, packet.readVarInt(), packet.readByte());
    }

    public LittleMaidScreenHandler(int syncId, PlayerInventory playerInventory, int entityId, int unpaidDays) {
        super(Registration.LITTLE_MAID_SCREEN_HANDLER.get(), syncId);
        this.playerInventory = playerInventory;
        this.unpaidDays = unpaidDays;

        LittleMaidEntity maid = (LittleMaidEntity) playerInventory.player.getEntityWorld().getEntityById(entityId);
        this.maid = maid;
        if (maid == null) {
            throw new RuntimeException("メイドさんが存在しません。");
        } else {
            this.maidInventory = maid.getInventory();
            this.handInventory = new Inventory() {
                private EquipmentSlot index(int slot) {
                    return slot == 0 ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                }

                @Override
                public ItemStack getStack(int slot) {
                    EquipmentSlot equipmentSlot = index(slot);
                    return maid.getEquippedStack(equipmentSlot);
                }

                @Override
                public ItemStack removeStack(int slot, int amount) {
                    EquipmentSlot equipmentSlot = index(slot);
                    return maid.getEquippedStack(equipmentSlot).split(amount);
                }

                @Override
                public ItemStack removeStack(int slot) {
                    ItemStack result;
                    EquipmentSlot equipmentSlot = index(slot);
                    result = maid.getEquippedStack(equipmentSlot);
                    maid.equipStack(equipmentSlot, ItemStack.EMPTY);
                    return result;
                }

                @Override
                public void setStack(int slot, ItemStack stack) {
                    EquipmentSlot equipmentSlot = index(slot);
                    maid.equipStack(equipmentSlot, stack);
                }

                @Override
                public boolean isEmpty() {
                    return maid.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty()
                            && maid.getEquippedStack(EquipmentSlot.OFFHAND).isEmpty();
                }

                @Override
                public int size() {
                    return 2;
                }

                @Override
                public boolean canPlayerUse(PlayerEntity player) {
                    return true;
                }

                @Override
                public void markDirty() {

                }

                @Override
                public void clear() {

                }
            };
            this.armorInventory = new Inventory() {
                private EquipmentSlot index(int slot) {
                    return switch (slot) {
                        case 0 -> EquipmentSlot.FEET;
                        case 1 -> EquipmentSlot.LEGS;
                        case 2 -> EquipmentSlot.CHEST;
                        default -> EquipmentSlot.HEAD;
                    };
                }

                @Override
                public ItemStack getStack(int slot) {
                    EquipmentSlot equipmentSlot = index(slot);
                    return maid.getEquippedStack(equipmentSlot);
                }

                @Override
                public ItemStack removeStack(int slot, int amount) {
                    EquipmentSlot equipmentSlot = index(slot);
                    return maid.getEquippedStack(equipmentSlot).split(amount);
                }

                @Override
                public ItemStack removeStack(int slot) {
                    ItemStack result;
                    EquipmentSlot equipmentSlot = index(slot);
                    result = maid.getEquippedStack(equipmentSlot);
                    maid.equipStack(equipmentSlot, ItemStack.EMPTY);
                    return result;
                }

                @Override
                public void setStack(int slot, ItemStack stack) {
                    EquipmentSlot equipmentSlot = index(slot);
                    maid.equipStack(equipmentSlot, stack);
                }

                @Override
                public boolean isEmpty() {
                    return maid.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
                            && maid.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
                            && maid.getEquippedStack(EquipmentSlot.LEGS).isEmpty()
                            && maid.getEquippedStack(EquipmentSlot.FEET).isEmpty();
                }

                @Override
                public int size() {
                    return 4;
                }

                @Override
                public boolean canPlayerUse(PlayerEntity player) {
                    return true;
                }

                @Override
                public void markDirty() {

                }

                @Override
                public void clear() {

                }
            };
        }

        maidInventory.onOpen(playerInventory.player);

        layoutMaidInventorySlots();
        layoutPlayerInventorySlots(8, 126);
    }

    public LittleMaidEntity getGuiEntity() {
        return maid;
    }

    public int getUnpaidDays() {
        return unpaidDays;
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
        Identifier atlas = new Identifier("textures/atlas/blocks.png");
        //index 0~17
        addSlotBox(maidInventory, 0, 8, 76, 9, 18, 2, 18);

        //18~19
        addSlot(new Slot(handInventory, 0, 116, 44));
        addSlot(new Slot(handInventory, 1, 152, 44) {
            @Override
            public Pair<Identifier, Identifier> getBackgroundSprite() {
                return Pair.of(atlas, new Identifier("item/empty_armor_slot_shield"));
            }
        });

        //20~23
        addSlot(new Slot(armorInventory, EquipmentSlot.HEAD.getEntitySlotId(), 8, 8) {

            @Override
            public int getMaxItemCount() {
                return 1;
            }

            @Override
            public boolean canInsert(ItemStack stack) {
                return MobEntity.getPreferredEquipmentSlot(stack) == EquipmentSlot.HEAD;
            }

            @Override
            public Pair<Identifier, Identifier> getBackgroundSprite() {
                return Pair.of(atlas, new Identifier("item/empty_armor_slot_helmet"));
            }
        });
        addSlot(new Slot(armorInventory, EquipmentSlot.CHEST.getEntitySlotId(), 8, 44) {
            @Override
            public int getMaxItemCount() {
                return 1;
            }

            @Override
            public boolean canInsert(ItemStack stack) {
                return MobEntity.getPreferredEquipmentSlot(stack) == EquipmentSlot.CHEST;
            }

            @Override
            public Pair<Identifier, Identifier> getBackgroundSprite() {
                return Pair.of(atlas, new Identifier("item/empty_armor_slot_chestplate"));
            }
        });
        addSlot(new Slot(armorInventory, EquipmentSlot.LEGS.getEntitySlotId(), 80, 8) {
            @Override
            public int getMaxItemCount() {
                return 1;
            }

            @Override
            public boolean canInsert(ItemStack stack) {
                return MobEntity.getPreferredEquipmentSlot(stack) == EquipmentSlot.LEGS;
            }

            @Override
            public Pair<Identifier, Identifier> getBackgroundSprite() {
                return Pair.of(atlas, new Identifier("item/empty_armor_slot_leggings"));
            }
        });
        addSlot(new Slot(armorInventory, EquipmentSlot.FEET.getEntitySlotId(), 80, 44) {
            @Override
            public int getMaxItemCount() {
                return 1;
            }

            @Override
            public boolean canInsert(ItemStack stack) {
                return MobEntity.getPreferredEquipmentSlot(stack) == EquipmentSlot.FEET;
            }

            @Override
            public Pair<Identifier, Identifier> getBackgroundSprite() {
                return Pair.of(atlas, new Identifier("item/empty_armor_slot_boots"));
            }
        });
    }

}
