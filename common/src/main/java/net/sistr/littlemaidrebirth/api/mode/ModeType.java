package net.sistr.littlemaidrebirth.api.mode;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

import java.util.List;
import java.util.function.BiFunction;

public class ModeType<T extends Mode> {
    private final BiFunction<ModeType<T>, LittleMaidEntity, T> function;
    private final ImmutableList<ItemMatcher> matchers;

    protected ModeType(BiFunction<ModeType<T>, LittleMaidEntity, T> function, List<ItemMatcher> matchers) {
        this.function = function;
        this.matchers = ImmutableList.copyOf(matchers);
    }

    public T create(LittleMaidEntity maid) {
        return function.apply(this, maid);
    }

    public boolean isModeItem(ItemStack stack) {
        return matchers.stream().anyMatch(matcher -> matcher.isMatch(stack));
    }

    public static <T extends Mode> Builder<T> builder(BiFunction<ModeType<T>, LittleMaidEntity, T> function) {
        return  new Builder<>(function);
    }

    public static class Builder<T extends Mode> {
        private final BiFunction<ModeType<T>, LittleMaidEntity, T> function;
        private final ObjectArrayList<ItemMatcher> matchers = new ObjectArrayList<>();

        public Builder(BiFunction<ModeType<T>, LittleMaidEntity, T> function) {
            this.function = function;
        }

        public Builder<T> addItemMatcher(ItemMatcher matcher) {
            matchers.add(matcher);
            return this;
        }

        public ModeType<T> build() {
            return new ModeType<>(function, matchers);
        }

    }

}
