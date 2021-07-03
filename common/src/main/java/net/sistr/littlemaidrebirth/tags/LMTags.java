package net.sistr.littlemaidrebirth.tags;

import dev.architectury.hooks.tags.TagHooks;
import net.minecraft.item.Item;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LittleMaidReBirthMod;

public class LMTags {

    public static class Items {
        public static final Tag.Identified<Item> MAIDS_EMPLOYABLE = register("maids_employable");
        public static final Tag.Identified<Item> MAIDS_SALARY = register("maids_salary");

        public static final Tag.Identified<Item> FENCER_MODE = register("fencer_mode");
        public static final Tag.Identified<Item> ARCHER_MODE = register("archer_mode");
        public static final Tag.Identified<Item> COOKING_MODE = register("cooking_mode");
        public static final Tag.Identified<Item> RIPPER_MODE = register("ripper_mode");
        public static final Tag.Identified<Item> TORCHER_MODE = register("torcher_mode");
        public static final Tag.Identified<Item> HEALER_MODE = register("healer_mode");

        private static Tag.Identified<Item> register(String id) {
            return TagHooks.optionalItem(new Identifier(LittleMaidReBirthMod.MODID, id));
        }
    }
}
