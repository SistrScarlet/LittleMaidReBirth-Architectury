package net.sistr.littlemaidrebirth.api.mode;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

//todo ここからモード追加ができるようにする
public class ModeManager {

    public static ModeManager INSTANCE = new ModeManager();

    private final Map<Class<?>, ModeItems> MODES = Maps.newHashMap();

    public void register(Class<?> mode, ModeItems items) {
        MODES.put(mode, items);
    }

    public boolean containModeItem(Mode mode, ItemStack stack) {
        ModeItems modeItems = MODES.get(mode.getClass());
        if (modeItems == null) return false;
        return modeItems.contains(stack);
    }

    public Optional<ModeItems> getModeItems(Class<?> clazz) {
        return Optional.ofNullable(MODES.get(clazz));
    }

    //todo 除外判定
    public static class ModeItems {
        public final Set<String> names = Sets.newHashSet();
        public final Set<Item> items = Sets.newHashSet();
        public final Set<Class<?>> classes = Sets.newHashSet();
        public final Set<Class<?>> interfaces = Sets.newHashSet();
        public final Set<Tag<Item>> tags = Sets.newHashSet();
        public final Set<CheckModeItem> checkModeItems = Sets.newHashSet();

        public ModeItems add(String name) {
            names.add(name);
            return this;
        }

        public ModeItems add(Identifier name) {
            names.add(name.toString());
            return this;
        }

        public ModeItems add(ItemStack stack) {
            items.add(stack.getItem());
            return this;
        }

        public ModeItems add(Item item) {
            items.add(item);
            return this;
        }

        public ModeItems add(Class<?> itemClass) {
            if (itemClass.isInterface()) interfaces.add(itemClass);
            else classes.add(itemClass);
            return this;
        }

        public ModeItems add(Tag<Item> itemTag) {
            tags.add(itemTag);
            return this;
        }

        public ModeItems add(CheckModeItem checkModeItem) {
            checkModeItems.add(checkModeItem);
            return this;
        }

        public boolean contains(ItemStack stack) {
            for (CheckModeItem checkModeItem : checkModeItems) {
                if (checkModeItem.checkModeItem(stack)) {
                    return true;
                }
            }
            Item item = stack.getItem();
            if (items.contains(item)) return true;

            String itemName = Registry.ITEM.getId(item).toString();
            if (names.contains(itemName)) return true;

            for (Tag<Item> itemTag : tags) if (itemTag.contains(item)) return true;

            Class<?> itemClass = item.getClass();
            if (classes.contains(itemClass)) return true;

            for (Class<?> interfaceClass : interfaces) {
                if (interfaceClass.isAssignableFrom(itemClass)) {
                    classes.add(itemClass);
                    return true;
                }
            }

            if (isContainsSuperLoop(itemClass)) {
                classes.add(itemClass);
                return true;
            }
            return false;
        }

        private boolean isContainsSuperLoop(Class<?> itemClass) {
            Class<?> superItem = itemClass.getSuperclass();
            if (superItem == null) return false;
            if (classes.contains(superItem)) {
                return true;
            }
            return isContainsSuperLoop(superItem);
        }

    }

    public interface CheckModeItem {
        boolean checkModeItem(ItemStack stack);
    }

}
