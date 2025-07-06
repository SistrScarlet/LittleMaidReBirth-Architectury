package net.sistr.littlemaidrebirth.api.mode;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;

import java.util.function.Supplier;

/**
 * よく使うItemMatcherをまとめたクラス
 */
public class ItemMatchers {

    @Deprecated
    public static ItemMatcher item(Item item) {
        return new ItemInstance(item);
    }

    // Mod読み込み順の問題でエラーが発生する場合があるため、Modアイテムにはこちらを使用すること
    public static ItemMatcher item(Supplier<Item> item) {
        return (stack) -> stack.getItem() == item.get();
    }

    public static ItemMatcher name(String name) {
        return new NameMatcher(name);
    }

    public static ItemMatcher tag(TagKey<Item> tag) {
        return new TagMatcher(tag);
    }

    public static ItemMatcher clazz(Class<?> clazz) {
        return new ClassMatcher(clazz);
    }

    private record ItemInstance(Item item) implements ItemMatcher {

        @Override
        public boolean isMatch(ItemStack stack) {
            return stack.getItem() == item;
        }
    }

    private record NameMatcher(String name) implements ItemMatcher {

        @Override
        public boolean isMatch(ItemStack stack) {
            String itemName = Registries.ITEM.getId(stack.getItem()).toString();
            return name.contains(itemName);
        }
    }

    private record TagMatcher(TagKey<Item> tag) implements ItemMatcher {

        @Override
        public boolean isMatch(ItemStack stack) {
            return stack.isIn(tag);
        }
    }

    private record ClassMatcher(Class<?> clazz) implements ItemMatcher {

        @Override
        public boolean isMatch(ItemStack stack) {
            Class<?> itemClass = stack.getItem().getClass();
            return clazz == itemClass || clazz.isAssignableFrom(itemClass);
        }

    }

}
