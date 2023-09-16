package net.sistr.littlemaidrebirth.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.Heightmap;

public class SpawnRestrictionRegister {
    @ExpectPlatform
    public static <T extends MobEntity> void callRegister(Object opt, EntityType<T> type, SpawnRestriction.Location location, Heightmap.Type heightmap, SpawnRestriction.SpawnPredicate<T> spawnPredicate) {
        throw new AssertionError("This should not occur!");
    }
}
