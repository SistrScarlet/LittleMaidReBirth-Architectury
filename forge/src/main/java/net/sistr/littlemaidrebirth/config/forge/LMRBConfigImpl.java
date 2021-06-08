package net.sistr.littlemaidrebirth.config.forge;


public class LMRBConfigImpl {

    public static boolean canSpawnLM() {
        return LMRBForgeConfig.CAN_SPAWN_LM.get();
    }

    public static boolean canDespawnLM() {
        return LMRBForgeConfig.CAN_DESPAWN_LM.get();
    }

    public static int getSpawnWeightLM() {
        return LMRBForgeConfig.SPAWN_WEIGHT_LM.get();
    }

    public static int getSpawnLimitLM() {
        return LMRBForgeConfig.SPAWN_LIMIT_LM.get();
    }

    public static int getMaxSpawnGroupSizeLM() {
        return LMRBForgeConfig.SPAWN_MAX_GROUP_SIZE_LM.get();
    }

    public static int getMinSpawnGroupSizeLM() {
        return LMRBForgeConfig.SPAWN_MIN_GROUP_SIZE_LM.get();
    }

    public static boolean canResurrectionLM() {
        return LMRBForgeConfig.CAN_RESURRECTION_LM.get();
    }

    public static boolean canPickupItemByNoOwnerLM() {
        return LMRBForgeConfig.CAN_PICKUP_ITEM_BY_NO_OWNER_LM.get();
    }

}
