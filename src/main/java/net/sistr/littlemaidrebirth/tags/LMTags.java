package net.sistr.littlemaidrebirth.tags;

import net.fabricmc.fabric.api.tag.TagRegistry;
import net.minecraft.item.Item;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LittleMaidReBirthMod;

public class LMTags {

    public static class Items {
        public static final Tag<Item> MAIDS_EMPLOYABLE = register("maids_employable");
        public static final Tag<Item> MAIDS_SALARY = register("maids_salary");

        private static Tag<Item> register(String id) {
            return TagRegistry.item(new Identifier(LittleMaidReBirthMod.MODID, id));
        }

    }
}
