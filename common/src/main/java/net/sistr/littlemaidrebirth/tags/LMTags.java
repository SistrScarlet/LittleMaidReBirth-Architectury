package net.sistr.littlemaidrebirth.tags;

import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LMRBMod;

/**
 * メイドさんに関するタグを置いとくとこ
 */
public class LMTags {

    public static class Items {
        public static final TagKey<Item> MAIDS_EMPLOYABLE = register("maids_employable");
        public static final TagKey<Item> MAIDS_SALARY = register("maids_salary");

        public static final TagKey<Item> FENCER_MODE = register("fencer_mode");
        public static final TagKey<Item> ARCHER_MODE = register("archer_mode");
        public static final TagKey<Item> COOKING_MODE = register("cooking_mode");
        public static final TagKey<Item> RIPPER_MODE = register("ripper_mode");
        public static final TagKey<Item> TORCHER_MODE = register("torcher_mode");
        public static final TagKey<Item> HEALER_MODE = register("healer_mode");

        private static TagKey<Item> register(String id) {
            return TagKey.of(RegistryKeys.ITEM, new Identifier(LMRBMod.MODID, id));
        }
    }
}
