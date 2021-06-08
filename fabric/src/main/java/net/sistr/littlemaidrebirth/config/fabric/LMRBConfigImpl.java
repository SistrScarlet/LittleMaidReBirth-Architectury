package net.sistr.littlemaidrebirth.config.fabric;


public class LMRBConfigImpl {

    public static boolean canSpawnLM() {
        return true;
    }

    public static boolean canDespawnLM() {
        return false;
    }

    public static int getSpawnWeightLM() {
        return 5;
    }

    public static int getSpawnLimitLM() {
        return 20;
    }

    public static int getMaxSpawnGroupSizeLM() {
        return 3;
    }

    public static int getMinSpawnGroupSizeLM() {
        return 1;
    }

    public static boolean canResurrectionLM() {
        return false;
    }

    public static boolean canPickupItemByNoOwnerLM() {
        return true;
    }

}
