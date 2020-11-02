package net.sistr.lmrb.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.biome.SpawnSettings;
import net.sistr.lmrb.util.ExtendSpawnSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(SpawnSettings.class)
public class MixinSpawnSettings implements ExtendSpawnSettings {
    @Shadow @Final private Map<SpawnGroup, List<SpawnSettings.SpawnEntry>> spawners;
    @Shadow @Final private Map<EntityType<?>, SpawnSettings.SpawnDensity> spawnCosts;
    private final Map<SpawnGroup, List<SpawnSettings.SpawnEntry>> additionalSpawners = init();
    private final Map<EntityType<?>, SpawnSettings.SpawnDensity> additionalSpawnCosts = Maps.newLinkedHashMap();
    private final Map<SpawnGroup, List<SpawnSettings.SpawnEntry>> cacheSpawners = init();

    private static Map<SpawnGroup, List<SpawnSettings.SpawnEntry>> init() {
        Map<SpawnGroup, List<SpawnSettings.SpawnEntry>> temp = Maps.newHashMap();
        for (SpawnGroup spawnGroup : SpawnGroup.values()) {
            temp.put(spawnGroup, Lists.newArrayList());
        }
        return ImmutableMap.copyOf(temp);
    }

    @Override
    public void addSpawnEntry_LM(SpawnGroup spawnGroup, SpawnSettings.SpawnEntry spawnEntry) {
        this.additionalSpawners.get(spawnGroup).add(spawnEntry);
    }

    @Override
    public void addSpawnCost_LM(EntityType<?> entityType, double mass, double gravityLimit) {
        this.additionalSpawnCosts.put(entityType, new SpawnSettings.SpawnDensity(gravityLimit, mass));
    }

    @Inject(at = @At("HEAD"), method = "getSpawnEntry", cancellable = true)
    public void onGetSpawnEntry(SpawnGroup spawnGroup, CallbackInfoReturnable<List<SpawnSettings.SpawnEntry>> cir) {
        List<SpawnSettings.SpawnEntry> entries = spawners.get(spawnGroup);
        List<SpawnSettings.SpawnEntry> additionalEntries = additionalSpawners.get(spawnGroup);
        List<SpawnSettings.SpawnEntry> cacheEntries = cacheSpawners.get(spawnGroup);
        if (entries.size() + additionalEntries.size() != cacheEntries.size()) {
            cacheEntries.clear();
            cacheEntries.addAll(entries);
            cacheEntries.addAll(additionalEntries);
        }
        cir.setReturnValue(cacheEntries);
    }

    /*@Inject(at = @At("HEAD"), method = "getSpawnDensity", cancellable = true)
    public void onGetSpawnDensity(EntityType<?> entityType, CallbackInfoReturnable<SpawnSettings.SpawnDensity> cir) {
        if (additionalSpawnCosts.isEmpty()) {
            return;
        }
        if (spawnCosts.containsKey(entityType)) {
            return;
        }
        cir.setReturnValue(additionalSpawnCosts.get(entityType));
    }*/
}
