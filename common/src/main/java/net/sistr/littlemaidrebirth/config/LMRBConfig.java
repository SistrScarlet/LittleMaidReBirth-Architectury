package net.sistr.littlemaidrebirth.config;


import dev.architectury.injectables.annotations.ExpectPlatform;

public class LMRBConfig {

    @ExpectPlatform
    public static boolean canSpawnLM() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean canDespawnLM() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static int getSpawnWeightLM() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static int getMinSpawnGroupSizeLM() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static int getMaxSpawnGroupSizeLM() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean canResurrectionLM() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean canPickupItemByNoOwnerLM() {
        throw new AssertionError();
    }

}
