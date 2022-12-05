package net.sistr.littlemaidrebirth.api.mode;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.TagKey;
import net.minecraft.util.registry.Registry;

/**
 * よく使うItemMatcherをまとめたクラス
 */
public class ItemMatchers {

    public static ItemMatcher item(Item item) {
        return new ItemInstance(item);
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
            String itemName = Registry.ITEM.getId(stack.getItem()).toString();
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
