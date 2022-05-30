package net.sistr.littlemaidrebirth.util.forge;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.Heightmap;

public class SpawnRestrictionRegisterImpl {
    public static <T extends MobEntity> void callRegister(EntityType<T> type, SpawnRestriction.Location location, Heightmap.Type heightmap, SpawnRestriction.SpawnPredicate<T> spawnPredicate) {
        SpawnRestriction.register(type, location, heightmap, spawnPredicate);
    }
}
