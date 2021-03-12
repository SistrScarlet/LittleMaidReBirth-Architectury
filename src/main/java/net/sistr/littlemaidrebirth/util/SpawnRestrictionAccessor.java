package net.sistr.littlemaidrebirth.util;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.Heightmap;

public interface SpawnRestrictionAccessor {

    <T extends MobEntity> void register(EntityType<T> type, SpawnRestriction.Location location,
                                        Heightmap.Type heightmapType, SpawnRestriction.SpawnPredicate<T> predicate);

}
