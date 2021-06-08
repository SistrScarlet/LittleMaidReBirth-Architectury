package net.sistr.littlemaidrebirth.tags;

import me.shedaniel.architectury.hooks.TagHooks;
import net.minecraft.item.Item;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LittleMaidReBirthMod;

public class LMTags {

    public static class Items {
        public static final Tag.Identified<Item> MAIDS_EMPLOYABLE = register("maids_employable");
        public static final Tag.Identified<Item> MAIDS_SALARY = register("maids_salary");

        private static Tag.Identified<Item> register(String id) {
            return TagHooks.getItemOptional(new Identifier(LittleMaidReBirthMod.MODID, id));
        }

    }
}
