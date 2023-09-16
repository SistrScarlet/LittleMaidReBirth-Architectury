package net.sistr.littlemaidrebirth.util.forge;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;

public class SpawnRestrictionRegisterImpl {
    public static <T extends MobEntity> void callRegister(Object opt, EntityType<T> type, SpawnRestriction.Location location, Heightmap.Type heightmap, SpawnRestriction.SpawnPredicate<T> spawnPredicate) {
        if (opt instanceof SpawnPlacementRegisterEvent event) {
            event.register(type, location, heightmap, spawnPredicate, SpawnPlacementRegisterEvent.Operation.OR);
        }
    }
}
