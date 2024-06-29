package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CrossbowItem.class)
public interface CrossbowItemInvoker {

    @Invoker("getSpeed")
    static float getSpeed(ChargedProjectilesComponent stack) {
        throw new AssertionError();
    }

}
