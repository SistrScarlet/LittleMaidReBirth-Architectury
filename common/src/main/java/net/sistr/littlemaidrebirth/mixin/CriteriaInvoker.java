package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.advancement.criterion.Criterion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Criteria.class)
public interface CriteriaInvoker {

    @Invoker
    static <T extends Criterion<?>> T invokeRegister(T object) {
        throw new AssertionError();
    }
}
