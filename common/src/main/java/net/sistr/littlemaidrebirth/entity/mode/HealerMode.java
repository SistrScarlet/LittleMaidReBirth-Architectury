package net.sistr.littlemaidrebirth.entity.mode;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

import java.util.OptionalInt;

public class HealerMode extends Mode {
    protected final LittleMaidEntity mob;
    protected final int inventoryStart;
    protected final int inventoryEnd;

    public HealerMode(ModeType<? extends Mode> modeType, String name, LittleMaidEntity mob, int inventoryStart, int inventoryEnd) {
        super(modeType, name);
        this.mob = mob;
        this.inventoryStart = inventoryStart;
        this.inventoryEnd = inventoryEnd;
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

}
