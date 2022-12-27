package net.sistr.littlemaidrebirth.util.fabric;

import net.fabricmc.fabric.mixin.object.builder.SpawnRestrictionAccessor;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.Heightmap;

public class SpawnRestrictionRegisterImpl {
    public static <T extends MobEntity> void callRegister(EntityType<T> type, SpawnRestriction.Location location, Heightmap.Type heightmap, SpawnRestriction.SpawnPredicate<T> spawnPredicate) {
        SpawnRestrictionAccessor.callRegister(type, location, heightmap, spawnPredicate);
    }
}
