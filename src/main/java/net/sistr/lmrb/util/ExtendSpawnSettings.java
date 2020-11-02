package net.sistr.lmrb.util;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.biome.SpawnSettings;

public interface ExtendSpawnSettings {

    void addSpawnEntry_LM(SpawnGroup spawnGroup, SpawnSettings.SpawnEntry spawnEntry);

    void addSpawnCost_LM(EntityType<?> entityType, double mass, double gravityLimit);

}
