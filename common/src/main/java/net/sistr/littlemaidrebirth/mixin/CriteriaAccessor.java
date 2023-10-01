package net.sistr.littlemaidrebirth.mixin;

import com.google.common.collect.BiMap;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.advancement.criterion.Criterion;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Criteria.class)
public interface CriteriaAccessor {

    @Accessor("VALUES")
    static BiMap<Identifier, Criterion<?>> getValues() {
        throw new AssertionError();
    }

}
