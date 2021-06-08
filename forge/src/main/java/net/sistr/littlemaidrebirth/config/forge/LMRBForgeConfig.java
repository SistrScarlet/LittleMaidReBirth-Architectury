package net.sistr.littlemaidrebirth.config.forge;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod.EventBusSubscriber
public class LMRBForgeConfig {

    public static final String CATEGORY_COMMON = "common";
    public static final String CATEGORY_CLIENT = "client";
    public static final String SUBCATEGORY_SPAWN = "spawn";
    public static final String SUBCATEGORY_OTHER = "other";

    public static ForgeConfigSpec COMMON_CONFIG;
    public static ForgeConfigSpec CLIENT_CONFIG;

    public static ForgeConfigSpec.BooleanValue CAN_SPAWN_LM;
    public static ForgeConfigSpec.BooleanValue CAN_DESPAWN_LM;
    public static ForgeConfigSpec.IntValue SPAWN_WEIGHT_LM;
    public static ForgeConfigSpec.IntValue SPAWN_LIMIT_LM;
    public static ForgeConfigSpec.IntValue SPAWN_MAX_GROUP_SIZE_LM;
    public static ForgeConfigSpec.IntValue SPAWN_MIN_GROUP_SIZE_LM;

    public static ForgeConfigSpec.BooleanValue CAN_RESURRECTION_LM;
    public static ForgeConfigSpec.BooleanValue CAN_PICKUP_ITEM_BY_NO_OWNER_LM;

    static {
        ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
        ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();


        COMMON_BUILDER.comment("Common settings").push(CATEGORY_COMMON);

        setupSpawnConfig(COMMON_BUILDER);
        setupOtherConfig(COMMON_BUILDER);

        COMMON_BUILDER.pop();

        CLIENT_BUILDER.comment("Client settings").push(CATEGORY_CLIENT);
        CLIENT_BUILDER.pop();

        COMMON_CONFIG = COMMON_BUILDER.build();
        CLIENT_CONFIG = CLIENT_BUILDER.build();
    }

    private static void setupSpawnConfig(ForgeConfigSpec.Builder COMMON_BUILDER) {
        COMMON_BUILDER.comment("Spawn settings").push(SUBCATEGORY_SPAWN);
        CAN_SPAWN_LM = COMMON_BUILDER.comment("Whether LittleMaid can spawn or not")
                .define("canSpawnLM", true);
        CAN_DESPAWN_LM = COMMON_BUILDER.comment("Whether LittleMaid can despawn or not")
                .define("canDespawnLM", false);
        SPAWN_WEIGHT_LM = COMMON_BUILDER.comment("LittleMaid spawn weight")
                .defineInRange("spawnWeightLM", 5, 1, 50);
        SPAWN_LIMIT_LM = COMMON_BUILDER.comment("LittleMaid spawn limit")
                .defineInRange("spawnLimitLM", 20, 1, 200);
        SPAWN_MAX_GROUP_SIZE_LM = COMMON_BUILDER.comment("LittleMaid max group size")
                .defineInRange("spawnMaxGroupSizeLM", 3, 1, 30);
        SPAWN_MIN_GROUP_SIZE_LM = COMMON_BUILDER.comment("LittleMaid min group size")
                .defineInRange("spawnMinGroupSizeLM", 1, 1, 30);
        COMMON_BUILDER.pop();
    }

    private static void setupOtherConfig(ForgeConfigSpec.Builder COMMON_BUILDER) {
        COMMON_BUILDER.comment("Other settings").push(SUBCATEGORY_OTHER);
        CAN_RESURRECTION_LM = COMMON_BUILDER.comment("Whether LittleMaid will be back or not")
                .define("canResurrectionLM", false);
        CAN_PICKUP_ITEM_BY_NO_OWNER_LM = COMMON_BUILDER.comment("Whether LMs without owners will pick up items")
                .define("canPickupItemByNoOwnerLM", true);
        COMMON_BUILDER.pop();
    }

    @SubscribeEvent
    public static void onLoad(final ModConfig.Loading configEvent) {

    }

    @SubscribeEvent
    public static void onReload(final ModConfig.Reloading configEvent) {
    }

}
