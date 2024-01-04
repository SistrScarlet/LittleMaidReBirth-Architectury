package net.sistr.littlemaidrebirth.api.mode;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidmodelloader.util.Tuple;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

import java.util.List;
import java.util.function.BiFunction;

/**
 * モードの生成と、モードアイテムの判別をするクラス
 * メイド専用
 */
public class ModeType<T extends Mode> {
    private final BiFunction<ModeType<T>, LittleMaidEntity, T> function;
    private final ImmutableList<Tuple<ItemMatcher.Priority, ItemMatcher>> matchers;

    protected ModeType(BiFunction<ModeType<T>, LittleMaidEntity, T> function, List<Tuple<ItemMatcher.Priority, ItemMatcher>> matchers) {
        this.function = function;
        this.matchers = ImmutableList.copyOf(matchers);
    }

    public T create(LittleMaidEntity maid) {
        return function.apply(this, maid);
    }

    /**
     * 注意：このメソッドはpriorityを考慮しない。
     */
    public boolean isModeItem(ItemStack stack) {
        return matchers.stream().anyMatch(matcher -> matcher.getB().isMatch(stack));
    }

    public List<Tuple<ItemMatcher.Priority, ItemMatcher>> getItemMatcherList() {
        return Lists.newArrayList(matchers);
    }

    public static <T extends Mode> Builder<T> builder(BiFunction<ModeType<T>, LittleMaidEntity, T> function) {
        return new Builder<>(function);
    }

    public static class Builder<T extends Mode> {
        private final BiFunction<ModeType<T>, LittleMaidEntity, T> function;
        private final ObjectArrayList<Tuple<ItemMatcher.Priority, ItemMatcher>> matchers = new ObjectArrayList<>();

        public Builder(BiFunction<ModeType<T>, LittleMaidEntity, T> function) {
            this.function = function;
        }

        /**
         * @deprecated 優先度が追加され、明示的に指定する必要がある。
         */
        @Deprecated
        public Builder<T> addItemMatcher(ItemMatcher matcher) {
            matchers.add(new Tuple<>(ItemMatcher.Priority.NORMAL, matcher));
            return this;
        }

        public Builder<T> addItemMatcher(ItemMatcher matcher, ItemMatcher.Priority priority) {
            matchers.add(new Tuple<>(priority, matcher));
            return this;
        }

        public ModeType<T> build() {
            return new ModeType<>(function, matchers);
        }

    }

}
