package net.sistr.littlemaidrebirth.tags;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
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
            return TagKey.of(Registry.ITEM_KEY, new Identifier(LMRBMod.MODID, id));
        }
    }

    public static class Blocks {
        public static final TagKey<Block> MAID_ALTER_COMPONENT_BLOCKS = register("maid_alter_component_blocks");

        private static TagKey<Block> register(String id) {
            return TagKey.of(Registry.BLOCK_KEY, new Identifier(LMRBMod.MODID, id));
        }
    }

    public static class Biomes {
        public static final TagKey<Biome> MAID_SPAWN_BIOME = register("maid_spawn_biome");
        public static final TagKey<Biome> MAID_SPAWN_EXCLUDE_BIOME = register("maid_spawn_exclude_biome");

        private static TagKey<Biome> register(String id) {
            return TagKey.of(Registry.BIOME_KEY, new Identifier(LMRBMod.MODID, id));
        }
    }
}
