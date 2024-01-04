package net.sistr.littlemaidrebirth.api.mode;

import net.minecraft.item.ItemStack;

/**
 * アイテムが条件にマッチするかをチェックするインターフェイス
 */
public interface ItemMatcher {
    boolean isMatch(ItemStack stack);

    /**
     * アイテムマッチの優先順位。
     * 重複のおそれがあるものは低く、おそれがないものは高く設定する。
     * バニラのクラスを指定する場合などはLOW、もしくはLOWERを推奨する。
     * */
    interface Priority {
        Priority LOWER = of(0);
        Priority LOW = of(100);
        Priority NORMAL = of(200);
        Priority HIGH = of(300);
        Priority HIGHER = of(400);

        int get();

        static Priority of(int priority) {
            return () -> priority;
        }

        default Priority offset(int offset) {
            return () -> get() + offset;
        }
    }
}
