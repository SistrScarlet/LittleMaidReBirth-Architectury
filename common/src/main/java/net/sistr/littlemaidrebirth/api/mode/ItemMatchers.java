package net.sistr.littlemaidrebirth.api.mode;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.Tag;
import net.minecraft.util.registry.Registry;

public class ItemMatchers {

    public static ItemMatcher item(Item item) {
        return new ItemInstance(item);
    }

    public static ItemMatcher name(String name) {
        return new NameMatcher(name);
    }

    public static ItemMatcher tag(Tag<Item> tag) {
        return new TagMatcher(tag);
    }

    public static ItemMatcher clazz(Class<?> clazz) {
        return new ClassMatcher(clazz);
    }

    private static class ItemInstance implements ItemMatcher {
        private final Item item;

        public ItemInstance(Item item) {
            this.item = item;
        }

        @Override
        public boolean isMatch(ItemStack stack) {
            return stack.getItem() == item;
        }
    }

    private static class NameMatcher implements ItemMatcher {
        private final String name;

        public NameMatcher(String name) {
            this.name = name;
        }

        @Override
        public boolean isMatch(ItemStack stack) {
            String itemName = Registry.ITEM.getId(stack.getItem()).toString();
            return name.contains(itemName);
        }
    }

    private static class TagMatcher implements ItemMatcher {
        private final Tag<Item> tag;

        public TagMatcher(Tag<Item> tag) {
            this.tag = tag;
        }

        @Override
        public boolean isMatch(ItemStack stack) {
            return tag.contains(stack.getItem());
        }
    }

    private static class ClassMatcher implements ItemMatcher {
        private final Class<?> clazz;

        public ClassMatcher(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public boolean isMatch(ItemStack stack) {
            Class<?> itemClass = stack.getItem().getClass();
            return clazz == itemClass || clazz.isAssignableFrom(itemClass);
        }

    }

}
