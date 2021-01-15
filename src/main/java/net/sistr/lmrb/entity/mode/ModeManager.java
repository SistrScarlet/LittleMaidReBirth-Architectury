package net.sistr.lmrb.entity.mode;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.Map;
import java.util.Set;

//todo モード切替判定用のインターフェースとデフォルト実装を用意してここではそれのインスタンスを保持する
public class ModeManager {

    public static ModeManager INSTANCE = new ModeManager();

    public final Map<Class<?>, ModeItems> MODES = Maps.newHashMap();

    public void register(Class<?> mode, ModeItems items) {
        MODES.put(mode, items);
    }

    public boolean containModeItem(Mode mode, ItemStack stack) {
        ModeItems modeItems = MODES.get(mode.getClass());
        if (modeItems == null) return false;
        return modeItems.contains(stack);
    }

    //todo 除外判定
    public static class ModeItems {
        public final Set<String> itemNames = Sets.newHashSet();
        public final Set<String> excludeItemNames = Sets.newHashSet();
        public final Set<Item> items = Sets.newHashSet();
        public final Set<Item> excludeItems = Sets.newHashSet();
        public final Set<Class<?>> itemClasses = Sets.newHashSet();
        public final Set<Class<?>> excludeItemClasses = Sets.newHashSet();
        public final Set<Tag<Item>> itemTags = Sets.newHashSet();
        public final Set<Tag<Item>> excludeItemTags = Sets.newHashSet();
        public final Set<CheckModeItem> checkModeItems = Sets.newHashSet();
        public final Set<CheckModeItem> excludeCheckModeItems = Sets.newHashSet();

        public ModeItems add(String name) {
            itemNames.add(name);
            return this;
        }

        public ModeItems add(Identifier name) {
            itemNames.add(name.toString());
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
            itemClasses.add(itemClass);
            return this;
        }

        public ModeItems add(Tag<Item> itemTag) {
            itemTags.add(itemTag);
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
            if (items.contains(item)) {
                return true;
            }
            String itemName = Registry.ITEM.getId(item).toString();
            if (itemNames.contains(itemName)) {
                return true;
            }
            for (Tag<Item> itemTag : itemTags) {
                if (itemTag.contains(item)) {
                    return true;
                }
            }
            Class<?> itemClass = item.getClass();
            if (itemClasses.contains(itemClass)) {
                return true;
            }
            return isContainsSuperLoop(itemClass);
        }

        private boolean isContainsSuperLoop(Class<?> itemClass) {
            Class<?> superItem = itemClass.getSuperclass();
            if (superItem == null) return false;
            if (itemClasses.contains(superItem)) {
                return true;
            }
            return isContainsSuperLoop(superItem);
        }

    }

    public interface CheckModeItem {
        boolean checkModeItem(ItemStack stack);
    }

}
