package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CrossbowItem.class)
public interface CrossbowItemInvoker {

    @Invoker("getSpeed")
    static float getSpeed(ItemStack stack) {
        throw new AssertionError();
    }

}
