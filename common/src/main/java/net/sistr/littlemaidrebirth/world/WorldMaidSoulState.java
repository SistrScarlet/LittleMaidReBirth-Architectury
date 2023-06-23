package net.sistr.littlemaidrebirth.world;

import com.google.common.collect.Maps;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.sistr.littlemaidrebirth.LMRBMod;
import org.apache.commons.compress.utils.Lists;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WorldMaidSoulState extends PersistentState {
    private final Map<UUID, List<MaidSoul>> maidSoulsMap = Maps.newHashMap();

    public void add(UUID ownerId, NbtCompound maidNbt) {
        maidSoulsMap.computeIfAbsent(ownerId, (id) -> Lists.newArrayList())
                .add(new MaidSoul(maidNbt));
    }

    public List<MaidSoul> get(UUID ownerId) {
        return maidSoulsMap.computeIfAbsent(ownerId, id -> Lists.newArrayList());
    }

    public void remove(UUID ownerId) {
        this.maidSoulsMap.remove(ownerId);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        var nbtEntries = new NbtList();
        for (Map.Entry<UUID, List<MaidSoul>> entry : maidSoulsMap.entrySet()) {
            var uuid = entry.getKey();
            var list = entry.getValue();
            var nbtEntry = new NbtCompound();
            nbtEntry.putUuid("id", uuid);
            var nbtMaidSouls = new NbtList();
            for (MaidSoul maidSoul : list) {
                nbtMaidSouls.add(maidSoul.nbt());
            }
            nbtEntry.put("maidSouls", nbtMaidSouls);
            nbtEntries.add(nbtEntry);
        }
        nbt.put("maidSoulsEntries", nbtEntries);
        return nbt;
    }

    public static WorldMaidSoulState createFromNbt(NbtCompound nbt) {
        WorldMaidSoulState worldMaidSoulState = new WorldMaidSoulState();
        var nbtEntries = nbt.getList("maidSoulsEntries", NbtElement.COMPOUND_TYPE);
        for (NbtElement nbtEntry : nbtEntries) {
            var id = ((NbtCompound) nbtEntry).getUuid("id");
            var nbtMaidSouls = ((NbtCompound) nbtEntry).getList("maidSouls", NbtElement.COMPOUND_TYPE);
            List<MaidSoul> maidSouls = Lists.newArrayList();
            for (NbtElement nbtMaidSoul : nbtMaidSouls) {
                maidSouls.add(new MaidSoul((NbtCompound) nbtMaidSoul));
            }
            worldMaidSoulState.maidSoulsMap.put(id, maidSouls);
        }
        return worldMaidSoulState;
    }

    public static WorldMaidSoulState getWorldMaidSoulState(ServerWorld world) {
        var persistentStateManager = world.getPersistentStateManager();

        return persistentStateManager.getOrCreate(
                WorldMaidSoulState::createFromNbt,
                WorldMaidSoulState::new,
                LMRBMod.MODID + "_maidsouls");
    }

    public record MaidSoul(NbtCompound nbt) {
    }
}
