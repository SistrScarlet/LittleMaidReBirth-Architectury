package net.sistr.littlemaidrebirth.entity.mode;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeManager;
import net.sistr.littlemaidrebirth.entity.InventorySupplier;
import net.sistr.littlemaidrebirth.entity.Tameable;

import java.util.OptionalInt;

public class HealerMode<T extends PathAwareEntity & Tameable & InventorySupplier> implements Mode {
    protected final T mob;
    protected final int inventoryStart;
    protected final int inventoryEnd;

    public HealerMode(T mob, int inventoryStart, int inventoryEnd) {
        this.mob = mob;
        this.inventoryStart = inventoryStart;
        this.inventoryEnd = inventoryEnd;
    }

    @Override
    public void startModeTask() {

    }

    @Override
    public boolean shouldExecute() {
        LivingEntity owner = mob.getTameOwner().orElse(null);
        if (!(owner instanceof PlayerEntity)) return false;
        if (!((PlayerEntity) owner).getHungerManager().isNotFull()) return false;
        return getFoodsIndex().isPresent();
    }

    public OptionalInt getFoodsIndex() {
        Inventory inventory = this.mob.getInventory();
        for (int i = inventoryStart; i < inventoryEnd; ++i) {
            ItemStack itemstack = inventory.getStack(i);
            if (isFoods(itemstack)) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    public boolean isFoods(ItemStack stack) {
        return stack.getItem().isFood();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return shouldExecute();
    }

    @Override
    public void startExecuting() {

    }

    @Override
    public void tick() {
        getFoodsIndex().ifPresent(index -> {
            Inventory inventory = mob.getInventory();
            ItemStack stack = inventory.getStack(index);
            mob.getTameOwner()
                    .ifPresent(owner -> {
                        owner.eatFood(owner.world, stack);
                        inventory.removeStack(index, 0);
                        if (owner instanceof SoundPlayable)
                            ((SoundPlayable) owner).play(LMSounds.HEALING);
                    });
        });
    }

    @Override
    public void resetTask() {

    }

    @Override
    public void endModeTask() {

    }

    @Override
    public void writeModeData(NbtCompound nbt) {

    }

    @Override
    public void readModeData(NbtCompound nbt) {

    }

    @Override
    public String getName() {
        return "Healer";
    }

    static {
        ModeManager.ModeItems items = new ModeManager.ModeItems();
        items.add(stack -> stack.getItem().isFood());
        ModeManager.INSTANCE.register(HealerMode.class, items);
    }

}
