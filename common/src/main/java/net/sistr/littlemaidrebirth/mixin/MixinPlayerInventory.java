package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.sistr.littlemaidrebirth.util.PlayerInventoryAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(PlayerInventory.class)
public class MixinPlayerInventory implements PlayerInventoryAccessor {


    @Shadow @Final private List<DefaultedList<ItemStack>> combinedInventory;

    @Override
    public List<DefaultedList<ItemStack>> getCombinedInventory() {
        return this.combinedInventory;
    }
}
