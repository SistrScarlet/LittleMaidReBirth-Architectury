package net.sistr.littlemaidrebirth.block;

import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.util.SalaryBoxPosListener;
import net.sistr.littlemaidrebirth.setup.Registration;
import net.sistr.littlemaidrebirth.tags.LMTags;

public class SalaryBoxBlockEntity extends LootableContainerBlockEntity {
    private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(27, ItemStack.EMPTY);
    private final ViewerCountManager stateManager =
            new ViewerCountManager() {

                @Override
                protected void onContainerOpen(World world, BlockPos pos, BlockState state) {
                    SalaryBoxBlockEntity.this.playSound(state, SoundEvents.BLOCK_BARREL_OPEN);
                    SalaryBoxBlockEntity.this.setOpen(state, true);
                }

                @Override
                protected void onContainerClose(World world, BlockPos pos, BlockState state) {
                    SalaryBoxBlockEntity.this.playSound(state, SoundEvents.BLOCK_BARREL_CLOSE);
                    SalaryBoxBlockEntity.this.setOpen(state, false);
                }

                @Override
                protected void onViewerCountUpdate(
                        World world,
                        BlockPos pos,
                        BlockState state,
                        int oldViewerCount,
                        int newViewerCount) {}

                @Override
                protected boolean isPlayerViewing(PlayerEntity player) {
                    if (player.currentScreenHandler instanceof GenericContainerScreenHandler) {
                        Inventory inventory =
                                ((GenericContainerScreenHandler) player.currentScreenHandler)
                                        .getInventory();
                        return inventory == SalaryBoxBlockEntity.this;
                    }
                    return false;
                }
            };

    public SalaryBoxBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.SALARY_BOX_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        if (!this.writeLootTable(nbt)) {
            Inventories.writeNbt(nbt, this.inventory, registries);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        if (!this.readLootTable(nbt)) {
            Inventories.readNbt(nbt, this.inventory, registries);
        }
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    protected DefaultedList<ItemStack> getHeldStacks() {
        return this.inventory;
    }

    @Override
    protected void setHeldStacks(DefaultedList<ItemStack> list) {
        this.inventory = list;
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("container.littlemaidrebirth.salary_box");
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, this);
    }

    @Override
    public void onOpen(PlayerEntity player) {
        if (!this.removed && !player.isSpectator()) {
            this.stateManager.openContainer(
                    player, this.getWorld(), this.getPos(), this.getCachedState());
        }
    }

    @Override
    public void onClose(PlayerEntity player) {
        if (!this.removed && !player.isSpectator()) {
            this.stateManager.closeContainer(
                    player, this.getWorld(), this.getPos(), this.getCachedState());
        }
    }

    public void tick() {
        if (!this.removed) {
            this.stateManager.updateViewerCount(
                    this.getWorld(), this.getPos(), this.getCachedState());
        }
    }

    public static void tick(
            World world, BlockPos pos, BlockState state, SalaryBoxBlockEntity blockEntity) {
        if (!blockEntity.hasSalary()) {
            return;
        }
        if (world.getRandom().nextFloat() > (1.0f / getConfigInterval())) {
            return;
        }

        var centerPos = pos.toCenterPos();
        float range = getConfigNotifyRange();
        var box =
                new Box(
                        centerPos.x - range,
                        centerPos.y - range,
                        centerPos.z - range,
                        centerPos.x + range,
                        centerPos.y + range,
                        centerPos.z + range);
        var entityList =
                world.getEntitiesByClass(
                        Entity.class,
                        box,
                        e -> e instanceof SalaryBoxPosListener && isinNotifyRange(pos, e.getPos()));
        for (Entity entity : entityList) {
            ((SalaryBoxPosListener) entity).listenSalaryBoxPos(pos);
        }
    }

    void setOpen(BlockState state, boolean open) {
        if (this.world == null) {
            return;
        }
        this.world.setBlockState(
                this.getPos(), state.with(BarrelBlock.OPEN, open), Block.NOTIFY_ALL);
    }

    void playSound(BlockState state, SoundEvent soundEvent) {
        if (this.world == null) {
            return;
        }
        Vec3i vec3i = state.get(BarrelBlock.FACING).getVector();
        double d = this.pos.getX() + 0.5 + vec3i.getX() / 2.0;
        double e = this.pos.getY() + 0.5 + vec3i.getY() / 2.0;
        double f = this.pos.getZ() + 0.5 + vec3i.getZ() / 2.0;
        this.world.playSound(
                null,
                d,
                e,
                f,
                soundEvent,
                SoundCategory.BLOCKS,
                0.5f,
                this.world.random.nextFloat() * 0.1f + 0.9f);
    }

    public static boolean isinNotifyRange(Vec3i boxPos, Vec3d entityPos) {
        return boxPos.getSquaredDistance(entityPos)
                < getConfigNotifyRange() * getConfigNotifyRange();
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return stack.isIn(LMTags.Items.MAIDS_SALARY);
    }

    public boolean hasSalary() {
        for (int i = 0; i < this.size(); i++) {
            var stack = this.getStack(i);
            if (!stack.isEmpty() && stack.isIn(LMTags.Items.MAIDS_SALARY)) {
                return true;
            }
        }
        return false;
    }

    private static float getConfigNotifyRange() {
        return LMRBMod.getConfig().contract.memorySalaryBoxDistance;
    }

    private static int getConfigInterval() {
        return LMRBMod.getConfig().contract.memorySalaryBoxInterval;
    }
}
